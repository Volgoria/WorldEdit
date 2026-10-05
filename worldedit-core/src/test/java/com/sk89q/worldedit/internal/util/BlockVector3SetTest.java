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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockVector3SetTest {

    static Stream<BlockVector3> positions() {
        return Stream.of(
            BlockVector3.ZERO,
            BlockVector3.at(-1, -1, -1),
            BlockVector3.at(30_000_000, 2047, -30_000_000),
            BlockVector3.at(-30_000_000, -2048, 30_000_000),
            // Not long-packable
            BlockVector3.at(0, 2048, 0),
            BlockVector3.at(0, -2049, 0),
            BlockVector3.at(30_000_001, 0, 0),
            BlockVector3.at(Integer.MAX_VALUE, 0, Integer.MIN_VALUE)
        );
    }

    @ParameterizedTest
    @MethodSource("positions")
    void addAndContains(BlockVector3 position) {
        BlockVector3Set set = new BlockVector3Set();
        assertTrue(set.isEmpty());
        assertFalse(set.contains(position));
        assertTrue(set.add(position));
        assertFalse(set.add(position));
        assertTrue(set.contains(position));
        assertEquals(1, set.size());
        assertFalse(set.isEmpty());
        // Neighbours are distinct entries
        assertFalse(set.contains(position.add(1, 0, 0)));
        assertFalse(set.contains(position.add(0, 0, 1)));
    }

    @ParameterizedTest
    @MethodSource("positions")
    void yNeighboursAreDistinct(BlockVector3 position) {
        BlockVector3Set set = new BlockVector3Set();
        set.add(position);
        BlockVector3 above = position.add(0, 1, 0);
        assertFalse(set.contains(above));
        assertTrue(set.add(above));
        assertEquals(2, set.size());
    }
}
