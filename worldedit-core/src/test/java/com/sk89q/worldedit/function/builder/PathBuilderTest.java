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

import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Path builder")
class PathBuilderTest extends BuildTestBase {

    private static TestExtent ground() {
        TestExtent extent = new TestExtent();
        extent.fill(-20, -5, -20, 20, 0, 20, stone);
        return extent;
    }

    @Test
    @DisplayName("a straight path of width 3 covers a 3-wide band")
    void straightPath() throws Exception {
        TestExtent extent = ground();
        PathBuilder builder = new PathBuilder(3, 0, true, 8);
        builder.build(extent, List.of(BlockVector3.at(0, 0, 0), BlockVector3.at(10, 0, 0)), false, gold, air);

        Set<BlockVector3> placed = extent.positionsOf(gold);
        assertEquals(13 * 3, placed.size());
        for (BlockVector3 pos : placed) {
            assertEquals(0, pos.y());
            assertTrue(pos.x() >= -1 && pos.x() <= 11 && Math.abs(pos.z()) <= 1, "Unexpected block at " + pos);
        }
    }

    @Test
    @DisplayName("follows the terrain and clears headroom")
    void followsTerrain() throws Exception {
        TestExtent extent = ground();
        // A step in the terrain and an obstacle above the path
        extent.fill(5, 1, -5, 20, 2, 5, stone);
        extent.fill(2, 1, 0, 2, 3, 0, dirt);
        PathBuilder builder = new PathBuilder(1, 2, true, 8);
        builder.build(extent, List.of(BlockVector3.at(0, 0, 0), BlockVector3.at(10, 0, 0)), false, gold, air);

        assertEquals(gold, extent.getBlock(BlockVector3.at(0, 0, 0)));
        assertEquals(gold, extent.getBlock(BlockVector3.at(10, 2, 0)));
        assertEquals(stone, extent.getBlock(BlockVector3.at(10, 1, 0)));
        // The dirt pillar is the surface there: the path goes over it, clearing above
        assertEquals(gold, extent.getBlock(BlockVector3.at(2, 3, 0)));
        assertEquals(dirt, extent.getBlock(BlockVector3.at(2, 2, 0)));
    }

    @Test
    @DisplayName("clears blocks above a path laid at a fixed height")
    void clearsHeadroom() throws Exception {
        TestExtent extent = ground();
        extent.fill(3, 1, 0, 3, 3, 0, dirt);
        PathBuilder builder = new PathBuilder(1, 2, false, 8);
        builder.build(extent, List.of(BlockVector3.at(0, 0, 0), BlockVector3.at(5, 0, 0)), false, gold, air);
        assertEquals(gold, extent.getBlock(BlockVector3.at(3, 0, 0)));
        assertEquals(air, extent.getBlock(BlockVector3.at(3, 1, 0)));
        assertEquals(air, extent.getBlock(BlockVector3.at(3, 2, 0)));
        assertEquals(dirt, extent.getBlock(BlockVector3.at(3, 3, 0)));
    }

    @Test
    @DisplayName("a floating path interpolates the height between points")
    void floatingPath() throws Exception {
        TestExtent extent = new TestExtent();
        PathBuilder builder = new PathBuilder(1, 0, false, 8);
        builder.build(extent, List.of(BlockVector3.at(0, 10, 0), BlockVector3.at(10, 20, 0)), false, gold, air);
        for (int x = 0; x <= 10; x++) {
            assertEquals(gold, extent.getBlock(BlockVector3.at(x, 10 + x, 0)));
        }
        assertEquals(11, extent.positionsOf(gold).size());
    }

    @Test
    @DisplayName("diagonal paths are connected and loops are closed")
    void diagonalAndLoop() {
        PathBuilder builder = new PathBuilder(1, 0, true, 8);
        List<BlockVector3> points = List.of(BlockVector3.at(0, 0, 0), BlockVector3.at(6, 0, 3), BlockVector3.at(0, 0, 6));
        Map<BlockVector2, Integer> open = builder.getColumns(points, false);
        Map<BlockVector2, Integer> closed = builder.getColumns(points, true);
        assertTrue(closed.size() > open.size());
        assertTrue(closed.containsKey(BlockVector2.at(0, 3)));
        assertTrue(!open.containsKey(BlockVector2.at(0, 3)));

        // Every column is adjacent (8-connectivity) to another one
        for (BlockVector2 column : closed.keySet()) {
            boolean connected = false;
            for (int dx = -1; dx <= 1 && !connected; dx++) {
                for (int dz = -1; dz <= 1 && !connected; dz++) {
                    if ((dx != 0 || dz != 0) && closed.containsKey(column.add(dx, dz))) {
                        connected = true;
                    }
                }
            }
            assertTrue(connected, "Isolated column " + column);
        }
    }

    @Test
    @DisplayName("even widths are exact")
    void evenWidth() {
        PathBuilder builder = new PathBuilder(2, 0, true, 8);
        Map<BlockVector2, Integer> columns = builder.getColumns(
            List.of(BlockVector3.at(0, 0, 0), BlockVector3.at(0, 0, 4)), false);
        // 2 blocks wide; the band also extends one block past the end point
        assertEquals(6 * 2, columns.size());
        for (BlockVector2 column : columns.keySet()) {
            assertTrue(column.x() == 0 || column.x() == 1, "Unexpected column " + column);
        }
    }

    @Test
    @DisplayName("needs at least two points")
    void needsTwoPoints() {
        PathBuilder builder = new PathBuilder(1, 0, true, 8);
        assertThrows(IllegalArgumentException.class, () -> builder.getColumns(List.of(BlockVector3.ZERO), false));
        assertThrows(IllegalArgumentException.class, () -> new PathBuilder(0, 0, true, 8));
    }

}
