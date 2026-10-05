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

import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.MaskingExtent;
import com.sk89q.worldedit.function.mask.BlockTypeMask;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.mask.Masks;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector2;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.transform.ScaleAndTranslateTransform;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.CylinderRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Countable;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;
import com.sk89q.worldedit.util.test.InMemoryWorld;
import com.sk89q.worldedit.util.test.SimpleMaterialRegistries;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pins the observable behaviour (number of affected blocks and the resulting
 * world contents) of the main {@link EditSession} operations on an in-memory
 * world.
 *
 * <p>The expected fingerprints were recorded from the implementation before
 * {@link EditSession} was split into helper classes.</p>
 */
@DisplayName("EditSession operations")
class EditSessionOperationsTest extends BaseWorldEditTest {

    private static BlockState air;
    private static BlockState stone;
    private static BlockState dirt;
    private static BlockState glass;

    @BeforeAll
    static void setUpBlocks() {
        when(MOCKED_PLATFORM.getRegistries()).thenReturn(SimpleMaterialRegistries.create());
        air = new BlockType("pintest:air").getDefaultState();
        stone = new BlockType("pintest:stone").getDefaultState();
        dirt = new BlockType("pintest:dirt").getDefaultState();
        glass = new BlockType("pintest:glass").getDefaultState();
    }

    @FunctionalInterface
    private interface Edit {
        int apply(EditSession session) throws Exception;
    }

    private static InMemoryWorld newWorld() {
        return new InMemoryWorld(air, -64, 319);
    }

    /**
     * Fill a box with stone, with a dirt layer on top, to give region operations
     * something to work on.
     */
    private static InMemoryWorld terrainWorld() {
        InMemoryWorld world = newWorld();
        for (BlockVector3 pos : new CuboidRegion(BlockVector3.at(-6, 0, -6), BlockVector3.at(6, 4, 6))) {
            world.blocks().put(pos, pos.y() == 4 ? dirt : stone);
        }
        // a few distinct markers so copies and moves are observable
        world.blocks().put(BlockVector3.at(0, 5, 0), glass);
        world.blocks().put(BlockVector3.at(2, 6, -1), glass);
        return world;
    }

    private static int run(InMemoryWorld world, Edit edit) throws Exception {
        try (EditSession session = WorldEdit.getInstance().newEditSessionBuilder().world(world.world()).build()) {
            return edit.apply(session);
        }
    }

    /**
     * An order-independent fingerprint of the world's stored blocks.
     */
    private static long fingerprint(InMemoryWorld world) {
        long result = 0;
        for (Map.Entry<BlockVector3, BlockState> entry : world.blocks().entrySet()) {
            BlockVector3 pos = entry.getKey();
            long h = pos.x() * 73856093L ^ pos.y() * 19349663L ^ pos.z() * 83492791L;
            h = h * 31 + entry.getValue().getBlockType().id().hashCode();
            h ^= h >>> 29;
            h *= 0x9E3779B97F4A7C15L;
            result += h ^ (h >>> 32);
        }
        return result;
    }

    private static long nonAirCount(InMemoryWorld world) {
        return world.blocks().values().stream().filter(b -> !b.equals(air)).count();
    }

    private static void check(InMemoryWorld world, int affected,
                              int expectedAffected, long expectedNonAir, long expectedFingerprint) {
        String actual = affected + ", " + nonAirCount(world) + ", " + fingerprint(world) + "L";
        String expected = expectedAffected + ", " + expectedNonAir + ", " + expectedFingerprint + "L";
        assertEquals(expected, actual, "affected, non-air blocks, fingerprint");
    }

    @ParameterizedTest(name = "makeSphere filled={0}")
    @ValueSource(booleans = {true, false})
    void makeSphere(boolean filled) throws Exception {
        InMemoryWorld world = newWorld();
        int affected = run(world, s -> s.makeSphere(BlockVector3.at(1, 10, 2), stone, 4.5, filled));
        if (filled) {
            check(world, affected, 744, 491, -4704433585175505801L);
        } else {
            check(world, affected, 288, 210, -7192144751463960724L);
        }
    }

    @ParameterizedTest(name = "makeEllipsoid filled={0}")
    @ValueSource(booleans = {true, false})
    void makeEllipsoid(boolean filled) throws Exception {
        InMemoryWorld world = newWorld();
        int affected = run(world, s -> s.makeSphere(BlockVector3.at(0, 0, 0), stone, 5, 2, 3.5, filled));
        if (filled) {
            check(world, affected, 400, 227, -632044549812580688L);
        } else {
            check(world, affected, 208, 138, -8622049628011865817L);
        }
    }

