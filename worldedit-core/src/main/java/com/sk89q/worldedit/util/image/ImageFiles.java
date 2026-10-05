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

package com.sk89q.worldedit.util.image;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.util.io.file.FilenameException;

import java.io.File;
import java.nio.file.Path;

/**
 * Resolves user-supplied image file names inside WorldEdit's image folder.
 *
 * <p>All resolution goes through {@link WorldEdit#getSafeOpenFile} and
 * {@link WorldEdit#getSafeSaveFile}, which reject invalid names and any
 * path escaping the folder.</p>
 */
public final class ImageFiles {

    /**
     * The name of the image folder, relative to WorldEdit's working directory.
     */
    public static final String DIRECTORY = "images";

    private ImageFiles() {
    }

    /**
     * Get the image folder.
     *
     * @param worldEdit the WorldEdit instance
     * @return the folder, which may not exist yet
     */
    public static Path getDirectory(WorldEdit worldEdit) {
        return worldEdit.getWorkingDirectoryPath(DIRECTORY);
    }

    /**
     * Resolve an image to read.
     *
     * @param worldEdit the WorldEdit instance
     * @param actor the actor
     * @param directory the folder to resolve in
     * @param filename the user-supplied name
     * @return the file, which may not exist
     * @throws FilenameException if the name is invalid or leaves the folder
     */
    public static File resolveOpen(WorldEdit worldEdit, Actor actor, File directory, String filename)
            throws FilenameException {
        return worldEdit.getSafeOpenFile(actor, directory, filename, "png",
            Images.READ_EXTENSIONS.toArray(new String[0]));
    }

    /**
     * Resolve a PNG image to write. A {@code .png} extension is added if missing.
     *
     * @param worldEdit the WorldEdit instance
     * @param actor the actor
     * @param directory the folder to resolve in
     * @param filename the user-supplied name
     * @return the file
     * @throws FilenameException if the name is invalid or leaves the folder
     */
    public static File resolveSave(WorldEdit worldEdit, Actor actor, File directory, String filename)
            throws FilenameException {
        return worldEdit.getSafeSaveFile(actor, directory, filename, "png");
    }
}
