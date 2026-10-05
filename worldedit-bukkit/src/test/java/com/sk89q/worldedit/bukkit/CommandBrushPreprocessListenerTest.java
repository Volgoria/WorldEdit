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
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommandBrushPreprocessListenerTest {

    private PluginManager pluginManager;
    private BukkitPlayer player;
    private CommandBrushPreprocessListener listener;

    @BeforeEach
    void setUp() {
        pluginManager = mock(PluginManager.class);
        Server server = mock(Server.class);
        doReturn(List.of()).when(server).getOnlinePlayers();
        Player bukkitPlayer = mock(Player.class);
        when(bukkitPlayer.getServer()).thenReturn(server);
        player = mock(BukkitPlayer.class);
        when(player.getPlayer()).thenReturn(bukkitPlayer);
        listener = new CommandBrushPreprocessListener(pluginManager);
    }

    private void onPreprocess(Consumer<PlayerCommandPreprocessEvent> handler) {
        doAnswer(invocation -> {
            handler.accept(invocation.getArgument(0));
            return null;
        }).when(pluginManager).callEvent(any(Event.class));
    }

    @Test
    void allowedCommandsAreUntouched() {
        CommandBrushDispatchEvent event = new CommandBrushDispatchEvent(player, "//set stone");
        listener.onCommandBrushDispatch(event);
        verify(pluginManager).callEvent(any(PlayerCommandPreprocessEvent.class));
        assertFalse(event.isCancelled());
        assertEquals("//set stone", event.getCommand());
    }

    @Test
    void cancelledPreprocessCancelsTheCommand() {
        onPreprocess(preprocess -> {
            assertEquals("//set stone", preprocess.getMessage());
            preprocess.setCancelled(true);
        });
        CommandBrushDispatchEvent event = new CommandBrushDispatchEvent(player, "//set stone");
        listener.onCommandBrushDispatch(event);
        assertTrue(event.isCancelled());
    }

    @Test
    void rewrittenCommandsAreRunInstead() {
        onPreprocess(preprocess -> preprocess.setMessage("//set dirt"));
        CommandBrushDispatchEvent event = new CommandBrushDispatchEvent(player, "//set stone");
        listener.onCommandBrushDispatch(event);
        assertFalse(event.isCancelled());
        assertEquals("//set dirt", event.getCommand());
    }

    @Test
    void preprocessFromTheWrongThreadFailsClosed() {
        doThrow(new IllegalStateException("async")).when(pluginManager).callEvent(any(Event.class));
        CommandBrushDispatchEvent event = new CommandBrushDispatchEvent(player, "//set stone");
        listener.onCommandBrushDispatch(event);
        assertTrue(event.isCancelled());
    }

    @Test
    void otherPlayersAreIgnored() {
        CommandBrushDispatchEvent event = new CommandBrushDispatchEvent(
            mock(com.sk89q.worldedit.entity.Player.class), "//set stone");
        listener.onCommandBrushDispatch(event);
        verify(pluginManager, never()).callEvent(any(Event.class));
        assertFalse(event.isCancelled());
    }
}
