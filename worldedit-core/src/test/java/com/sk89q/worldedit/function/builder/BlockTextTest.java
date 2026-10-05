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

package com.sk89q.worldedit.function.builder;

import com.sk89q.worldedit.math.BlockVector3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Block text")
class BlockTextTest extends BuildTestBase {

    private static final BlockVector3 EAST = BlockVector3.UNIT_X;
    private static final BlockVector3 NORTH = BlockVector3.UNIT_MINUS_Z;

    private static int filledPixels(BlockFont.Glyph glyph) {
        int count = 0;
        for (int row = 0; row < glyph.height(); row++) {
            for (int column = 0; column < glyph.width(); column++) {
                if (glyph.isFilled(row, column)) {
                    count++;
                }
            }
        }
        return count;
    }

    @Test
    @DisplayName("the bundled font covers letters, digits and common punctuation")
    void bundledFontIsComplete() {
        BlockFont font = BlockFont.getDefault();
        assertEquals(7, font.getHeight());
        for (char c = 'A'; c <= 'Z'; c++) {
            assertTrue(font.hasGlyph(c), "Missing glyph " + c);
            assertTrue(font.hasGlyph(Character.toLowerCase(c)), "Missing lowercase fallback " + c);
            assertEquals(7, font.getGlyph(c).height());
        }
        for (char c = '0'; c <= '9'; c++) {
            assertTrue(font.hasGlyph(c), "Missing glyph " + c);
        }
        for (char c : " .,!?-+:;'\"/()=_#*<>[]%&@$".toCharArray()) {
            assertTrue(font.hasGlyph(c), "Missing glyph " + c);
        }
        assertFalse(font.hasGlyph('é'));
        assertSame(font.getGlyph('?'), font.getGlyph('é'));
    }

    @Test
    @DisplayName("a letter is written upright, bottom-left at the origin")
    void writesSingleLetter() throws Exception {
        TestExtent extent = new TestExtent();
        BlockText text = new BlockText(BlockFont.getDefault(), 1, 1);
        int affected = text.write(extent, BlockVector3.ZERO, stone, "I", EAST, BlockVector3.UNIT_Y, NORTH);

        Set<BlockVector3> placed = extent.positionsOf(stone);
        assertEquals(filledPixels(BlockFont.getDefault().getGlyph('I')), placed.size());
        assertEquals(placed.size(), affected);
        // Top bar of the I
        assertTrue(placed.contains(BlockVector3.at(0, 6, 0)));
        assertTrue(placed.contains(BlockVector3.at(1, 6, 0)));
        assertTrue(placed.contains(BlockVector3.at(2, 6, 0)));
        // Stem
        assertTrue(placed.contains(BlockVector3.at(1, 3, 0)));
        assertFalse(placed.contains(BlockVector3.at(0, 3, 0)));
        // Bottom bar
        assertTrue(placed.contains(BlockVector3.at(0, 0, 0)));
        for (BlockVector3 pos : placed) {
            assertEquals(0, pos.z());
            assertTrue(pos.y() >= 0 && pos.y() <= 6);
        }
    }

    @Test
    @DisplayName("letters are separated by one column")
    void lettersAreSpaced() throws Exception {
        BlockFont font = BlockFont.getDefault();
        BlockText text = new BlockText(font, 1, 1);
        assertEquals(11, text.getWidth("AB"));
        assertEquals(7, text.getWidth("II"));

        TestExtent extent = new TestExtent();
        text.write(extent, BlockVector3.ZERO, stone, "II", EAST, BlockVector3.UNIT_Y, NORTH);
        assertEquals(2 * filledPixels(font.getGlyph('I')), extent.positionsOf(stone).size());
        assertFalse(extent.positionsOf(stone).contains(BlockVector3.at(3, 6, 0)));
        assertTrue(extent.positionsOf(stone).contains(BlockVector3.at(4, 6, 0)));
    }

    @Test
    @DisplayName("scale and thickness multiply each pixel")
    void scaleAndThickness() throws Exception {
        BlockFont font = BlockFont.getDefault();
        BlockText text = new BlockText(font, 2, 3);
        assertEquals(22, text.getWidth("AB"));
        assertEquals(14, text.getTextHeight("A"));

        TestExtent extent = new TestExtent();
        text.write(extent, BlockVector3.ZERO, stone, "L", EAST, BlockVector3.UNIT_Y, NORTH);
        Set<BlockVector3> placed = extent.positionsOf(stone);
        assertEquals(filledPixels(font.getGlyph('L')) * 2 * 2 * 3, placed.size());
        for (BlockVector3 pos : placed) {
            assertTrue(pos.z() <= 0 && pos.z() >= -2, "Unexpected depth at " + pos);
        }
        // The bottom bar of the L is 10 blocks long and 2 high
        for (int x = 0; x < 10; x++) {
            assertTrue(placed.contains(BlockVector3.at(x, 0, -2)));
            assertTrue(placed.contains(BlockVector3.at(x, 1, 0)));
        }
    }

    @Test
    @DisplayName("text can be laid flat and in any horizontal direction")
    void flatText() throws Exception {
        TestExtent extent = new TestExtent();
        BlockText text = new BlockText(BlockFont.getDefault(), 1, 1);
        // Reading north, the top of the letters pointing west, extruded upwards
        text.write(extent, BlockVector3.ZERO, stone, "I", NORTH, BlockVector3.UNIT_MINUS_X, BlockVector3.UNIT_Y);
        Set<BlockVector3> placed = extent.positionsOf(stone);
        assertEquals(filledPixels(BlockFont.getDefault().getGlyph('I')), placed.size());
        for (BlockVector3 pos : placed) {
            assertEquals(0, pos.y());
        }
        assertTrue(placed.contains(BlockVector3.at(-6, 0, -2)));
    }

    @Test
    @DisplayName("multiple lines are stacked from the top")
    void multipleLines() throws Exception {
        TestExtent extent = new TestExtent();
        BlockText text = new BlockText(BlockFont.getDefault(), 1, 1);
        assertEquals(15, text.getTextHeight("I\\nI"));
        assertEquals(3, text.getTextWidth("I\\nI"));
        text.write(extent, BlockVector3.ZERO, stone, "I\\nI", EAST, BlockVector3.UNIT_Y, NORTH);
        Set<BlockVector3> placed = extent.positionsOf(stone);
        assertTrue(placed.contains(BlockVector3.at(0, 14, 0)));
        assertTrue(placed.contains(BlockVector3.at(0, 8, 0)));
        assertFalse(placed.contains(BlockVector3.at(1, 7, 0)));
        assertTrue(placed.contains(BlockVector3.at(0, 0, 0)));
    }

    @Test
    @DisplayName("malformed fonts are rejected")
    void malformedFont() {
        assertThrows(IllegalArgumentException.class,
            () -> BlockFont.parse(new StringReader("char A\n#.#\n##\n")));
        assertThrows(IllegalArgumentException.class,
            () -> BlockFont.parse(new StringReader("char A\n#x#\n")));
        assertThrows(IllegalArgumentException.class,
            () -> BlockFont.parse(new StringReader("char A\n###\nchar B\n###\n###\n")));
        assertThrows(IllegalArgumentException.class,
            () -> BlockFont.parse(new StringReader("; only a comment\n")));
    }

}
