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
import com.sk89q.worldedit.bukkit.gui.Menu;
import com.sk89q.worldedit.bukkit.gui.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates shapes at the player's position with size presets.
 */
final class GenerationMenu extends Menu {

    private static final int[] SHAPE_SLOTS = {10, 11, 12, 13, 14, 15, 16, 21, 23};
    private static final int[] PRESETS = {3, 5, 8, 10, 15, 20, 30};

    private final WorldEditGui gui;

    GenerationMenu(WorldEditGui gui) {
        super(Text.DARK_GRAY + "WorldEdit » Generation", 6);
        this.gui = gui;
    }

    static Material icon(GenerationShape shape) {
        return switch (shape) {
            case SPHERE -> Material.SLIME_BLOCK;
            case CYLINDER -> Material.CAULDRON;
            case CONE -> Material.POINTED_DRIPSTONE;
            case PYRAMID -> Material.SANDSTONE_STAIRS;
            case TORUS -> Material.HEART_OF_THE_SEA;
            case DOME -> Material.TURTLE_HELMET;
            case DISK -> Material.HEAVY_WEIGHTED_PRESSURE_PLATE;
            case HELIX -> Material.TWISTING_VINES;
            case ARCH -> Material.STONE_BRICK_STAIRS;
        };
    }

    private int maxSize() {
        return gui.config().maxRadius;
    }

    @Override
    protected void build(Player viewer) {
        PlayerGuiState state = gui.state(viewer);
        int size = GuiCommands.clampSize(state.getGenerationSize(), maxSize());
        state.setGenerationSize(size);
        String pattern = state.getPattern().toPattern();

        GenerationShape[] shapes = GenerationShape.values();
        for (int i = 0; i < shapes.length && i < SHAPE_SLOTS.length; i++) {
            GenerationShape shape = shapes[i];
            String command = GuiCommands.generate(shape, pattern, size, state.isGenerationHollow());
            List<String> lore = new ArrayList<>();
            lore.add(Text.GRAY + shape.getDescription());
            lore.add(Text.GRAY + "Pattern: " + Text.WHITE + pattern);
            if (shape.supportsHollow()) {
                lore.add(Text.GRAY + "Hollow: " + Text.WHITE + (state.isGenerationHollow() ? "yes" : "no"));
            }
            lore.add(Text.DARK_GRAY + command);
            lore.add("");
            lore.add(Text.YELLOW + "Click to generate where you stand");
            set(SHAPE_SLOTS[i], ItemBuilder.of(icon(shape))
                .name(Text.GOLD + shape.getDisplayName())
                .lore(lore)
                .build(), (player, _) -> {
                    player.closeInventory();
                    gui.run(player, command);
                });
        }

        Runnable refresh = () -> refresh(viewer);
        for (int i = 0; i < PRESETS.length; i++) {
            int preset = GuiCommands.clampSize(PRESETS[i], maxSize());
            boolean selected = preset == size;
            set(slot(3, 1 + i), ItemBuilder.of(selected ? Material.LIME_CONCRETE : Material.WHITE_CONCRETE)
                .name((selected ? Text.GREEN : Text.WHITE) + "Size " + preset)
                .amount(preset)
                .glow(selected)
                .build(), (_, _) -> {
                    state.setGenerationSize(preset);
                    refresh.run();
                });
        }
        set(slot(4, 1), Buttons.sizeStep(-5, size, maxSize(), state::setGenerationSize, refresh));
        set(slot(4, 2), Buttons.sizeStep(-1, size, maxSize(), state::setGenerationSize, refresh));
        set(slot(4, 4), Buttons.sizeDisplay(size, maxSize(), "Size"));
        set(slot(4, 6), Buttons.sizeStep(1, size, maxSize(), state::setGenerationSize, refresh));
        set(slot(4, 7), Buttons.sizeStep(5, size, maxSize(), state::setGenerationSize, refresh));

        set(slot(5, 0), Buttons.back("the main menu", (player, _) -> gui.openMain(player)));
        set(slot(5, 3), Buttons.pattern(state, (player, _) ->
            new PatternMenu(gui, "generation", p -> new GenerationMenu(gui).open(p)).open(player)));
        set(slot(5, 5), Buttons.toggle("Hollow", state.isGenerationHollow(),
            "Hollow spheres, cylinders, cones, ...",
            () -> state.setGenerationHollow(!state.isGenerationHollow()), refresh));
        set(slot(5, 8), Buttons.close());

        fillBorder(BORDER);
        fillEmpty(BACKGROUND);
    }
}
