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

package com.sk89q.worldedit.extent.clipboard;

import com.sk89q.worldedit.BaseWorldEditTest;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.EllipsoidRegion;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Block array clipboard")
class BlockArrayClipboardTest extends BaseWorldEditTest {

    private static BlockState[] states;

    @BeforeAll
    static void setUpBlocks() {
        states = new BlockState[7];
        for (int i = 0; i < states.length; i++) {
            states[i] = new BlockType("clipboardtest:block_" + i).getDefaultState();
        }
    }

    private static BlockState stateFor(BlockVector3 position) {
        return states[Math.floorMod(position.x() * 5 + position.y() * 3 + position.z(), states.length)];
    }

    @Test
    @DisplayName("stores and returns blocks at every position of a cuboid")
    void storesBlocksInCuboid() {
        CuboidRegion region = new CuboidRegion(BlockVector3.at(-3, -70, 10), BlockVector3.at(4, -60, 15));
        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
        assertEquals(region.getMinimumPoint(), clipboard.getMinimumPoint());
        assertEquals(region.getMaximumPoint(), clipboard.getMaximumPoint());
        assertEquals(BlockVector3.at(8, 11, 6), clipboard.getDimensions());

        for (BlockVector3 position : region) {
            assertTrue(clipboard.setBlock(position, stateFor(position)));
        }
        for (BlockVector3 position : region) {
            assertSame(stateFor(position), clipboard.getBlock(position));
            assertSame(stateFor(position), clipboard.getFullBlock(position).toImmutableState());
        }
    }

    @Test
    @DisplayName("refuses blocks outside the region")
    void refusesOutsideBlocks() {
        CuboidRegion region = new CuboidRegion(BlockVector3.at(0, 0, 0), BlockVector3.at(3, 3, 3));
        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
        assertFalse(clipboard.setBlock(BlockVector3.at(4, 0, 0), states[0]));
        assertFalse(clipboard.setBlock(BlockVector3.at(0, -1, 0), states[0]));
        assertFalse(clipboard.setBlock(BlockVector3.at(0, 0, 4), states[0]));
    }

    @Test
    @DisplayName("respects the shape of a non-cuboid region")
    void respectsNonCuboidRegion() {
        EllipsoidRegion region = new EllipsoidRegion(BlockVector3.ZERO, Vector3.at(3, 3, 3));
        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
        // A corner of the bounding box is outside the sphere
        assertFalse(clipboard.setBlock(region.getMaximumPoint(), states[0]));
        assertTrue(clipboard.setBlock(BlockVector3.ZERO, states[1]));
        assertSame(states[1], clipboard.getBlock(BlockVector3.ZERO));
    }

    @Test
    @DisplayName("stores biomes inside the bounding box")
    void storesBiomes() {
        CuboidRegion region = new CuboidRegion(BlockVector3.at(-2, 0, -2), BlockVector3.at(2, 2, 2));
        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
        assertFalse(clipboard.hasBiomes());
        BiomeType biome = new BiomeType("clipboardtest:biome");
        assertTrue(clipboard.setBiome(BlockVector3.at(-2, 1, 2), biome));
        assertFalse(clipboard.setBiome(BlockVector3.at(3, 1, 2), biome));
        assertTrue(clipboard.hasBiomes());
        assertSame(biome, clipboard.getBiome(BlockVector3.at(-2, 1, 2)));
    }
}
