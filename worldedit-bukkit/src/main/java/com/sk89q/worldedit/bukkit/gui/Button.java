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

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A clickable icon in a {@link Menu}.
 *
 * @param icon the item shown in the slot
 * @param handler what happens on click
 */
public record Button(ItemStack icon, ClickHandler handler) {

    /**
     * Handles a click on a button.
     */
    @FunctionalInterface
    public interface ClickHandler {

        /**
         * Called when the button is clicked.
         *
         * @param player the player who clicked
         * @param click the type of click
         */
        void onClick(Player player, ClickType click);
    }

    private static final ClickHandler NO_OP = (_, _) -> {
    };

    public Button {
        checkNotNull(icon, "icon");
        checkNotNull(handler, "handler");
    }

    /**
     * Create a decorative button that does nothing when clicked.
     *
     * @param icon the icon
     * @return the button
     */
    public static Button decoration(ItemStack icon) {
        return new Button(icon, NO_OP);
    }

    /**
     * Whether this button is purely decorative.
     *
     * @return true if clicking it does nothing
     */
    public boolean isDecoration() {
        return handler == NO_OP;
    }
}
