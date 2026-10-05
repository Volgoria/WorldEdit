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

import org.junit.jupiter.api.Test;

import static com.sk89q.worldedit.cli.SaveFailureReporter.Report.FULL;
import static com.sk89q.worldedit.cli.SaveFailureReporter.Report.REMINDER;
import static com.sk89q.worldedit.cli.SaveFailureReporter.Report.SILENT;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SaveFailureReporterTest {

    private final SaveFailureReporter reporter = new SaveFailureReporter();

    @Test
    void repeatedFailuresAreReportedOnceThenRemindedOnExit() {
        assertEquals(FULL, reporter.failed("old.schematic", false));
        for (int i = 0; i < 5; i++) {
            assertEquals(SILENT, reporter.failed("old.schematic", false));
        }
        assertEquals(REMINDER, reporter.failed("old.schematic", true));
    }

    @Test
    void firstFailureOnExitIsReportedInFull() {
        assertEquals(FULL, reporter.failed("old.schematic", true));
    }

    @Test
    void worldsAreReportedSeparately() {
        assertEquals(FULL, reporter.failed("a", false));
        assertEquals(FULL, reporter.failed("b", false));
        assertEquals(SILENT, reporter.failed("a", false));
    }

    @Test
    void successfulSaveResetsTheReport() {
        assertEquals(FULL, reporter.failed("a", false));
        reporter.saved("a");
        assertEquals(FULL, reporter.failed("a", false));
    }
}
