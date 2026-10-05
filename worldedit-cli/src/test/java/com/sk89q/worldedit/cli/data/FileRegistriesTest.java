/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.cli.data;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileRegistriesTest {

    private static final int DATA_VERSION = 3700;
    private static final String DATA =
        """
        {
          "blocks": {
            "minecraft:stone": {"defaultstate": "minecraft:stone", "properties": {}},
            "minecraft:oak_log": {
              "defaultstate": "minecraft:oak_log[axis=y]",
              "properties": {"axis": {"values": ["x", "y", "z"], "type": "enum"}}
            },
            "minecraft:air": {"defaultstate": "minecraft:air"}
          },
          "items": ["minecraft:stone"],
          "biomes": ["minecraft:plains"],
          "blocktags": {"minecraft:logs": ["minecraft:oak_log"]}
        }
        """;

    private static final FileRegistries.Downloader NO_DOWNLOADS = url -> {
        throw new AssertionError("Should not download " + url);
    };

    @TempDir
    Path tempDir;

    private Path cachedFile() {
        return tempDir.resolve(DATA_VERSION + "_1.json");
    }

    @Test
    void loadsCachedFileWithoutDownloading() throws IOException {
        Files.writeString(cachedFile(), DATA);
        DataFile dataFile = FileRegistries.loadDataFile(tempDir, DATA_VERSION, NO_DOWNLOADS);

        assertEquals(3, dataFile.blocks().size());
        assertEquals("minecraft:oak_log[axis=y]", dataFile.blocks().get("minecraft:oak_log").defaultState());
        assertEquals(List.of("x", "y", "z"), dataFile.blocks().get("minecraft:oak_log").properties().get("axis").values());
        assertEquals(List.of("minecraft:stone"), dataFile.items());
        assertEquals(Map.of("minecraft:logs", List.of("minecraft:oak_log")), dataFile.blockTags());
    }

    @Test
    void missingSectionsAreEmpty() throws IOException {
        Files.writeString(cachedFile(), DATA);
        DataFile dataFile = FileRegistries.loadDataFile(tempDir, DATA_VERSION, NO_DOWNLOADS);

        assertTrue(dataFile.entities().isEmpty());
        assertTrue(dataFile.itemTags().isEmpty());
        assertTrue(dataFile.entityTags().isEmpty());
        assertTrue(dataFile.blocks().get("minecraft:air").properties().isEmpty());
    }

    @Test
    void downloadsAndCachesMissingFile() throws IOException {
        Path cacheFolder = tempDir.resolve("cli-data");
        AtomicReference<URL> requested = new AtomicReference<>();
        DataFile dataFile = FileRegistries.loadDataFile(cacheFolder, DATA_VERSION, url -> {
            requested.set(url);
            return new ByteArrayInputStream(DATA.getBytes(StandardCharsets.UTF_8));
        });

        assertEquals(3, dataFile.blocks().size());
        assertTrue(requested.get().toString().endsWith("/" + DATA_VERSION + "/1"), requested.get()::toString);
        assertEquals(DATA, Files.readString(cacheFolder.resolve(DATA_VERSION + "_1.json")));
        assertEquals(List.of(cacheFolder.resolve(DATA_VERSION + "_1.json")), listFiles(cacheFolder));

        // Second load is served from the cache
        assertEquals(dataFile, FileRegistries.loadDataFile(cacheFolder, DATA_VERSION, NO_DOWNLOADS));
    }

    @Test
    void failedDownloadLeavesNothingBehind() throws IOException {
        RuntimeException e = assertThrows(RuntimeException.class,
            () -> FileRegistries.loadDataFile(tempDir, DATA_VERSION, _ -> new InputStream() {
                private int sent;

                @Override
                public int read() throws IOException {
                    if (sent++ < 10) {
                        return '{';
                    }
                    throw new IOException("Connection reset");
                }

                @Override
                public int read(byte[] b, int off, int len) throws IOException {
                    if (len == 0) {
                        return 0;
                    }
                    int value = read();
                    b[off] = (byte) value;
                    return 1;
                }
            }));
        assertTrue(e.getMessage().contains("Failed to download"), e.getMessage());
        assertTrue(e.getMessage().contains(String.valueOf(DATA_VERSION)), e.getMessage());
        assertTrue(listFiles(tempDir).isEmpty(), () -> "Partial download left behind: " + listFiles(tempDir));
    }

    @Test
    void corruptCachedFileIsRemoved() throws IOException {
        Files.writeString(cachedFile(), "{\"blocks\": {");
        RuntimeException e = assertThrows(RuntimeException.class,
            () -> FileRegistries.loadDataFile(tempDir, DATA_VERSION, NO_DOWNLOADS));
        assertTrue(e.getMessage().contains("re-download"), e.getMessage());
        assertFalse(Files.exists(cachedFile()));
    }

    @Test
    void emptyCachedFileIsRemoved() throws IOException {
        Files.writeString(cachedFile(), "");
        assertThrows(RuntimeException.class, () -> FileRegistries.loadDataFile(tempDir, DATA_VERSION, NO_DOWNLOADS));
        assertFalse(Files.exists(cachedFile()));
    }

    private static List<Path> listFiles(Path folder) {
        try (Stream<Path> files = Files.list(folder)) {
            return files.toList();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
}
