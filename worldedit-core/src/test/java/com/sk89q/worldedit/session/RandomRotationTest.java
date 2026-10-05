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

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.math.transform.Identity;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class RandomRotationTest {

    private static void assertVectorEquals(Vector3 expected, Vector3 actual) {
        assertTrue(expected.distance(actual) < 1e-9, () -> "expected " + expected + " but was " + actual);
    }

    @Test
    void randomQuarterTurnCoversAllAngles() {
        Random random = new Random(42);
        Set<Integer> seen = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            int angle = RandomRotation.randomQuarterTurn(random);
            assertTrue(angle == 0 || angle == 90 || angle == 180 || angle == 270, "unexpected angle " + angle);
            seen.add(angle);
        }
        assertEquals(Set.of(0, 90, 180, 270), seen);
    }

    @Test
    void rotatedDoesNotModifyOriginal() {
        ClipboardHolder holder = new ClipboardHolder(mock(Clipboard.class));
        ClipboardHolder rotated = RandomRotation.rotated(holder, 90);

        assertInstanceOf(Identity.class, holder.getTransform());
        assertSame(holder.getClipboard(), rotated.getClipboard());
    }

    @Test
    void rotationIsClockwiseLikeRotateCommand() {
        ClipboardHolder holder = new ClipboardHolder(mock(Clipboard.class));
        Vector3 east = Vector3.at(1, 0, 0);

        assertVectorEquals(east, RandomRotation.rotated(holder, 0).getTransform().apply(east));
        // Clockwise when viewed from above: east -> south (+z)
        assertVectorEquals(Vector3.at(0, 0, 1), RandomRotation.rotated(holder, 90).getTransform().apply(east));
        assertVectorEquals(Vector3.at(-1, 0, 0), RandomRotation.rotated(holder, 180).getTransform().apply(east));
        assertVectorEquals(Vector3.at(0, 0, -1), RandomRotation.rotated(holder, 270).getTransform().apply(east));
    }

    @Test
    void rotationStacksOnExistingTransform() {
        ClipboardHolder holder = new ClipboardHolder(mock(Clipboard.class));
        holder.setTransform(new AffineTransform().rotateY(-90));
        Vector3 east = Vector3.at(1, 0, 0);

        assertVectorEquals(Vector3.at(-1, 0, 0), RandomRotation.rotated(holder, 90).getTransform().apply(east));
        assertVectorEquals(east, RandomRotation.rotated(holder, 270).getTransform().apply(east));
    }

    @Test
    void rejectsNonQuarterTurns() {
        ClipboardHolder holder = new ClipboardHolder(mock(Clipboard.class));
        assertThrows(IllegalArgumentException.class, () -> RandomRotation.rotated(holder, 45));
    }
}
