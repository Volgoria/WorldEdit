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
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * A chest-style menu.
 *
 * <p>The menu is its own {@link InventoryHolder}, which is how
 * {@link MenuListener} recognises menu inventories. Subclasses describe their
 * contents in {@link #build(Player)}; the menu is rebuilt each time it is
 * opened or {@linkplain #refresh(Player) refreshed}.</p>
 */
public abstract class Menu implements InventoryHolder {

    protected static final ItemStack BORDER = ItemBuilder.filler(Material.GRAY_STAINED_GLASS_PANE);
    protected static final ItemStack BACKGROUND = ItemBuilder.filler(Material.BLACK_STAINED_GLASS_PANE);

    private final String title;
    private final int rows;
    private final Map<Integer, Button> buttons = new HashMap<>();
    @Nullable
    private Inventory inventory;

    /**
     * Create a menu.
     *
     * @param title the inventory title
     * @param rows the number of rows, 1 to 6
     */
    protected Menu(String title, int rows) {
        checkArgument(rows >= 1 && rows <= 6, "rows must be between 1 and 6");
        this.title = title;
        this.rows = rows;
    }

    /**
     * Get the slot index for a row and column, both zero-based.
     *
     * @param row the row
     * @param column the column, 0 to 8
     * @return the slot index
     */
    public static int slot(int row, int column) {
        return row * 9 + column;
    }

    /**
     * Populate the menu using {@link #set(int, Button)}.
     *
     * @param viewer the player the menu is built for
     */
    protected abstract void build(Player viewer);

    /**
     * Called when the player closes the menu, including when another menu
     * replaces it.
     *
     * @param player the player
     */
    public void onClose(Player player) {
    }

    /**
     * Open the menu for a player.
     *
     * @param player the player
     */
    @SuppressWarnings("deprecation") // String titles keep Spigot compatibility
    public void open(Player player) {
        inventory = Bukkit.createInventory(this, rows * 9, title);
        render(player);
        player.openInventory(inventory);
    }

    /**
     * Rebuild the menu contents in place, keeping it open.
     *
     * @param player the viewer
     */
    public void refresh(Player player) {
        if (inventory == null || player.getOpenInventory().getTopInventory() != inventory) {
            open(player);
        } else {
            render(player);
        }
    }

    private void render(Player player) {
        buttons.clear();
        build(player);
        Inventory inv = getInventory();
        inv.clear();
        buttons.forEach((slot, button) -> inv.setItem(slot, button.icon()));
    }

    /**
     * Put a button in a slot.
     *
     * @param slot the slot
     * @param button the button
     */
    protected void set(int slot, Button button) {
        checkArgument(slot >= 0 && slot < rows * 9, "slot out of range");
        buttons.put(slot, button);
    }

    /**
     * Put a button in a slot unless the slot already has one.
     *
     * @param slot the slot
     * @param button the button
     */
    protected void setIfAbsent(int slot, Button button) {
        checkArgument(slot >= 0 && slot < rows * 9, "slot out of range");
        buttons.putIfAbsent(slot, button);
    }

    /**
     * Put a button in a slot.
     *
     * @param slot the slot
     * @param icon the icon
     * @param handler the click handler
     */
    protected void set(int slot, ItemStack icon, Button.ClickHandler handler) {
        set(slot, new Button(icon, handler));
    }

    /**
     * Fill the outer ring of the menu with a decoration.
     *
     * @param icon the decoration
     */
    protected void fillBorder(ItemStack icon) {
        for (int i = 0; i < rows * 9; i++) {
            int row = i / 9;
            int column = i % 9;
            if (row == 0 || row == rows - 1 || column == 0 || column == 8) {
                setIfAbsent(i, Button.decoration(icon));
            }
        }
    }

    /**
     * Fill all slots without a button with a decoration.
     *
     * @param icon the decoration
     */
    protected void fillEmpty(ItemStack icon) {
        for (int i = 0; i < rows * 9; i++) {
            setIfAbsent(i, Button.decoration(icon));
        }
    }

    /**
     * Handle a click on a slot of this menu.
     *
     * @param player the player
     * @param slot the slot
     * @param click the click type
     * @return true if an interactive button was clicked
     */
    public boolean handleClick(Player player, int slot, ClickType click) {
        Button button = buttons.get(slot);
        if (button == null || button.isDecoration()) {
            return false;
        }
        button.handler().onClick(player, click);
        return true;
    }

    public int getRows() {
        return rows;
    }

    @Override
    public Inventory getInventory() {
        if (inventory == null) {
            throw new IllegalStateException("Menu has not been opened");
        }
        return inventory;
    }
}
