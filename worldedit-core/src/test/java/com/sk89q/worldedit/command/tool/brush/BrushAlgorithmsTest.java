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

package com.sk89q.worldedit.command.tool.brush;

import com.sk89q.worldedit.BaseWorldEditTest;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.BlockTypeMask;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import com.sk89q.worldedit.world.registry.BlockRegistry;
import com.sk89q.worldedit.world.registry.Registries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Random;
import java.util.Set;
import javax.annotation.Nullable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Brush algorithms")
class BrushAlgorithmsTest extends BaseWorldEditTest {

    private static BlockState air;
    private static BlockState stone;
    private static BlockState water;
    private static BlockState grass;

    @BeforeAll
    static void setUpBlocks() {
        // The bundled registry has no notion of air, so provide one that does
        BlockRegistry blockRegistry = new BlockRegistry() {
            @Override
            public com.sk89q.worldedit.util.formatting.text.Component getRichName(BlockType blockType) {
                return TextComponent.of(blockType.id());
            }

            @Override
            public BlockMaterial getMaterial(BlockType blockType) {
                BlockMaterial material = mock(BlockMaterial.class);
                when(material.isAir()).thenReturn(blockType.id().endsWith(":air"));
                when(material.isLiquid()).thenReturn(blockType.id().endsWith(":water"));
                return material;
            }

            @Override
            public Map<String, ? extends com.sk89q.worldedit.registry.state.Property<?>> getProperties(BlockType blockType) {
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

        air = new BlockType("brushtest:air").getDefaultState();
        stone = new BlockType("brushtest:stone").getDefaultState();
        water = new BlockType("brushtest:water").getDefaultState();
        grass = new BlockType("brushtest:grass").getDefaultState();
    }

    /**
     * A simple in-memory extent between y = -16 and y = 16.
     */
    private static final class TestExtent implements Extent {
        private final Map<BlockVector3, BlockState> blocks = new HashMap<>();

        @Override
        public BlockVector3 getMinimumPoint() {
            return BlockVector3.at(-64, -16, -64);
        }

        @Override
        public BlockVector3 getMaximumPoint() {
            return BlockVector3.at(64, 16, 64);
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
            blocks.put(position, block.toImmutableState());
            return true;
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
    }

    /**
     * Ground made of stone at and below y = 0, within the given horizontal range.
     */
    private static TestExtent flatGround(int range) {
        TestExtent extent = new TestExtent();
        extent.fill(-range, -16, -range, range, 0, range, stone);
        return extent;
    }

    private static int circleColumns(double radius) {
        int r = (int) Math.floor(radius);
        double radiusSq = (radius + 0.5) * (radius + 0.5);
        int count = 0;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz <= radiusSq) {
                    count++;
                }
            }
        }
        return count;
    }

    @Test
    @DisplayName("blob with no roughness is a sphere")
    void blobWithoutRoughnessIsSphere() throws Exception {
        TestExtent extent = new TestExtent();
        BlockVector3 center = BlockVector3.at(0, 0, 0);
        double radius = 4;
        new BlobBrush(0, new Random(1)).apply(extent, center, stone, radius);

        Set<BlockVector3> placed = extent.positionsOf(stone);
        for (int x = -6; x <= 6; x++) {
            for (int y = -6; y <= 6; y++) {
                for (int z = -6; z <= 6; z++) {
                    BlockVector3 pos = BlockVector3.at(x, y, z);
                    boolean inside = pos.toVector3().length() <= radius + 0.5;
                    assertEquals(inside, placed.contains(pos), "Unexpected block state at " + pos);
                }
            }
        }
    }

