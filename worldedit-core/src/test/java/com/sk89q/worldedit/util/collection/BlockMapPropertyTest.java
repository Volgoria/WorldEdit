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

import com.sk89q.worldedit.math.BlockVector3;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks {@link BlockMap} against {@link HashMap} for random sequences of operations.
 */
class BlockMapPropertyTest {

    private static final int MAX_XZ = (1 << 25) - 1;
    private static final int MIN_XZ = -(1 << 25);

    enum OpKind {
        PUT, PUT_NULL, REMOVE, PUT_IF_ABSENT, COMPUTE, COMPUTE_IF_ABSENT, COMPUTE_IF_PRESENT, MERGE,
        REPLACE, REPLACE_EXPECTED, REMOVE_EXPECTED, ITERATOR_REMOVE, KEY_SET_REMOVE, SET_VALUE, CLEAR
    }

    record Op(OpKind kind, BlockVector3 position, int value) {
    }

    /**
     * Positions mostly packed into a few sections, but also anywhere in the supported range.
     */
    @Provide
    Arbitrary<BlockVector3> positions() {
        Arbitrary<BlockVector3> clustered = Combinators.combine(
            Arbitraries.integers().between(-20, 20),
            Arbitraries.integers().between(-20, 20),
            Arbitraries.integers().between(-20, 20)
        ).as(BlockVector3::at);
        Arbitrary<BlockVector3> anywhere = Combinators.combine(
            Arbitraries.integers().between(MIN_XZ, MAX_XZ),
            Arbitraries.integers(),
            Arbitraries.integers().between(MIN_XZ, MAX_XZ)
        ).as(BlockVector3::at);
        Arbitrary<BlockVector3> edges = Combinators.combine(
            Arbitraries.of(MIN_XZ, MIN_XZ + 15, -1, 0, 15, 16, MAX_XZ - 15, MAX_XZ),
            Arbitraries.of(Integer.MIN_VALUE, -(1 << 24) - 1, -(1 << 24), -1, 0, (1 << 24) - 1, 1 << 24,
                Integer.MAX_VALUE),
            Arbitraries.of(MIN_XZ, -1, 0, MAX_XZ)
        ).as(BlockVector3::at);
        return Arbitraries.frequencyOf(
            Tuple.of(8, clustered),
            Tuple.of(1, anywhere),
            Tuple.of(1, edges)
        );
    }

    @Provide
    Arbitrary<List<BlockVector3>> positionLists() {
        return positions().list().ofMaxSize(300);
    }

    @Provide
    Arbitrary<List<Op>> ops() {
        Arbitrary<Op> op = Combinators.combine(
            Arbitraries.frequencyOf(
                Tuple.of(30, Arbitraries.just(OpKind.PUT)),
                Tuple.of(2, Arbitraries.just(OpKind.PUT_NULL)),
                Tuple.of(8, Arbitraries.just(OpKind.REMOVE)),
                Tuple.of(3, Arbitraries.just(OpKind.PUT_IF_ABSENT)),
                Tuple.of(3, Arbitraries.just(OpKind.COMPUTE)),
                Tuple.of(2, Arbitraries.just(OpKind.COMPUTE_IF_ABSENT)),
                Tuple.of(2, Arbitraries.just(OpKind.COMPUTE_IF_PRESENT)),
                Tuple.of(2, Arbitraries.just(OpKind.MERGE)),
                Tuple.of(2, Arbitraries.just(OpKind.REPLACE)),
                Tuple.of(2, Arbitraries.just(OpKind.REPLACE_EXPECTED)),
                Tuple.of(2, Arbitraries.just(OpKind.REMOVE_EXPECTED)),
                Tuple.of(2, Arbitraries.just(OpKind.ITERATOR_REMOVE)),
                Tuple.of(2, Arbitraries.just(OpKind.KEY_SET_REMOVE)),
                Tuple.of(2, Arbitraries.just(OpKind.SET_VALUE)),
                Tuple.of(1, Arbitraries.just(OpKind.CLEAR))
            ),
            positions(),
            // a small value range makes equal values, and so conditional operations, likely
            Arbitraries.integers().between(0, 5)
        ).as(Op::new);
        return op.list().ofMaxSize(400);
    }

