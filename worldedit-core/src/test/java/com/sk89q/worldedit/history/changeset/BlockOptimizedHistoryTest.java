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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.sk89q.worldedit.BaseWorldEditTest;
import com.sk89q.worldedit.history.UndoContext;
import com.sk89q.worldedit.history.change.BlockChange;
import com.sk89q.worldedit.history.change.Change;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import org.enginehub.linbus.tree.LinCompoundTag;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("A BlockOptimizedHistory")
class BlockOptimizedHistoryTest extends BaseWorldEditTest {

    private static BaseBlock[] blocks;

    @BeforeAll
    static void createBlocks() {
        BlockState[] states = new BlockState[4];
        for (int i = 0; i < states.length; i++) {
            states[i] = new BlockType("historytest:block_" + i).getDefaultState();
        }
        blocks = new BaseBlock[] {
            states[0].toBaseBlock(),
            states[1].toBaseBlock(),
            states[2].toBaseBlock(),
            states[3].toBaseBlock(LinCompoundTag.builder().putString("a", "b").build()),
        };
    }

    private record OtherChange(int id) implements Change {
        @Override
        public void undo(UndoContext context) {
        }

        @Override
        public void redo(UndoContext context) {
        }
    }

    /**
     * The implementation this class used to have, built on two {@code LocatedBlockList}s.
     */
    private static final class ReferenceHistory {
        private final List<Change> others = new ArrayList<>();
        private final Map<BlockVector3, BaseBlock> firstPrevious = new LinkedHashMap<>();
        private final List<BlockVector3> currentOrder = new ArrayList<>();
        private final Map<BlockVector3, BaseBlock> lastCurrent = new HashMap<>();

        void add(Change change) {
            if (change instanceof BlockChange blockChange) {
                firstPrevious.putIfAbsent(blockChange.position(), blockChange.previous());
                currentOrder.add(blockChange.position());
                lastCurrent.put(blockChange.position(), blockChange.current());
            } else {
                others.add(change);
            }
        }

        List<Change> forward() {
            List<Change> result = new ArrayList<>(others);
            for (BlockVector3 position : currentOrder) {
                BaseBlock block = lastCurrent.get(position);
                result.add(new BlockChange(position, block, block));
            }
            return result;
        }

        List<Change> backward() {
            List<Change> result = new ArrayList<>(Lists.reverse(others));
            for (Map.Entry<BlockVector3, BaseBlock> entry : Lists.reverse(new ArrayList<>(firstPrevious.entrySet()))) {
                result.add(new BlockChange(entry.getKey(), entry.getValue(), entry.getValue()));
            }
            return result;
        }

        int size() {
            return others.size() + firstPrevious.size();
        }
    }

    @Test
    void matchesTheReferenceImplementation() {
        Random random = new Random(7);
        for (int round = 0; round < 300; round++) {
            BlockOptimizedHistory history = new BlockOptimizedHistory();
            ReferenceHistory reference = new ReferenceHistory();
            int count = random.nextInt(round % 10 == 0 ? 10_000 : 200);
            // a small area makes repeated positions likely in some rounds
            int area = 1 + random.nextInt(round % 3 == 0 ? 4 : 64);
            for (int i = 0; i < count; i++) {
                Change change;
                if (random.nextInt(20) == 0) {
                    change = new OtherChange(i);
                } else {
                    BlockVector3 position = random.nextInt(50) == 0
                        ? BlockVector3.at(random.nextInt(), random.nextInt(), random.nextInt())
                        : BlockVector3.at(random.nextInt(area), random.nextInt(area) - area / 2, random.nextInt(area));
                    change = new BlockChange(position, blocks[random.nextInt(blocks.length)],
                        blocks[random.nextInt(blocks.length)]);
                }
                if (random.nextInt(100) == 0) {
                    // changes are ignored while not recording
                    history.setRecordChanges(false);
                    history.add(change);
                    history.setRecordChanges(true);
                } else {
                    history.add(change);
                    reference.add(change);
                }
                if (random.nextInt(Math.max(1, count / 4)) == 0) {
                    assertEquals(reference.size(), history.size());
                }
            }
            assertEquals(reference.size(), history.size());
            assertEquals(reference.forward(), ImmutableList.copyOf(history.forwardIterator()));
            assertEquals(reference.backward(), ImmutableList.copyOf(history.backwardIterator()));
        }
    }

    @Test
    void iteratorsAreExhausted() {
        BlockOptimizedHistory history = new BlockOptimizedHistory();
        history.add(new BlockChange(BlockVector3.ZERO, blocks[0], blocks[1]));
        for (Iterator<Change> iterator : List.of(history.forwardIterator(), history.backwardIterator())) {
            iterator.next();
            assertFalse(iterator.hasNext());
            assertThrows(NoSuchElementException.class, iterator::next);
        }
    }
}
