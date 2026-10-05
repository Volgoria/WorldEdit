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

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Pure pagination math used by {@link PaginatedMenu}.
 */
public final class Pagination {

    private Pagination() {
    }

    /**
     * Get the number of pages needed to show the given number of items.
     *
     * <p>There is always at least one page, even when there are no items.</p>
     *
     * @param totalItems the number of items
     * @param pageSize the number of items per page
     * @return the number of pages, at least 1
     */
    public static int pageCount(int totalItems, int pageSize) {
        checkArgument(pageSize > 0, "pageSize must be positive");
        checkArgument(totalItems >= 0, "totalItems must not be negative");
        if (totalItems == 0) {
            return 1;
        }
        return (totalItems + pageSize - 1) / pageSize;
    }

    /**
     * Clamp a zero-based page index into the valid range.
     *
     * @param page the requested page
     * @param totalItems the number of items
     * @param pageSize the number of items per page
     * @return a page index between 0 and {@code pageCount - 1}
     */
    public static int clampPage(int page, int totalItems, int pageSize) {
        int last = pageCount(totalItems, pageSize) - 1;
        return Math.max(0, Math.min(page, last));
    }

    /**
     * Get the index of the first item shown on a page.
     *
     * @param page the page (clamped)
     * @param totalItems the number of items
     * @param pageSize the number of items per page
     * @return the inclusive start index
     */
    public static int startIndex(int page, int totalItems, int pageSize) {
        return clampPage(page, totalItems, pageSize) * pageSize;
    }

    /**
     * Get the index after the last item shown on a page.
     *
     * @param page the page (clamped)
     * @param totalItems the number of items
     * @param pageSize the number of items per page
     * @return the exclusive end index
     */
    public static int endIndex(int page, int totalItems, int pageSize) {
        return Math.min(totalItems, startIndex(page, totalItems, pageSize) + pageSize);
    }

    /**
     * Get the items shown on a page.
     *
     * @param items all items
     * @param page the page (clamped)
     * @param pageSize the number of items per page
     * @param <T> the item type
     * @return a view of the items on that page
     */
    public static <T> List<T> pageItems(List<T> items, int page, int pageSize) {
        int total = items.size();
        return items.subList(startIndex(page, total, pageSize), endIndex(page, total, pageSize));
    }
}
