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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A bitmap font used to write text with blocks.
 *
 * <p>The font format is a plain text file. Each glyph starts with a line
 * {@code char <c>} (or {@code char space}), followed by one row of pixels per
 * line, from top to bottom, where {@code #} is a filled pixel and {@code .}
 * an empty one. Lines starting with {@code ;} are comments. Letters are
 * case-insensitive: a lowercase character falls back to its uppercase glyph.</p>
 */
public final class BlockFont {

    private static final String DEFAULT_FONT = "font5x7.txt";
    @Nullable
    private static volatile BlockFont defaultFont;

    /**
     * A single character of a font.
     */
    public static final class Glyph {

        private final boolean[][] pixels;

        /**
         * Create a new glyph.
         *
         * @param pixels the pixels, indexed by row (top first) then column
         */
        public Glyph(boolean[][] pixels) {
            this.pixels = new boolean[pixels.length][];
            for (int row = 0; row < pixels.length; row++) {
                this.pixels[row] = pixels[row].clone();
            }
        }

        /**
         * Get the width of the glyph, in pixels.
         *
         * @return the width
         */
        public int width() {
            return pixels.length == 0 ? 0 : pixels[0].length;
        }

        /**
         * Get the height of the glyph, in pixels.
         *
         * @return the height
         */
        public int height() {
            return pixels.length;
        }

        /**
         * Test whether a pixel is filled.
         *
         * @param row the row, 0 being the top row
         * @param column the column, 0 being the leftmost column
         * @return true if filled
         */
        public boolean isFilled(int row, int column) {
            return pixels[row][column];
        }
    }

    private final Map<Character, Glyph> glyphs;
    private final int height;

    private BlockFont(Map<Character, Glyph> glyphs, int height) {
        this.glyphs = Collections.unmodifiableMap(glyphs);
        this.height = height;
    }

    /**
     * Get the font bundled with WorldEdit.
     *
     * @return the default font
     */
    public static BlockFont getDefault() {
        BlockFont font = defaultFont;
        if (font == null) {
            synchronized (BlockFont.class) {
                font = defaultFont;
                if (font == null) {
                    InputStream stream = BlockFont.class.getResourceAsStream(DEFAULT_FONT);
                    checkNotNull(stream, "Missing bundled font " + DEFAULT_FONT);
                    try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                        font = parse(reader);
                    } catch (IOException e) {
                        throw new UncheckedIOException("Failed to read the bundled font", e);
                    }
                    defaultFont = font;
                }
            }
        }
        return font;
    }

    /**
     * Parse a font.
     *
     * @param reader the reader to read the font from
     * @return the font
     * @throws IOException on read error
     * @throws IllegalArgumentException if the font is malformed
     */
    public static BlockFont parse(Reader reader) throws IOException {
        BufferedReader in = new BufferedReader(reader);
        Map<Character, Glyph> glyphs = new HashMap<>();
        int height = -1;
        Character current = null;
        List<String> rows = new ArrayList<>();
        String line;
        while ((line = in.readLine()) != null) {
            line = line.strip();
            if (line.isEmpty() || line.startsWith(";")) {
                continue;
            }
            if (line.startsWith("char ")) {
                height = finishGlyph(glyphs, current, rows, height);
                String name = line.substring(5);
                if (name.equals("space")) {
                    current = ' ';
                } else {
                    checkArgument(name.length() == 1, "Invalid glyph name: %s", name);
                    current = name.charAt(0);
                }
                rows.clear();
            } else {
                checkArgument(current != null, "Pixel row before any glyph: %s", line);
                rows.add(line);
            }
        }
        height = finishGlyph(glyphs, current, rows, height);
        checkArgument(!glyphs.isEmpty(), "The font has no glyphs");
        return new BlockFont(glyphs, height);
    }

    private static int finishGlyph(Map<Character, Glyph> glyphs, @Nullable Character current, List<String> rows, int height) {
        if (current == null) {
            return height;
        }
        checkArgument(!rows.isEmpty(), "Glyph '%s' has no rows", current);
        checkArgument(height == -1 || rows.size() == height,
            "Glyph '%s' has %s rows, expected %s", current, rows.size(), height);
        int width = rows.get(0).length();
        boolean[][] pixels = new boolean[rows.size()][width];
        for (int row = 0; row < rows.size(); row++) {
            String text = rows.get(row);
            checkArgument(text.length() == width, "Glyph '%s' has rows of different widths", current);
            for (int column = 0; column < width; column++) {
                char c = text.charAt(column);
                checkArgument(c == '#' || c == '.', "Invalid pixel '%s' in glyph '%s'", c, current);
                pixels[row][column] = c == '#';
            }
        }
        glyphs.put(current, new Glyph(pixels));
        return rows.size();
    }

    /**
     * Get the height of every glyph of this font.
     *
     * @return the height, in pixels
     */
    public int getHeight() {
        return height;
    }

    /**
     * Test whether this font has a glyph for the given character, either
     * directly or through its uppercase form.
     *
     * @param c the character
     * @return true if the character can be written without a fallback
     */
    public boolean hasGlyph(char c) {
        return lookup(c) != null;
    }

    /**
     * Get the glyph of a character. Unknown characters are rendered with the
     * {@code ?} glyph if there is one, or with an empty glyph otherwise.
     *
     * @param c the character
     * @return the glyph
     */
    public Glyph getGlyph(char c) {
        Glyph glyph = lookup(c);
        if (glyph == null) {
            glyph = glyphs.get('?');
        }
        if (glyph == null) {
            glyph = new Glyph(new boolean[height][1]);
        }
        return glyph;
    }

    @Nullable
    private Glyph lookup(char c) {
        Glyph glyph = glyphs.get(c);
        if (glyph == null) {
            String upper = String.valueOf(c).toUpperCase(Locale.ROOT);
            if (upper.length() == 1) {
                glyph = glyphs.get(upper.charAt(0));
            }
        }
        return glyph;
    }

}
