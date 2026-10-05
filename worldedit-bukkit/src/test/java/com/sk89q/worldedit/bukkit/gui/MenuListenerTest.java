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

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MenuListenerTest {

    // Menu cannot be loaded without a server, so a marker holder stands in for it
    private interface FakeMenu extends InventoryHolder {
    }

    private final MenuListener listener = new MenuListener(mock(GuiScheduler.class),
        inventory -> inventory.getHolder() instanceof FakeMenu);

    private static InventoryView viewWithTop(InventoryHolder holder) {
        Inventory top = mock(Inventory.class);
        when(top.getHolder()).thenReturn(holder);
        InventoryView view = mock(InventoryView.class);
        when(view.getTopInventory()).thenReturn(top);
        return view;
    }

    @Test
    void clicksAreCancelledAgainAtHighestPriority() {
        InventoryView view = viewWithTop(mock(FakeMenu.class));
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getView()).thenReturn(view);

        // Another plugin un-cancelled the click after the LOWEST handler ran
        listener.enforceClickCancelled(event);
        verify(event).setCancelled(true);
    }

    @Test
    void dragsAreCancelledAgainAtHighestPriority() {
        InventoryView view = viewWithTop(mock(FakeMenu.class));
        InventoryDragEvent event = mock(InventoryDragEvent.class);
        when(event.getView()).thenReturn(view);

        listener.enforceDragCancelled(event);
        verify(event).setCancelled(true);
    }

    @Test
    void otherInventoriesAreLeftAlone() {
        InventoryView view = viewWithTop(mock(InventoryHolder.class));
        InventoryClickEvent click = mock(InventoryClickEvent.class);
        when(click.getView()).thenReturn(view);
        InventoryDragEvent drag = mock(InventoryDragEvent.class);
        when(drag.getView()).thenReturn(view);

        listener.enforceClickCancelled(click);
        listener.enforceDragCancelled(drag);
        verify(click, never()).setCancelled(true);
        verify(drag, never()).setCancelled(true);
    }

    @Test
    void enforcersRunLateAndSeeCancelledEvents() throws NoSuchMethodException {
        Method click = MenuListener.class.getMethod("enforceClickCancelled", InventoryClickEvent.class);
        Method drag = MenuListener.class.getMethod("enforceDragCancelled", InventoryDragEvent.class);
        for (Method method : new Method[] { click, drag }) {
            EventHandler handler = method.getAnnotation(EventHandler.class);
            assertNotNull(handler, method.getName());
            assertEquals(EventPriority.HIGHEST, handler.priority(), method.getName());
            assertFalse(handler.ignoreCancelled(), method.getName());
        }
    }
}
