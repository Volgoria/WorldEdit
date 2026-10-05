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

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;

import java.awt.image.BufferedImage;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Renders a top-down map of a region: one pixel per column, coloured after
 * the highest non-air block.
 *
 * <p>The image's X axis is world X and its Y axis is world Z, so north is
 * at the top. Empty columns are transparent.</p>
 */
public final class TopViewRenderer {

    /**
     * The colour of blocks that are not in the palette.
     */
    public static final int UNKNOWN_COLOR = 0x808080;

    private static final double BRIGHTER = 255.0 / 220.0;
    private static final double DARKER = 180.0 / 220.0;

    private TopViewRenderer() {
    }

    /**
     * Render a region.
     *
     * @param extent the extent to read from
     * @param region the region
     * @param palette the palette providing block colours
     * @param shade whether to shade slopes like in-game maps (lighter when higher than the column to the north)
     * @return the image, of the size of the region's bounding box
     */
    public static BufferedImage render(Extent extent, Region region, BlockPalette palette, boolean shade) {
        checkNotNull(extent);
        checkNotNull(region);
        checkNotNull(palette);
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        int w = max.x() - min.x() + 1;
        int l = max.z() - min.z() + 1;
        int[] tops = new int[w * l];
        int[] pixels = new int[w * l];
        for (int z = 0; z < l; z++) {
            for (int x = 0; x < w; x++) {
                int wx = min.x() + x;
                int wz = min.z() + z;
                int i = z * w + x;
                tops[i] = Surfaces.NO_BLOCK;
                if (!Surfaces.isInFootprint(region, wx, wz)) {
                    continue;
                }
                int top = Surfaces.topBlockY(extent, wx, wz, min.y(), max.y());
                tops[i] = top;
                if (top == Surfaces.NO_BLOCK) {
                    continue;
                }
                String id = extent.getBlock(BlockVector3.at(wx, top, wz)).getBlockType().id();
                int rgb = palette.getRenderColor(id).orElse(UNKNOWN_COLOR);
                if (shade && z > 0 && tops[i - w] != Surfaces.NO_BLOCK) {
                    if (top > tops[i - w]) {
                        rgb = scale(rgb, BRIGHTER);
                    } else if (top < tops[i - w]) {
                        rgb = scale(rgb, DARKER);
                    }
                }
                pixels[i] = 0xFF000000 | rgb;
            }
        }
        return Images.fromArgb(pixels, w, l);
    }

    private static int scale(int rgb, double factor) {
        int r = Math.min(255, (int) Math.round(((rgb >> 16) & 0xFF) * factor));
        int g = Math.min(255, (int) Math.round(((rgb >> 8) & 0xFF) * factor));
        int b = Math.min(255, (int) Math.round((rgb & 0xFF) * factor));
        return (r << 16) | (g << 8) | b;
    }
}
