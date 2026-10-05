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

package com.sk89q.worldedit.extent.buffer;

import com.sk89q.worldedit.BaseWorldEditTest;
import com.sk89q.worldedit.extent.NullExtent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ForgetfulExtentBufferTest extends BaseWorldEditTest {

    @Test
    void emptyBufferHasZeroBounds() {
        Region region = new ForgetfulExtentBuffer(new NullExtent()).asRegion();
        assertEquals(BlockVector3.ZERO, region.getMinimumPoint());
        assertEquals(BlockVector3.ZERO, region.getMaximumPoint());
    }

    @Test
    void boundsCoverEveryChangedPosition() throws Exception {
        BlockState stone = new BlockType("buffertest:stone").getDefaultState();
        ForgetfulExtentBuffer buffer = new ForgetfulExtentBuffer(new NullExtent());
        Region region = buffer.asRegion();

        buffer.setBlock(BlockVector3.at(5, 6, 7), stone);
        assertEquals(BlockVector3.at(5, 6, 7), region.getMinimumPoint());
        assertEquals(BlockVector3.at(5, 6, 7), region.getMaximumPoint());

        buffer.setBlock(BlockVector3.at(-3, 10, 7), stone);
        assertEquals(BlockVector3.at(-3, 6, 7), region.getMinimumPoint());
        assertEquals(BlockVector3.at(5, 10, 7), region.getMaximumPoint());

        // inside the bounds: no change
        buffer.setBlock(BlockVector3.at(0, 8, 7), stone);
        assertEquals(BlockVector3.at(-3, 6, 7), region.getMinimumPoint());
        assertEquals(BlockVector3.at(5, 10, 7), region.getMaximumPoint());

        // biomes count too
        buffer.setBiome(BlockVector3.at(1, -64, 100), new BiomeType("buffertest:plains"));
        assertEquals(BlockVector3.at(-3, -64, 7), region.getMinimumPoint());
        assertEquals(BlockVector3.at(5, 10, 100), region.getMaximumPoint());
        assertEquals(BlockVector3.at(-3, -64, 7), buffer.asRegion().getMinimumPoint());
    }
}
