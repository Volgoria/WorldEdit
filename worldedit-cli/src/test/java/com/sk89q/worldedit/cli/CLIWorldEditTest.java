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

import org.enginehub.linbus.tree.LinCompoundTag;
import org.enginehub.linbus.tree.LinIntTag;
import org.enginehub.linbus.tree.LinTagType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CLIWorldEditTest {

    private static final String DATA_FILE =
        """
        {
          "blocks": {
            "minecraft:air": {"defaultstate": "minecraft:air", "properties": {}},
            "minecraft:stone": {"defaultstate": "minecraft:stone", "properties": {}}
          },
          "items": [],
          "entities": [],
          "biomes": ["minecraft:plains"],
          "itemtags": {},
          "blocktags": {},
          "entitytags": {}
        }
        """;

    @TempDir
    Path tempDir;

    private Path workingDir;
    private ByteArrayOutputStream outBytes;
    private PrintStream out;

    @BeforeEach
    void setUp() {
        workingDir = tempDir.resolve("worldedit");
        outBytes = new ByteArrayOutputStream();
        out = new PrintStream(outBytes, true, StandardCharsets.UTF_8);
    }

    private int launch(String... args) {
        return launchWithInput("", args);
    }

    private int launchWithInput(String stdin, String... args) {
        InputStream input = new ByteArrayInputStream(stdin.getBytes(StandardCharsets.UTF_8));
        return CLIWorldEdit.launch(args, input, out, workingDir);
    }

    /**
     * Create a 3x1x1 air schematic, and pre-seed the data file cache so no download is attempted.
     */
    private Path setUpAirSchematic() throws IOException {
        Path cliData = Files.createDirectories(workingDir.resolve("cli-data"));
        Files.writeString(cliData.resolve(TestSchematics.DATA_VERSION + "_1.json"), DATA_FILE);
        Path schematic = tempDir.resolve("tiny.schem");
        TestSchematics.writeFilled(schematic, 3, "minecraft:air");
        return schematic;
    }

    private static void assertAllStone(Path schematic) throws IOException {
        LinCompoundTag saved = TestSchematics.readSchematicTag(schematic);
        assertEquals(3, saved.getTag("Width", LinTagType.shortTag()).valueAsShort());
        assertEquals(TestSchematics.DATA_VERSION, saved.getTag("DataVersion", LinTagType.intTag()).valueAsInt());
        LinCompoundTag blocks = saved.getTag("Blocks", LinTagType.compoundTag());
        Map<String, ?> palette = blocks.getTag("Palette", LinTagType.compoundTag()).value();
        LinIntTag stoneId = (LinIntTag) palette.get("minecraft:stone");
        assertTrue(stoneId != null, () -> "Stone missing from palette " + palette);
        byte id = (byte) stoneId.valueAsInt();
        assertArrayEquals(new byte[] { id, id, id }, blocks.getTag("Data", LinTagType.byteArrayTag()).value());
    }

    private String output() {
        return outBytes.toString(StandardCharsets.UTF_8);
    }

    @Test
    void helpPrintsUsageWithoutStarting() {
        assertEquals(CLIWorldEdit.EXIT_OK, launch("--help"));
        assertTrue(output().contains("--non-interactive"), output());
        assertFalse(Files.exists(workingDir), "--help should not create the working directory");
    }

    @Test
    void invalidArgumentsPrintUsage() {
        assertEquals(CLIWorldEdit.EXIT_USAGE, launch("--bogus"));
        assertTrue(output().startsWith("Error: "), output());
        assertTrue(output().contains("usage: "), output());
    }

    @Test
    void missingFileFailsCleanly() {
        assertEquals(CLIWorldEdit.EXIT_ERROR, launch("-f", tempDir.resolve("missing.schem").toString()));
        assertFalse(Files.exists(workingDir), "Nothing should be started for a missing file");
    }

    @Test
    void unsupportedFileFailsCleanly() throws IOException {
        Path file = tempDir.resolve("notes.txt");
        Files.writeString(file, "hello");
        assertEquals(CLIWorldEdit.EXIT_ERROR, launch("-f", file.toString()));
        assertFalse(Files.exists(workingDir));
    }

    @Test
    void missingScriptFailsCleanly() throws IOException {
        Path schematic = tempDir.resolve("tiny.schem");
        TestSchematics.writeFilled(schematic, 2, "minecraft:air");
        assertEquals(CLIWorldEdit.EXIT_ERROR,
            launch("-f", schematic.toString(), "-s", tempDir.resolve("missing.txt").toString(), "-n"));
        assertFalse(Files.exists(workingDir));
    }

    @Test
    void nonInteractiveScriptEditsAndSavesSchematic() throws IOException {
        Path schematic = setUpAirSchematic();
        Path script = tempDir.resolve("fill.txt");
        Files.writeString(script,
            """
            # Fill the whole schematic with stone
            cli selectworld

            //set minecraft:stone
            """);

        assertEquals(CLIWorldEdit.EXIT_OK,
            launch("--file", schematic.toString(), "--script", script.toString(), "--non-interactive"));

        assertAllStone(schematic);
    }

    @Test
    void unknownScriptCommandFailsButKeepsEdits() throws IOException {
        Path schematic = setUpAirSchematic();
        Path script = tempDir.resolve("fill.txt");
        Files.writeString(script,
            """
            /cli selectworld
            //set minecraft:stone
            /not-a-real-command
            """);

        assertEquals(CLIWorldEdit.EXIT_ERROR,
            launch("-f", schematic.toString(), "-s", script.toString(), "-n"));

        assertAllStone(schematic);
    }

    @Test
    void interactiveCommandsStopAtStop() throws IOException {
        Path schematic = setUpAirSchematic();

        int exitCode = launchWithInput(
            """
            /cli selectworld
            //set minecraft:stone
            stop
            //set minecraft:air
            """, "-f", schematic.toString());

        assertEquals(CLIWorldEdit.EXIT_OK, exitCode);
        assertAllStone(schematic);
    }
}
