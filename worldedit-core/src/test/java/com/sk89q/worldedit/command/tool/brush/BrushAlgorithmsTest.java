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
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.mask.BlockTypeMask;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.entity.EntityType;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import com.sk89q.worldedit.world.registry.BlockRegistry;
import com.sk89q.worldedit.world.registry.Registries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        private int createdEntities;

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
            createdEntities++;
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

    @Test
    @DisplayName("spline brush builds a curve through the clicked points")
    void splineBuildsThroughPoints() throws Exception {
        EditSession editSession = mock(EditSession.class);
        SplineBrush brush = new SplineBrush(false, 0);
        BlockVector3 first = BlockVector3.at(0, 0, 0);
        BlockVector3 second = BlockVector3.at(10, 5, 0);
        BlockVector3 third = BlockVector3.at(10, 5, 10);

        brush.build(editSession, first, stone, 2);
        brush.build(editSession, second, stone, 2);
        brush.build(editSession, third, stone, 2);
        verifyNoInteractions(editSession);
        assertEquals(List.of(first, second, third), brush.getPoints());

        // Clicking next to the last point finishes the curve
        brush.build(editSession, third.add(1, 0, 0), stone, 2);
        verify(editSession).drawSpline(stone, List.of(first, second, third), 0, 0, 0, 10, 2, true);
        assertTrue(brush.getPoints().isEmpty());
    }

    @Test
    @DisplayName("spline brush resets a lone point and caps the number of points")
    void splineResetsAndCaps() throws Exception {
        EditSession editSession = mock(EditSession.class);
        SplineBrush brush = new SplineBrush(true, 0.5);
        BlockVector3 point = BlockVector3.at(3, 3, 3);
        brush.build(editSession, point, stone, 1);
        brush.build(editSession, point, stone, 1);
        assertTrue(brush.getPoints().isEmpty());

        for (int i = 0; i < SplineBrush.MAX_POINTS + 5; i++) {
            brush.build(editSession, BlockVector3.at(i * 3, 0, 0), stone, 1);
        }
        assertEquals(SplineBrush.MAX_POINTS, brush.getPoints().size());
        assertEquals(BlockVector3.at(15, 0, 0), brush.getPoints().getFirst());
        verifyNoInteractions(editSession);
    }

    @Test
    @DisplayName("copy-paste brush copies a sphere and pastes it elsewhere")
    void copyPasteCopiesAndPastes() throws Exception {
        TestExtent extent = new TestExtent();
        extent.put(BlockVector3.at(0, 0, 0), stone);
        extent.put(BlockVector3.at(1, 0, 0), grass);
        extent.put(BlockVector3.at(0, 2, 0), water);
        // Outside the copied sphere
        extent.put(BlockVector3.at(3, 3, 0), stone);

        CopyPasteBrush brush = new CopyPasteBrush(true, false);
        assertEquals(0, brush.apply(extent, BlockVector3.at(0, 0, 0), 2));
        assertTrue(brush.hasCopy());
        assertEquals(3, brush.apply(extent, BlockVector3.at(20, 0, 20), 2));
        assertTrue(!brush.hasCopy());

        assertEquals(stone, extent.getBlock(BlockVector3.at(20, 0, 20)));
        assertEquals(grass, extent.getBlock(BlockVector3.at(21, 0, 20)));
        assertEquals(water, extent.getBlock(BlockVector3.at(20, 2, 20)));
        assertEquals(air, extent.getBlock(BlockVector3.at(23, 3, 20)));
    }

    @Test
    @DisplayName("copy-paste brush can keep its copy and paste air")
    void copyPasteKeepsCopy() throws Exception {
        TestExtent extent = new TestExtent();
        extent.put(BlockVector3.at(0, 0, 0), stone);
        extent.fill(10, 0, 0, 12, 0, 0, grass);

        CopyPasteBrush brush = new CopyPasteBrush(false, true);
        brush.apply(extent, BlockVector3.at(0, 0, 0), 1);
        brush.apply(extent, BlockVector3.at(11, 0, 0), 1);
        assertTrue(brush.hasCopy());
        brush.apply(extent, BlockVector3.at(30, 0, 0), 1);

        assertEquals(stone, extent.getBlock(BlockVector3.at(11, 0, 0)));
        // Air was pasted over the grass next to the center
        assertEquals(air, extent.getBlock(BlockVector3.at(10, 0, 0)));
        assertEquals(air, extent.getBlock(BlockVector3.at(12, 0, 0)));
        assertEquals(stone, extent.getBlock(BlockVector3.at(30, 0, 0)));
    }

    @Test
    @DisplayName("shatter cracks solid blocks along fragment borders")
    void shatterCracksAlongBorders() throws Exception {
        TestExtent extent = new TestExtent();
        extent.fill(-10, -10, -10, 10, 10, 10, stone);
        BlockVector3 center = BlockVector3.at(0, 0, 0);
        double radius = 6;
        ShatterBrush brush = new ShatterBrush(2, 1, new Random(3));
        List<Vector3> seeds = brush.createSeeds(center, radius);
        assertEquals(2, seeds.size());
        for (Vector3 seed : seeds) {
            assertTrue(seed.distance(center.toVector3()) <= radius);
        }

        Set<BlockVector3> cracks = new HashSet<>(brush.findCracks(extent, center, radius, seeds));
        assertTrue(!cracks.isEmpty());
        for (BlockVector3 pos : BrushHelper.ballPositions(center, radius)) {
            double d1 = seeds.get(0).distance(pos.toVector3());
            double d2 = seeds.get(1).distance(pos.toVector3());
            assertEquals(Math.abs(d1 - d2) < 1, cracks.contains(pos), "Unexpected crack state at " + pos);
        }
    }

    @Test
    @DisplayName("shatter leaves air and blocks outside the sphere alone")
    void shatterOnlyAffectsSolidBlocksInSphere() throws Exception {
        TestExtent extent = flatGround(12);
        new ShatterBrush(6, 1.5, new Random(5)).apply(extent, BlockVector3.at(0, 0, 0), water, 5);

        Set<BlockVector3> cracked = extent.positionsOf(water);
        assertTrue(!cracked.isEmpty());
        for (BlockVector3 pos : cracked) {
            assertTrue(pos.y() <= 0, "Air was cracked at " + pos);
            assertTrue(pos.toVector3().length() <= 5.5, "Crack outside of the sphere at " + pos);
        }
    }

    @Test
    @DisplayName("surface splatter paints patches on the surface only")
    void surfaceSplatterPaintsSurface() throws Exception {
        TestExtent extent = flatGround(12);
        BlockVector3 center = BlockVector3.at(0, 0, 0);
        SurfaceSplatterBrush brush = new SurfaceSplatterBrush(4, 3, new Random(11));
        brush.apply(extent, center, grass, 8);

        Set<BlockVector3> painted = extent.positionsOf(grass);
        assertTrue(!painted.isEmpty());
        assertTrue(painted.size() < circleColumns(8));
        for (BlockVector3 pos : painted) {
            assertEquals(0, pos.y(), "Painted below the surface at " + pos);
            assertTrue(pos.x() * pos.x() + pos.z() * pos.z() <= 8.5 * 8.5, "Painted outside the brush at " + pos);
        }

        TestExtent same = flatGround(12);
        new SurfaceSplatterBrush(4, 3, new Random(11)).apply(same, center, grass, 8);
        assertEquals(painted, same.positionsOf(grass));
    }

    @Test
    @DisplayName("layer brush layers blocks by depth below the surface")
    void layerBySurfaceDepth() throws Exception {
        TestExtent extent = flatGround(20);
        new LayerBrush(List.of(grass, water), false).apply(extent, BlockVector3.at(0, 0, 0), 4);

        assertEquals(grass, extent.getBlock(BlockVector3.at(0, 0, 0)));
        assertEquals(water, extent.getBlock(BlockVector3.at(0, -1, 0)));
        assertEquals(stone, extent.getBlock(BlockVector3.at(0, -2, 0)));
        assertEquals(air, extent.getBlock(BlockVector3.at(0, 1, 0)));
        // Outside of the sphere
        assertEquals(stone, extent.getBlock(BlockVector3.at(6, 0, 0)));
        for (BlockVector3 pos : extent.positionsOf(grass)) {
            assertEquals(0, pos.y());
        }
        for (BlockVector3 pos : extent.positionsOf(water)) {
            assertEquals(-1, pos.y());
        }
    }

    @Test
    @DisplayName("layer brush in concentric mode builds layered spheres")
    void layerConcentric() throws Exception {
        TestExtent extent = new TestExtent();
        LayerBrush brush = new LayerBrush(List.of(stone, water, grass), true);
        brush.apply(extent, BlockVector3.at(0, 0, 0), 4);

        assertEquals(stone, extent.getBlock(BlockVector3.at(0, 0, 0)));
        assertEquals(stone, extent.getBlock(BlockVector3.at(1, 0, 0)));
        assertEquals(water, extent.getBlock(BlockVector3.at(0, 2, 0)));
        assertEquals(grass, extent.getBlock(BlockVector3.at(0, 0, 4)));
        assertEquals(air, extent.getBlock(BlockVector3.at(0, 0, 5)));
        assertEquals(BrushHelper.ballPositions(BlockVector3.at(0, 0, 0), 4).size(),
            extent.positionsOf(stone).size() + extent.positionsOf(water).size() + extent.positionsOf(grass).size());
    }

    private static ClipboardHolder column(int height) {
        BlockArrayClipboard clipboard = new BlockArrayClipboard(
            new CuboidRegion(BlockVector3.at(100, 50, 100), BlockVector3.at(100, 50 + height - 1, 100)));
        try {
            for (int y = 0; y < height; y++) {
                clipboard.setBlock(BlockVector3.at(100, 50 + y, 100), stone);
            }
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        clipboard.setOrigin(BlockVector3.at(90, 40, 90));
        return new ClipboardHolder(clipboard);
    }

    @Test
    @DisplayName("schematic population plans spaced placements on the surface")
    void populatePlansSpacedPlacements() {
        TestExtent extent = flatGround(20);
        ClipboardHolder schematic = column(2);
        PopulateSchematicBrush brush = new PopulateSchematicBrush(List.of(schematic), 100, 4, true, true, new Random(9));
        List<PopulateSchematicBrush.Placement> placements = brush.findPlacements(extent, BlockVector3.at(0, 0, 0), 10);

        assertTrue(placements.size() > 3);
        for (PopulateSchematicBrush.Placement placement : placements) {
            assertEquals(1, placement.position().y());
            assertTrue(placement.rotation() % 90 == 0 && placement.rotation() >= 0 && placement.rotation() < 360);
            for (PopulateSchematicBrush.Placement other : placements) {
                if (other != placement) {
                    int dx = other.position().x() - placement.position().x();
                    int dz = other.position().z() - placement.position().z();
                    assertTrue(dx * dx + dz * dz >= 16, "Placements too close: " + placement + ", " + other);
                }
            }
        }

        PopulateSchematicBrush empty = new PopulateSchematicBrush(List.of(schematic), 0, 4, false, true, new Random(9));
        assertTrue(empty.findPlacements(extent, BlockVector3.at(0, 0, 0), 10).isEmpty());
    }

    @Test
    @DisplayName("schematic population pastes schematics standing on the surface")
    void populatePastesOnSurface() throws Exception {
        TestExtent extent = flatGround(20);
        PopulateSchematicBrush.paste(extent,
            new PopulateSchematicBrush.Placement(BlockVector3.at(3, 1, -2), column(3), 90), true);

        assertEquals(stone, extent.getBlock(BlockVector3.at(3, 1, -2)));
        assertEquals(stone, extent.getBlock(BlockVector3.at(3, 3, -2)));
        assertEquals(air, extent.getBlock(BlockVector3.at(3, 4, -2)));
        assertEquals(air, extent.getBlock(BlockVector3.at(4, 1, -2)));
    }

    @Test
    @DisplayName("schematic population never spawns the entities of the schematics")
    void populateSkipsEntities() throws Exception {
        TestExtent extent = flatGround(20);
        ClipboardHolder schematic = column(3);
        Clipboard clipboard = schematic.getClipboard();
        clipboard.createEntity(new Location(clipboard, Vector3.at(100.5, 51, 100.5)),
            new BaseEntity(new EntityType("minecraft:pig")));
        assertEquals(1, clipboard.getEntities().size());

        PopulateSchematicBrush.paste(extent, new PopulateSchematicBrush.Placement(BlockVector3.at(3, 1, -2), schematic, 0), true);

        assertEquals(stone, extent.getBlock(BlockVector3.at(3, 1, -2)));
        assertEquals(0, extent.createdEntities);
    }

    @Test
    @DisplayName("command brush expands placeholders and runs every command")
    void commandBrushRunsCommands() throws Exception {
        assertEquals(List.of("//set stone", "/up 2"), CommandBrush.parseCommands(" //set stone ; up 2;; "));
        assertEquals("//pos1 1,-2,3 r=4 w=world p=Steve",
            CommandBrush.expand("//pos1 {x},{y},{z} r={size} w={world} p={player}", BlockVector3.at(1, -2, 3), 4, "world", "Steve"));
        assertEquals("/x 2.50", CommandBrush.expand("/x {size}", BlockVector3.ZERO, 2.5, "w", "p"));

        List<String> ran = new ArrayList<>();
        CommandBrush brush = new CommandBrush("//sphere stone {size}; //pos1 {x},{y},{z}", (_, command) -> ran.add(command));
        Player player = mock(Player.class);
        World world = mock(World.class);
        when(world.getName()).thenReturn("world");
        when(player.getWorld()).thenReturn(world);
        when(player.getName()).thenReturn("Steve");
        EditSession editSession = mock(EditSession.class);
        brush.build(player, mock(LocalSession.class), editSession, BlockVector3.at(5, 6, 7), null, 3);

        assertEquals(List.of("//sphere stone 3", "//pos1 5,6,7"), ran);
        verifyNoInteractions(editSession);
    }

    @Test
    @DisplayName("command brush cannot recurse into itself and runs a bounded number of commands")
    void commandBrushIsLoopSafe() throws Exception {
        Player player = mock(Player.class);
        World world = mock(World.class);
        when(world.getName()).thenReturn("world");
        when(player.getWorld()).thenReturn(world);
        when(player.getName()).thenReturn("Steve");
        EditSession editSession = mock(EditSession.class);
        LocalSession session = mock(LocalSession.class);

        List<String> ran = new ArrayList<>();
        CommandBrush[] self = new CommandBrush[1];
        // A dispatcher whose command uses the brush again, as a command triggering a tool would
        self[0] = new CommandBrush("//a; //b", (p, command) -> {
            ran.add(command);
            self[0].build(p, session, editSession, BlockVector3.ZERO, null, 1);
        });
        self[0].build(player, session, editSession, BlockVector3.ZERO, null, 1);
        assertEquals(List.of("//a", "//b"), ran);

        // The guard is released afterwards
        ran.clear();
        self[0].build(player, session, editSession, BlockVector3.ZERO, null, 1);
        assertEquals(2, ran.size());

        String tooMany = String.join(";", Collections.nCopies(CommandBrush.MAX_COMMANDS + 1, "//pos1"));
        assertThrows(IllegalArgumentException.class, () -> new CommandBrush(tooMany, (_, _) -> { }));
        new CommandBrush(String.join(";", Collections.nCopies(CommandBrush.MAX_COMMANDS, "//pos1")), (_, _) -> { });
    }
}
