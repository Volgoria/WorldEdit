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

package com.sk89q.worldedit.util.image;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Block colour palette")
class BlockPaletteTest {

    @Test
    @DisplayName("Lab conversion maps white, black and grey onto the lightness axis")
    void labReferenceColours() {
        LabColor white = LabColor.fromRgb(0xFFFFFF);
        assertEquals(100, white.l(), 0.01);
        assertEquals(0, white.a(), 0.01);
        assertEquals(0, white.b(), 0.01);

        LabColor black = LabColor.fromRgb(0x000000);
        assertEquals(0, black.l(), 0.01);

        LabColor grey = LabColor.fromRgb(0x777777);
        assertEquals(50, grey.l(), 1);
        assertEquals(0, grey.a(), 0.01);
        assertEquals(0, grey.b(), 0.01);
    }

    @Test
    @DisplayName("bundled palette loads and every entry matches its own colour")
    void defaultPaletteIsSelfConsistent() {
        BlockPalette palette = BlockPalette.getDefault();
        assertTrue(palette.size() > 100, "Expected a rich palette");
        Set<String> ids = new HashSet<>();
        for (BlockPalette.Entry entry : palette.getEntries()) {
            assertTrue(ids.add(entry.blockId()), "Duplicate entry " + entry.blockId());
            assertTrue(entry.blockId().startsWith("minecraft:"));
            assertEquals(entry.rgb(), palette.nearest(entry.rgb()).rgb(), entry.blockId());
            assertEquals(entry.rgb(), palette.getRenderColor(entry.blockId()).orElseThrow());
        }
        // Render-only colours are not placeable, but can be drawn
        assertTrue(palette.getRenderColor("minecraft:grass_block").isPresent());
        assertFalse(palette.getEntry("minecraft:grass_block").isPresent());
        assertFalse(palette.getEntry("minecraft:sand").isPresent(), "Gravity blocks must not be placeable");
    }

    @Test
    @DisplayName("primary colours map to the expected blocks")
    void primaryColours() {
        BlockPalette palette = BlockPalette.getDefault();
        assertTrue(palette.nearest(0xFFFFFF).blockId().matches("minecraft:(white_wool|snow_block|quartz_block)"),
            palette.nearest(0xFFFFFF).blockId());
        assertTrue(palette.nearest(0x000000).blockId().matches("minecraft:(black_concrete|obsidian|coal_block)"),
            palette.nearest(0x000000).blockId());
        assertTrue(palette.nearest(0xB02E26).blockId().contains("red"), palette.nearest(0xB02E26).blockId());
        assertTrue(palette.nearest(0x3C44AA).blockId().contains("blue"), palette.nearest(0x3C44AA).blockId());
        assertTrue(palette.nearest(0xFED83D).blockId().matches(".*(yellow|gold).*"), palette.nearest(0xFED83D).blockId());
    }

    @Test
    @DisplayName("matching is perceptual, not plain RGB distance")
    void perceptualMatching() {
        // In plain RGB, dark blue 0x00008B is closer to dark grey 0x202020
        // than to blue 0x4040FF, but perceptually it is clearly blue.
        int target = 0x00008B;
        int grey = 0x202020;
        int blue = 0x4040FF;
        assertTrue(rgbDistanceSquared(target, grey) < rgbDistanceSquared(target, blue));

        BlockPalette palette = new BlockPalette(ImmutableMap.of(
            "test:grey", grey,
            "test:blue", blue
        ), Map.of());
        assertEquals("test:blue", palette.nearest(target).blockId());
        // Alpha is ignored
        assertEquals("test:blue", palette.nearest(0xFF000000 | target).blockId());
    }

    private static int rgbDistanceSquared(int a, int b) {
        int dr = ((a >> 16) & 0xFF) - ((b >> 16) & 0xFF);
        int dg = ((a >> 8) & 0xFF) - ((b >> 8) & 0xFF);
        int db = (a & 0xFF) - (b & 0xFF);
        return dr * dr + dg * dg + db * db;
    }

    @Test
    @DisplayName("parses JSON, ignores unknown sections and filters entries")
    void jsonAndFilter() {
        String json = "{\"__comment\": \"x\","
            + " \"placeable\": {\"a:one\": \"#FF0000\", \"a:two\": \"#00FF00\"},"
            + " \"render\": {\"a:three\": \"#0000FF\"}}";
        BlockPalette palette = BlockPalette.fromJson(new StringReader(json));
        assertEquals(2, palette.size());
        assertEquals(0xFF0000, palette.get(0).rgb());
        assertEquals(0x0000FF, palette.getRenderColor("a:three").orElseThrow());

        BlockPalette filtered = palette.filter(id -> !id.equals("a:one"));
        assertEquals(1, filtered.size());
        assertEquals("a:two", filtered.nearest(0xFF0000).blockId());
        // Filtered entries can still be rendered
        assertEquals(0xFF0000, filtered.getRenderColor("a:one").orElseThrow());

        assertThrows(IllegalArgumentException.class, () -> BlockPalette.parseColor("#FFF"));
        BlockPalette empty = palette.filter(_ -> false);
        assertThrows(IllegalArgumentException.class, () -> empty.nearestIndex(0));
    }
}
