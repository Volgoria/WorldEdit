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

import java.util.Arrays;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Image quantization and resampling")
class ImageQuantizerTest {

    private static final BlockPalette BLACK_AND_WHITE = new BlockPalette(ImmutableMap.of(
        "test:black", 0x000000,
        "test:white", 0xFFFFFF
    ), Map.of());

    private static int[] solid(int argb, int count) {
        int[] pixels = new int[count];
        Arrays.fill(pixels, argb);
        return pixels;
    }

    @Test
    @DisplayName("transparent pixels are skipped")
    void transparentPixels() {
        int[] pixels = { 0x00FFFFFF, 0x7F000000, 0x80000000, 0xFFFFFFFF };
        for (boolean dither : new boolean[] { false, true }) {
            int[] result = ImageQuantizer.quantize(pixels, 2, 2, BLACK_AND_WHITE, dither);
            assertArrayEquals(new int[] { ImageQuantizer.TRANSPARENT, ImageQuantizer.TRANSPARENT, 0, 1 }, result);
        }
    }

    @Test
    @DisplayName("without dithering, a flat colour maps to a single block")
    void noDither() {
        int[] result = ImageQuantizer.quantize(solid(0xFF909090, 64), 8, 8, BLACK_AND_WHITE, false);
        for (int index : result) {
            assertEquals(1, index);
        }
    }

    @Test
    @DisplayName("dithering is deterministic and preserves average brightness")
    void ditherDeterministic() {
        int width = 32;
        int height = 32;
        int[] pixels = solid(0xFF808080, width * height);
        int[] first = ImageQuantizer.quantize(pixels, width, height, BLACK_AND_WHITE, true);
        int[] second = ImageQuantizer.quantize(pixels.clone(), width, height, BLACK_AND_WHITE, true);
        assertArrayEquals(first, second);

        int white = 0;
        for (int index : first) {
            white += index;
        }
        double ratio = (double) white / first.length;
        // Mid-grey 0x80 should come out as roughly half white
        assertEquals(128 / 255.0, ratio, 0.05);

        // Random images too
        Random random = new Random(42);
        int[] noise = new int[width * height];
        for (int i = 0; i < noise.length; i++) {
            noise[i] = 0xFF000000 | random.nextInt(0x1000000);
        }
        BlockPalette palette = BlockPalette.getDefault();
        assertArrayEquals(
            ImageQuantizer.quantize(noise, width, height, palette, true),
            ImageQuantizer.quantize(noise, width, height, palette, true));
    }

    @Test
    @DisplayName("rejects mismatched dimensions")
    void badDimensions() {
        assertThrows(IllegalArgumentException.class,
            () -> ImageQuantizer.quantize(new int[3], 2, 2, BLACK_AND_WHITE, false));
    }

    @Test
    @DisplayName("resizing averages colours and ignores transparent pixels")
    void resize() {
        int[] pixels = { 0xFFFF0000, 0xFF0000FF, 0x00FFFFFF, 0xFFFF0000 };
        int[] one = Images.resize(pixels, 2, 2, 1, 1);
        int pixel = one[0];
        // 3 of 4 pixels are opaque
        assertEquals(191, pixel >>> 24);
        // Transparent white does not lighten the result
        assertEquals(170, (pixel >> 16) & 0xFF);
        assertEquals(0, (pixel >> 8) & 0xFF);
        assertEquals(85, pixel & 0xFF);

        // Upscaling is nearest-neighbour
        int[] big = Images.resize(new int[] { 0xFF000000, 0xFFFFFFFF }, 2, 1, 4, 2);
        assertArrayEquals(new int[] {
            0xFF000000, 0xFF000000, 0xFFFFFFFF, 0xFFFFFFFF,
            0xFF000000, 0xFF000000, 0xFFFFFFFF, 0xFFFFFFFF,
        }, big);

        int[] same = Images.resize(pixels, 2, 2, 2, 2);
        assertArrayEquals(pixels, same);
        assertTrue(same != pixels, "Should return a copy");
    }
}
