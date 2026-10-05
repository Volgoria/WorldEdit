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

import java.io.IOException;

public interface CLIWorld {

    /**
     * Saves this world back to file if dirty or forced.
     *
     * <p>A world that could not be saved stays dirty.</p>
     *
     * @param force Force a save
     * @throws IOException if the world could not be saved, including when its
     *     file format can only be read
     */
    void save(boolean force) throws IOException;

    /**
     * Gets whether the world is dirty.
     *
     * @return If it's dirty
     */
    boolean isDirty();

    /**
     * Set the world's dirty status.
     *
     * @param dirty if dirty
     */
    void setDirty(boolean dirty);
}
