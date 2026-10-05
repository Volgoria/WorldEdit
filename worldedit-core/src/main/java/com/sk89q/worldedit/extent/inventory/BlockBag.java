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

package com.sk89q.worldedit.extent.inventory;

import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;

/**
 * Represents a source to get blocks from and store removed ones.
 */
public abstract class BlockBag {

    /**
     * Stores a block as if it was mined.
     *
     * <p>Blocks that have no item form (such as air, fluids or fire) cannot be
     * stored and are silently discarded.</p>
     *
     * @param blockState the block state
     * @throws BlockBagException on error
     */
    public void storeDroppedBlock(BlockState blockState) throws BlockBagException {
        if (blockState == null) {
            return;
        }
        BlockType blockType = blockState.getBlockType();
        if (blockType.getMaterial().isAir() || !blockType.hasItemType()) {
            return;
        }

        storeBlock(blockState);
    }

    /**
     * Sets a block as if it was placed by hand.
     *
     * <p>Blocks that are replaced during placement (such as air or fluids)
     * do not need to be fetched.</p>
     *
     * @param blockState The block state
     * @throws BlockBagException on error, such as {@link OutOfBlocksException}
     *     if the bag does not contain the block
     */
    public void fetchPlacedBlock(BlockState blockState) throws BlockBagException {
        if (blockState.getBlockType().getMaterial().isReplacedDuringPlacement()) {
            return;
        }
        fetchBlock(blockState);
    }

    /**
     * Get a block.
     *
     * @param blockState the block state
     * @throws BlockBagException on error
     */
    public abstract void fetchBlock(BlockState blockState) throws BlockBagException;

    /**
     * Store a block.
     *
     * @param blockState The block state
     * @throws BlockBagException on error
     */
    public void storeBlock(BlockState blockState) throws BlockBagException {
        this.storeBlock(blockState, 1);
    }

    /**
     * Store a block.
     *
     * @param blockState The block state
     * @param amount The amount
     * @throws BlockBagException on error
     */
    public abstract void storeBlock(BlockState blockState, int amount) throws BlockBagException;

    /**
     * Checks to see if a block exists without removing it.
     *
     * @param blockState the block state
     * @return whether the block exists
     */
    public boolean peekBlock(BlockState blockState) {
        try {
            fetchBlock(blockState);
            storeBlock(blockState);
            return true;
        } catch (BlockBagException _) {
            return false;
        }
    }

    /**
     * Flush any changes. This is called at the end.
     */
    public abstract void flushChanges();

    /**
     * Adds a position to be used a source.
     *
     * @param pos the position
     */
    public abstract void addSourcePosition(Location pos);

    /**
     * Adds a position to be used a source.
     *
     * @param pos the position
     */
    public abstract void addSingleSourcePosition(Location pos);
}
