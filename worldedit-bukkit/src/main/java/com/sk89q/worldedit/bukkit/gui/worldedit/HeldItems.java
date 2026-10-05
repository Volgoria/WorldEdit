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

import com.sk89q.worldedit.bukkit.gui.ItemBuilder;
import com.sk89q.worldedit.bukkit.gui.Text;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Makes sure players hold an item that WorldEdit can bind a brush or tool to.
 */
final class HeldItems {

    private HeldItems() {
    }

    /**
     * WorldEdit cannot bind tools to air or to blocks. Creative players get a
     * blaze rod in a free hotbar slot; other players are never given items
     * and are asked to hold one instead.
     *
     * @param player the player
     * @return true if the held item can take a brush or tool
     */
    static boolean ensureBindable(Player player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack held = inventory.getItemInMainHand();
        if (!held.getType().isAir() && !held.getType().isBlock()) {
            return true;
        }
        if (player.getGameMode() != GameMode.CREATIVE) {
            Buttons.sendMessage(player, Text.RED + "Hold a non-block item (e.g. a stick) to bind a tool to it.");
            return false;
        }
        int target = held.getType().isAir() ? inventory.getHeldItemSlot() : -1;
        for (int i = 0; i < 9 && target < 0; i++) {
            ItemStack item = inventory.getItem(i);
            if (item == null || item.getType().isAir()) {
                target = i;
            }
        }
        if (target < 0) {
            Buttons.sendMessage(player, Text.RED + "Hold a non-block item, or free a hotbar slot for a tool.");
            return false;
        }
        inventory.setItem(target, ItemBuilder.of(Material.BLAZE_ROD)
            .name(Text.LIGHT_PURPLE + "WorldEdit Tool")
            .lore(Text.GRAY + "Right-click to use the bound brush or tool")
            .build());
        inventory.setHeldItemSlot(target);
        return true;
    }
}
