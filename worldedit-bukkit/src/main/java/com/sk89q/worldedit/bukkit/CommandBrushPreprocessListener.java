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

package com.sk89q.worldedit.bukkit;

import com.sk89q.worldedit.event.platform.CommandBrushDispatchEvent;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import org.apache.logging.log4j.Logger;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.PluginManager;

/**
 * Runs the commands of command brushes through Bukkit's
 * {@link PlayerCommandPreprocessEvent}, as if the player had typed them, so
 * that command blockers (e.g. region flags denying {@code //set}) and command
 * loggers see them too.
 *
 * <p>A cancelled Bukkit event cancels the command. A command rewritten by
 * another plugin is run instead, still through WorldEdit; a rewrite that is
 * not a command cancels it.</p>
 */
public class CommandBrushPreprocessListener {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    private final PluginManager pluginManager;

    /**
     * Create a new listener.
     *
     * @param pluginManager the plugin manager to fire Bukkit events with
     */
    public CommandBrushPreprocessListener(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    @Subscribe
    public void onCommandBrushDispatch(CommandBrushDispatchEvent event) {
        if (!(event.getPlayer() instanceof BukkitPlayer player)) {
            return;
        }
        PlayerCommandPreprocessEvent preprocess = new PlayerCommandPreprocessEvent(player.getPlayer(), event.getCommand());
        try {
            pluginManager.callEvent(preprocess);
        } catch (IllegalStateException e) {
            // Synchronous events cannot be fired from this thread: fail closed
            LOGGER.warn("Could not check a command brush command with other plugins; it was not run", e);
            event.setCancelled(true);
            return;
        }
        if (preprocess.isCancelled()) {
            event.setCancelled(true);
            return;
        }
        String message = preprocess.getMessage();
        if (!message.equals(event.getCommand())) {
            if (message.startsWith("/") && message.length() > 1) {
                event.setCommand(message);
            } else {
                event.setCancelled(true);
            }
        }
    }
}
