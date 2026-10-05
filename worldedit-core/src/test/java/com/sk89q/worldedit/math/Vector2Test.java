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

package com.sk89q.worldedit.math;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("2D vectors")
public class Vector2Test {

    private static final double EPSILON = 1e-9;

    @Test
    void factoryReturnsSharedConstants() {
        assertSame(Vector2.ZERO, Vector2.at(0, 0));
        assertSame(Vector2.ONE, Vector2.at(1, 1));
        assertSame(BlockVector2.ZERO, BlockVector2.at(0, 0));
        assertSame(BlockVector2.ONE, BlockVector2.at(1, 1));
        assertEquals(new Vector2(0.5, 0), Vector2.at(0.5, 0));
    }

    @Test
    void arithmetic() {
        Vector2 a = Vector2.at(3, -4);
        Vector2 b = Vector2.at(0.5, 2);
        assertEquals(Vector2.at(3.5, -2), a.add(b));
        assertEquals(Vector2.at(2.5, -6), a.subtract(b));
        assertEquals(Vector2.at(1.5, -8), a.multiply(b));
        assertEquals(Vector2.at(6, -2), a.divide(b));
        assertEquals(Vector2.at(-6, 8), a.multiply(-2));
        assertEquals(Vector2.at(4, 0), a.add(b, b));
        assertEquals(5, a.length(), EPSILON);
        assertEquals(25, a.lengthSq(), EPSILON);
        assertEquals(1, a.normalize().length(), EPSILON);
        assertEquals(-6.5, a.dot(b), EPSILON);
        assertEquals(5, Vector2.ZERO.distance(a), EPSILON);
    }

    @Test
    void roundingAndAbs() {
        Vector2 v = Vector2.at(-1.5, 2.5);
        assertEquals(Vector2.at(-2, 2), v.floor());
        assertEquals(Vector2.at(-1, 3), v.ceil());
        assertEquals(Vector2.at(-1, 3), v.round());
        assertEquals(Vector2.at(1.5, 2.5), v.abs());
        assertEquals(BlockVector2.at(-2, 2), v.toBlockPoint());
    }

    @Test
    void minMaxAndContainment() {
        Vector2 a = Vector2.at(-1, 5);
        Vector2 b = Vector2.at(2, -6);
        assertEquals(Vector2.at(-1, -6), a.getMinimum(b));
        assertEquals(Vector2.at(2, 5), a.getMaximum(b));
        assertTrue(Vector2.at(0, 0).containedWithin(a.getMinimum(b), a.getMaximum(b)));
        assertFalse(Vector2.at(3, 0).containedWithin(a.getMinimum(b), a.getMaximum(b)));
    }

    @Test
    void conversionsTo3D() {
        Vector2 v = Vector2.at(1.5, -2);
        assertEquals(Vector3.at(1.5, 0, -2), v.toVector3());
        assertEquals(Vector3.at(1.5, 64, -2), v.toVector3(64));
        BlockVector2 b = BlockVector2.at(3, -4);
        assertEquals(BlockVector3.at(3, 0, -4), b.toBlockVector3());
        assertEquals(BlockVector3.at(3, 70, -4), b.toBlockVector3(70));
        assertEquals(Vector2.at(3, -4), b.toVector2());
    }

    @Test
    void transform2D() {
        Vector2 rotated = Vector2.at(1, 0).transform2D(90, 0, 0, 0, 0);
        assertEquals(0, rotated.x(), EPSILON);
        assertEquals(1, rotated.z(), EPSILON);

        BlockVector2 blockRotated = BlockVector2.at(3, 1).transform2D(180, 1, 1, 10, 0);
        assertEquals(BlockVector2.at(9, 1), blockRotated);
    }

    @Test
    @Disabled("Bug: transform2D uses Math.cos/sin(Math.toRadians(angle)) and then floors, so residue such as "
        + "cos(270deg) = -1.8e-16 becomes an off-by-one. Fix: use MathUtils.dCos/dSin as AffineTransform does.")
    void blockTransform2DIsExactForRightAngles() {
        // floating point residue from Math.cos/sin must not be floored into an off-by-one
        assertEquals(BlockVector2.at(0, 2), BlockVector2.at(2, 0).transform2D(90, 0, 0, 0, 0));
        assertEquals(BlockVector2.at(-2, 0), BlockVector2.at(2, 0).transform2D(180, 0, 0, 0, 0));
        assertEquals(BlockVector2.at(0, -2), BlockVector2.at(2, 0).transform2D(270, 0, 0, 0, 0));
        assertEquals(BlockVector2.at(5, 3), BlockVector2.at(3, 3).transform2D(-90, 3, 1, 0, 0));
    }

    @Test
    void blockVectorFloorsDoubles() {
        assertEquals(BlockVector2.at(-1, 0), BlockVector2.at(-0.1, 0.9));
        assertEquals(BlockVector2.at(-2, 1), BlockVector2.at(-1.5, 1.5));
    }

    @Test
    void blockVectorArithmetic() {
        BlockVector2 a = BlockVector2.at(7, -9);
        assertEquals(BlockVector2.at(8, -8), a.add(BlockVector2.ONE));
        assertEquals(BlockVector2.at(14, -18), a.multiply(2));
        // integer division truncates towards zero
        assertEquals(BlockVector2.at(3, -4), a.divide(2));
        // shifting rounds towards negative infinity, matching chunk coordinates
        assertEquals(BlockVector2.at(0, -1), a.shr(4));
        assertEquals(130, a.lengthSq());
        assertEquals(-63, a.dot(BlockVector2.at(-9, 0)));
        assertEquals(BlockVector2.at(7, 9), a.abs());
        // normalizing a block vector floors the unit vector's components
        assertEquals(BlockVector2.at(0, -1), BlockVector2.at(5, -5).normalize());
    }

    @Test
    void gridArrangementComparatorSortsRowsByZ() {
        List<BlockVector2> list = new ArrayList<>(List.of(
            BlockVector2.at(1, 1), BlockVector2.at(0, 1), BlockVector2.at(1, 0), BlockVector2.at(0, 0)
        ));
        list.sort(BlockVector2.COMPARING_GRID_ARRANGEMENT);
        assertEquals(List.of(
            BlockVector2.at(0, 0), BlockVector2.at(1, 0), BlockVector2.at(0, 1), BlockVector2.at(1, 1)
        ), list);
    }
}