    @ParameterizedTest(name = "makeCylinder filled={0}")
    @ValueSource(booleans = {true, false})
    void makeCylinder(boolean filled) throws Exception {
        InMemoryWorld world = newWorld();
        int affected = run(world, s -> s.makeCylinder(BlockVector3.at(3, 5, -2), stone, 5, 2.5, 4, filled));
        if (filled) {
            check(world, affected, 288, 212, 7337415549555583961L);
        } else {
            check(world, affected, 112, 96, -1486895518356053342L);
        }
    }

    @Test
    void makeCylinderDownwardsClampedToWorld() throws Exception {
        InMemoryWorld world = newWorld();
        int affected = run(world, s -> s.makeCylinder(BlockVector3.at(0, -60, 0), stone, 3, -10, true));
        check(world, affected, 520, 370, -6742931658509259001L);
    }

    @Test
    void makeCylinderClampedAtTop() throws Exception {
        InMemoryWorld world = newWorld();
        int affected = run(world, s -> s.makeCylinder(BlockVector3.at(0, 317, 0), stone, 2, 10, false));
        check(world, affected, 48, 36, -4733685217041859301L);
    }

    @ParameterizedTest(name = "makeCone filled={0}")
    @ValueSource(booleans = {true, false})
    void makeCone(boolean filled) throws Exception {
        InMemoryWorld world = newWorld();
        int affected = run(world, s -> s.makeCone(BlockVector3.at(0, 0, 0), stone, 5, 4, -6, filled, 1));
        if (filled) {
            check(world, affected, 236, 164, -6044433847024901407L);
        } else {
            check(world, affected, 104, 81, 1905819295553091072L);
        }
    }

    @ParameterizedTest(name = "makePyramid filled={0}")
    @ValueSource(booleans = {true, false})
    void makePyramid(boolean filled) throws Exception {
        InMemoryWorld world = newWorld();
        int affected = run(world, s -> s.makePyramid(BlockVector3.at(0, 0, 0), stone, 5, filled));
        if (filled) {
            check(world, affected, 220, 165, 9212873012109986493L);
        } else {
            check(world, affected, 100, 81, 8685854269188781898L);
        }
    }

    @Test
    void setBlocks() throws Exception {
        InMemoryWorld world = newWorld();
        int affected = run(world, s -> s.setBlocks(new CuboidRegion(BlockVector3.at(-1, 0, -2), BlockVector3.at(3, 2, 2)), glass));
        check(world, affected, 75, 75, -8078180658073134652L);
    }

    @Test
    void replaceBlocksWithMask() throws Exception {
        InMemoryWorld world = terrainWorld();
        int affected = run(world, s -> s.replaceBlocks(new CuboidRegion(BlockVector3.at(-3, 0, -3), BlockVector3.at(3, 6, 3)),
            new BlockTypeMask(s, dirt.getBlockType()), glass));
        check(world, affected, 49, 847, 4098087772941103800L);
    }

    @Test
    void replaceBlocksWithFilterSet() throws Exception {
        InMemoryWorld world = terrainWorld();
        int affected = run(world, s -> s.replaceBlocks(new CuboidRegion(BlockVector3.at(-3, 0, -3), BlockVector3.at(3, 6, 3)),
            Set.of(stone.toBaseBlock()), dirt));
        check(world, affected, 196, 847, -6275710639019104002L);
    }

    @Test
    void replaceBlocksNullFilterReplacesExisting() throws Exception {
        InMemoryWorld world = terrainWorld();
        int affected = run(world, s -> s.replaceBlocks(new CuboidRegion(BlockVector3.at(-1, 3, -1), BlockVector3.at(1, 7, 1)),
            (Set<BaseBlock>) null, glass));
        check(world, affected, 19, 847, -8363332492553744451L);
    }

    @Test
    void makeCuboidFaces() throws Exception {
        InMemoryWorld world = newWorld();
        int affected = run(world, s -> s.makeCuboidFaces(new CuboidRegion(BlockVector3.at(0, 0, 0), BlockVector3.at(4, 3, 5)), (Pattern) stone));
        check(world, affected, 148, 96, 8830490029102679997L);
    }

    @Test
    void makeFacesOfCylinder() throws Exception {
        InMemoryWorld world = newWorld();
        Region region = new CylinderRegion(BlockVector3.at(0, 0, 0), Vector2.at(4, 4), 0, 5);
        int affected = run(world, s -> s.makeFaces(region, stone));
        check(world, affected, 234, 234, -4422807553377196644L);
    }

