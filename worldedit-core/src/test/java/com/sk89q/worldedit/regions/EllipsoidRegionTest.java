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
import com.sk89q.worldedit.math.Vector3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("An ellipsoid region")
public class EllipsoidRegionTest extends BaseWorldEditTest {

    private static Set<BlockVector3> bruteForce(Region region) {
        BlockVector3 min = region.getMinimumPoint().subtract(2, 2, 2);
        BlockVector3 max = region.getMaximumPoint().add(2, 2, 2);
        Set<BlockVector3> result = new HashSet<>();
        for (int x = min.x(); x <= max.x(); x++) {
            for (int y = min.y(); y <= max.y(); y++) {
                for (int z = min.z(); z <= max.z(); z++) {
                    BlockVector3 pos = BlockVector3.at(x, y, z);
                    if (region.contains(pos)) {
                        result.add(pos);
                    }
                }
            }
        }
        return result;
    }

    @ParameterizedTest
    @CsvSource({
        "0, 0, 0, 0, 0, 0",
        "0, 0, 0, 1, 1, 1",
        "10, 64, -10, 4, 4, 4",
        "-3, -60, 7, 2, 5, 3",
        "0, 0, 0, 7, 1, 2",
    })
    @DisplayName("iterates exactly the contained positions, each once, symmetrically")
    void iterationMatchesContains(int cx, int cy, int cz, int rx, int ry, int rz) {
        BlockVector3 center = BlockVector3.at(cx, cy, cz);
        EllipsoidRegion region = new EllipsoidRegion(center, Vector3.at(rx, ry, rz));
        Set<BlockVector3> seen = new HashSet<>();
        for (BlockVector3 pos : region) {
            assertTrue(region.contains(pos), () -> "iterated position not contained: " + pos);
            assertTrue(seen.add(pos), () -> "duplicate position " + pos);
        }
        assertEquals(bruteForce(region), seen);
        assertTrue(seen.contains(center));
        for (BlockVector3 pos : seen) {
            BlockVector3 mirrored = center.multiply(2).subtract(pos);
            assertTrue(seen.contains(mirrored), () -> "not symmetric: " + pos + " vs " + mirrored);
        }
        // the extreme points along each axis are part of the shape
        assertTrue(seen.contains(center.add(rx, 0, 0)));
        assertTrue(seen.contains(center.add(0, -ry, 0)));
        assertTrue(seen.contains(center.add(0, 0, rz)));
        assertFalse(seen.contains(center.add(rx + 1, 0, 0)));
    }

    @ParameterizedTest
    @CsvSource({
        "3, 3, 3",
        "5, 5, 5",
        "8, 3, 6",
        "12, 12, 12",
    })
    @DisplayName("volume estimate is close to the actual block count")
    void volumeEstimate(int rx, int ry, int rz) {
        EllipsoidRegion region = new EllipsoidRegion(BlockVector3.ZERO, Vector3.at(rx, ry, rz));
        long actual = bruteForce(region).size();
        long estimate = region.getVolume();
        assertEquals((long) Math.floor(4.0 / 3.0 * Math.PI * (rx + 0.5) * (ry + 0.5) * (rz + 0.5)), estimate);
        assertTrue(Math.abs(actual - estimate) <= 0.1 * actual,
            () -> "estimate " + estimate + " too far from actual " + actual);
    }

    @Test
    void boundsAndDimensions() {
        EllipsoidRegion region = new EllipsoidRegion(BlockVector3.at(1, 2, 3), Vector3.at(4, 5, 6));
        assertEquals(BlockVector3.at(-3, -3, -3), region.getMinimumPoint());
        assertEquals(BlockVector3.at(5, 7, 9), region.getMaximumPoint());
        assertEquals(9, region.getWidth());
        assertEquals(11, region.getHeight());
        assertEquals(13, region.getLength());
        assertEquals(Vector3.at(1, 2, 3), region.getCenter());
        assertEquals(Vector3.at(4, 5, 6), region.getRadius());
    }

    @Test
    void shiftMovesCenterOnly() {
        EllipsoidRegion region = new EllipsoidRegion(BlockVector3.ZERO, Vector3.at(2, 3, 4));
        long volume = region.getVolume();
        region.shift(BlockVector3.at(10, -20, 30));
        assertEquals(Vector3.at(10, -20, 30), region.getCenter());
        assertEquals(Vector3.at(2, 3, 4), region.getRadius());
        assertEquals(volume, region.getVolume());
        assertTrue(region.contains(BlockVector3.at(12, -20, 30)));
        assertFalse(region.contains(BlockVector3.ZERO));
    }

    @Test
    void expandAndContract() throws RegionOperationException {
        EllipsoidRegion region = new EllipsoidRegion(BlockVector3.ZERO, Vector3.at(3, 3, 3));
        region.expand(BlockVector3.at(0, 4, 0));
        assertEquals(Vector3.at(3, 5, 3), region.getRadius());
        assertEquals(Vector3.at(0, 2, 0), region.getCenter());

        region.contract(BlockVector3.at(0, 4, 0));
        assertEquals(Vector3.at(3, 3, 3), region.getRadius());
        assertEquals(Vector3.at(0, 0, 0), region.getCenter());

        // contracting clamps to a minimum radius of 1
        region.contract(BlockVector3.at(20, 0, 0), BlockVector3.at(-20, 0, 0));
        assertEquals(Vector3.at(1, 3, 3), region.getRadius());
    }

