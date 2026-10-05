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

import com.google.common.collect.ImmutableList;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("A cuboid region")
public class CuboidRegionTest {

    private static CuboidRegion region(int x1, int y1, int z1, int x2, int y2, int z2) {
        return new CuboidRegion(BlockVector3.at(x1, y1, z1), BlockVector3.at(x2, y2, z2));
    }

    static List<Arguments> cuboids() {
        return List.of(
            Arguments.of(region(0, 0, 0, 0, 0, 0)),
            Arguments.of(region(0, 0, 0, 3, 4, 5)),
            Arguments.of(region(3, 4, 5, 0, 0, 0)),
            Arguments.of(region(-5, -64, -5, 5, -60, 5)),
            Arguments.of(region(10, 3, -2, -1, 7, 4))
        );
    }

    @ParameterizedTest
    @MethodSource("cuboids")
    @DisplayName("iterates exactly getVolume() distinct contained positions")
    void iterationMatchesVolume(CuboidRegion region) {
        Set<BlockVector3> seen = new HashSet<>();
        for (BlockVector3 pos : region) {
            assertTrue(region.contains(pos), () -> "iterated position not contained: " + pos);
            assertTrue(seen.add(pos), () -> "duplicate position " + pos);
        }
        assertEquals(region.getVolume(), seen.size());
        assertEquals((long) region.getWidth() * region.getHeight() * region.getLength(), region.getVolume());
    }

    @ParameterizedTest
    @MethodSource("cuboids")
    @DisplayName("flat iteration covers the X-Z footprint exactly once")
    void flatIteration(CuboidRegion region) {
        Set<BlockVector2> seen = new HashSet<>();
        for (BlockVector2 pos : region.asFlatRegion()) {
            assertTrue(seen.add(pos), () -> "duplicate position " + pos);
        }
        assertEquals((long) region.getWidth() * region.getLength(), seen.size());
    }

