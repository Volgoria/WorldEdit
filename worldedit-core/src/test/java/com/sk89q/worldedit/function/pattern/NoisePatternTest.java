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
import com.sk89q.worldedit.math.Vector2;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.noise.NoiseGenerator;
import com.sk89q.worldedit.math.noise.PerlinNoise;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoisePatternTest {

    /**
     * Noise that is simply the X coordinate, clamped to [0, 1].
     */
    private static final NoiseGenerator X_NOISE = new NoiseGenerator() {
        @Override
        public float noise(Vector2 position) {
            return (float) Math.max(0, Math.min(1, position.x()));
        }

        @Override
        public float noise(Vector3 position) {
            return (float) Math.max(0, Math.min(1, position.x()));
        }
    };

    private static int indexAt(NoisePattern pattern, List<MarkerPattern> markers, int x) {
        return MarkerPattern.indexOf(markers, pattern.applyBlock(BlockVector3.at(x, 0, 0)));
    }

    @Test
    void mapsNoiseRangeToPatterns() {
        List<MarkerPattern> markers = MarkerPattern.create(4);
        // scale 8: x in [0, 8] maps to noise [0, 1]
        NoisePattern pattern = new NoisePattern(X_NOISE, 8, markers);
        assertEquals(0, indexAt(pattern, markers, -5));
        assertEquals(0, indexAt(pattern, markers, 1));
        assertEquals(1, indexAt(pattern, markers, 2));
        assertEquals(2, indexAt(pattern, markers, 4));
        assertEquals(3, indexAt(pattern, markers, 7));
        // a noise value of exactly 1 must still map to the last pattern
        assertEquals(3, indexAt(pattern, markers, 8));
        assertEquals(3, indexAt(pattern, markers, 100));
    }

    @Test
    void perlinNoiseUsesAllPatternsInPatches() {
        List<MarkerPattern> markers = MarkerPattern.create(2);
        PerlinNoise noise = new PerlinNoise();
        noise.setSeed(42);
        NoisePattern pattern = new NoisePattern(noise, 16, markers);
        int[] counts = new int[2];
        int sameAsNeighbour = 0;
        for (int x = 0; x < 64; x++) {
            for (int z = 0; z < 64; z++) {
                int index = MarkerPattern.indexOf(markers, pattern.applyBlock(BlockVector3.at(x, 0, z)));
                counts[index]++;
                if (index == MarkerPattern.indexOf(markers, pattern.applyBlock(BlockVector3.at(x + 1, 0, z)))) {
                    sameAsNeighbour++;
                }
            }
        }
        assertTrue(counts[0] > 0 && counts[1] > 0, "both patterns should be used");
        // coherent noise: almost all neighbours share their pattern
        assertTrue(sameAsNeighbour > 64 * 64 * 0.85, "noise should form patches, got " + sameAsNeighbour);
    }

    @Test
    void rejectsInvalidArguments() {
        List<MarkerPattern> markers = MarkerPattern.create(2);
        assertThrows(IllegalArgumentException.class, () -> new NoisePattern(X_NOISE, 0, markers));
        assertThrows(IllegalArgumentException.class, () -> new NoisePattern(X_NOISE, 1, List.of()));
    }
}
