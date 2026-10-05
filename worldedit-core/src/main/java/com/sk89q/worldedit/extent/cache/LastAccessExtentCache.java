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

package com.sk89q.worldedit.extent.cache;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import javax.annotation.Nullable;

/**
 * Returns the same cached {@link BlockState} for repeated calls to
 * {@link #getBlock(BlockVector3)} with the same position.
 */
public class LastAccessExtentCache extends AbstractDelegateExtent {

    // The last block and full block read, each with its position (null when
    // nothing is cached). Plain fields rather than holder objects, so that a
    // cache miss does not allocate: every block an EditSession changes misses.
    private @Nullable BlockVector3 lastBlockPosition;
    private BlockState lastBlock;
    private @Nullable BlockVector3 lastFullBlockPosition;
    private BaseBlock lastFullBlock;

    /**
     * Create a new instance.
     *
     * @param extent the extent
     */
    public LastAccessExtentCache(Extent extent) {
        super(extent);
    }

    @Override
    public BlockState getBlock(BlockVector3 position) {
        BlockVector3 lastBlockPosition = this.lastBlockPosition;
        BlockVector3 lastFullBlockPosition = this.lastFullBlockPosition;
        if (lastBlockPosition != null && lastBlockPosition.equals(position)) {
            return lastBlock;
        } else if (lastFullBlockPosition != null && lastFullBlockPosition.equals(position)) {
            return lastFullBlock.toImmutableState();
        } else {
            BlockState block = super.getBlock(position);
            this.lastBlockPosition = position;
            this.lastBlock = block;
            return block;
        }
    }

    @Override
    public BaseBlock getFullBlock(BlockVector3 position) {
        BlockVector3 lastFullBlockPosition = this.lastFullBlockPosition;
        if (lastFullBlockPosition != null && lastFullBlockPosition.equals(position)) {
            return lastFullBlock;
        } else {
            BaseBlock block = super.getFullBlock(position);
            this.lastFullBlockPosition = position;
            this.lastFullBlock = block;
            return block;
        }
    }

    @Override
    public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 location, T block) throws WorldEditException {
        if (super.setBlock(location, block)) {
            if (lastFullBlockPosition != null && lastFullBlockPosition.equals(location)) {
                this.lastFullBlockPosition = location;
                this.lastFullBlock = block.toBaseBlock();
            }
            if (lastBlockPosition != null && lastBlockPosition.equals(location)) {
                this.lastBlockPosition = location;
                this.lastBlock = block.toImmutableState();
            }

            return true;
        }
        return false;
    }

}
