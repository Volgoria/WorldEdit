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
 * A semi-elliptical arch standing on the ground.
 *
 * <p>The origin is the bottom center of the arch. The arch spans
 * horizontally across the axis, rises upwards, and extends along the
 * axis (its depth), so that one can walk through it along the axis.</p>
 */
public class ArchShape extends OrientedShape {

    private final double outerU;
    private final double outerV;
    private final double innerU;
    private final double innerV;
    private final int minW;
    private final int maxW;

    /**
     * Create a new arch.
     *
     * @param width the total outer width of the arch; rounded up to an odd number so it is symmetric
     * @param height the total outer height of the arch, in blocks
     * @param thickness the thickness of the arch band, in blocks
     * @param depth the depth of the arch along the axis, in blocks
     * @param axis the horizontal axis to walk through the arch along; must be {@link ShapeAxis#X} or {@link ShapeAxis#Z}
     */
    public ArchShape(int width, int height, int thickness, int depth, ShapeAxis axis) {
        super(axis, localMin(width, depth), localMax(width, height, depth), false);
        checkArgument(axis != ShapeAxis.Y, "arches must be oriented along a horizontal axis");
        checkArgument(width >= 1, "width must be >= 1");
        checkArgument(height >= 1, "height must be >= 1");
        checkArgument(thickness >= 1, "thickness must be >= 1");
        checkArgument(depth >= 1, "depth must be >= 1");
        int halfWidth = width / 2;
        this.outerU = halfWidth + 0.5;
        this.outerV = height - 0.5;
        this.innerU = outerU - thickness;
        this.innerV = outerV - thickness;
        this.minW = -(depth - 1) / 2;
        this.maxW = minW + depth - 1;
    }

    private static BlockVector3 localMin(int width, int depth) {
        return BlockVector3.at(-(width / 2), 0, -(depth - 1) / 2);
    }

    private static BlockVector3 localMax(int width, int height, int depth) {
        return BlockVector3.at(width / 2, height - 1, -(depth - 1) / 2 + depth - 1);
    }

    @Override
    protected boolean containsLocal(int u, int v, int w) {
        if (v < 0 || w < minW || w > maxW) {
            return false;
        }
        if (!insideEllipse(u, v, outerU, outerV, false)) {
            return false;
        }
        return innerU <= 0 || innerV <= 0 || !insideEllipse(u, v, innerU, innerV, true);
    }

    private static boolean insideEllipse(int u, int v, double radiusU, double radiusV, boolean strict) {
        double nu = u / radiusU;
        double nv = v / radiusV;
        double distance = nu * nu + nv * nv;
        return strict ? distance < 1 : distance <= 1;
    }
}
