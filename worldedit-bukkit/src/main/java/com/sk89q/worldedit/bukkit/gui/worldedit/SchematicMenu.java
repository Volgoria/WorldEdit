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

import com.sk89q.worldedit.bukkit.gui.Button;
import com.sk89q.worldedit.bukkit.gui.ItemBuilder;
import com.sk89q.worldedit.bukkit.gui.PaginatedMenu;
import com.sk89q.worldedit.bukkit.gui.Text;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import org.apache.logging.log4j.Logger;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Browses the schematics folder; loads schematics and saves the clipboard.
 */
final class SchematicMenu extends PaginatedMenu<SchematicFiles.Entry> {

    private static final Logger LOGGER = LogManagerCompat.getLogger();
    private static final DateTimeFormatter DATE_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT).withZone(ZoneId.systemDefault());

    private final WorldEditGui gui;
    private List<SchematicFiles.Entry> files = List.of();
    private boolean loadFailed;

    SchematicMenu(WorldEditGui gui) {
        super(Text.DARK_GRAY + "WorldEdit » Schematics");
        this.gui = gui;
        reload();
    }

    private void reload() {
        try {
            files = SchematicFiles.list(gui.schematicsFolder(), Arrays.asList(ClipboardFormats.getFileExtensionArray()));
            loadFailed = false;
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not list schematics in " + gui.schematicsFolder(), e);
            files = List.of();
            loadFailed = true;
        }
    }

    @Override
    protected List<SchematicFiles.Entry> entries(Player viewer) {
        return SchematicFiles.filter(files, gui.state(viewer).getSchematicSearch());
    }

    @Override
    protected String emptyMessage() {
        return loadFailed ? "Could not read the schematics folder" : "No schematics found";
    }

    @Override
    protected Button entryButton(Player viewer, SchematicFiles.Entry entry) {
        String path = entry.relativePath();
        int slash = path.lastIndexOf('/');
        return new Button(ItemBuilder.of(entry.extension().equals("schem") ? Material.FILLED_MAP : Material.MAP)
            .name(Text.GOLD + entry.fileName())
            .lore(
                Text.GRAY + "Folder: " + Text.WHITE + (slash >= 0 ? path.substring(0, slash) : "/"),
                Text.GRAY + "Size: " + Text.WHITE + Text.formatSize(entry.size()),
                Text.GRAY + "Modified: " + Text.WHITE + DATE_FORMAT.format(Instant.ofEpochMilli(entry.lastModified())),
                "",
                Text.YELLOW + "Click: " + Text.GRAY + "load into your clipboard",
                Text.YELLOW + "Shift-click: " + Text.GRAY + "show info in chat"
            )
            .build(), (player, click) -> {
                if (click.isShiftClick()) {
                    showInfo(player, entry);
                    return;
                }
                player.closeInventory();
                gui.run(player, GuiCommands.schematicLoad(path));
            });
    }

    private void showInfo(Player player, SchematicFiles.Entry entry) {
        Path file = gui.schematicsFolder().resolve(entry.relativePath());
        String format = "unknown";
        if (Files.isRegularFile(file)) {
            ClipboardFormat detected = ClipboardFormats.findByPath(file);
            if (detected != null) {
                format = detected.getName();
            }
        }
        Buttons.sendMessage(player, Text.GOLD + entry.fileName());
        player.sendMessage(Text.GRAY + "  Path: " + Text.WHITE + entry.relativePath());
        player.sendMessage(Text.GRAY + "  Format: " + Text.WHITE + format);
        player.sendMessage(Text.GRAY + "  Size: " + Text.WHITE + Text.formatSize(entry.size()));
        player.sendMessage(Text.GRAY + "  Modified: " + Text.WHITE
            + DATE_FORMAT.format(Instant.ofEpochMilli(entry.lastModified())));
        player.sendMessage(Text.GRAY + "  Load with: " + Text.WHITE + GuiCommands.schematicLoad(entry.relativePath()));
    }

    @Override
    protected void buildControls(Player viewer) {
        String search = gui.state(viewer).getSchematicSearch();
        set(slot(CONTROL_ROW, 1), Buttons.back("the main menu", (player, _) -> gui.openMain(player)));
        set(slot(CONTROL_ROW, 2), ItemBuilder.of(Material.OAK_SIGN)
            .name(Text.GOLD + "Search" + (search.isEmpty() ? "" : ": " + Text.WHITE + search))
            .lore(Text.YELLOW + "Left-click to filter by name", Text.YELLOW + "Right-click to clear the filter")
            .glow(!search.isEmpty())
            .build(), (player, click) -> {
                if (click.isRightClick()) {
                    gui.state(player).setSchematicSearch("");
                    setPage(0);
                    refresh(player);
                    return;
                }
                gui.prompts().ask(player, "Type part of a schematic name:", input -> {
                    gui.state(player).setSchematicSearch(input);
                    setPage(0);
                    open(player);
                }, () -> open(player));
            });
        set(slot(CONTROL_ROW, 3), ItemBuilder.of(Material.WRITABLE_BOOK)
            .name(Text.GREEN + "Save clipboard")
            .lore(Text.GRAY + "Save your clipboard as a new", Text.GRAY + "schematic file.",
                "", Text.YELLOW + "Click to type a name in chat")
            .build(), (player, _) -> promptSave(player));
        set(slot(CONTROL_ROW, 5), ItemBuilder.of(Material.SUNFLOWER)
            .name(Text.GOLD + "Refresh")
            .lore(Text.GRAY + "Re-read the schematics folder")
            .build(), (player, _) -> {
                reload();
                refresh(player);
            });
        set(slot(CONTROL_ROW, 7), Buttons.close());
    }

    private void promptSave(Player player) {
        gui.prompts().ask(player, "Name for the schematic (letters, digits, _ and -; use / for folders):",
            input -> {
                if (!GuiCommands.isValidSchematicName(input)) {
                    Buttons.sendMessage(player, Text.RED + "Invalid name. Use only letters, digits, _ and -.");
                    open(player);
                    return;
                }
                gui.run(player, GuiCommands.schematicSave(input, false));
            }, () -> open(player));
    }
}
