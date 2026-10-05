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

import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Keeps track of the menu inventories that may still be shown to a player,
 * so they can be emptied when the plugin is disabled.
 *
 * <p>Once the plugin is disabled its listeners are gone and nothing cancels
 * clicks any more; an emptied menu has nothing left to take. Inventories are
 * held weakly, so a menu that is never closed cannot leak.</p>
 */
public final class OpenMenus {

    private static final OpenMenus TRACKED = new OpenMenus();

    /**
     * Get the tracker used by {@link Menu} and {@link MenuListener}.
     *
     * @return the tracker
     */
    public static OpenMenus tracked() {
        return TRACKED;
    }

    private final Map<Inventory, Boolean> inventories = Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * Record that a menu inventory has been created and shown.
     *
     * @param inventory the inventory
     */
    public void opened(Inventory inventory) {
        inventories.put(inventory, Boolean.TRUE);
    }

    /**
     * Record that a menu inventory has been closed.
     *
     * @param inventory the inventory
     */
    public void closed(Inventory inventory) {
        inventories.remove(inventory);
    }

    /**
     * Get the number of tracked inventories.
     *
     * @return the count
     */
    public int size() {
        return inventories.size();
    }

    /**
     * Remove every icon from all tracked inventories and stop tracking them.
     *
     * @return the number of inventories emptied
     */
    public int clearAll() {
        List<Inventory> snapshot;
        synchronized (inventories) {
            snapshot = new ArrayList<>(inventories.keySet());
            inventories.clear();
        }
        for (Inventory inventory : snapshot) {
            inventory.clear();
        }
        return snapshot.size();
    }
}
