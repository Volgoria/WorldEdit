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

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntUnaryOperator;

/**
 * Buttons shared by several WorldEdit menus.
 */
final class Buttons {

    private Buttons() {
    }

    /**
     * Resolve an icon by Bukkit material name.
     *
     * @param name the material name, e.g. {@code OAK_SAPLING}
     * @return the material, or paper if it does not exist on this server
     */
    static Material material(String name) {
        Material material = Material.getMaterial(name.toUpperCase(Locale.ROOT));
        return material != null && material.isItem() && !material.isAir() ? material : Material.PAPER;
    }

    static Button back(String target, Button.ClickHandler handler) {
        return new Button(ItemBuilder.of(Material.ARROW)
            .name(Text.YELLOW + "Back")
            .lore(Text.GRAY + "Return to " + target)
            .build(), handler);
    }

    static Button backToMain(WorldEditGui gui) {
        return back("the main menu", (player, _) -> gui.openMain(player));
    }

    static Button close() {
        return new Button(ItemBuilder.of(Material.BARRIER)
            .name(Text.RED + "Close")
            .build(), (player, _) -> player.closeInventory());
    }

    /**
     * Create a button that runs a fixed command.
     *
     * @param gui the GUI
     * @param icon the icon
     * @param name the name
     * @param command the command
     * @param closeMenu whether to close the menu first, to let the player read the output
     * @param after run after the command when the menu stays open, e.g. a refresh
     * @param description the lore lines
     * @return the button
     */
    static Button command(WorldEditGui gui, Material icon, String name, String command, boolean closeMenu,
                          Consumer<Player> after, String... description) {
        return new Button(ItemBuilder.of(icon)
            .name(Text.GOLD + name)
            .lore(gray(description))
            .lore(Text.DARK_GRAY + command)
            .build(), (player, _) -> {
                if (closeMenu) {
                    player.closeInventory();
                }
                gui.run(player, command);
                if (!closeMenu) {
                    after.accept(player);
                }
            });
    }

    private static List<String> gray(String... lines) {
        return Arrays.stream(lines).map(line -> line.isEmpty() ? line : Text.GRAY + line).toList();
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

    /**
     * Create a pattern button that opens the block picker, which returns to
     * the given menu.
     *
     * @param gui the GUI
     * @param state the viewer's state
     * @param backTarget the name of the menu to return to
     * @param reopen reopens that menu
     * @return the button
     */
    static Button patternPicker(WorldEditGui gui, PlayerGuiState state, String backTarget, Consumer<Player> reopen) {
        return pattern(state, (player, _) -> new PatternMenu(gui, backTarget, reopen).open(player));
    }

    /**
     * Create a button showing the replace mask: left-click types a new one in
     * chat, right-click clears it.
     *
     * @param gui the GUI
     * @param state the viewer's state
     * @param reopen reopens the menu afterwards
     * @return the button
     */
    static Button mask(WorldEditGui gui, PlayerGuiState state, Consumer<Player> reopen) {
        String mask = state.getMask();
        return new Button(ItemBuilder.of(mask == null ? Material.GLASS : Material.TINTED_GLASS)
            .name(Text.GOLD + "Replace mask: " + Text.WHITE + (mask == null ? "any non-air block" : mask))
            .lore(
                Text.GRAY + "Which blocks //replace changes.",
                "",
                Text.YELLOW + "Left-click to type a mask",
                Text.YELLOW + "Right-click to clear"
            ).build(), (player, click) -> {
                if (click.isRightClick()) {
                    gui.state(player).setMask(null);
                    reopen.accept(player);
                    return;
                }
                gui.ask(player, "Type a WorldEdit mask, e.g. 'grass_block,dirt':", InputKind.PATTERN_OR_MASK, input -> {
                    gui.state(player).setMask(input);
                    sendMessage(player, "Replace mask is now " + Text.WHITE + input);
                    reopen.accept(player);
                }, reopen);
            });
    }

    /**
     * Create a search button: left-click types a query, right-click clears it.
     *
     * @param gui the GUI
     * @param current the current query
     * @param question the chat question
     * @param setter stores the new query (empty to clear) and refreshes the menu
     * @param reopen reopens the menu if the prompt is cancelled
     * @return the button
     */
    static Button search(WorldEditGui gui, String current, String question, BiConsumer<Player, String> setter,
                         Consumer<Player> reopen) {
        return new Button(ItemBuilder.of(Material.OAK_SIGN)
            .name(Text.GOLD + "Search" + (current.isEmpty() ? "" : ": " + Text.WHITE + current))
            .lore(Text.YELLOW + "Left-click to filter by name", Text.YELLOW + "Right-click to clear the filter")
            .glow(!current.isEmpty())
            .build(), (player, click) -> {
                if (click.isRightClick()) {
                    setter.accept(player, "");
                    return;
                }
                gui.prompts().ask(player, question, input -> setter.accept(player, input), () -> reopen.accept(player));
            });
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

    /**
     * Create a button changing a value by one, or by five when shift-clicked.
     *
     * @param increase true for a plus button
     * @param current the current value
     * @param clamp clamps a new value into range
     * @param setter stores the new value
     * @param refresh refreshes the menu
     * @return the button
     */
    static Button step(boolean increase, int current, IntUnaryOperator clamp, IntConsumer setter, Runnable refresh) {
        int sign = increase ? 1 : -1;
        int one = clamp.applyAsInt(current + sign);
        int five = clamp.applyAsInt(current + 5 * sign);
        return new Button(ItemBuilder.of(increase ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE)
            .name(increase ? Text.GREEN + "+1" : Text.RED + "-1")
            .lore(Text.GRAY + "Click: " + current + " -> " + one, Text.GRAY + "Shift-click: " + current + " -> " + five)
            .build(), (_, click) -> {
                setter.accept(click.isShiftClick() ? five : one);
                refresh.run();
            });
    }

    static Button sizeDisplay(int size, int max, String what) {
        return Button.decoration(ItemBuilder.of(Material.SLIME_BALL)
            .name(Text.GOLD + what + ": " + Text.WHITE + size)
            .lore(Text.GRAY + "Maximum: " + max)
            .amount(size)
            .build());
    }

    /**
     * Get the largest size allowed by a WorldEdit limit.
     *
     * @param configuredMax the limit from WorldEdit's configuration
     * @return the effective maximum
     */
    static int effectiveMax(int configuredMax) {
        return GuiCommands.clampSize(Integer.MAX_VALUE, configuredMax);
    }

    static void sendMessage(Player player, String message) {
        player.sendMessage(Text.PREFIX + message);
    }
}
