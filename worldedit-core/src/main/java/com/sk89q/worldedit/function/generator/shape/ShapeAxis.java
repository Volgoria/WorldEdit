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

package com.sk89q.worldedit.function.generator.shape;

import com.sk89q.worldedit.math.BlockVector3;

import javax.annotation.Nullable;

/**
 * The axis a generated shape is oriented along.
 *
 * <p>Oriented shapes are described in local coordinates {@code (u, v, w)},
 * where {@code w} runs along the axis and {@code u}/{@code v} span the plane
 * perpendicular to it. For the horizontal axes, {@code v} is always the
 * vertical (y) direction.</p>
 */
public enum ShapeAxis {
    /**
     * The east/west axis; local {@code (u, v, w)} map to world {@code (z, y, x)}.
     */
    X,
    /**
     * The up/down axis; local {@code (u, v, w)} map to world {@code (x, z, y)}.
     */
    Y,
    /**
     * The north/south axis; local {@code (u, v, w)} map to world {@code (x, y, z)}.
     */
    Z;

    /**
     * Get the axis that best matches a direction, using its dominant component.
     *
     * @param direction the direction
     * @return the axis
     */
    public static ShapeAxis fromDirection(BlockVector3 direction) {
        int ax = Math.abs(direction.x());
        int ay = Math.abs(direction.y());
        int az = Math.abs(direction.z());
        if (ay >= ax && ay >= az) {
            return Y;
        }
        return ax >= az ? X : Z;
    }

    /**
     * Get the horizontal axis that best matches a direction, ignoring its
     * vertical component.
     *
     * @param direction the direction
     * @return the axis, or {@code null} if the direction is purely vertical
     */
    @Nullable
    public static ShapeAxis fromHorizontalDirection(BlockVector3 direction) {
        int ax = Math.abs(direction.x());
        int az = Math.abs(direction.z());
        if (ax == 0 && az == 0) {
            return null;
        }
        return ax >= az ? X : Z;
    }

    /**
     * Convert local coordinates to a world offset.
     *
     * @param u the first in-plane coordinate
     * @param v the second in-plane coordinate
     * @param w the coordinate along the axis
     * @return the world offset
     */
    public BlockVector3 toWorld(int u, int v, int w) {
        return switch (this) {
            case X -> BlockVector3.at(w, v, u);
            case Y -> BlockVector3.at(u, w, v);
            case Z -> BlockVector3.at(u, v, w);
        };
    }

    /**
     * Convert a world offset to local coordinates.
     *
     * @param x the x offset
     * @param y the y offset
     * @param z the z offset
     * @return the local coordinates, as {@code (u, v, w)}
     */
    public BlockVector3 toLocal(int x, int y, int z) {
        return switch (this) {
            case X -> BlockVector3.at(z, y, x);
            case Y -> BlockVector3.at(x, z, y);
            case Z -> BlockVector3.at(x, y, z);
        };
    }
}
