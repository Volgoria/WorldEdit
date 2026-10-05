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

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A grid of column heights, convertible to and from greyscale images and
 * the surface of a region.
 *
 * <p>Heights are relative to the bottom of a region: a column of height
 * {@code h} has blocks from {@code minY} to {@code minY + h - 1}, and
 * height 0 means an empty column. In images, black is height 0 and white is
 * {@code maxHeight}.</p>
 */
public final class Heightmap {

    private static final int MAX_16_BIT = 0xFFFF;

    private final int width;
    private final int length;
    private final int[] heights;

    /**
     * Create a new heightmap.
     *
     * @param width the size along X
     * @param length the size along Z
     * @param heights the heights, row by row ({@code z * width + x})
     */
    public Heightmap(int width, int length, int[] heights) {
        checkArgument(width > 0 && length > 0, "Size must be positive");
        checkArgument(heights.length == width * length, "Height array does not match the dimensions");
        this.width = width;
        this.length = length;
        this.heights = heights;
    }

    public int getWidth() {
        return width;
    }

    public int getLength() {
        return length;
    }

    /**
     * Get the height of a column.
     *
     * @param x the X offset, from 0
     * @param z the Z offset, from 0
     * @return the height
     */
    public int getHeight(int x, int z) {
        return heights[z * width + x];
    }

    /**
     * Get a copy of all heights, row by row.
     *
     * @return the heights
     */
    public int[] toArray() {
        return heights.clone();
    }

