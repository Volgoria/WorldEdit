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

import com.sk89q.worldedit.session.SessionKey;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CLICommandSenderTest {

    private static final String RESET = "\u001B[0m";

    private Logger logger;
    private CLICommandSender sender;

    @BeforeEach
    void setUp() {
        logger = mock(Logger.class);
        sender = new CLICommandSender(logger);
    }

    @Test
    void requiresLogger() {
        assertThrows(NullPointerException.class, () -> new CLICommandSender(null));
    }

    @Test
    @SuppressWarnings("deprecation")
    void printSplitsLinesAndColours() {
        sender.print("first\nsecond");
        InOrder order = inOrder(logger);
        order.verify(logger).info("\u001B[35mfirst" + RESET);
        order.verify(logger).info("\u001B[35msecond" + RESET);
    }

    @Test
    @SuppressWarnings("deprecation")
    void printErrorIsRed() {
        sender.printError("bad");
        verify(logger).error("\u001B[31mbad" + RESET);
    }

    @Test
    @SuppressWarnings("deprecation")
    void printDebugIsGreen() {
        sender.printDebug("details");
        verify(logger).debug("\u001B[32mdetails" + RESET);
    }

    @Test
    @SuppressWarnings("deprecation")
    void printRawIsUncoloured() {
        sender.printRaw("a\nb");
        InOrder order = inOrder(logger);
        order.verify(logger).info("a");
        order.verify(logger).info("b");
    }

    @Test
    void consoleHasAllPermissions() {
        assertTrue(sender.hasPermission("worldedit.anything"));
        sender.checkPermission("worldedit.anything");
        assertTrue(sender.canDestroyBedrock());
        assertArrayEquals(new String[0], sender.getGroups());
        assertFalse(sender.isPlayer());
    }

    @Test
    void stableIdentity() {
        assertEquals("Console", sender.getName());
        CLICommandSender other = new CLICommandSender(logger);
        assertEquals(sender.getUniqueId(), other.getUniqueId());

        SessionKey key = sender.getSessionKey();
        assertEquals(sender.getUniqueId(), key.getUniqueId());
        assertEquals("Console", key.getName());
        assertTrue(key.isActive());
        assertTrue(key.isPersistent());
    }
}
