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

import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Terrain flattener")
class TerrainFlattenerTest extends BuildTestBase {

    private static final CuboidRegion REGION = new CuboidRegion(BlockVector3.at(0, -10, 0), BlockVector3.at(4, 10, 4));

    /**
     * Columns of dirt topped with grass, the surface of column x being at y = x - 2.
     */
    private static TestExtent slope() {
        TestExtent extent = new TestExtent();
        for (int x = 0; x <= 4; x++) {
            extent.fill(x, -10, 0, x, x - 3, 4, dirt);
            extent.fill(x, x - 2, 0, x, x - 2, 4, grass);
        }
        return extent;
    }

    @Test
    @DisplayName("finds the surface and its average height")
    void surfaceHeights() {
        TestExtent extent = slope();
        TerrainFlattener flattener = new TerrainFlattener(extent, REGION, new ExistingBlockMask(extent), air, null);
        assertEquals(OptionalInt.of(-2), flattener.getSurfaceHeight(0, 0));
        assertEquals(OptionalInt.of(2), flattener.getSurfaceHeight(4, 3));
        assertEquals(OptionalInt.of(0), flattener.getAverageSurfaceHeight());
        assertEquals(OptionalInt.empty(), flattener.getSurfaceHeight(10, 10));
    }

    @Test
    @DisplayName("cuts above and fills below the target height")
    void flattens() throws Exception {
        TestExtent extent = slope();
        TerrainFlattener flattener = new TerrainFlattener(extent, REGION, new ExistingBlockMask(extent), air, null);
        flattener.flatten(0);

        for (int x = 0; x <= 4; x++) {
            for (int z = 0; z <= 4; z++) {
                assertEquals(grass, extent.getBlock(BlockVector3.at(x, 0, z)), "Top at " + x + ", " + z);
                for (int y = 1; y <= 10; y++) {
                    assertEquals(air, extent.getBlock(BlockVector3.at(x, y, z)));
                }
                for (int y = -10; y < 0; y++) {
                    assertEquals(dirt, extent.getBlock(BlockVector3.at(x, y, z)), "Fill at " + x + ", " + y + ", " + z);
                }
            }
        }
    }

    @Test
    @DisplayName("fills with the given pattern, also in empty columns")
    void fillsWithPattern() throws Exception {
        TestExtent extent = slope();
        // Remove a column entirely
        extent.fill(2, -10, 2, 2, 10, 2, air);
        TerrainFlattener flattener = new TerrainFlattener(extent, REGION, new ExistingBlockMask(extent), air, stone);
        flattener.flatten(1);

        // Raised columns: old surface replaced by the pattern, surface moved up
        assertEquals(stone, extent.getBlock(BlockVector3.at(0, -2, 0)));
        assertEquals(stone, extent.getBlock(BlockVector3.at(0, 0, 0)));
        assertEquals(grass, extent.getBlock(BlockVector3.at(0, 1, 0)));
        assertEquals(dirt, extent.getBlock(BlockVector3.at(0, -3, 0)));
        // Lowered column keeps its grass on top
        assertEquals(grass, extent.getBlock(BlockVector3.at(4, 1, 0)));
        assertEquals(air, extent.getBlock(BlockVector3.at(4, 2, 0)));
        // The empty column is filled from the bottom of the region
        for (int y = -10; y <= 1; y++) {
            assertEquals(stone, extent.getBlock(BlockVector3.at(2, y, 2)));
        }
        assertEquals(air, extent.getBlock(BlockVector3.at(2, 2, 2)));
    }

    @Test
    @DisplayName("does not touch blocks outside the region")
    void staysInRegion() throws Exception {
        TestExtent extent = slope();
        extent.fill(5, -10, 0, 5, 5, 0, stone);
        extent.fill(0, 11, 0, 0, 11, 0, stone);
        TerrainFlattener flattener = new TerrainFlattener(extent, REGION, new ExistingBlockMask(extent), air, null);
        flattener.flatten(-5);
        assertEquals(stone, extent.getBlock(BlockVector3.at(5, 5, 0)));
        assertEquals(stone, extent.getBlock(BlockVector3.at(0, 11, 0)));
        assertEquals(grass, extent.getBlock(BlockVector3.at(3, -5, 3)));
        assertEquals(air, extent.getBlock(BlockVector3.at(3, -4, 3)));
    }

}
