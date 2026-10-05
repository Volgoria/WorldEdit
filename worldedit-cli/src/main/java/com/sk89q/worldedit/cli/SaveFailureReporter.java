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

package com.sk89q.worldedit.cli;

import java.util.HashSet;
import java.util.Set;

/**
 * Decides how loudly a failed world save is reported.
 *
 * <p>Worlds are saved after every command, so a world that cannot be saved
 * (for example one in a load-only format) would otherwise repeat the same
 * error after each command. The full error is shown once per world; later
 * failures are silent, except for a short reminder when the CLI exits. A
 * successful save resets this, so a new failure is reported in full again.</p>
 */
final class SaveFailureReporter {

    /**
     * How to report a failed save.
     */
    enum Report {
        /** Print the full error. */
        FULL,
        /** The error was already printed; print nothing. */
        SILENT,
        /** The error was already printed; print a short reminder before exiting. */
        REMINDER
    }

    private final Set<String> reported = new HashSet<>();

    /**
     * Record a failed save.
     *
     * @param world the name of the world
     * @param exiting whether this is the last save before the CLI exits
     * @return how to report it
     */
    Report failed(String world, boolean exiting) {
        if (reported.add(world)) {
            return Report.FULL;
        }
        return exiting ? Report.REMINDER : Report.SILENT;
    }

    /**
     * Record a successful save.
     *
     * @param world the name of the world
     */
    void saved(String world) {
        reported.remove(world);
    }
}
