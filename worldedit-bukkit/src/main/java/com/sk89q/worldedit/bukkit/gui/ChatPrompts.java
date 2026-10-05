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
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.Duration;
import java.util.function.Consumer;

/**
 * Asks players to type a value in chat, e.g. a schematic name or a search
 * query, with a timeout and a {@code cancel} keyword.
 *
 * <p>A reply typed shortly after the prompt timed out is swallowed rather than
 * sent to public chat. Prompts are dropped when the player quits or changes
 * world.</p>
 */
public final class ChatPrompts implements Listener {

    private static final String EXPIRED_MESSAGE =
        "That prompt had already timed out; your message was not sent to chat.";

    private final ChatPromptRegistry registry;
    private final GuiScheduler scheduler;
    private final Duration timeout;

    /**
     * Create the prompt service.
     *
     * @param registry the prompt state
     * @param scheduler the scheduler used to get back onto the player's thread
     * @param timeout how long players have to answer
     */
    public ChatPrompts(ChatPromptRegistry registry, GuiScheduler scheduler, Duration timeout) {
        this.registry = registry;
        this.scheduler = scheduler;
        this.timeout = timeout;
    }

    /**
     * Close the player's inventory and ask them a question in chat.
     *
     * <p>Both callbacks run on the player's thread.</p>
     *
     * @param player the player
     * @param question the question
     * @param onInput called with the answer
     * @param onCancel called if the player types {@code cancel} or runs out of time
     */
    public void ask(Player player, String question, Consumer<String> onInput, Runnable onCancel) {
        player.closeInventory();
        ChatPromptRegistry.Prompt prompt = registry.begin(player.getUniqueId(), timeout, onInput, onCancel);
        player.sendMessage(Text.PREFIX + Text.YELLOW + question);
        player.sendMessage(Text.PREFIX + "Type it in chat, or type " + Text.RED + ChatPromptRegistry.CANCEL_WORD
            + Text.GRAY + " to go back. (" + timeout.toSeconds() + "s)");

        scheduler.runLater(player, () -> {
            ChatPromptRegistry.Prompt expired = registry.expire(player.getUniqueId(), prompt.id());
            if (expired != null) {
                player.sendMessage(Text.PREFIX + Text.RED + "No answer received, prompt cancelled.");
                expired.onCancel().run();
            }
        }, timeout.toSeconds() * 20);
    }

    /**
     * Check whether the player is currently being asked something.
     *
     * @param player the player
     * @return true if a prompt is pending
     */
    public boolean isPrompted(Player player) {
        return registry.isPending(player.getUniqueId());
    }

    @SuppressWarnings("deprecation") // The legacy chat event also exists on Spigot
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        ChatPromptRegistry.Result result = registry.handle(player.getUniqueId(), event.getMessage());
        if (result.consumesMessage()) {
            // Answers, including late ones, are never broadcast to other players
            event.setCancelled(true);
        }
        ChatPromptRegistry.Prompt prompt = result.prompt();
        switch (result.outcome()) {
            case ACCEPTED -> scheduler.run(player, () -> prompt.onInput().accept(result.input()));
            case CANCELLED -> scheduler.run(player, () -> {
                player.sendMessage(Text.PREFIX + "Cancelled.");
                prompt.onCancel().run();
            });
            case EXPIRED -> scheduler.run(player, () -> {
                player.sendMessage(Text.PREFIX + Text.RED + EXPIRED_MESSAGE);
                prompt.onCancel().run();
            });
            case LATE -> scheduler.run(player, () ->
                player.sendMessage(Text.PREFIX + Text.RED + EXPIRED_MESSAGE));
            default -> {
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        registry.discard(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (registry.discard(player.getUniqueId())) {
            player.sendMessage(Text.PREFIX + Text.RED + "You changed world, prompt cancelled.");
        }
    }

    /**
     * Forget every pending prompt.
     */
    public void clear() {
        registry.clear();
    }
}
