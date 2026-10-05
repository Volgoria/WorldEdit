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

import java.util.List;
import javax.annotation.Nullable;

/**
 * The player's choices that command templates are expanded with.
 *
 * @param pattern the pattern argument
 * @param blocks the blocks of the pattern when picked from the block picker,
 *     empty if the pattern was typed by hand
 * @param mask the replace mask, or null
 * @param size the size
 * @param hollow whether hollow shapes are requested
 */
public record CommandContext(String pattern, List<String> blocks, @Nullable String mask, int size, boolean hollow) {

    public CommandContext {
        blocks = List.copyOf(blocks);
    }

    /**
     * Create a context from a player's GUI state.
     *
     * @param state the state
     * @param size the size to use
     * @param hollow whether hollow shapes are requested
     * @return the context
     */
    public static CommandContext of(PlayerGuiState state, int size, boolean hollow) {
        PatternSelection selection = state.getPattern();
        return new CommandContext(selection.toPattern(), selection.getBlocks(), state.getMask(), size, hollow);
    }

    /**
     * Create a context with just a pattern.
     *
     * @param pattern the pattern
     * @param size the size
     * @param hollow whether hollow shapes are requested
     * @return the context
     */
    public static CommandContext of(String pattern, int size, boolean hollow) {
        return new CommandContext(pattern, List.of(), null, size, hollow);
    }

    /**
     * Get the pattern as several space-separated arguments, one per block,
     * as taken by {@code /brush layer}.
     *
     * @return the layers
     */
    public String layers() {
        return blocks.isEmpty() ? pattern : String.join(" ", blocks);
    }

    /**
     * Get a copy with another hollow setting.
     *
     * @param hollow whether hollow shapes are requested
     * @return the context
     */
    public CommandContext withHollow(boolean hollow) {
        return hollow == this.hollow ? this : new CommandContext(pattern, blocks, mask, size, hollow);
    }
}
