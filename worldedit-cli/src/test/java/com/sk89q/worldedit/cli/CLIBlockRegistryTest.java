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

import com.sk89q.worldedit.registry.state.BooleanProperty;
import com.sk89q.worldedit.registry.state.DirectionalProperty;
import com.sk89q.worldedit.registry.state.EnumProperty;
import com.sk89q.worldedit.registry.state.IntegerProperty;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.util.Direction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CLIBlockRegistryTest {

    @Test
    void integerProperty() {
        Property<?> property = CLIBlockRegistry.createProperty("int", "age", List.of("0", "1", "2"));
        assertInstanceOf(IntegerProperty.class, property);
        assertEquals("age", property.name());
        assertEquals(List.of(0, 1, 2), property.values());
    }

    @Test
    void booleanProperty() {
        Property<?> property = CLIBlockRegistry.createProperty("bool", "lit", List.of("true", "false"));
        assertInstanceOf(BooleanProperty.class, property);
        assertEquals(List.of(true, false), property.values());
    }

    @Test
    void enumProperty() {
        Property<?> property = CLIBlockRegistry.createProperty("enum", "half", List.of("top", "bottom"));
        assertInstanceOf(EnumProperty.class, property);
        assertEquals(List.of("top", "bottom"), property.values());
    }

    @Test
    void directionalProperty() {
        Property<?> property = CLIBlockRegistry.createProperty("direction", "facing", List.of("north", "up"));
        assertInstanceOf(DirectionalProperty.class, property);
        assertEquals(List.of(Direction.NORTH, Direction.UP), property.values());
    }

    @Test
    void unknownPropertyType() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> CLIBlockRegistry.createProperty("float", "weird", List.of("0.5")));
        assertTrue(e.getMessage().contains("float") && e.getMessage().contains("weird"), e.getMessage());
    }
}
