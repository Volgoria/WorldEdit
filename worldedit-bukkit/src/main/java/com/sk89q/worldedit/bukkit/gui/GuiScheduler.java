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

package com.sk89q.worldedit.bukkit.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Runs GUI work on the thread that owns a player, on both Bukkit and Folia.
 */
public final class GuiScheduler {

    private final Plugin plugin;
    private final boolean folia;

    /**
     * Create a scheduler.
     *
     * @param plugin the owning plugin
     * @param folia whether the server uses Folia's regionised scheduler
     */
    public GuiScheduler(Plugin plugin, boolean folia) {
        this.plugin = plugin;
        this.folia = folia;
    }

    /**
     * Run a task on the player's thread on the next tick.
     *
     * @param player the player
     * @param task the task
     */
    public void run(Player player, Runnable task) {
        runLater(player, task, 1);
    }

    /**
     * Run a task on the player's thread after a delay.
     *
     * <p>The task is silently dropped if the player logs out first.</p>
     *
     * @param player the player
     * @param task the task
     * @param delayTicks the delay in ticks, at least 1
     */
    public void runLater(Player player, Runnable task, long delayTicks) {
        if (!plugin.isEnabled()) {
            return;
        }
        long delay = Math.max(1, delayTicks);
        if (folia) {
            player.getScheduler().runDelayed(plugin, _ -> task.run(), null, delay);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    task.run();
                }
            }, delay);
        }
    }
}
