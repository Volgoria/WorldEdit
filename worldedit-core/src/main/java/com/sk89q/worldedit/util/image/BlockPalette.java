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
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A palette of block colours, used to turn images into blocks and blocks
 * into images.
 *
 * <p>A palette has two parts: the <em>placeable</em> entries, which are
 * candidates when converting pixels to blocks, and a wider table of
 * <em>render</em> colours (a superset of the placeable ones) used when
 * drawing blocks as pixels.</p>
 */
public final class BlockPalette {

    /**
     * A placeable palette entry.
     *
     * @param blockId the block id, e.g. {@code minecraft:white_wool}
     * @param rgb the average colour, as {@code 0xRRGGBB}
     * @param lab the average colour in Lab space
     */
    public record Entry(String blockId, int rgb, LabColor lab) {
    }

    private static final String DEFAULT_RESOURCE = "palette.json";
    private static volatile BlockPalette defaultPalette;

    private final List<Entry> entries;
    private final Map<String, Integer> renderColors;
    private final Map<Integer, Integer> nearestCache = new ConcurrentHashMap<>();

    /**
     * Create a new palette.
     *
     * @param placeable block id to colour ({@code 0xRRGGBB}) of placeable blocks, in priority order
     * @param renderOnly additional colours only used for rendering
     */
    public BlockPalette(Map<String, Integer> placeable, Map<String, Integer> renderOnly) {
        checkNotNull(placeable);
        checkNotNull(renderOnly);
        ImmutableList.Builder<Entry> list = ImmutableList.builder();
        Map<String, Integer> render = new LinkedHashMap<>(renderOnly);
        for (Map.Entry<String, Integer> e : placeable.entrySet()) {
            int rgb = e.getValue() & 0xFFFFFF;
            list.add(new Entry(e.getKey(), rgb, LabColor.fromRgb(rgb)));
            render.put(e.getKey(), rgb);
        }
        this.entries = list.build();
        this.renderColors = ImmutableMap.copyOf(render);
    }

    /**
     * Get the built-in palette.
     *
     * @return the default palette
     */
    public static BlockPalette getDefault() {
        BlockPalette palette = defaultPalette;
        if (palette == null) {
            synchronized (BlockPalette.class) {
                palette = defaultPalette;
                if (palette == null) {
                    try (InputStream in = BlockPalette.class.getResourceAsStream(DEFAULT_RESOURCE)) {
                        if (in == null) {
                            throw new IllegalStateException("Missing bundled resource " + DEFAULT_RESOURCE);
                        }
                        palette = fromJson(new InputStreamReader(in, StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                    defaultPalette = palette;
                }
            }
        }
        return palette;
    }

    /**
     * Read a palette from JSON of the form
     * {@code {"placeable": {"id": "#RRGGBB", ...}, "render": {...}}}.
     *
     * @param reader the reader
     * @return the palette
     */
    public static BlockPalette fromJson(Reader reader) {
        JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
        return new BlockPalette(readColors(root, "placeable"), readColors(root, "render"));
    }

    private static Map<String, Integer> readColors(JsonObject root, String key) {
        Map<String, Integer> result = new LinkedHashMap<>();
        JsonElement section = root.get(key);
        if (section == null || !section.isJsonObject()) {
            return result;
        }
        for (Map.Entry<String, JsonElement> e : section.getAsJsonObject().entrySet()) {
            result.put(e.getKey(), parseColor(e.getValue().getAsString()));
        }
        return result;
    }

    /**
     * Parse a colour of the form {@code #RRGGBB}.
     *
     * @param text the text
     * @return the colour as {@code 0xRRGGBB}
     */
    public static int parseColor(String text) {
        String hex = text.startsWith("#") ? text.substring(1) : text;
        checkArgument(hex.length() == 6, "Expected a colour of the form #RRGGBB, got %s", text);
        return Integer.parseInt(hex, 16);
    }

    /**
     * Create a palette containing only the placeable entries accepted by
     * the given filter. Render colours are kept.
     *
     * @param filter the filter on block ids
     * @return a new palette
     */
    public BlockPalette filter(Predicate<String> filter) {
        Map<String, Integer> placeable = new LinkedHashMap<>();
        Map<String, Integer> render = new LinkedHashMap<>(renderColors);
        for (Entry entry : entries) {
            if (filter.test(entry.blockId())) {
                placeable.put(entry.blockId(), entry.rgb());
            }
        }
        render.keySet().removeAll(placeable.keySet());
        return new BlockPalette(placeable, render);
    }

    /**
     * Get the placeable entries.
     *
     * @return the entries
     */
    public List<Entry> getEntries() {
        return entries;
    }

    /**
     * Get the number of placeable entries.
     *
     * @return the size
     */
    public int size() {
        return entries.size();
    }

    /**
     * Get a placeable entry by index.
     *
     * @param index the index
     * @return the entry
     */
    public Entry get(int index) {
        return entries.get(index);
    }

    /**
     * Find the placeable entry perceptually closest to the given colour.
     *
     * @param rgb the colour, {@code 0xRRGGBB} (alpha ignored)
     * @return the index of the closest entry
     */
    public int nearestIndex(int rgb) {
        checkArgument(!entries.isEmpty(), "Palette has no placeable entries");
        return nearestCache.computeIfAbsent(rgb & 0xFFFFFF, this::computeNearest);
    }

    private int computeNearest(int rgb) {
        LabColor lab = LabColor.fromRgb(rgb);
        int best = 0;
        double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < entries.size(); i++) {
            double d = entries.get(i).lab().distanceSquared(lab);
            if (d < bestDistance) {
                bestDistance = d;
                best = i;
            }
        }
        return best;
    }

    /**
     * Find the placeable entry perceptually closest to the given colour.
     *
     * @param rgb the colour, {@code 0xRRGGBB} (alpha ignored)
     * @return the closest entry
     */
    public Entry nearest(int rgb) {
        return entries.get(nearestIndex(rgb));
    }

    /**
     * Get the colour used to render a block.
     *
     * @param blockId the block id
     * @return the colour, as {@code 0xRRGGBB}, if known
     */
    public OptionalInt getRenderColor(String blockId) {
        Integer rgb = renderColors.get(blockId);
        return rgb == null ? OptionalInt.empty() : OptionalInt.of(rgb);
    }

    /**
     * Get the placeable entry for a block id.
     *
     * @param blockId the block id
     * @return the entry, if placeable
     */
    public Optional<Entry> getEntry(String blockId) {
        return entries.stream().filter(e -> e.blockId().equals(blockId)).findFirst();
    }
}
