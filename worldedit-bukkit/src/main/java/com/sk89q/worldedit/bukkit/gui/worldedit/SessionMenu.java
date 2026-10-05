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

import com.sk89q.worldedit.EmptyClipboardException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.gui.Button;
import com.sk89q.worldedit.bukkit.gui.ItemBuilder;
import com.sk89q.worldedit.bukkit.gui.Menu;
import com.sk89q.worldedit.bukkit.gui.Text;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.SideEffect;
import com.sk89q.worldedit.util.SideEffectSet;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Session settings: limits, fast mode, side effects, global mask, clipboard
 * and history.
 */
final class SessionMenu extends Menu {

    private final WorldEditGui gui;

    SessionMenu(WorldEditGui gui) {
        super(Text.DARK_GRAY + "WorldEdit » Settings", 6);
        this.gui = gui;
    }

    @Override
    protected void build(Player viewer) {
        LocalSession session = gui.session(viewer);
        Consumer<Player> refresh = this::refresh;

        // Row 1: limits and global settings
        int limit = session.getBlockChangeLimit();
        set(slot(1, 1), numberSetting(Material.HOPPER, "Block change limit", limit,
            "The most blocks one edit may change.", "Type the new limit (-1 for none):", GuiCommands::limit));
        int timeout = session.getTimeout();
        set(slot(1, 2), numberSetting(Material.CLOCK, "Expression timeout", timeout,
            "Milliseconds an expression may run.", "Type the new timeout in ms (-1 for none):",
            GuiCommands::timeout));
        boolean fast = fastMode(session);
        set(slot(1, 4), Buttons.toggle("Fast mode", fast, "Skip lighting and neighbour updates",
            () -> gui.run(viewer, GuiCommands.fast(!fast)), () -> refresh(viewer)));
        Mask globalMask = session.getMask();
        set(slot(1, 6), Buttons.command(gui, globalMask == null ? Material.GLASS_BOTTLE : Material.TINTED_GLASS,
            "Global mask: " + Text.WHITE + (globalMask == null ? "none" : "set"),
            GuiCommands.globalMask(null), false, refresh,
            "Limits which blocks every edit may change.", "", "Click to clear it",
            "Set one from the Patterns & Masks menu."));
        set(slot(1, 7), Buttons.command(gui, Material.WRITABLE_BOOK, "All side effects", "//perf -h", true, refresh,
            "Show every side effect in chat."));

        // Row 2: side effects
        SideEffectSet sideEffects = session.getSideEffectSet();
        List<SideEffect> exposed = WorldEdit.getInstance().getPlatformManager().getSupportedSideEffects().stream()
            .filter(SideEffect::isExposed)
            .sorted()
            .limit(7)
            .toList();
        for (int i = 0; i < exposed.size(); i++) {
            SideEffect effect = exposed.get(i);
            boolean on = sideEffects.getState(effect) != SideEffect.State.OFF;
            set(slot(2, 1 + i), Buttons.toggle(Text.humanize(effect.name()), on, "Side effect of your edits (//perf)",
                () -> gui.run(viewer, GuiCommands.sideEffect(effect.name(), !on)), () -> refresh(viewer)));
        }

        // Row 3: clipboard
        set(slot(3, 1), clipboardInfo(session));
        set(slot(3, 2), Buttons.command(gui, Material.PAPER, "Copy", GuiCommands.copy(), false, refresh,
            "Copy the selection to your clipboard."));
        set(slot(3, 3), Buttons.command(gui, Material.SLIME_BALL, "Paste", GuiCommands.paste(), true, refresh,
            "Paste your clipboard where you stand,", "skipping air."));
        for (int i = 0; i < GuiCommands.ROTATIONS.size(); i++) {
            int degrees = GuiCommands.ROTATIONS.get(i);
            set(slot(3, 4 + i), Buttons.command(gui, Material.COMPASS, "Rotate " + degrees + "°",
                GuiCommands.rotate(degrees), false, refresh, "Rotate your clipboard around Y."));
        }
        set(slot(3, 7), Buttons.command(gui, Material.ITEM_FRAME, "Flip", GuiCommands.flip(), false, refresh,
            "Flip your clipboard in the", "direction you look."));

        // Row 4: history
        int undo = session.getHistoryPointer();
        int redo = Math.max(0, session.getHistory().size() - undo);
        set(slot(4, 1), Buttons.command(gui, Material.LAVA_BUCKET, "Clear clipboard", GuiCommands.clearClipboard(),
            false, refresh, "Forget your clipboard."));
        set(slot(4, 3), historyButton(false, undo));
        set(slot(4, 4), Buttons.command(gui, Material.BOOK, "History", "//history", true, refresh,
            undo + " edit(s) to undo, " + redo + " to redo.", "Click to list them in chat."));
        set(slot(4, 5), historyButton(true, redo));
        set(slot(4, 7), Buttons.command(gui, Material.CAULDRON, "Clear history", GuiCommands.clearHistory(), false,
            refresh, "Forget all your edits; they can", "no longer be undone."));

        set(slot(5, 0), Buttons.backToMain(gui));
        set(slot(5, 8), Buttons.close());
        fillBorder(BORDER);
        fillEmpty(BACKGROUND);
    }

