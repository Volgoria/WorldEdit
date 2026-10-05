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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class OpenMenusTest {

    @Test
    void clearAllEmptiesEveryOpenMenu() {
        OpenMenus menus = new OpenMenus();
        Inventory first = mock(Inventory.class);
        Inventory second = mock(Inventory.class);
        menus.opened(first);
        menus.opened(second);

        assertEquals(2, menus.clearAll());
        verify(first).clear();
        verify(second).clear();
        assertEquals(0, menus.size());
    }

    @Test
    void closedMenusAreForgotten() {
        OpenMenus menus = new OpenMenus();
        Inventory closed = mock(Inventory.class);
        menus.opened(closed);
        menus.closed(closed);

        assertEquals(0, menus.clearAll());
        verify(closed, never()).clear();
    }
}
