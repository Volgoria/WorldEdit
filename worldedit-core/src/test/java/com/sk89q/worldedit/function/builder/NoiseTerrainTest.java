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

package com.sk89q.worldedit.function.builder;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector2;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.noise.NoiseGenerator;
import com.sk89q.worldedit.math.noise.PerlinNoise;
import com.sk89q.worldedit.regions.CuboidRegion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Noise terrain")
class NoiseTerrainTest extends BuildTestBase {

    private static final CuboidRegion REGION = new CuboidRegion(BlockVector3.at(0, 0, 0), BlockVector3.at(15, 20, 15));

    private static NoiseGenerator constant(float value) {
        return new NoiseGenerator() {
            @Override
            public float noise(Vector2 position) {
                return value;
            }

            @Override
            public float noise(Vector3 position) {
                return value;
            }
        };
    }

    @Test
    @DisplayName("fills each column up to the noise height, with a top layer")
    void constantNoise() throws Exception {
        TestExtent extent = new TestExtent();
        extent.fill(0, 0, 0, 15, 20, 15, dirt);
        NoiseTerrain terrain = new NoiseTerrain(constant(0.5f), 16, 10);
        assertEquals(5, terrain.getHeight(3, 7));
        terrain.generate(extent, REGION, stone, grass, air);

        for (int x = 0; x <= 15; x++) {
            for (int z = 0; z <= 15; z++) {
                for (int y = 0; y < 5; y++) {
                    assertEquals(stone, extent.getBlock(BlockVector3.at(x, y, z)));
                }
                assertEquals(grass, extent.getBlock(BlockVector3.at(x, 5, z)));
                for (int y = 6; y <= 20; y++) {
                    assertEquals(air, extent.getBlock(BlockVector3.at(x, y, z)));
                }
            }
        }
    }

    @Test
    @DisplayName("can keep the blocks above the terrain")
    void keepAbove() throws Exception {
        TestExtent extent = new TestExtent();
        extent.put(BlockVector3.at(0, 15, 0), gold);
        new NoiseTerrain(constant(0f), 16, 10).generate(extent, REGION, stone, null, null);
        assertEquals(stone, extent.getBlock(BlockVector3.at(0, 0, 0)));
        assertEquals(air, extent.getBlock(BlockVector3.at(0, 1, 0)));
        assertEquals(gold, extent.getBlock(BlockVector3.at(0, 15, 0)));
    }

    @Test
    @DisplayName("perlin terrain stays within its amplitude and varies")
    void perlinTerrain() throws Exception {
        PerlinNoise noise = new PerlinNoise();
        noise.setSeed(1234);
        NoiseTerrain terrain = new NoiseTerrain(noise, 8, 12);
        TestExtent extent = new TestExtent();
        terrain.generate(extent, REGION, stone, null, air);

        Set<Integer> heights = new HashSet<>();
        for (int x = 0; x <= 15; x++) {
            for (int z = 0; z <= 15; z++) {
                int height = terrain.getHeight(x, z);
                assertTrue(height >= 0 && height <= 12, "Height out of range: " + height);
                heights.add(height);
                assertEquals(stone, extent.getBlock(BlockVector3.at(x, height, z)));
                assertEquals(air, extent.getBlock(BlockVector3.at(x, height + 1, z)));
            }
        }
        assertTrue(heights.size() > 1, "Terrain should not be flat");
        for (BlockVector3 pos : extent.nonAir()) {
            assertTrue(REGION.contains(pos), "Block outside of the region at " + pos);
        }
    }

    @Test
    @DisplayName("rejects invalid parameters")
    void invalidParameters() {
        assertThrows(IllegalArgumentException.class, () -> new NoiseTerrain(constant(0), 0, 10));
        assertThrows(IllegalArgumentException.class, () -> new NoiseTerrain(constant(0), 4, -1));
    }

}
