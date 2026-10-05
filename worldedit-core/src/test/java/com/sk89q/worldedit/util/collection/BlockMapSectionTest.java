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

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Size;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockMapSectionTest {

    /**
     * Check the section against the model, including iteration over its slots.
     */
    private static void assertMatches(Map<Integer, Object> model, BlockMapSection section) {
        assertEquals(model.size(), section.size());
        assertEquals(model.isEmpty(), section.isEmpty());
        for (int index = 0; index < BlockMapSection.SIZE; index++) {
            assertEquals(model.get(index), section.get(index), "index " + index);
        }
        Map<Integer, Object> iterated = new HashMap<>();
        for (int slot = section.nextSlot(0); slot >= 0; slot = section.nextSlot(slot + 1)) {
            Object previous = iterated.put(section.indexAt(slot), section.valueAt(slot));
            assertNull(previous, "index visited twice");
        }
        assertEquals(model, iterated);
    }

    /**
     * Each op is {@code kind * 4096 + index}, where kind 0 and 1 put, and kind 2 removes.
     */
    @Property(tries = 300)
    void behavesLikeAMap(@ForAll @Size(max = 3000) List<@IntRange(min = 0, max = 3 * 4096 - 1) Integer> ops,
                         @ForAll @IntRange(min = 1, max = 4096) int indexRange,
                         @ForAll boolean startDense) {
        BlockMapSection section = new BlockMapSection(0, 0, 0, startDense);
        Map<Integer, Object> model = new HashMap<>();
        int counter = 0;
        for (int op : ops) {
            int index = (op & 4095) % indexRange;
            if (op >>> 12 < 2) {
                Object value = "v" + counter++;
                assertEquals(model.put(index, value), section.put(index, value));
            } else {
                assertEquals(model.remove(index), section.remove(index));
            }
            assertEquals(model.size(), section.size());
        }
        assertMatches(model, section);
    }

    @Property(tries = 200)
    void removingThroughCursorVisitsEverything(
        @ForAll @Size(max = 2000) List<@IntRange(min = 0, max = 4095) Integer> indexes,
        @ForAll @IntRange(min = 0, max = 2) int removeEvery) {
        BlockMapSection section = new BlockMapSection(0, 0, 0, false);
        Map<Integer, Object> model = new HashMap<>();
        for (int index : indexes) {
            section.put(index, index);
            model.put(index, index);
        }
        Map<Integer, Object> visited = new HashMap<>();
        int count = 0;
        for (int slot = section.nextSlot(0); slot >= 0; slot = section.nextSlot(slot + 1)) {
            int index = section.indexAt(slot);
            visited.put(index, section.valueAt(slot));
            if (removeEvery != 0 && count++ % removeEvery == 0) {
                assertEquals(index, section.removeAt(slot));
                model.remove(index);
            }
        }
        assertEquals(indexes.stream().distinct().count(), visited.size());
        assertMatches(model, section);
    }

    @Test
    void becomesDenseWhenFilled() {
        BlockMapSection section = new BlockMapSection(0, 0, 0, false);
        for (int i = 0; i < BlockMapSection.DENSE_THRESHOLD; i++) {
            section.put(i * 7 % BlockMapSection.SIZE, i);
        }
        assertFalse(section.isDense());
        section.put(BlockMapSection.SIZE - 1, "last");
        assertTrue(section.isDense());
        assertEquals(BlockMapSection.DENSE_THRESHOLD + 1, section.size());
        assertEquals("last", section.get(BlockMapSection.SIZE - 1));
        for (int i = 0; i < BlockMapSection.DENSE_THRESHOLD; i++) {
            assertEquals(i, section.get(i * 7 % BlockMapSection.SIZE));
        }
    }

    @Test
    void staysSparseWithChurn() {
        // Repeatedly adding and removing values must not exhaust the free slots
        BlockMapSection section = new BlockMapSection(0, 0, 0, false);
        for (int i = 0; i < 100_000; i++) {
            int index = i % BlockMapSection.SIZE;
            section.put(index, i);
            if (i >= 8) {
                assertEquals(i - 8, section.remove((i - 8) % BlockMapSection.SIZE));
            }
        }
        assertFalse(section.isDense());
        assertEquals(8, section.size());
    }

    @Test
    void reconstructsCoordinates() {
        BlockMapSection section = new BlockMapSection(-32, 4096, 48, false);
        int index = BlockMapSection.index(-29, 4096 + 15, 50);
        assertEquals(-29, section.blockX(index));
        assertEquals(4096 + 15, section.blockY(index));
        assertEquals(50, section.blockZ(index));
    }
}
