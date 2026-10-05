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
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@DisplayName("MathUtils")
public class MathUtilsTest {

    @ParameterizedTest
    @CsvSource({
        "0, 16, 0",
        "5, 16, 5",
        "16, 16, 0",
        "17, 16, 1",
        "-1, 16, 15",
        "-16, 16, 0",
        "-17, 16, 15",
        "7, 3, 1",
        "-7, 3, 2",
    })
    @DisplayName("divisorMod always returns a value in [0, n)")
    void divisorMod(int a, int n, int expected) {
        assertEquals(expected, MathUtils.divisorMod(a, n));
        assertEquals(Math.floorMod(a, n), MathUtils.divisorMod(a, n));
    }

    @ParameterizedTest
    @CsvSource({
        "0, 1, 0",
        "90, 0, 1",
        "180, -1, 0",
        "270, 0, -1",
        "360, 1, 0",
        "450, 0, 1",
        "-90, 0, -1",
        "-180, -1, 0",
        "-270, 0, 1",
        "-360, 1, 0",
        "720, 1, 0",
    })
    @DisplayName("dCos and dSin are exact for multiples of 90 degrees")
    void exactRightAngles(double degrees, double cos, double sin) {
        // exact equality, without a delta: this is the point of these helpers
        assertEquals(cos, MathUtils.dCos(degrees));
        assertEquals(sin, MathUtils.dSin(degrees));
    }

    @ParameterizedTest
    @ValueSource(doubles = {1, 30, 45, 60, 89.5, 90.5, 135, -45, 123.456, 1000})
    @DisplayName("dCos and dSin match Math.cos/sin for other angles")
    void otherAngles(double degrees) {
        assertEquals(Math.cos(Math.toRadians(degrees)), MathUtils.dCos(degrees), 1e-12);
        assertEquals(Math.sin(Math.toRadians(degrees)), MathUtils.dSin(degrees), 1e-12);
    }

    @Test
    void naiveTrigIsNotExact() {
        // documents why dCos/dSin exist
        double naive = Math.cos(Math.toRadians(90));
        assertEquals(0, naive, 1e-15);
        assertEquals(0.0, MathUtils.dCos(90));
        assertNotEquals(0.0, naive);
    }

    @ParameterizedTest
    @CsvSource({
        "0, 0",
        "0.4, 0",
        "0.5, 1",
        "1.5, 2",
        "2.5, 3",
        "-0.4, 0",
        "-0.5, -1",
        "-1.5, -2",
        "-2.5, -3",
        "3.7, 4",
        "-3.7, -4",
    })
    @DisplayName("roundHalfUp rounds ties away from zero")
    void roundHalfUp(double value, double expected) {
        assertEquals(expected, MathUtils.roundHalfUp(value), 0);
    }
}