    private static Integer nullIfZero(int value) {
        return value == 0 ? null : value;
    }

    @Property(tries = 1000)
    void behavesLikeHashMap(@ForAll("ops") List<Op> ops) {
        BlockMap<Integer> map = BlockMap.create();
        Map<BlockVector3, Integer> model = new HashMap<>();
        for (Op op : ops) {
            BlockVector3 pos = op.position();
            Integer value = op.value();
            switch (op.kind()) {
                case PUT -> assertEquals(model.put(pos, value), map.put(pos, value));
                case PUT_NULL -> assertEquals(model.put(pos, null), map.put(pos, null));
                case REMOVE -> assertEquals(model.remove(pos), map.remove(pos));
                case PUT_IF_ABSENT -> assertEquals(model.putIfAbsent(pos, value), map.putIfAbsent(pos, value));
                case COMPUTE -> assertEquals(
                    model.compute(pos, (_, old) -> nullIfZero(Objects.requireNonNullElse(old, 0) + op.value() % 2)),
                    map.compute(pos, (_, old) -> nullIfZero(Objects.requireNonNullElse(old, 0) + op.value() % 2))
                );
                case COMPUTE_IF_ABSENT -> assertEquals(
                    model.computeIfAbsent(pos, _ -> nullIfZero(op.value())),
                    map.computeIfAbsent(pos, _ -> nullIfZero(op.value()))
                );
                case COMPUTE_IF_PRESENT -> assertEquals(
                    model.computeIfPresent(pos, (_, old) -> nullIfZero(old * op.value() % 6)),
                    map.computeIfPresent(pos, (_, old) -> nullIfZero(old * op.value() % 6))
                );
                case MERGE -> assertEquals(
                    model.merge(pos, value, (a, b) -> nullIfZero((a + b) % 6)),
                    map.merge(pos, value, (a, b) -> nullIfZero((a + b) % 6))
                );
                case REPLACE -> assertEquals(model.replace(pos, value), map.replace(pos, value));
                case REPLACE_EXPECTED -> assertEquals(
                    model.replace(pos, value, value + 1), map.replace(pos, value, value + 1)
                );
                case REMOVE_EXPECTED -> assertEquals(model.remove(pos, value), map.remove(pos, value));
                case ITERATOR_REMOVE -> {
                    // remove every n-th entry while iterating
                    int every = op.value() + 1;
                    int count = 0;
                    Iterator<Map.Entry<BlockVector3, Integer>> iterator = map.entrySet().iterator();
                    while (iterator.hasNext()) {
                        Map.Entry<BlockVector3, Integer> entry = iterator.next();
                        if (count++ % every == 0) {
                            assertTrue(model.containsKey(entry.getKey()));
                            assertEquals(model.remove(entry.getKey()), entry.getValue());
                            iterator.remove();
                        }
                    }
                }
                case KEY_SET_REMOVE -> assertEquals(model.keySet().remove(pos), map.keySet().remove(pos));
                case SET_VALUE -> {
                    for (Map.Entry<BlockVector3, Integer> entry : map.entrySet()) {
                        if (entry.getKey().y() % 2 == 0) {
                            Integer newValue = op.value() == 0 ? null : entry.getKey().x() & 7;
                            assertEquals(model.put(entry.getKey(), newValue), entry.setValue(newValue));
                            assertEquals(newValue, entry.getValue());
                        }
                    }
                }
                case CLEAR -> {
                    model.clear();
                    map.clear();
                }
                default -> throw new AssertionError(op.kind());
            }
            assertEquals(model.size(), map.size());
        }
        assertSameContents(model, map);
    }

