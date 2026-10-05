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

        // Row 1: tools and choices
        set(slot(1, 2), entry(Material.BRUSH, "Brushes", "Sphere, smooth, spline, layer, shatter,",
            "command and more brushes.", (player, _) -> WorldEditMenus.brushes(gui).open(player)));
        set(slot(1, 3), entry(Material.BLAZE_ROD, "Tools", "Wands, measure, inspect, tree,",
            "flood fill and other tools.", (player, _) -> WorldEditMenus.tools(gui).open(player)));
        set(slot(1, 4), entry(Buttons.patternIcon(state), "Blocks", "Pick the blocks used by every other menu.",
            "Current: " + Text.WHITE + state.getPattern().toPattern(),
            (player, _) -> new PatternMenu(gui, "the main menu", gui::openMain).open(player)));
        set(slot(1, 5), entry(Material.TINTED_GLASS, "Patterns & Masks", "Gradients, stripes, noise, and masks",
            "such as slopes, walls and floors.", (player, _) -> new PresetMenu(gui).open(player)));
        set(slot(1, 6), entry(Material.COMPARATOR, "Settings", "Limits, fast mode, side effects,",
            "clipboard and history.", (player, _) -> new SessionMenu(gui).open(player)));
        // Row 3: editing the world
        set(slot(3, 2), entry(Material.GOLDEN_AXE, "Selection & Region", "Wand, positions, //set, //replace,",
            "//walls, undo and redo.", (player, _) -> WorldEditMenus.selection(gui).open(player)));
        set(slot(3, 3), entry(Material.BEACON, "Generation", "Spheres, cylinders, pyramids, tori,",
            "domes, disks, helices and arches.", (player, _) -> WorldEditMenus.generation(gui).open(player)));
        set(slot(3, 4), entry(Material.BRICKS, "Build", "Text, symmetry, terrain, paths,",
            "caves, stairs and more.", (player, _) -> WorldEditMenus.build(gui).open(player)));
        set(slot(3, 5), entry(Material.BOOKSHELF, "Schematics", "Browse, load, inspect, rename",
            "and save schematics.", (player, _) -> new SchematicMenu(gui).open(player)));
        set(slot(3, 6), entry(Material.PAINTING, "Images", "Pixel art, top-view maps",
            "and heightmaps.", (player, _) -> new ImageMenu(gui).open(player)));

        set(slot(4, 4), Buttons.close());
        fillBorder(BORDER);
        fillEmpty(BACKGROUND);
    }

    private static Button entry(Material icon, String name, String line1, String line2, Button.ClickHandler handler) {
        return new Button(ItemBuilder.of(icon)
            .name(Text.GOLD + name)
            .lore(Text.GRAY + line1, Text.GRAY + line2, "", Text.YELLOW + "Click to open")
            .build(), handler);
    }
}
