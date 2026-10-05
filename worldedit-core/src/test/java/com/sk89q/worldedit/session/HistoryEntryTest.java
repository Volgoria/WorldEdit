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

package com.sk89q.worldedit.session;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.world.World;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HistoryEntryTest {

    private static EditSession edit(int size, World world) {
        EditSession editSession = mock(EditSession.class);
        when(editSession.size()).thenReturn(size);
        when(editSession.getWorld()).thenReturn(world);
        return editSession;
    }

    private static World world(String name) {
        World world = mock(World.class);
        when(world.getName()).thenReturn(name);
        return world;
    }

    @Test
    void emptySessionHasNoEntries() {
        assertTrue(HistoryEntry.summarize(new LocalSession()).isEmpty());
    }

    @Test
    void summarizesMostRecentFirst() {
        World overworld = world("overworld");
        World nether = world("nether");
        LocalSession session = new LocalSession();
        session.remember(edit(10, overworld));
        session.remember(edit(0, overworld)); // empty edits are not remembered
        session.remember(edit(25, nether));
        session.remember(edit(3, null));

        assertEquals(3, session.getHistory().size());
        assertEquals(3, session.getHistoryPointer());
        assertEquals(List.of(
            new HistoryEntry(1, 3, null, false),
            new HistoryEntry(2, 25, "nether", false),
            new HistoryEntry(3, 10, "overworld", false)
        ), HistoryEntry.summarize(session));
    }

    @Test
    void marksUndoneEntries() {
        World overworld = world("overworld");
        List<EditSession> history = List.of(edit(1, overworld), edit(2, overworld), edit(3, overworld));

        assertEquals(List.of(
            new HistoryEntry(-1, 3, "overworld", true),
            new HistoryEntry(0, 2, "overworld", true),
            new HistoryEntry(1, 1, "overworld", false)
        ), HistoryEntry.summarize(history, 1));

        assertTrue(HistoryEntry.summarize(history, 0).stream().allMatch(HistoryEntry::undone));
    }

    @Test
    void historySnapshotIsImmutable() {
        LocalSession session = new LocalSession();
        session.remember(edit(5, null));
        assertThrows(UnsupportedOperationException.class, () -> session.getHistory().clear());
        session.clearHistory();
        assertEquals(0, session.getHistoryPointer());
        assertTrue(session.getHistory().isEmpty());
    }

    @Test
    void rejectsOutOfRangePointer() {
        assertThrows(IllegalArgumentException.class, () -> HistoryEntry.summarize(List.of(), 1));
    }
}
