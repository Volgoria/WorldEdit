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

package com.sk89q.worldedit;

import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.world.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Pins the undo/redo history behaviour of {@link LocalSession}.
 */
class LocalSessionHistoryTest extends BaseWorldEditTest {

    private int originalMaxHistory;
    private LocalSession session;
    private World world;
    private Player player;

    @BeforeEach
    void setUp() {
        originalMaxHistory = LocalSession.MAX_HISTORY_SIZE;
        session = new LocalSession(WorldEdit.getInstance().getConfiguration());
        world = mock(World.class);
        player = mock(Player.class, Answers.RETURNS_SMART_NULLS);
    }

    @AfterEach
    void restoreMaxHistory() {
        LocalSession.MAX_HISTORY_SIZE = originalMaxHistory;
    }

    private EditSession edit(int size) {
        EditSession editSession = mock(EditSession.class);
        doReturn(size).when(editSession).size();
        doReturn(world).when(editSession).getWorld();
        return editSession;
    }

    @Test
    void emptyEditsAreNotRemembered() {
        session.remember(edit(0));
        assertTrue(session.getHistory().isEmpty());
        assertEquals(0, session.getHistoryPointer());
    }

    @Test
    void historyIsTrimmedToMaxSize() {
        LocalSession.MAX_HISTORY_SIZE = 3;
        List<EditSession> edits = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            EditSession editSession = edit(1);
            edits.add(editSession);
            session.remember(editSession);
        }
        assertEquals(edits.subList(2, 5), session.getHistory());
        assertEquals(3, session.getHistoryPointer());
    }

    @Test
    void undoAndRedoWalkTheHistory() {
        EditSession first = edit(1);
        EditSession second = edit(2);
        session.remember(first);
        session.remember(second);

        assertSame(second, session.undo(null, player));
        assertEquals(1, session.getHistoryPointer());
        verify(second).undo(any());
        assertSame(first, session.undo(null, player));
        assertEquals(0, session.getHistoryPointer());

        // Nothing left to undo: the pointer stays at zero
        assertNull(session.undo(null, player));
        assertEquals(0, session.getHistoryPointer());

        assertSame(first, session.redo(null, player));
        assertEquals(1, session.getHistoryPointer());
        verify(first).redo(any());
        assertSame(second, session.redo(null, player));
        assertEquals(2, session.getHistoryPointer());

        // Nothing left to redo
        assertNull(session.redo(null, player));
        assertEquals(2, session.getHistoryPointer());
    }

    @Test
    void rememberAfterUndoDiscardsRedoEntries() {
        EditSession first = edit(1);
        EditSession second = edit(1);
        EditSession third = edit(1);
        session.remember(first);
        session.remember(second);
        session.undo(null, player);

        session.remember(third);
        assertEquals(List.of(first, third), session.getHistory());
        assertEquals(2, session.getHistoryPointer());
        assertNull(session.redo(null, player));
        verify(second, never()).redo(any());
    }

    @Test
    void clearHistoryResetsPointer() {
        session.remember(edit(1));
        session.clearHistory();
        assertTrue(session.getHistory().isEmpty());
        assertEquals(0, session.getHistoryPointer());
        assertNull(session.undo(null, player));
    }

    @Test
    void concurrentRememberKeepsHistoryConsistent() throws Exception {
        LocalSession.MAX_HISTORY_SIZE = 10_000;
        int threads = 4;
        int perThread = 500;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                futures.add(executor.submit(() -> {
                    for (int i = 0; i < perThread; i++) {
                        session.remember(edit(1));
                        // Copying must never observe a half-updated history
                        session.getHistory();
                    }
                }));
            }
            for (Future<?> future : futures) {
                future.get(1, TimeUnit.MINUTES);
            }
        } finally {
            executor.shutdownNow();
        }
        assertEquals(threads * perThread, session.getHistory().size());
        assertEquals(threads * perThread, session.getHistoryPointer());
    }
}
