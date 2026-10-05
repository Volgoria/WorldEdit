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
import com.sk89q.worldedit.util.Direction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Stair builder")
class StairBuilderTest extends BuildTestBase {

    @Test
    @DisplayName("straight stairs go up one block per step and face the ascent")
    void straightStairs() throws Exception {
        TestExtent extent = new TestExtent();
        int affected = StairBuilder.straight(extent, BlockVector3.ZERO, BlockVector3.UNIT_X, 4, 3, stairs, null);

        assertEquals(12, affected);
        for (int step = 0; step < 4; step++) {
            for (int w = -1; w <= 1; w++) {
                assertEquals(stairs.with(FACING, Direction.EAST), extent.getBlock(BlockVector3.at(step, step, w)));
            }
        }
        assertEquals(12, extent.nonAir().size());
    }

    @Test
    @DisplayName("straight stairs can be supported")
    void supportedStairs() throws Exception {
        TestExtent extent = new TestExtent();
        StairBuilder.straight(extent, BlockVector3.ZERO, BlockVector3.UNIT_MINUS_Z, 3, 1, stone, dirt);
        assertEquals(stone, extent.getBlock(BlockVector3.at(0, 2, -2)));
        assertEquals(dirt, extent.getBlock(BlockVector3.at(0, 1, -2)));
        assertEquals(dirt, extent.getBlock(BlockVector3.at(0, 0, -2)));
        assertEquals(dirt, extent.getBlock(BlockVector3.at(0, 0, -1)));
        assertEquals(6, extent.nonAir().size());
    }

    @Test
    @DisplayName("blocks without a facing property are placed unchanged")
    void orientIgnoresOtherBlocks() {
        assertEquals(stone.toBaseBlock(), StairBuilder.orient(stone.toBaseBlock(), BlockVector3.UNIT_X.toVector3()));
        assertEquals(stairs.with(FACING, Direction.SOUTH).toBaseBlock(),
            StairBuilder.orient(stairs.toBaseBlock(), BlockVector3.UNIT_Z.toVector3()));
    }

    @Test
    @DisplayName("a full turn of spiral stairs covers each column once")
    void spiralCoversEachColumnOnce() throws Exception {
        TestExtent extent = new TestExtent();
        int radius = 3;
        StairBuilder.spiral(extent, BlockVector3.ZERO, radius, 8, 8, true, stone, gold);

        for (int y = 0; y < 8; y++) {
            assertEquals(gold, extent.getBlock(BlockVector3.at(0, y, 0)));
        }
        double radiusSq = (radius + 0.5) * (radius + 0.5);
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if ((x == 0 && z == 0) || x * x + z * z > radiusSq) {
                    continue;
                }
                int count = 0;
                for (int y = -1; y <= 9; y++) {
                    if (extent.getBlock(BlockVector3.at(x, y, z)).equals(stone)) {
                        count++;
                    }
                }
                assertEquals(1, count, "Column " + x + ", " + z);
            }
        }
    }

    @Test
    @DisplayName("spiral stairs repeat every turn and go up in the right direction")
    void spiralDirection() throws Exception {
        TestExtent clockwise = new TestExtent();
        StairBuilder.spiral(clockwise, BlockVector3.ZERO, 3, 8, 4, true, stone, null);
        // Clockwise seen from above: south-east, south-west, north-west, north-east
        assertEquals(stone, clockwise.getBlock(BlockVector3.at(2, 0, 2)));
        assertEquals(stone, clockwise.getBlock(BlockVector3.at(-2, 1, 2)));
        assertEquals(stone, clockwise.getBlock(BlockVector3.at(-2, 2, -2)));
        assertEquals(stone, clockwise.getBlock(BlockVector3.at(2, 3, -2)));
        // The second turn
        assertEquals(stone, clockwise.getBlock(BlockVector3.at(2, 4, 2)));
        assertEquals(air, clockwise.getBlock(BlockVector3.at(0, 0, 0)));

        TestExtent counter = new TestExtent();
        StairBuilder.spiral(counter, BlockVector3.ZERO, 3, 4, 4, false, stone, null);
        assertEquals(stone, counter.getBlock(BlockVector3.at(2, 0, -2)));
        assertEquals(stone, counter.getBlock(BlockVector3.at(-2, 1, -2)));
        assertEquals(stone, counter.getBlock(BlockVector3.at(-2, 2, 2)));
        assertEquals(stone, counter.getBlock(BlockVector3.at(2, 3, 2)));
    }

    @Test
    @DisplayName("spiral steps face the direction of ascent")
    void spiralFacing() throws Exception {
        TestExtent extent = new TestExtent();
        StairBuilder.spiral(extent, BlockVector3.ZERO, 2, 8, 8, true, stairs, null);
        // At the east side, going clockwise means going south
        assertEquals(stairs.with(FACING, Direction.SOUTH), extent.getBlock(BlockVector3.at(2, 0, 0)));
        // At the south side, going clockwise means going west
        assertEquals(stairs.with(FACING, Direction.WEST), extent.getBlock(BlockVector3.at(0, 2, 2)));
    }

    @Test
    @DisplayName("angles are measured in the direction of ascent")
    void angles() {
        assertEquals(0, StairBuilder.angleOf(1, 0, true), 1e-9);
        assertEquals(Math.PI / 2, StairBuilder.angleOf(0, 1, true), 1e-9);
        assertEquals(3 * Math.PI / 2, StairBuilder.angleOf(0, 1, false), 1e-9);
        assertTrue(StairBuilder.angleOf(-1, -1, true) < Math.PI * 2);
    }

    @Test
    @DisplayName("rejects invalid parameters")
    void invalidParameters() {
        TestExtent extent = new TestExtent();
        assertThrows(IllegalArgumentException.class,
            () -> StairBuilder.straight(extent, BlockVector3.ZERO, BlockVector3.UNIT_Y, 3, 1, stone, null));
        assertThrows(IllegalArgumentException.class,
            () -> StairBuilder.straight(extent, BlockVector3.ZERO, BlockVector3.UNIT_X, 0, 1, stone, null));
        assertThrows(IllegalArgumentException.class,
            () -> StairBuilder.spiral(extent, BlockVector3.ZERO, 0, 3, 8, true, stone, null));
        assertThrows(IllegalArgumentException.class,
            () -> StairBuilder.spiral(extent, BlockVector3.ZERO, 3, 3, 1, true, stone, null));
    }

}
