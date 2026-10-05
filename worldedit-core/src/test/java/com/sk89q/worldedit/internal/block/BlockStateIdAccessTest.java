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

package com.sk89q.worldedit.internal.block;

import com.sk89q.worldedit.BaseWorldEditTest;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Extends BaseWorldEditTest to share its lock: the ID registry is global
@DisplayName("Block state internal IDs")
class BlockStateIdAccessTest extends BaseWorldEditTest {

    private static BlockState newState(String name) {
        return new BlockType("idaccesstest:" + name).getDefaultState();
    }

    @Test
    @DisplayName("assigns and resolves WorldEdit-provided IDs")
    void providedIds() {
        BlockState state = newState("provided");
        BlockStateIdAccess.register(state, BlockStateIdAccess.invalidId());
        int id = BlockStateIdAccess.getBlockStateId(state);
        assertTrue(BlockStateIdAccess.isValidInternalId(id));
        assertSame(state, BlockStateIdAccess.getBlockStateById(id));
    }

    @Test
    @DisplayName("resolves platform IDs, small and large")
    void platformIds() {
        BlockState small = newState("small");
        BlockState large = newState("large");
        BlockStateIdAccess.register(small, 70_001);
        BlockStateIdAccess.register(large, 5_000_001);
        assertEquals(70_001, BlockStateIdAccess.getBlockStateId(small));
        assertSame(small, BlockStateIdAccess.getBlockStateById(70_001));
        assertSame(large, BlockStateIdAccess.getBlockStateById(5_000_001));
        assertNull(BlockStateIdAccess.getBlockStateById(70_002));
        assertNull(BlockStateIdAccess.getBlockStateById(5_000_002));
        assertNull(BlockStateIdAccess.getBlockStateById(BlockStateIdAccess.invalidId()));
    }

    @Test
    @DisplayName("refuses a duplicate ID for a different state")
    void refusesDuplicates() {
        BlockState first = newState("first");
        BlockState second = newState("second");
        BlockStateIdAccess.register(first, 70_100);
        // Re-registering the same state is fine
        BlockStateIdAccess.register(first, 70_100);
        assertThrows(IllegalStateException.class, () -> BlockStateIdAccess.register(second, 70_100));
        assertSame(first, BlockStateIdAccess.getBlockStateById(70_100));
    }
}