    @Test
    void makeWallsOfCuboid() throws Exception {
        InMemoryWorld world = newWorld();
        int affected = run(world, s -> s.makeWalls(new CuboidRegion(BlockVector3.at(0, 0, 0), BlockVector3.at(4, 3, 5)), (Pattern) stone));
        check(world, affected, 88, 72, 399995681675816582L);
    }

    @Test
    void makeWallsOfCylinder() throws Exception {
        InMemoryWorld world = newWorld();
        Region region = new CylinderRegion(BlockVector3.at(0, 0, 0), Vector2.at(4, 3), 0, 5);
        int affected = run(world, s -> s.makeWalls(region, stone));
        check(world, affected, 120, 120, 7276327060287317754L);
    }

    @Test
    void overlayCuboidBlocks() throws Exception {
        InMemoryWorld world = terrainWorld();
        int affected = run(world, s -> s.overlayCuboidBlocks(new CuboidRegion(BlockVector3.at(-3, 0, -3), BlockVector3.at(3, 8, 3)), (Pattern) glass));
        check(world, affected, 49, 896, 1342959541598819325L);
    }

    @Test
    void stackCuboidRegionSkippingAir() throws Exception {
        InMemoryWorld world = terrainWorld();
        int affected = run(world, s -> s.stackCuboidRegion(new CuboidRegion(BlockVector3.at(-1, 3, -1), BlockVector3.at(2, 6, 1)),
            BlockVector3.UNIT_X, 3, false));
        check(world, affected, 78, 901, -7199706550512108093L);
    }

    @Test
    void stackCuboidRegionWithAir() throws Exception {
        InMemoryWorld world = terrainWorld();
        int affected = run(world, s -> s.stackCuboidRegion(new CuboidRegion(BlockVector3.at(-1, 3, -1), BlockVector3.at(2, 6, 1)),
            BlockVector3.UNIT_MINUS_Z, 2, true));
        check(world, affected, 96, 859, 1239438259671705183L);
    }

    @Test
    void moveRegion() throws Exception {
        InMemoryWorld world = terrainWorld();
        int affected = run(world, s -> s.moveRegion(new CuboidRegion(BlockVector3.at(-2, 3, -2), BlockVector3.at(2, 6, 2)),
            BlockVector3.UNIT_Y, 3, false, glass));
        check(world, affected, 100, 922, 4828246080961036950L);
    }

    @Test
    void moveRegionWithMask() throws Exception {
        InMemoryWorld world = terrainWorld();
        int affected = run(world, s -> s.moveRegion(new CuboidRegion(BlockVector3.at(-2, 3, -2), BlockVector3.at(2, 6, 2)),
            BlockVector3.UNIT_X, 2, false, false, new ExistingBlockMask(s), dirt));
        check(world, affected, 52, 849, 6104611609609734910L);
    }

    @Test
    void center() throws Exception {
        InMemoryWorld world = newWorld();
        int affected = run(world, s -> s.center(new CuboidRegion(BlockVector3.at(0, 0, 0), BlockVector3.at(5, 4, 7)), glass));
        check(world, affected, 4, 4, 286177130145389840L);
    }

    @ParameterizedTest(name = "fillXZ recursive={0}")
    @ValueSource(booleans = {true, false})
    void fillXZ(boolean recursive) throws Exception {
        InMemoryWorld world = terrainWorld();
        // dig a pit, with an overhang so that recursive and downward fill differ
        for (BlockVector3 pos : new CuboidRegion(BlockVector3.at(-3, 1, -3), BlockVector3.at(3, 4, 3))) {
            world.blocks().put(pos, air);
        }
        for (BlockVector3 pos : new CuboidRegion(BlockVector3.at(-3, 4, -3), BlockVector3.at(3, 4, 0))) {
            world.blocks().put(pos, stone);
        }
        int affected = run(world, s -> s.fillXZ(BlockVector3.at(0, 4, 2), glass, 5, 4, recursive));
        if (recursive) {
            check(world, affected, 153, 832, 8495097858610828681L);
        } else {
            check(world, affected, 84, 763, -3129867543792451077L);
        }
    }

    @ParameterizedTest(name = "drawLine filled={0}")
    @ValueSource(booleans = {true, false})
    void drawLine(boolean filled) throws Exception {
        InMemoryWorld world = newWorld();
        int affected = run(world, s -> s.drawLine(stone, List.of(
            BlockVector3.at(0, 0, 0), BlockVector3.at(12, 5, -3), BlockVector3.at(12, 5, -3), BlockVector3.at(4, 15, 7)
        ), 2, filled));
        if (filled) {
            check(world, affected, 398, 398, 8917887837068007580L);
        } else {
            check(world, affected, 262, 262, -3388842830314021685L);
        }
    }

