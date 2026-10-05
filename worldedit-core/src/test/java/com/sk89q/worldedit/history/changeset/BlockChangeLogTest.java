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

import com.sk89q.worldedit.BaseWorldEditTest;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import org.enginehub.linbus.tree.LinCompoundTag;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("A BlockChangeLog")
class BlockChangeLogTest extends BaseWorldEditTest {

    private static BlockState[] states;

    @BeforeAll
    static void createStates() {
        states = new BlockState[4];
        for (int i = 0; i < states.length; i++) {
            states[i] = new BlockType("changelogtest:block_" + i).getDefaultState();
        }
    }

    private static BlockVector3 randomPosition(Random random) {
        return switch (random.nextInt(4)) {
            // packable
            case 0, 1 -> BlockVector3.at(random.nextInt(2000) - 1000, random.nextInt(4096) - 2048,
                random.nextInt(2000) - 1000);
            // y beyond the packable range
            case 2 -> BlockVector3.at(random.nextInt(2000) - 1000, random.nextInt(), random.nextInt(2000) - 1000);
            // anything
            default -> BlockVector3.at(random.nextInt(), random.nextInt(), random.nextInt());
        };
    }

    @Test
    void storesPositionsAndBlocksInOrder() {
        Random random = new Random(42);
        for (int round = 0; round < 20; round++) {
            BlockChangeLog log = new BlockChangeLog(true);
            List<BlockVector3> positions = new ArrayList<>();
            List<BaseBlock> blocks = new ArrayList<>();
            // cross several chunk boundaries, sometimes with only packable positions
            int count = random.nextInt(3) == 0 ? random.nextInt(50) : random.nextInt(20_000);
            boolean onlyPackable = random.nextBoolean();
            for (int i = 0; i < count; i++) {
                BlockVector3 position = onlyPackable
                    ? BlockVector3.at(random.nextInt(100), random.nextInt(100), random.nextInt(100))
                    : randomPosition(random);
                BaseBlock block = states[random.nextInt(states.length)].toBaseBlock();
                log.add(position, block);
                positions.add(position);
                blocks.add(block);
                assertEquals(i + 1, log.size());
            }
            for (int i = 0; i < count; i++) {
                assertEquals(positions.get(i), log.position(i), "position " + i);
                assertSame(blocks.get(i), log.block(i), "block " + i);
            }
            BlockChangeLog copy = log.copyPositions();
            assertEquals(count, copy.size());
            for (int i = 0; i < count; i++) {
                assertEquals(positions.get(i), copy.position(i), "copied position " + i);
            }
            if (count > 0) {
                assertThrows(IllegalStateException.class, () -> copy.block(0));
            }
        }
    }

    @Test
    void storesBlocksWithoutNbtAsSharedInstances() {
        BlockChangeLog log = new BlockChangeLog(true);
        BaseBlock unshared = new BaseBlock(states[0]) {
        };
        BaseBlock withNbt = states[1].toBaseBlock(LinCompoundTag.builder().putString("a", "b").build());
        log.add(BlockVector3.ZERO, unshared);
        log.add(BlockVector3.ONE, withNbt);
        assertSame(states[0].toBaseBlock(), log.block(0));
        assertSame(withNbt, log.block(1));
    }

    @Test
    void widensAChunkWhenAPositionCannotBePacked() {
        BlockChangeLog log = new BlockChangeLog(false);
        BlockVector3 packable = BlockVector3.at(-5, 100, 7);
        BlockVector3 wide = BlockVector3.at(1 << 28, 1 << 20, -(1 << 28));
        log.add(packable, null);
        log.add(wide, null);
        log.add(packable, null);
        assertEquals(packable, log.position(0));
        assertEquals(wide, log.position(1));
        assertEquals(packable, log.position(2));
    }
}
