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

package com.sk89q.worldedit.function.visitor;

import com.sk89q.worldedit.function.mask.RegionMask;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Breadth-first search visitors")
class BreadthFirstSearchTest {

    private static List<BlockVector3> flood(CuboidRegion region, BlockVector3 start) {
        List<BlockVector3> visited = new ArrayList<>();
        RecursiveVisitor visitor = new RecursiveVisitor(new RegionMask(region), position -> {
            visited.add(position);
            return true;
        });
        visitor.visit(start);
        Operations.completeBlindly(visitor);
        assertEquals(visited.size(), visitor.getAffected());
        return visited;
    }

    private static void assertVisitsWholeRegionOnce(CuboidRegion region, BlockVector3 start) {
        List<BlockVector3> visited = flood(region, start);
        Set<BlockVector3> unique = new HashSet<>(visited);
        assertEquals(visited.size(), unique.size(), "A position was visited twice");
        assertEquals(region.getVolume(), visited.size());
        for (BlockVector3 position : region) {
            assertTrue(unique.contains(position), "Missed " + position);
        }
    }

    @Test
    @DisplayName("visits every connected position exactly once")
    void visitsEveryPositionOnce() {
        CuboidRegion region = new CuboidRegion(BlockVector3.at(-5, -3, -4), BlockVector3.at(6, 7, 5));
        assertVisitsWholeRegionOnce(region, BlockVector3.at(0, 0, 0));
    }

    @Test
    @DisplayName("visits positions beyond the long-packable range exactly once")
    void visitsUnpackablePositions() {
        // Spans the y boundary of the long-packed form (y > 2047)
        CuboidRegion region = new CuboidRegion(BlockVector3.at(0, 2040, 0), BlockVector3.at(4, 2056, 4));
        assertVisitsWholeRegionOnce(region, BlockVector3.at(2, 2045, 2));
        // And huge horizontal coordinates
        int far = 40_000_000;
        CuboidRegion farRegion = new CuboidRegion(BlockVector3.at(far, 0, far), BlockVector3.at(far + 4, 4, far + 4));
        assertVisitsWholeRegionOnce(farRegion, BlockVector3.at(far + 1, 1, far + 1));
    }

    @Test
    @DisplayName("visits in breadth-first order")
    void visitsInBreadthFirstOrder() {
        CuboidRegion region = new CuboidRegion(BlockVector3.at(-4, -4, -4), BlockVector3.at(4, 4, 4));
        BlockVector3 start = BlockVector3.ZERO;
        List<BlockVector3> visited = flood(region, start);
        int lastDistance = 0;
        for (BlockVector3 position : visited) {
            BlockVector3 delta = position.subtract(start).abs();
            int distance = delta.x() + delta.y() + delta.z();
            assertFalse(distance < lastDistance, "Visited " + position + " out of order");
            lastDistance = distance;
        }
    }

    @Test
    @DisplayName("repeated roots are only visited once")
    void repeatedRootsVisitedOnce() {
        CuboidRegion region = new CuboidRegion(BlockVector3.ZERO, BlockVector3.ZERO);
        List<BlockVector3> visited = new ArrayList<>();
        RecursiveVisitor visitor = new RecursiveVisitor(new RegionMask(region), position -> {
            visited.add(position);
            return true;
        });
        visitor.visit(BlockVector3.ZERO);
        visitor.visit(BlockVector3.ZERO);
        Operations.completeBlindly(visitor);
        assertEquals(List.of(BlockVector3.ZERO), visited);
    }
}
