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

import com.sk89q.worldedit.BaseWorldEditTest;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Polygonal2DRegionTest extends BaseWorldEditTest {

    @ParameterizedTest
    @MethodSource("areaTestData")
    void testArea(int[][] coordinates, long expectedVolume) {
        List<BlockVector2> points = toPoints(coordinates);
        Polygonal2DRegion region = new Polygonal2DRegion(null, points, 0, 0);

        assertEquals(expectedVolume, region.getVolume());
    }

    static List<Arguments> areaTestData() {
        return List.of(
                Arguments.of(new int[][]{{0, 0}, {0, 1}, {1, 1}, {1, 0}}, 4), // square
                Arguments.of(new int[][]{{0, 0}, {2, 2}, {2, 0}}, 6), // triangle
                Arguments.of(new int[][]{{6, 3}, {6, 1}, {0, 0}}, 10), // polygon with separated parts
                Arguments.of(new int[][]{{-1, 1}, {4, 1}, {4, -3}, {-1, -3}}, 30), // x < 0
                Arguments.of(new int[][]{
                        {0, 9}, {6, 9}, {6, 0}, {1, 2}, {4, 4}, {3, 7}, {0, 5},
                }, 47), // concave
                Arguments.of(new int[][]{
                        {0, 4}, {2, 6}, {4, 6}, {6, 4}, {6, 2}, {4, 0}, {2, 0}, {0, 2},
                }, 37), // octagon
                Arguments.of(new int[][]{
                        {0, 0}, {2, 2}, {2, 4}, {0, 6}, {6, 6}, {4, 4}, {4, 2}, {6, 0},
                }, 33), // hourglass
                Arguments.of(new int[][]{
                        {0, 5}, {11, 5}, {11, 0}, {9, 0}, {9, 4}, {7, 4}, {7, 1}, {6, 1},
                        {6, 2}, {4, 2}, {4, 1}, {3, 1}, {3, 0}, {1, 0}, {1, 3}, {0, 3},
                }, 60), // checks if new direction is well assigned
                Arguments.of(new int[][]{
                        {0, 5}, {2, 3}, {5, 3}, {7, 1}, {0, 1},
                }, 24), // horizontal and downwards
                Arguments.of(new int[][]{
                        {0, 0}, {2, 2}, {4, 2}, {6, 4}, {6, 0},
                }, 21), // horizontal and upwards
                Arguments.of(new int[][]{
                        {0, 5}, {3, 5}, {2, 3}, {5, 3}, {7, 5}, {7, 1}, {0, 1},
                }, 34), // horizontal with upwards and downwards
                Arguments.of(new int[][]{
                        {0, 5}, {3, 5}, {2, 3}, {4, 3}, {5, 3}, {7, 5}, {7, 1}, {0, 1},
                }, 34), // horizontal, upwards, downwards, redundant point
                Arguments.of(new int[][]{
                        {1, 3}, {3, 3}, {4, 5}, {6, 8}, {7, 6}, {9, 6}, {12, 8}, {11, 6}, {11, 4},
                        {10, 2}, {8, 0}, {6, 1}, {9, 4}, {6, 3}, {4, 1}, {3, 0}, {1, 1},
                }, 55) // complex polygon
        );
    }

    @ParameterizedTest
    @MethodSource("areaTestData")
    void iterationCountMatchesVolume(int[][] coordinates, long expectedVolume) {
        Polygonal2DRegion region = new Polygonal2DRegion(null, toPoints(coordinates), 3, 5);

        Set<BlockVector3> seen = new HashSet<>();
        for (BlockVector3 pos : region) {
            assertTrue(region.contains(pos), () -> "iterated position not contained: " + pos);
            assertTrue(seen.add(pos), () -> "duplicate position " + pos);
        }
        assertEquals(expectedVolume * 3, seen.size());
        assertEquals(expectedVolume * 3, region.getVolume());

        Set<BlockVector2> flat = new HashSet<>();
        region.asFlatRegion().forEach(flat::add);
        assertEquals(expectedVolume, flat.size());
    }

    @Test
    void containsIncludesEdgesAndRespectsHeight() {
        Polygonal2DRegion region = new Polygonal2DRegion(null, toPoints(new int[][]{{0, 0}, {4, 0}, {4, 4}, {0, 4}}), 0, 2);
        assertTrue(region.contains(BlockVector3.at(0, 0, 0)));
        assertTrue(region.contains(BlockVector3.at(4, 2, 4)));
        assertTrue(region.contains(BlockVector3.at(2, 1, 0)));
        assertFalse(region.contains(BlockVector3.at(2, 3, 2)));
        assertFalse(region.contains(BlockVector3.at(2, -1, 2)));
        assertFalse(region.contains(BlockVector3.at(5, 1, 2)));
    }

    @Test
    void degeneratePolygonContainsNothing() {
        Polygonal2DRegion region = new Polygonal2DRegion(null, toPoints(new int[][]{{0, 0}, {4, 0}}), 0, 2);
        assertFalse(region.contains(BlockVector3.at(0, 0, 0)));
        assertFalse(region.contains(BlockVector3.at(2, 0, 0)));
    }

    @Test
    void boundsFollowPoints() {
        Polygonal2DRegion region = new Polygonal2DRegion(null, toPoints(new int[][]{{-3, 2}, {5, -1}, {0, 7}}), 10, 4);
        // minY and maxY are swapped into order
        assertEquals(BlockVector3.at(-3, 4, -1), region.getMinimumPoint());
        assertEquals(BlockVector3.at(5, 10, 7), region.getMaximumPoint());
        assertEquals(9, region.getWidth());
        assertEquals(7, region.getHeight());
        assertEquals(9, region.getLength());
    }

    @Test
    void shiftMovesPointsAndHeight() {
        Polygonal2DRegion region = new Polygonal2DRegion(null, toPoints(new int[][]{{0, 0}, {2, 0}, {2, 2}, {0, 2}}), 0, 1);
        long volume = region.getVolume();
        region.shift(BlockVector3.at(10, 5, -10));
        assertEquals(BlockVector3.at(10, 5, -10), region.getMinimumPoint());
        assertEquals(BlockVector3.at(12, 6, -8), region.getMaximumPoint());
        assertEquals(volume, region.getVolume());
        assertEquals(BlockVector2.at(10, -10), region.getPoints().getFirst());
    }

    @Test
    void expandAndContractOnlyVertically() throws RegionOperationException {
        Polygonal2DRegion region = new Polygonal2DRegion(null, toPoints(new int[][]{{0, 0}, {2, 0}, {2, 2}, {0, 2}}), 0, 1);
        region.expand(BlockVector3.at(0, 3, 0), BlockVector3.at(0, -2, 0));
        assertEquals(-2, region.getMinimumY());
        assertEquals(4, region.getMaximumY());
        region.contract(BlockVector3.at(0, 1, 0), BlockVector3.at(0, -1, 0));
        assertEquals(-1, region.getMinimumY());
        assertEquals(3, region.getMaximumY());

        assertThrows(RegionOperationException.class, () -> region.expand(BlockVector3.at(1, 0, 0)));
        assertThrows(RegionOperationException.class, () -> region.contract(BlockVector3.at(0, 0, 1)));
    }

    @Test
    void expandYOnlyGrows() {
        Polygonal2DRegion region = new Polygonal2DRegion();
        assertTrue(region.expandY(5));
        assertFalse(region.expandY(5));
        assertTrue(region.expandY(2));
        assertTrue(region.expandY(9));
        assertFalse(region.expandY(7));
        assertEquals(2, region.getMinimumY());
        assertEquals(9, region.getMaximumY());
    }

    @Test
    void copyIsIndependent() {
        Polygonal2DRegion region = new Polygonal2DRegion(null, toPoints(new int[][]{{0, 0}, {2, 0}, {2, 2}}), 0, 1);
        Polygonal2DRegion copy = new Polygonal2DRegion(region);
        copy.addPoint(BlockVector2.at(0, 2));
        copy.shift(BlockVector3.at(1, 1, 1));
        assertEquals(3, region.size());
        assertEquals(4, copy.size());
        assertEquals(BlockVector3.ZERO, region.getMinimumPoint());
    }

    private static List<BlockVector2> toPoints(int[]... coordinates) {
        List<BlockVector2> points = new ArrayList<>();
        for (int[] coordinate : coordinates) {
            points.add(BlockVector2.at(coordinate[0], coordinate[1]));
        }

        return points;
    }

}