    @SuppressWarnings("deprecation") // //fast is still the command players use
    private static boolean fastMode(LocalSession session) {
        return session.hasFastMode();
    }

    private Button numberSetting(Material icon, String name, int current, String description, String question,
                                 Function<Integer, String> command) {
        return new Button(ItemBuilder.of(icon)
            .name(Text.GOLD + name + ": " + Text.WHITE + (current < 0 ? "none" : current))
            .lore(Text.GRAY + description, Text.DARK_GRAY + command.apply(null), "",
                Text.YELLOW + "Left-click to type a value",
                Text.YELLOW + "Right-click to reset to the default")
            .build(), (player, click) -> {
                if (click.isRightClick()) {
                    gui.run(player, command.apply(null));
                    refresh(player);
                    return;
                }
                gui.ask(player, question, InputKind.NUMBER, value -> {
                    gui.run(player, command.apply(Integer.valueOf(value)));
                    open(player);
                }, this::open);
            });
    }

    private Button historyButton(boolean redo, int available) {
        String name = redo ? "Redo" : "Undo";
        return new Button(ItemBuilder.of(redo ? Material.RECOVERY_COMPASS : Material.CLOCK)
            .name((available > 0 ? Text.GOLD : Text.GRAY) + name + " (" + available + " available)")
            .lore(Text.YELLOW + "Click: " + Text.GRAY + name.toLowerCase(Locale.ROOT) + " one edit",
                Text.YELLOW + "Shift-click: " + Text.GRAY + "up to 5 edits")
            .amount(Math.max(1, available))
            .build(), (player, click) -> {
                gui.run(player, GuiCommands.history(redo, click.isShiftClick() ? Math.min(5, available) : 1));
                refresh(player);
            });
    }

    private static Button clipboardInfo(LocalSession session) {
        List<String> lore;
        try {
            ClipboardHolder holder = session.getClipboard();
            Clipboard clipboard = holder.getClipboard();
            BlockVector3 size = clipboard.getDimensions();
            lore = List.of(
                Text.GRAY + "Size: " + Text.WHITE + size.x() + " x " + size.y() + " x " + size.z(),
                Text.GRAY + "Transformed: " + Text.WHITE + (holder.getTransform().isIdentity() ? "no" : "yes")
            );
        } catch (EmptyClipboardException _) {
            lore = List.of(Text.GRAY + "Empty: copy a selection first.");
        }
        return Button.decoration(ItemBuilder.of(Material.CHEST)
            .name(Text.GOLD + "Clipboard")
            .lore(lore)
            .build());
    }
}
