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

package com.sk89q.worldedit.extent.clipboard.io;

import com.google.common.collect.ImmutableMap;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.registry.Registry;
import com.sk89q.worldedit.registry.state.BooleanProperty;
import com.sk89q.worldedit.registry.state.DirectionalProperty;
import com.sk89q.worldedit.registry.state.EnumProperty;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.entity.EntityType;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import com.sk89q.worldedit.world.registry.BlockRegistry;
import com.sk89q.worldedit.world.registry.Registries;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Registers a few block and entity types with properties for clipboard format tests.
 *
 * <p>The block registry knows about air (any id ending in {@code :air}) and the properties of
 * {@link #STAIRS}, which the bundled registry does not provide.</p>
 */
public final class ClipboardIoTestSupport {

    public static final String AIR = "minecraft:air";
    public static final String STONE = "minecraft:stone";
    public static final String GLASS = "minecraft:glass";
    public static final String CHEST = "minecraft:chest";
    public static final String STAIRS = "minecraft:oak_stairs";
    public static final String STRUCTURE_VOID = "minecraft:structure_void";
    public static final String PIG = "minecraft:pig";

    private static final List<String> REGISTERED_BLOCKS = new ArrayList<>();
    private static final List<String> REGISTERED_ENTITIES = new ArrayList<>();

    private static final Map<String, Map<String, ? extends Property<?>>> PROPERTIES = ImmutableMap.of(
        STAIRS, ImmutableMap.of(
            "facing", new DirectionalProperty("facing",
                List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)),
            "half", new EnumProperty("half", List.of("top", "bottom")),
            "waterlogged", new BooleanProperty("waterlogged", List.of(true, false))
        ),
        CHEST, ImmutableMap.of(
            "facing", new DirectionalProperty("facing",
                List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST))
        )
    );

    /**
     * Install the test registries on the given (mocked) platform and register the test types.
     *
     * @param platform the mocked platform
     */
    public static void install(Platform platform) {
        BlockRegistry blockRegistry = new BlockRegistry() {
            @Override
            public Component getRichName(BlockType blockType) {
                return TextComponent.of(blockType.id());
            }

            @Override
            public BlockMaterial getMaterial(BlockType blockType) {
                BlockMaterial material = mock(BlockMaterial.class);
                boolean air = blockType.id().endsWith(":air");
                boolean transparent = air || blockType.id().equals(GLASS) || blockType.id().equals(STRUCTURE_VOID);
                when(material.isAir()).thenReturn(air);
                when(material.isFullCube()).thenReturn(!air && !blockType.id().equals(STAIRS));
                when(material.isOpaque()).thenReturn(!transparent);
                return material;
            }

            @Override
            public Map<String, ? extends Property<?>> getProperties(BlockType blockType) {
                return PROPERTIES.getOrDefault(blockType.id(), ImmutableMap.of());
            }

            @Override
            public OptionalInt getInternalBlockStateId(BlockState state) {
                return OptionalInt.empty();
            }
        };
        Registries registries = mock(Registries.class);
        when(registries.getBlockRegistry()).thenReturn(blockRegistry);
        when(platform.getRegistries()).thenReturn(registries);

        // The air types are already registered for every test (see CommonBlockTypesSessionListener),
        // so BlockTypes.AIR is set whichever test loads BlockTypes first; the rest are only used here.
        for (String id : List.of(AIR, STONE, GLASS, CHEST, STAIRS, STRUCTURE_VOID, "minecraft:oak_wood")) {
            if (BlockType.REGISTRY.get(id) == null) {
                BlockType.REGISTRY.register(id, new BlockType(id));
                REGISTERED_BLOCKS.add(id);
            }
        }
        if (EntityType.REGISTRY.get(PIG) == null) {
            EntityType.REGISTRY.register(PIG, new EntityType(PIG));
            REGISTERED_ENTITIES.add(PIG);
        }
    }

    /**
     * Remove the types registered by {@link #install(Platform)}, so other tests can register their own.
     */
    public static void uninstall() {
        try {
            Field field = Registry.class.getDeclaredField("map");
            field.setAccessible(true);
            Map<?, ?> blocks = (Map<?, ?>) field.get(BlockType.REGISTRY);
            REGISTERED_BLOCKS.forEach(blocks::remove);
            REGISTERED_BLOCKS.clear();
            Map<?, ?> entities = (Map<?, ?>) field.get(EntityType.REGISTRY);
            REGISTERED_ENTITIES.forEach(entities::remove);
            REGISTERED_ENTITIES.clear();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Get the default state of a registered test block.
     *
     * @param id the block id
     * @return the default state
     */
    public static BlockState state(String id) {
        return checkNotNull(BlockType.REGISTRY.get(id), id).getDefaultState();
    }

    private ClipboardIoTestSupport() {
    }
}
