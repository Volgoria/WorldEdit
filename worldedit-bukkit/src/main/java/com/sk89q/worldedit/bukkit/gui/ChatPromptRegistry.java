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

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Thread-safe bookkeeping for "type something in chat" prompts.
 *
 * <p>This class holds no Bukkit state, so it can be unit tested. Chat events
 * arrive on async threads; callers are responsible for moving the returned
 * callbacks back onto the right thread.</p>
 */
public final class ChatPromptRegistry {

    /**
     * The word that cancels a pending prompt.
     */
    public static final String CANCEL_WORD = "cancel";

    /**
     * What happened to a chat message offered to the registry.
     */
    public enum Outcome {
        /** No prompt was pending; the message is ordinary chat. */
        NOT_PROMPTED,
        /** A prompt was pending but had expired; it has been discarded. */
        EXPIRED,
        /** The player cancelled the prompt. */
        CANCELLED,
        /** The message answers the prompt. */
        ACCEPTED
    }

    /**
     * The result of {@link #handle(UUID, String)}.
     *
     * @param outcome the outcome
     * @param prompt the prompt concerned, null for {@link Outcome#NOT_PROMPTED}
     * @param input the trimmed message
     */
    public record Result(Outcome outcome, @Nullable Prompt prompt, String input) {

        /**
         * Whether the chat message should be hidden from other players.
         *
         * @return true if the message was meant for a prompt
         */
        public boolean consumesMessage() {
            return outcome == Outcome.CANCELLED || outcome == Outcome.ACCEPTED;
        }
    }

    /**
     * A pending prompt.
     *
     * @param id a unique id, used to time out exactly this prompt
     * @param expiresAt the time, in clock millis, after which input is rejected
     * @param onInput called with the player's answer
     * @param onCancel called when the player cancels or the prompt times out
     */
    public record Prompt(long id, long expiresAt, Consumer<String> onInput, Runnable onCancel) {
    }

    private final Map<UUID, Prompt> pending = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong();
    private final LongSupplier clock;

    /**
     * Create a registry using the system clock.
     */
    public ChatPromptRegistry() {
        this(System::currentTimeMillis);
    }

    /**
     * Create a registry with a custom clock, for tests.
     *
     * @param clock supplies the current time in millis
     */
    public ChatPromptRegistry(LongSupplier clock) {
        this.clock = checkNotNull(clock);
    }

    /**
     * Start a prompt, replacing any prompt already pending for the player.
     *
     * @param player the player
     * @param timeout how long the player has to answer
     * @param onInput called with the answer
     * @param onCancel called on cancel or timeout
     * @return the new prompt
     */
    public Prompt begin(UUID player, Duration timeout, Consumer<String> onInput, Runnable onCancel) {
        checkNotNull(player);
        checkArgument(!timeout.isNegative() && !timeout.isZero(), "timeout must be positive");
        Prompt prompt = new Prompt(nextId.incrementAndGet(), clock.getAsLong() + timeout.toMillis(),
            checkNotNull(onInput), checkNotNull(onCancel));
        pending.put(player, prompt);
        return prompt;
    }

    /**
     * Offer a chat message from a player.
     *
     * @param player the player
     * @param message the raw message
     * @return what to do with it
     */
    public Result handle(UUID player, String message) {
        String input = message.trim();
        Prompt prompt = pending.remove(player);
        if (prompt == null) {
            return new Result(Outcome.NOT_PROMPTED, null, input);
        }
        if (clock.getAsLong() > prompt.expiresAt()) {
            return new Result(Outcome.EXPIRED, prompt, input);
        }
        if (input.toLowerCase(Locale.ROOT).equals(CANCEL_WORD)) {
            return new Result(Outcome.CANCELLED, prompt, input);
        }
        return new Result(Outcome.ACCEPTED, prompt, input);
    }

    /**
     * Expire a specific prompt if it is still the one pending for the player.
     *
     * @param player the player
     * @param promptId the prompt id
     * @return the removed prompt, or null if it was already answered or replaced
     */
    @Nullable
    public Prompt expire(UUID player, long promptId) {
        Prompt current = pending.get(player);
        if (current != null && current.id() == promptId && pending.remove(player, current)) {
            return current;
        }
        return null;
    }

    /**
     * Drop any prompt pending for the player without notifying it.
     *
     * @param player the player
     * @return true if a prompt was pending
     */
    public boolean discard(UUID player) {
        return pending.remove(player) != null;
    }

    /**
     * Check whether a prompt is pending for the player.
     *
     * @param player the player
     * @return true if pending
     */
    public boolean isPending(UUID player) {
        return pending.containsKey(player);
    }

    /**
     * Drop all pending prompts.
     */
    public void clear() {
        pending.clear();
    }
}
