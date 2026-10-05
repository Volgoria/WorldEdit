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
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Ready-made patterns ({@code #gradient}, {@code #stripes}, ...) and masks
 * ({@code #y}, {@code #angle}, {@code #wall}, ...) built from the blocks the
 * player picked.
 */
final class PresetMenu extends Menu {

    private static final int[] PATTERN_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
    private static final int[] MASK_SLOTS = {28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};

    private final WorldEditGui gui;

    PresetMenu(WorldEditGui gui) {
        super(Text.DARK_GRAY + "WorldEdit » Patterns & Masks", 6);
        this.gui = gui;
    }

    private static Presets.Context context(PlayerGuiState state, Player player) {
        Location location = player.getLocation();
        return Presets.Context.of(state.getPattern(), state.getPresetValue(),
            location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    @Override
    protected void build(Player viewer) {
        PlayerGuiState state = gui.state(viewer);
        Presets.Context context = context(state, viewer);

        set(slot(0, 2), Button.decoration(ItemBuilder.of(Material.PAINTING)
            .name(Text.LIGHT_PURPLE + "Pattern presets")
            .lore(Text.GRAY + "Rows 2-3. Click one to use it as", Text.GRAY + "your pattern.")
            .build()));
        set(slot(0, 6), Button.decoration(ItemBuilder.of(Material.TINTED_GLASS)
            .name(Text.LIGHT_PURPLE + "Mask presets")
            .lore(Text.GRAY + "Rows 4-5. Click one to use it as", Text.GRAY + "your //replace mask.")
            .build()));

        Presets.PatternPreset[] patterns = Presets.PatternPreset.values();
        for (int i = 0; i < patterns.length && i < PATTERN_SLOTS.length; i++) {
            Presets.PatternPreset preset = patterns[i];
            set(PATTERN_SLOTS[i], presetButton(preset.getIcon(), preset.getDisplayName(), preset.getDescription(),
                preset.usesValue(), preset.getMinBlocks(), preset.build(context), state, true));
        }
        Presets.MaskPreset[] masks = Presets.MaskPreset.values();
        for (int i = 0; i < masks.length && i < MASK_SLOTS.length; i++) {
            Presets.MaskPreset preset = masks[i];
            set(MASK_SLOTS[i], presetButton(preset.getIcon(), preset.getDisplayName(), preset.getDescription(),
                preset.usesValue(), preset.getMinBlocks(), preset.build(context), state, false));
        }

        Runnable refresh = () -> refresh(viewer);
        int value = state.getPresetValue();
        set(slot(5, 0), Buttons.backToMain(gui));
        set(slot(5, 1), Buttons.patternPicker(gui, state, "the presets", this::open));
        set(slot(5, 2), Buttons.step(false, value, Presets::clampValue, state::setPresetValue, refresh));
        set(slot(5, 3), Buttons.sizeDisplay(value, Presets.MAX_VALUE, "Preset value"));
        set(slot(5, 4), Buttons.step(true, value, Presets::clampValue, state::setPresetValue, refresh));
        set(slot(5, 5), Buttons.mask(gui, state, this::open));
        set(slot(5, 6), Buttons.command(gui, Material.GLASS_BOTTLE, "Clear global mask", GuiCommands.globalMask(null),
            false, this::refresh, "Let edits change every block again"));
        set(slot(5, 8), Buttons.close());

        fillBorder(BORDER);
        fillEmpty(BACKGROUND);
    }

    private Button presetButton(String icon, String name, String description, boolean usesValue, int minBlocks,
                                @Nullable String built, PlayerGuiState state, boolean pattern) {
        List<String> lore = new ArrayList<>();
        lore.add(Text.GRAY + description);
        if (usesValue) {
            lore.add(Text.GRAY + "Value: " + Text.WHITE + state.getPresetValue());
        }
        lore.add("");
        if (built == null) {
            lore.add(Text.RED + "Pick at least " + minBlocks + " blocks first");
            lore.add(Text.GRAY + "(shift-click blocks in the block picker)");
        } else {
            lore.add(Text.DARK_GRAY + built);
            lore.add("");
            if (pattern) {
                lore.add(Text.YELLOW + "Click: " + Text.GRAY + "use as your pattern");
            } else {
                lore.add(Text.YELLOW + "Left-click: " + Text.GRAY + "use as //replace mask");
                lore.add(Text.YELLOW + "Right-click: " + Text.GRAY + "set as global mask (//gmask)");
                lore.add(Text.YELLOW + "Shift-click: " + Text.GRAY + "set as held brush mask (/mask)");
            }
        }
        return new Button(ItemBuilder.of(Buttons.material(icon))
            .name(Text.GOLD + name)
            .lore(lore)
            .glow(built != null && (pattern ? built.equals(state.getPattern().toPattern()) : built.equals(state.getMask())))
            .build(), (player, click) -> {
                if (built == null) {
                    Buttons.sendMessage(player, Text.RED + "Pick at least " + minBlocks
                        + " blocks first: shift-click blocks in the block picker.");
                } else if (pattern) {
                    usePattern(player, built);
                } else {
                    useMask(player, built, click);
                }
            });
    }

    private void usePattern(Player player, String built) {
        if (!GuiCommands.isValidPatternOrMask(built)) {
            Buttons.sendMessage(player, Text.RED + "That pattern is too long.");
            return;
        }
        gui.state(player).getPattern().setCustom(built);
        Buttons.sendMessage(player, "Pattern is now " + Text.WHITE + built);
        refresh(player);
    }

    private void useMask(Player player, String built, ClickType click) {
        if (!GuiCommands.isValidPatternOrMask(built)) {
            Buttons.sendMessage(player, Text.RED + "That mask is too long.");
            return;
        }
        if (click.isShiftClick()) {
            player.closeInventory();
            gui.run(player, GuiCommands.brushMask(built));
        } else if (click.isRightClick()) {
            gui.run(player, GuiCommands.globalMask(built));
            refresh(player);
        } else {
            gui.state(player).setMask(built);
            Buttons.sendMessage(player, "Replace mask is now " + Text.WHITE + built);
            refresh(player);
        }
    }
}
