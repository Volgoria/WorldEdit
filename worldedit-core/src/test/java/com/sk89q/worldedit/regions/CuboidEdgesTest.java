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

package com.sk89q.worldedit.regions;

import com.sk89q.worldedit.math.BlockVector3;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CuboidEdgesTest {

    /**
     * Brute-force reference: a position is on an edge if at least two of its
     * coordinates lie on the boundary of the cuboid.
     */
    private static Set<BlockVector3> bruteForce(BlockVector3 min, BlockVector3 max) {
        Set<BlockVector3> result = new HashSet<>();
        for (int x = min.x(); x <= max.x(); x++) {
            for (int y = min.y(); y <= max.y(); y++) {
                for (int z = min.z(); z <= max.z(); z++) {
                    int onBoundary = 0;
                    if (x == min.x() || x == max.x()) {
                        onBoundary++;
                    }
                    if (y == min.y() || y == max.y()) {
                        onBoundary++;
                    }
                    if (z == min.z() || z == max.z()) {
                        onBoundary++;
                    }
                    if (onBoundary >= 2) {
                        result.add(BlockVector3.at(x, y, z));
                    }
                }
            }
        }
        return result;
    }

    @ParameterizedTest(name = "{0}x{1}x{2}")
    @CsvSource({
        "1,1,1",
        "2,2,2",
        "3,3,3",
        "5,1,1",
        "1,4,1",
        "1,1,6",
        "4,1,3",
        "7,5,9",
        "2,10,2",
    })
    void matchesBruteForceWithoutDuplicates(int sizeX, int sizeY, int sizeZ) {
        BlockVector3 min = BlockVector3.at(-3, 60, 10);
        BlockVector3 max = min.add(sizeX - 1, sizeY - 1, sizeZ - 1);

        List<BlockVector3> edges = CuboidEdges.getEdgePositions(min, max);

        assertEquals(edges.size(), new HashSet<>(edges).size(), "edge positions must be unique");
        assertEquals(bruteForce(min, max), new HashSet<>(edges));
    }

    @Test
    void cornerOrderDoesNotMatter() {
        BlockVector3 a = BlockVector3.at(5, 0, -5);
        BlockVector3 b = BlockVector3.at(-2, 7, 3);
        assertEquals(
            new HashSet<>(CuboidEdges.getEdgePositions(a, b)),
            new HashSet<>(CuboidEdges.getEdgePositions(b, a))
        );
    }

    @Test
    void cubeHasExpectedCount() {
        // 8 corners + 12 edges of 1 interior block each
        assertEquals(20, CuboidEdges.getEdgePositions(BlockVector3.ZERO, BlockVector3.at(2, 2, 2)).size());
        // General formula for an n-cube: 12n - 16
        assertEquals(12 * 10 - 16, CuboidEdges.getEdgePositions(BlockVector3.ZERO, BlockVector3.at(9, 9, 9)).size());
    }

    @Test
    void usesBoundingBoxOfRegion() {
        CuboidRegion region = new CuboidRegion(BlockVector3.at(0, 0, 0), BlockVector3.at(4, 4, 4));
        List<BlockVector3> edges = CuboidEdges.getEdgePositions(region);
        assertTrue(edges.contains(BlockVector3.at(0, 2, 0)));
        assertTrue(edges.contains(BlockVector3.at(4, 4, 2)));
        assertFalse(edges.contains(BlockVector3.at(2, 2, 2)), "center is not an edge");
        assertFalse(edges.contains(BlockVector3.at(2, 2, 0)), "face center is not an edge");
    }

    @Test
    void forEachVisitsSamePositionsInOrder() {
        BlockVector3 a = BlockVector3.at(3, -2, 7);
        BlockVector3 b = BlockVector3.at(-1, 5, 2);
        List<BlockVector3> visited = new ArrayList<>();
        CuboidEdges.forEachEdgePosition(a, b, visited::add);
        assertEquals(CuboidEdges.getEdgePositions(a, b), visited);
    }

    @Test
    void forEachStopsOnConsumerException() {
        int[] count = { 0 };
        IOException thrown = assertThrows(IOException.class, () ->
            CuboidEdges.forEachEdgePosition(BlockVector3.ZERO, BlockVector3.at(9, 9, 9), _ -> {
                if (++count[0] == 5) {
                    throw new IOException("stop");
                }
            }));
        assertEquals("stop", thrown.getMessage());
        assertEquals(5, count[0]);
    }
}
