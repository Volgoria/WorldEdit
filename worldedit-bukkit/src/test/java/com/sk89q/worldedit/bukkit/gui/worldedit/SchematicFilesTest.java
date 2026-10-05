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

package com.sk89q.worldedit.bukkit.gui.worldedit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchematicFilesTest {

    private static final Set<String> EXTENSIONS = Set.of("schem", "schematic");

    @Test
    void listsMatchingFilesRecursivelySorted(@TempDir Path root) throws IOException {
        Files.writeString(root.resolve("Tower.schem"), "abc");
        Files.writeString(root.resolve("castle.SCHEMATIC"), "abcdef");
        Files.writeString(root.resolve("notes.txt"), "ignored");
        Files.writeString(root.resolve("noextension"), "ignored");
        Files.createDirectories(root.resolve("builds/old"));
        Files.writeString(root.resolve("builds/house.schem"), "x");
        Files.writeString(root.resolve("builds/old/barn.schem"), "x");
        Files.createDirectories(root.resolve("folder.schem"));

        List<SchematicFiles.Entry> entries = SchematicFiles.list(root, EXTENSIONS);
        assertEquals(List.of("builds/house.schem", "builds/old/barn.schem", "castle.SCHEMATIC", "Tower.schem"),
            entries.stream().map(SchematicFiles.Entry::relativePath).toList());

        SchematicFiles.Entry castle = entries.get(2);
        assertEquals(6, castle.size());
        assertEquals("castle.SCHEMATIC", castle.fileName());
        assertEquals("schematic", castle.extension());
        assertEquals("barn.schem", entries.get(1).fileName());
    }

    @Test
    void missingFolderIsEmpty(@TempDir Path root) throws IOException {
        assertTrue(SchematicFiles.list(root.resolve("missing"), EXTENSIONS).isEmpty());
    }

    @Test
    void filterIgnoresCase() {
        List<SchematicFiles.Entry> entries = List.of(
            new SchematicFiles.Entry("builds/House.schem", 1, 0),
            new SchematicFiles.Entry("castle.schem", 1, 0)
        );
        assertEquals(entries, SchematicFiles.filter(entries, ""));
        assertEquals(entries, SchematicFiles.filter(entries, null));
        assertEquals(List.of(entries.get(0)), SchematicFiles.filter(entries, "house"));
        assertEquals(List.of(entries.get(0)), SchematicFiles.filter(entries, " BUILDS/ "));
        assertTrue(SchematicFiles.filter(entries, "barn").isEmpty());
    }

    @Test
    void extension() {
        assertEquals("schem", SchematicFiles.extension("a.b.SCHEM"));
        assertEquals("", SchematicFiles.extension("noext"));
        assertEquals("", SchematicFiles.extension(".hidden"));
    }
}
