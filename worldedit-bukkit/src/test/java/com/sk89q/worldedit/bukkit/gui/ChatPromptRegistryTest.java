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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatPromptRegistryTest {

    private final AtomicLong now = new AtomicLong(1_000);
    private final UUID player = UUID.randomUUID();
    private final List<String> answers = new ArrayList<>();
    private final AtomicInteger cancels = new AtomicInteger();
    private ChatPromptRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ChatPromptRegistry(now::get);
    }

    private ChatPromptRegistry.Prompt begin() {
        return registry.begin(player, Duration.ofSeconds(60), answers::add, cancels::incrementAndGet);
    }

    @Test
    void chatWithoutPromptIsUntouched() {
        ChatPromptRegistry.Result result = registry.handle(player, "hello");
        assertEquals(ChatPromptRegistry.Outcome.NOT_PROMPTED, result.outcome());
        assertNull(result.prompt());
        assertFalse(result.consumesMessage());
    }

    @Test
    void answerIsAcceptedOnceAndTrimmed() {
        ChatPromptRegistry.Prompt prompt = begin();
        assertTrue(registry.isPending(player));

        ChatPromptRegistry.Result result = registry.handle(player, "  my_house  ");
        assertEquals(ChatPromptRegistry.Outcome.ACCEPTED, result.outcome());
        assertSame(prompt, result.prompt());
        assertEquals("my_house", result.input());
        assertTrue(result.consumesMessage());
        result.prompt().onInput().accept(result.input());
        assertEquals(List.of("my_house"), answers);

        assertFalse(registry.isPending(player));
        assertEquals(ChatPromptRegistry.Outcome.NOT_PROMPTED, registry.handle(player, "again").outcome());
    }

    @Test
    void cancelWordCancelsIgnoringCase() {
        begin();
        ChatPromptRegistry.Result result = registry.handle(player, " CaNcEl ");
        assertEquals(ChatPromptRegistry.Outcome.CANCELLED, result.outcome());
        assertTrue(result.consumesMessage());
        assertFalse(registry.isPending(player));
    }

    @Test
    void lateAnswerIsExpiredAndNotConsumed() {
        begin();
        now.addAndGet(60_001);
        ChatPromptRegistry.Result result = registry.handle(player, "too late");
        assertEquals(ChatPromptRegistry.Outcome.EXPIRED, result.outcome());
        assertFalse(result.consumesMessage());
        assertFalse(registry.isPending(player));
    }

    @Test
    void answerExactlyAtDeadlineIsAccepted() {
        begin();
        now.addAndGet(60_000);
        assertEquals(ChatPromptRegistry.Outcome.ACCEPTED, registry.handle(player, "ok").outcome());
    }

    @Test
    void timeoutOnlyExpiresTheMatchingPrompt() {
        ChatPromptRegistry.Prompt first = begin();
        ChatPromptRegistry.Prompt second = begin();
        // The timer of the replaced prompt must not cancel the new one
        assertNull(registry.expire(player, first.id()));
        assertTrue(registry.isPending(player));

        assertNotNull(registry.expire(player, second.id()));
        assertFalse(registry.isPending(player));
        // Expiring twice is harmless
        assertNull(registry.expire(player, second.id()));
    }

    @Test
    void timeoutAfterAnswerDoesNothing() {
        ChatPromptRegistry.Prompt prompt = begin();
        registry.handle(player, "answer");
        assertNull(registry.expire(player, prompt.id()));
    }

    @Test
    void promptsArePerPlayer() {
        begin();
        UUID other = UUID.randomUUID();
        assertEquals(ChatPromptRegistry.Outcome.NOT_PROMPTED, registry.handle(other, "hi").outcome());
        assertTrue(registry.isPending(player));
    }

    @Test
    void discardAndClearDropPrompts() {
        begin();
        assertTrue(registry.discard(player));
        assertFalse(registry.discard(player));
        begin();
        registry.clear();
        assertFalse(registry.isPending(player));
        assertEquals(0, cancels.get());
    }

    @Test
    void rejectsNonPositiveTimeout() {
        assertThrows(IllegalArgumentException.class,
            () -> registry.begin(player, Duration.ZERO, answers::add, cancels::incrementAndGet));
    }
}
