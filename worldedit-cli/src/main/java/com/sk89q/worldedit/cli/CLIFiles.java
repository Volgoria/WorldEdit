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

package com.sk89q.worldedit.cli;

import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.internal.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import javax.annotation.Nullable;

/**
 * File handling helpers for the CLI: validation of the input files,
 * format detection and script parsing.
 */
final class CLIFiles {

    private CLIFiles() {
    }

    /**
     * Get the file extensions that may be opened by the CLI, for use in a file chooser.
     *
     * @return the extensions
     */
    static String[] openableExtensions() {
        String[] clipboardExtensions = ClipboardFormats.getFileExtensionArray();
        String[] formats = Arrays.copyOf(clipboardExtensions, clipboardExtensions.length + 1);
        formats[formats.length - 1] = "dat";
        return formats;
    }

    /**
     * Check that the given path is an existing, readable, regular file.
     *
     * @param path the path
     * @param description what the file is, for error messages
     * @throws IllegalArgumentException if it is not
     */
    static void checkReadableFile(Path path, String description) {
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("The " + description + " '" + path + "' does not exist.");
        }
        if (Files.isDirectory(path)) {
            throw new IllegalArgumentException("The " + description + " '" + path + "' is a directory, not a file.");
        }
        if (!Files.isReadable(path)) {
            throw new IllegalArgumentException("The " + description + " '" + path + "' is not readable.");
        }
    }

    /**
     * Validate the given file and detect its clipboard format.
     *
     * @param path the path
     * @return the detected format
     * @throws IllegalArgumentException if the file is missing or not a supported format
     */
    static ClipboardFormat detectFormat(Path path) {
        checkReadableFile(path, "file");
        Path fileName = path.getFileName();
        if (fileName != null && fileName.toString().endsWith("level.dat")) {
            throw new IllegalArgumentException("level.dat file support is unfinished; please provide a schematic file.");
        }
        ClipboardFormat format = ClipboardFormats.findByPath(path);
        if (format == null) {
            throw new IllegalArgumentException("The file '" + path + "' is not in a supported schematic format."
                + " Supported file extensions: " + String.join(", ", new TreeSet<>(Arrays.asList(ClipboardFormats.getFileExtensionArray()))));
        }
        return format;
    }

    /**
     * Read the Minecraft data version of the given schematic.
     *
     * @param format the format of the file
     * @param path the path
     * @return the data version
     * @throws IOException if the file could not be read
     * @throws IllegalArgumentException if the schematic doesn't declare a data version
     */
    static int readDataVersion(ClipboardFormat format, Path path) throws IOException {
        if (format == BuiltInClipboardFormat.MCEDIT_SCHEMATIC) {
            return Constants.DATA_VERSION_MC_1_13_2;
        }
        try (InputStream stream = Files.newInputStream(path);
             ClipboardReader reader = format.getReader(stream)) {
            return reader.getDataVersion()
                .orElseThrow(() -> new IllegalArgumentException("Failed to obtain data version from schematic '" + path + "'."));
        }
    }

    /**
     * Read a script file into a list of commands, skipping blank and comment lines.
     *
     * @param path the script path
     * @return the commands, in order
     * @throws IOException if the file could not be read
     */
    static List<String> readScript(Path path) throws IOException {
        checkReadableFile(path, "script file");
        List<String> commands = new ArrayList<>();
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            String command = normalizeCommand(line);
            if (command != null && !command.startsWith("#")) {
                commands.add(command);
            }
        }
        return commands;
    }

    /**
     * Normalize a line of input into a command, or {@code null} if it's blank.
     *
     * <p>
     * Surrounding whitespace (and a UTF-8 byte order mark) is stripped. Commands are
     * expected to start with a {@code /}, as in game; one is added if missing, so
     * {@code cli selectworld} and {@code /cli selectworld} are equivalent.
     * Comments ({@code #...}) and {@code stop} are returned as-is.
     * </p>
     *
     * @param line the raw line
     * @return the command, or {@code null} if there is nothing to run
     */
    @Nullable
    static String normalizeCommand(String line) {
        String command = line.strip();
        if (command.startsWith("﻿")) {
            command = command.substring(1).strip();
        }
        if (command.isEmpty() || command.equals("/")) {
            return null;
        }
        if (command.startsWith("#") || isStopCommand(command)) {
            return command;
        }
        if (!command.startsWith("/")) {
            command = "/" + command;
        }
        return command;
    }

    /**
     * Check whether the given (normalized) line is the command to stop the CLI.
     *
     * @param command the command
     * @return true if it stops the CLI
     */
    static boolean isStopCommand(String command) {
        return command.equals("stop") || command.equals("/stop");
    }
}
