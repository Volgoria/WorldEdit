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

package com.sk89q.worldedit.history.changeset;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.history.UndoContext;
import com.sk89q.worldedit.history.change.Change;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("An ArrayListHistory")
public class ArrayListHistoryTest {

    /**
     * A change that records undo and redo calls into a shared log.
     */
    private record RecordingChange(String name, List<String> log) implements Change {
        @Override
        public void undo(UndoContext context) throws WorldEditException {
            log.add("undo " + name);
        }

        @Override
        public void redo(UndoContext context) throws WorldEditException {
            log.add("redo " + name);
        }
    }

    private static List<Change> drain(Iterator<Change> iterator) {
        return ImmutableList.copyOf(iterator);
    }

    @Test
    void startsEmptyAndRecording() {
        ArrayListHistory history = new ArrayListHistory();
        assertEquals(0, history.size());
        assertTrue(history.isRecordingChanges());
        assertFalse(history.forwardIterator().hasNext());
        assertFalse(history.backwardIterator().hasNext());
    }

    @Test
    void iteratesInInsertionAndReverseOrder() {
        List<String> log = new ArrayList<>();
        ArrayListHistory history = new ArrayListHistory();
        List<Change> changes = List.of(
            new RecordingChange("a", log), new RecordingChange("b", log), new RecordingChange("c", log)
        );
        changes.forEach(history::add);

        assertEquals(3, history.size());
        assertEquals(changes, drain(history.forwardIterator()));
        assertEquals(Lists.reverse(changes), drain(history.backwardIterator()));
    }

    @Test
    void undoThenRedoReplaysInTheRightOrder() throws WorldEditException {
        List<String> log = new ArrayList<>();
        ArrayListHistory history = new ArrayListHistory();
        history.add(new RecordingChange("first", log));
        history.add(new RecordingChange("second", log));
        history.add(new RecordingChange("third", log));

        UndoContext context = new UndoContext();
        for (Iterator<Change> it = history.backwardIterator(); it.hasNext(); ) {
            it.next().undo(context);
        }
        for (Iterator<Change> it = history.forwardIterator(); it.hasNext(); ) {
            it.next().redo(context);
        }

        assertEquals(List.of(
            "undo third", "undo second", "undo first",
            "redo first", "redo second", "redo third"
        ), log);
    }

    @Test
    void ignoresChangesWhileNotRecording() {
        List<String> log = new ArrayList<>();
        ArrayListHistory history = new ArrayListHistory();
        history.add(new RecordingChange("kept", log));

        history.setRecordChanges(false);
        assertFalse(history.isRecordingChanges());
        history.add(new RecordingChange("dropped", log));
        assertEquals(1, history.size());

        history.setRecordChanges(true);
        history.add(new RecordingChange("kept too", log));
        assertEquals(2, history.size());
        assertEquals(List.of("kept", "kept too"),
            drain(history.forwardIterator()).stream().map(c -> ((RecordingChange) c).name()).toList());
    }

    @Test
    void rejectsNullChanges() {
        ArrayListHistory history = new ArrayListHistory();
        assertThrows(NullPointerException.class, () -> history.add(null));
        assertEquals(0, history.size());
    }

    @Test
    void sameChangeCanBeRecordedTwice() {
        List<String> log = new ArrayList<>();
        ArrayListHistory history = new ArrayListHistory();
        Change change = new RecordingChange("x", log);
        history.add(change);
        history.add(change);
        assertEquals(2, history.size());
    }
}
