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

import javax.annotation.Nullable;

/**
 * The kinds of values players can type in chat for a GUI action, with how
 * each one is validated and turned into a command argument.
 */
public enum InputKind {
    /**
     * Free text, passed as one quoted argument so it can contain spaces and
     * cannot be mistaken for flags.
     */
    TEXT("Text cannot contain quotes, backslashes or colour codes, and is limited to "
        + GuiCommands.MAX_TEXT_LENGTH + " characters."),
    /**
     * The name of a new file: letters, digits, {@code _} and {@code -},
     * optionally in sub-folders, without an extension.
     */
    FILE_NAME("Invalid name. Use only letters, digits, _ and -, with / for folders."),
    /**
     * An existing file or folder, optionally with an extension.
     */
    FILE("Invalid file name. Use only letters, digits, _, - and one extension, with / for folders."),
    /**
     * Files or folders separated with commas; {@code #clipboard} is accepted.
     */
    FILE_LIST("Invalid list. Give file or folder names separated with ',' (or #clipboard)."),
    /**
     * A whole number, at least -1 (meaning "no limit" for WorldEdit settings).
     */
    NUMBER("Please type a whole number, -1 for no limit."),
    /**
     * A single argument without spaces, such as a pattern or a mask.
     */
    ARGUMENT("This value cannot contain spaces.");

    private final String error;

    InputKind(String error) {
        this.error = error;
    }

    /**
     * Get the message shown when the input is rejected.
     *
     * @return the message
     */
    public String errorMessage() {
        return error;
    }

    /**
     * Validate chat input and turn it into a command argument.
     *
     * @param raw the raw chat input
     * @return the argument, or null if the input is not acceptable
     */
    @Nullable
    public String toArgument(String raw) {
        String input = raw.trim();
        return switch (this) {
            case TEXT -> GuiCommands.isSafeText(input) ? '"' + input + '"' : null;
            case FILE_NAME -> GuiCommands.isValidSchematicName(input) ? input : null;
            case FILE -> GuiCommands.isValidFileReference(input) ? input : null;
            case FILE_LIST -> GuiCommands.isValidFileList(input) ? input : null;
            case NUMBER -> {
                Integer value = GuiCommands.parseLimit(input);
                yield value == null ? null : value.toString();
            }
            case ARGUMENT -> GuiCommands.isSingleArgument(input) ? input : null;
        };
    }
}
