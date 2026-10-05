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

package com.sk89q.worldedit;

import com.sk89q.worldedit.function.mask.BlockTypeMask;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.util.test.InMemoryWorld;
import com.sk89q.worldedit.util.test.SimpleMaterialRegistries;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.when;

/**
 * Pins the behaviour of {@link EditSession} operations that hard-code vanilla
 * block types (via {@link BlockTypes}) on an in-memory world.
 *
 * <p>{@link BlockTypes} resolves its constants once, when first loaded, so these
 * tests run in a JVM of their own (the {@code isolatedRegistryTest} task), and are
 * skipped if anything loaded it before the vanilla types were registered.</p>
 */
@Tag("isolated-registry")
@DisplayName("EditSession operations on vanilla blocks")
class EditSessionVanillaBlocksTest extends BaseWorldEditTest {

    private static final List<String> IDS = List.of(
        "minecraft:air", "minecraft:stone", "minecraft:water", "minecraft:lava",
        "minecraft:ice", "minecraft:snow", "minecraft:dirt", "minecraft:coarse_dirt",
        "minecraft:grass_block"
    );

    static {
        // Must happen before anything loads BlockTypes, so not in a @BeforeAll method
        for (String id : IDS) {
            if (BlockType.REGISTRY.get(id) == null) {
                BlockType.REGISTRY.register(id, new BlockType(id));
            }
        }
    }

    @BeforeAll
    static void useSimpleMaterials() {
        when(MOCKED_PLATFORM.getRegistries()).thenReturn(SimpleMaterialRegistries.create());
    }

    @BeforeEach
    void requireVanillaTypes() {
        for (String id : IDS) {
            assumeTrue(BlockTypes.get(id) != null, "vanilla block types are not registered");
        }
        assumeTrue(BlockTypes.AIR != null && BlockTypes.WATER != null && BlockTypes.LAVA != null
            && BlockTypes.ICE != null && BlockTypes.SNOW != null && BlockTypes.DIRT != null
            && BlockTypes.COARSE_DIRT != null && BlockTypes.GRASS_BLOCK != null,
            "BlockTypes was loaded before the vanilla block types were registered");
        assumeTrue(BlockTypes.AIR.getMaterial().isAir() && BlockTypes.WATER.getMaterial().isLiquid()
            && BlockTypes.STONE.getMaterial().isSolid(), "block materials come from another registry");
    }

    private static BlockState state(String id) {
        return BlockTypes.get(id).getDefaultState();
    }

    private static InMemoryWorld basin() {
        InMemoryWorld world = new InMemoryWorld(state("minecraft:air"), -64, 319);
        for (BlockVector3 pos : new CuboidRegion(BlockVector3.at(-6, 0, -6), BlockVector3.at(6, 4, 6))) {
            boolean wall = Math.abs(pos.x()) == 6 || Math.abs(pos.z()) == 6 || pos.y() == 0;
            world.blocks().put(pos, wall ? state("minecraft:stone") : state("minecraft:water"));
        }
        world.blocks().put(BlockVector3.at(0, 4, 0), state("minecraft:ice"));
        world.blocks().put(BlockVector3.at(1, 5, 1), state("minecraft:snow"));
        world.blocks().put(BlockVector3.at(6, 4, 6), state("minecraft:dirt"));
        world.blocks().put(BlockVector3.at(-6, 4, 6), state("minecraft:coarse_dirt"));
        return world;
    }

    @FunctionalInterface
    private interface Edit {
        int apply(EditSession session) throws Exception;
    }

    private static String run(InMemoryWorld world, Edit edit) throws Exception {
        int affected;
        try (EditSession session = WorldEdit.getInstance().newEditSessionBuilder().world(world.world()).build()) {
            affected = edit.apply(session);
        }
        Map<String, Long> counts = world.blocks().values().stream()
            .collect(Collectors.groupingBy(b -> b.getBlockType().id(), java.util.TreeMap::new, Collectors.counting()));
        return affected + " " + counts;
    }

    @Test
    void drainArea() throws Exception {
        assertEquals("239 {minecraft:air=239, minecraft:coarse_dirt=1, minecraft:dirt=1, minecraft:ice=1, minecraft:snow=1, minecraft:stone=359, minecraft:water=244}", run(basin(), s -> s.drainArea(BlockVector3.at(0, 3, 0), 4)));
    }

    @Test
    void fixLiquid() throws Exception {
        InMemoryWorld world = basin();
        for (BlockVector3 pos : new CuboidRegion(BlockVector3.at(-5, 2, -5), BlockVector3.at(5, 4, 0))) {
            world.blocks().put(pos, state("minecraft:air"));
        }
        assertEquals("258 {minecraft:air=134, minecraft:coarse_dirt=1, minecraft:dirt=1, minecraft:snow=1, minecraft:stone=359, minecraft:water=350}", run(world, s -> s.fixLiquid(BlockVector3.at(0, 3, 2), 5, BlockTypes.WATER)));
    }

    @Test
    void removeAbove() throws Exception {
        assertEquals("50 {minecraft:air=50, minecraft:coarse_dirt=1, minecraft:dirt=1, minecraft:snow=1, minecraft:stone=359, minecraft:water=434}", run(basin(), s -> s.removeAbove(BlockVector3.at(0, 3, 0), 3, 2)));
    }

    @Test
    void removeBelow() throws Exception {
        assertEquals("27 {minecraft:air=27, minecraft:coarse_dirt=1, minecraft:dirt=1, minecraft:ice=1, minecraft:snow=1, minecraft:stone=359, minecraft:water=456}", run(basin(), s -> s.removeBelow(BlockVector3.at(2, 3, 0), 2, 3)));
    }

    @Test
    void removeNear() throws Exception {
        assertEquals("43 {minecraft:air=43, minecraft:coarse_dirt=1, minecraft:dirt=1, minecraft:ice=1, minecraft:snow=1, minecraft:stone=316, minecraft:water=483}", run(basin(), s -> s.removeNear(BlockVector3.at(5, 2, 5), new BlockTypeMask(s, BlockTypes.STONE), 3)));
    }

    @Test
    void thaw() throws Exception {
        assertEquals("2 {minecraft:air=1, minecraft:coarse_dirt=1, minecraft:dirt=1, minecraft:stone=359, minecraft:water=484}", run(basin(), s -> s.thaw(BlockVector3.at(0, 4, 0), 3, 5)));
    }

    @Test
    void green() throws Exception {
        assertEquals("2 {minecraft:grass_block=2, minecraft:ice=1, minecraft:snow=1, minecraft:stone=359, minecraft:water=483}", run(basin(), s -> s.green(BlockVector3.at(0, 4, 0), 9, 5, false)));
    }

    @Test
    void greenOnlyNormalDirt() throws Exception {
        assertEquals("1 {minecraft:coarse_dirt=1, minecraft:grass_block=1, minecraft:ice=1, minecraft:snow=1, minecraft:stone=359, minecraft:water=483}", run(basin(), s -> s.green(BlockVector3.at(0, 4, 0), 9, 5, true)));
    }

}
