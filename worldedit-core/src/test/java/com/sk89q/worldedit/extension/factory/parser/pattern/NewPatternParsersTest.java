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

package com.sk89q.worldedit.extension.factory.parser.pattern;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.factory.PatternFactory;
import com.sk89q.worldedit.extension.factory.parser.ParserTestBase;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.pattern.CheckerPattern;
import com.sk89q.worldedit.function.pattern.GradientPattern;
import com.sk89q.worldedit.function.pattern.NoisePattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.pattern.StripePattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import com.sk89q.worldedit.world.World;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class NewPatternParsersTest extends ParserTestBase {

    private final PatternFactory factory = WorldEdit.getInstance().getPatternFactory();

    private Pattern parse(String input) throws InputParseException {
        return factory.parseFromInput(input, newContext());
    }

    private String blockAt(Pattern pattern, int x, int y, int z) {
        return idOf(pattern.applyBlock(BlockVector3.at(x, y, z)));
    }

    private List<String> suggest(String input) {
        return factory.getSuggestions(input, newContext());
    }

    // #gradient

    @Test
    void gradientWithExplicitLevels() throws InputParseException {
        GradientPattern pattern = assertInstanceOf(GradientPattern.class, parse("#gradient[stone,dirt,sand][0][10]"));
        assertEquals(3, pattern.getPatterns().size());
        assertEquals(0, pattern.getFromY());
        assertEquals(10, pattern.getToY());
        assertEquals("stone", blockAt(pattern, 0, 0, 0));
        assertEquals("dirt", blockAt(pattern, 0, 5, 0));
        assertEquals("sand", blockAt(pattern, 0, 10, 0));
    }

    @Test
    void gradientUsesSelection() throws InputParseException {
        World world = mock(World.class);
        LocalSession session = new LocalSession();
        session.setRegionSelector(world, new CuboidRegionSelector(world, BlockVector3.at(0, 70, 0), BlockVector3.at(5, 40, 5)));
        ParserContext context = newContext();
        context.setSession(session);
        context.setWorld(world);
        GradientPattern pattern = assertInstanceOf(
            GradientPattern.class, factory.parseFromInput("#gradient[stone,dirt]", context)
        );
        assertEquals(40, pattern.getFromY());
        assertEquals(70, pattern.getToY());
    }

    @Test
    void gradientErrors() {
        // no selection available
        assertThrows(InputParseException.class, () -> parse("#gradient[stone,dirt]"));
        // only one level
        assertThrows(InputParseException.class, () -> parse("#gradient[stone,dirt][5]"));
        assertThrows(InputParseException.class, () -> parse("#gradient[stone,dirt][a][5]"));
        assertThrows(InputParseException.class, () -> parse("#gradient[stone,,dirt][0][5]"));
        assertThrows(InputParseException.class, () -> parse("#gradient[notablock][0][5]"));
    }

    // #stripes

    @Test
    void stripes() throws InputParseException {
        StripePattern pattern = assertInstanceOf(StripePattern.class, parse("#stripes[y][stone,dirt][2]"));
        assertEquals(BlockVector3.UNIT_Y, pattern.getDirection());
        assertEquals(2, pattern.getThickness());
        assertEquals("stone", blockAt(pattern, 0, 0, 0));
        assertEquals("stone", blockAt(pattern, 9, 1, 3));
        assertEquals("dirt", blockAt(pattern, 0, 2, 0));
        assertEquals("dirt", blockAt(pattern, 0, -1, 0));

        StripePattern diagonal = assertInstanceOf(StripePattern.class, parse("#stripes[X-Z][stone,dirt]"));
        assertEquals(BlockVector3.at(1, 0, -1), diagonal.getDirection());
        assertEquals(1, diagonal.getThickness());
    }

    @Test
    void stripesAxes() throws InputParseException {
        assertEquals(BlockVector3.at(1, 1, 1), StripePatternParser.parseAxes("xyz"));
        assertEquals(BlockVector3.at(-1, 0, 0), StripePatternParser.parseAxes("-x"));
        for (String invalid : new String[] {"", "-", "x-", "xx", "--x", "w", "x y"}) {
            assertThrows(InputParseException.class, () -> StripePatternParser.parseAxes(invalid), invalid);
        }
    }

    @Test
    void stripesErrors() {
        assertThrows(InputParseException.class, () -> parse("#stripes[y]"));
        assertThrows(InputParseException.class, () -> parse("#stripes[q][stone]"));
        assertThrows(InputParseException.class, () -> parse("#stripes[y][stone][0]"));
    }

    // #checker

    @Test
    void checker() throws InputParseException {
        CheckerPattern pattern = assertInstanceOf(CheckerPattern.class, parse("#checker[stone][dirt]"));
        assertEquals(1, pattern.getSize());
        assertEquals("stone", blockAt(pattern, 0, 0, 0));
        assertEquals("dirt", blockAt(pattern, 1, 0, 0));
        assertEquals("stone", blockAt(pattern, 1, 0, 1));

        CheckerPattern big = assertInstanceOf(CheckerPattern.class, parse("#checker[stone][dirt,sand][4]"));
        assertEquals(4, big.getSize());
    }

    @Test
    void nestedPatterns() throws InputParseException {
        Pattern pattern = parse("#checker[#stripes[y][stone,dirt]][glass]");
        assertEquals("stone", blockAt(pattern, 0, 0, 0));
        assertEquals("glass", blockAt(pattern, 0, 1, 0));
        assertEquals("dirt", blockAt(pattern, 1, 1, 0));
    }

    @Test
    void checkerErrors() {
        assertThrows(InputParseException.class, () -> parse("#checker[stone]"));
        assertThrows(InputParseException.class, () -> parse("#checker[stone][dirt][-1]"));
        assertThrows(InputParseException.class, () -> parse("#checker[stone][dirt"));
    }

    // #noise

    @Test
    void noise() throws InputParseException {
        NoisePattern pattern = assertInstanceOf(NoisePattern.class, parse("#noise[12.5][stone,dirt,sand]"));
        assertEquals(12.5, pattern.getScale());
        assertEquals(3, pattern.getPatterns().size());
    }

    @Test
    void noiseIsEqualized() {
        assertEquals(0.5, NoisePatternParser.EqualizedNoise.equalize(0.5), 1e-6);
        assertTrue(NoisePatternParser.EqualizedNoise.equalize(0.0) < 0.02);
        assertTrue(NoisePatternParser.EqualizedNoise.equalize(1.0) > 0.98);
        // roughly the 10% and 90% quantiles of the raw Perlin noise
        assertEquals(0.1, NoisePatternParser.EqualizedNoise.equalize(0.21), 0.02);
        assertEquals(0.9, NoisePatternParser.EqualizedNoise.equalize(0.79), 0.02);
    }

    @Test
    void noiseErrors() {
        assertThrows(InputParseException.class, () -> parse("#noise[0][stone,dirt]"));
        assertThrows(InputParseException.class, () -> parse("#noise[big][stone,dirt]"));
        assertThrows(InputParseException.class, () -> parse("#noise[stone,dirt]"));
    }

    // suggestions

    @Test
    void suggestsNames() {
        assertTrue(suggest("#gr").contains("#gradient"));
        assertTrue(suggest("#st").contains("#stripes"));
        assertTrue(suggest("#ch").contains("#checker"));
        assertTrue(suggest("#no").contains("#noise"));
        assertTrue(suggest("#checker").contains("#checker["));
    }

    @Test
    void suggestsArguments() {
        assertTrue(suggest("#stripes[").containsAll(List.of("#stripes[x", "#stripes[y", "#stripes[z")));
        assertTrue(suggest("#stripes[y]").contains("#stripes[y]["));
        assertTrue(suggest("#noise[1").contains("#noise[16"));
        assertTrue(suggest("#checker[stone][dirt][").contains("#checker[stone][dirt][2"));
        assertTrue(
            suggest("#checker[sto").stream().anyMatch(s -> s.startsWith("#checker[") && s.contains("stone")),
            "should suggest blocks for pattern arguments"
        );
        assertTrue(
            suggest("#gradient[stone,sa").stream().anyMatch(s -> s.startsWith("#gradient[stone,") && s.contains("sand")),
            "should suggest blocks for pattern list entries"
        );
    }
}
