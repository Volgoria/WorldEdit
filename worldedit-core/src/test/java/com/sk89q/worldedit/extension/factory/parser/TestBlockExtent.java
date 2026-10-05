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

package com.sk89q.worldedit.extension.factory.parser;

import com.sk89q.worldedit.extent.NullExtent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.registry.BlockMaterial;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * An in-memory extent of mocked blocks with controlled materials, so that
 * masks depending on materials can be tested without the bundled block data.
 * Unset positions are air.
 */
public final class TestBlockExtent extends NullExtent {

    /**
     * A mocked block, with its state and full block.
     *
     * @param state the block state
     * @param full the full block
     */
    public record Block(BlockState state, BaseBlock full) {
    }

    public static final Block AIR = block(true, false, false, false);
    public static final Block STONE = block(false, true, true, false);
    public static final Block GLASS = block(false, true, false, false);
    public static final Block WATER = block(false, false, false, true);

    private static Block block(boolean air, boolean solid, boolean opaque, boolean liquid) {
        BlockMaterial material = mock(BlockMaterial.class);
        when(material.isAir()).thenReturn(air);
        when(material.isSolid()).thenReturn(solid);
        when(material.isOpaque()).thenReturn(opaque);
        when(material.isLiquid()).thenReturn(liquid);
        BlockType type = mock(BlockType.class);
        when(type.getMaterial()).thenReturn(material);
        BlockState state = mock(BlockState.class);
        when(state.getBlockType()).thenReturn(type);
        BaseBlock full = mock(BaseBlock.class);
        when(full.getBlockType()).thenReturn(type);
        return new Block(state, full);
    }

    private final Map<BlockVector3, Block> blocks = new HashMap<>();
    private final BlockVector3 min;
    private final BlockVector3 max;

    /**
     * Create an extent with zero bounds.
     */
    public TestBlockExtent() {
        this(BlockVector3.ZERO, BlockVector3.ZERO);
    }

    /**
     * Create an extent with the given bounds, which only affect
     * {@link #getMinimumPoint()} and {@link #getMaximumPoint()}.
     *
     * @param min the minimum point
     * @param max the maximum point
     */
    public TestBlockExtent(BlockVector3 min, BlockVector3 max) {
        this.min = min;
        this.max = max;
    }

    /**
     * Set a block.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     * @param block the block
     * @return this extent
     */
    public TestBlockExtent set(int x, int y, int z, Block block) {
        blocks.put(BlockVector3.at(x, y, z), block);
        return this;
    }

    /**
     * Fill a cuboid, given by inclusive corners, with a block.
     *
     * @param from the minimum corner
     * @param to the maximum corner
     * @param block the block
     * @return this extent
     */
    public TestBlockExtent fill(BlockVector3 from, BlockVector3 to, Block block) {
        for (int x = from.x(); x <= to.x(); x++) {
            for (int y = from.y(); y <= to.y(); y++) {
                for (int z = from.z(); z <= to.z(); z++) {
                    set(x, y, z, block);
                }
            }
        }
        return this;
    }

    /**
     * Get the block at a position.
     *
     * @param x the X coordinate
     * @param y the Y coordinate
     * @param z the Z coordinate
     * @return the block, air if unset
     */
    public Block get(int x, int y, int z) {
        return blocks.getOrDefault(BlockVector3.at(x, y, z), AIR);
    }

    @Override
    public BlockVector3 getMinimumPoint() {
        return min;
    }

    @Override
    public BlockVector3 getMaximumPoint() {
        return max;
    }

    @Override
    public BlockState getBlock(BlockVector3 position) {
        return blocks.getOrDefault(position, AIR).state();
    }

    @Override
    public BaseBlock getFullBlock(BlockVector3 position) {
        return blocks.getOrDefault(position, AIR).full();
    }
}
