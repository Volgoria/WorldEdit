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

package com.sk89q.worldedit.session.storage;

import com.sk89q.worldedit.LocalSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonFileSessionStoreTest {

    @TempDir
    Path dir;

    private JsonFileSessionStore store;
    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        store = new JsonFileSessionStore(dir);
    }

    @Test
    void missingFileGivesFreshSession() throws IOException {
        LocalSession session = store.load(id);
        assertNotNull(session);
        assertNull(session.getLastScript());
    }

    @Test
    void savedSessionRoundTrips() throws IOException {
        LocalSession session = new LocalSession();
        session.setLastScript("draw.js");
        store.save(id, session);

        assertEquals("draw.js", store.load(id).getLastScript());
        assertFalse(Files.exists(dir.resolve(id + ".json.tmp")));
    }

    @Test
    void emptyFileIsDeletedAndReplacedByFreshSession() throws IOException {
        Path file = dir.resolve(id + ".json");
        Files.writeString(file, "");

        LocalSession session = store.load(id);
        assertNotNull(session);
        assertFalse(Files.exists(file));
    }

    @Test
    void malformedFileIsReportedAsIOException() throws IOException {
        Files.writeString(dir.resolve(id + ".json"), "{not json");
        assertThrows(IOException.class, () -> store.load(id));
    }
}
