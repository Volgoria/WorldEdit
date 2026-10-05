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

import com.sk89q.worldedit.internal.util.LogManagerCompat;
import org.apache.logging.log4j.Logger;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

import javax.annotation.Nullable;

/**
 * The single listener driving every {@link Menu}.
 *
 * <p>Any click or drag while a menu is the top inventory is cancelled, so
 * players can neither take icons out nor put their own items in. Clicks on
 * menu slots are forwarded to the menu on the next tick, because opening or
 * closing inventories from inside a click event is unsafe.</p>
 */
public final class MenuListener implements Listener {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    private final GuiScheduler scheduler;

    public MenuListener(GuiScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Nullable
    private static Menu menuOf(Inventory inventory) {
        return inventory.getHolder() instanceof Menu menu ? menu : null;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        Menu menu = menuOf(top);
        if (menu == null) {
            return;
        }
        // Cancel everything, including shift-clicks and number keys from the
        // player's own inventory, to prevent item theft or insertion.
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        int slot = event.getRawSlot();
        ClickType click = event.getClick();
        if (slot < 0 || slot >= top.getSize() || click == ClickType.DOUBLE_CLICK) {
            return;
        }
        scheduler.run(player, () -> {
            if (player.getOpenInventory().getTopInventory() != top) {
                return;
            }
            try {
                if (menu.handleClick(player, slot, click)) {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.0f);
                }
            } catch (RuntimeException e) {
                LOGGER.error("Error while handling a WorldEdit menu click", e);
                player.sendMessage(Text.PREFIX + Text.RED + "Something went wrong, see the server log.");
                player.closeInventory();
            }
        });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrag(InventoryDragEvent event) {
        if (menuOf(event.getView().getTopInventory()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        Menu menu = menuOf(event.getInventory());
        if (menu != null && event.getPlayer() instanceof Player player) {
            menu.onClose(player);
        }
    }
}
