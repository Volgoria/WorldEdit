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
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.util.HashSet;
import java.util.Set;

/**
 * A compact set of block positions, for membership tests in hot loops over
 * mostly connected positions, such as flood fills.
 *
 * <p>Positions are stored as bits, in a bitset for each 16x16x16 section that
 * holds any position. Positions outside the range of 26 bits for X and Z, and
 * 24 bits for Y, fall back to a regular set. This set cannot be iterated.</p>
 */
public final class BlockVector3Set {

    private static final int MIN_XZ = -(1 << 25);
    private static final int MAX_XZ = (1 << 25) - 1;
    private static final int MIN_Y = -(1 << 23);
    private static final int MAX_Y = (1 << 23) - 1;
    private static final long BITS_22 = (1L << 22) - 1;
    private static final long BITS_20 = (1L << 20) - 1;
    private static final int WORDS_PER_SECTION = 16 * 16 * 16 / 64;

    private static boolean inSectionRange(int x, int y, int z) {
        return MIN_XZ <= x && x <= MAX_XZ && MIN_XZ <= z && z <= MAX_XZ && MIN_Y <= y && y <= MAX_Y;
    }

    private static long sectionKey(int x, int y, int z) {
        return ((x >> 4) & BITS_22) | (((z >> 4) & BITS_22) << 22) | (((y >> 4) & BITS_20) << 44);
    }

    private static int bitIndex(int x, int y, int z) {
        return (x & 15) | ((z & 15) << 4) | ((y & 15) << 8);
    }

    private final Long2ObjectOpenHashMap<long[]> sections = new Long2ObjectOpenHashMap<>();
    private final Set<BlockVector3> outOfRange = new HashSet<>();
    private int size;

    // Cache of the most recently accessed section, keyed by section coordinates
    private int lastSectionX;
    private int lastSectionY;
    private int lastSectionZ;
    private long[] lastSection;

    private long[] findSection(int x, int y, int z, boolean create) {
        int sectionX = x >> 4;
        int sectionY = y >> 4;
        int sectionZ = z >> 4;
        long[] section = lastSection;
        if (section != null && sectionX == lastSectionX && sectionY == lastSectionY && sectionZ == lastSectionZ) {
            return section;
        }
        long key = sectionKey(x, y, z);
        section = sections.get(key);
        if (section == null) {
            if (!create) {
                return null;
            }
            section = new long[WORDS_PER_SECTION];
            sections.put(key, section);
        }
        lastSectionX = sectionX;
        lastSectionY = sectionY;
        lastSectionZ = sectionZ;
        lastSection = section;
        return section;
    }

    /**
     * Add a position.
     *
     * @param position the position
     * @return true if the position was not already present
     */
    public boolean add(BlockVector3 position) {
        int x = position.x();
        int y = position.y();
        int z = position.z();
        if (!inSectionRange(x, y, z)) {
            if (outOfRange.add(position)) {
                size++;
                return true;
            }
            return false;
        }
        long[] section = findSection(x, y, z, true);
        int bit = bitIndex(x, y, z);
        long word = section[bit >>> 6];
        long mask = 1L << bit;
        if ((word & mask) != 0) {
            return false;
        }
        section[bit >>> 6] = word | mask;
        size++;
        return true;
    }

    /**
     * Check whether a position is present.
     *
     * @param position the position
     * @return true if present
     */
    public boolean contains(BlockVector3 position) {
        int x = position.x();
        int y = position.y();
        int z = position.z();
        if (!inSectionRange(x, y, z)) {
            return outOfRange.contains(position);
        }
        long[] section = findSection(x, y, z, false);
        if (section == null) {
            return false;
        }
        int bit = bitIndex(x, y, z);
        return (section[bit >>> 6] & (1L << bit)) != 0;
    }

    /**
     * Get the number of positions in this set.
     *
     * @return the size
     */
    public int size() {
        return size;
    }

    /**
     * Check whether this set is empty.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return size == 0;
    }
}