    private static void assertSameContents(Map<BlockVector3, Integer> model, BlockMap<Integer> map) {
        assertEquals(model.size(), map.size());
        assertEquals(model.isEmpty(), map.isEmpty());
        assertEquals(model, map);
        assertEquals(map, model);
        assertEquals(model.hashCode(), map.hashCode());
        assertEquals(model.keySet(), map.keySet());
        assertEquals(new HashSet<>(model.entrySet()), map.entrySet());
        for (Map.Entry<BlockVector3, Integer> entry : model.entrySet()) {
            assertTrue(map.containsKey(entry.getKey()));
            assertEquals(entry.getValue(), map.get(entry.getKey()));
            assertEquals(entry.getValue(), map.getOrDefault(entry.getKey(), -1));
            assertTrue(map.containsValue(entry.getValue()));
        }
        List<Integer> values = new ArrayList<>(map.values());
        assertEquals(model.size(), values.size());
        Map<BlockVector3, Integer> forEach = new HashMap<>();
        map.forEach((key, value) -> {
            assertFalse(forEach.containsKey(key), "visited twice: " + key);
            forEach.put(key, value);
        });
        assertEquals(model, forEach);
        // copies are equal, both generic and through the fast path
        assertEquals(model, BlockMap.copyOf(model));
        assertEquals(map, BlockMap.copyOf(map));
        BlockMap<Integer> copy = BlockMap.create();
        copy.putAll(map);
        assertEquals(map, copy);
    }

    @Property(tries = 300)
    void iteratesSectionBySectionInFirstWriteOrder(@ForAll("positionLists") List<BlockVector3> positions) {
        BlockMap<Integer> map = BlockMap.create();
        List<BlockVector3> sectionOrder = new ArrayList<>();
        for (BlockVector3 position : positions) {
            map.put(position, 1);
            BlockVector3 section = position.shr(4);
            if (!sectionOrder.contains(section)) {
                sectionOrder.add(section);
            }
        }
        // Sections are grouped by the top 8 bits of y, so only compare within those groups
        List<BlockVector3> keys = new ArrayList<>(map.keySet());
        Set<BlockVector3> finishedSections = new HashSet<>();
        for (int i = 1; i < keys.size(); i++) {
            BlockVector3 previous = keys.get(i - 1).shr(4);
            BlockVector3 current = keys.get(i).shr(4);
            if (!previous.equals(current)) {
                assertTrue(finishedSections.add(previous), "section visited twice: " + previous);
                assertFalse(finishedSections.contains(current), "section visited twice: " + current);
                if (previous.y() >> 20 == current.y() >> 20) {
                    assertTrue(sectionOrder.indexOf(previous) < sectionOrder.indexOf(current),
                        "section " + previous + " before section " + current);
                }
            }
        }
    }

    @Test
    void iteratesDenseSectionsInXzyOrder() {
        BlockMap<Integer> map = BlockMap.create();
        List<BlockVector3> expected = new ArrayList<>();
        for (int y = 15; y >= 0; y--) {
            for (int z = 15; z >= 0; z--) {
                for (int x = 15; x >= 0; x--) {
                    map.put(BlockVector3.at(x, y, z), 1);
                    expected.addFirst(BlockVector3.at(x, y, z));
                }
            }
        }
        assertEquals(expected, new ArrayList<>(map.keySet()));
    }

    @Test
    void wrapsCoordinatesOutsideTheSupportedRange() {
        BlockMap<Integer> map = BlockMap.create();
        map.put(BlockVector3.at(MAX_XZ + 1, 5, MIN_XZ - 1), 1);
        assertEquals(1, map.get(BlockVector3.at(MIN_XZ, 5, MAX_XZ)));
        assertEquals(BlockVector3.at(MIN_XZ, 5, MAX_XZ), map.keySet().iterator().next());
    }
}
