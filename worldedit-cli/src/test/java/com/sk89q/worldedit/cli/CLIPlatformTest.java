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

import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extension.platform.Preference;
import com.sk89q.worldedit.world.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CLIPlatformTest {

    @TempDir
    Path tempDir;

    private CLIPlatform platform;

    @BeforeEach
    void setUp() {
        platform = new CLIPlatform(new CLIWorldEdit(tempDir));
    }

    @AfterEach
    void tearDown() {
        platform.shutdown();
    }

    @Test
    void repeatingTaskKeepsRunning() throws InterruptedException {
        // Previously the task rescheduled itself, which throws IllegalStateException on the second run
        CountDownLatch latch = new CountDownLatch(3);
        int id = platform.schedule(0, 1, latch::countDown);
        assertTrue(id >= 0);
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Repeating task did not run 3 times");
    }

    @Test
    void oneShotTaskRunsOnce() throws InterruptedException {
        AtomicInteger runs = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(1);
        platform.schedule(0, 0, () -> {
            runs.incrementAndGet();
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        Thread.sleep(5 * CLIPlatform.MILLIS_PER_TICK);
        assertEquals(1, runs.get());
    }

    @Test
    void delayIsInTicks() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        long start = System.nanoTime();
        platform.schedule(4, -1, latch::countDown);
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        assertTrue(elapsedMillis >= 4 * CLIPlatform.MILLIS_PER_TICK - 10, "Ran after only " + elapsedMillis + "ms");
    }

    @Test
    void failingTaskDoesNotKillScheduler() throws InterruptedException {
        platform.schedule(0, -1, () -> {
            throw new IllegalStateException("boom");
        });
        CountDownLatch latch = new CountDownLatch(1);
        platform.schedule(1, -1, latch::countDown);
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Scheduler died after a failing task");
    }

    @Test
    void schedulingAfterShutdownFails() {
        platform.shutdown();
        assertEquals(-1, platform.schedule(0, -1, () -> { }));
    }

    @Test
    void taskIdsAreUnique() {
        int first = platform.schedule(100, -1, () -> { });
        int second = platform.schedule(100, -1, () -> { });
        assertTrue(first != second);
    }

    @Test
    void dataVersion() {
        assertEquals(-1, platform.getDataVersion());
        platform.setDataVersion(TestSchematics.DATA_VERSION);
        assertEquals(TestSchematics.DATA_VERSION, platform.getDataVersion());
    }

    @Test
    void identity() {
        assertEquals("enginehub:cli", platform.id());
        assertEquals("CLI-Official", platform.getPlatformName());
        // No jar manifest when running tests
        assertEquals("unknown", platform.getVersion());
        assertEquals(platform.getVersion(), platform.getPlatformVersion());
    }

    @Test
    void capabilities() {
        assertEquals(Preference.PREFERRED, platform.getCapabilities().get(Capability.WORLD_EDITING));
        assertEquals(Preference.PREFER_OTHERS, platform.getCapabilities().get(Capability.CONFIGURATION));
        assertTrue(platform.getSupportedSideEffects().isEmpty());
    }

    @Test
    void worlds() {
        World world = mock(World.class);
        when(world.id()).thenReturn("tiny.schem");
        World other = mock(World.class);
        when(other.id()).thenReturn("other.schem");

        assertTrue(platform.getWorlds().isEmpty());
        assertNull(platform.matchWorld(world));

        platform.addWorld(world);
        assertEquals(1, platform.getWorlds().size());
        assertSame(world, platform.matchWorld(world));
        assertNull(platform.matchWorld(other));
    }

    @Test
    void noPlayers() {
        // (isValidMobType is not tested here: it needs a running WorldEdit, and initializing
        // EntityTypes without one would break the class for the rest of the test run)
        assertNull(platform.matchPlayer(mock(Player.class)));
    }
}
