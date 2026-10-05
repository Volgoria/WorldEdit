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

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.util.Direction.Flag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Direction")
public class DirectionTest {

    @ParameterizedTest
    @EnumSource(Direction.class)
    @DisplayName("has a unit vector and exactly one category")
    void unitVectorAndSingleCategory(Direction direction) {
        assertEquals(1, direction.toVector().length(), 1e-9);
        int categories = (direction.isCardinal() ? 1 : 0)
            + (direction.isOrdinal() ? 1 : 0)
            + (direction.isSecondaryOrdinal() ? 1 : 0)
            + (direction.isUpright() ? 1 : 0);
        assertEquals(1, categories);
        assertEquals(direction.isUpright(), direction.toVector().y() != 0);
    }

    @ParameterizedTest
    @EnumSource(Direction.class)
    @DisplayName("finds itself as the closest direction to its own vector")
    void findClosestToSelf(Direction direction) {
        assertEquals(direction, Direction.findClosest(direction.toVector(), Flag.ALL));
    }

    @ParameterizedTest
    @EnumSource(Direction.class)
    @DisplayName("round-trips through the rotation index when it has one")
    void rotationIndexRoundTrip(Direction direction) {
        OptionalInt index = direction.toRotationIndex();
        assertEquals(!direction.isUpright(), index.isPresent());
        index.ifPresent(i -> assertEquals(Optional.of(direction), Direction.fromRotationIndex(i)));
    }

    @Test
    void rotationIndicesAreSequentialClockwise() {
        for (int i = 0; i < 16; i++) {
            Direction current = Direction.fromRotationIndex(i).orElseThrow();
            Direction next = Direction.fromRotationIndex((i + 1) % 16).orElseThrow();
            // each step is 22.5 degrees
            double angle = Math.toDegrees(Math.acos(current.toVector().dot(next.toVector())));
            assertEquals(22.5, angle, 1e-9, () -> current + " -> " + next);
            // and the rotation is clockwise when viewed from above (positive y)
            assertTrue(current.toVector().cross(next.toVector()).y() < 0, () -> current + " -> " + next);
        }
        assertEquals(Optional.empty(), Direction.fromRotationIndex(-1));
        assertEquals(Optional.empty(), Direction.fromRotationIndex(16));
    }

    @Test
    void blockVectors() {
        assertEquals(BlockVector3.at(0, 0, -1), Direction.NORTH.toBlockVector());
        assertEquals(BlockVector3.at(1, 0, 0), Direction.EAST.toBlockVector());
        assertEquals(BlockVector3.at(0, 0, 1), Direction.SOUTH.toBlockVector());
        assertEquals(BlockVector3.at(-1, 0, 0), Direction.WEST.toBlockVector());
        assertEquals(BlockVector3.at(0, 1, 0), Direction.UP.toBlockVector());
        assertEquals(BlockVector3.at(0, -1, 0), Direction.DOWN.toBlockVector());
        assertEquals(BlockVector3.at(1, 0, -1), Direction.NORTHEAST.toBlockVector());
        assertEquals(BlockVector3.at(-1, 0, 1), Direction.SOUTHWEST.toBlockVector());
    }

    @Test
    void findClosestRespectsFlags() {
        Vector3 northEastish = Vector3.at(1, 0, -1.2);
        assertEquals(Direction.NORTHEAST, Direction.findClosest(northEastish, Flag.CARDINAL | Flag.ORDINAL));
        assertEquals(Direction.NORTH, Direction.findClosest(northEastish, Flag.CARDINAL));
        assertEquals(Direction.NORTH_NORTHEAST, Direction.findClosest(Vector3.at(0.4, 0, -1), Flag.ALL));
    }

    @Test
    void findClosestIgnoresYWithoutUprightFlag() {
        Vector3 mostlyUp = Vector3.at(0.1, 10, 0);
        assertEquals(Direction.UP, Direction.findClosest(mostlyUp, Flag.ALL));
        assertEquals(Direction.EAST, Direction.findClosest(mostlyUp, Flag.CARDINAL));
        assertEquals(Direction.DOWN, Direction.findClosest(Vector3.at(0, -1, 0), Flag.UPRIGHT));
    }

    @Test
    void findClosestWithNoFlagsReturnsNull() {
        assertNull(Direction.findClosest(Vector3.UNIT_X, 0));
    }

    @Test
    void valuesOfFiltersByFlags() {
        assertEquals(List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST),
            Direction.valuesOf(Flag.CARDINAL));
        assertEquals(List.of(Direction.UP, Direction.DOWN), Direction.valuesOf(Flag.UPRIGHT));
        assertEquals(6, Direction.valuesOf(Flag.CARDINAL | Flag.UPRIGHT).size());
        assertEquals(EnumSet.allOf(Direction.class), EnumSet.copyOf(Direction.valuesOf(Flag.ALL)));
        assertTrue(Direction.valuesOf(0).isEmpty());
        assertFalse(Direction.valuesOf(Flag.ORDINAL).contains(Direction.NORTH));
    }
}
