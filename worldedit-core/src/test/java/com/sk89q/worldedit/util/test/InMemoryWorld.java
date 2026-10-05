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

package com.sk89q.worldedit.util.test;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.SideEffectSet;
import com.sk89q.worldedit.util.collection.BlockMap;
import com.sk89q.worldedit.world.NullWorld;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.util.Map;

/**
 * A {@link World} that stores blocks in memory, for running
 * {@link com.sk89q.worldedit.EditSession} operations in tests.
 */
public final class InMemoryWorld extends NullWorld {

    private final Map<BlockVector3, BlockState> blocks = BlockMap.create();
    private final BlockState defaultBlock;
    private final int minY;
    private final int maxY;
    private long blockReads;

    public InMemoryWorld(BlockState defaultBlock, int minY, int maxY) {
        this.defaultBlock = defaultBlock;
        this.minY = minY;
        this.maxY = maxY;
    }

    public World world() {
        return this;
    }

    public Map<BlockVector3, BlockState> blocks() {
        return blocks;
    }

    /**
     * Get the number of times a block was read from this world.
     *
     * @return the number of reads
     */
    public long getBlockReads() {
        return blockReads;
    }

    @Override
    public String getName() {
        return "in-memory";
    }

    @Override
    public String id() {
        return "in-memory";
    }

    @Override
    public int getMinY() {
        return minY;
    }

    @Override
    public int getMaxY() {
        return maxY;
    }

    @Override
    public BlockState getBlock(BlockVector3 position) {
        blockReads++;
        return blocks.getOrDefault(position, defaultBlock);
    }

    @Override
    public BaseBlock getFullBlock(BlockVector3 position) {
        return getBlock(position).toBaseBlock();
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 position, B block, SideEffectSet sideEffects) {
        blocks.put(position, block.toImmutableState());
        return true;
    }
}
