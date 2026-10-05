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

package com.sk89q.worldedit.cli;

import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.internal.Constants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CLIFilesTest {

    @TempDir
    Path tempDir;

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "//set stone|//set stone",
        "/cli selectworld|/cli selectworld",
        "cli selectworld|/cli selectworld",
        "'  //pos1  '|//pos1",
        "\uFEFF//set stone|//set stone",
        "stop|stop",
        "/stop|/stop",
        "'# a comment'|'# a comment'",
    })
    void normalizeCommand(String input, String expected) {
        assertEquals(expected, CLIFiles.normalizeCommand(input));
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "   ", "\t", "/", " / " })
    void normalizeBlankCommand(String input) {
        assertNull(CLIFiles.normalizeCommand(input));
    }

    @Test
    void stopCommands() {
        assertTrue(CLIFiles.isStopCommand("stop"));
        assertTrue(CLIFiles.isStopCommand("/stop"));
        assertFalse(CLIFiles.isStopCommand("//stop"));
        assertFalse(CLIFiles.isStopCommand("/stopwatch"));
    }

    @Test
    void readScriptSkipsBlankAndCommentLines() throws IOException {
        Path script = tempDir.resolve("script.txt");
        // Starts with a byte order mark, as some editors write
        Files.writeString(script,
            """
            \uFEFF# Build a floor

            cli selectworld
            \s\s
              # indented comment
            //set stone
            stop
            """, StandardCharsets.UTF_8);
        assertEquals(List.of("/cli selectworld", "//set stone", "stop"), CLIFiles.readScript(script));
    }

    @Test
    void readMissingScript() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> CLIFiles.readScript(tempDir.resolve("nope.txt")));
        assertTrue(e.getMessage().contains("script file"), e.getMessage());
        assertTrue(e.getMessage().contains("does not exist"), e.getMessage());
    }

    @Test
    void detectFormatOfMissingFile() {
        Path missing = tempDir.resolve("missing.schem");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> CLIFiles.detectFormat(missing));
        assertEquals("The file '" + missing + "' does not exist.", e.getMessage());
    }

    @Test
    void detectFormatOfDirectory() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> CLIFiles.detectFormat(tempDir));
        assertTrue(e.getMessage().contains("is a directory"), e.getMessage());
    }

    @Test
    void detectFormatOfLevelDat() throws IOException {
        Path levelDat = Files.createFile(tempDir.resolve("level.dat"));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> CLIFiles.detectFormat(levelDat));
        assertTrue(e.getMessage().contains("level.dat"), e.getMessage());
    }

    @Test
    void detectFormatOfUnsupportedFile() throws IOException {
        Path notASchematic = tempDir.resolve("fake.schem");
        Files.writeString(notASchematic, "definitely not NBT");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> CLIFiles.detectFormat(notASchematic));
        assertTrue(e.getMessage().contains("not in a supported schematic format"), e.getMessage());
        assertTrue(e.getMessage().contains("schem"), "Should list supported extensions: " + e.getMessage());
    }

    @Test
    void detectAndReadSpongeV3Schematic() throws IOException {
        Path schematic = tempDir.resolve("tiny.schem");
        TestSchematics.writeFilled(schematic, 2, "minecraft:air");

        assertSame(BuiltInClipboardFormat.SPONGE_V3_SCHEMATIC, CLIFiles.detectFormat(schematic));
        assertEquals(TestSchematics.DATA_VERSION,
            CLIFiles.readDataVersion(BuiltInClipboardFormat.SPONGE_V3_SCHEMATIC, schematic));
    }

    @Test
    void mcEditSchematicsUseFixedDataVersion() throws IOException {
        // MCEdit schematics don't record a data version, so the file isn't even read
        assertEquals(Constants.DATA_VERSION_MC_1_13_2,
            CLIFiles.readDataVersion(BuiltInClipboardFormat.MCEDIT_SCHEMATIC, tempDir.resolve("unused.schematic")));
    }

    @Test
    void openableExtensionsIncludeSchematicsAndLevelDat() {
        List<String> extensions = Arrays.asList(CLIFiles.openableExtensions());
        assertTrue(extensions.contains("schem"), extensions::toString);
        assertTrue(extensions.contains("dat"), extensions::toString);
    }
}
