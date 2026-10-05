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

package com.sk89q.worldedit.function.block;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.visitor.LayerVisitor;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SurfaceLayerFunctionTest {

    private final Set<BlockVector3> changed = new HashSet<>();

    /**
     * Terrain whose surface height is {@code x}, i.e. a staircase rising along X,
     * with a one block cave at (2, 1, 0).
     */
    private static final Mask TERRAIN = position ->
        position.y() <= position.x() && !position.equals(BlockVector3.at(2, 1, 0));

    private int run(int depth, CuboidRegion region) throws WorldEditException {
        SurfaceLayerFunction function = new SurfaceLayerFunction(TERRAIN, depth, position -> changed.add(position));
        LayerVisitor visitor = new LayerVisitor(region, region.getMinimumY(), region.getMaximumY(), function);
        Operations.complete(visitor);
        return function.getAffected();
    }

    @Test
    void replacesOnlyTopLayer() throws WorldEditException {
        CuboidRegion region = new CuboidRegion(BlockVector3.at(0, 0, 0), BlockVector3.at(4, 10, 0));
        int affected = run(1, region);

        assertEquals(5, affected);
        for (int x = 0; x <= 4; x++) {
            assertTrue(changed.contains(BlockVector3.at(x, x, 0)), "top of column " + x);
        }
    }

    @Test
    void replacesRequestedDepthAndSkipsCaves() throws WorldEditException {
        CuboidRegion region = new CuboidRegion(BlockVector3.at(0, 0, 0), BlockVector3.at(4, 10, 0));
        run(3, region);

        Set<BlockVector3> expected = new HashSet<>();
        for (int x = 0; x <= 4; x++) {
            for (int y = x; y > x - 3 && y >= 0; y--) {
                expected.add(BlockVector3.at(x, y, 0));
            }
        }
        // The cave is within the depth of column 2 but is not ground
        expected.remove(BlockVector3.at(2, 1, 0));
        assertEquals(expected, changed);
    }

    @Test
    void doesNotGoBelowRegion() throws WorldEditException {
        CuboidRegion region = new CuboidRegion(BlockVector3.at(4, 3, 0), BlockVector3.at(4, 10, 0));
        run(5, region);
        assertEquals(Set.of(BlockVector3.at(4, 4, 0), BlockVector3.at(4, 3, 0)), changed);
    }

    @Test
    void skipsBuriedColumns() throws WorldEditException {
        // The top of the region is below the terrain surface in column 4, so nothing is exposed
        CuboidRegion region = new CuboidRegion(BlockVector3.at(4, 0, 0), BlockVector3.at(4, 2, 0));
        assertEquals(0, run(2, region));
        assertFalse(changed.contains(BlockVector3.at(4, 2, 0)));
    }

    @Test
    void rejectsInvalidDepth() {
        assertThrows(IllegalArgumentException.class, () -> new SurfaceLayerFunction(TERRAIN, 0, _ -> true));
    }
}
