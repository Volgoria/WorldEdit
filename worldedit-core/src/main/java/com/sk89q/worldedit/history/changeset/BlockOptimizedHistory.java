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

import com.sk89q.worldedit.history.change.BlockChange;
import com.sk89q.worldedit.history.change.Change;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.collection.BlockMap;
import com.sk89q.worldedit.world.block.BaseBlock;

import java.util.Iterator;
import java.util.NoSuchElementException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An extension of {@link ArrayListHistory} that stores {@link BlockChange}s
 * separately, keeping only the first previous block and the last current
 * block of each position.
 *
 * <p>Whether this is a good idea or not is highly questionable, but this class
 * exists because this is how history was implemented in WorldEdit for
 * many years.</p>
 */
public class BlockOptimizedHistory extends ArrayListHistory {

    /**
     * The last current block of each changed position.
     */
    private final BlockMap<BaseBlock> current = BlockMap.createForBaseBlock();
    /**
     * Each changed position, in the order first changed, with its first previous block.
     */
    private final BlockChangeLog previous = new BlockChangeLog(true);
    /**
     * Every block change's position, in order, if any position was changed
     * more than once. Otherwise, this would be the same as the positions of
     * {@link #previous}, and is {@code null}.
     */
    private BlockChangeLog currentOrder;

    @Override
    public void add(Change change) {
        checkNotNull(change);

        if (isRecordingChanges()) {
            if (change instanceof BlockChange blockChange) {
                // Kept out of line so that this method stays small enough to be inlined
                // into ChangeSetExtent, where the BlockChange can then be scalar-replaced
                addBlockChange(blockChange.position(), blockChange.previous(), blockChange.current());
            } else {
                super.add(change);
            }
        }
    }

    private void addBlockChange(BlockVector3 position, BaseBlock previousBlock, BaseBlock currentBlock) {
        if (current.put(position, currentBlock) == null) {
            previous.add(position, previousBlock);
            if (currentOrder != null) {
                currentOrder.add(position, null);
            }
        } else {
            if (currentOrder == null) {
                currentOrder = previous.copyPositions();
            }
            currentOrder.add(position, null);
        }
    }

    @Override
    public Iterator<Change> forwardIterator() {
        Iterator<Change> changes = super.forwardIterator();
        return new Iterator<>() {
            private int index;

            private BlockChangeLog order() {
                return currentOrder != null ? currentOrder : previous;
            }

            @Override
            public boolean hasNext() {
                return changes.hasNext() || index < order().size();
            }

            @Override
            public Change next() {
                if (changes.hasNext()) {
                    return changes.next();
                }
                BlockChangeLog order = order();
                if (index >= order.size()) {
                    throw new NoSuchElementException();
                }
                BlockVector3 position = order.position(index++);
                BaseBlock block = current.get(position);
                return new BlockChange(position, block, block);
            }
        };
    }

    @Override
    public Iterator<Change> backwardIterator() {
        Iterator<Change> changes = super.backwardIterator();
        return new Iterator<>() {
            private int index = previous.size();

            @Override
            public boolean hasNext() {
                return changes.hasNext() || index > 0;
            }

            @Override
            public Change next() {
                if (changes.hasNext()) {
                    return changes.next();
                }
                if (index <= 0) {
                    throw new NoSuchElementException();
                }
                index--;
                BaseBlock block = previous.block(index);
                return new BlockChange(previous.position(index), block, block);
            }
        };
    }

    @Override
    public int size() {
        return super.size() + previous.size();
    }
}
