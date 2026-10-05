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
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Cave carver")
class CaveCarverTest extends BuildTestBase {

    private static final CuboidRegion REGION = new CuboidRegion(BlockVector3.at(-12, -12, -12), BlockVector3.at(12, 12, 12));

    private static TestExtent solid() {
        TestExtent extent = new TestExtent();
        extent.fill(-16, -16, -16, 16, 16, 16, stone);
        return extent;
    }

    @Test
    @DisplayName("worms are continuous and stay inside the region")
    void wormsStayInside() {
        CaveCarver carver = new CaveCarver(2, 200, 42);
        List<List<Vector3>> worms = carver.getWorms(REGION, 3);
        assertEquals(3, worms.size());
        assertEquals(REGION.getCenter(), worms.get(0).get(0));
        for (List<Vector3> worm : worms) {
            assertEquals(200, worm.size());
            for (int i = 0; i < worm.size(); i++) {
                Vector3 point = worm.get(i);
                assertTrue(point.x() >= -12.0001 && point.x() <= 12.0001
                    && point.y() >= -12.0001 && point.y() <= 12.0001
                    && point.z() >= -12.0001 && point.z() <= 12.0001, "Worm left the region at " + point);
                if (i > 0) {
                    assertTrue(point.distance(worm.get(i - 1)) <= 1.0001, "Worm jumped at step " + i);
                }
            }
        }
    }

    @Test
    @DisplayName("carves air only inside the region, around the worm")
    void carvesInsideRegion() throws Exception {
        TestExtent extent = solid();
        CaveCarver carver = new CaveCarver(2, 60, 7);
        int affected = carver.carve(extent, REGION, 1, air);

        Set<BlockVector3> carved = extent.positionsOf(air);
        assertEquals(carved.size(), affected);
        assertTrue(carved.contains(BlockVector3.ZERO));
        assertTrue(carved.size() > 60, "The cave is too small: " + carved.size());
        for (BlockVector3 pos : carved) {
            assertTrue(REGION.contains(pos), "Carved outside of the region at " + pos);
        }
        // Each carved block is near the centre line
        List<Vector3> worm = carver.getWorms(REGION, 1).get(0);
        for (BlockVector3 pos : carved) {
            double nearest = Double.MAX_VALUE;
            for (Vector3 point : worm) {
                nearest = Math.min(nearest, pos.toVector3().distance(point));
            }
            assertTrue(nearest <= 2 + 1e-6, "Carved too far from the worm at " + pos);
        }
    }

    @Test
    @DisplayName("the same seed gives the same cave")
    void deterministic() throws Exception {
        TestExtent first = solid();
        TestExtent second = solid();
        new CaveCarver(1.5, 80, 99).carve(first, REGION, 2, air);
        new CaveCarver(1.5, 80, 99).carve(second, REGION, 2, air);
        assertEquals(first.positionsOf(air), second.positionsOf(air));
    }

    @Test
    @DisplayName("the radius varies within bounds")
    void radiusBounds() {
        CaveCarver carver = new CaveCarver(4, 10, 0);
        for (int step = 0; step < 100; step++) {
            double radius = carver.getRadiusAt(step);
            assertTrue(radius >= 4 * 0.7 - 1e-9 && radius <= 4 + 1e-9);
        }
        assertThrows(IllegalArgumentException.class, () -> new CaveCarver(0.1, 10, 0));
        assertThrows(IllegalArgumentException.class, () -> new CaveCarver(2, 0, 0));
    }

}
