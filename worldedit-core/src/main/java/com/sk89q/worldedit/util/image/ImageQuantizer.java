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

import java.util.Arrays;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Maps the pixels of an image to the entries of a {@link BlockPalette}.
 */
public final class ImageQuantizer {

    /**
     * The index returned for pixels that should be skipped.
     */
    public static final int TRANSPARENT = -1;

    /**
     * Pixels with an alpha below this value are treated as transparent.
     */
    public static final int ALPHA_THRESHOLD = 128;

    private ImageQuantizer() {
    }

    /**
     * Quantize an image to palette indices.
     *
     * <p>The result is deterministic: the same input always produces the
     * same output.</p>
     *
     * @param argb the pixels, row by row, as {@code 0xAARRGGBB}
     * @param width the width of the image
     * @param height the height of the image
     * @param palette the palette
     * @param dither whether to apply Floyd-Steinberg error diffusion
     * @return palette indices, row by row, or {@link #TRANSPARENT}
     */
    public static int[] quantize(int[] argb, int width, int height, BlockPalette palette, boolean dither) {
        checkNotNull(argb);
        checkNotNull(palette);
        checkArgument(width >= 0 && height >= 0 && argb.length == width * height,
            "Pixel array does not match the dimensions");
        int[] result = new int[argb.length];
        if (!dither) {
            for (int i = 0; i < argb.length; i++) {
                result[i] = isTransparent(argb[i]) ? TRANSPARENT : palette.nearestIndex(argb[i]);
            }
            return result;
        }

        // Error buffers for the current and the next row: 3 channels per pixel
        float[] current = new float[width * 3];
        float[] next = new float[width * 3];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int i = y * width + x;
                int pixel = argb[i];
                if (isTransparent(pixel)) {
                    result[i] = TRANSPARENT;
                    continue;
                }
                float r = clamp(((pixel >> 16) & 0xFF) + current[x * 3]);
                float g = clamp(((pixel >> 8) & 0xFF) + current[x * 3 + 1]);
                float b = clamp((pixel & 0xFF) + current[x * 3 + 2]);
                int wanted = (Math.round(r) << 16) | (Math.round(g) << 8) | Math.round(b);
                int index = palette.nearestIndex(wanted);
                result[i] = index;

                int chosen = palette.get(index).rgb();
                float er = r - ((chosen >> 16) & 0xFF);
                float eg = g - ((chosen >> 8) & 0xFF);
                float eb = b - (chosen & 0xFF);
                if (x + 1 < width) {
                    spread(current, x + 1, er, eg, eb, 7f / 16f);
                    if (y + 1 < height) {
                        spread(next, x + 1, er, eg, eb, 1f / 16f);
                    }
                }
                if (y + 1 < height) {
                    spread(next, x, er, eg, eb, 5f / 16f);
                    if (x > 0) {
                        spread(next, x - 1, er, eg, eb, 3f / 16f);
                    }
                }
            }
            float[] swap = current;
            current = next;
            next = swap;
            Arrays.fill(next, 0f);
        }
        return result;
    }

    private static void spread(float[] buffer, int x, float er, float eg, float eb, float weight) {
        buffer[x * 3] += er * weight;
        buffer[x * 3 + 1] += eg * weight;
        buffer[x * 3 + 2] += eb * weight;
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(255f, value));
    }

    /**
     * Check whether a pixel counts as transparent.
     *
     * @param argb the pixel
     * @return true if transparent
     */
    public static boolean isTransparent(int argb) {
        return (argb >>> 24) < ALPHA_THRESHOLD;
    }
}
