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

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A summary of one remembered edit in a {@link LocalSession}'s history.
 *
 * @param stepsBack how many undo steps away this entry is: 1 is the edit the next undo would revert,
 *                  0 or less means it has been undone (and -n+1 is the n-th redo)
 * @param blocksChanged the number of blocks changed by the edit
 * @param worldName the name of the world the edit happened in, or {@code null} if unknown
 * @param undone whether the edit is currently undone (and can be redone)
 */
public record HistoryEntry(int stepsBack, int blocksChanged, @Nullable String worldName, boolean undone) {

    /**
     * Summarize the history of a session, most recent entry first.
     *
     * @param session the session
     * @return the summarized entries
     */
    public static List<HistoryEntry> summarize(LocalSession session) {
        checkNotNull(session);
        return summarize(session.getHistory(), session.getHistoryPointer());
    }

    /**
     * Summarize a history list, most recent entry first.
     *
     * @param history the edit sessions, oldest first
     * @param pointer the history pointer; entries at or after it are undone
     * @return the summarized entries
     */
    public static List<HistoryEntry> summarize(List<EditSession> history, int pointer) {
        checkNotNull(history);
        checkArgument(pointer >= 0 && pointer <= history.size(), "pointer out of range");
        List<HistoryEntry> entries = new ArrayList<>(history.size());
        for (int i = history.size() - 1; i >= 0; i--) {
            EditSession editSession = history.get(i);
            World world = editSession.getWorld();
            entries.add(new HistoryEntry(
                pointer - i,
                editSession.size(),
                world == null ? null : world.getName(),
                i >= pointer
            ));
        }
        return entries;
    }
}