    @Test
    @DisplayName("blob stays within its noise bounds")
    void blobStaysWithinBounds() throws Exception {
        TestExtent extent = new TestExtent();
        BlockVector3 center = BlockVector3.at(3, 2, -5);
        double radius = 8;
        BlobBrush brush = new BlobBrush(100, new Random(42));
        brush.apply(extent, center, stone, radius);

        double amplitude = brush.getAmplitude();
        double inner = radius * (1 - amplitude) + 0.5;
        double outer = radius * (1 + amplitude) + 0.5;
        Set<BlockVector3> placed = extent.positionsOf(stone);
        assertTrue(placed.contains(center));
        int bound = (int) Math.ceil(outer) + 1;
        for (int x = -bound; x <= bound; x++) {
            for (int y = -bound; y <= bound; y++) {
                for (int z = -bound; z <= bound; z++) {
                    BlockVector3 pos = center.add(x, y, z);
                    double distance = BlockVector3.at(x, y, z).toVector3().length();
                    if (distance <= inner) {
                        assertTrue(placed.contains(pos), "Expected block inside the blob at " + pos);
                    } else if (distance > outer) {
                        assertTrue(!placed.contains(pos), "Expected no block outside the blob at " + pos);
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("blob shape depends on the seed")
    void blobShapeDependsOnSeed() throws Exception {
        BlockVector3 center = BlockVector3.at(0, 0, 0);
        TestExtent first = new TestExtent();
        new BlobBrush(80, new Random(7)).apply(first, center, stone, 6);
        TestExtent same = new TestExtent();
        new BlobBrush(80, new Random(7)).apply(same, center, stone, 6);
        TestExtent other = new TestExtent();
        new BlobBrush(80, new Random(8)).apply(other, center, stone, 6);

        assertEquals(first.positionsOf(stone), same.positionsOf(stone));
        assertNotEquals(first.positionsOf(stone), other.positionsOf(stone));
    }

    @Test
    @DisplayName("overlay places layers on top of the surface")
    void overlayPlacesLayers() throws Exception {
        TestExtent extent = flatGround(10);
        new OverlayBrush(2, false).apply(extent, BlockVector3.at(0, 0, 0), grass, 3);

        Set<BlockVector3> placed = extent.positionsOf(grass);
        assertEquals(circleColumns(3) * 2, placed.size());
        for (BlockVector3 pos : placed) {
            assertTrue(pos.y() == 1 || pos.y() == 2, "Unexpected layer height at " + pos);
        }
    }

    @Test
    @DisplayName("overlay in replace mode re-surfaces the terrain")
    void overlayReplacesSurface() throws Exception {
        TestExtent extent = flatGround(10);
        new OverlayBrush(2, true).apply(extent, BlockVector3.at(0, 0, 0), grass, 3);

        Set<BlockVector3> placed = extent.positionsOf(grass);
        assertEquals(circleColumns(3) * 2, placed.size());
        for (BlockVector3 pos : placed) {
            assertTrue(pos.y() == 0 || pos.y() == -1, "Unexpected replaced height at " + pos);
        }
        assertEquals(air, extent.getBlock(BlockVector3.at(0, 1, 0)));
    }

    @Test
    @DisplayName("overlay only covers the topmost surface of a column")
    void overlayCoversTopmostSurface() throws Exception {
        TestExtent extent = flatGround(10);
        // A floating ledge over the origin
        extent.put(BlockVector3.at(0, 2, 0), stone);
        // A pillar that rises above the brush range, so its top is out of reach
        extent.fill(1, 1, 0, 1, 16, 0, stone);
        new OverlayBrush(1, false).apply(extent, BlockVector3.at(0, 0, 0), grass, 3);

        assertEquals(grass, extent.getBlock(BlockVector3.at(0, 3, 0)));
        assertEquals(air, extent.getBlock(BlockVector3.at(0, 1, 0)));
        assertTrue(extent.positionsOf(grass).stream().noneMatch(pos -> pos.x() == 1 && pos.z() == 0));
        assertEquals(circleColumns(3) - 1, extent.positionsOf(grass).size());
    }

    @Test
    @DisplayName("fill fills depressions up to the targeted level")
    void fillFillsDepressions() throws Exception {
        TestExtent extent = flatGround(10);
        // A pit four blocks deep, and a shallow dent next to it
        extent.fill(0, -3, 0, 0, 0, 0, air);
        extent.put(BlockVector3.at(1, 0, 0), air);
        new FillBrush(5).apply(extent, BlockVector3.at(0, 0, 0), water, 2);

        assertEquals(Set.of(
            BlockVector3.at(0, 0, 0),
            BlockVector3.at(0, -1, 0),
            BlockVector3.at(0, -2, 0),
            BlockVector3.at(0, -3, 0),
            BlockVector3.at(1, 0, 0)
        ), extent.positionsOf(water));
    }

    @Test
    @DisplayName("fill leaves columns without ground in range alone")
    void fillSkipsBottomlessColumns() throws Exception {
        TestExtent extent = flatGround(10);
        extent.fill(0, -16, 0, 0, 0, 0, air);
        new FillBrush(5).apply(extent, BlockVector3.at(0, 0, 0), water, 2);

        assertTrue(extent.positionsOf(water).isEmpty());
        assertEquals(air, extent.getBlock(BlockVector3.at(0, 0, 0)));
    }

    @Test
    @DisplayName("drain removes liquids only within the sphere")
    void drainRemovesLiquidsInSphere() throws Exception {
        TestExtent extent = new TestExtent();
        extent.fill(-8, -8, -8, 8, 8, 8, water);
        extent.put(BlockVector3.at(1, 0, 0), stone);
        DrainBrush.apply(extent, new BlockTypeMask(extent, water.getBlockType()), air, BlockVector3.at(0, 0, 0), 3);

        assertEquals(air, extent.getBlock(BlockVector3.at(0, 0, 0)));
        assertEquals(air, extent.getBlock(BlockVector3.at(0, 3, 0)));
        assertEquals(stone, extent.getBlock(BlockVector3.at(1, 0, 0)));
        assertEquals(water, extent.getBlock(BlockVector3.at(0, 5, 0)));
        assertEquals(water, extent.getBlock(BlockVector3.at(3, 3, 3)));
    }

    @Test
    @DisplayName("line brush draws between successive targets")
    void lineDrawsBetweenTargets() throws Exception {
        EditSession editSession = mock(EditSession.class);
        LineBrush brush = new LineBrush(false, false);
        BlockVector3 first = BlockVector3.at(0, 0, 0);
        BlockVector3 second = BlockVector3.at(10, 5, 0);
        BlockVector3 third = BlockVector3.at(10, 5, 10);

        brush.build(editSession, first, stone, 1);
        verifyNoInteractions(editSession);
        assertEquals(first, brush.getAnchor());

        brush.build(editSession, second, stone, 1);
        verify(editSession).drawLine(stone, first, second, 1, true);
        assertNull(brush.getAnchor());

        brush.build(editSession, third, stone, 1);
        verify(editSession, times(1)).drawLine(any(), any(BlockVector3.class), any(BlockVector3.class), anyDouble(), anyBoolean());
        assertEquals(third, brush.getAnchor());
    }

    @Test
    @DisplayName("line brush in chain mode continues from the last point")
    void lineChainsSegments() throws Exception {
        EditSession editSession = mock(EditSession.class);
        LineBrush brush = new LineBrush(true, true);
        BlockVector3 first = BlockVector3.at(0, 0, 0);
        BlockVector3 second = BlockVector3.at(10, 5, 0);
        BlockVector3 third = BlockVector3.at(10, 5, 10);

        brush.build(editSession, first, stone, 2);
        brush.build(editSession, second, stone, 2);
        brush.build(editSession, third, stone, 2);

        verify(editSession).drawLine(stone, first, second, 2, false);
        verify(editSession).drawLine(stone, second, third, 2, false);
        assertEquals(third, brush.getAnchor());
    }
}
