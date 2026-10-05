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

package com.sk89q.worldedit.function.mask;

import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Matches positions whose coordinate along one axis is within an inclusive range.
 *
 * <p>For the Y axis, {@link BoundedHeightMask} is equivalent.</p>
 */
public class CoordinateRangeMask extends AbstractMask {

    /**
     * An axis of the world.
     */
    public enum Axis {
        X, Y, Z;

        /**
         * Get the coordinate of a position along this axis.
         *
         * @param position the position
         * @return the coordinate
         */
        public int of(BlockVector3 position) {
            return switch (this) {
                case X -> position.x();
                case Y -> position.y();
                case Z -> position.z();
            };
        }
    }

    private final Axis axis;
    private final int min;
    private final int max;

    /**
     * Create a new mask.
     *
     * @param axis the axis
     * @param min the minimum coordinate
     * @param max the maximum coordinate, at least {@code min}
     */
    public CoordinateRangeMask(Axis axis, int min, int max) {
        checkNotNull(axis);
        checkArgument(min <= max, "min <= max required");
        this.axis = axis;
        this.min = min;
        this.max = max;
    }

    public Axis getAxis() {
        return axis;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    @Override
    public boolean test(BlockVector3 vector) {
        int value = axis.of(vector);
        return value >= min && value <= max;
    }

    @Nullable
    @Override
    public Mask2D toMask2D() {
        return switch (axis) {
            case X -> (BlockVector2 vector) -> vector.x() >= min && vector.x() <= max;
            case Z -> (BlockVector2 vector) -> vector.z() >= min && vector.z() <= max;
            case Y -> null;
        };
    }
}