    /**
     * Read the brightness of every pixel of an image, from 0 to 1.
     *
     * <p>Greyscale images (8 or 16 bit) are read at full precision; other
     * images use their luma.</p>
     *
     * @param image the image
     * @return the values, row by row
     */
    public static double[] readGrey(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        double[] values = new double[w * h];
        ColorModel cm = image.getColorModel();
        Raster raster = image.getRaster();
        boolean grey = cm.getColorSpace().getType() == ColorSpace.TYPE_GRAY
            && !(cm instanceof IndexColorModel);
        if (grey) {
            double max = (1L << raster.getSampleModel().getSampleSize(0)) - 1;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    values[y * w + x] = raster.getSample(x, y, 0) / max;
                }
            }
        } else {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int rgb = image.getRGB(x, y);
                    double luma = 0.299 * ((rgb >> 16) & 0xFF) + 0.587 * ((rgb >> 8) & 0xFF) + 0.114 * (rgb & 0xFF);
                    values[y * w + x] = luma / 255.0;
                }
            }
        }
        return values;
    }

    /**
     * Create a heightmap from brightness values, rescaling them to a new size
     * with bilinear interpolation.
     *
     * <p>When the target size equals the source size, every column maps to
     * exactly one pixel.</p>
     *
     * @param values brightness values from 0 to 1, row by row
     * @param sourceWidth the width of the values grid
     * @param sourceLength the height of the values grid
     * @param width the target width
     * @param length the target length
     * @param maxHeight the height corresponding to a value of 1
     * @return the heightmap
     */
    public static Heightmap fromValues(double[] values, int sourceWidth, int sourceLength,
                                       int width, int length, int maxHeight) {
        checkArgument(values.length == sourceWidth * sourceLength, "Value array does not match the dimensions");
        checkArgument(maxHeight >= 0, "Max height must not be negative");
        int[] heights = new int[width * length];
        for (int z = 0; z < length; z++) {
            double sz = (z + 0.5) * sourceLength / length - 0.5;
            for (int x = 0; x < width; x++) {
                double sx = (x + 0.5) * sourceWidth / width - 0.5;
                double v = bilinear(values, sourceWidth, sourceLength, sx, sz);
                heights[z * width + x] = (int) Math.round(Math.max(0, Math.min(1, v)) * maxHeight);
            }
        }
        return new Heightmap(width, length, heights);
    }

    private static double bilinear(double[] values, int w, int h, double x, double y) {
        x = Math.max(0, Math.min(w - 1, x));
        y = Math.max(0, Math.min(h - 1, y));
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        int x1 = Math.min(w - 1, x0 + 1);
        int y1 = Math.min(h - 1, y0 + 1);
        double fx = x - x0;
        double fy = y - y0;
        double top = values[y0 * w + x0] * (1 - fx) + values[y0 * w + x1] * fx;
        double bottom = values[y1 * w + x0] * (1 - fx) + values[y1 * w + x1] * fx;
        return top * (1 - fy) + bottom * fy;
    }

    /**
     * Render this heightmap as a 16-bit greyscale image.
     *
     * @param maxHeight the height drawn as white
     * @return the image
     */
    public BufferedImage toImage(int maxHeight) {
        checkArgument(maxHeight > 0, "Max height must be positive");
        BufferedImage image = new BufferedImage(width, length, BufferedImage.TYPE_USHORT_GRAY);
        WritableRaster raster = image.getRaster();
        for (int z = 0; z < length; z++) {
            for (int x = 0; x < width; x++) {
                int h = Math.max(0, Math.min(maxHeight, getHeight(x, z)));
                raster.setSample(x, z, 0, (int) Math.round((double) h * MAX_16_BIT / maxHeight));
            }
        }
        return image;
    }

    /**
     * Read the surface heights of a region. Columns outside the region's
     * footprint, or without any block, have height 0.
     *
     * @param extent the extent to read from
     * @param region the region
     * @return the heightmap, covering the region's bounding box
     */
    public static Heightmap fromSurface(Extent extent, Region region) {
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        int w = max.x() - min.x() + 1;
        int l = max.z() - min.z() + 1;
        int[] heights = new int[w * l];
        for (int z = 0; z < l; z++) {
            for (int x = 0; x < w; x++) {
                int wx = min.x() + x;
                int wz = min.z() + z;
                if (!Surfaces.isInFootprint(region, wx, wz)) {
                    continue;
                }
                int top = Surfaces.topBlockY(extent, wx, wz, min.y(), max.y());
                if (top != Surfaces.NO_BLOCK) {
                    heights[z * w + x] = top - min.y() + 1;
                }
            }
        }
        return new Heightmap(w, l, heights);
    }

    /**
     * Build terrain from this heightmap over a region's footprint.
     *
     * <p>The heightmap must have the size of the region's bounding box. Each
     * column is filled from the bottom of the region upwards. Heights are
     * clamped to the region's height.</p>
     *
     * @param extent the extent to write to
     * @param region the region
     * @param top the pattern for the top block of each column
     * @param under the pattern for the {@code underDepth} blocks below the top
     * @param underDepth how many blocks use the {@code under} pattern
     * @param fill the pattern for the rest of each column
     * @param clearAbove whether to set blocks above each column, within the region, to air
     * @return the number of blocks changed
     * @throws WorldEditException if a block could not be set, e.g. when the change limit is reached
     */
    public int apply(Extent extent, Region region, Pattern top, Pattern under, int underDepth, Pattern fill,
                     boolean clearAbove) throws WorldEditException {
        checkNotNull(extent);
        checkNotNull(region);
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        checkArgument(max.x() - min.x() + 1 == width && max.z() - min.z() + 1 == length,
            "Heightmap size does not match the region");
        int regionHeight = max.y() - min.y() + 1;
        int changed = 0;
        for (int z = 0; z < length; z++) {
            for (int x = 0; x < width; x++) {
                int wx = min.x() + x;
                int wz = min.z() + z;
                if (!Surfaces.isInFootprint(region, wx, wz)) {
                    continue;
                }
                int h = Math.min(regionHeight, getHeight(x, z));
                for (int dy = 0; dy < h; dy++) {
                    int depth = h - 1 - dy;
                    Pattern pattern = depth == 0 ? top : depth <= underDepth ? under : fill;
                    changed += set(extent, BlockVector3.at(wx, min.y() + dy, wz), pattern);
                }
                if (clearAbove) {
                    for (int dy = h; dy < regionHeight; dy++) {
                        changed += set(extent, BlockVector3.at(wx, min.y() + dy, wz), null);
                    }
                }
            }
        }
        return changed;
    }

    private static int set(Extent extent, BlockVector3 pos, @Nullable Pattern pattern) throws WorldEditException {
        boolean result = pattern == null
            ? extent.setBlock(pos, BlockTypes.AIR.getDefaultState())
            : extent.setBlock(pos, pattern.applyBlock(pos));
        return result ? 1 : 0;
    }
}
