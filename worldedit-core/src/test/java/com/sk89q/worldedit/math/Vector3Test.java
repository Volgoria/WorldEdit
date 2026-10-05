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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("A 3D vector")
public class Vector3Test {

    private static final double EPSILON = 1e-9;

    @Test
    void factoryReturnsSharedConstants() {
        assertSame(Vector3.ZERO, Vector3.at(0, 0, 0));
        assertSame(Vector3.ONE, Vector3.at(1, 1, 1));
        assertEquals(Vector3.at(0, 0.5, 0), new Vector3(0, 0.5, 0));
        assertEquals(Vector3.at(1, 1.5, 1), new Vector3(1, 1.5, 1));
    }

    @Test
    void arithmetic() {
        Vector3 a = Vector3.at(1, 2, 3);
        Vector3 b = Vector3.at(-4, 0.5, 10);
        assertEquals(Vector3.at(-3, 2.5, 13), a.add(b));
        assertEquals(Vector3.at(5, 1.5, -7), a.subtract(b));
        assertEquals(Vector3.at(-4, 1, 30), a.multiply(b));
        assertEquals(Vector3.at(2, 4, 6), a.multiply(2));
        assertEquals(Vector3.at(0.5, 1, 1.5), a.divide(2));
        assertEquals(Vector3.at(-0.25, 4, 0.3), a.divide(b));
        assertEquals(Vector3.at(-2, 4.5, 16), a.add(b, a, Vector3.ZERO).add(Vector3.ZERO));
        assertEquals(Vector3.at(4, -0.5, -10), a.subtract(a, b));
        assertEquals(Vector3.at(-4, 1, 30), a.multiply(b, Vector3.ONE));
        assertEquals(Vector3.at(2, 3, 4), a.add(1, 1, 1));
        assertEquals(Vector3.at(0, 1, 2), a.subtract(1, 1, 1));
    }

    @Test
    void lengthAndDistance() {
        Vector3 v = Vector3.at(2, 3, 6);
        assertEquals(7, v.length(), EPSILON);
        assertEquals(49, v.lengthSq(), EPSILON);
        assertEquals(7, Vector3.ZERO.distance(v), EPSILON);
        assertEquals(49, v.distanceSq(Vector3.ZERO), EPSILON);
        assertEquals(1, v.normalize().length(), EPSILON);
        assertEquals(Vector3.at(2.0 / 7, 3.0 / 7, 6.0 / 7), v.normalize());
    }

    @Test
    void dotAndCross() {
        assertEquals(0, Vector3.UNIT_X.dot(Vector3.UNIT_Y), 0);
        assertEquals(32, Vector3.at(1, 2, 3).dot(Vector3.at(4, 5, 6)), 0);
        assertEquals(Vector3.UNIT_Z, Vector3.UNIT_X.cross(Vector3.UNIT_Y));
        assertEquals(Vector3.UNIT_X, Vector3.UNIT_Y.cross(Vector3.UNIT_Z));
        assertEquals(Vector3.UNIT_Y, Vector3.UNIT_Z.cross(Vector3.UNIT_X));
        Vector3 a = Vector3.at(1, 2, 3);
        Vector3 b = Vector3.at(-2, 0.5, 4);
        Vector3 cross = a.cross(b);
        assertEquals(0, cross.dot(a), EPSILON);
        assertEquals(0, cross.dot(b), EPSILON);
        assertEquals(cross.multiply(-1), b.cross(a));
    }

    @ParameterizedTest
    @CsvSource({
        "1.5, 1, 2, 2",
        "-1.5, -2, -1, -1",
        "0.49, 0, 1, 0",
        "-0.5, -1, -0, 0",
        "2.5, 2, 3, 3",
        "-2.5, -3, -2, -2",
    })
    @DisplayName("floor, ceil and round (half up towards positive infinity)")
    void rounding(double value, double floor, double ceil, double round) {
        Vector3 v = Vector3.at(value, value, value);
        assertEquals(floor, v.floor().x(), 0);
        assertEquals(ceil, v.ceil().y(), 0);
        assertEquals(round, v.round().z(), 0);
    }

