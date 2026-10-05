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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Selection, region and history commands.
 */
final class SelectionMenu extends Menu {

    private static final Map<SelectionAction, Integer> LAYOUT = new EnumMap<>(SelectionAction.class);

    static {
        // Row 1: making a selection
        LAYOUT.put(SelectionAction.WAND, slot(1, 1));
        LAYOUT.put(SelectionAction.POS1, slot(1, 2));
        LAYOUT.put(SelectionAction.POS2, slot(1, 3));
        LAYOUT.put(SelectionAction.HPOS1, slot(1, 4));
        LAYOUT.put(SelectionAction.HPOS2, slot(1, 5));
        LAYOUT.put(SelectionAction.SEL_CUBOID, slot(1, 6));
        LAYOUT.put(SelectionAction.SEL_POLY, slot(1, 7));
        // Row 2: inspecting and adjusting it
        LAYOUT.put(SelectionAction.DESELECT, slot(2, 2));
        LAYOUT.put(SelectionAction.EXPAND_VERT, slot(2, 3));
        LAYOUT.put(SelectionAction.SIZE, slot(2, 5));
        LAYOUT.put(SelectionAction.DISTR, slot(2, 6));
        // Row 3: editing it
        LAYOUT.put(SelectionAction.SET, slot(3, 2));
        LAYOUT.put(SelectionAction.REPLACE, slot(3, 3));
        LAYOUT.put(SelectionAction.WALLS, slot(3, 4));
        LAYOUT.put(SelectionAction.FACES, slot(3, 5));
        LAYOUT.put(SelectionAction.WIREFRAME, slot(3, 6));
        // Row 4: history
        LAYOUT.put(SelectionAction.UNDO, slot(4, 3));
        LAYOUT.put(SelectionAction.HISTORY, slot(4, 4));
        LAYOUT.put(SelectionAction.REDO, slot(4, 5));
    }

    private final WorldEditGui gui;

    SelectionMenu(WorldEditGui gui) {
        super(Text.DARK_GRAY + "WorldEdit » Selection", 6);
        this.gui = gui;
    }

    static Material icon(SelectionAction action) {
        return switch (action) {
            case WAND -> Material.WOODEN_AXE;
            case POS1 -> Material.LIME_BANNER;
            case POS2 -> Material.RED_BANNER;
            case HPOS1 -> Material.LIME_DYE;
            case HPOS2 -> Material.RED_DYE;
            case SEL_CUBOID -> Material.CHEST;
            case SEL_POLY -> Material.PRISMARINE_SHARD;
            case DESELECT -> Material.BARRIER;
            case EXPAND_VERT -> Material.LADDER;
            case SIZE -> Material.COMPASS;
            case DISTR -> Material.MAP;
            case SET -> Material.GRASS_BLOCK;
            case REPLACE -> Material.SHEARS;
            case WALLS -> Material.STONE_BRICK_WALL;
            case FACES -> Material.GLASS;
            case WIREFRAME -> Material.IRON_BARS;
            case UNDO -> Material.CLOCK;
            case REDO -> Material.RECOVERY_COMPASS;
            case HISTORY -> Material.BOOK;
        };
    }

    @Override
    protected void build(Player viewer) {
        PlayerGuiState state = gui.state(viewer);
        String pattern = state.getPattern().toPattern();
        for (Map.Entry<SelectionAction, Integer> entry : LAYOUT.entrySet()) {
            SelectionAction action = entry.getKey();
            String command = GuiCommands.selection(action, pattern, state.getMask());
            List<String> lore = new ArrayList<>();
            lore.add(Text.GRAY + action.getDescription());
            if (action.usesPattern()) {
                lore.add(Text.GRAY + "Pattern: " + Text.WHITE + pattern);
            }
            if (action == SelectionAction.REPLACE) {
                lore.add(Text.GRAY + "Mask: " + Text.WHITE + (state.getMask() == null ? "any non-air block" : state.getMask()));
            }
            lore.add(Text.DARK_GRAY + command);
            set(entry.getValue(), ItemBuilder.of(icon(action))
                .name(Text.GOLD + action.getDisplayName())
                .lore(lore)
                .build(), (player, _) -> {
                    if (action.closesMenu()) {
                        player.closeInventory();
                    }
                    gui.run(player, command);
                    if (!action.closesMenu()) {
                        refresh(player);
                    }
                });
        }

        set(slot(5, 0), Buttons.back("the main menu", (player, _) -> gui.openMain(player)));
        set(slot(5, 3), Buttons.pattern(state, (player, _) ->
            new PatternMenu(gui, "selection", p -> new SelectionMenu(gui).open(p)).open(player)));
        set(slot(5, 5), Buttons.mask(state, (player, click) -> {
            if (click.isRightClick()) {
                gui.state(player).setMask(null);
                refresh(player);
                return;
            }
            gui.prompts().ask(player, "Type a WorldEdit mask for //replace, e.g. 'grass_block,dirt':", input -> {
                if (GuiCommands.isSingleArgument(input)) {
                    gui.state(player).setMask(input);
                } else {
                    Buttons.sendMessage(player, Text.RED + "Masks cannot contain spaces.");
                }
                open(player);
            }, () -> open(player));
        }));
        set(slot(5, 8), Buttons.close());

        fillBorder(BORDER);
        fillEmpty(BACKGROUND);
    }
}
