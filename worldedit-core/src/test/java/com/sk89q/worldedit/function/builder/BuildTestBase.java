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

package com.sk89q.worldedit.function.builder;

import com.sk89q.worldedit.BaseWorldEditTest;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.registry.state.DirectionalProperty;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import com.sk89q.worldedit.world.registry.BlockRegistry;
import com.sk89q.worldedit.world.registry.Registries;
import org.junit.jupiter.api.BeforeAll;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import javax.annotation.Nullable;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Shared set-up for the builder algorithm tests: a block registry that knows
 * about air and a "facing" property, and an in-memory extent.
 */
abstract class BuildTestBase extends BaseWorldEditTest {

    static final DirectionalProperty FACING = new DirectionalProperty("facing",
        List.of(Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST));

    static BlockState air;
    static BlockState stone;
    static BlockState dirt;
    static BlockState grass;
    static BlockState gold;
    static BlockState stairs;

    @BeforeAll
    static void setUpBlocks() {
        BlockRegistry blockRegistry = new BlockRegistry() {
            @Override
            public Component getRichName(BlockType blockType) {
                return TextComponent.of(blockType.id());
            }

            @Override
            public BlockMaterial getMaterial(BlockType blockType) {
                BlockMaterial material = mock(BlockMaterial.class);
                when(material.isAir()).thenReturn(blockType.id().endsWith(":air"));
                return material;
            }

            @Override
            public Map<String, ? extends Property<?>> getProperties(BlockType blockType) {
                if (blockType.id().endsWith(":stairs")) {
                    return Map.of("facing", FACING);
                }
                return Collections.emptyMap();
            }

            @Override
            public OptionalInt getInternalBlockStateId(BlockState state) {
                return OptionalInt.empty();
            }
        };
        Registries registries = mock(Registries.class);
        when(registries.getBlockRegistry()).thenReturn(blockRegistry);
        when(MOCKED_PLATFORM.getRegistries()).thenReturn(registries);

        air = new BlockType("buildtest:air").getDefaultState();
        stone = new BlockType("buildtest:stone").getDefaultState();
        dirt = new BlockType("buildtest:dirt").getDefaultState();
        grass = new BlockType("buildtest:grass").getDefaultState();
        gold = new BlockType("buildtest:gold").getDefaultState();
        stairs = new BlockType("buildtest:stairs").getDefaultState();
    }

    /**
     * A simple in-memory extent between y = -32 and y = 32, filled with air.
     */
    static final class TestExtent implements Extent {
        private final Map<BlockVector3, BlockState> blocks = new HashMap<>();
        int setCount;

        @Override
        public BlockVector3 getMinimumPoint() {
            return BlockVector3.at(-64, -32, -64);
        }

        @Override
        public BlockVector3 getMaximumPoint() {
            return BlockVector3.at(64, 32, 64);
        }

        @Override
        public List<? extends Entity> getEntities(Region region) {
            return Collections.emptyList();
        }

        @Override
        public List<? extends Entity> getEntities() {
            return Collections.emptyList();
        }

        @Nullable
        @Override
        public Entity createEntity(Location location, BaseEntity entity) {
            return null;
        }

        @Override
        public BlockState getBlock(BlockVector3 position) {
            return blocks.getOrDefault(position, air);
        }

        @Override
        public BaseBlock getFullBlock(BlockVector3 position) {
            return getBlock(position).toBaseBlock();
        }

        @Override
        public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 position, T block) {
            setCount++;
            BlockState state = block.toImmutableState();
            BlockState previous = blocks.put(position, state);
            return previous == null ? !state.equals(air) : !previous.equals(state);
        }

        @Nullable
        @Override
        public Operation commit() {
            return null;
        }

        void put(BlockVector3 position, BlockState block) {
            blocks.put(position, block);
        }

        void fill(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, BlockState block) {
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        put(BlockVector3.at(x, y, z), block);
                    }
                }
            }
        }

        Set<BlockVector3> positionsOf(BlockState block) {
            Set<BlockVector3> result = new HashSet<>();
            blocks.forEach((pos, state) -> {
                if (state.equals(block)) {
                    result.add(pos);
                }
            });
            return result;
        }

        Set<BlockVector3> nonAir() {
            Set<BlockVector3> result = new HashSet<>();
            blocks.forEach((pos, state) -> {
                if (!state.equals(air)) {
                    result.add(pos);
                }
            });
            return result;
        }
    }

}