    @Test
    void absAndMinMax() {
        Vector3 a = Vector3.at(-1, 5, -3);
        Vector3 b = Vector3.at(2, -6, -3);
        assertEquals(Vector3.at(1, 5, 3), a.abs());
        assertEquals(Vector3.at(-1, -6, -3), a.getMinimum(b));
        assertEquals(Vector3.at(2, 5, -3), a.getMaximum(b));
    }

    @Test
    void containedWithinIsInclusive() {
        Vector3 min = Vector3.at(0, 0, 0);
        Vector3 max = Vector3.at(1, 1, 1);
        assertTrue(Vector3.at(0, 1, 0.5).containedWithin(min, max));
        assertFalse(Vector3.at(0, 1.0001, 0.5).containedWithin(min, max));
        assertFalse(Vector3.at(-0.0001, 0, 0).containedWithin(min, max));
    }

    @Test
    void clampY() {
        Vector3 v = Vector3.at(3, 100, 4);
        assertEquals(Vector3.at(3, 64, 4), v.clampY(0, 64));
        assertEquals(Vector3.at(3, 200, 4), v.clampY(200, 300));
        assertSame(v, v.clampY(0, 255));
        assertThrows(IllegalArgumentException.class, () -> v.clampY(10, 5));
    }

    @Test
    void withers() {
        Vector3 v = Vector3.at(1, 2, 3);
        assertEquals(Vector3.at(9, 2, 3), v.withX(9));
        assertEquals(Vector3.at(1, 9, 3), v.withY(9));
        assertEquals(Vector3.at(1, 2, 9), v.withZ(9));
    }

    @Test
    void blockConversionsFloor() {
        Vector3 v = Vector3.at(-0.5, 1.999, -2.0001);
        assertEquals(-1, v.blockX());
        assertEquals(1, v.blockY());
        assertEquals(-3, v.blockZ());
        assertEquals(BlockVector3.at(-1, 1, -3), v.toBlockPoint());
        assertEquals(Vector2.at(-0.5, -2.0001), v.toVector2());
    }

    @Test
    void transform2DRotatesAroundAPoint() {
        Vector3 v = Vector3.at(2, 7, 1);
        Vector3 rotated = v.transform2D(90, 1, 1, 0, 0);
        assertEquals(1, rotated.x(), EPSILON);
        assertEquals(7, rotated.y(), 0);
        assertEquals(2, rotated.z(), EPSILON);

        Vector3 translated = v.transform2D(0, 0, 0, 5, -5);
        assertEquals(Vector3.at(7, 7, -4), translated);
    }

    @ParameterizedTest
    @CsvSource({
        "0, 0, 1, 0",
        "-1, 0, 0, 90",
        "0, 0, -1, 180",
        "1, 0, 0, 270",
    })
    void yaw(double x, double y, double z, double expectedYaw) {
        assertEquals(expectedYaw, Vector3.at(x, y, z).toYaw(), EPSILON);
    }

    @ParameterizedTest
    @CsvSource({
        "0, 1, 0, -90",
        "0, -1, 0, 90",
        "1, 0, 0, 0",
        "1, 1, 0, -45",
        "0, -1, 1, 45",
    })
    void pitch(double x, double y, double z, double expectedPitch) {
        assertEquals(expectedPitch, Vector3.at(x, y, z).toPitch(), EPSILON);
    }

    @Test
    void yzxComparatorSortsByYThenZThenX() {
        List<Vector3> list = new ArrayList<>(List.of(
            Vector3.at(1, 1, 0), Vector3.at(0, 0, 1), Vector3.at(1, 0, 0), Vector3.at(0, 0, 0)
        ));
        list.sort(Vector3.sortByCoordsYzx());
        assertEquals(List.of(
            Vector3.at(0, 0, 0), Vector3.at(1, 0, 0), Vector3.at(0, 0, 1), Vector3.at(1, 1, 0)
        ), list);
    }

    @Test
    void stringForms() {
        Vector3 v = Vector3.at(1, -2.5, 3);
        assertEquals("(1.0, -2.5, 3.0)", v.toString());
        assertEquals("1.0,-2.5,3.0", v.toParserString());
    }
}
