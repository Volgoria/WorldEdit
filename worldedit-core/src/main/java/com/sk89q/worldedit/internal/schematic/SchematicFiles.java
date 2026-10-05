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

package com.sk89q.worldedit.internal.schematic;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;
import com.sk89q.worldedit.util.io.file.FilenameException;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import javax.annotation.Nullable;

/**
 * Helpers for the schematic file management commands. Not public API.
 */
public final class SchematicFiles {

    /**
     * Get the lower case extension of a file name, without the dot.
     *
     * @param path the path
     * @return the extension, or an empty string if there is none
     */
    public static String getExtension(Path path) {
        Path fileName = path.getFileName();
        if (fileName == null) {
            return "";
        }
        String name = fileName.toString();
        int dot = name.lastIndexOf('.');
        return dot <= 0 || dot == name.length() - 1 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * Get a file next to the given one, with the same base name and another extension.
     *
     * @param file the file
     * @param extension the new extension, without the dot
     * @return the sibling file
     */
    public static File withExtension(File file, String extension) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        String baseName = dot > 0 ? name.substring(0, dot) : name;
        return new File(file.getParentFile(), baseName + "." + extension);
    }

    /**
     * Check whether saving with the given format also writes a {@code .mtl} material
     * library next to the saved file.
     *
     * @param format the format
     * @return true for the Wavefront OBJ format
     */
    public static boolean writesMaterialLibrary(ClipboardFormat format) {
        return format == BuiltInClipboardFormat.WAVEFRONT_OBJ;
    }

    /**
     * Resolve the destination of a rename or copy inside the schematics folder.
     *
     * <p>The destination keeps the extension of the source file (it is appended if
     * missing or different), so the file keeps matching its content. The usual WorldEdit
     * file name checks apply, which reject invalid characters and paths that escape
     * the schematics folder.</p>
     *
     * @param worldEdit the WorldEdit instance
     * @param actor the actor, or null
     * @param root the schematics folder
     * @param source the source file
     * @param targetName the user supplied destination name
     * @return the destination file
     * @throws FilenameException if the destination is not acceptable
     */
    public static File resolveDestination(WorldEdit worldEdit, @Nullable Actor actor, File root,
                                          File source, String targetName) throws FilenameException {
        String extension = getExtension(source.toPath());
        if (targetName.equals("#")) {
            // Never open a file dialog for a destination
            throw new FilenameException(targetName,
                TranslatableComponent.of("worldedit.error.invalid-filename.invalid-characters"));
        }
        return worldEdit.getSafeSaveFile(actor, root, targetName, extension.isEmpty() ? null : extension);
    }

    /**
     * Move or copy a schematic file, creating parent folders as needed.
     *
     * @param source the source file
     * @param destination the destination file
     * @param move true to move, false to copy
     * @param overwrite whether an existing destination may be replaced
     * @throws IOException on I/O error, or if the destination exists and may not be replaced,
     *     or if source and destination are the same file
     */
    public static void transfer(Path source, Path destination, boolean move, boolean overwrite) throws IOException {
        Path normalizedSource = source.toAbsolutePath().normalize();
        Path normalizedDestination = destination.toAbsolutePath().normalize();
        if (normalizedSource.equals(normalizedDestination)) {
            throw new IOException("Source and destination are the same file");
        }
        if (!Files.isRegularFile(normalizedSource)) {
            throw new IOException("Source is not a file: " + source.getFileName());
        }
        if (!overwrite && Files.exists(normalizedDestination)) {
            throw new FileAlreadyExistsException(destination.getFileName().toString());
        }
        Path parent = normalizedDestination.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        CopyOption[] options = overwrite
            ? new CopyOption[] { StandardCopyOption.REPLACE_EXISTING }
            : new CopyOption[0];
        if (move) {
            Files.move(normalizedSource, normalizedDestination, options);
        } else {
            Files.copy(normalizedSource, normalizedDestination, options);
        }
    }

    /**
     * Check whether a schematic file matches the list filters.
     *
     * @param root the schematics folder
     * @param file the file
     * @param nameFilter case-insensitive substring of the path relative to the root, or null/empty for any
     * @param format the format whose file extensions the file must have, or null for any
     * @return true if the file matches
     */
    public static boolean matchesFilter(Path root, Path file, @Nullable String nameFilter, @Nullable ClipboardFormat format) {
        if (nameFilter != null && !nameFilter.isEmpty()) {
            String relative = root.relativize(file).toString().toLowerCase(Locale.ROOT);
            if (!relative.contains(nameFilter.toLowerCase(Locale.ROOT))) {
                return false;
            }
        }
        if (format != null) {
            String extension = getExtension(file);
            return format.getFileExtensions().stream().anyMatch(ext -> ext.equalsIgnoreCase(extension));
        }
        return true;
    }

    /**
     * Summary statistics of a clipboard, used by {@code //schem info}.
     *
     * @param size the dimensions of the clipboard
     * @param volume the number of positions in the region
     * @param nonAirBlocks the number of blocks that are not air
     * @param blockEntities the number of blocks with NBT data
     * @param entities the number of entities
     */
    public record Summary(BlockVector3 size, long volume, long nonAirBlocks, long blockEntities, int entities) {
    }

    /**
     * Compute summary statistics of a clipboard.
     *
     * @param clipboard the clipboard
     * @return the summary
     */
    public static Summary summarize(Clipboard clipboard) {
        Region region = clipboard.getRegion();
        long nonAir = 0;
        long blockEntities = 0;
        for (BlockVector3 point : region) {
            BaseBlock block = clipboard.getFullBlock(point);
            if (!isAir(block.getBlockType())) {
                nonAir++;
            }
            if (block.getNbtReference() != null) {
                blockEntities++;
            }
        }
        return new Summary(
            BlockVector3.at(region.getWidth(), region.getHeight(), region.getLength()),
            region.getVolume(),
            nonAir,
            blockEntities,
            clipboard.getEntities().size()
        );
    }

    private static boolean isAir(BlockType type) {
        // Compare by id, block type instances may differ between registries
        return type.equals(BlockTypes.AIR) || type.equals(BlockTypes.CAVE_AIR) || type.equals(BlockTypes.VOID_AIR);
    }

    private SchematicFiles() {
    }
}
