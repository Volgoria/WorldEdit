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
import com.sk89q.worldedit.math.Vector2;
import com.sk89q.worldedit.math.Vector3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("A cylinder region")
public class CylinderRegionTest extends BaseWorldEditTest {

    private static long bruteForceCount(Region region) {
        BlockVector3 min = region.getMinimumPoint().subtract(2, 2, 2);
        BlockVector3 max = region.getMaximumPoint().add(2, 2, 2);
        long count = 0;
        for (int x = min.x(); x <= max.x(); x++) {
            for (int y = min.y(); y <= max.y(); y++) {
                for (int z = min.z(); z <= max.z(); z++) {
                    if (region.contains(BlockVector3.at(x, y, z))) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    @ParameterizedTest
    @CsvSource({
        "0, 0, 0, 0, 0, 0, 0",
        "0, 0, 0, 1, 1, 0, 3",
        "5, 64, -5, 3, 3, 60, 70",
        "-7, 0, 9, 2, 5, -10, -6",
        "100, 10, 100, 6, 6, 10, 12",
    })
    @DisplayName("iterates exactly the contained positions, each once")
    void iterationMatchesContains(int cx, int cy, int cz, int rx, int rz, int minY, int maxY) {
        CylinderRegion region = new CylinderRegion(BlockVector3.at(cx, cy, cz), Vector2.at(rx, rz), minY, maxY);
        Set<BlockVector3> seen = new HashSet<>();
        for (BlockVector3 pos : region) {
            assertTrue(region.contains(pos), () -> "iterated position not contained: " + pos);
            assertTrue(seen.add(pos), () -> "duplicate position " + pos);
        }
        assertEquals(bruteForceCount(region), seen.size());
        // the shape is symmetric around its center in X and Z
        for (BlockVector3 pos : seen) {
            BlockVector3 mirrored = BlockVector3.at(2 * cx - pos.x(), pos.y(), 2 * cz - pos.z());
            assertTrue(seen.contains(mirrored), () -> "not symmetric: " + pos + " vs " + mirrored);
        }
    }

    @ParameterizedTest
    @CsvSource({
        "3, 3, 1",
        "5, 5, 10",
        "10, 4, 3",
        "20, 20, 2",
    })
    @DisplayName("volume estimate is close to the actual block count")
    void volumeEstimate(int rx, int rz, int height) {
        CylinderRegion region = new CylinderRegion(BlockVector3.ZERO, Vector2.at(rx, rz), 0, height - 1);
        long actual = bruteForceCount(region);
        long estimate = region.getVolume();
        assertEquals((long) Math.floor(Math.PI * (rx + 0.5) * (rz + 0.5) * height), estimate);
        assertTrue(Math.abs(actual - estimate) <= 0.1 * actual,
            () -> "estimate " + estimate + " too far from actual " + actual);
    }

    @Test
    void dimensionsAndBounds() {
        CylinderRegion region = new CylinderRegion(BlockVector3.at(10, 0, -10), Vector2.at(3, 5), -4, 4);
        assertEquals(Vector2.at(3, 5), region.getRadius());
        assertEquals(BlockVector3.at(7, -4, -15), region.getMinimumPoint());
        assertEquals(BlockVector3.at(13, 4, -5), region.getMaximumPoint());
        assertEquals(7, region.getWidth());
        assertEquals(9, region.getHeight());
        assertEquals(11, region.getLength());
        assertEquals(Vector3.at(10, 0, -10), region.getCenter());
    }

    @Test
    void containsRespectsHeight() {
        CylinderRegion region = new CylinderRegion(BlockVector3.ZERO, Vector2.at(2, 2), 0, 3);
        assertTrue(region.contains(BlockVector3.at(0, 0, 0)));
        assertTrue(region.contains(BlockVector3.at(2, 3, 0)));
        assertFalse(region.contains(BlockVector3.at(0, -1, 0)));
        assertFalse(region.contains(BlockVector3.at(0, 4, 0)));
        assertFalse(region.contains(BlockVector3.at(2, 0, 2)));
        assertFalse(region.contains(BlockVector3.at(3, 0, 0)));
    }

    @Test
    void shiftMovesCenterAndHeight() {
        CylinderRegion region = new CylinderRegion(BlockVector3.ZERO, Vector2.at(2, 2), 0, 3);
        region.shift(BlockVector3.at(5, -2, 7));
        assertEquals(Vector3.at(5, -0.5, 7), region.getCenter());
        assertEquals(-2, region.getMinimumY());
        assertEquals(1, region.getMaximumY());
        assertTrue(region.contains(BlockVector3.at(7, -2, 7)));
        assertFalse(region.contains(BlockVector3.at(2, 0, 0)));
    }

    @Test
    void expandHorizontallyGrowsRadiusAndShiftsCenter() throws RegionOperationException {
        CylinderRegion region = new CylinderRegion(BlockVector3.ZERO, Vector2.at(2, 2), 0, 0);
        region.expand(BlockVector3.at(2, 0, 0));
        assertEquals(Vector2.at(3, 2), region.getRadius());
        assertEquals(Vector3.at(1, 0, 0), region.getCenter());

        // symmetric expansion keeps the center in place
        region.expand(BlockVector3.at(0, 0, 2), BlockVector3.at(0, 0, -2));
        assertEquals(Vector2.at(3, 4), region.getRadius());
        assertEquals(Vector3.at(1, 0, 0), region.getCenter());
    }

    @Test
    void expandVertically() throws RegionOperationException {
        CylinderRegion region = new CylinderRegion(BlockVector3.ZERO, Vector2.at(2, 2), 0, 0);
        region.expand(BlockVector3.at(0, 5, 0), BlockVector3.at(0, -3, 0));
        assertEquals(-3, region.getMinimumY());
        assertEquals(5, region.getMaximumY());
    }

    @Test
    void oddHorizontalChangeIsRejected() {
        CylinderRegion region = new CylinderRegion(BlockVector3.ZERO, Vector2.at(2, 2), 0, 0);
        assertThrows(RegionOperationException.class, () -> region.expand(BlockVector3.at(1, 0, 0)));
        assertThrows(RegionOperationException.class, () -> region.contract(BlockVector3.at(0, 0, 3)));
    }

    @Test
    void contractClampsRadiusAndHeight() throws RegionOperationException {
        CylinderRegion region = new CylinderRegion(BlockVector3.ZERO, Vector2.at(2, 2), 0, 4);
        region.contract(BlockVector3.at(10, 0, 10), BlockVector3.at(-10, 0, -10));
        assertEquals(Vector2.at(1, 1), region.getRadius());

        region.contract(BlockVector3.at(0, 100, 0));
        assertEquals(4, region.getMinimumY());
        assertEquals(4, region.getMaximumY());
    }

    @Test
    void setYExtendsHeightOnlyOutward() {
        CylinderRegion region = new CylinderRegion();
        assertTrue(region.setY(10));
        assertEquals(10, region.getMinimumY());
        assertEquals(10, region.getMaximumY());
        assertFalse(region.setY(10));
        assertTrue(region.setY(4));
        assertTrue(region.setY(12));
        assertFalse(region.setY(8));
        assertEquals(4, region.getMinimumY());
        assertEquals(12, region.getMaximumY());
    }

    @Test
    void extendRadiusNeverShrinks() {
        CylinderRegion region = new CylinderRegion(BlockVector3.ZERO, Vector2.at(4, 2), 0, 0);
        region.extendRadius(Vector2.at(1, 6));
        assertEquals(Vector2.at(4, 6), region.getRadius());
    }

    @Test
    void copyConstructorIsIndependent() {
        CylinderRegion region = new CylinderRegion(BlockVector3.at(1, 2, 3), Vector2.at(4, 5), 0, 6);
        CylinderRegion copy = new CylinderRegion(region);
        assertEquals(region.getCenter(), copy.getCenter());
        assertEquals(region.getRadius(), copy.getRadius());
        assertEquals(region.getMinimumPoint(), copy.getMinimumPoint());
        assertEquals(region.getMaximumPoint(), copy.getMaximumPoint());
        copy.shift(BlockVector3.at(1, 1, 1));
        assertEquals(BlockVector2.at(1, 3), region.getCenter().toVector2().toBlockPoint());
    }

    @Test
    void polygonizeStaysWithinFootprint() {
        CylinderRegion region = new CylinderRegion(BlockVector3.ZERO, Vector2.at(10, 10), 0, 0);
        List<BlockVector2> points = region.polygonize(-1);
        // vertices are floored to block coordinates, so they may be up to one block further out
        double limit = 10.5 + Math.sqrt(2);
        for (BlockVector2 point : points) {
            assertTrue(point.toVector2().length() <= limit, () -> "point too far outside cylinder: " + point);
        }
        // n = ceil(pi * |(10.5, 10.5)|)
        assertEquals((int) Math.ceil(Math.PI * Math.hypot(10.5, 10.5)), points.size());
        // a limit caps the number of points at one less than the limit
        assertEquals(15, region.polygonize(16).size());
    }

    @ParameterizedTest
    @CsvSource({
        "0, 0, 0, 2.7, 2.7",
        "0, 0, 0, 2.3, 2.3",
        "0, 0, 0, 2.5, 2.5",
        "0, 0, 0, 0.4, 0.6",
        "5, 64, -5, 3.2, 4.9",
        "-11, -30, 17, 6.75, 0.99",
    })
    @DisplayName("with fractional radii, iterates exactly the positions contains accepts, symmetrically")
    void fractionalRadiiIterationMatchesContains(int cx, int cy, int cz, double rx, double rz) {
        BlockVector3 center = BlockVector3.at(cx, cy, cz);
        CylinderRegion region = new CylinderRegion(center, Vector2.at(rx, rz), cy - 1, cy + 1);
        Set<BlockVector3> iterated = new HashSet<>();
        for (BlockVector3 pos : region) {
            assertTrue(iterated.add(pos), () -> "duplicate position " + pos);
        }
        int margin = (int) Math.ceil(Math.max(rx, rz)) + 3;
        Set<BlockVector3> expected = new HashSet<>();
        for (int x = cx - margin; x <= cx + margin; x++) {
            for (int y = cy - margin; y <= cy + margin; y++) {
                for (int z = cz - margin; z <= cz + margin; z++) {
                    BlockVector3 pos = BlockVector3.at(x, y, z);
                    if (region.contains(pos)) {
                        expected.add(pos);
                    }
                }
            }
        }
        assertEquals(expected, iterated);
        Set<BlockVector2> flat = new HashSet<>();
        region.asFlatRegion().forEach(flat::add);
        assertEquals(expected.stream().map(BlockVector3::toBlockVector2).collect(Collectors.toSet()), flat);
        for (BlockVector3 pos : iterated) {
            BlockVector3 mirrored = BlockVector3.at(2 * cx - pos.x(), pos.y(), 2 * cz - pos.z());
            assertTrue(iterated.contains(mirrored), () -> "not symmetric: " + pos + " vs " + mirrored);
        }
        // the bounds are tight and symmetric around the center
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        assertEquals(cx - min.x(), max.x() - cx);
        assertEquals(cz - min.z(), max.z() - cz);
        assertEquals(min, iterated.stream().reduce(BlockVector3::getMinimum).orElseThrow());
        assertEquals(max, iterated.stream().reduce(BlockVector3::getMaximum).orElseThrow());
        assertEquals(max.x() - min.x() + 1, region.getWidth());
        assertEquals(max.z() - min.z() + 1, region.getLength());
    }

    @ParameterizedTest
    @CsvSource({
        "0, 0, 0, 0, 0",
        "0, 0, 0, 1, 2",
        "7, -64, 3, 5, 5",
        "-100, 300, 42, 12, 1",
    })
    @DisplayName("with integer radii, keeps the historical bounds of center +/- radius")
    void integerRadiiKeepHistoricalBounds(int cx, int cy, int cz, int rx, int rz) {
        BlockVector3 center = BlockVector3.at(cx, cy, cz);
        Vector2 radius = Vector2.at(rx, rz);
        CylinderRegion region = new CylinderRegion(center, radius, cy - 2, cy + 2);
        // the previous implementation floored center -/+ radius
        assertEquals(center.toBlockVector2().toVector2().subtract(radius).toVector3(cy - 2).toBlockPoint(),
            region.getMinimumPoint());
        assertEquals(center.toBlockVector2().toVector2().add(radius).toVector3(cy + 2).toBlockPoint(),
            region.getMaximumPoint());
        assertEquals(2 * rx + 1, region.getWidth());
        assertEquals(2 * rz + 1, region.getLength());
    }
}
