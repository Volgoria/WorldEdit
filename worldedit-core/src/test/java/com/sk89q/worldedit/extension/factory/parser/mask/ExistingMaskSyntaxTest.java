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
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.NoMatchException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.mask.BiomeMask;
import com.sk89q.worldedit.function.mask.BlockStateMask;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.mask.ExpressionMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.NoiseFilter;
import com.sk89q.worldedit.function.mask.OffsetsMask;
import com.sk89q.worldedit.function.mask.RegionMask;
import com.sk89q.worldedit.function.mask.SolidBlockMask;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BiomeType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Pins the syntax and behaviour of the mask parsers that predate this fork.
 */
class ExistingMaskSyntaxTest extends ParserTestBase {

    private final MaskFactory factory = WorldEdit.getInstance().getMaskFactory();

    @BeforeAll
    static void registerBiomes() {
        for (String id : new String[] {"minecraft:plains", "minecraft:desert"}) {
            if (BiomeType.REGISTRY.get(id) == null) {
                BiomeType.REGISTRY.register(id, new BiomeType(id));
            }
        }
    }

    private Mask parse(String input) throws InputParseException {
        return factory.parseFromInput(input, newContext());
    }

    private List<String> suggest(String input) {
        return factory.getSuggestions(input, newContext());
    }

    private static boolean test(Mask mask, int x, int y, int z) {
        return mask.test(BlockVector3.at(x, y, z));
    }

    @Test
    void negate() throws InputParseException {
        Mask mask = parse("!#y[0][0]");
        assertFalse(test(mask, 0, 0, 0));
        assertTrue(test(mask, 0, 1, 0));
        assertThrows(InputParseException.class, () -> parse("!"));
        assertThrows(InputParseException.class, () -> parse("!#nope"));
    }

    @Test
    void offsets() throws InputParseException {
        Mask below = parse(">#y[0][0]");
        assertTrue(test(below, 0, 1, 0));
        assertFalse(test(below, 0, 0, 0));
        assertFalse(test(below, 0, -1, 0));

        Mask above = parse("<#y[0][0]");
        assertTrue(test(above, 0, -1, 0));
        assertFalse(test(above, 0, 1, 0));

        Mask adjacent = parse("~#y[0][0]");
        assertTrue(test(adjacent, 0, 1, 0));
        assertTrue(test(adjacent, 0, -1, 0));
        assertTrue(test(adjacent, 3, 0, 3));
        assertFalse(test(adjacent, 0, 2, 0));

        OffsetsMask bare = assertInstanceOf(OffsetsMask.class, parse(">"));
        assertInstanceOf(ExistingBlockMask.class, bare.getMask());
        assertEquals(BlockVector3.at(0, -1, 0), bare.getOffsets().iterator().next());
    }

    @Test
    void noise() throws InputParseException {
        assertEquals(0.5, assertInstanceOf(NoiseFilter.class, parse("%50")).getDensity());
        assertEquals(0.1, assertInstanceOf(NoiseFilter.class, parse("%10")).getDensity(), 1e-9);
        assertFalse(test(parse("%0"), 0, 0, 0) && test(parse("%0"), 1, 0, 0) && test(parse("%0"), 2, 0, 0));
        assertTrue(test(parse("%100"), 0, 0, 0));
    }

    @Test
    void expression() throws InputParseException {
        Mask mask = assertInstanceOf(ExpressionMask.class, parse("=y<5"));
        assertTrue(test(mask, 0, 4, 0));
        assertFalse(test(mask, 0, 5, 0));
        assertThrows(InputParseException.class, () -> parse("=)("));
    }

    @Test
    void biome() throws InputParseException {
        BiomeMask mask = assertInstanceOf(BiomeMask.class, parse("$plains,minecraft:desert"));
        assertEquals(2, mask.getBiomes().size());
        assertThrows(NoMatchException.class, () -> parse("$nope"));
    }

    @Test
    void blockState() throws InputParseException {
        assertInstanceOf(BlockStateMask.class, parse("^[facing=north]"));
        assertInstanceOf(BlockStateMask.class, parse("^=[facing=north,half=top]"));
    }

    @Test
    void simpleAliases() throws InputParseException {
        assertInstanceOf(ExistingBlockMask.class, parse("#existing"));
        assertInstanceOf(SolidBlockMask.class, parse("#solid"));
        assertInstanceOf(OffsetsMask.class, parse("#exposed"));
        assertInstanceOf(OffsetsMask.class, parse("#surface"));
        assertInstanceOf(RegionMask.class, parse("#dregion"));
        assertInstanceOf(RegionMask.class, parse("#dsel"));
        assertInstanceOf(Mask.class, parse("#air"));
        assertInstanceOf(Mask.class, parse("#fullcube"));
    }

    @Test
    void region() throws InputParseException {
        assertThrows(InputParseException.class, () -> parse("#region"));
        World world = mock(World.class);
        LocalSession session = new LocalSession();
        assertThrows(InputParseException.class, () -> {
            ParserContext context = newContext();
            context.setSession(session);
            context.setWorld(world);
            factory.parseFromInput("#sel", context);
        });
        session.setRegionSelector(world, new CuboidRegionSelector(world, BlockVector3.at(0, 0, 0), BlockVector3.at(5, 5, 5)));
        ParserContext context = newContext();
        context.setSession(session);
        context.setWorld(world);
        RegionMask mask = assertInstanceOf(RegionMask.class, factory.parseFromInput("#selection", context));
        assertTrue(test(mask, 5, 5, 5));
        assertFalse(test(mask, 6, 5, 5));
    }

    @Test
    void unknownInput() {
        assertThrows(NoMatchException.class, () -> parse("#doesnotexist"));
    }

    @Test
    void suggestions() {
        assertTrue(suggest("").containsAll(List.of("!", ">", "<", "~", "%", "$", "=", "^[", "^=[", "#existing")));
        assertTrue(suggest("!#ex").contains("!#existing"));
        assertTrue(suggest(">#ex").contains(">#existing"));
        assertTrue(suggest("~#so").contains("~#solid"));
        assertEquals(List.of("%10", "%25", "%50", "%75"), suggest("%"));
        assertEquals(List.of("%50"), suggest("%5"));
        assertTrue(suggest("$pla").stream().anyMatch(s -> s.startsWith("$") && s.contains("plains")));
        assertTrue(suggest("$plains,des").stream().anyMatch(s -> s.startsWith("$plains,") && s.contains("desert")));
        assertEquals(List.of(), suggest("=y"));
        assertTrue(suggest("#sel").contains("#selection"));
        assertTrue(suggest("#existing #so").contains("#existing #solid"));
    }
}