    @Test
    void drawSpline() throws Exception {
        InMemoryWorld world = newWorld();
        int affected = run(world, s -> s.drawSpline(stone, List.of(
            BlockVector3.at(0, 0, 0), BlockVector3.at(10, 4, 3), BlockVector3.at(5, 12, -6)
        ), 0, 0, 0, 10, 1, true));
        check(world, affected, 165, 165, 1774575983633433266L);
    }

    @Test
    void hollowOutRegion() throws Exception {
        InMemoryWorld world = terrainWorld();
        int affected = run(world, s -> s.hollowOutRegion(new CuboidRegion(BlockVector3.at(-6, 0, -6), BlockVector3.at(6, 4, 6)), 2, glass));
        check(world, affected, 845, 847, -5800557835597652168L);
    }

    @Test
    void morph() throws Exception {
        InMemoryWorld world = terrainWorld();
        int affected = run(world, s -> s.morph(BlockVector3.at(5, 4, 5), 3, 2, 2, 4, 1));
        check(world, affected, 729, 825, 7313038804788683332L);
    }

    @Test
    void deformRegion() throws Exception {
        InMemoryWorld world = terrainWorld();
        CuboidRegion region = new CuboidRegion(BlockVector3.at(-6, 0, -6), BlockVector3.at(6, 8, 6));
        var transform = new ScaleAndTranslateTransform(Vector3.ZERO, Vector3.ONE);
        int affected = run(world, s -> s.deformRegion(region, transform, "y-=1; x+=z", -1, s.getWorld(), transform));
        check(world, affected, 1521, 637, 2158519412427857697L);
    }

    @Test
    void makeShape() throws Exception {
        InMemoryWorld world = newWorld();
        CuboidRegion region = new CuboidRegion(BlockVector3.at(-5, 0, -5), BlockVector3.at(5, 10, 5));
        var transform = new ScaleAndTranslateTransform(Vector3.at(0, 5, 0), Vector3.at(5, 5, 5));
        int affected = run(world, s -> s.makeShape(region, transform, stone, "x*x+y*y+z*z<1 && x>-0.4", true, -1));
        check(world, affected, 186, 186, -631654544441953773L);
    }

    @Test
    void countAndDistribution() throws Exception {
        InMemoryWorld world = terrainWorld();
        CuboidRegion region = new CuboidRegion(BlockVector3.at(-2, 2, -2), BlockVector3.at(3, 6, 3));
        try (EditSession session = WorldEdit.getInstance().newEditSessionBuilder().world(world.world()).build()) {
            assertEquals(36, session.countBlocks(region, Set.of(dirt.toBaseBlock())));
            assertEquals(110, session.countBlocks(region, new ExistingBlockMask(session)));
            List<Countable<BlockState>> distribution = session.getBlockDistribution(region, false);
            assertEquals(4, distribution.size());
            long total = distribution.stream().mapToLong(Countable::getAmount).sum();
            assertEquals(region.getVolume(), total);
            assertEquals(6, session.getHighestTerrainBlock(2, -1, 0, 20));
            assertEquals(4, session.getHighestTerrainBlock(1, 1, 0, 20));
            assertEquals(3, session.getHighestTerrainBlock(1, 1, 0, 20, new BlockTypeMask(session, stone.getBlockType())));
            assertEquals(-5, session.getHighestTerrainBlock(30, 30, -5, 20));
        }
    }

    @Test
    void tracingReportsFailedActions() throws Exception {
        InMemoryWorld world = newWorld();
        Actor actor = mock(Actor.class);
        try (EditSession session = WorldEdit.getInstance().newEditSessionBuilder()
            .world(world.world()).actor(actor).tracing(true).build()) {
            session.setMask(Masks.negate(Masks.alwaysTrue()));
            session.setBlock(BlockVector3.at(1, 2, 3), stone);
            session.setMask(null);
            session.setBlock(BlockVector3.at(4, 5, 6), stone);
        }
        assertEquals(Map.of(BlockVector3.at(4, 5, 6), stone), world.blocks());

        ArgumentCaptor<Component> message = ArgumentCaptor.forClass(Component.class);
        verify(actor).printDebug(message.capture());
        verify(actor, never()).printError(any(Component.class));
        TranslatableComponent failure = (TranslatableComponent) message.getValue();
        assertEquals("worldedit.trace.action-failed", failure.key());
        assertEquals(List.of("[SET_BLOCK]", BlockVector3.at(1, 2, 3).toString(), MaskingExtent.class.getName()),
            failure.args().stream().map(arg -> ((TextComponent) arg).content()).toList());
    }
}
