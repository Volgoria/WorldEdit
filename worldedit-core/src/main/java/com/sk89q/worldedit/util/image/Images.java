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

import com.google.common.collect.ImmutableList;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Helpers for reading, writing and resampling images.
 */
public final class Images {

    /**
     * File extensions accepted when reading images.
     */
    public static final ImmutableList<String> READ_EXTENSIONS = ImmutableList.of("png", "jpg", "jpeg", "gif", "bmp");

    /**
     * The largest number of pixels an image may have to be read: 2048 x 2048
     * (4 megapixels, 16 MB once decoded). Images are scaled down to at most
     * 1024 blocks a side anyway, so larger sources only cost memory and time.
     */
    public static final long MAX_PIXELS = 2048L * 2048L;

    private Images() {
    }

    /**
     * Read an image, refusing images larger than {@code maxPixels} before
     * decoding them.
     *
     * @param file the file
     * @param maxPixels the maximum number of pixels
     * @return the image
     * @throws IOException if the file cannot be read or is not a supported image
     */
    public static BufferedImage read(File file, long maxPixels) throws IOException {
        try (ImageInputStream in = ImageIO.createImageInputStream(file)) {
            if (in == null) {
                throw new IOException("Cannot open " + file.getName());
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                throw new IOException("Unsupported image format: " + file.getName());
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(in, true, true);
                long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                if (pixels > maxPixels) {
                    throw new IOException("Image is too large (" + reader.getWidth(0) + "x" + reader.getHeight(0) + ")");
                }
                return reader.read(0);
            } finally {
                reader.dispose();
            }
        }
    }

    /**
     * Write an image as PNG, creating parent directories as needed.
     *
     * @param image the image
     * @param file the destination
     * @throws IOException on I/O error
     */
    public static void writePng(BufferedImage image, File file) throws IOException {
        Path parent = file.toPath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        if (!ImageIO.write(image, "png", file)) {
            throw new IOException("No PNG writer available");
        }
    }

    /**
     * Get the pixels of an image as {@code 0xAARRGGBB}, row by row.
     *
     * @param image the image
     * @return the pixels
     */
    public static int[] toArgb(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        return image.getRGB(0, 0, w, h, null, 0, w);
    }

    /**
     * Create an ARGB image from pixels.
     *
     * @param argb the pixels, row by row
     * @param width the width
     * @param height the height
     * @return the image
     */
    public static BufferedImage fromArgb(int[] argb, int width, int height) {
        checkArgument(argb.length == width * height, "Pixel array does not match the dimensions");
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, width, height, argb, 0, width);
        return image;
    }

    /**
     * Resample ARGB pixels to a new size.
     *
     * <p>Each target pixel is the alpha-weighted average of the source
     * pixels it covers (area averaging), which gives good results when
     * shrinking. When growing, this degrades gracefully to nearest-neighbour
     * sampling, which keeps pixel art crisp.</p>
     *
     * @param argb the source pixels
     * @param width the source width
     * @param height the source height
     * @param newWidth the target width
     * @param newHeight the target height
     * @return the resampled pixels
     */
    public static int[] resize(int[] argb, int width, int height, int newWidth, int newHeight) {
        checkArgument(argb.length == width * height, "Pixel array does not match the dimensions");
        checkArgument(newWidth > 0 && newHeight > 0, "Target size must be positive");
        if (newWidth == width && newHeight == height) {
            return argb.clone();
        }
        int[] result = new int[newWidth * newHeight];
        for (int ty = 0; ty < newHeight; ty++) {
            int y0 = (int) ((long) ty * height / newHeight);
            int y1 = Math.max(y0 + 1, (int) ((long) (ty + 1) * height / newHeight));
            for (int tx = 0; tx < newWidth; tx++) {
                int x0 = (int) ((long) tx * width / newWidth);
                int x1 = Math.max(x0 + 1, (int) ((long) (tx + 1) * width / newWidth));
                long a = 0;
                long r = 0;
                long g = 0;
                long b = 0;
                int count = 0;
                for (int y = y0; y < y1; y++) {
                    for (int x = x0; x < x1; x++) {
                        int p = argb[y * width + x];
                        int pa = p >>> 24;
                        a += pa;
                        r += (long) ((p >> 16) & 0xFF) * pa;
                        g += (long) ((p >> 8) & 0xFF) * pa;
                        b += (long) (p & 0xFF) * pa;
                        count++;
                    }
                }
                int pixel = 0;
                if (a > 0) {
                    int outA = (int) Math.round((double) a / count);
                    int outR = (int) Math.round((double) r / a);
                    int outG = (int) Math.round((double) g / a);
                    int outB = (int) Math.round((double) b / a);
                    pixel = (outA << 24) | (outR << 16) | (outG << 8) | outB;
                }
                result[ty * newWidth + tx] = pixel;
            }
        }
        return result;
    }
}
