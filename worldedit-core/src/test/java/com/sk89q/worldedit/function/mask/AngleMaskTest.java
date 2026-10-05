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

package com.sk89q.worldedit.function.mask;

import com.sk89q.worldedit.math.BlockVector3;
import org.junit.jupiter.api.Test;

import java.util.function.IntBinaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AngleMaskTest {

    private static final double DELTA = 1e-6;

    /**
     * A terrain mask for a height map: everything at or below the height is terrain.
     */
    private static Mask heightMap(IntBinaryOperator height) {
        return position -> position.y() <= height.applyAsInt(position.x(), position.z());
    }

    @Test
    void flatTerrainIsZeroDegrees() {
        Mask terrain = heightMap((_, _) -> 10);
        AngleMask flat = new AngleMask(terrain, 0, 0, 1);
        assertEquals(0, flat.getAngle(BlockVector3.at(0, 10, 0)), DELTA);
        assertTrue(flat.test(BlockVector3.at(0, 10, 0)));
        // a block below the surface uses the surface of its column
        assertTrue(flat.test(BlockVector3.at(0, 5, 0)));
        // air never matches
        assertFalse(flat.test(BlockVector3.at(0, 11, 0)));
    }

    @Test
    void staircaseIsFortyFiveDegrees() {
        // one block of height per block along X
        Mask terrain = heightMap((x, _) -> x);
        AngleMask mask = new AngleMask(terrain, 40, 50, 1);
        assertEquals(45, mask.getAngle(BlockVector3.at(3, 3, 0)), DELTA);
        assertTrue(mask.test(BlockVector3.at(3, 3, 0)));
        assertFalse(new AngleMask(terrain, 0, 30, 1).test(BlockVector3.at(3, 3, 0)));
    }

    @Test
    void distanceRefinesResolution() {
        // one block of height every two blocks along Z
        Mask terrain = heightMap((_, z) -> Math.floorDiv(z, 2));
        AngleMask mask = new AngleMask(terrain, 0, 90, 2);
        assertEquals(Math.toDegrees(Math.atan(0.5)), mask.getAngle(BlockVector3.at(0, 2, 4)), DELTA);
    }

    @Test
    void cliffIsNinetyDegrees() {
        // a 100 block high wall at x >= 0
        Mask terrain = heightMap((x, _) -> x >= 0 ? 100 : 0);
        AngleMask steep = new AngleMask(terrain, 80, 90, 1);
        assertEquals(90, steep.getAngle(BlockVector3.at(0, 100, 0)), DELTA);
        assertEquals(90, steep.getAngle(BlockVector3.at(-1, 0, 0)), DELTA);
        assertTrue(steep.test(BlockVector3.at(0, 100, 0)));
        // far away from the wall it is flat again
        assertFalse(steep.test(BlockVector3.at(5, 100, 0)));
        assertFalse(steep.test(BlockVector3.at(-5, 0, 0)));
    }

    @Test
    void overhangUsesNearestSurface() {
        // a floating layer at y = 20 above flat ground at y = 0
        Mask terrain = position -> position.y() <= 0 || position.y() == 20;
        AngleMask flat = new AngleMask(terrain, 0, 0, 1);
        assertTrue(flat.test(BlockVector3.at(0, 0, 0)));
        assertTrue(flat.test(BlockVector3.at(0, 20, 0)));
    }

    @Test
    void rejectsInvalidArguments() {
        Mask terrain = heightMap((_, _) -> 0);
        assertThrows(IllegalArgumentException.class, () -> new AngleMask(terrain, -1, 10, 1));
        assertThrows(IllegalArgumentException.class, () -> new AngleMask(terrain, 0, 91, 1));
        assertThrows(IllegalArgumentException.class, () -> new AngleMask(terrain, 50, 40, 1));
        assertThrows(IllegalArgumentException.class, () -> new AngleMask(terrain, 0, 10, 0));
    }
}
