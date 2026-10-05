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

package com.sk89q.worldedit.event.platform;

import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.event.AbstractCancellable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Posted before a command brush runs one of its commands as a player.
 *
 * <p>Commands typed by players go through the platform's own command
 * pipeline (for example Bukkit's command preprocessing), where other plugins
 * may block, log or rewrite them. Command brushes dispatch straight to
 * WorldEdit, so platforms listen to this event to apply the same rules:
 * cancelling it skips the command, and {@link #setCommand(String)} replaces
 * it. The event is posted on the thread using the brush.</p>
 */
public class CommandBrushDispatchEvent extends AbstractCancellable {

    private final Player player;
    private String command;

    /**
     * Create a new instance.
     *
     * @param player the player using the brush, who runs the command
     * @param command the command, starting with {@code /}
     */
    public CommandBrushDispatchEvent(Player player, String command) {
        checkNotNull(player);
        this.player = player;
        this.command = checkCommand(command);
    }

    private static String checkCommand(String command) {
        checkNotNull(command);
        checkArgument(command.startsWith("/") && command.length() > 1, "command must start with /");
        return command;
    }

    /**
     * Get the player who runs the command.
     *
     * @return the player
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Get the command to run.
     *
     * @return the command, starting with {@code /}
     */
    public String getCommand() {
        return command;
    }

    /**
     * Replace the command to run. It is still run through WorldEdit.
     *
     * @param command the command, starting with {@code /}
     */
    public void setCommand(String command) {
        this.command = checkCommand(command);
    }
}
