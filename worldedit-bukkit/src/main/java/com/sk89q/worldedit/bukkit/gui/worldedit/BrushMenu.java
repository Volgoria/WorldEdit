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
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;

/**
 * Picks a brush, its size and its pattern, then binds it to the held item.
 */
final class BrushMenu extends Menu {

    private static final int[] BRUSH_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
    };

    private final WorldEditGui gui;

    BrushMenu(WorldEditGui gui) {
        super(Text.DARK_GRAY + "WorldEdit » Brushes", 6);
        this.gui = gui;
    }

    static Material icon(BrushType type) {
        return switch (type) {
            case SPHERE -> Material.SLIME_BALL;
            case CYLINDER -> Material.CAULDRON;
            case SMOOTH -> Material.FEATHER;
            case BLOB -> Material.MAGMA_CREAM;
            case SPLATTER -> Material.INK_SAC;
            case OVERLAY -> Material.MOSS_CARPET;
            case FILL -> Material.BUCKET;
            case DRAIN -> Material.SPONGE;
            case LINE -> Material.STRING;
            case RAISE -> Material.PISTON;
            case LOWER -> Material.STICKY_PISTON;
            case ERODE -> Material.GRAVEL;
            case DILATE -> Material.CLAY_BALL;
            case GRAVITY -> Material.SAND;
            case SNOW -> Material.SNOWBALL;
            case EXTINGUISH -> Material.WATER_BUCKET;
            case FOREST -> Material.OAK_SAPLING;
            case BUTCHER -> Material.IRON_SWORD;
            case CLIPBOARD -> Material.PAPER;
        };
    }

    private int maxSize() {
        return gui.config().maxBrushRadius;
    }

    @Override
    protected void build(Player viewer) {
        PlayerGuiState state = gui.state(viewer);
        int size = GuiCommands.clampSize(state.getBrushSize(), maxSize());
        state.setBrushSize(size);
        String pattern = state.getPattern().toPattern();

        BrushType[] types = BrushType.values();
        for (int i = 0; i < types.length && i < BRUSH_SLOTS.length; i++) {
            BrushType type = types[i];
            String command = GuiCommands.brush(type, pattern, size, state.isBrushHollow());
            List<String> lore = new ArrayList<>();
            lore.add(Text.GRAY + type.getDescription());
            lore.add("");
            if (type.usesPattern()) {
                lore.add(Text.GRAY + "Uses your pattern: " + Text.WHITE + pattern);
            }
            if (type.supportsHollow()) {
                lore.add(Text.GRAY + "Hollow: " + Text.WHITE + (state.isBrushHollow() ? "yes" : "no"));
            }
            lore.add(Text.DARK_GRAY + command);
            lore.add("");
            lore.add(Text.YELLOW + "Click to bind to your held item");
            set(BRUSH_SLOTS[i], ItemBuilder.of(icon(type))
                .name(Text.GOLD + type.getDisplayName() + " brush")
                .lore(lore)
                .build(), (player, _) -> bind(player, command));
        }

        // Size controls
        Runnable refresh = () -> refresh(viewer);
        set(slot(4, 1), Buttons.sizeStep(-5, size, maxSize(), state::setBrushSize, refresh));
        set(slot(4, 2), Buttons.sizeStep(-1, size, maxSize(), state::setBrushSize, refresh));
        set(slot(4, 4), Buttons.sizeDisplay(size, maxSize(), "Brush size"));
        set(slot(4, 6), Buttons.sizeStep(1, size, maxSize(), state::setBrushSize, refresh));
        set(slot(4, 7), Buttons.sizeStep(5, size, maxSize(), state::setBrushSize, refresh));

        // Bottom row
        set(slot(5, 0), Buttons.back("the main menu", (player, _) -> gui.openMain(player)));
        set(slot(5, 2), Buttons.pattern(state, (player, _) ->
            new PatternMenu(gui, "the brushes", p -> new BrushMenu(gui).open(p)).open(player)));
        set(slot(5, 4), ItemBuilder.of(Material.BRUSH)
            .name(Text.RED + "Unbind brush")
            .lore(Text.GRAY + "Remove the brush from your held item", Text.DARK_GRAY + GuiCommands.unbindBrush())
            .build(), (player, _) -> {
                gui.run(player, GuiCommands.unbindBrush());
                player.closeInventory();
            });
        set(slot(5, 6), Buttons.toggle("Hollow", state.isBrushHollow(),
            "Hollow spheres, cylinders and lines",
            () -> state.setBrushHollow(!state.isBrushHollow()), refresh));
        set(slot(5, 8), Buttons.close());

        fillBorder(BORDER);
        fillEmpty(BACKGROUND);
    }

    private void bind(Player player, String command) {
        if (!ensureBindableItem(player)) {
            return;
        }
        player.closeInventory();
        gui.run(player, command);
    }

    /**
     * WorldEdit cannot bind tools to air or to blocks. Creative players get a
     * blaze rod in a free hotbar slot; others are asked to hold an item.
     *
     * @param player the player
     * @return true if the held item can take a brush
     */
    private boolean ensureBindableItem(Player player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack held = inventory.getItemInMainHand();
        if (!held.getType().isAir() && !held.getType().isBlock()) {
            return true;
        }
        if (player.getGameMode() != GameMode.CREATIVE) {
            Buttons.sendMessage(player, Text.RED + "Hold a non-block item (e.g. a stick) to bind a brush to it.");
            return false;
        }
        int target = held.getType().isAir() ? inventory.getHeldItemSlot() : -1;
        for (int i = 0; i < 9 && target < 0; i++) {
            ItemStack item = inventory.getItem(i);
            if (item == null || item.getType().isAir()) {
                target = i;
            }
        }
        if (target < 0) {
            Buttons.sendMessage(player, Text.RED + "Hold a non-block item, or free a hotbar slot for a brush.");
            return false;
        }
        inventory.setItem(target, ItemBuilder.of(Material.BLAZE_ROD)
            .name(Text.LIGHT_PURPLE + "WorldEdit Brush")
            .lore(Text.GRAY + "Right-click to use the bound brush")
            .build());
        inventory.setHeldItemSlot(target);
        return true;
    }
}
