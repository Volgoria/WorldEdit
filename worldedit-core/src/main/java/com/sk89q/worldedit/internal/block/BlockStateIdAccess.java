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

package com.sk89q.worldedit.internal.block;

import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.registry.BlockRegistry;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.Arrays;
import java.util.BitSet;
import java.util.OptionalInt;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkState;

public final class BlockStateIdAccess {

    private static final int INVALID_ID = -1;
    private static final int EXPECTED_BLOCK_COUNT = 2 << 14;
    private static final Int2ObjectOpenHashMap<BlockState> TO_STATE =
        new Int2ObjectOpenHashMap<>(EXPECTED_BLOCK_COUNT);
    /**
     * IDs below this are also stored in {@link #byId}. Internal IDs are dense
     * in practice, so this covers all of them while bounding the array size.
     */
    private static final int MAX_ARRAY_ID = 1 << 20;
    /**
     * Array mirror of {@link #TO_STATE} for fast lookups, which happen for
     * every block read on some platforms. Only written under the class lock,
     * and re-published through the volatile field after every change.
     */
    private static volatile BlockState[] byId = new BlockState[0];

    static {
        TO_STATE.defaultReturnValue(null);
    }

    public interface BlockStateInternalId {
        int getInternalId(BlockState blockState);

        void setInternalId(BlockState blockState, int internalId);
    }

    private static BlockStateInternalId blockStateInternalId;

    public static void setBlockStateInternalId(BlockStateInternalId blockStateInternalId) {
        BlockStateIdAccess.blockStateInternalId = blockStateInternalId;
    }

    /**
     * An invalid internal ID, for verification purposes.
     * @return an internal ID which is never valid
     */
    public static int invalidId() {
        return INVALID_ID;
    }

    public static boolean isValidInternalId(int internalId) {
        return internalId != INVALID_ID;
    }

    public static int getBlockStateId(BlockState holder) {
        return blockStateInternalId.getInternalId(holder);
    }

    public static @Nullable BlockState getBlockStateById(int id) {
        BlockState[] states = byId;
        if (id >= 0 && id < states.length) {
            return states[id];
        }
        synchronized (BlockStateIdAccess.class) {
            return TO_STATE.get(id);
        }
    }

    /**
     * For platforms that don't have an internal ID system,
     * {@link BlockRegistry#getInternalBlockStateId(BlockState)} will return
     * {@link OptionalInt#empty()}. In those cases, we will use our own ID system,
     * since it's useful for other entries as well.
     *
     * @return an unused ID in WorldEdit's ID tracker
     */
    private static int provideUnusedWorldEditId() {
        return usedIds.nextClearBit(0);
    }

    private static final BitSet usedIds = new BitSet();

    public static synchronized void register(BlockState blockState, int id) {
        int i = isValidInternalId(id) ? id : provideUnusedWorldEditId();
        BlockState existing = TO_STATE.get(i);
        checkState(existing == null || existing == blockState,
            "BlockState %s is using the same block ID (%s) as BlockState %s",
            blockState, i, existing);
        blockStateInternalId.setInternalId(blockState, i);
        TO_STATE.put(i, blockState);
        usedIds.set(i);
        if (i >= 0 && i < MAX_ARRAY_ID) {
            BlockState[] states = byId;
            if (i >= states.length) {
                // Grow geometrically, so registering every state stays linear
                int newLength = Math.min(MAX_ARRAY_ID, Math.max(i + 1, states.length * 2));
                states = Arrays.copyOf(states, newLength);
            }
            states[i] = blockState;
            byId = states;
        }
    }

    public static synchronized void clear() {
        for (BlockState value : TO_STATE.values()) {
            blockStateInternalId.setInternalId(value, invalidId());
        }
        TO_STATE.clear();
        usedIds.clear();
        byId = new BlockState[0];
    }

    private BlockStateIdAccess() {
    }

}
