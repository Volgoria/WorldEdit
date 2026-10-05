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

import java.util.regex.Pattern;
import javax.annotation.Nullable;

/**
 * Builds the WorldEdit command lines run by the GUI.
 *
 * <p>Every returned string starts with a slash and is in the form expected by
 * {@link com.sk89q.worldedit.event.platform.CommandEvent}, e.g.
 * {@code "/brush sphere stone 5"} or {@code "//set stone"}.</p>
 */
public final class GuiCommands {

    /**
     * Hard upper bound for any size picked in the GUI, used when WorldEdit's
     * own configuration does not impose a limit.
     */
    public static final int HARD_MAX_SIZE = 100;

    private static final Pattern SCHEMATIC_NAME = Pattern.compile("[A-Za-z0-9_\\-]+(/[A-Za-z0-9_\\-]+)*");

    private GuiCommands() {
    }

    /**
     * Build a {@code /brush} command.
     *
     * @param type the brush type
     * @param pattern the pattern
     * @param size the brush size
     * @param hollow whether to request a hollow brush (ignored if unsupported)
     * @return the command
     */
    public static String brush(BrushType type, String pattern, int size, boolean hollow) {
        String args = CommandTemplate.expand(type.getArgumentTemplate(), pattern, null, size,
            hollow && type.supportsHollow());
        return "/brush " + type.getSubCommand() + " " + args;
    }

    /**
     * Build the command that unbinds the brush from the held item.
     *
     * @return the command
     */
    public static String unbindBrush() {
        return "/brush none";
    }

    /**
     * Build a generation command such as {@code //sphere}.
     *
     * @param shape the shape
     * @param pattern the pattern
     * @param size the size
     * @param hollow whether to request a hollow shape (ignored if unsupported)
     * @return the command
     */
    public static String generate(GenerationShape shape, String pattern, int size, boolean hollow) {
        return CommandTemplate.expand(shape.getCommandTemplate(), pattern, null, size,
            hollow && shape.supportsHollow());
    }

    /**
     * Build a selection / region command.
     *
     * @param action the action
     * @param pattern the pattern
     * @param mask the mask for {@code //replace}, or null to replace any non-air block
     * @return the command
     */
    public static String selection(SelectionAction action, String pattern, @Nullable String mask) {
        return CommandTemplate.expand(action.getCommandTemplate(), pattern, mask, 0, false);
    }

    /**
     * Build a {@code /schem load} command.
     *
     * @param relativePath the path relative to the schematics folder, using {@code /}
     * @return the command
     */
    public static String schematicLoad(String relativePath) {
        return "/schem load " + quote(relativePath);
    }

    /**
     * Build a {@code /schem save} command.
     *
     * @param name the file name, see {@link #isValidSchematicName(String)}
     * @param overwrite whether to overwrite an existing file
     * @return the command
     */
    public static String schematicSave(String name, boolean overwrite) {
        return "/schem save " + (overwrite ? "-f " : "") + quote(name);
    }

    /**
     * Quote a command argument if it contains whitespace or quotes.
     *
     * @param argument the argument
     * @return the argument, quoted if required
     */
    public static String quote(String argument) {
        if (argument.isEmpty() || argument.chars().anyMatch(c -> Character.isWhitespace(c) || c == '"')) {
            return '"' + argument.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
        }
        return argument;
    }

    /**
     * Check whether a name typed in chat is acceptable for saving a schematic.
     *
     * <p>Only letters, digits, {@code _} and {@code -} are allowed, optionally
     * in sub-folders separated by {@code /}. This rules out path traversal,
     * spaces and extensions.</p>
     *
     * @param name the name
     * @return true if acceptable
     */
    public static boolean isValidSchematicName(String name) {
        return name.length() <= 64 && SCHEMATIC_NAME.matcher(name).matches();
    }

    /**
     * Check whether a pattern or mask typed in chat looks usable as a single
     * command argument. WorldEdit itself performs the real validation.
     *
     * @param input the input
     * @return true if it is non-empty and contains no whitespace
     */
    public static boolean isSingleArgument(String input) {
        return !input.isEmpty() && input.length() <= 256 && input.chars().noneMatch(Character::isWhitespace);
    }

    /**
     * Clamp a size into {@code [1, max]}.
     *
     * @param size the requested size
     * @param configuredMax the limit from WorldEdit's configuration; zero or
     *     negative means unlimited, in which case {@link #HARD_MAX_SIZE} applies
     * @return the clamped size
     */
    public static int clampSize(int size, int configuredMax) {
        int max = configuredMax > 0 ? Math.min(configuredMax, HARD_MAX_SIZE) : HARD_MAX_SIZE;
        return Math.max(1, Math.min(size, max));
    }
}
