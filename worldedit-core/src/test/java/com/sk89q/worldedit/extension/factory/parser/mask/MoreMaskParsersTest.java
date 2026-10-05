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

package com.sk89q.worldedit.extension.factory.parser.mask;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.factory.MaskFactory;
import com.sk89q.worldedit.extension.factory.parser.ParserTestBase;
import com.sk89q.worldedit.extension.factory.parser.TestBlockExtent;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.function.mask.BoundedHeightMask;
import com.sk89q.worldedit.function.mask.CoordinateRangeMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.Mask2D;
import com.sk89q.worldedit.function.mask.MaskIntersection;
import com.sk89q.worldedit.function.mask.NoiseFilter;
import com.sk89q.worldedit.function.mask.OffsetsMask;
import com.sk89q.worldedit.function.mask.RadiusMask;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.Placement;
import com.sk89q.worldedit.session.PlacementType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class MoreMaskParsersTest extends ParserTestBase {

    private final MaskFactory factory = WorldEdit.getInstance().getMaskFactory();

    private Mask parse(String input) throws InputParseException {
        return factory.parseFromInput(input, newContext());
    }

    private Mask parse(String input, TestBlockExtent extent) throws InputParseException {
        ParserContext context = newContext();
        context.setExtent(extent);
        return factory.parseFromInput(input, context);
    }

    private List<String> suggest(String input) {
        return factory.getSuggestions(input, newContext());
    }

    private static boolean test(Mask mask, int x, int y, int z) {
        return mask.test(BlockVector3.at(x, y, z));
    }

    /**
     * A 7x7 stone floor at y=0 with a 2 blocks high pillar at the center,
     * and a single block below the floor at x=2.
     */
    private static TestBlockExtent terrain() {
        return new TestBlockExtent()
            .fill(BlockVector3.at(-3, 0, -3), BlockVector3.at(3, 0, 3), TestBlockExtent.STONE)
            .set(0, 1, 0, TestBlockExtent.STONE)
            .set(0, 2, 0, TestBlockExtent.STONE)
            .set(2, -1, 0, TestBlockExtent.STONE);
    }

    // #x, #y, #z

    @Test
    void axisRanges() throws InputParseException {
        CoordinateRangeMask x = assertInstanceOf(CoordinateRangeMask.class, parse("#x[10][0]"));
        assertEquals(CoordinateRangeMask.Axis.X, x.getAxis());
        assertTrue(test(x, 0, 500, -500));
        assertTrue(test(x, 10, 0, 0));
        assertFalse(test(x, 11, 0, 0));
        assertFalse(test(x, -1, 0, 0));

        Mask z = parse("#z[*][-5]");
        assertTrue(test(z, 0, 0, -1_000_000));
        assertFalse(test(z, 0, 0, -4));

        assertInstanceOf(BoundedHeightMask.class, parse("#y[0][5]"));
        assertInstanceOf(MaskIntersection.class, parse("#x[0][5] #z[0][5]"));
    }

    @Test
    void axisRangesTo2D() throws InputParseException {
        Mask2D x = parse("#x[0][5]").toMask2D();
        assertNotNull(x);
        assertTrue(x.test(BlockVector2.at(5, 100)));
        assertFalse(x.test(BlockVector2.at(6, 0)));
        Mask2D z = parse("#z[0][5]").toMask2D();
        assertNotNull(z);
        assertTrue(z.test(BlockVector2.at(100, 0)));
        assertFalse(z.test(BlockVector2.at(0, -1)));
        assertNull(new CoordinateRangeMask(CoordinateRangeMask.Axis.Y, 0, 1).toMask2D());
    }

    @Test
    void axisRangeErrors() {
        assertThrows(InputParseException.class, () -> parse("#x[1]"));
        assertThrows(InputParseException.class, () -> parse("#z[a][1]"));
        assertThrows(InputParseException.class, () -> parse("#x[1][2][3]"));
    }

    // #offset

    @Test
    void offset() throws InputParseException {
        OffsetsMask mask = assertInstanceOf(OffsetsMask.class, parse("#offset[1][0][-2][#x[5][5]]"));
        assertEquals(List.of(BlockVector3.at(1, 0, -2)), mask.getOffsets().asList());
        assertTrue(test(mask, 4, 0, 0));
        assertFalse(test(mask, 5, 0, 0));
    }

    @Test
    void offsetWithIntersection() throws InputParseException {
        // spaces inside brackets do not split the outer intersection
        Mask mask = parse("#offset[0][-1][0][#x[0][5] #z[0][5]]");
        assertTrue(test(mask, 2, 7, 2));
        assertFalse(test(mask, 2, 7, 6));
        Mask outer = parse("#offset[0][-1][0][#x[0][5] #z[0][5]] #y[0][3]");
        assertInstanceOf(MaskIntersection.class, outer);
        assertTrue(test(outer, 2, 3, 2));
        assertFalse(test(outer, 2, 7, 2));
    }

    @Test
    void offsetErrors() {
        assertThrows(InputParseException.class, () -> parse("#offset[0][1][0]"));
        assertThrows(InputParseException.class, () -> parse("#offset[0][a][0][#existing]"));
        assertThrows(InputParseException.class, () -> parse("#offset[0][1][0][#nope]"));
    }

    // #radius

    private static ParserContext placementContext() {
        LocalSession session = new LocalSession();
        session.setPlacement(new Placement(PlacementType.WORLD, BlockVector3.at(10, 64, -10)));
        ParserContext context = newContext();
        context.setSession(session);
        context.setActor(mock(Actor.class));
        return context;
    }

    @Test
    void radius() throws InputParseException {
        RadiusMask sphere = assertInstanceOf(RadiusMask.class, factory.parseFromInput("#radius[3]", placementContext()));
        assertEquals(BlockVector3.at(10, 64, -10), sphere.getCenter());
        assertTrue(test(sphere, 10, 64, -10));
        assertTrue(test(sphere, 13, 64, -10));
        assertTrue(test(sphere, 12, 66, -9));
        assertFalse(test(sphere, 14, 64, -10));
        assertFalse(test(sphere, 12, 66, -8));

        RadiusMask shell = assertInstanceOf(RadiusMask.class, factory.parseFromInput("#radius[5][2.5]", placementContext()));
        assertEquals(2.5, shell.getMinRadius());
        assertEquals(5, shell.getMaxRadius());
        assertFalse(test(shell, 10, 64, -10));
        assertTrue(test(shell, 10, 67, -10));
        assertFalse(test(shell, 10, 70, -10));
        // far positions do not overflow
        assertFalse(test(shell, Integer.MAX_VALUE, 64, Integer.MIN_VALUE));
    }

    @Test
    void radiusErrors() {
        // no session or actor
        assertThrows(InputParseException.class, () -> parse("#radius[5]"));
        assertThrows(InputParseException.class, () -> factory.parseFromInput("#radius[-1]", placementContext()));
        assertThrows(InputParseException.class, () -> factory.parseFromInput("#radius[big]", placementContext()));
        assertThrows(InputParseException.class, () -> factory.parseFromInput("#radius[NaN]", placementContext()));
        assertThrows(InputParseException.class, () -> factory.parseFromInput("#radius[1][2][3]", placementContext()));
    }

    // surface masks

    @Test
    void wall() throws InputParseException {
        TestBlockExtent extent = terrain();
        Mask mask = parse("#wall", extent);
        assertTrue(test(mask, 0, 1, 0));
        assertTrue(test(mask, 0, 2, 0));
        // floor blocks surrounded by stone
        assertFalse(test(mask, 0, 0, 0));
        assertFalse(test(mask, 1, 0, 1));
        // edge of the floor
        assertTrue(test(mask, 3, 0, 0));
        // air never matches
        assertFalse(test(mask, 1, 1, 0));
    }

    @Test
    void floorAndTop() throws InputParseException {
        TestBlockExtent extent = terrain();
        for (String alias : new String[] {"#floor", "#top"}) {
            Mask mask = parse(alias, extent);
            assertTrue(test(mask, 1, 0, 0), alias);
            assertTrue(test(mask, 0, 2, 0), alias);
            assertFalse(test(mask, 0, 0, 0), alias);
            assertFalse(test(mask, 0, 1, 0), alias);
            assertFalse(test(mask, 1, 1, 0), alias);
        }
    }

    @Test
    void ceilingAndRoof() throws InputParseException {
        TestBlockExtent extent = terrain();
        for (String alias : new String[] {"#ceiling", "#roof"}) {
            Mask mask = parse(alias, extent);
            assertTrue(test(mask, 1, 0, 0), alias);
            assertTrue(test(mask, 2, -1, 0), alias);
            assertFalse(test(mask, 2, 0, 0), alias);
            assertFalse(test(mask, 0, 2, 0), alias);
            assertFalse(test(mask, 1, -1, 0), alias);
        }
    }

    @Test
    void materials() throws InputParseException {
        TestBlockExtent extent = new TestBlockExtent()
            .set(0, 0, 0, TestBlockExtent.STONE)
            .set(1, 0, 0, TestBlockExtent.GLASS)
            .set(2, 0, 0, TestBlockExtent.WATER);
        Mask liquid = parse("#liquid", extent);
        Mask opaque = parse("#opaque", extent);
        Mask transparent = parse("#transparent", extent);
        assertFalse(test(liquid, 0, 0, 0));
        assertTrue(test(liquid, 2, 0, 0));
        assertTrue(test(opaque, 0, 0, 0));
        assertFalse(test(opaque, 1, 0, 0));
        assertFalse(test(transparent, 0, 0, 0));
        assertTrue(test(transparent, 1, 0, 0));
        assertTrue(test(transparent, 2, 0, 0));
        assertTrue(test(transparent, 3, 0, 0));
    }

    @Test
    void surfaceMasksCombine() throws InputParseException {
        TestBlockExtent extent = terrain();
        Mask mask = parse("#wall !#floor", extent);
        assertTrue(test(mask, 0, 1, 0));
        assertFalse(test(mask, 0, 2, 0));
        assertTrue(test(parse("!#wall", extent), 0, 0, 0));
    }

    // fixed behaviour of older parsers

    @Test
    void noisePercentages() throws InputParseException {
        assertEquals(0.125, assertInstanceOf(NoiseFilter.class, parse("%12.5")).getDensity(), 1e-9);
        assertThrows(InputParseException.class, () -> parse("%"));
        assertThrows(InputParseException.class, () -> parse("%abc"));
        assertThrows(InputParseException.class, () -> parse("%150"));
        assertThrows(InputParseException.class, () -> parse("%-1"));
    }

    // suggestions

    @Test
    void suggestsNames() {
        assertTrue(suggest("#wa").contains("#wall"));
        assertTrue(suggest("#ro").contains("#roof"));
        assertTrue(suggest("#ce").contains("#ceiling"));
        assertTrue(suggest("#fl").contains("#floor"));
        assertTrue(suggest("#li").contains("#liquid"));
        assertTrue(suggest("#tr").contains("#transparent"));
        assertTrue(suggest("#op").contains("#opaque"));
        assertTrue(suggest("#ra").contains("#radius"));
        assertTrue(suggest("#of").contains("#offset"));
        assertTrue(suggest("#x").contains("#x["));
        assertTrue(suggest("#z").contains("#z["));
    }

    @Test
    void suggestsArguments() {
        assertTrue(suggest("#x[").containsAll(List.of("#x[*", "#x[0", "#x[100")));
        assertTrue(suggest("#offset[").contains("#offset[-1"));
        assertTrue(suggest("#offset[0][1][0][#wa").contains("#offset[0][1][0][#wall"));
        assertTrue(suggest("#radius[1").contains("#radius[10"));
        // suggestions after a space continue with a new mask
        assertTrue(suggest("#wall ").contains("#wall #existing"));
        assertTrue(suggest("#offset[0][-1][0][#x[0][5] #wa").contains("#offset[0][-1][0][#x[0][5] #wall"));
    }
}
