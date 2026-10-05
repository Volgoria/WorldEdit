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

package com.sk89q.worldedit.command.tool.brush;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Cracks the terrain along the borders of random fragments.
 *
 * <p>A number of random seed points are scattered in the brush sphere, which
 * splits it into Voronoi cells. Every non-air block lying on the border
 * between two cells (where its distances to the two nearest seeds differ by
 * less than the crack width) is replaced, which produces a network of
 * fractured lines, like shattered rock or cracked ground.</p>
 */
public class ShatterBrush implements Brush {

    private final int fragments;
    private final double width;
    private final Random random;

    /**
     * Create a new shatter brush.
     *
     * @param fragments the number of fragments, at least 2
     * @param width the width of the cracks, greater than 0
     */
    public ShatterBrush(int fragments, double width) {
        this(fragments, width, new Random());
    }

    /**
     * Create a new shatter brush.
     *
     * @param fragments the number of fragments, at least 2
     * @param width the width of the cracks, greater than 0
     * @param random the source of randomness
     */
    public ShatterBrush(int fragments, double width, Random random) {
        checkArgument(fragments >= 2, "fragments must be at least 2");
        checkArgument(width > 0, "width must be positive");
        this.fragments = fragments;
        this.width = width;
        this.random = random;
    }

    @Override
    public void build(EditSession editSession, BlockVector3 position, Pattern pattern, double size) throws MaxChangedBlocksException {
        apply(editSession, position, pattern, size);
    }

    /**
     * Crack the terrain in the given extent.
     *
     * @param extent the extent
     * @param position the center of the brush
     * @param pattern the pattern used for the cracks, or {@code null} for air
     * @param size the radius of the brush
     * @throws MaxChangedBlocksException if the maximum block change limit is exceeded
     */
    public void apply(Extent extent, BlockVector3 position, Pattern pattern, double size) throws MaxChangedBlocksException {
        List<Vector3> seeds = createSeeds(position, size);
        BrushHelper.setBlocks(extent, findCracks(extent, position, size, seeds), BrushHelper.orDefault(pattern, BlockTypes.AIR));
    }

    /**
     * Scatter the seed points uniformly within the brush sphere.
     *
     * @param position the center of the brush
     * @param size the radius of the brush
     * @return the seed points
     */
    List<Vector3> createSeeds(BlockVector3 position, double size) {
        List<Vector3> seeds = new ArrayList<>(fragments);
        Vector3 center = position.toVector3();
        while (seeds.size() < fragments) {
            Vector3 offset = Vector3.at(random.nextDouble() * 2 - 1, random.nextDouble() * 2 - 1, random.nextDouble() * 2 - 1);
            if (offset.lengthSq() <= 1) {
                seeds.add(center.add(offset.multiply(size)));
            }
        }
        return seeds;
    }

    /**
     * Compute the crack positions for the given seeds, without modifying the extent.
     *
     * @param extent the extent
     * @param position the center of the brush
     * @param size the radius of the brush
     * @param seeds the seed points of the fragments
     * @return the positions to crack
     */
    List<BlockVector3> findCracks(Extent extent, BlockVector3 position, double size, List<Vector3> seeds) {
        List<BlockVector3> cracks = new ArrayList<>();
        for (BlockVector3 pos : BrushHelper.ballPositions(position, size)) {
            if (BrushHelper.isAir(extent, pos)) {
                continue;
            }
            if (isOnBorder(pos.toVector3(), seeds)) {
                cracks.add(pos);
            }
        }
        return cracks;
    }

    private boolean isOnBorder(Vector3 point, List<Vector3> seeds) {
        double nearest = Double.MAX_VALUE;
        double second = Double.MAX_VALUE;
        for (Vector3 seed : seeds) {
            double distance = seed.distance(point);
            if (distance < nearest) {
                second = nearest;
                nearest = distance;
            } else if (distance < second) {
                second = distance;
            }
        }
        return second - nearest < width;
    }
}
