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

package com.sk89q.worldedit.bukkit.gui.worldedit;

import com.sk89q.worldedit.bukkit.gui.ItemBuilder;
import com.sk89q.worldedit.bukkit.gui.PaginatedMenu;
import com.sk89q.worldedit.bukkit.gui.Text;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import org.apache.logging.log4j.Logger;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * A paginated, searchable browser of the files in one of WorldEdit's folders.
 *
 * <p>The bottom row holds back (46), search (47), two menu-specific controls
 * (48 and 50), refresh (51) and close (52), around the page navigation.</p>
 */
abstract class FileBrowserMenu extends PaginatedMenu<FolderFiles.Entry> {

    private static final Logger LOGGER = LogManagerCompat.getLogger();
    private static final DateTimeFormatter DATE_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT).withZone(ZoneId.systemDefault());

    protected final WorldEditGui gui;
    private final String what;
    private List<FolderFiles.Entry> files = List.of();
    private boolean loadFailed;

    /**
     * Create the browser.
     *
     * @param gui the GUI
     * @param title the menu title, without prefix
     * @param what what is listed, plural and lower case, e.g. {@code "schematics"}
     */
    protected FileBrowserMenu(WorldEditGui gui, String title, String what) {
        super(Text.DARK_GRAY + "WorldEdit » " + title);
        this.gui = gui;
        this.what = what;
    }

    /**
     * Get the folder to list.
     *
     * @return the folder
     */
    protected abstract Path folder();

    /**
     * Get the accepted file extensions, without dots.
     *
     * @return the extensions
     */
    protected abstract Collection<String> extensions();

    /**
     * Get the viewer's search query.
     *
     * @param state the viewer's state
     * @return the query
     */
    protected abstract String search(PlayerGuiState state);

    /**
     * Store the viewer's search query.
     *
     * @param state the viewer's state
     * @param query the query, empty to clear
     */
    protected abstract void setSearch(PlayerGuiState state, String query);

    /**
     * Add the menu-specific controls, in slots 48 and 50.
     *
     * @param viewer the viewer
     */
    protected abstract void buildExtraControls(Player viewer);

    @Override
    public void open(Player player) {
        reload();
        super.open(player);
    }

    private void reload() {
        try {
            files = FolderFiles.list(folder(), extensions());
            loadFailed = false;
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not list " + what + " in " + folder(), e);
            files = List.of();
            loadFailed = true;
        }
    }

    @Override
    protected List<FolderFiles.Entry> entries(Player viewer) {
        return FolderFiles.filter(files, search(gui.state(viewer)));
    }

    @Override
    protected String emptyMessage() {
        return loadFailed ? "Could not read the " + what + " folder" : "No " + what + " found";
    }

    /**
     * Get the lore lines describing a file.
     *
     * @param entry the file
     * @return the lines
     */
    protected static List<String> describe(FolderFiles.Entry entry) {
        String path = entry.relativePath();
        int slash = path.lastIndexOf('/');
        return List.of(
            Text.GRAY + "Folder: " + Text.WHITE + (slash >= 0 ? path.substring(0, slash) : "/"),
            Text.GRAY + "Size: " + Text.WHITE + Text.formatSize(entry.size()),
            Text.GRAY + "Modified: " + Text.WHITE + DATE_FORMAT.format(Instant.ofEpochMilli(entry.lastModified()))
        );
    }

    @Override
    protected final void buildControls(Player viewer) {
        set(slot(CONTROL_ROW, 1), Buttons.backToMain(gui));
        set(slot(CONTROL_ROW, 2), Buttons.search(gui, search(gui.state(viewer)), "Type part of a file name:",
            (player, query) -> {
                setSearch(gui.state(player), query);
                setPage(0);
                open(player);
            }, this::open));
        buildExtraControls(viewer);
        set(slot(CONTROL_ROW, 6), ItemBuilder.of(Material.SUNFLOWER)
            .name(Text.GOLD + "Refresh")
            .lore(Text.GRAY + "Re-read the " + what + " folder")
            .build(), (player, _) -> open(player));
        set(slot(CONTROL_ROW, 7), Buttons.close());
    }

    /**
     * Ask for a new file name in chat and run a command with it.
     *
     * @param player the player
     * @param question the question
     * @param command builds the command from the validated name
     */
    protected void askName(Player player, String question, Function<String, String> command) {
        gui.ask(player, question + " (letters, digits, _ and -; use / for folders)", InputKind.FILE_NAME,
            name -> gui.run(player, command.apply(name)), this::open);
    }
}