    @Test
    void iteratorThrowsWhenExhausted() {
        Iterator<BlockVector3> it = region(0, 0, 0, 0, 0, 0).iterator();
        assertTrue(it.hasNext());
        assertEquals(BlockVector3.ZERO, it.next());
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    void minMaxIndependentOfCornerOrder() {
        CuboidRegion a = region(5, -3, 2, -1, 7, 9);
        assertEquals(BlockVector3.at(-1, -3, 2), a.getMinimumPoint());
        assertEquals(BlockVector3.at(5, 7, 9), a.getMaximumPoint());
        assertEquals(-3, a.getMinimumY());
        assertEquals(7, a.getMaximumY());
        assertEquals(7, a.getWidth());
        assertEquals(11, a.getHeight());
        assertEquals(8, a.getLength());
        assertEquals(Vector3.at(2, 2, 5.5), a.getCenter());
    }

    @Test
    void containsIsInclusiveOnBoundaries() {
        CuboidRegion r = region(0, 0, 0, 2, 2, 2);
        assertTrue(r.contains(BlockVector3.at(0, 0, 0)));
        assertTrue(r.contains(BlockVector3.at(2, 2, 2)));
        assertTrue(r.contains(BlockVector3.at(1, 2, 0)));
        assertFalse(r.contains(BlockVector3.at(-1, 0, 0)));
        assertFalse(r.contains(BlockVector3.at(0, 3, 0)));
        assertFalse(r.contains(BlockVector3.at(0, 0, 3)));
    }

    @Test
    void expandGrowsTowardsChangeDirection() {
        CuboidRegion r = region(0, 0, 0, 4, 4, 4);
        r.expand(BlockVector3.at(2, 0, 0));
        assertEquals(BlockVector3.at(0, 0, 0), r.getMinimumPoint());
        assertEquals(BlockVector3.at(6, 4, 4), r.getMaximumPoint());

        r.expand(BlockVector3.at(0, -3, 0));
        assertEquals(BlockVector3.at(0, -3, 0), r.getMinimumPoint());

        r.expand(BlockVector3.at(0, 0, 1), BlockVector3.at(0, 0, -1));
        assertEquals(BlockVector3.at(0, -3, -1), r.getMinimumPoint());
        assertEquals(BlockVector3.at(6, 4, 5), r.getMaximumPoint());
    }

    @Test
    void expandWorksWhenPos1IsMaximum() {
        CuboidRegion r = region(4, 4, 4, 0, 0, 0);
        r.expand(BlockVector3.at(3, 3, 3));
        assertEquals(BlockVector3.at(7, 7, 7), r.getMaximumPoint());
        assertEquals(BlockVector3.at(7, 7, 7), r.getPos1());
        assertEquals(BlockVector3.ZERO, r.getPos2());
    }

    @Test
    void contractShrinksFromOppositeSide() {
        CuboidRegion r = region(0, 0, 0, 9, 9, 9);
        // a positive change contracts the minimum face upwards
        r.contract(BlockVector3.at(2, 0, 0));
        assertEquals(BlockVector3.at(2, 0, 0), r.getMinimumPoint());
        // a negative change contracts the maximum face downwards
        r.contract(BlockVector3.at(0, -3, 0));
        assertEquals(BlockVector3.at(9, 6, 9), r.getMaximumPoint());
        assertEquals(8 * 7 * 10, r.getVolume());
    }

    @Test
    void expandThenContractIsIdentity() {
        CuboidRegion r = region(-2, 5, 3, 6, 10, 8);
        CuboidRegion original = r.clone();
        r.expand(BlockVector3.at(3, 0, 0), BlockVector3.at(0, -2, 0));
        r.contract(BlockVector3.at(-3, 0, 0), BlockVector3.at(0, 2, 0));
        assertEquals(original.getMinimumPoint(), r.getMinimumPoint());
        assertEquals(original.getMaximumPoint(), r.getMaximumPoint());
    }

    @Test
    void shiftMovesBothCorners() {
        CuboidRegion r = region(0, 0, 0, 2, 3, 4);
        long volume = r.getVolume();
        r.shift(BlockVector3.at(-10, 5, 7));
        assertEquals(BlockVector3.at(-10, 5, 7), r.getMinimumPoint());
        assertEquals(BlockVector3.at(-8, 8, 11), r.getMaximumPoint());
        assertEquals(volume, r.getVolume());
    }

    @Test
    void cloneIsIndependent() {
        CuboidRegion r = region(0, 0, 0, 1, 1, 1);
        CuboidRegion copy = r.clone();
        assertNotSame(r, copy);
        copy.shift(BlockVector3.at(5, 5, 5));
        assertEquals(BlockVector3.ZERO, r.getMinimumPoint());
        assertEquals(BlockVector3.at(5, 5, 5), copy.getMinimumPoint());
    }

    @Test
    void fromCenter() {
        CuboidRegion zero = CuboidRegion.fromCenter(BlockVector3.at(3, 4, 5), 0);
        assertEquals(1, zero.getVolume());
        assertTrue(zero.contains(BlockVector3.at(3, 4, 5)));

        CuboidRegion two = CuboidRegion.fromCenter(BlockVector3.at(3, 4, 5), 2);
        assertEquals(125, two.getVolume());
        assertEquals(BlockVector3.at(1, 2, 3), two.getMinimumPoint());
        assertEquals(BlockVector3.at(5, 6, 7), two.getMaximumPoint());

        assertThrows(IllegalArgumentException.class, () -> CuboidRegion.fromCenter(BlockVector3.ZERO, -1));
    }

    @Test
    void makeCuboidUsesBoundingBox() {
        EllipsoidRegion ellipsoid = new EllipsoidRegion(BlockVector3.at(10, 10, 10), Vector3.at(2, 3, 4));
        CuboidRegion box = CuboidRegion.makeCuboid(ellipsoid);
        assertEquals(ellipsoid.getMinimumPoint(), box.getMinimumPoint());
        assertEquals(ellipsoid.getMaximumPoint(), box.getMaximumPoint());
    }

    @Test
    void chunksSpanNegativeCoordinates() {
        CuboidRegion r = region(-1, 0, -1, 16, 0, 0);
        assertEquals(
            Set.of(BlockVector2.at(-1, -1), BlockVector2.at(0, -1), BlockVector2.at(1, -1),
                BlockVector2.at(-1, 0), BlockVector2.at(0, 0), BlockVector2.at(1, 0)),
            r.getChunks()
        );
        assertEquals(6, r.getChunkCubes().size());
    }

    @Test
    void facesAndWallsCoverOnlyTheShell() {
        CuboidRegion r = region(0, 0, 0, 4, 4, 4);
        Region faces = r.getFaces();
        Region walls = r.getWalls();
        int faceCount = 0;
        int wallCount = 0;
        for (BlockVector3 pos : r) {
            boolean onX = pos.x() == 0 || pos.x() == 4;
            boolean onY = pos.y() == 0 || pos.y() == 4;
            boolean onZ = pos.z() == 0 || pos.z() == 4;
            assertEquals(onX || onY || onZ, faces.contains(pos), () -> "faces mismatch at " + pos);
            assertEquals(onX || onZ, walls.contains(pos), () -> "walls mismatch at " + pos);
            if (faces.contains(pos)) {
                faceCount++;
            }
            if (walls.contains(pos)) {
                wallCount++;
            }
        }
        assertEquals(125 - 27, faceCount);
        assertEquals((25 - 9) * 5, wallCount);
    }

    @Test
    void polygonizeReturnsFourCorners() {
        CuboidRegion r = region(0, 0, 0, 3, 0, 5);
        assertEquals(
            ImmutableList.of(BlockVector2.at(0, 0), BlockVector2.at(0, 5), BlockVector2.at(3, 5), BlockVector2.at(3, 0)),
            r.polygonize(-1)
        );
    }
}
