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

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Fluent builder for menu icons.
 */
public final class ItemBuilder {

    private final Material material;
    private int amount = 1;
    private String name;
    private final List<String> lore = new ArrayList<>();
    private boolean glow;

    private ItemBuilder(Material material) {
        // Some blocks (e.g. water, wall torches) have no item form.
        this.material = material.isItem() && !material.isAir() ? material : Material.BARRIER;
    }

    /**
     * Start building an icon of the given material.
     *
     * @param material the material
     * @return the builder
     */
    public static ItemBuilder of(Material material) {
        return new ItemBuilder(material);
    }

    /**
     * Create a nameless filler pane, used to decorate menu borders.
     *
     * @param material the pane material
     * @return the item
     */
    public static ItemStack filler(Material material) {
        return of(material).name(" ").build();
    }

    public ItemBuilder name(String name) {
        this.name = name;
        return this;
    }

    public ItemBuilder lore(String... lines) {
        lore.addAll(Arrays.asList(lines));
        return this;
    }

    public ItemBuilder lore(List<String> lines) {
        lore.addAll(lines);
        return this;
    }

    public ItemBuilder amount(int amount) {
        this.amount = Math.max(1, Math.min(material.getMaxStackSize(), amount));
        return this;
    }

    public ItemBuilder glow(boolean glow) {
        this.glow = glow;
        return this;
    }

    /**
     * Build the item stack.
     *
     * @return the item
     */
    @SuppressWarnings("deprecation") // Legacy strings keep Spigot compatibility
    public ItemStack build() {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (name != null) {
                meta.setDisplayName(Text.WHITE + name);
            }
            if (!lore.isEmpty()) {
                meta.setLore(new ArrayList<>(lore));
            }
            if (glow) {
                meta.setEnchantmentGlintOverride(true);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }
}
