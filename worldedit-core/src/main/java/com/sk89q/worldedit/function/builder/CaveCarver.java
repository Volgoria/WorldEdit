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

package com.sk89q.worldedit.function.builder;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.noise.PerlinNoise;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Carves winding tunnels ("Perlin worms") inside a region.
 *
 * <p>A worm starts at a point and moves one block at a time. Its heading and
 * pitch are steered by smooth Perlin noise, and its radius varies slightly
 * along the way. When it is about to leave the region, it turns back.
 * Only blocks inside the region are carved.</p>
 */
public final class CaveCarver {

    /**
     * The longest tunnel that can be carved. Every step of a tunnel is kept in
     * memory, so the length must be bounded before carving.
     */
    public static final int MAX_LENGTH = 4096;

    private static final double MAX_PITCH = 0.6;

    private final double radius;
    private final int length;
    private final long seed;

    /**
     * Create a new cave carver.
     *
     * @param radius the average radius of the tunnels
     * @param length the length of each tunnel, in blocks
     * @param seed the seed of the random generator
     */
    public CaveCarver(double radius, int length, long seed) {
        checkArgument(radius >= 0.5, "radius must be at least 0.5");
        checkArgument(length >= 1 && length <= MAX_LENGTH, "length must be between 1 and " + MAX_LENGTH);
        this.radius = radius;
        this.length = length;
        this.seed = seed;
    }

    /**
     * Compute the centre line of tunnels, without carving anything.
     *
     * @param region the region the tunnels stay in
     * @param count the number of tunnels
     * @return the centre points of each step of each tunnel
     */
    public List<List<Vector3>> getWorms(Region region, int count) {
        checkNotNull(region);
        checkArgument(count >= 1, "count must be at least 1");
        Random random = new Random(seed);
        PerlinNoise noise = new PerlinNoise();
        noise.setSeed(random.nextInt());
        noise.setOctaveCount(2);

        Vector3 min = region.getMinimumPoint().toVector3();
        Vector3 max = region.getMaximumPoint().toVector3();
        List<List<Vector3>> worms = new ArrayList<>();
        for (int worm = 0; worm < count; worm++) {
            Vector3 position;
            if (worm == 0) {
                position = region.getCenter();
            } else {
                position = randomPoint(region, random);
            }
            double yaw = random.nextDouble() * Math.PI * 2;
            double pitch = 0;
            List<Vector3> points = new ArrayList<>();
            for (int step = 0; step < length; step++) {
                points.add(position);
                double t = step * 0.05;
                yaw += (noise.noise(Vector3.at(t, worm * 13.37, 0.5)) - 0.5) * 0.8;
                pitch += (noise.noise(Vector3.at(0.5, worm * 13.37, t)) - 0.5) * 0.4;
                pitch = Math.max(-MAX_PITCH, Math.min(MAX_PITCH, pitch * 0.95));
                Vector3 heading = Vector3.at(Math.cos(pitch) * Math.cos(yaw), Math.sin(pitch), Math.cos(pitch) * Math.sin(yaw));
                Vector3 next = position.add(heading);
                if (!isInside(next, min, max)) {
                    // Turn back towards the centre of the region
                    Vector3 back = region.getCenter().subtract(position);
                    yaw = Math.atan2(back.z(), back.x());
                    pitch = 0;
                    heading = Vector3.at(Math.cos(yaw), 0, Math.sin(yaw));
                    next = position.add(heading);
                }
                position = next;
            }
            worms.add(points);
        }
        return worms;
    }

    /**
     * Get the radius of a tunnel at a given step.
     *
     * @param step the step
     * @return the radius
     */
    public double getRadiusAt(int step) {
        return radius * (0.85 + 0.15 * Math.sin(step * 0.3));
    }

    /**
     * Carve the tunnels.
     *
     * @param extent the extent
     * @param region the region the tunnels stay in
     * @param count the number of tunnels
     * @param air the block used to carve the tunnels
     * @return the number of blocks changed
     * @throws WorldEditException on error, such as hitting the change limit
     */
    public int carve(Extent extent, Region region, int count, BlockStateHolder<?> air) throws WorldEditException {
        checkNotNull(extent);
        BaseBlock airBlock = air.toBaseBlock();
        int affected = 0;
        for (List<Vector3> worm : getWorms(region, count)) {
            for (int step = 0; step < worm.size(); step++) {
                affected += carveSphere(extent, region, worm.get(step), getRadiusAt(step), airBlock);
            }
        }
        return affected;
    }

    private static int carveSphere(Extent extent, Region region, Vector3 center, double radius, BaseBlock air)
        throws WorldEditException {
        int affected = 0;
        int bound = (int) Math.ceil(radius);
        BlockVector3 base = center.toBlockPoint();
        double radiusSq = radius * radius;
        for (int x = -bound; x <= bound; x++) {
            for (int y = -bound; y <= bound; y++) {
                for (int z = -bound; z <= bound; z++) {
                    BlockVector3 pos = base.add(x, y, z);
                    if (pos.toVector3().distanceSq(center) > radiusSq || !region.contains(pos)) {
                        continue;
                    }
                    if (!extent.getBlock(pos).getBlockType().getMaterial().isAir() && extent.setBlock(pos, air)) {
                        affected++;
                    }
                }
            }
        }
        return affected;
    }

    private static boolean isInside(Vector3 point, Vector3 min, Vector3 max) {
        return point.x() >= min.x() && point.y() >= min.y() && point.z() >= min.z()
            && point.x() <= max.x() && point.y() <= max.y() && point.z() <= max.z();
    }

    private static Vector3 randomPoint(Region region, Random random) {
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 size = region.getMaximumPoint().subtract(min);
        for (int attempt = 0; attempt < 32; attempt++) {
            BlockVector3 candidate = min.add(
                random.nextInt(size.x() + 1), random.nextInt(size.y() + 1), random.nextInt(size.z() + 1));
            if (region.contains(candidate)) {
                return candidate.toVector3();
            }
        }
        return region.getCenter();
    }

}
