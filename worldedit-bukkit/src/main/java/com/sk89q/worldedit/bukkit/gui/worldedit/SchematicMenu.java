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
import com.sk89q.worldedit.bukkit.gui.Text;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Browses the schematics folder: loads, inspects, renames and copies
 * schematics, and saves the clipboard in a chosen format.
 */
final class SchematicMenu extends FileBrowserMenu {

    SchematicMenu(WorldEditGui gui) {
        super(gui, "Schematics", "schematics");
    }

    @Override
    protected Path folder() {
        return gui.schematicsFolder();
    }

    @Override
    protected List<String> browsePermissions() {
        return GuiCommands.SCHEMATIC_BROWSE_PERMISSIONS;
    }

    @Override
    protected Collection<String> extensions() {
        return Arrays.asList(ClipboardFormats.getFileExtensionArray());
    }

    @Override
    protected String search(PlayerGuiState state) {
        return state.getSchematicSearch();
    }

    @Override
    protected void setSearch(PlayerGuiState state, String query) {
        state.setSchematicSearch(query);
    }

    @Override
    protected Button entryButton(Player viewer, FolderFiles.Entry entry) {
        String path = entry.relativePath();
        return new Button(ItemBuilder.of(entry.extension().equals("schem") ? Material.FILLED_MAP : Material.MAP)
            .name(Text.GOLD + entry.fileName())
            .lore(describe(entry))
            .lore(
                "",
                Text.YELLOW + "Left-click: " + Text.GRAY + "load into your clipboard",
                Text.YELLOW + "Shift-left-click: " + Text.GRAY + "show info (//schem info)",
                Text.YELLOW + "Right-click: " + Text.GRAY + "rename",
                Text.YELLOW + "Shift-right-click: " + Text.GRAY + "copy"
            )
            .build(), (player, click) -> onClick(player, path, click));
    }

    private void onClick(Player player, String path, ClickType click) {
        if (click == ClickType.SHIFT_RIGHT) {
            askName(player, "Name for the copy of " + path + "?", name -> GuiCommands.schematicCopy(path, name));
        } else if (click.isRightClick()) {
            askName(player, "New name for " + path + "?", name -> GuiCommands.schematicRename(path, name));
        } else if (click.isShiftClick()) {
            player.closeInventory();
            gui.run(player, GuiCommands.schematicInfo(path));
        } else {
            player.closeInventory();
            gui.run(player, GuiCommands.schematicLoad(path));
        }
    }

    @Override
    protected void buildExtraControls(Player viewer) {
        PlayerGuiState state = gui.state(viewer);
        String format = state.getSaveFormat();
        set(slot(CONTROL_ROW, 3), ItemBuilder.of(Material.WRITABLE_BOOK)
            .name(Text.GREEN + "Save clipboard")
            .lore(Text.GRAY + "Save your clipboard as a new", Text.GRAY + "schematic file.",
                Text.GRAY + "Format: " + Text.WHITE + format,
                "", Text.YELLOW + "Click to type a name in chat")
            .build(), (player, _) -> askName(player, "Name for the schematic?",
                name -> GuiCommands.schematicSave(name, gui.state(player).getSaveFormat(), false)));
        set(slot(CONTROL_ROW, 5), ItemBuilder.of(Material.NAME_TAG)
            .name(Text.GOLD + "Save format: " + Text.WHITE + format)
            .lore(
                Text.GRAY + "sponge: the default .schem format",
                Text.GRAY + "structure: vanilla structure block .nbt",
                Text.GRAY + "obj: 3D model (export only)",
                Text.GRAY + "json: block list (export only)",
                "", Text.YELLOW + "Click to cycle"
            )
            .build(), (player, _) -> {
                PlayerGuiState playerState = gui.state(player);
                playerState.setSaveFormat(GuiCommands.nextSaveFormat(playerState.getSaveFormat()));
                refresh(player);
            });
    }
}
