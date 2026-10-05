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

import com.sk89q.worldedit.math.BlockVector3;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Matches positions whose euclidean distance to a center lies within an
 * inclusive range, forming a sphere or a spherical shell.
 */
public class RadiusMask extends AbstractMask {

    private final BlockVector3 center;
    private final double minRadius;
    private final double maxRadius;
    private final double minSquared;
    private final double maxSquared;

    /**
     * Create a new mask.
     *
     * @param center the center
     * @param minRadius the minimum distance, at least 0
     * @param maxRadius the maximum distance, at least {@code minRadius}
     */
    public RadiusMask(BlockVector3 center, double minRadius, double maxRadius) {
        checkNotNull(center);
        checkArgument(minRadius >= 0, "minRadius must be at least 0");
        checkArgument(maxRadius >= minRadius, "maxRadius must be at least minRadius");
        this.center = center;
        this.minRadius = minRadius;
        this.maxRadius = maxRadius;
        this.minSquared = minRadius * minRadius;
        this.maxSquared = maxRadius * maxRadius;
    }

    public BlockVector3 getCenter() {
        return center;
    }

    public double getMinRadius() {
        return minRadius;
    }

    public double getMaxRadius() {
        return maxRadius;
    }

    @Override
    public boolean test(BlockVector3 vector) {
        // double arithmetic avoids overflow for far away positions
        double dx = (double) vector.x() - center.x();
        double dy = (double) vector.y() - center.y();
        double dz = (double) vector.z() - center.z();
        double distanceSquared = dx * dx + dy * dy + dz * dz;
        return distanceSquared >= minSquared && distanceSquared <= maxSquared;
    }
}
