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

package com.sk89q.worldedit.internal.util;

import com.sk89q.worldedit.math.BlockVector3;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import java.util.HashSet;
import java.util.Set;

/**
 * A compact set of block positions, for membership tests in hot loops.
 *
 * <p>Positions within the long-packable range (see
 * {@link BlockVector3#isLongPackable(BlockVector3)}) are stored as primitive
 * longs, avoiding an object and a hash node per position. Other positions
 * fall back to a regular set. This set cannot be iterated.</p>
 */
public final class BlockVector3Set {

    private final LongSet packed = new LongOpenHashSet();
    private final Set<BlockVector3> unpackable = new HashSet<>();

    /**
     * Add a position.
     *
     * @param position the position
     * @return true if the position was not already present
     */
    public boolean add(BlockVector3 position) {
        if (BlockVector3.isLongPackable(position)) {
            return packed.add(position.toLongPackedForm());
        }
        return unpackable.add(position);
    }

    /**
     * Check whether a position is present.
     *
     * @param position the position
     * @return true if present
     */
    public boolean contains(BlockVector3 position) {
        if (BlockVector3.isLongPackable(position)) {
            return packed.contains(position.toLongPackedForm());
        }
        return unpackable.contains(position);
    }

    /**
     * Get the number of positions in this set.
     *
     * @return the size
     */
    public int size() {
        return packed.size() + unpackable.size();
    }

    /**
     * Check whether this set is empty.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return packed.isEmpty() && unpackable.isEmpty();
    }
}
