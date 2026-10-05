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

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.factory.MaskFactory;
import com.sk89q.worldedit.extension.factory.parser.ParserTestBase;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.function.mask.AngleMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.MaskIntersection;
import com.sk89q.worldedit.function.mask.OffsetsMask;
import com.sk89q.worldedit.math.BlockVector3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NewMaskParsersTest extends ParserTestBase {

    private final MaskFactory factory = WorldEdit.getInstance().getMaskFactory();

    private Mask parse(String input) throws InputParseException {
        return factory.parseFromInput(input, newContext());
    }

    private List<String> suggest(String input) {
        return factory.getSuggestions(input, newContext());
    }

    private static boolean test(Mask mask, int x, int y, int z) {
        return mask.test(BlockVector3.at(x, y, z));
    }

    // #y

    @Test
    void heightRange() throws InputParseException {
        Mask mask = parse("#y[10][20]");
        assertFalse(test(mask, 0, 9, 0));
        assertTrue(test(mask, 0, 10, 0));
        assertTrue(test(mask, 5, 20, -5));
        assertFalse(test(mask, 0, 21, 0));

        // bounds in any order
        Mask swapped = parse("#y[20][10]");
        assertTrue(test(swapped, 0, 15, 0));
        assertFalse(test(swapped, 0, 25, 0));
    }

    @Test
    void heightRangeUnbounded() throws InputParseException {
        Mask below = parse("#y[*][62]");
        assertTrue(test(below, 0, -2000, 0));
        assertFalse(test(below, 0, 63, 0));
        Mask above = parse("#y[62][*]");
        assertTrue(test(above, 0, 3000, 0));
        assertFalse(test(above, 0, 61, 0));
    }

    @Test
    void heightRangeErrors() {
        assertThrows(InputParseException.class, () -> parse("#y[10]"));
        assertThrows(InputParseException.class, () -> parse("#y[a][10]"));
        assertThrows(InputParseException.class, () -> parse("#y[1][2][3]"));
    }

    @Test
    void heightRangeCombinesWithOtherMasks() throws InputParseException {
        Mask mask = parse("#y[0][10] #y[5][15]");
        assertInstanceOf(MaskIntersection.class, mask);
        assertFalse(test(mask, 0, 4, 0));
        assertTrue(test(mask, 0, 7, 0));
        assertFalse(test(mask, 0, 11, 0));
    }

    // #angle

    @Test
    void angle() throws InputParseException {
        AngleMask mask = assertInstanceOf(AngleMask.class, parse("#angle[60][30]"));
        assertEquals(30, mask.getMinAngle());
        assertEquals(60, mask.getMaxAngle());
        assertEquals(1, mask.getDistance());

        AngleMask far = assertInstanceOf(AngleMask.class, parse("#angle[0][22.5][3]"));
        assertEquals(22.5, far.getMaxAngle());
        assertEquals(3, far.getDistance());
    }

    @Test
    void angleErrors() {
        assertThrows(InputParseException.class, () -> parse("#angle[30]"));
        assertThrows(InputParseException.class, () -> parse("#angle[-5][30]"));
        assertThrows(InputParseException.class, () -> parse("#angle[0][100]"));
        assertThrows(InputParseException.class, () -> parse("#angle[0][45][0]"));
        assertThrows(InputParseException.class, () -> parse("#angle[steep][45]"));
    }

    // #adjacent

    @Test
    void adjacentCounts() throws InputParseException {
        OffsetsMask mask = assertInstanceOf(OffsetsMask.class, parse("#adjacent[#y[0][0]][1][1]"));
        assertEquals(1, mask.getMinMatches());
        assertEquals(1, mask.getMaxMatches());
        // directly above the layer: only the block below matches
        assertTrue(test(mask, 0, 1, 0));
        // within the layer: four horizontal neighbours match
        assertFalse(test(mask, 0, 0, 0));
        assertFalse(test(mask, 0, 5, 0));

        Mask four = parse("#adjacent[#y[0][0]][4]");
        assertTrue(test(four, 0, 0, 0));
        assertFalse(test(four, 0, 1, 0));
    }

    @Test
    void adjacentDefaults() throws InputParseException {
        OffsetsMask mask = assertInstanceOf(OffsetsMask.class, parse("#adjacent[#y[0][0]]"));
        assertEquals(1, mask.getMinMatches());
        assertEquals(6, mask.getMaxMatches());
        assertEquals(6, mask.getOffsets().size());
    }

    @Test
    void adjacentErrors() {
        assertThrows(InputParseException.class, () -> parse("#adjacent"));
        assertThrows(InputParseException.class, () -> parse("#adjacent[#y[0][0]][7]"));
        assertThrows(InputParseException.class, () -> parse("#adjacent[#y[0][0]][1][2][3]"));
        assertThrows(InputParseException.class, () -> parse("#adjacent[#nope][1]"));
    }

    // suggestions

    @Test
    void suggestsNames() {
        assertTrue(suggest("#an").contains("#angle"));
        assertTrue(suggest("#ad").contains("#adjacent"));
        assertTrue(suggest("#y").contains("#y["));
    }

    @Test
    void suggestsArguments() {
        assertTrue(suggest("#y[").containsAll(List.of("#y[*", "#y[0", "#y[64")));
        assertTrue(suggest("#angle[4").contains("#angle[45"));
        assertTrue(suggest("#angle[0][45]").contains("#angle[0][45]["));
        assertTrue(suggest("#adjacent[#y[0][0]][").contains("#adjacent[#y[0][0]][6"));
        assertTrue(suggest("#adjacent[#an").contains("#adjacent[#angle"));
        // suggestions work for the last mask of an intersection
        assertTrue(suggest("#y[0][5] #angle[1").contains("#y[0][5] #angle[15"));
    }
}
