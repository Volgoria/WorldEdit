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

import org.apache.commons.cli.ParseException;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CLIArgumentsTest {

    @Test
    void noArguments() throws ParseException {
        CLIArguments args = CLIArguments.parse(new String[0]);
        assertEquals(new CLIArguments(false, null, null, false), args);
    }

    @Test
    void shortOptions() throws ParseException {
        CLIArguments args = CLIArguments.parse(new String[] { "-f", "house.schem", "-s", "build.txt", "-n" });
        assertEquals(new CLIArguments(false, Paths.get("house.schem"), Paths.get("build.txt"), true), args);
    }

    @Test
    void longOptions() throws ParseException {
        CLIArguments args = CLIArguments.parse(new String[] {
            "--file", "house.schem", "--script", "build.txt", "--non-interactive",
        });
        assertEquals(new CLIArguments(false, Paths.get("house.schem"), Paths.get("build.txt"), true), args);
    }

    @Test
    void scriptWithoutNonInteractiveKeepsReadingInput() throws ParseException {
        CLIArguments args = CLIArguments.parse(new String[] { "-f", "a.schem", "-s", "b.txt" });
        assertFalse(args.nonInteractive());
    }

    @Test
    void helpWinsOverEverythingElse() throws ParseException {
        assertTrue(CLIArguments.parse(new String[] { "-h" }).help());
        CLIArguments args = CLIArguments.parse(new String[] { "--file", "a.schem", "--help", "-n" });
        assertTrue(args.help());
        assertNull(args.file());
    }

    @Test
    void nonInteractiveRequiresScript() {
        ParseException e = assertThrows(ParseException.class,
            () -> CLIArguments.parse(new String[] { "-f", "a.schem", "-n" }));
        assertTrue(e.getMessage().contains("--script"), e.getMessage());
    }

    @Test
    void unknownOptionIsRejected() {
        assertThrows(ParseException.class, () -> CLIArguments.parse(new String[] { "--frobnicate" }));
    }

    @Test
    void missingOptionValueIsRejected() {
        assertThrows(ParseException.class, () -> CLIArguments.parse(new String[] { "-f" }));
    }

    @Test
    void strayArgumentsAreRejected() {
        ParseException e = assertThrows(ParseException.class,
            () -> CLIArguments.parse(new String[] { "house.schem" }));
        assertTrue(e.getMessage().contains("house.schem"), e.getMessage());
    }

    @Test
    void blankPathIsRejected() {
        assertThrows(ParseException.class, () -> CLIArguments.parse(new String[] { "-f", " " }));
    }

    @Test
    void usageListsAllOptions() {
        String usage = CLIArguments.usage();
        assertTrue(usage.contains("usage: " + CLIArguments.COMMAND_NAME), usage);
        for (String option : new String[] { "--file", "--script", "--non-interactive", "--help" }) {
            assertTrue(usage.contains(option), () -> "Missing " + option + " in:\n" + usage);
        }
    }
}
