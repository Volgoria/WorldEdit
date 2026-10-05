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

package com.sk89q.worldedit.cli.schematic;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.SideEffectSet;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClipboardWorldTest {

    @TempDir
    Path tempDir;

    private Path file;
    private Clipboard clipboard;
    private ClipboardFormat format;
    private ClipboardWriter writer;
    private ClipboardWorld world;

    @BeforeEach
    void setUp() throws IOException {
        file = tempDir.resolve("My House.schem");
        Files.writeString(file, "original", StandardCharsets.UTF_8);
        clipboard = mock(Clipboard.class);
        format = mock(ClipboardFormat.class);
        writer = mock(ClipboardWriter.class);
        when(format.supportsWriting()).thenReturn(true);
        // The mocked writer writes a marker to whatever stream it's given
        when(format.getWriter(any())).thenAnswer(invocation -> {
            OutputStream out = invocation.getArgument(0);
            doAnswer(_ -> {
                out.write("saved".getBytes(StandardCharsets.UTF_8));
                return null;
            }).when(writer).write(any());
            doAnswer(_ -> {
                out.close();
                return null;
            }).when(writer).close();
            return writer;
        });
        world = new ClipboardWorld(file.toFile(), format, clipboard, "My House.schem");
    }

    @Test
    void nameAndId() {
        assertEquals("My House.schem", world.getName());
        assertEquals("my_house.schem", world.id());
    }

    @Test
    void delegatesGeometryToClipboard() {
        BlockVector3 min = BlockVector3.at(1, 2, 3);
        BlockVector3 max = BlockVector3.at(4, 5, 6);
        when(clipboard.getMinimumPoint()).thenReturn(min);
        when(clipboard.getMaximumPoint()).thenReturn(max);
        when(clipboard.getOrigin()).thenReturn(min);

        assertSame(min, world.getMinimumPoint());
        assertSame(max, world.getMaximumPoint());
        assertSame(min, world.getSpawnPosition());
    }

    @Test
    void startsClean() {
        assertFalse(world.isDirty());
    }

    @Test
    void successfulBlockChangeMarksDirty() throws WorldEditException {
        BlockState block = mock(BlockState.class);
        when(clipboard.setBlock(BlockVector3.ZERO, block)).thenReturn(true);
        assertTrue(world.setBlock(BlockVector3.ZERO, block, SideEffectSet.none()));
        assertTrue(world.isDirty());
    }

    @Test
    void failedBlockChangeStaysClean() throws WorldEditException {
        BlockState block = mock(BlockState.class);
        when(clipboard.setBlock(BlockVector3.ZERO, block)).thenReturn(false);
        assertFalse(world.setBlock(BlockVector3.ZERO, block, SideEffectSet.none()));
        assertFalse(world.isDirty());
    }

    @Test
    void biomeChangeMarksDirty() {
        BiomeType biome = mock(BiomeType.class);
        when(clipboard.setBiome(BlockVector3.ZERO, biome)).thenReturn(true);
        assertTrue(world.setBiome(BlockVector3.ZERO, biome));
        assertTrue(world.isDirty());
    }

    @Test
    void originChangeMarksDirty() {
        world.setOrigin(BlockVector3.ONE);
        verify(clipboard).setOrigin(BlockVector3.ONE);
        assertTrue(world.isDirty());
    }

    @Test
    void cleanWorldIsNotSaved() throws IOException {
        world.save(false);
        verify(format, never()).getWriter(any());
        assertEquals("original", Files.readString(file));
    }

    @Test
    void dirtyWorldIsSaved() throws IOException {
        world.setDirty(true);
        world.save(false);
        verify(writer).write(world);
        assertEquals("saved", Files.readString(file));
        assertFalse(world.isDirty());
        assertEquals(List.of(file), listFiles());
    }

    @Test
    void forcedSaveWritesCleanWorld() throws IOException {
        world.save(true);
        assertEquals("saved", Files.readString(file));
    }

    @Test
    void saveCreatesMissingFile() throws IOException {
        Files.delete(file);
        world.save(true);
        assertEquals("saved", Files.readString(file));
    }

    @Test
    void failedSaveKeepsOriginalFileAndDirtyFlag() throws IOException {
        ClipboardFormat failingFormat = mock(ClipboardFormat.class);
        ClipboardWriter failingWriter = mock(ClipboardWriter.class);
        when(failingFormat.supportsWriting()).thenReturn(true);
        when(failingFormat.getWriter(any())).thenReturn(failingWriter);
        doThrow(new IOException("disk full")).when(failingWriter).write(any());
        world = new ClipboardWorld(file.toFile(), failingFormat, clipboard, "My House.schem");
        world.setDirty(true);

        assertThrows(IOException.class, () -> world.save(false));

        assertEquals("original", Files.readString(file), "Original schematic must not be truncated");
        assertTrue(world.isDirty(), "Unsaved changes must still be marked dirty");
        assertEquals(List.of(file), listFiles(), "Temporary file must be cleaned up");
    }

    @Test
    void loadOnlyFormatRefusesToSave() throws IOException {
        ClipboardFormat loadOnly = mock(ClipboardFormat.class);
        when(loadOnly.supportsWriting()).thenReturn(false);
        when(loadOnly.getName()).thenReturn("mcedit");
        world = new ClipboardWorld(file.toFile(), loadOnly, clipboard, "My House.schem");
        assertFalse(world.canSave());

        // nothing to save yet
        world.save(false);

        world.setDirty(true);
        IOException e = assertThrows(IOException.class, () -> world.save(false));
        assertTrue(e.getMessage().contains("mcedit"), e.getMessage());
        verify(loadOnly, never()).getWriter(any());
        assertEquals("original", Files.readString(file));
        assertTrue(world.isDirty());
    }

    private List<Path> listFiles() throws IOException {
        try (Stream<Path> files = Files.list(tempDir)) {
            return files.toList();
        }
    }
}
