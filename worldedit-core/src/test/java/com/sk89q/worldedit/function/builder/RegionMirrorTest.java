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
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.util.Direction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Region mirror")
class RegionMirrorTest extends BuildTestBase {

    @Test
    @DisplayName("copies the opposite half onto the half in the direction")
    void mirrorsEvenWidth() throws Exception {
        TestExtent extent = new TestExtent();
        CuboidRegion region = new CuboidRegion(BlockVector3.at(0, 0, 0), BlockVector3.at(9, 2, 0));
        extent.put(BlockVector3.at(1, 0, 0), stone);
        extent.put(BlockVector3.at(3, 1, 0), gold);
        extent.put(BlockVector3.at(9, 2, 0), dirt);

        RegionMirror.mirror(extent, region, BlockVector3.UNIT_X, false);

        assertEquals(stone, extent.getBlock(BlockVector3.at(8, 0, 0)));
        assertEquals(gold, extent.getBlock(BlockVector3.at(6, 1, 0)));
        // Air from (0, 2, 0) overwrites the target half
        assertEquals(air, extent.getBlock(BlockVector3.at(9, 2, 0)));
        // The source half is untouched
        assertEquals(stone, extent.getBlock(BlockVector3.at(1, 0, 0)));
        assertEquals(gold, extent.getBlock(BlockVector3.at(3, 1, 0)));
    }

    @Test
    @DisplayName("can skip air and go in the negative direction")
    void mirrorsNegativeSkippingAir() throws Exception {
        TestExtent extent = new TestExtent();
        CuboidRegion region = new CuboidRegion(BlockVector3.at(0, 0, 0), BlockVector3.at(0, 0, 9));
        extent.put(BlockVector3.at(0, 0, 7), stone);
        extent.put(BlockVector3.at(0, 0, 0), dirt);

        int affected = RegionMirror.mirror(extent, region, BlockVector3.UNIT_MINUS_Z, true);

        assertEquals(1, affected);
        assertEquals(stone, extent.getBlock(BlockVector3.at(0, 0, 2)));
        // (0, 0, 9) is air, so the dirt at its mirror position stays
        assertEquals(dirt, extent.getBlock(BlockVector3.at(0, 0, 0)));
    }

    @Test
    @DisplayName("leaves the centre plane of an odd-sized region alone")
    void oddWidthKeepsCentre() throws Exception {
        TestExtent extent = new TestExtent();
        CuboidRegion region = new CuboidRegion(BlockVector3.at(0, 0, 0), BlockVector3.at(0, 8, 0));
        extent.fill(0, 0, 0, 0, 3, 0, stone);
        extent.put(BlockVector3.at(0, 4, 0), gold);

        RegionMirror.mirror(extent, region, BlockVector3.UNIT_Y, false);

        assertEquals(gold, extent.getBlock(BlockVector3.at(0, 4, 0)));
        for (int y = 5; y <= 8; y++) {
            assertEquals(stone, extent.getBlock(BlockVector3.at(0, y, 0)));
        }
    }

    @Test
    @DisplayName("flips the orientation of blocks")
    void flipsOrientation() throws Exception {
        TestExtent extent = new TestExtent();
        CuboidRegion region = new CuboidRegion(BlockVector3.at(-5, 0, -5), BlockVector3.at(4, 0, 4));
        extent.put(BlockVector3.at(-3, 0, 2), stairs.with(FACING, Direction.EAST));
        extent.put(BlockVector3.at(-2, 0, 1), stairs.with(FACING, Direction.NORTH));

        RegionMirror.mirror(extent, region, BlockVector3.UNIT_X, false);

        assertEquals(stairs.with(FACING, Direction.WEST), extent.getBlock(BlockVector3.at(2, 0, 2)));
        assertEquals(stairs.with(FACING, Direction.NORTH), extent.getBlock(BlockVector3.at(1, 0, 1)));
    }

    @Test
    @DisplayName("rejects diagonal directions")
    void rejectsDiagonal() {
        TestExtent extent = new TestExtent();
        CuboidRegion region = new CuboidRegion(BlockVector3.ZERO, BlockVector3.ONE);
        assertThrows(IllegalArgumentException.class,
            () -> RegionMirror.mirror(extent, region, BlockVector3.at(1, 0, 1), false));
    }

}
