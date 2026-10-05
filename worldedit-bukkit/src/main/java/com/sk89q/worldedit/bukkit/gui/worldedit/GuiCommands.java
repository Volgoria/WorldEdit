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

import com.google.common.base.Splitter;

import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
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

    /**
     * The longest text accepted from a chat prompt.
     */
    public static final int MAX_TEXT_LENGTH = 200;

    /**
     * The longest pattern or mask accepted from chat or built by a preset.
     */
    public static final int MAX_PATTERN_LENGTH = 256;

    /**
     * The schematic formats offered when saving, the first being the default.
     */
    public static final List<String> SAVE_FORMATS = List.of("sponge", "structure", "obj", "json");

    /**
     * The angles offered to rotate the clipboard.
     */
    public static final List<Integer> ROTATIONS = List.of(90, 180, 270);

    /**
     * The permissions of which one is needed to browse the schematics folder, the same
     * as {@code //schem list}.
     */
    public static final List<String> SCHEMATIC_BROWSE_PERMISSIONS = List.of("worldedit.schematic.list");

    /**
     * The permissions of which one is needed to browse the images folder: those of the
     * commands working with images.
     */
    public static final List<String> IMAGE_BROWSE_PERMISSIONS = List.of(
        "worldedit.image.paste", "worldedit.image.export",
        "worldedit.image.heightmap.import", "worldedit.image.heightmap.export"
    );

    private static final Pattern SCHEMATIC_NAME = Pattern.compile("[A-Za-z0-9_\\-]+(/[A-Za-z0-9_\\-]+)*");
    private static final Pattern FILE_REFERENCE =
        Pattern.compile("[A-Za-z0-9_\\-]+(/[A-Za-z0-9_\\-]+)*(\\.[A-Za-z0-9]{1,10})?");

    private GuiCommands() {
    }

    /**
     * Check whether a player may browse a folder.
     *
     * @param hasPermission checks a permission of the player
     * @param permissions the permissions of which one is needed
     * @return true if the player has one of the permissions
     */
    public static boolean mayBrowse(Predicate<String> hasPermission, List<String> permissions) {
        return permissions.stream().anyMatch(hasPermission);
    }

    // ---- Templated actions ---------------------------------------------------------------------

    /**
     * Build the command of a menu action.
     *
     * @param action the action
     * @param context the player's choices
     * @param input the validated chat answer for {@code {input}}, or null to
     *     show a placeholder (previews only)
     * @return the command
     */
    public static String action(GuiAction action, CommandContext context, @Nullable String input) {
        ActionSpec spec = action.spec();
        return CommandTemplate.expand(spec.template(), context.withHollow(context.hollow() && spec.supportsHollow()),
            input);
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
        return action(type, CommandContext.of(pattern, size, hollow), null);
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
     * Build the command that unbinds the tool from the held item.
     *
     * @return the command
     */
    public static String unbindTool() {
        return "/tool none";
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
        return action(shape, CommandContext.of(pattern, size, hollow), null);
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
        return action(action, new CommandContext(pattern, List.of(), mask, 0, false), null);
    }

    // ---- Schematics ----------------------------------------------------------------------------

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
     * Build a {@code /schem save} command in the default format.
     *
     * @param name the file name, see {@link #isValidSchematicName(String)}
     * @param overwrite whether to overwrite an existing file
     * @return the command
     */
    public static String schematicSave(String name, boolean overwrite) {
        return schematicSave(name, SAVE_FORMATS.get(0), overwrite);
    }

    /**
     * Build a {@code /schem save} command.
     *
     * @param name the file name, see {@link #isValidSchematicName(String)}
     * @param format the format, one of {@link #SAVE_FORMATS}
     * @param overwrite whether to overwrite an existing file
     * @return the command
     */
    public static String schematicSave(String name, String format, boolean overwrite) {
        String command = "/schem save " + (overwrite ? "-f " : "") + quote(name);
        return format.equals(SAVE_FORMATS.get(0)) ? command : command + " " + format;
    }

    /**
     * Get the save format following the given one, wrapping around.
     *
     * @param format the current format
     * @return the next format, or the default if the format is unknown
     */
    public static String nextSaveFormat(String format) {
        int index = SAVE_FORMATS.indexOf(format);
        return SAVE_FORMATS.get((index + 1) % SAVE_FORMATS.size());
    }

    /**
     * Build a {@code /schem info} command.
     *
     * @param relativePath the schematic, relative to the schematics folder
     * @return the command
     */
    public static String schematicInfo(String relativePath) {
        return "/schem info " + quote(relativePath);
    }

    /**
     * Build a {@code /schem rename} command. The extension is kept by WorldEdit.
     *
     * @param relativePath the schematic, relative to the schematics folder
     * @param newName the new name, see {@link #isValidSchematicName(String)}
     * @return the command
     */
    public static String schematicRename(String relativePath, String newName) {
        return "/schem rename " + quote(relativePath) + " " + quote(newName);
    }

    /**
     * Build a {@code /schem copy} command. The extension is kept by WorldEdit.
     *
     * @param relativePath the schematic, relative to the schematics folder
     * @param newName the name of the copy, see {@link #isValidSchematicName(String)}
     * @return the command
     */
    public static String schematicCopy(String relativePath, String newName) {
        return "/schem copy " + quote(relativePath) + " " + quote(newName);
    }

    // ---- Images --------------------------------------------------------------------------------

    /**
     * Build an {@code //image} command.
     *
     * @param relativePath the image, relative to the images folder
     * @param upright build the image upright instead of flat
     * @param dither apply dithering
     * @return the command
     */
    public static String image(String relativePath, boolean upright, boolean dither) {
        return "//image " + (upright ? "-v " : "") + (dither ? "-d " : "") + quote(relativePath);
    }

    /**
     * Build a {@code //topview} command, shading slopes.
     *
     * @param name the image name, see {@link #isValidSchematicName(String)}
     * @return the command
     */
    public static String topView(String name) {
        return "//topview -s " + quote(name);
    }

    /**
     * Build a {@code //heightmap import} command.
     *
     * @param relativePath the image, relative to the images folder
     * @return the command
     */
    public static String heightmapImport(String relativePath) {
        return "//heightmap import " + quote(relativePath);
    }

    /**
     * Build a {@code //heightmap export} command.
     *
     * @param name the image name, see {@link #isValidSchematicName(String)}
     * @return the command
     */
    public static String heightmapExport(String name) {
        return "//heightmap export " + quote(name);
    }

    // ---- Session -------------------------------------------------------------------------------

    /**
     * Build a {@code //gmask} command.
     *
     * @param mask the global mask, or null to clear it
     * @return the command
     */
    public static String globalMask(@Nullable String mask) {
        return mask == null || mask.isBlank() ? "//gmask" : "//gmask " + quote(mask);
    }

    /**
     * Build a {@code /mask} command, which sets the mask of the held brush.
     *
     * @param mask the brush mask, or null to clear it
     * @return the command
     */
    public static String brushMask(@Nullable String mask) {
        return mask == null || mask.isBlank() ? "/mask" : "/mask " + quote(mask);
    }

    /**
     * Build a {@code //limit} command.
     *
     * @param limit the block change limit, -1 for none, or null for the default
     * @return the command
     */
    public static String limit(@Nullable Integer limit) {
        return limit == null ? "//limit" : "//limit " + limit;
    }

    /**
     * Build a {@code //timeout} command.
     *
     * @param timeout the expression timeout in milliseconds, -1 for none, or null for the default
     * @return the command
     */
    public static String timeout(@Nullable Integer timeout) {
        return timeout == null ? "//timeout" : "//timeout " + timeout;
    }

    /**
     * Build a {@code //fast} command.
     *
     * @param enable the new state
     * @return the command
     */
    public static String fast(boolean enable) {
        return "//fast " + (enable ? "on" : "off");
    }

    /**
     * Build a {@code //perf} command toggling one side effect.
     *
     * @param sideEffect the side effect name, e.g. {@code LIGHTING}
     * @param enable the new state
     * @return the command
     */
    public static String sideEffect(String sideEffect, boolean enable) {
        return "//perf " + sideEffect.toLowerCase(Locale.ROOT) + " " + (enable ? "on" : "off");
    }

    /**
     * Build a {@code //rotate} command rotating the clipboard around the Y axis.
     *
     * @param degrees the angle
     * @return the command
     */
    public static String rotate(int degrees) {
        return "//rotate " + degrees;
    }

    /**
     * Build the command that flips the clipboard in the direction the player looks.
     *
     * @return the command
     */
    public static String flip() {
        return "//flip";
    }

    /**
     * Build the command that empties the clipboard.
     *
     * @return the command
     */
    public static String clearClipboard() {
        return "/clearclipboard";
    }

    /**
     * Build the command that copies the selection to the clipboard.
     *
     * @return the command
     */
    public static String copy() {
        return "//copy";
    }

    /**
     * Build the command that pastes the clipboard, skipping air.
     *
     * @return the command
     */
    public static String paste() {
        return "//paste -a";
    }

    /**
     * Build an {@code //undo} or {@code //redo} command.
     *
     * @param redo true for redo
     * @param times how many edits, at least 1
     * @return the command
     */
    public static String history(boolean redo, int times) {
        String command = redo ? "//redo" : "//undo";
        return times > 1 ? command + " " + times : command;
    }

    /**
     * Build the command that forgets the edit history.
     *
     * @return the command
     */
    public static String clearHistory() {
        return "//clearhistory";
    }

    // ---- Validation ----------------------------------------------------------------------------

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
     * Check whether a name typed in chat is acceptable for saving a schematic
     * or an image.
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
     * Check whether a reference to an existing file typed in chat is
     * acceptable: like {@link #isValidSchematicName(String)}, plus an
     * optional extension.
     *
     * @param name the name
     * @return true if acceptable
     */
    public static boolean isValidFileReference(String name) {
        return name.length() <= 80 && FILE_REFERENCE.matcher(name).matches();
    }

    /**
     * Check whether a comma-separated list of files or folders typed in chat
     * is acceptable; {@code #clipboard} may be used as an entry.
     *
     * @param list the list
     * @return true if acceptable
     */
    public static boolean isValidFileList(String list) {
        if (list.isEmpty() || list.length() > 256 || list.startsWith(",") || list.endsWith(",")) {
            return false;
        }
        for (String entry : Splitter.on(',').split(list)) {
            if (!entry.equalsIgnoreCase("#clipboard") && !isValidFileReference(entry)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check whether free text typed in chat can safely be put between quotes
     * as a single command argument.
     *
     * @param text the text
     * @return true if it is not blank, not too long, and contains no quotes,
     *     backslashes, colour codes or control characters
     */
    public static boolean isSafeText(String text) {
        return !text.isBlank() && text.length() <= MAX_TEXT_LENGTH
            && text.chars().noneMatch(c -> c == '"' || c == '\\' || c == '§' || Character.isISOControl(c));
    }

    /**
     * Parse a limit typed in chat.
     *
     * @param input the input
     * @return the limit, at least -1, or null if the input is not a whole number in range
     */
    @Nullable
    public static Integer parseLimit(String input) {
        if (input.isEmpty() || input.length() > 10) {
            return null;
        }
        try {
            int value = Integer.parseInt(input);
            return value >= -1 ? value : null;
        } catch (NumberFormatException _) {
            return null;
        }
    }

    /**
     * Check whether a pattern or mask typed in chat is safe to put in a
     * command. Spaces are allowed (masks are intersected with spaces), since
     * the value is {@linkplain #quote(String) quoted} when the command is
     * built; quotes, backslashes, colour codes and control characters are
     * not. WorldEdit itself performs the real validation.
     *
     * @param input the input, see {@link #normalizePatternOrMask(String)}
     * @return true if it can be used
     */
    public static boolean isValidPatternOrMask(String input) {
        return !input.isBlank() && input.length() <= MAX_PATTERN_LENGTH
            && input.chars().noneMatch(c -> c == '"' || c == '\\' || c == '§' || Character.isISOControl(c)
                || (c != ' ' && (Character.isWhitespace(c) || Character.isSpaceChar(c))));
    }

    /**
     * Trim a pattern or mask typed in chat and collapse runs of spaces, since
     * WorldEdit separates arguments with single spaces.
     *
     * @param input the input
     * @return the normalised value
     */
    public static String normalizePatternOrMask(String input) {
        return input.trim().replaceAll(" {2,}", " ");
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
