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

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.test.InMemoryWorld;
import com.sk89q.worldedit.util.test.SimpleMaterialRegistries;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@DisplayName("EditSession hollow")
class EditSessionHollowTest extends BaseWorldEditTest {

    private static final List<BlockVector3> DIRECTIONS = List.of(
        BlockVector3.UNIT_X, BlockVector3.UNIT_MINUS_X,
        BlockVector3.UNIT_Y, BlockVector3.UNIT_MINUS_Y,
        BlockVector3.UNIT_Z, BlockVector3.UNIT_MINUS_Z
    );

    private static BlockState air;
    private static BlockState stone;
    private static BlockState glass;

    @BeforeAll
    static void setUpBlocks() {
        when(MOCKED_PLATFORM.getRegistries()).thenReturn(SimpleMaterialRegistries.create());
        air = new BlockType("hollowtest:air").getDefaultState();
        stone = new BlockType("hollowtest:stone").getDefaultState();
        glass = new BlockType("hollowtest:glass").getDefaultState();
    }

    private static boolean isSolid(InMemoryWorld world, BlockVector3 position) {
        return world.world().getBlock(position).getBlockType().getMaterial().isSolid();
    }

    /**
     * A direct implementation of the documented hollowing behaviour.
     */
    private static Set<BlockVector3> expectedHollowed(InMemoryWorld world, Region region, int thickness) {
        Set<BlockVector3> outside = new HashSet<>();
        ArrayDeque<BlockVector3> queue = new ArrayDeque<>();
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        for (BlockVector3 position : region) {
            boolean onFace = position.x() == min.x() || position.x() == max.x()
                || position.y() == min.y() || position.y() == max.y()
                || position.z() == min.z() || position.z() == max.z();
            if (onFace) {
                queue.add(position);
            }
        }
        while (!queue.isEmpty()) {
            BlockVector3 current = queue.poll();
            if (isSolid(world, current) || !outside.add(current) || !region.contains(current)) {
                continue;
            }
            for (BlockVector3 direction : DIRECTIONS) {
                queue.add(current.add(direction));
            }
        }
        for (int i = 1; i < thickness; i++) {
            Set<BlockVector3> grown = new HashSet<>();
            for (BlockVector3 position : region) {
                if (DIRECTIONS.stream().anyMatch(d -> outside.contains(position.add(d)))) {
                    grown.add(position);
                }
            }
            outside.addAll(grown);
        }
        Set<BlockVector3> result = new HashSet<>();
        for (BlockVector3 position : region) {
            if (DIRECTIONS.stream().noneMatch(d -> outside.contains(position.add(d)))) {
                result.add(position);
            }
        }
        return result;
    }

    @ParameterizedTest(name = "thickness {0}")
    @ValueSource(ints = {1, 2, 3})
    void hollowsSphereWithSealedPocket(int thickness) throws Exception {
        InMemoryWorld world = new InMemoryWorld(air, -64, 319);
        BlockVector3 center = BlockVector3.at(8, 8, 8);
        CuboidRegion region = new CuboidRegion(BlockVector3.ZERO, BlockVector3.at(16, 16, 16));
        for (BlockVector3 position : region) {
            double distance = position.distance(center);
            if (distance <= 6 && distance > 1.5) {
                world.blocks().put(position, stone);
            }
        }
        // A tunnel from the edge into the sphere, so 'outside' reaches in
        for (int x = 0; x <= 8; x++) {
            world.blocks().put(BlockVector3.at(x, 4, 8), air);
        }

        Set<BlockVector3> expected = expectedHollowed(world, region, thickness);
        assertTrue(expected.size() > 100, "Test scenario should hollow out something");
        assertTrue(expected.size() < region.getVolume() / 2, "Test scenario should keep the outside");

        int affected;
        try (EditSession session = WorldEdit.getInstance().newEditSessionBuilder().world(world.world()).build()) {
            affected = session.hollowOutRegion(region, thickness, glass);
        }

        Set<BlockVector3> glassBlocks = world.blocks().entrySet().stream()
            .filter(e -> e.getValue().equals(glass))
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
        assertEquals(expected, glassBlocks);
        assertEquals(expected.size(), affected);
    }
}
