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

import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * A vertical helix (spiral) rising from the origin, made of one or more
 * evenly spaced strands.
 */
public class HelixShape extends GeneratedShape {

    private static final double SAMPLES_PER_BLOCK = 4;

    private final Set<BlockVector3> blocks = new HashSet<>();
    private final BlockVector3 minimum;
    private final BlockVector3 maximum;

    /**
     * Create a new helix.
     *
     * @param radius the horizontal distance from the center axis to the strands
     * @param height the height of the helix, in blocks
     * @param turns the number of full turns each strand makes
     * @param thickness the diameter of each strand, in blocks
     * @param strands the number of strands, evenly spaced around the axis
     */
    public HelixShape(double radius, int height, double turns, double thickness, int strands) {
        super(false);
        checkArgument(radius >= 0, "radius must be >= 0");
        checkArgument(height >= 1, "height must be >= 1");
        checkArgument(turns >= 0, "turns must be >= 0");
        checkArgument(thickness >= 1, "thickness must be >= 1");
        checkArgument(strands >= 1, "strands must be >= 1");

        double halfThickness = thickness / 2;
        double halfThicknessSq = halfThickness * halfThickness;
        int stamp = (int) Math.floor(halfThickness);
        int top = height - 1;

        double horizontalLength = 2 * Math.PI * radius * turns;
        double length = Math.sqrt(horizontalLength * horizontalLength + (double) top * top);
        int samples = Math.max(1, (int) Math.ceil(length * SAMPLES_PER_BLOCK));

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (int strand = 0; strand < strands; strand++) {
            double phase = 2 * Math.PI * strand / strands;
            for (int i = 0; i <= samples; i++) {
                double t = (double) i / samples;
                double angle = 2 * Math.PI * turns * t + phase;
                int cx = (int) Math.round(radius * Math.cos(angle));
                int cy = (int) Math.round(top * t);
                int cz = (int) Math.round(radius * Math.sin(angle));
                for (int dx = -stamp; dx <= stamp; dx++) {
                    for (int dy = -stamp; dy <= stamp; dy++) {
                        for (int dz = -stamp; dz <= stamp; dz++) {
                            if (dx * dx + dy * dy + dz * dz > halfThicknessSq) {
                                continue;
                            }
                            int x = cx + dx;
                            int y = cy + dy;
                            int z = cz + dz;
                            if (blocks.add(BlockVector3.at(x, y, z))) {
                                minX = Math.min(minX, x);
                                minY = Math.min(minY, y);
                                minZ = Math.min(minZ, z);
                                maxX = Math.max(maxX, x);
                                maxY = Math.max(maxY, y);
                                maxZ = Math.max(maxZ, z);
                            }
                        }
                    }
                }
            }
        }

        this.minimum = BlockVector3.at(minX, minY, minZ);
        this.maximum = BlockVector3.at(maxX, maxY, maxZ);
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
        return blocks.contains(BlockVector3.at(x, y, z));
    }
}