    @Test
    void oddChangeIsRejected() {
        EllipsoidRegion region = new EllipsoidRegion(BlockVector3.ZERO, Vector3.at(3, 3, 3));
        assertThrows(RegionOperationException.class, () -> region.expand(BlockVector3.at(0, 1, 0)));
        assertThrows(RegionOperationException.class, () -> region.contract(BlockVector3.at(3, 0, 0)));
    }

    @Test
    void extendRadiusNeverShrinks() {
        EllipsoidRegion region = new EllipsoidRegion(BlockVector3.ZERO, Vector3.at(5, 1, 3));
        region.extendRadius(Vector3.at(2, 4, 3));
        assertEquals(Vector3.at(5, 4, 3), region.getRadius());
    }

    @Test
    void cloneIsIndependent() {
        EllipsoidRegion region = new EllipsoidRegion(BlockVector3.ZERO, Vector3.at(2, 2, 2));
        EllipsoidRegion copy = region.clone();
        copy.setCenter(BlockVector3.at(9, 9, 9));
        copy.setRadius(Vector3.at(1, 1, 1));
        assertEquals(Vector3.ZERO, region.getCenter());
        assertEquals(Vector3.at(2, 2, 2), region.getRadius());
    }

    @Test
    void chunksOnlyIncludeColumnsTouchedByTheEquator() {
        EllipsoidRegion region = new EllipsoidRegion(BlockVector3.at(16, 0, 16), Vector3.at(2, 2, 2));
        assertEquals(
            Set.of(BlockVector2.at(0, 0), BlockVector2.at(0, 1), BlockVector2.at(1, 0), BlockVector2.at(1, 1)),
            region.getChunks()
        );
        EllipsoidRegion inner = new EllipsoidRegion(BlockVector3.at(8, 0, 8), Vector3.at(3, 3, 3));
        assertEquals(Set.of(BlockVector2.at(0, 0)), inner.getChunks());
    }

    private static Set<BlockVector3> scanContains(Region region, BlockVector3 center, int margin) {
        Set<BlockVector3> result = new HashSet<>();
        for (int x = center.x() - margin; x <= center.x() + margin; x++) {
            for (int y = center.y() - margin; y <= center.y() + margin; y++) {
                for (int z = center.z() - margin; z <= center.z() + margin; z++) {
                    BlockVector3 pos = BlockVector3.at(x, y, z);
                    if (region.contains(pos)) {
                        result.add(pos);
                    }
                }
            }
        }
        return result;
    }

    @ParameterizedTest
    @CsvSource({
        "0, 0, 0, 2.7, 2.7, 2.7",
        "0, 0, 0, 2.3, 2.3, 2.3",
        "0, 0, 0, 2.5, 2.5, 2.5",
        "0, 0, 0, 0.4, 0.6, 0.5",
        "5, 64, -5, 3.2, 1.5, 4.9",
        "-11, -30, 17, 6.75, 2.1, 0.99",
    })
    @DisplayName("with fractional radii, iterates exactly the positions contains accepts, symmetrically")
    void fractionalRadiiIterationMatchesContains(int cx, int cy, int cz, double rx, double ry, double rz) {
        BlockVector3 center = BlockVector3.at(cx, cy, cz);
        EllipsoidRegion region = new EllipsoidRegion(center, Vector3.at(rx, ry, rz));
        Set<BlockVector3> iterated = new HashSet<>();
        for (BlockVector3 pos : region) {
            assertTrue(iterated.add(pos), () -> "duplicate position " + pos);
        }
        int margin = (int) Math.ceil(Math.max(rx, Math.max(ry, rz))) + 3;
        Set<BlockVector3> expected = scanContains(region, center, margin);
        assertEquals(expected, iterated);
        for (BlockVector3 pos : iterated) {
            BlockVector3 mirrored = center.multiply(2).subtract(pos);
            assertTrue(iterated.contains(mirrored), () -> "not symmetric: " + pos + " vs " + mirrored);
        }
        // the bounds are tight and symmetric around the center
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        assertEquals(center.subtract(min), max.subtract(center));
        assertEquals(min, iterated.stream().reduce(BlockVector3::getMinimum).orElseThrow());
        assertEquals(max, iterated.stream().reduce(BlockVector3::getMaximum).orElseThrow());
        assertEquals(max.x() - min.x() + 1, region.getWidth());
        assertEquals(max.y() - min.y() + 1, region.getHeight());
        assertEquals(max.z() - min.z() + 1, region.getLength());
    }

    @ParameterizedTest
    @CsvSource({
        "0, 0, 0, 0, 0, 0",
        "0, 0, 0, 1, 2, 3",
        "7, -64, 3, 5, 5, 5",
        "-100, 300, 42, 12, 1, 9",
    })
    @DisplayName("with integer radii, keeps the historical bounds of center +/- radius")
    void integerRadiiKeepHistoricalBounds(int cx, int cy, int cz, int rx, int ry, int rz) {
        BlockVector3 center = BlockVector3.at(cx, cy, cz);
        Vector3 radius = Vector3.at(rx, ry, rz);
        EllipsoidRegion region = new EllipsoidRegion(center, radius);
        // the previous implementation floored center -/+ radius
        assertEquals(center.toVector3().subtract(radius).toBlockPoint(), region.getMinimumPoint());
        assertEquals(center.toVector3().add(radius).toBlockPoint(), region.getMaximumPoint());
        assertEquals(2 * rx + 1, region.getWidth());
        assertEquals(2 * ry + 1, region.getHeight());
        assertEquals(2 * rz + 1, region.getLength());
    }
}
