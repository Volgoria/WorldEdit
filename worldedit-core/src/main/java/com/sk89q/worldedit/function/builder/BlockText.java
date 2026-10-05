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

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Writes text with blocks, using a {@link BlockFont}.
 *
 * <p>The text is laid out on a plane described by two unit vectors:
 * {@code right}, the reading direction, and {@code up}, the direction from the
 * bottom to the top of the letters. Letters are extruded along a third
 * {@code depth} vector. The origin is the bottom-left corner of the first
 * letter of the last line.</p>
 */
public final class BlockText {

    /**
     * The separator used to split text into multiple lines.
     */
    public static final String LINE_SEPARATOR = "\\n";

    private final BlockFont font;
    private final int scale;
    private final int thickness;

    /**
     * Create a new text writer.
     *
     * @param font the font
     * @param scale the size of a font pixel, in blocks
     * @param thickness the thickness of the letters, in blocks
     */
    public BlockText(BlockFont font, int scale, int thickness) {
        checkNotNull(font);
        checkArgument(scale >= 1, "scale must be at least 1");
        checkArgument(thickness >= 1, "thickness must be at least 1");
        this.font = font;
        this.scale = scale;
        this.thickness = thickness;
    }

    /**
     * Split text into its lines.
     *
     * @param text the text
     * @return the lines
     */
    public static List<String> lines(String text) {
        return List.of(text.split(java.util.regex.Pattern.quote(LINE_SEPARATOR), -1));
    }

    /**
     * Get the width of a line of text, in blocks.
     *
     * @param line the line
     * @return the width
     */
    public int getWidth(String line) {
        int width = 0;
        for (int i = 0; i < line.length(); i++) {
            if (i > 0) {
                width += 1;
            }
            width += font.getGlyph(line.charAt(i)).width();
        }
        return width * scale;
    }

    /**
     * Get the width of the widest line of the text, in blocks.
     *
     * @param text the text, possibly with several lines
     * @return the width
     */
    public int getTextWidth(String text) {
        int width = 0;
        for (String line : lines(text)) {
            width = Math.max(width, getWidth(line));
        }
        return width;
    }

    /**
     * Get the height of the text, in blocks.
     *
     * @param text the text, possibly with several lines
     * @return the height
     */
    public int getTextHeight(String text) {
        int lineCount = lines(text).size();
        return (lineCount * font.getHeight() + (lineCount - 1)) * scale;
    }

    /**
     * Write text.
     *
     * @param extent the extent to write to
     * @param origin the bottom-left corner of the text
     * @param pattern the pattern of the letters
     * @param text the text, with lines separated by {@link #LINE_SEPARATOR}
     * @param right the reading direction
     * @param up the direction towards the top of the letters
     * @param depth the direction in which letters are extruded
     * @return the number of blocks changed
     * @throws WorldEditException on error, such as hitting the change limit
     */
    public int write(Extent extent, BlockVector3 origin, Pattern pattern, String text,
                     BlockVector3 right, BlockVector3 up, BlockVector3 depth) throws WorldEditException {
        List<String> lines = lines(text);
        int lineHeight = (font.getHeight() + 1) * scale;
        int affected = 0;
        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            String line = lines.get(lineIndex);
            int baseV = (lines.size() - 1 - lineIndex) * lineHeight;
            int cursor = 0;
            for (int i = 0; i < line.length(); i++) {
                BlockFont.Glyph glyph = font.getGlyph(line.charAt(i));
                affected += writeGlyph(extent, origin, pattern, glyph, cursor, baseV, right, up, depth);
                cursor += (glyph.width() + 1) * scale;
            }
        }
        return affected;
    }

    private int writeGlyph(Extent extent, BlockVector3 origin, Pattern pattern, BlockFont.Glyph glyph,
                           int baseU, int baseV, BlockVector3 right, BlockVector3 up, BlockVector3 depth)
        throws WorldEditException {
        int affected = 0;
        for (int row = 0; row < glyph.height(); row++) {
            for (int column = 0; column < glyph.width(); column++) {
                if (!glyph.isFilled(row, column)) {
                    continue;
                }
                int pixelU = baseU + column * scale;
                int pixelV = baseV + (glyph.height() - 1 - row) * scale;
                for (int su = 0; su < scale; su++) {
                    for (int sv = 0; sv < scale; sv++) {
                        for (int t = 0; t < thickness; t++) {
                            BlockVector3 pos = origin
                                .add(right.multiply(pixelU + su))
                                .add(up.multiply(pixelV + sv))
                                .add(depth.multiply(t));
                            if (extent.setBlock(pos, pattern.applyBlock(pos))) {
                                affected++;
                            }
                        }
                    }
                }
            }
        }
        return affected;
    }

}
