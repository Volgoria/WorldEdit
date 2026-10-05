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

package com.sk89q.worldedit.bukkit.gui;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaginationTest {

    @Test
    void pageCountIsAtLeastOne() {
        assertEquals(1, Pagination.pageCount(0, 45));
        assertEquals(1, Pagination.pageCount(1, 45));
        assertEquals(1, Pagination.pageCount(45, 45));
    }

    @Test
    void pageCountRoundsUp() {
        assertEquals(2, Pagination.pageCount(46, 45));
        assertEquals(2, Pagination.pageCount(90, 45));
        assertEquals(3, Pagination.pageCount(91, 45));
    }

    @Test
    void pageCountRejectsBadArguments() {
        assertThrows(IllegalArgumentException.class, () -> Pagination.pageCount(10, 0));
        assertThrows(IllegalArgumentException.class, () -> Pagination.pageCount(-1, 10));
    }

    @Test
    void clampPageKeepsPageInRange() {
        assertEquals(0, Pagination.clampPage(-3, 100, 45));
        assertEquals(1, Pagination.clampPage(1, 100, 45));
        assertEquals(2, Pagination.clampPage(2, 100, 45));
        assertEquals(2, Pagination.clampPage(99, 100, 45));
        assertEquals(0, Pagination.clampPage(5, 0, 45));
    }

    @Test
    void indicesCoverEachPage() {
        assertEquals(0, Pagination.startIndex(0, 100, 45));
        assertEquals(45, Pagination.endIndex(0, 100, 45));
        assertEquals(90, Pagination.startIndex(2, 100, 45));
        assertEquals(100, Pagination.endIndex(2, 100, 45));
        // Out of range pages are clamped to the last page
        assertEquals(90, Pagination.startIndex(7, 100, 45));
    }

    @Test
    void pageItemsReturnsTheRightSlice() {
        List<Integer> items = IntStream.range(0, 100).boxed().toList();
        assertEquals(IntStream.range(0, 45).boxed().toList(), Pagination.pageItems(items, 0, 45));
        assertEquals(IntStream.range(45, 90).boxed().toList(), Pagination.pageItems(items, 1, 45));
        assertEquals(IntStream.range(90, 100).boxed().toList(), Pagination.pageItems(items, 2, 45));
        assertTrue(Pagination.pageItems(List.of(), 3, 45).isEmpty());
    }

    @Test
    void everyItemAppearsExactlyOnce() {
        for (int total = 0; total < 200; total += 7) {
            List<Integer> items = IntStream.range(0, total).boxed().toList();
            int seen = 0;
            for (int page = 0; page < Pagination.pageCount(total, 45); page++) {
                List<Integer> slice = Pagination.pageItems(items, page, 45);
                for (int i = 0; i < slice.size(); i++) {
                    assertEquals(seen + i, slice.get(i));
                }
                seen += slice.size();
            }
            assertEquals(total, seen);
        }
    }
}
