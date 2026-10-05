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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GradientPatternTest {

    private static final int LAYER_SIZE = 32;

    /**
     * Count, per pattern, how often it is used on a full horizontal layer.
     */
    private static int[] countLayer(GradientPattern pattern, List<MarkerPattern> markers, int y) {
        int[] counts = new int[markers.size()];
        for (int x = 0; x < LAYER_SIZE; x++) {
            for (int z = 0; z < LAYER_SIZE; z++) {
                int index = MarkerPattern.indexOf(markers, pattern.applyBlock(BlockVector3.at(x, y, z)));
                counts[index]++;
            }
        }
        return counts;
    }

    @Test
    void endsArePure() {
        List<MarkerPattern> markers = MarkerPattern.create(2);
        GradientPattern pattern = new GradientPattern(markers, 0, 10);
        int total = LAYER_SIZE * LAYER_SIZE;
        assertEquals(total, countLayer(pattern, markers, 0)[0]);
        assertEquals(total, countLayer(pattern, markers, 10)[1]);
        // outside of the range, the nearest end is used
        assertEquals(total, countLayer(pattern, markers, -20)[0]);
        assertEquals(total, countLayer(pattern, markers, 50)[1]);
    }

    @Test
    void transitionIsSmoothAndMonotonic() {
        List<MarkerPattern> markers = MarkerPattern.create(2);
        GradientPattern pattern = new GradientPattern(markers, 0, 10);
        int previous = -1;
        for (int y = 0; y <= 10; y++) {
            int upper = countLayer(pattern, markers, y)[1];
            assertTrue(upper >= previous, "share of the upper pattern must not decrease");
            previous = upper;
        }
        double middleShare = countLayer(pattern, markers, 5)[1] / (double) (LAYER_SIZE * LAYER_SIZE);
        assertTrue(middleShare > 0.4 && middleShare < 0.6, "middle layer should be about half/half, was " + middleShare);
    }

    @Test
    void middlePatternIsPureAtItsLevel() {
        List<MarkerPattern> markers = MarkerPattern.create(3);
        GradientPattern pattern = new GradientPattern(markers, 0, 10);
        assertEquals(LAYER_SIZE * LAYER_SIZE, countLayer(pattern, markers, 5)[1]);
        int[] between = countLayer(pattern, markers, 2);
        assertEquals(0, between[2]);
        assertTrue(between[0] > 0 && between[1] > 0);
    }

    @Test
    void invertedGradient() {
        List<MarkerPattern> markers = MarkerPattern.create(2);
        GradientPattern pattern = new GradientPattern(markers, 10, 0);
        assertSame(markers.get(0).block(), pattern.applyBlock(BlockVector3.at(3, 10, 3)));
        assertSame(markers.get(1).block(), pattern.applyBlock(BlockVector3.at(3, 0, 3)));
    }

    @Test
    void isDeterministic() {
        List<MarkerPattern> markers = MarkerPattern.create(4);
        GradientPattern first = new GradientPattern(markers, -30, 70);
        GradientPattern second = new GradientPattern(markers, -30, 70);
        for (int y = -30; y <= 70; y += 7) {
            BlockVector3 position = BlockVector3.at(y * 3, y, -y);
            assertSame(first.applyBlock(position), second.applyBlock(position));
            assertSame(first.applyBlock(position), first.applyBlock(position));
        }
    }

    @Test
    void degenerateRanges() {
        List<MarkerPattern> markers = MarkerPattern.create(2);
        GradientPattern flat = new GradientPattern(markers, 5, 5);
        assertSame(markers.get(0).block(), flat.applyBlock(BlockVector3.at(0, 9, 0)));
        GradientPattern single = new GradientPattern(markers.subList(0, 1), 0, 10);
        assertSame(markers.get(0).block(), single.applyBlock(BlockVector3.at(0, 7, 0)));
        assertThrows(IllegalArgumentException.class, () -> new GradientPattern(List.of(), 0, 1));
    }
}
