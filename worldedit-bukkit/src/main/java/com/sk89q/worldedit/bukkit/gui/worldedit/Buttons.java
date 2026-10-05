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
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.IntConsumer;

/**
 * Buttons shared by several WorldEdit menus.
 */
final class Buttons {

    private Buttons() {
    }

    static Button back(String target, Button.ClickHandler handler) {
        return new Button(ItemBuilder.of(Material.ARROW)
            .name(Text.YELLOW + "Back")
            .lore(Text.GRAY + "Return to " + target)
            .build(), handler);
    }

    static Button close() {
        return new Button(ItemBuilder.of(Material.BARRIER)
            .name(Text.RED + "Close")
            .build(), (player, _) -> player.closeInventory());
    }

    /**
     * Get an icon for the player's current pattern.
     *
     * @param state the state
     * @return a material representing the pattern
     */
    static Material patternIcon(PlayerGuiState state) {
        List<String> blocks = state.getPattern().getBlocks();
        if (blocks.isEmpty()) {
            return Material.PAINTING;
        }
        Material material = Material.matchMaterial(blocks.get(0));
        return material != null && material.isItem() && !material.isAir() ? material : Material.PAINTING;
    }

    static Button pattern(PlayerGuiState state, Button.ClickHandler handler) {
        return new Button(ItemBuilder.of(patternIcon(state))
            .name(Text.GOLD + "Pattern: " + Text.WHITE + state.getPattern().toPattern())
            .lore(
                Text.GRAY + "The blocks used by brushes,",
                Text.GRAY + "generation and region commands.",
                "",
                Text.YELLOW + "Click to change"
            ).build(), handler);
    }

    static Button mask(PlayerGuiState state, Button.ClickHandler handler) {
        String mask = state.getMask();
        return new Button(ItemBuilder.of(mask == null ? Material.GLASS : Material.TINTED_GLASS)
            .name(Text.GOLD + "Replace mask: " + Text.WHITE + (mask == null ? "any non-air block" : mask))
            .lore(
                Text.GRAY + "Which blocks //replace changes.",
                "",
                Text.YELLOW + "Left-click to type a mask",
                Text.YELLOW + "Right-click to clear"
            ).build(), handler);
    }

    static Button toggle(String name, boolean enabled, String description, Runnable toggle, Runnable refresh) {
        return new Button(ItemBuilder.of(enabled ? Material.LIME_DYE : Material.GRAY_DYE)
            .name((enabled ? Text.GREEN : Text.RED) + name + ": " + (enabled ? "on" : "off"))
            .lore(Text.GRAY + description, "", Text.YELLOW + "Click to toggle")
            .glow(enabled)
            .build(), (_, _) -> {
                toggle.run();
                refresh.run();
            });
    }

    static Button sizeStep(int delta, int current, int max, IntConsumer setter, Runnable refresh) {
        boolean increase = delta > 0;
        return new Button(ItemBuilder.of(increase ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE)
            .name((increase ? Text.GREEN + "+" : Text.RED + "-") + Math.abs(delta))
            .lore(Text.GRAY + "Size: " + current + " -> " + GuiCommands.clampSize(current + delta, max))
            .amount(Math.abs(delta))
            .build(), (_, _) -> {
                setter.accept(GuiCommands.clampSize(current + delta, max));
                refresh.run();
            });
    }

    static Button sizeDisplay(int size, int max, String what) {
        String limit = max > 0 ? Integer.toString(Math.min(max, GuiCommands.HARD_MAX_SIZE))
            : Integer.toString(GuiCommands.HARD_MAX_SIZE);
        return Button.decoration(ItemBuilder.of(Material.SLIME_BALL)
            .name(Text.GOLD + what + ": " + Text.WHITE + size)
            .lore(Text.GRAY + "Maximum: " + limit)
            .amount(size)
            .build());
    }

    static void sendMessage(Player player, String message) {
        player.sendMessage(Text.PREFIX + message);
    }
}
