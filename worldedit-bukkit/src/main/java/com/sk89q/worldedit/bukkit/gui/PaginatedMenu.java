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

import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * A six-row menu showing a list of entries over several pages.
 *
 * <p>The first five rows hold entries; the bottom row holds the page
 * navigation in slots 45 (previous), 49 (page info) and 53 (next). Subclasses
 * may place extra controls in the other bottom-row slots from
 * {@link #buildControls(Player)}.</p>
 *
 * @param <T> the entry type
 */
public abstract class PaginatedMenu<T> extends Menu {

    public static final int PAGE_SIZE = 45;
    protected static final int CONTROL_ROW = 5;

    private int page;

    protected PaginatedMenu(String title) {
        super(title, 6);
    }

    /**
     * Get all entries, in display order.
     *
     * @param viewer the viewer
     * @return the entries
     */
    protected abstract List<T> entries(Player viewer);

    /**
     * Create the button for an entry.
     *
     * @param viewer the viewer
     * @param entry the entry
     * @return the button
     */
    protected abstract Button entryButton(Player viewer, T entry);

    /**
     * Add extra controls to the bottom row (slots 46-48 and 50-52).
     *
     * @param viewer the viewer
     */
    protected void buildControls(Player viewer) {
    }

    /**
     * Get the text shown when there are no entries.
     *
     * @return the text
     */
    protected String emptyMessage() {
        return "Nothing to show";
    }

    @Override
    protected final void build(Player viewer) {
        List<T> entries = entries(viewer);
        int total = entries.size();
        page = Pagination.clampPage(page, total, PAGE_SIZE);
        int pages = Pagination.pageCount(total, PAGE_SIZE);

        List<T> visible = Pagination.pageItems(entries, page, PAGE_SIZE);
        for (int i = 0; i < visible.size(); i++) {
            set(i, entryButton(viewer, visible.get(i)));
        }
        if (total == 0) {
            set(slot(2, 4), Button.decoration(ItemBuilder.of(Material.BARRIER)
                .name(Text.RED + emptyMessage()).build()));
        }

        if (page > 0) {
            set(slot(CONTROL_ROW, 0), ItemBuilder.of(Material.ARROW)
                .name(Text.YELLOW + "Previous page")
                .lore(Text.GRAY + "Shift-click: first page").build(), (player, click) -> {
                    setPage(click.isShiftClick() ? 0 : page - 1);
                    refresh(player);
                });
        }
        set(slot(CONTROL_ROW, 4), Button.decoration(ItemBuilder.of(Material.PAPER)
            .name(Text.GOLD + "Page " + (page + 1) + " / " + pages)
            .lore(Text.GRAY + total + (total == 1 ? " entry" : " entries"))
            .amount(page + 1)
            .build()));
        if (page < pages - 1) {
            set(slot(CONTROL_ROW, 8), ItemBuilder.of(Material.ARROW)
                .name(Text.YELLOW + "Next page")
                .lore(Text.GRAY + "Shift-click: last page").build(), (player, click) -> {
                    setPage(click.isShiftClick() ? pages - 1 : page + 1);
                    refresh(player);
                });
        }
        buildControls(viewer);
        for (int column = 0; column < 9; column++) {
            setIfAbsent(slot(CONTROL_ROW, column), Button.decoration(BORDER));
        }
    }

    public int getPage() {
        return page;
    }

    /**
     * Set the page shown next time the menu is built. Out of range values are
     * clamped when building.
     *
     * @param page the zero-based page
     */
    public void setPage(int page) {
        this.page = Math.max(0, page);
    }
}
