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
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks {@link BlockVector3Set} against {@link HashSet}.
 */
class BlockVector3SetPropertyTest {

    @Provide
    Arbitrary<List<BlockVector3>> positionLists() {
        Arbitrary<BlockVector3> clustered = Combinators.combine(
            Arbitraries.integers().between(-40, 40),
            Arbitraries.integers().between(-40, 40),
            Arbitraries.integers().between(-40, 40)
        ).as(BlockVector3::at);
        Arbitrary<BlockVector3> anywhere = Combinators.combine(
            Arbitraries.integers(), Arbitraries.integers(), Arbitraries.integers()
        ).as(BlockVector3::at);
        // around the edges of the range stored in bitsets
        Arbitrary<Integer> xzEdge = Arbitraries.of(-(1 << 25) - 1, -(1 << 25), -(1 << 25) + 16, -1, 0,
            (1 << 25) - 17, (1 << 25) - 1, 1 << 25, Integer.MIN_VALUE, Integer.MAX_VALUE);
        Arbitrary<Integer> yEdge = Arbitraries.of(-(1 << 23) - 1, -(1 << 23), -1, 0, (1 << 23) - 1, 1 << 23,
            Integer.MIN_VALUE, Integer.MAX_VALUE);
        Arbitrary<BlockVector3> edges = Combinators.combine(xzEdge, yEdge, xzEdge).as(BlockVector3::at);
        return Arbitraries.frequencyOf(
            Tuple.of(8, clustered),
            Tuple.of(1, anywhere),
            Tuple.of(1, edges)
        ).list().ofMaxSize(500);
    }

    @Property(tries = 1000)
    void behavesLikeHashSet(@ForAll("positionLists") List<BlockVector3> added,
                            @ForAll("positionLists") List<BlockVector3> queried) {
        BlockVector3Set set = new BlockVector3Set();
        Set<BlockVector3> model = new HashSet<>();
        for (BlockVector3 position : added) {
            assertEquals(model.add(position), set.add(position), "add " + position);
            assertEquals(model.size(), set.size());
        }
        assertEquals(model.isEmpty(), set.isEmpty());
        for (BlockVector3 position : added) {
            assertTrue(set.contains(position), "contains " + position);
        }
        for (BlockVector3 position : queried) {
            assertEquals(model.contains(position), set.contains(position), "contains " + position);
            for (BlockVector3 neighbour : List.of(position.add(1, 0, 0), position.add(0, 1, 0),
                position.add(0, 0, 1), position.add(-1, 0, 0), position.add(0, -1, 0), position.add(0, 0, -1))) {
                assertEquals(model.contains(neighbour), set.contains(neighbour), "contains " + neighbour);
            }
        }
    }
}
