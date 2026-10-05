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
import com.sk89q.worldedit.bukkit.gui.Menu;
import com.sk89q.worldedit.bukkit.gui.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * The {@code /wegui} landing menu.
 */
final class MainMenu extends Menu {

    private final WorldEditGui gui;

    MainMenu(WorldEditGui gui) {
        super(Text.DARK_GRAY + "WorldEdit", 5);
        this.gui = gui;
    }

    @Override
    protected void build(Player viewer) {
        PlayerGuiState state = gui.state(viewer);
        set(slot(0, 4), Button.decoration(ItemBuilder.of(Material.WOODEN_AXE)
            .name(Text.LIGHT_PURPLE + Text.BOLD + "WorldEdit")
            .lore(
                Text.GRAY + "Pattern: " + Text.WHITE + state.getPattern().toPattern(),
                Text.GRAY + "Mask: " + Text.WHITE + (state.getMask() == null ? "none" : state.getMask()),
                Text.GRAY + "Brush size: " + Text.WHITE + state.getBrushSize(),
                Text.GRAY + "Generation size: " + Text.WHITE + state.getGenerationSize()
            )
            .glow(true)
            .build()));

        set(slot(2, 2), ItemBuilder.of(Material.BRUSH)
            .name(Text.GOLD + "Brushes")
            .lore(Text.GRAY + "Bind sphere, smooth, overlay and", Text.GRAY + "other brushes to your item.",
                "", Text.YELLOW + "Click to open")
            .build(), (player, _) -> new BrushMenu(gui).open(player));
        set(slot(2, 3), ItemBuilder.of(Buttons.patternIcon(state))
            .name(Text.GOLD + "Blocks & Patterns")
            .lore(Text.GRAY + "Pick the blocks used by every", Text.GRAY + "other menu.",
                Text.GRAY + "Current: " + Text.WHITE + state.getPattern().toPattern(),
                "", Text.YELLOW + "Click to open")
            .build(), (player, _) -> new PatternMenu(gui, "the main menu", gui::openMain).open(player));
        set(slot(2, 4), ItemBuilder.of(Material.BOOKSHELF)
            .name(Text.GOLD + "Schematics")
            .lore(Text.GRAY + "Browse, load and save schematics.", "", Text.YELLOW + "Click to open")
            .build(), (player, _) -> new SchematicMenu(gui).open(player));
        set(slot(2, 5), ItemBuilder.of(Material.GOLDEN_AXE)
            .name(Text.GOLD + "Selection & Region")
            .lore(Text.GRAY + "Wand, positions, //set, //replace,", Text.GRAY + "//walls, undo and redo.",
                "", Text.YELLOW + "Click to open")
            .build(), (player, _) -> new SelectionMenu(gui).open(player));
        set(slot(2, 6), ItemBuilder.of(Material.BEACON)
            .name(Text.GOLD + "Generation")
            .lore(Text.GRAY + "Spheres, cylinders, pyramids, tori,", Text.GRAY + "domes, disks, helices and arches.",
                "", Text.YELLOW + "Click to open")
            .build(), (player, _) -> new GenerationMenu(gui).open(player));

        set(slot(4, 4), Buttons.close());
        fillBorder(BORDER);
        fillEmpty(BACKGROUND);
    }
}
