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

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Half of an ellipsoid, cut horizontally through its center at the origin.
 *
 * <p>When hollow, only the curved surface is generated, so the flat face of
 * the dome is left open.</p>
 */
public class DomeShape extends GeneratedShape {

    private final double radiusX;
    private final double radiusY;
    private final double radiusZ;
    private final boolean inverted;
    private final BlockVector3 minimum;
    private final BlockVector3 maximum;

    /**
     * Create a new dome.
     *
     * @param radiusX the radius along the x axis
     * @param radiusY the height of the dome
     * @param radiusZ the radius along the z axis
     * @param inverted true to generate the lower half (a bowl) instead of the upper half
     * @param hollow true to only generate the curved surface
     */
    public DomeShape(double radiusX, double radiusY, double radiusZ, boolean inverted, boolean hollow) {
        super(hollow);
        checkArgument(radiusX >= 0 && radiusY >= 0 && radiusZ >= 0, "radii must be >= 0");
        this.radiusX = radiusX + 0.5;
        this.radiusY = radiusY + 0.5;
        this.radiusZ = radiusZ + 0.5;
        this.inverted = inverted;
        int ceilX = (int) Math.ceil(radiusX);
        int ceilY = (int) Math.ceil(radiusY);
        int ceilZ = (int) Math.ceil(radiusZ);
        this.minimum = BlockVector3.at(-ceilX, inverted ? -ceilY : 0, -ceilZ);
        this.maximum = BlockVector3.at(ceilX, inverted ? 0 : ceilY, ceilZ);
    }

    @Override
    public BlockVector3 getMinimumOffset() {
        return minimum;
    }

    @Override
    public BlockVector3 getMaximumOffset() {
        return maximum;
    }

    @Override
    protected boolean contains(int x, int y, int z) {
        if (inverted ? y > 0 : y < 0) {
            return false;
        }
        return containsForHollow(x, y, z);
    }

    @Override
    protected boolean containsForHollow(int x, int y, int z) {
        double nx = x / radiusX;
        double ny = y / radiusY;
        double nz = z / radiusZ;
        return nx * nx + ny * ny + nz * nz <= 1;
    }
}
