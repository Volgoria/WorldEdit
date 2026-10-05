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

package com.sk89q.worldedit.function.mask;

import com.google.common.collect.ImmutableList;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OffsetsMaskTest {

    // matches the plane x = 0
    private static final Mask PLANE = position -> position.x() == 0;
    private static final Mask2D PLANE_2D = position -> position.x() == 0;

    @Test
    void countsMatchingOffsets() {
        OffsetsMask one = OffsetsMask.builder(PLANE).minMatches(1).maxMatches(1).build();
        assertTrue(one.test(BlockVector3.at(1, 5, 5)));
        assertTrue(one.test(BlockVector3.at(-1, 5, 5)));
        // in the plane, four neighbours match
        assertFalse(one.test(BlockVector3.at(0, 5, 5)));
        assertFalse(one.test(BlockVector3.at(2, 5, 5)));

        OffsetsMask four = OffsetsMask.builder(PLANE).minMatches(4).build();
        assertTrue(four.test(BlockVector3.at(0, 0, 0)));
        assertFalse(four.test(BlockVector3.at(1, 0, 0)));
    }

    @Test
    void excludesSelf() {
        OffsetsMask mask = OffsetsMask.builder(PLANE).excludeSelf(true).build();
        assertFalse(mask.test(BlockVector3.at(0, 0, 0)));
        assertTrue(mask.test(BlockVector3.at(1, 0, 0)));
    }

    @Test
    void customOffsets() {
        OffsetsMask mask = OffsetsMask.builder(PLANE)
            .offsets(ImmutableList.of(BlockVector3.at(-2, 0, 0), BlockVector3.at(-3, 0, 0)))
            .build();
        assertEquals(2, mask.getOffsets().size());
        assertTrue(mask.test(BlockVector3.at(2, 0, 0)));
        assertTrue(mask.test(BlockVector3.at(3, 0, 0)));
        assertFalse(mask.test(BlockVector3.at(1, 0, 0)));
    }

    @Test
    void mask2D() {
        OffsetsMask2D mask = OffsetsMask2D.builder(PLANE_2D).minMatches(1).maxMatches(1).build();
        assertTrue(mask.test(BlockVector2.at(1, 0)));
        assertFalse(mask.test(BlockVector2.at(0, 0)));
        assertFalse(mask.test(BlockVector2.at(3, 0)));

        Mask2D converted = OffsetsMask.builder(Masks.asMask(PLANE_2D)).excludeSelf(true).build().toMask2D();
        assertNotNull(converted);
        assertTrue(converted.test(BlockVector2.at(-1, 7)));
        assertFalse(converted.test(BlockVector2.at(0, 7)));
    }
}
