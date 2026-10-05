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

package com.sk89q.worldedit.extent.clipboard.io.export;

import com.sk89q.worldedit.world.block.BlockType;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Approximate display colours for block types, used by export formats that have no textures.
 *
 * <p>Colours are derived from the block id: dye colours first (wool, concrete, glass...),
 * then common materials, and finally a stable hash-based colour for anything else.</p>
 */
public final class BlockColors {

    private static final Map<String, Integer> DYE_COLORS = new LinkedHashMap<>();
    private static final Map<String, Integer> MATERIAL_COLORS = new LinkedHashMap<>();

    static {
        // Order matters: longer prefixes must come before their shorter variants
        DYE_COLORS.put("light_blue", 0x3AAFD9);
        DYE_COLORS.put("light_gray", 0x8E8E86);
        DYE_COLORS.put("white", 0xE9ECEC);
        DYE_COLORS.put("orange", 0xF07613);
        DYE_COLORS.put("magenta", 0xBD44B3);
        DYE_COLORS.put("yellow", 0xF8C527);
        DYE_COLORS.put("lime", 0x70B919);
        DYE_COLORS.put("pink", 0xED8DAC);
        DYE_COLORS.put("gray", 0x3E4447);
        DYE_COLORS.put("cyan", 0x158991);
        DYE_COLORS.put("purple", 0x792AAC);
        DYE_COLORS.put("blue", 0x35399D);
        DYE_COLORS.put("brown", 0x724728);
        DYE_COLORS.put("green", 0x546D1B);
        DYE_COLORS.put("red", 0xA12722);
        DYE_COLORS.put("black", 0x141519);

        // Matched as substrings of the block id, in order
        MATERIAL_COLORS.put("grass_block", 0x7CBD6B);
        MATERIAL_COLORS.put("water", 0x3F76E4);
        MATERIAL_COLORS.put("lava", 0xCF5B13);
        MATERIAL_COLORS.put("leaves", 0x48B518);
        MATERIAL_COLORS.put("dark_oak", 0x4F3218);
        MATERIAL_COLORS.put("spruce", 0x6B5030);
        MATERIAL_COLORS.put("birch", 0xC5B57C);
        MATERIAL_COLORS.put("jungle", 0xA0734D);
        MATERIAL_COLORS.put("acacia", 0xA95B33);
        MATERIAL_COLORS.put("mangrove", 0x763331);
        MATERIAL_COLORS.put("cherry", 0xE4B4A6);
        MATERIAL_COLORS.put("bamboo", 0xC2AF52);
        MATERIAL_COLORS.put("crimson", 0x6B344A);
        MATERIAL_COLORS.put("warped", 0x2B6963);
        MATERIAL_COLORS.put("oak", 0xA2834F);
        MATERIAL_COLORS.put("deepslate", 0x505052);
        MATERIAL_COLORS.put("blackstone", 0x2A2328);
        MATERIAL_COLORS.put("end_stone", 0xDBDE9E);
        MATERIAL_COLORS.put("sandstone", 0xD8CB9B);
        MATERIAL_COLORS.put("sand", 0xDBD3A0);
        MATERIAL_COLORS.put("gravel", 0x837F7E);
        MATERIAL_COLORS.put("cobblestone", 0x7F7F7F);
        MATERIAL_COLORS.put("stone_brick", 0x7A7A7A);
        MATERIAL_COLORS.put("nether_brick", 0x2C1519);
        MATERIAL_COLORS.put("brick", 0x966153);
        MATERIAL_COLORS.put("andesite", 0x888889);
        MATERIAL_COLORS.put("diorite", 0xBCBCBC);
        MATERIAL_COLORS.put("granite", 0x956755);
        MATERIAL_COLORS.put("stone", 0x7D7D7D);
        MATERIAL_COLORS.put("dirt", 0x866043);
        MATERIAL_COLORS.put("mud", 0x3C393D);
        MATERIAL_COLORS.put("clay", 0xA0A6B3);
        MATERIAL_COLORS.put("terracotta", 0x985E43);
        MATERIAL_COLORS.put("snow", 0xF9FEFE);
        MATERIAL_COLORS.put("ice", 0x91B7FD);
        MATERIAL_COLORS.put("glass", 0xC0F5FE);
        MATERIAL_COLORS.put("obsidian", 0x0F0A18);
        MATERIAL_COLORS.put("netherrack", 0x6F3634);
        MATERIAL_COLORS.put("quartz", 0xEBE5DE);
        MATERIAL_COLORS.put("prismarine", 0x63AB9E);
        MATERIAL_COLORS.put("purpur", 0xA97DA9);
        MATERIAL_COLORS.put("copper", 0xC06C50);
        MATERIAL_COLORS.put("iron", 0xD8D8D8);
        MATERIAL_COLORS.put("gold", 0xF6D03D);
        MATERIAL_COLORS.put("diamond", 0x62EDE4);
        MATERIAL_COLORS.put("emerald", 0x2ACB57);
        MATERIAL_COLORS.put("lapis", 0x1F4389);
        MATERIAL_COLORS.put("redstone", 0xAA0F01);
        MATERIAL_COLORS.put("coal", 0x2E2E2E);
        MATERIAL_COLORS.put("glowstone", 0xFBDA74);
        MATERIAL_COLORS.put("lantern", 0xE9B567);
        MATERIAL_COLORS.put("torch", 0xFFD84A);
        MATERIAL_COLORS.put("hay", 0xA68B0C);
        MATERIAL_COLORS.put("moss", 0x596E2D);
        MATERIAL_COLORS.put("bedrock", 0x555555);
        MATERIAL_COLORS.put("log", 0x6B5432);
        MATERIAL_COLORS.put("planks", 0xA2834F);
        MATERIAL_COLORS.put("wood", 0x6B5432);
    }

    /**
     * Get an approximate RGB colour ({@code 0xRRGGBB}) for a block type.
     *
     * @param type the block type
     * @return the colour
     */
    public static int getColor(BlockType type) {
        return getColor(type.id());
    }

    /**
     * Get an approximate RGB colour ({@code 0xRRGGBB}) for a block id.
     *
     * @param id the block id, with or without namespace
     * @return the colour
     */
    public static int getColor(String id) {
        String path = id.toLowerCase(Locale.ROOT);
        int colon = path.indexOf(':');
        if (colon >= 0) {
            path = path.substring(colon + 1);
        }
        for (Map.Entry<String, Integer> entry : DYE_COLORS.entrySet()) {
            if (path.startsWith(entry.getKey() + "_")) {
                return entry.getValue();
            }
        }
        for (Map.Entry<String, Integer> entry : MATERIAL_COLORS.entrySet()) {
            if (path.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        // Stable fallback colour, kept in a mid-range so it is neither too dark nor too bright
        int hash = id.hashCode();
        int r = 64 + ((hash >>> 16) & 0x7F);
        int g = 64 + ((hash >>> 8) & 0x7F);
        int b = 64 + (hash & 0x7F);
        return (r << 16) | (g << 8) | b;
    }

    /**
     * Get the opacity of a block type for rendering, between 0 and 1.
     *
     * @param type the block type
     * @return the opacity
     */
    public static double getOpacity(BlockType type) {
        String path = type.id().toLowerCase(Locale.ROOT);
        if (path.contains("glass") || path.endsWith(":ice") || path.contains("water")) {
            return 0.6;
        }
        return 1.0;
    }

    private BlockColors() {
    }
}
