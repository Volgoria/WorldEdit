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

package com.sk89q.worldedit.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins how {@link PropertiesConfiguration} reads values and writes back defaults.
 */
class PropertiesConfigurationTest {

    @TempDir
    Path dir;

    private PropertiesConfiguration config;

    @BeforeEach
    void setUp() {
        config = new PropertiesConfiguration(dir.resolve("worldedit.properties"));
    }

    @Test
    void missingIntWritesDefault() {
        assertEquals(7, config.getInt("a", 7));
        assertEquals("7", config.properties.getProperty("a"));
    }

    @Test
    void validIntIsParsedAndKept() {
        config.properties.setProperty("a", "42");
        assertEquals(42, config.getInt("a", 7));
        assertEquals("42", config.properties.getProperty("a"));
    }

    @Test
    void malformedIntIsReplacedByDefault() {
        config.properties.setProperty("a", "nope");
        assertEquals(7, config.getInt("a", 7));
        assertEquals("7", config.properties.getProperty("a"));
    }

    @Test
    void doubleValues() {
        assertEquals(1.5, config.getDouble("d", 1.5));
        assertEquals("1.5", config.properties.getProperty("d"));

        config.properties.setProperty("d", "2.25");
        assertEquals(2.25, config.getDouble("d", 1.5));

        config.properties.setProperty("d", "x");
        assertEquals(1.5, config.getDouble("d", 1.5));
        assertEquals("1.5", config.properties.getProperty("d"));
    }

    @Test
    void booleanValues() {
        assertTrue(config.getBool("b", true));
        assertEquals("true", config.properties.getProperty("b"));

        config.properties.setProperty("b", "1");
        assertTrue(config.getBool("b", false));
        config.properties.setProperty("b", "TRUE");
        assertTrue(config.getBool("b", false));
        config.properties.setProperty("b", "yes");
        assertFalse(config.getBool("b", true));
    }

    @Test
    void stringSets() {
        assertEquals(Set.of("x", "y"), config.getStringSet("s", new String[] { "x", "y" }));
        assertEquals("x,y", config.properties.getProperty("s"));

        config.properties.setProperty("s", " a , b,a ");
        assertEquals(Set.of("a", "b"), config.getStringSet("s", new String[0]));
    }

    @Test
    void intSetsSkipMalformedEntries() {
        assertEquals(Set.of(1, 2), config.getIntSet("i", new int[] { 1, 2 }));
        assertEquals("1,2", config.properties.getProperty("i"));

        config.properties.setProperty("i", "3, x ,4");
        assertEquals(Set.of(3, 4), config.getIntSet("i", new int[0]));
    }
}
