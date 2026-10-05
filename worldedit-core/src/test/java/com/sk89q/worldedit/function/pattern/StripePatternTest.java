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

package com.sk89q.worldedit.function.pattern;

import com.sk89q.worldedit.math.BlockVector3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StripePatternTest {

    private static int indexAt(StripePattern pattern, List<MarkerPattern> markers, int x, int y, int z) {
        return MarkerPattern.indexOf(markers, pattern.applyBlock(BlockVector3.at(x, y, z)));
    }

    @Test
    void horizontalLayers() {
        List<MarkerPattern> markers = MarkerPattern.create(3);
        StripePattern pattern = new StripePattern(markers, BlockVector3.UNIT_Y, 1);
        for (int y = -6; y < 6; y++) {
            int expected = Math.floorMod(y, 3);
            assertEquals(expected, indexAt(pattern, markers, 0, y, 0));
            assertEquals(expected, indexAt(pattern, markers, 17, y, -40));
        }
    }

    @Test
    void thicknessGroupsLayers() {
        List<MarkerPattern> markers = MarkerPattern.create(2);
        StripePattern pattern = new StripePattern(markers, BlockVector3.UNIT_X, 3);
        int[] expected = {0, 0, 0, 1, 1, 1, 0, 0, 0, 1, 1, 1};
        for (int i = 0; i < expected.length; i++) {
            int x = i - 6;
            assertEquals(expected[i], indexAt(pattern, markers, x, 5, 5), "x=" + x);
        }
    }

    @Test
    void diagonalStripes() {
        List<MarkerPattern> markers = MarkerPattern.create(2);
        StripePattern pattern = new StripePattern(markers, BlockVector3.at(1, 0, 1), 1);
        assertEquals(0, indexAt(pattern, markers, 0, 0, 0));
        assertEquals(1, indexAt(pattern, markers, 1, 0, 0));
        assertEquals(1, indexAt(pattern, markers, 0, 0, 1));
        assertEquals(0, indexAt(pattern, markers, 1, 0, -1));
        assertEquals(0, indexAt(pattern, markers, 1, 0, 1));
    }

    @Test
    void rejectsInvalidArguments() {
        List<MarkerPattern> markers = MarkerPattern.create(2);
        assertThrows(IllegalArgumentException.class, () -> new StripePattern(markers, BlockVector3.ZERO, 1));
        assertThrows(IllegalArgumentException.class, () -> new StripePattern(markers, BlockVector3.UNIT_Y, 0));
        assertThrows(IllegalArgumentException.class, () -> new StripePattern(List.of(), BlockVector3.UNIT_Y, 1));
    }
}
