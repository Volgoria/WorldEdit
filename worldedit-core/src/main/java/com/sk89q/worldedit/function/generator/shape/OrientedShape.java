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

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A shape described in local {@code (u, v, w)} coordinates, oriented along a
 * {@link ShapeAxis}.
 */
public abstract class OrientedShape extends GeneratedShape {

    private final ShapeAxis axis;
    private final BlockVector3 minimum;
    private final BlockVector3 maximum;

    /**
     * Create a new oriented shape.
     *
     * @param axis the axis
     * @param localMin the minimum of the bounding box, in local coordinates
     * @param localMax the maximum of the bounding box, in local coordinates
     * @param hollow true to only generate the surface
     */
    protected OrientedShape(ShapeAxis axis, BlockVector3 localMin, BlockVector3 localMax, boolean hollow) {
        super(hollow);
        this.axis = checkNotNull(axis);
        BlockVector3 a = axis.toWorld(localMin.x(), localMin.y(), localMin.z());
        BlockVector3 b = axis.toWorld(localMax.x(), localMax.y(), localMax.z());
        this.minimum = a.getMinimum(b);
        this.maximum = a.getMaximum(b);
    }

    /**
     * Get the axis of this shape.
     *
     * @return the axis
     */
    public ShapeAxis getAxis() {
        return axis;
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
    protected final boolean contains(int x, int y, int z) {
        BlockVector3 local = axis.toLocal(x, y, z);
        return containsLocal(local.x(), local.y(), local.z());
    }

    @Override
    protected final boolean containsForHollow(int x, int y, int z) {
        BlockVector3 local = axis.toLocal(x, y, z);
        return containsLocalForHollow(local.x(), local.y(), local.z());
    }

    /**
     * Get the lowest local {@code w} coordinate of a slab of the given
     * thickness centred on {@code w = 0}. The slab spans
     * {@code [centredMin(t), centredMin(t) + t - 1]}; for even thicknesses
     * the extra layer is on the positive side.
     *
     * @param thickness the thickness of the slab, at least 1
     * @return the lowest {@code w} coordinate
     */
    protected static int centredMin(int thickness) {
        return -(thickness - 1) / 2;
    }

    /**
     * Get the normalized squared distance of a point to the centre of an
     * axis-aligned ellipse: at most 1 inside the ellipse, above 1 outside.
     *
     * @param u the first coordinate
     * @param v the second coordinate
     * @param radiusU the radius along the first coordinate
     * @param radiusV the radius along the second coordinate
     * @return the normalized squared distance
     */
    protected static double ellipseDistance(int u, int v, double radiusU, double radiusV) {
        double nu = u / radiusU;
        double nv = v / radiusV;
        return nu * nu + nv * nv;
    }

    /**
     * Test whether the given local coordinates are inside the shape.
     *
     * @param u the first in-plane coordinate
     * @param v the second in-plane coordinate
     * @param w the coordinate along the axis
     * @return true if inside
     */
    protected abstract boolean containsLocal(int u, int v, int w);

    /**
     * Local equivalent of {@link #containsForHollow(int, int, int)}.
     *
     * @param u the first in-plane coordinate
     * @param v the second in-plane coordinate
     * @param w the coordinate along the axis
     * @return true if considered inside for the hollow test
     */
    protected boolean containsLocalForHollow(int u, int v, int w) {
        return containsLocal(u, v, w);
    }
}
