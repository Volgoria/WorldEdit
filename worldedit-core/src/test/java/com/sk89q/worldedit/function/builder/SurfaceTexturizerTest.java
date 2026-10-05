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

import com.sk89q.worldedit.function.mask.BlockTypeMask;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Surface texturizer")
class SurfaceTexturizerTest extends BuildTestBase {

    private static final CuboidRegion REGION = new CuboidRegion(BlockVector3.at(-5, -5, -5), BlockVector3.at(5, 5, 5));

    private static TestExtent cube() {
        TestExtent extent = new TestExtent();
        extent.fill(0, 0, 0, 4, 4, 4, stone);
        return extent;
    }

    @Test
    @DisplayName("finds the exposed blocks of a solid")
    void exposedBlocks() {
        TestExtent extent = cube();
        assertEquals(125 - 27, SurfaceTexturizer.getExposed(extent, REGION, null).size());
        assertTrue(SurfaceTexturizer.isExposed(extent, BlockVector3.at(0, 2, 2)));
        assertFalse(SurfaceTexturizer.isExposed(extent, BlockVector3.at(2, 2, 2)));
        assertFalse(SurfaceTexturizer.isExposed(extent, BlockVector3.at(-1, 2, 2)));
    }

    @Test
    @DisplayName("replaces every exposed block at 100%, and none at 0%")
    void extremes() throws Exception {
        TestExtent full = cube();
        assertEquals(98, SurfaceTexturizer.apply(full, REGION, gold, 1, null, new Random(1)));
        assertEquals(98, full.positionsOf(gold).size());
        assertEquals(27, full.positionsOf(stone).size());
        assertEquals(stone, full.getBlock(BlockVector3.at(2, 2, 2)));

        TestExtent none = cube();
        assertEquals(0, SurfaceTexturizer.apply(none, REGION, gold, 0, null, new Random(1)));
        assertEquals(125, none.positionsOf(stone).size());
    }

    @Test
    @DisplayName("replaces about the requested fraction, and respects the mask")
    void fractionAndMask() throws Exception {
        TestExtent extent = cube();
        int affected = SurfaceTexturizer.apply(extent, REGION, gold, 0.5, null, new Random(3));
        assertTrue(affected > 25 && affected < 75, "Unexpected count " + affected);

        TestExtent masked = cube();
        masked.put(BlockVector3.at(0, 0, 0), dirt);
        SurfaceTexturizer.apply(masked, REGION, gold, 1, new BlockTypeMask(masked, dirt.getBlockType()), new Random(1));
        assertEquals(1, masked.positionsOf(gold).size());
        assertEquals(gold, masked.getBlock(BlockVector3.at(0, 0, 0)));
    }

    @Test
    @DisplayName("rejects fractions outside of [0, 1]")
    void invalidFraction() {
        assertThrows(IllegalArgumentException.class,
            () -> SurfaceTexturizer.apply(cube(), REGION, gold, 1.5, null, new Random()));
    }

}
