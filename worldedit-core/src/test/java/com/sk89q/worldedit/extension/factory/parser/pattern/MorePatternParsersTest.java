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

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.factory.PatternFactory;
import com.sk89q.worldedit.extension.factory.parser.ParserTestBase;
import com.sk89q.worldedit.extension.factory.parser.TestBlockExtent;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.pattern.ConditionalPattern;
import com.sk89q.worldedit.function.pattern.ExistingBlockPattern;
import com.sk89q.worldedit.function.pattern.OffsetPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.pattern.RandomPattern;
import com.sk89q.worldedit.function.pattern.StripePattern;
import com.sk89q.worldedit.math.BlockVector3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MorePatternParsersTest extends ParserTestBase {

    private final PatternFactory factory = WorldEdit.getInstance().getPatternFactory();
    private final TestBlockExtent extent = new TestBlockExtent()
        .set(0, 0, 0, TestBlockExtent.STONE)
        .set(0, 1, 0, TestBlockExtent.GLASS)
        .set(5, 5, 5, TestBlockExtent.WATER);

    private Pattern parse(String input) throws InputParseException {
        ParserContext context = newContext();
        context.setExtent(extent);
        return factory.parseFromInput(input, context);
    }

    private String blockAt(Pattern pattern, int x, int y, int z) {
        return idOf(pattern.applyBlock(BlockVector3.at(x, y, z)));
    }

    private List<String> suggest(String input) {
        return factory.getSuggestions(input, newContext());
    }

    // #linear

    @Test
    void linear() throws InputParseException {
        StripePattern pattern = assertInstanceOf(StripePattern.class, parse("#linear[stone,dirt,sand]"));
        assertEquals(BlockVector3.ONE, pattern.getDirection());
        assertEquals("stone", blockAt(pattern, 0, 0, 0));
        assertEquals("dirt", blockAt(pattern, 1, 0, 0));
        assertEquals("sand", blockAt(pattern, 0, 1, 1));
        assertEquals("stone", blockAt(pattern, 1, 1, 1));
        assertEquals("sand", blockAt(pattern, -1, 0, 0));
        // any two face-adjacent blocks differ
        assertNotEquals(blockAt(pattern, 4, 7, 2), blockAt(pattern, 4, 8, 2));
    }

    @Test
    void linearErrors() {
        assertThrows(InputParseException.class, () -> parse("#linear"));
        assertThrows(InputParseException.class, () -> parse("#linear[]"));
        assertThrows(InputParseException.class, () -> parse("#linear[stone][dirt]"));
        assertThrows(InputParseException.class, () -> parse("#linear[stone,nope]"));
    }

    // #existing

    @Test
    void existing() throws InputParseException {
        ExistingBlockPattern pattern = assertInstanceOf(ExistingBlockPattern.class, parse("#existing"));
        assertSame(TestBlockExtent.STONE.full(), pattern.applyBlock(BlockVector3.ZERO));
        assertSame(TestBlockExtent.AIR.full(), pattern.applyBlock(BlockVector3.at(9, 9, 9)));
    }

    // #offset

    @Test
    void offset() throws InputParseException {
        OffsetPattern above = assertInstanceOf(OffsetPattern.class, parse("#offset[0][1][0][#existing]"));
        assertEquals(BlockVector3.UNIT_Y, above.getOffset());
        assertSame(TestBlockExtent.GLASS.full(), above.applyBlock(BlockVector3.ZERO));
        assertSame(TestBlockExtent.STONE.full(), above.applyBlock(BlockVector3.at(0, -1, 0)));

        Pattern shifted = parse("#offset[1][0][0][#linear[stone,dirt]]");
        assertEquals("dirt", blockAt(shifted, 0, 0, 0));
        assertEquals("stone", blockAt(shifted, 1, 0, 0));

        // constant patterns are unaffected
        assertEquals("sand", blockAt(parse("#offset[-3][2][7][sand]"), 0, 0, 0));
    }

    @Test
    void offsetErrors() {
        assertThrows(InputParseException.class, () -> parse("#offset[0][1][stone]"));
        assertThrows(InputParseException.class, () -> parse("#offset[0][x][0][stone]"));
        assertThrows(InputParseException.class, () -> parse("#offset[0][1][0][nope]"));
    }

    // #mask

    @Test
    void conditional() throws InputParseException {
        ConditionalPattern pattern = assertInstanceOf(ConditionalPattern.class, parse("#mask[#y[0][5]][stone][dirt]"));
        assertEquals("stone", blockAt(pattern, 0, 0, 0));
        assertEquals("stone", blockAt(pattern, 9, 5, 9));
        assertEquals("dirt", blockAt(pattern, 0, 6, 0));
        assertEquals("dirt", blockAt(pattern, 0, -1, 0));
    }

    @Test
    void conditionalKeepsOtherBlocks() throws InputParseException {
        Pattern pattern = parse("#mask[#y[0][0]][sand]");
        assertEquals("sand", blockAt(pattern, 3, 0, 3));
        assertSame(TestBlockExtent.GLASS.full(), pattern.applyBlock(BlockVector3.at(0, 1, 0)));
        assertSame(TestBlockExtent.WATER.full(), pattern.applyBlock(BlockVector3.at(5, 5, 5)));
    }

    @Test
    void conditionalNesting() throws InputParseException {
        // masks with spaces, nested patterns and random lists inside brackets
        Pattern pattern = parse("#mask[#x[0][0] #z[0][0]][#mask[#y[0][0]][stone][dirt]][50%sand,50%glass]");
        assertEquals("stone", blockAt(pattern, 0, 0, 0));
        assertEquals("dirt", blockAt(pattern, 0, 1, 0));
        String other = blockAt(pattern, 1, 0, 0);
        assertTrue(other.equals("sand") || other.equals("glass"), other);
        assertInstanceOf(RandomPattern.class, parse("#mask[#y[0][0]][stone],dirt"));
    }

    @Test
    void conditionalErrors() {
        assertThrows(InputParseException.class, () -> parse("#mask[#y[0][5]]"));
        assertThrows(InputParseException.class, () -> parse("#mask[#nope][stone]"));
        assertThrows(InputParseException.class, () -> parse("#mask[#y[0][5]][nope]"));
        assertThrows(InputParseException.class, () -> parse("#mask[#y[0][5]][stone][dirt][sand]"));
    }

    // suggestions

    @Test
    void suggestions() {
        assertTrue(suggest("#li").contains("#linear"));
        assertTrue(suggest("#linear").contains("#linear["));
        assertTrue(suggest("#ex").contains("#existing"));
        assertTrue(suggest("#of").contains("#offset"));
        assertTrue(suggest("#offset[").contains("#offset[0"));
        assertTrue(suggest("#offset[0][1][0][#ex").contains("#offset[0][1][0][#existing"));
        assertTrue(suggest("#ma").contains("#mask"));
        assertTrue(suggest("#mask[#y").contains("#mask[#y["));
        assertTrue(suggest("#mask[#y[0][5]][#ex").contains("#mask[#y[0][5]][#existing"));
        assertTrue(suggest("#mask[#y[0][5]][stone][#li").contains("#mask[#y[0][5]][stone][#linear"));
        assertTrue(suggest("#linear[stone,#ex").contains("#linear[stone,#existing"));
    }
}
