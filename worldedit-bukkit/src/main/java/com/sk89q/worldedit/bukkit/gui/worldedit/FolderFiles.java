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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Lists the files shown in the file browsers (schematics and images).
 *
 * <p>Only regular files are listed: symbolic links are not followed, so a
 * listing never leads outside the folder. Files whose path could not be
 * passed safely as a command argument are skipped.</p>
 */
public final class FolderFiles {

    /**
     * How deep sub-folders are searched.
     */
    public static final int MAX_DEPTH = 4;

    /**
     * The maximum number of files listed.
     */
    public static final int MAX_FILES = 2000;

    /**
     * A listed file.
     *
     * @param relativePath the path relative to the listed folder, using {@code /}
     * @param size the file size in bytes
     * @param lastModified the last modification time in epoch millis
     */
    public record Entry(String relativePath, long size, long lastModified) {

        /**
         * Get the file name without folders.
         *
         * @return the file name
         */
        public String fileName() {
            int slash = relativePath.lastIndexOf('/');
            return slash >= 0 ? relativePath.substring(slash + 1) : relativePath;
        }

        /**
         * Get the file extension, lower case, without the dot.
         *
         * @return the extension, or an empty string
         */
        public String extension() {
            return FolderFiles.extension(fileName());
        }
    }

    private FolderFiles() {
    }

    /**
     * List files below a folder, sorted by path.
     *
     * @param root the folder
     * @param extensions accepted extensions, without dots (case-insensitive)
     * @return the files, empty if the folder does not exist
     * @throws IOException if the folder cannot be read
     */
    public static List<Entry> list(Path root, Collection<String> extensions) throws IOException {
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        Set<String> accepted = extensions.stream()
            .map(e -> e.toLowerCase(Locale.ROOT))
            .collect(Collectors.toSet());
        List<Entry> entries = new ArrayList<>();
        try (Stream<Path> stream = Files.find(root, MAX_DEPTH,
            (path, attributes) -> attributes.isRegularFile()
                && accepted.contains(extension(path.getFileName().toString())))) {
            stream.map(path -> toEntry(root, path))
                .filter(entry -> isSafeRelativePath(entry.relativePath()))
                .limit(MAX_FILES)
                .forEach(entries::add);
        }
        entries.sort(Comparator.comparing(e -> e.relativePath().toLowerCase(Locale.ROOT)));
        return entries;
    }

    /**
     * Filter entries whose path contains the query, ignoring case.
     *
     * @param entries the entries
     * @param query the query, blank to keep everything
     * @return the matching entries
     */
    public static List<Entry> filter(List<Entry> entries, String query) {
        if (query == null || query.isBlank()) {
            return entries;
        }
        String needle = query.trim().toLowerCase(Locale.ROOT);
        return entries.stream()
            .filter(e -> e.relativePath().toLowerCase(Locale.ROOT).contains(needle))
            .toList();
    }

    /**
     * Check whether a listed path can be shown and passed to a command.
     *
     * @param relativePath the path relative to the listed folder, using {@code /}
     * @return false for empty, absolute or parent paths, hidden files, and
     *     paths with quotes, backslashes, colour codes or control characters
     */
    public static boolean isSafeRelativePath(String relativePath) {
        if (relativePath.isEmpty() || relativePath.length() > 255 || relativePath.startsWith("/")) {
            return false;
        }
        for (String segment : relativePath.split("/", -1)) {
            if (segment.isEmpty() || segment.startsWith(".")) {
                return false;
            }
        }
        return relativePath.chars().noneMatch(c -> c == '"' || c == '\\' || c == '§' || Character.isISOControl(c));
    }

    static String extension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
    }

    private static Entry toEntry(Path root, Path file) {
        String relative = root.relativize(file).toString().replace(file.getFileSystem().getSeparator(), "/");
        long size = 0;
        long modified = 0;
        try {
            BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
            size = attributes.size();
            modified = attributes.lastModifiedTime().toMillis();
        } catch (IOException _) {
            // Show the file anyway, without metadata.
        }
        return new Entry(relative, size, modified);
    }
}
