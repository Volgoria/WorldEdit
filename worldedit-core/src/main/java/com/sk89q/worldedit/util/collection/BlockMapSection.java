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

package com.sk89q.worldedit.util.collection;

import java.util.Arrays;

/**
 * The values of one 16x16x16 section of a {@link BlockMap}, indexed by
 * {@code x | z << 4 | y << 8} of the position within the section.
 *
 * <p>A section starts out sparse, as a small open-addressing table, and switches
 * to a dense array of all 4096 slots once it holds enough values that the array
 * is about as small. Values are never {@code null}: a {@code null} result means
 * the index is absent.</p>
 *
 * <p>Removing values never moves other values, so a cursor over the slots stays
 * valid while values are removed through it.</p>
 */
final class BlockMapSection {

    static final int SIZE = 16 * 16 * 16;

    /**
     * Above this many values, the dense array is no larger than the sparse table.
     */
    static final int DENSE_THRESHOLD = SIZE / 8;

    private static final int INITIAL_SPARSE_CAPACITY = 8;
    private static final short FREE = -1;
    private static final short REMOVED = -2;

    // Base coordinates of the section, for reconstructing positions
    final int baseX;
    final int baseY;
    final int baseZ;

    /**
     * When dense, the value at each index. When sparse, the value in each slot.
     */
    private Object[] values;
    /**
     * When sparse, the index stored in each slot, or {@link #FREE} or {@link #REMOVED}.
     * {@code null} when dense.
     */
    private short[] keys;
    /**
     * When sparse, the number of slots that are not {@link #FREE}.
     */
    private int usedSlots;
    private int size;
    private boolean filled;

    BlockMapSection(int baseX, int baseY, int baseZ, boolean dense) {
        this.baseX = baseX;
        this.baseY = baseY;
        this.baseZ = baseZ;
        if (dense) {
            this.values = new Object[SIZE];
        } else {
            this.values = new Object[INITIAL_SPARSE_CAPACITY];
            this.keys = new short[INITIAL_SPARSE_CAPACITY];
            Arrays.fill(keys, FREE);
        }
    }

    static int index(int x, int y, int z) {
        return (x & 15) | ((z & 15) << 4) | ((y & 15) << 8);
    }

    int blockX(int index) {
        return baseX | (index & 15);
    }

    int blockY(int index) {
        return baseY | (index >>> 8);
    }

    int blockZ(int index) {
        return baseZ | ((index >>> 4) & 15);
    }

    int size() {
        return size;
    }

    boolean isEmpty() {
        return size == 0;
    }

    boolean isDense() {
        return keys == null;
    }

    /**
     * Record that this section has been well filled at some point.
     *
     * @return true if this was not recorded before
     */
    boolean markFilled() {
        if (filled) {
            return false;
        }
        filled = true;
        return true;
    }

    boolean wasFilled() {
        return filled;
    }

    private static int hash(int index, int mask) {
        return ((index * 0x9E3779B1) >>> 16) & mask;
    }

    /**
     * Find the slot holding {@code index}, or {@code -1}.
     */
    private int findSlot(int index) {
        short[] keys = this.keys;
        int mask = keys.length - 1;
        int slot = hash(index, mask);
        while (true) {
            short key = keys[slot];
            if (key == index) {
                return slot;
            }
            if (key == FREE) {
                return -1;
            }
            slot = (slot + 1) & mask;
        }
    }

    Object get(int index) {
        if (keys == null) {
            return values[index];
        }
        int slot = findSlot(index);
        return slot < 0 ? null : values[slot];
    }

    /**
     * Set the value at {@code index}.
     *
     * @param index the index
     * @param value the value, not {@code null}
     * @return the previous value, or {@code null} if absent
     */
    Object put(int index, Object value) {
        if (keys == null) {
            Object old = values[index];
            values[index] = value;
            if (old == null) {
                size++;
            }
            return old;
        }
        short[] keys = this.keys;
        int mask = keys.length - 1;
        int slot = hash(index, mask);
        int firstRemoved = -1;
        while (true) {
            short key = keys[slot];
            if (key == index) {
                Object old = values[slot];
                values[slot] = value;
                return old;
            }
            if (key == FREE) {
                break;
            }
            if (key == REMOVED && firstRemoved < 0) {
                firstRemoved = slot;
            }
            slot = (slot + 1) & mask;
        }
        if (size >= DENSE_THRESHOLD) {
            makeDense();
            values[index] = value;
            size++;
            return null;
        }
        if (firstRemoved >= 0) {
            slot = firstRemoved;
        } else {
            usedSlots++;
        }
        keys[slot] = (short) index;
        values[slot] = value;
        size++;
        if (usedSlots * 2 > keys.length) {
            // Grow if mostly full of values, otherwise just clear out removed slots
            rehash(size * 4 > keys.length ? keys.length * 2 : keys.length);
        }
        return null;
    }

    /**
     * Remove the value at {@code index}.
     *
     * @return the removed value, or {@code null} if absent
     */
    Object remove(int index) {
        if (keys == null) {
            Object old = values[index];
            if (old != null) {
                values[index] = null;
                size--;
            }
            return old;
        }
        int slot = findSlot(index);
        if (slot < 0) {
            return null;
        }
        return removeSlot(slot);
    }

    private Object removeSlot(int slot) {
        Object old = values[slot];
        values[slot] = null;
        keys[slot] = REMOVED;
        size--;
        return old;
    }

    private void rehash(int capacity) {
        short[] oldKeys = keys;
        Object[] oldValues = values;
        short[] newKeys = new short[capacity];
        Arrays.fill(newKeys, FREE);
        Object[] newValues = new Object[capacity];
        int mask = capacity - 1;
        for (int i = 0; i < oldKeys.length; i++) {
            short key = oldKeys[i];
            if (key >= 0) {
                int slot = hash(key, mask);
                while (newKeys[slot] != FREE) {
                    slot = (slot + 1) & mask;
                }
                newKeys[slot] = key;
                newValues[slot] = oldValues[i];
            }
        }
        keys = newKeys;
        values = newValues;
        usedSlots = size;
    }

    private void makeDense() {
        Object[] dense = new Object[SIZE];
        short[] oldKeys = keys;
        for (int i = 0; i < oldKeys.length; i++) {
            short key = oldKeys[i];
            if (key >= 0) {
                dense[key] = values[i];
            }
        }
        keys = null;
        values = dense;
        usedSlots = 0;
    }

    // Cursor access, for iteration. Slots are indexes into the backing arrays.

    /**
     * Get the first slot at or after {@code slot} that holds a value.
     *
     * @return the slot, or {@code -1} if there are no more
     */
    int nextSlot(int slot) {
        Object[] values = this.values;
        for (int i = slot; i < values.length; i++) {
            if (values[i] != null) {
                return i;
            }
        }
        return -1;
    }

    int indexAt(int slot) {
        return keys == null ? slot : keys[slot];
    }

    Object valueAt(int slot) {
        return values[slot];
    }

    void setValueAt(int slot, Object value) {
        values[slot] = value;
    }

    Object removeAt(int slot) {
        if (keys == null) {
            Object old = values[slot];
            values[slot] = null;
            size--;
            return old;
        }
        return removeSlot(slot);
    }
}
