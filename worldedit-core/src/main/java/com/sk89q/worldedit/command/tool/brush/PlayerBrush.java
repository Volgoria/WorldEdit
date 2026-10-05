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

package com.sk89q.worldedit.command.tool.brush;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;

import javax.annotation.Nullable;

/**
 * A brush that needs to know which player is using it.
 *
 * <p>When used through a {@link com.sk89q.worldedit.command.tool.BrushTool},
 * {@link #build(Player, LocalSession, EditSession, BlockVector3, Pattern, double)}
 * is called instead of {@link #build(EditSession, BlockVector3, Pattern, double)}.</p>
 */
public interface PlayerBrush extends Brush {

    /**
     * Build the object on behalf of a player.
     *
     * @param player the player using the brush
     * @param session the session of the player
     * @param editSession the {@code EditSession}
     * @param position the position
     * @param pattern the pattern
     * @param size the size of the brush
     * @throws MaxChangedBlocksException if the maximum block change limit is exceeded
     */
    void build(Player player, LocalSession session, EditSession editSession, BlockVector3 position,
               @Nullable Pattern pattern, double size) throws MaxChangedBlocksException;

}
