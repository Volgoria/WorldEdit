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
 * A flat elliptical disk perpendicular to an axis, centered on the origin.
 *
 * <p>When hollow, only the rim of the disk is generated (a ring); the faces
 * of the disk are left open.</p>
 */
public class DiskShape extends OrientedShape {

    private final double radiusU;
    private final double radiusV;
    private final int minW;
    private final int maxW;

    /**
     * Create a new disk.
     *
     * @param radiusU the radius along the first in-plane axis
     * @param radiusV the radius along the second in-plane axis
     * @param thickness the thickness along the axis, at least 1
     * @param axis the axis the disk is perpendicular to
     * @param hollow true to only generate the rim
     */
    public DiskShape(double radiusU, double radiusV, int thickness, ShapeAxis axis, boolean hollow) {
        super(axis, localMin(radiusU, radiusV, thickness), localMax(radiusU, radiusV, thickness), hollow);
        checkArgument(radiusU >= 0 && radiusV >= 0, "radii must be >= 0");
        checkArgument(thickness >= 1, "thickness must be >= 1");
        this.radiusU = radiusU + 0.5;
        this.radiusV = radiusV + 0.5;
        this.minW = -(thickness - 1) / 2;
        this.maxW = minW + thickness - 1;
    }

    private static BlockVector3 localMin(double radiusU, double radiusV, int thickness) {
        return BlockVector3.at(-(int) Math.ceil(radiusU), -(int) Math.ceil(radiusV), -(thickness - 1) / 2);
    }

    private static BlockVector3 localMax(double radiusU, double radiusV, int thickness) {
        return BlockVector3.at((int) Math.ceil(radiusU), (int) Math.ceil(radiusV), -(thickness - 1) / 2 + thickness - 1);
    }

    @Override
    protected boolean containsLocal(int u, int v, int w) {
        return w >= minW && w <= maxW && containsLocalForHollow(u, v, w);
    }

    @Override
    protected boolean containsLocalForHollow(int u, int v, int w) {
        double nu = u / radiusU;
        double nv = v / radiusV;
        return nu * nu + nv * nv <= 1;
    }
}
