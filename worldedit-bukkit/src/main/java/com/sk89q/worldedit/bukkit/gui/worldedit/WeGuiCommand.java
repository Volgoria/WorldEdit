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

import com.sk89q.worldedit.bukkit.gui.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/**
 * {@code /wegui [brushes|blocks|schematics|selection|generation]}, alias {@code //menu}.
 */
final class WeGuiCommand implements TabExecutor {

    private static final List<String> SECTIONS = List.of("brushes", "blocks", "schematics", "selection", "generation");

    private final WorldEditGui gui;

    WeGuiCommand(WorldEditGui gui) {
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Text.PREFIX + Text.RED + "Only players can open the WorldEdit menu.");
            return true;
        }
        if (!player.hasPermission(WorldEditGui.PERMISSION)) {
            player.sendMessage(Text.PREFIX + Text.RED + "You are not permitted to use the WorldEdit menu.");
            return true;
        }
        String section = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "";
        switch (section) {
            case "" -> gui.openMain(player);
            case "brushes", "brush" -> new BrushMenu(gui).open(player);
            case "blocks", "patterns", "pattern" -> new PatternMenu(gui, "the main menu", gui::openMain).open(player);
            case "schematics", "schematic", "schem" -> new SchematicMenu(gui).open(player);
            case "selection", "region" -> new SelectionMenu(gui).open(player);
            case "generation", "generate", "gen" -> new GenerationMenu(gui).open(player);
            default -> {
                player.sendMessage(Text.PREFIX + Text.RED + "Unknown section. Use one of: "
                    + String.join(", ", SECTIONS));
                return true;
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        return SECTIONS.stream().filter(s -> s.startsWith(prefix)).toList();
    }
}
