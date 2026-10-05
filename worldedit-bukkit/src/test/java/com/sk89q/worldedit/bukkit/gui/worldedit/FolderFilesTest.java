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

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FolderFilesTest {

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

        List<FolderFiles.Entry> entries = FolderFiles.list(root, EXTENSIONS);
        assertEquals(List.of("builds/house.schem", "builds/old/barn.schem", "castle.SCHEMATIC", "Tower.schem"),
            entries.stream().map(FolderFiles.Entry::relativePath).toList());

        FolderFiles.Entry castle = entries.get(2);
        assertEquals(6, castle.size());
        assertEquals("castle.SCHEMATIC", castle.fileName());
        assertEquals("schematic", castle.extension());
        assertEquals("barn.schem", entries.get(1).fileName());
    }

    @Test
    void missingFolderIsEmpty(@TempDir Path root) throws IOException {
        assertTrue(FolderFiles.list(root.resolve("missing"), EXTENSIONS).isEmpty());
    }

    @Test
    void filterIgnoresCase() {
        List<FolderFiles.Entry> entries = List.of(
            new FolderFiles.Entry("builds/House.schem", 1, 0),
            new FolderFiles.Entry("castle.schem", 1, 0)
        );
        assertEquals(entries, FolderFiles.filter(entries, ""));
        assertEquals(entries, FolderFiles.filter(entries, null));
        assertEquals(List.of(entries.get(0)), FolderFiles.filter(entries, "house"));
        assertEquals(List.of(entries.get(0)), FolderFiles.filter(entries, " BUILDS/ "));
        assertTrue(FolderFiles.filter(entries, "barn").isEmpty());
    }

    @Test
    void symbolicLinksAreNotFollowed(@TempDir Path temp) throws IOException {
        Path root = Files.createDirectories(temp.resolve("images"));
        Path outside = Files.createDirectories(temp.resolve("outside"));
        Files.writeString(outside.resolve("secret.png"), "x");
        Files.writeString(root.resolve("real.png"), "x");
        try {
            Files.createSymbolicLink(root.resolve("link.png"), outside.resolve("secret.png"));
            Files.createSymbolicLink(root.resolve("linked-folder"), outside);
        } catch (UnsupportedOperationException | IOException e) {
            Assumptions.abort("Symbolic links are not supported here: " + e);
        }
        assertEquals(List.of("real.png"), FolderFiles.list(root, Set.of("png")).stream()
            .map(FolderFiles.Entry::relativePath).toList());
    }

    @Test
    void unsafeAndHiddenFilesAreSkipped(@TempDir Path root) throws IOException {
        Files.writeString(root.resolve("ok file.png"), "x");
        Files.writeString(root.resolve(".hidden.png"), "x");
        Files.createDirectories(root.resolve(".git"));
        Files.writeString(root.resolve(".git/inside.png"), "x");
        try {
            Files.writeString(root.resolve("quo\"te.png"), "x");
        } catch (InvalidPathException | IOException _) {
            // Not a valid file name on this file system, nothing to check
        }
        assertEquals(List.of("ok file.png"), FolderFiles.list(root, Set.of("png")).stream()
            .map(FolderFiles.Entry::relativePath).toList());
    }

    @Test
    void safeRelativePaths() {
        assertTrue(FolderFiles.isSafeRelativePath("castle.schem"));
        assertTrue(FolderFiles.isSafeRelativePath("builds/my castle.schem"));
        assertFalse(FolderFiles.isSafeRelativePath(""));
        assertFalse(FolderFiles.isSafeRelativePath("/abs.schem"));
        assertFalse(FolderFiles.isSafeRelativePath("../up.schem"));
        assertFalse(FolderFiles.isSafeRelativePath("a//b.schem"));
        assertFalse(FolderFiles.isSafeRelativePath(".hidden/b.schem"));
        assertFalse(FolderFiles.isSafeRelativePath("quo\"te.schem"));
        assertFalse(FolderFiles.isSafeRelativePath("back\\slash.schem"));
        assertFalse(FolderFiles.isSafeRelativePath("new\nline.schem"));
        assertFalse(FolderFiles.isSafeRelativePath("x".repeat(256)));
    }

    @Test
    void extension() {
        assertEquals("schem", FolderFiles.extension("a.b.SCHEM"));
        assertEquals("", FolderFiles.extension("noext"));
        assertEquals("", FolderFiles.extension(".hidden"));
    }
}
