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

package com.sk89q.worldedit.bukkit.gui.worldedit;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * The pattern a player has picked in the GUI: either a mix of blocks picked
 * from the block picker, or a free-form pattern typed in chat.
 */
public final class PatternSelection {

    public static final String DEFAULT_BLOCK = "stone";

    /**
     * The maximum number of blocks in a mix.
     */
    public static final int MAX_MIX = 9;

    private final List<String> blocks = new ArrayList<>();
    @Nullable
    private String custom;

    public PatternSelection() {
        blocks.add(DEFAULT_BLOCK);
    }

    /**
     * Replace the selection with a single block.
     *
     * @param block the block id
     */
    public void set(String block) {
        checkArgument(!block.isBlank(), "block");
        custom = null;
        blocks.clear();
        blocks.add(normalize(block));
    }

    /**
     * Add a block to the random mix.
     *
     * @param block the block id
     * @return false if the block was already present or the mix is full
     */
    public boolean add(String block) {
        checkArgument(!block.isBlank(), "block");
        String id = normalize(block);
        if (custom != null) {
            custom = null;
            blocks.clear();
        }
        if (blocks.contains(id) || blocks.size() >= MAX_MIX) {
            return false;
        }
        blocks.add(id);
        return true;
    }

    /**
     * Use a free-form pattern, e.g. {@code 50%stone,50%andesite} or {@code ##wool}.
     *
     * @param pattern the pattern
     */
    public void setCustom(String pattern) {
        checkArgument(GuiCommands.isSingleArgument(pattern), "pattern must be a single argument");
        custom = pattern;
    }

    /**
     * Reset to the default block.
     */
    public void reset() {
        set(DEFAULT_BLOCK);
    }

    public boolean isCustom() {
        return custom != null;
    }

    /**
     * Get the blocks in the mix. Empty when a custom pattern is used.
     *
     * @return the blocks
     */
    public List<String> getBlocks() {
        return custom != null ? List.of() : List.copyOf(blocks);
    }

    /**
     * Get the pattern as a WorldEdit pattern argument.
     *
     * @return the pattern
     */
    public String toPattern() {
        if (custom != null) {
            return custom;
        }
        return String.join(",", blocks);
    }

    private static String normalize(String block) {
        String id = block.trim().toLowerCase(Locale.ROOT);
        return id.startsWith("minecraft:") ? id.substring("minecraft:".length()) : id;
    }
}
