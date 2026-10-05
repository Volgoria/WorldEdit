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

package com.sk89q.worldedit.extent.inventory;

import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BlockBagTest {

    private static BlockState block(boolean air, boolean hasItem, boolean replacedDuringPlacement) {
        BlockMaterial material = mock(BlockMaterial.class);
        when(material.isAir()).thenReturn(air);
        when(material.isReplacedDuringPlacement()).thenReturn(replacedDuringPlacement);
        BlockType type = mock(BlockType.class);
        when(type.getMaterial()).thenReturn(material);
        when(type.hasItemType()).thenReturn(hasItem);
        BlockState state = mock(BlockState.class);
        when(state.getBlockType()).thenReturn(type);
        return state;
    }

    private static final class RecordingBlockBag extends BlockBag {
        private final List<BlockState> fetched = new ArrayList<>();
        private final List<BlockState> stored = new ArrayList<>();
        private boolean empty;

        @Override
        public void fetchBlock(BlockState blockState) throws BlockBagException {
            fetched.add(blockState);
            if (empty) {
                throw new OutOfBlocksException();
            }
        }

        @Override
        public void storeBlock(BlockState blockState, int amount) {
            if (!blockState.getBlockType().hasItemType()) {
                // mirrors platform implementations, which reject such blocks
                throw new IllegalArgumentException("This block cannot be stored");
            }
            stored.add(blockState);
        }

        @Override
        public void flushChanges() {
        }

        @Override
        public void addSourcePosition(Location pos) {
        }

        @Override
        public void addSingleSourcePosition(Location pos) {
        }
    }

    @Test
    void storesBlocksWithAnItemForm() throws BlockBagException {
        RecordingBlockBag bag = new RecordingBlockBag();
        BlockState stone = block(false, true, false);

        bag.storeDroppedBlock(stone);

        assertEquals(List.of(stone), bag.stored);
    }

    @Test
    void discardsAirAndBlocksWithoutAnItemForm() throws BlockBagException {
        RecordingBlockBag bag = new RecordingBlockBag();

        bag.storeDroppedBlock(block(true, true, true));
        bag.storeDroppedBlock(block(false, false, true));
        bag.storeDroppedBlock(null);

        assertTrue(bag.stored.isEmpty());
    }

    @Test
    void fetchesPlacedBlockOnce() throws BlockBagException {
        RecordingBlockBag bag = new RecordingBlockBag();
        BlockState stone = block(false, true, false);

        bag.fetchPlacedBlock(stone);

        assertEquals(List.of(stone), bag.fetched);
    }

    @Test
    void doesNotFetchBlocksReplacedDuringPlacement() throws BlockBagException {
        RecordingBlockBag bag = new RecordingBlockBag();

        bag.fetchPlacedBlock(block(false, false, true));

        assertTrue(bag.fetched.isEmpty());
    }

    @Test
    void outOfBlocksIsPropagatedWithoutRetry() {
        RecordingBlockBag bag = new RecordingBlockBag();
        bag.empty = true;
        BlockState stone = block(false, true, false);

        assertThrows(OutOfBlocksException.class, () -> bag.fetchPlacedBlock(stone));
        assertEquals(List.of(stone), bag.fetched);
    }
}
