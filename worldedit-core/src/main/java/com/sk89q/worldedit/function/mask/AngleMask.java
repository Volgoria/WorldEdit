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
 * Matches terrain blocks whose local slope lies within an angle range.
 *
 * <p>The slope is measured from the surface height of the column containing
 * the tested block, compared to the surface heights of the four columns
 * {@code distance} blocks away along the X and Z axes. The steepest of these
 * four height differences determines the angle, in degrees, where 0 is
 * flat and 90 is a vertical wall.</p>
 *
 * <p>Only blocks matching the terrain mask can match this mask. The surface
 * height of a column is searched at most {@code 16 * distance} blocks up or
 * down; larger height differences are treated as a 90 degree slope.</p>
 */
public class AngleMask extends AbstractMask {

    private static final double EPSILON = 1e-9;

    private final Mask terrain;
    private final double minAngle;
    private final double maxAngle;
    private final int distance;
    private final int maxScan;

    /**
     * Create a new angle mask.
     *
     * @param terrain the mask matching terrain blocks, e.g. a {@link SolidBlockMask}
     * @param minAngle the minimum angle in degrees, between 0 and 90
     * @param maxAngle the maximum angle in degrees, between {@code minAngle} and 90
     * @param distance the horizontal distance at which the heights are compared, at least 1
     */
    public AngleMask(Mask terrain, double minAngle, double maxAngle, int distance) {
        checkNotNull(terrain);
        checkArgument(minAngle >= 0 && minAngle <= 90, "minAngle must be between 0 and 90");
        checkArgument(maxAngle >= minAngle && maxAngle <= 90, "maxAngle must be between minAngle and 90");
        checkArgument(distance >= 1, "distance must be at least 1");
        this.terrain = terrain;
        this.minAngle = minAngle;
        this.maxAngle = maxAngle;
        this.distance = distance;
        this.maxScan = 16 * distance;
    }

    public Mask getTerrain() {
        return terrain;
    }

    public double getMinAngle() {
        return minAngle;
    }

    public double getMaxAngle() {
        return maxAngle;
    }

    public int getDistance() {
        return distance;
    }

    @Override
    public boolean test(BlockVector3 vector) {
        if (!terrain.test(vector)) {
            return false;
        }
        double angle = getAngle(vector);
        return angle >= minAngle - EPSILON && angle <= maxAngle + EPSILON;
    }

    /**
     * Compute the slope angle at the given terrain block.
     *
     * @param vector a position matched by the terrain mask
     * @return the angle in degrees, between 0 and 90
     */
    public double getAngle(BlockVector3 vector) {
        int x = vector.x();
        int z = vector.z();
        int top = surfaceHeight(x, vector.y(), z);
        int maxDiff = 0;
        maxDiff = Math.max(maxDiff, Math.abs(surfaceHeight(x + distance, top, z) - top));
        maxDiff = Math.max(maxDiff, Math.abs(surfaceHeight(x - distance, top, z) - top));
        maxDiff = Math.max(maxDiff, Math.abs(surfaceHeight(x, top, z + distance) - top));
        maxDiff = Math.max(maxDiff, Math.abs(surfaceHeight(x, top, z - distance) - top));
        if (maxDiff >= maxScan) {
            return 90;
        }
        return Math.toDegrees(Math.atan2(maxDiff, distance));
    }

    /**
     * Find the surface height of the column at x/z, starting the search at
     * the given Y level.
     *
     * @return the Y of the highest terrain block of the terrain section
     *     closest to {@code startY}, clamped to {@code startY +/- maxScan}
     */
    private int surfaceHeight(int x, int startY, int z) {
        if (terrain.test(BlockVector3.at(x, startY, z))) {
            for (int k = 1; k <= maxScan; k++) {
                if (!terrain.test(BlockVector3.at(x, startY + k, z))) {
                    return startY + k - 1;
                }
            }
            return startY + maxScan;
        }
        for (int k = 1; k <= maxScan; k++) {
            if (terrain.test(BlockVector3.at(x, startY - k, z))) {
                return startY - k;
            }
        }
        return startY - maxScan;
    }
}
