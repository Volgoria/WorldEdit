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
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * A paginated, searchable block picker that edits the player's pattern and mask.
 */
final class PatternMenu extends PaginatedMenu<Material> {

    private final WorldEditGui gui;
    private final String backTarget;
    private final Consumer<Player> back;

    /**
     * Create the block picker.
     *
     * @param gui the GUI
     * @param backTarget the name of the menu the back button returns to
     * @param back opens that menu
     */
    PatternMenu(WorldEditGui gui, String backTarget, Consumer<Player> back) {
        super(Text.DARK_GRAY + "WorldEdit » Blocks");
        this.gui = gui;
        this.backTarget = backTarget;
        this.back = back;
    }

    /**
     * Get the block id WorldEdit uses for a material.
     *
     * @param material the material
     * @return the id, e.g. {@code oak_planks}
     */
    static String blockId(Material material) {
        return material.name().toLowerCase(Locale.ROOT);
    }

    @Override
    protected List<Material> entries(Player viewer) {
        String search = gui.state(viewer).getBlockSearch().toLowerCase(Locale.ROOT).replace(' ', '_');
        List<Material> all = gui.blockMaterials();
        if (search.isEmpty()) {
            return all;
        }
        return all.stream().filter(m -> blockId(m).contains(search)).toList();
    }

    @Override
    protected String emptyMessage() {
        return "No block matches your search";
    }

    @Override
    protected Button entryButton(Player viewer, Material material) {
        PlayerGuiState state = gui.state(viewer);
        String id = blockId(material);
        boolean selected = state.getPattern().getBlocks().contains(id);
        return new Button(ItemBuilder.of(material)
            .name((selected ? Text.GREEN : Text.WHITE) + Text.humanize(id))
            .lore(
                Text.DARK_GRAY + id,
                "",
                Text.YELLOW + "Left-click: " + Text.GRAY + "use only this block",
                Text.YELLOW + "Shift-click: " + Text.GRAY + "add to a random mix",
                Text.YELLOW + "Right-click: " + Text.GRAY + "use as //replace mask"
            )
            .glow(selected)
            .build(), (player, click) -> pick(player, id, click));
    }

    private void pick(Player player, String id, ClickType click) {
        PlayerGuiState state = gui.state(player);
        if (click.isShiftClick()) {
            if (state.getPattern().add(id)) {
                Buttons.sendMessage(player, "Pattern is now " + Text.WHITE + state.getPattern().toPattern());
            } else {
                Buttons.sendMessage(player, Text.RED + "That block is already in the mix, or the mix is full ("
                    + PatternSelection.MAX_MIX + " blocks).");
            }
        } else if (click.isRightClick()) {
            state.setMask(id);
            Buttons.sendMessage(player, "Replace mask is now " + Text.WHITE + id);
        } else {
            state.getPattern().set(id);
            Buttons.sendMessage(player, "Pattern is now " + Text.WHITE + id);
        }
        refresh(player);
    }

    @Override
    protected void buildControls(Player viewer) {
        PlayerGuiState state = gui.state(viewer);
        set(slot(CONTROL_ROW, 1), Buttons.back(backTarget, (player, _) -> back.accept(player)));
        set(slot(CONTROL_ROW, 2), Buttons.search(gui, state.getBlockSearch(), "Type part of a block name, e.g. 'planks':",
            (player, query) -> {
                gui.state(player).setBlockSearch(query);
                setPage(0);
                open(player);
            }, this::open));
        set(slot(CONTROL_ROW, 3), Buttons.pattern(state, (player, _) -> {
            gui.state(player).getPattern().reset();
            Buttons.sendMessage(player, "Pattern reset to " + PatternSelection.DEFAULT_BLOCK + ".");
            refresh(player);
        }));
        set(slot(CONTROL_ROW, 5), ItemBuilder.of(Material.WRITABLE_BOOK)
            .name(Text.GOLD + "Type a pattern")
            .lore(Text.GRAY + "Any WorldEdit pattern, e.g.",
                Text.WHITE + "50%stone,50%andesite" + Text.GRAY + " or " + Text.WHITE + "##wool",
                Text.GRAY + "Presets: see Patterns & Masks.",
                "", Text.YELLOW + "Click to type it in chat")
            .build(), (player, _) -> gui.ask(player, "Type a WorldEdit pattern:", InputKind.PATTERN_OR_MASK, input -> {
                gui.state(player).getPattern().setCustom(input);
                Buttons.sendMessage(player, "Pattern is now " + Text.WHITE + input);
                open(player);
            }, this::open));
        set(slot(CONTROL_ROW, 6), Buttons.mask(gui, state, this::open));
        set(slot(CONTROL_ROW, 7), Buttons.close());
    }
}
