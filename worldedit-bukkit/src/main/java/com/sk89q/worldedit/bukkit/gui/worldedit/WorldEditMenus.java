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

import java.util.EnumMap;
import java.util.Map;

/**
 * The menus built from {@link ActionMenu}: brushes, tools, build, selection
 * and generation.
 */
final class WorldEditMenus {

    private static final int[] GENERATION_PRESETS = {3, 5, 8, 10, 15, 20, 30};

    private static final Map<SelectionAction, Integer> SELECTION_LAYOUT = new EnumMap<>(SelectionAction.class);
    private static final Map<GenerationShape, Integer> GENERATION_LAYOUT = new EnumMap<>(GenerationShape.class);

    static {
        // Row 1: making a selection
        SELECTION_LAYOUT.put(SelectionAction.WAND, Menu.slot(1, 1));
        SELECTION_LAYOUT.put(SelectionAction.POS1, Menu.slot(1, 2));
        SELECTION_LAYOUT.put(SelectionAction.POS2, Menu.slot(1, 3));
        SELECTION_LAYOUT.put(SelectionAction.HPOS1, Menu.slot(1, 4));
        SELECTION_LAYOUT.put(SelectionAction.HPOS2, Menu.slot(1, 5));
        SELECTION_LAYOUT.put(SelectionAction.SEL_CUBOID, Menu.slot(1, 6));
        SELECTION_LAYOUT.put(SelectionAction.SEL_POLY, Menu.slot(1, 7));
        // Row 2: inspecting and adjusting it
        SELECTION_LAYOUT.put(SelectionAction.DESELECT, Menu.slot(2, 2));
        SELECTION_LAYOUT.put(SelectionAction.EXPAND_VERT, Menu.slot(2, 3));
        SELECTION_LAYOUT.put(SelectionAction.SIZE, Menu.slot(2, 5));
        SELECTION_LAYOUT.put(SelectionAction.DISTR, Menu.slot(2, 6));
        // Row 3: editing it
        SELECTION_LAYOUT.put(SelectionAction.SET, Menu.slot(3, 2));
        SELECTION_LAYOUT.put(SelectionAction.REPLACE, Menu.slot(3, 3));
        SELECTION_LAYOUT.put(SelectionAction.WALLS, Menu.slot(3, 4));
        SELECTION_LAYOUT.put(SelectionAction.FACES, Menu.slot(3, 5));
        SELECTION_LAYOUT.put(SelectionAction.WIREFRAME, Menu.slot(3, 6));
        // Row 4: history
        SELECTION_LAYOUT.put(SelectionAction.UNDO, Menu.slot(4, 3));
        SELECTION_LAYOUT.put(SelectionAction.HISTORY, Menu.slot(4, 4));
        SELECTION_LAYOUT.put(SelectionAction.REDO, Menu.slot(4, 5));

        int[] shapeSlots = {10, 11, 12, 13, 14, 15, 16, 21, 23};
        GenerationShape[] shapes = GenerationShape.values();
        for (int i = 0; i < shapes.length; i++) {
            GENERATION_LAYOUT.put(shapes[i], shapeSlots[i]);
        }
    }

    private WorldEditMenus() {
    }

    static Menu brushes(WorldEditGui gui) {
        return ActionMenu.builder(gui, "Brushes", BrushType.class)
            .size("Brush size", PlayerGuiState::getBrushSize, PlayerGuiState::setBrushSize, c -> c.maxBrushRadius)
            .hollow(PlayerGuiState::isBrushHollow, PlayerGuiState::setBrushHollow,
                "Hollow spheres, cylinders, lines and splines")
            .decorate((menu, _, _) -> menu.place(Menu.slot(5, 6), unbind(gui, "Unbind brush",
                "Remove the brush from your held item", GuiCommands.unbindBrush())))
            .build();
    }

    static Menu tools(WorldEditGui gui) {
        return ActionMenu.builder(gui, "Tools", ToolAction.class)
            .size("Range", PlayerGuiState::getToolSize, PlayerGuiState::setToolSize, c -> c.maxRadius)
            .decorate((menu, _, _) -> menu.place(Menu.slot(5, 6), unbind(gui, "Unbind tool",
                "Remove the tool from your held item", GuiCommands.unbindTool())))
            .build();
    }

    static Menu build(WorldEditGui gui) {
        return ActionMenu.builder(gui, "Build", BuildAction.class)
            .size("Size", PlayerGuiState::getBuildSize, PlayerGuiState::setBuildSize, c -> c.maxRadius)
            .build();
    }

    static Menu selection(WorldEditGui gui) {
        return ActionMenu.builder(gui, "Selection", SelectionAction.class)
            .layout(SELECTION_LAYOUT)
            .maskButton()
            .build();
    }

    static Menu generation(WorldEditGui gui) {
        return ActionMenu.builder(gui, "Generation", GenerationShape.class)
            .layout(GENERATION_LAYOUT)
            .size("Size", PlayerGuiState::getGenerationSize, PlayerGuiState::setGenerationSize, c -> c.maxRadius)
            .hollow(PlayerGuiState::isGenerationHollow, PlayerGuiState::setGenerationHollow,
                "Hollow spheres, cylinders, cones, ...")
            .decorate((menu, _, context) -> {
                int max = Buttons.effectiveMax(gui.config().maxRadius);
                for (int i = 0; i < GENERATION_PRESETS.length; i++) {
                    int preset = GuiCommands.clampSize(GENERATION_PRESETS[i], max);
                    boolean selected = preset == context.size();
                    menu.place(Menu.slot(3, 1 + i), new Button(
                        ItemBuilder.of(selected ? Material.LIME_CONCRETE : Material.WHITE_CONCRETE)
                            .name((selected ? Text.GREEN : Text.WHITE) + "Size " + preset)
                            .amount(preset)
                            .glow(selected)
                            .build(), (player, _) -> {
                                gui.state(player).setGenerationSize(preset);
                                menu.refresh(player);
                            }));
                }
            })
            .build();
    }

    private static Button unbind(WorldEditGui gui, String name, String description, String command) {
        return new Button(ItemBuilder.of(Material.BRUSH)
            .name(Text.RED + name)
            .lore(Text.GRAY + description, Text.DARK_GRAY + command)
            .build(), (player, _) -> {
                player.closeInventory();
                gui.run(player, command);
            });
    }
}
