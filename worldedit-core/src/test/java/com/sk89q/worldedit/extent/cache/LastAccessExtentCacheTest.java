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

import com.sk89q.worldedit.BaseWorldEditTest;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.NullExtent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LastAccessExtentCacheTest extends BaseWorldEditTest {

    private static BlockState air;
    private static BlockState stone;
    private static BlockState dirt;

    @BeforeAll
    static void setUpBlocks() {
        air = new BlockType("cachetest:air").getDefaultState();
        stone = new BlockType("cachetest:stone").getDefaultState();
        dirt = new BlockType("cachetest:dirt").getDefaultState();
    }

    /**
     * Stores blocks and records the reads that reach it.
     */
    private static final class Backing extends AbstractDelegateExtent {
        final Map<BlockVector3, BaseBlock> blocks = new HashMap<>();
        final List<String> reads = new ArrayList<>();
        boolean acceptSets = true;

        Backing() {
            super(new NullExtent());
        }

        @Override
        public BlockState getBlock(BlockVector3 position) {
            reads.add("getBlock " + position);
            return getFullBlock0(position).toImmutableState();
        }

        @Override
        public BaseBlock getFullBlock(BlockVector3 position) {
            reads.add("getFullBlock " + position);
            return getFullBlock0(position);
        }

        private BaseBlock getFullBlock0(BlockVector3 position) {
            return blocks.getOrDefault(position, air.toBaseBlock());
        }

        @Override
        public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 location, T block) {
            if (acceptSets) {
                blocks.put(location, block.toBaseBlock());
            }
            return acceptSets;
        }
    }

    @Test
    void cachesLastReadOfEachKind() throws Exception {
        Backing backing = new Backing();
        LastAccessExtentCache cache = new LastAccessExtentCache(backing);
        BlockVector3 a = BlockVector3.at(1, 2, 3);
        BlockVector3 b = BlockVector3.at(4, 5, 6);
        backing.setBlock(a, stone);

        assertEquals(stone, cache.getBlock(a));
        assertEquals(stone, cache.getBlock(BlockVector3.at(1, 2, 3)));
        assertEquals(List.of("getBlock " + a), backing.reads);

        // A full block read is cached separately, and also answers getBlock
        BaseBlock full = cache.getFullBlock(b);
        assertSame(full, cache.getFullBlock(b));
        assertEquals(air, cache.getBlock(b));
        // and the cached block is still there
        assertEquals(stone, cache.getBlock(a));
        assertEquals(List.of("getBlock " + a, "getFullBlock " + b), backing.reads);

        // A miss replaces only the cache of its own kind
        BlockVector3 c = BlockVector3.at(7, 8, 9);
        assertEquals(air, cache.getBlock(c));
        assertSame(full, cache.getFullBlock(b));
        assertEquals(stone, cache.getFullBlock(a).toImmutableState());
        // answered by the cached full block
        assertEquals(stone, cache.getBlock(a));
        assertEquals(List.of("getBlock " + a, "getFullBlock " + b, "getBlock " + c, "getFullBlock " + a),
            backing.reads);
    }

    @Test
    void fullBlockAnswersGetBlockWhenBlockIsNotCached() throws Exception {
        Backing backing = new Backing();
        LastAccessExtentCache cache = new LastAccessExtentCache(backing);
        BlockVector3 a = BlockVector3.at(1, 2, 3);
        backing.setBlock(a, dirt);

        cache.getFullBlock(a);
        assertEquals(dirt, cache.getBlock(a));
        assertEquals(List.of("getFullBlock " + a), backing.reads);
    }

    @Test
    void successfulSetUpdatesCachedPosition() throws Exception {
        Backing backing = new Backing();
        LastAccessExtentCache cache = new LastAccessExtentCache(backing);
        BlockVector3 a = BlockVector3.at(1, 2, 3);
        BlockVector3 b = BlockVector3.at(4, 5, 6);

        cache.getBlock(a);
        cache.getFullBlock(a);
        assertTrue(cache.setBlock(a, stone));
        assertEquals(stone, cache.getBlock(a));
        assertEquals(stone.toBaseBlock(), cache.getFullBlock(a));
        // setting another position leaves the cache alone
        assertTrue(cache.setBlock(b, dirt));
        assertEquals(stone, cache.getBlock(a));
        assertEquals(List.of("getBlock " + a, "getFullBlock " + a), backing.reads);
    }

    @Test
    void failedSetKeepsCachedBlock() throws Exception {
        Backing backing = new Backing();
        LastAccessExtentCache cache = new LastAccessExtentCache(backing);
        BlockVector3 a = BlockVector3.at(1, 2, 3);

        cache.getBlock(a);
        cache.getFullBlock(a);
        backing.acceptSets = false;
        assertFalse(cache.setBlock(a, stone));
        assertEquals(air, cache.getBlock(a));
        assertEquals(air.toBaseBlock(), cache.getFullBlock(a));
        assertEquals(2, backing.reads.size());
    }
}
