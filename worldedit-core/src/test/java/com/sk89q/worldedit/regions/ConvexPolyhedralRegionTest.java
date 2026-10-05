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
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.world.World;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("A convex polyhedral region")
public class ConvexPolyhedralRegionTest extends BaseWorldEditTest {

    private static ConvexPolyhedralRegion of(BlockVector3... vertices) {
        ConvexPolyhedralRegion region = new ConvexPolyhedralRegion((World) null);
        for (BlockVector3 vertex : vertices) {
            region.addVertex(vertex);
        }
        return region;
    }

    private static ConvexPolyhedralRegion cube(int size) {
        return of(
            BlockVector3.at(0, 0, 0), BlockVector3.at(size, 0, 0),
            BlockVector3.at(0, size, 0), BlockVector3.at(0, 0, size),
            BlockVector3.at(size, size, 0), BlockVector3.at(size, 0, size),
            BlockVector3.at(0, size, size), BlockVector3.at(size, size, size)
        );
    }

    private static ConvexPolyhedralRegion tetrahedron(int size) {
        return of(
            BlockVector3.at(0, 0, 0), BlockVector3.at(size, 0, 0),
            BlockVector3.at(0, size, 0), BlockVector3.at(0, 0, size)
        );
    }

    private static Set<BlockVector3> iterate(Region region) {
        Set<BlockVector3> seen = new HashSet<>();
        for (BlockVector3 pos : region) {
            assertTrue(region.contains(pos), () -> "iterated position not contained: " + pos);
            assertTrue(seen.add(pos), () -> "duplicate position " + pos);
        }
        return seen;
    }

    @Test
    void emptyRegionIsUndefinedAndContainsNothing() {
        ConvexPolyhedralRegion region = of();
        assertFalse(region.isDefined());
        assertFalse(region.contains(BlockVector3.ZERO));
    }

    @Test
    void fewerThanThreeVerticesIsUndefined() {
        ConvexPolyhedralRegion region = of(BlockVector3.ZERO, BlockVector3.at(1, 2, 3));
        assertFalse(region.isDefined());
        assertFalse(region.contains(BlockVector3.ZERO));
    }

    @Test
    void duplicateVertexIsIgnored() {
        ConvexPolyhedralRegion region = tetrahedron(4);
        assertFalse(region.addVertex(BlockVector3.at(4, 0, 0)));
        assertEquals(4, region.getVertices().size());
    }

    @Test
    void cubeContainsAllLatticePointsIncludingBoundary() {
        ConvexPolyhedralRegion region = cube(4);
        assertTrue(region.isDefined());
        assertEquals(BlockVector3.ZERO, region.getMinimumPoint());
        assertEquals(BlockVector3.at(4, 4, 4), region.getMaximumPoint());
        assertEquals(Vector3.at(2, 2, 2), region.getCenter());
        assertEquals(125, iterate(region).size());
        assertFalse(region.contains(BlockVector3.at(5, 0, 0)));
        assertFalse(region.contains(BlockVector3.at(-1, 2, 2)));
    }

    @Test
    void tetrahedronContainsOnlyPointsUnderTheDiagonalPlane() {
        ConvexPolyhedralRegion region = tetrahedron(4);
        Set<BlockVector3> seen = iterate(region);
        for (BlockVector3 pos : seen) {
            assertTrue(pos.x() + pos.y() + pos.z() <= 4, () -> "outside tetrahedron: " + pos);
        }
        // lattice points with x, y, z >= 0 and x + y + z <= 4: C(7, 3)
        assertEquals(35, seen.size());
        assertTrue(region.contains(BlockVector3.at(2, 1, 1)));
        assertFalse(region.contains(BlockVector3.at(2, 2, 1)));
    }

    @Test
    void interiorVertexDoesNotChangeTheHull() {
        ConvexPolyhedralRegion region = cube(4);
        Set<BlockVector3> before = iterate(region);
        region.addVertex(BlockVector3.at(2, 2, 2));
        assertEquals(before, iterate(region));
    }

    @Test
    void addingAVertexGrowsTheHull() {
        ConvexPolyhedralRegion region = tetrahedron(4);
        assertFalse(region.contains(BlockVector3.at(4, 4, 4)));
        region.addVertex(BlockVector3.at(4, 4, 4));
        assertTrue(region.contains(BlockVector3.at(4, 4, 4)));
        assertTrue(region.contains(BlockVector3.at(2, 2, 2)));
        assertEquals(BlockVector3.at(4, 4, 4), region.getMaximumPoint());
    }

    @Test
    void coplanarStartIsResolvedOnceANonCoplanarVertexArrives() {
        // the first four vertices are all on the y = 0 plane, so one is kept in the backlog
        ConvexPolyhedralRegion region = of(
            BlockVector3.at(0, 0, 0), BlockVector3.at(4, 0, 0),
            BlockVector3.at(0, 0, 4), BlockVector3.at(4, 0, 4)
        );
        assertEquals(4, region.getVertices().size());
        region.addVertex(BlockVector3.at(0, 4, 0));
        region.addVertex(BlockVector3.at(4, 4, 0));
        region.addVertex(BlockVector3.at(0, 4, 4));
        region.addVertex(BlockVector3.at(4, 4, 4));
        assertEquals(125, iterate(region).size());
        assertTrue(region.contains(BlockVector3.at(4, 0, 4)));
    }

    @Test
    void shiftMovesEverything() {
        ConvexPolyhedralRegion region = tetrahedron(4);
        Set<BlockVector3> before = iterate(region);
        BlockVector3 offset = BlockVector3.at(-10, 20, 5);
        region.shift(offset);

        Set<BlockVector3> expected = new HashSet<>();
        before.forEach(pos -> expected.add(pos.add(offset)));
        assertEquals(expected, iterate(region));
        assertEquals(offset, region.getMinimumPoint());
        assertTrue(region.getVertices().contains(BlockVector3.at(-6, 20, 5)));
    }

    @Test
    void cloneIsIndependent() {
        ConvexPolyhedralRegion region = tetrahedron(4);
        ConvexPolyhedralRegion copy = (ConvexPolyhedralRegion) region.clone();
        copy.addVertex(BlockVector3.at(4, 4, 4));
        copy.shift(BlockVector3.at(100, 0, 0));
        assertEquals(4, region.getVertices().size());
        assertFalse(region.contains(BlockVector3.at(4, 4, 4)));
        assertTrue(region.contains(BlockVector3.ZERO));
    }

    @Test
    void clearResetsTheRegion() {
        ConvexPolyhedralRegion region = cube(2);
        region.clear();
        assertFalse(region.isDefined());
        assertTrue(region.getVertices().isEmpty());
        assertTrue(region.getTriangles().isEmpty());
    }

    @Test
    void expandAndContractAreUnsupported() {
        ConvexPolyhedralRegion region = cube(2);
        assertThrows(RegionOperationException.class, () -> region.expand(BlockVector3.UNIT_X));
        assertThrows(RegionOperationException.class, () -> region.contract(BlockVector3.UNIT_X));
    }

    @Test
    void volumeIsTheBoundingBoxVolume() {
        // ConvexPolyhedralRegion does not compute its exact volume; it reports the bounding box.
        assertEquals(125, tetrahedron(4).getVolume());
    }
}
