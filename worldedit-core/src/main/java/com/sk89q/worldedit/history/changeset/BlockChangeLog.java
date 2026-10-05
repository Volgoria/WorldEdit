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

package com.sk89q.worldedit.history.changeset;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;

import java.util.Arrays;
import javax.annotation.Nullable;

/**
 * An append-only list of positions, each optionally paired with a block.
 *
 * <p>Entries are stored in fixed-size chunks, so the list never copies its
 * contents as it grows. Positions are stored in their packed {@code long} form
 * where possible. Blocks without NBT data are stored as their state's shared
 * {@link BaseBlock} instance.</p>
 */
final class BlockChangeLog {

    private static final int CHUNK_BITS = 12;
    private static final int CHUNK_SIZE = 1 << CHUNK_BITS;
    private static final int CHUNK_MASK = CHUNK_SIZE - 1;
    private static final int FIRST_CHUNK_CAPACITY = 16;

    private static final class Chunk {
        /**
         * The packed positions, or {@code null} once this chunk holds a position
         * that cannot be packed.
         */
        long[] packed;
        /**
         * The positions as x, y, z triples, if they cannot all be packed.
         */
        int[] wide;
        /**
         * The blocks, or {@code null} if the log has no blocks.
         */
        BaseBlock[] blocks;

        Chunk(int capacity, boolean withBlocks) {
            packed = new long[capacity];
            blocks = withBlocks ? new BaseBlock[capacity] : null;
        }

        int capacity() {
            return packed != null ? packed.length : wide.length / 3;
        }

        void grow(int capacity) {
            if (packed != null) {
                packed = Arrays.copyOf(packed, capacity);
            } else {
                wide = Arrays.copyOf(wide, capacity * 3);
            }
            if (blocks != null) {
                blocks = Arrays.copyOf(blocks, capacity);
            }
        }

        void widen(int size) {
            int[] wide = new int[capacity() * 3];
            for (int i = 0; i < size; i++) {
                long p = packed[i];
                BlockVector3 position = BlockVector3.fromLongPackedForm(p);
                wide[i * 3] = position.x();
                wide[i * 3 + 1] = position.y();
                wide[i * 3 + 2] = position.z();
            }
            this.wide = wide;
            this.packed = null;
        }

        void set(int offset, BlockVector3 position) {
            if (packed != null) {
                if (BlockVector3.isLongPackable(position)) {
                    packed[offset] = position.toLongPackedForm();
                    return;
                }
                widen(offset);
            }
            wide[offset * 3] = position.x();
            wide[offset * 3 + 1] = position.y();
            wide[offset * 3 + 2] = position.z();
        }

        BlockVector3 get(int offset) {
            if (packed != null) {
                return BlockVector3.fromLongPackedForm(packed[offset]);
            }
            return BlockVector3.at(wide[offset * 3], wide[offset * 3 + 1], wide[offset * 3 + 2]);
        }
    }

    private final boolean withBlocks;
    private Chunk[] chunks = new Chunk[0];
    private int size;

    /**
     * Create a new log.
     *
     * @param withBlocks whether each position is paired with a block
     */
    BlockChangeLog(boolean withBlocks) {
        this.withBlocks = withBlocks;
    }

    private static BaseBlock canonicalize(BaseBlock block) {
        if (block.getNbtReference() == null) {
            return block.toImmutableState().toBaseBlock();
        }
        return block;
    }

    /**
     * Add an entry.
     *
     * @param position the position
     * @param block the block, if this log has blocks, otherwise ignored
     */
    void add(BlockVector3 position, @Nullable BaseBlock block) {
        int chunkIndex = size >>> CHUNK_BITS;
        int offset = size & CHUNK_MASK;
        Chunk chunk;
        if (chunkIndex < chunks.length && chunks[chunkIndex] != null) {
            chunk = chunks[chunkIndex];
            if (offset == chunk.capacity()) {
                // only the first chunk grows
                chunk.grow(Math.min(CHUNK_SIZE, offset * 2));
            }
        } else {
            if (chunkIndex >= chunks.length) {
                chunks = Arrays.copyOf(chunks, Math.max(4, chunks.length * 2));
            }
            chunk = new Chunk(chunkIndex == 0 ? FIRST_CHUNK_CAPACITY : CHUNK_SIZE, withBlocks);
            chunks[chunkIndex] = chunk;
        }
        chunk.set(offset, position);
        if (chunk.blocks != null) {
            chunk.blocks[offset] = canonicalize(block);
        }
        size++;
    }

    int size() {
        return size;
    }

    BlockVector3 position(int index) {
        return chunks[index >>> CHUNK_BITS].get(index & CHUNK_MASK);
    }

    BaseBlock block(int index) {
        BaseBlock[] blocks = chunks[index >>> CHUNK_BITS].blocks;
        if (blocks == null) {
            throw new IllegalStateException("This log has no blocks");
        }
        return blocks[index & CHUNK_MASK];
    }

    /**
     * Create a log, without blocks, of the positions in this log.
     *
     * @return the new log
     */
    BlockChangeLog copyPositions() {
        BlockChangeLog copy = new BlockChangeLog(false);
        for (int i = 0; i < size; i++) {
            copy.add(position(i), null);
        }
        return copy;
    }
}
