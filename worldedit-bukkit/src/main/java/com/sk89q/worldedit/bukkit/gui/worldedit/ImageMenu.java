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
import com.sk89q.worldedit.util.image.Images;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/**
 * Browses WorldEdit's image folder: builds pixel art and imports heightmaps,
 * and exports the selection as a map or a heightmap.
 */
final class ImageMenu extends FileBrowserMenu {

    ImageMenu(WorldEditGui gui) {
        super(gui, "Images", "images");
    }

    @Override
    protected Path folder() {
        return gui.imagesFolder();
    }

    @Override
    protected List<String> browsePermissions() {
        return GuiCommands.IMAGE_BROWSE_PERMISSIONS;
    }

    @Override
    protected Collection<String> extensions() {
        return Images.READ_EXTENSIONS;
    }

    @Override
    protected String search(PlayerGuiState state) {
        return state.getImageSearch();
    }

    @Override
    protected void setSearch(PlayerGuiState state, String query) {
        state.setImageSearch(query);
    }

    @Override
    protected Button entryButton(Player viewer, FolderFiles.Entry entry) {
        String path = entry.relativePath();
        return new Button(ItemBuilder.of(Material.PAINTING)
            .name(Text.GOLD + entry.fileName())
            .lore(describe(entry))
            .lore(
                "",
                Text.YELLOW + "Left-click: " + Text.GRAY + "pixel art, flat (//image)",
                Text.YELLOW + "Right-click: " + Text.GRAY + "pixel art, upright",
                Text.YELLOW + "Shift-left-click: " + Text.GRAY + "pixel art, flat and dithered",
                Text.YELLOW + "Shift-right-click: " + Text.GRAY + "heightmap over the selection"
            )
            .build(), (player, click) -> {
                player.closeInventory();
                gui.run(player, command(path, click));
            });
    }

    private static String command(String path, ClickType click) {
        if (click == ClickType.SHIFT_RIGHT) {
            return GuiCommands.heightmapImport(path);
        }
        return GuiCommands.image(path, click.isRightClick(), click.isShiftClick());
    }

    @Override
    protected void buildExtraControls(Player viewer) {
        set(slot(CONTROL_ROW, 3), ItemBuilder.of(Material.FILLED_MAP)
            .name(Text.GREEN + "Export top view")
            .lore(Text.GRAY + "Save a shaded top-down map of", Text.GRAY + "the selection as a PNG image.",
                Text.DARK_GRAY + "//topview -s <name>",
                "", Text.YELLOW + "Click to type a name in chat")
            .build(), (player, _) -> askName(player, "Name for the map image?", GuiCommands::topView));
        set(slot(CONTROL_ROW, 5), ItemBuilder.of(Material.GRASS_BLOCK)
            .name(Text.GREEN + "Heightmaps")
            .lore(Text.GRAY + "Greyscale images of terrain heights.",
                Text.DARK_GRAY + "//heightmap export|import <name>",
                "",
                Text.YELLOW + "Left-click: " + Text.GRAY + "export the selection (type a name)",
                Text.YELLOW + "Right-click: " + Text.GRAY + "import over the selection (type a file)",
                Text.GRAY + "You can also shift-right-click an image.")
            .build(), (player, click) -> {
                if (click.isRightClick()) {
                    gui.ask(player, "Heightmap image to import, e.g. 'island.png':", InputKind.FILE,
                        file -> gui.run(player, GuiCommands.heightmapImport(file)), this::open);
                } else {
                    askName(player, "Name for the heightmap image?", GuiCommands::heightmapExport);
                }
            });
    }
}
