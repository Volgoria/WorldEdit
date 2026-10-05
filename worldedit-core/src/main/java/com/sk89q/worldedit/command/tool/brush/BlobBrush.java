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
import com.sk89q.worldedit.function.RegionMaskingFilter;
import com.sk89q.worldedit.function.block.BlockReplace;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.visitor.RegionVisitor;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.noise.PerlinNoise;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.Random;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Creates an organic, noise-deformed sphere ("blob").
 *
 * <p>The surface radius in every direction is perturbed by Perlin noise
 * sampled on the unit sphere, so each blob is star-shaped and solid
 * (there are no floating fragments), while every use produces a new shape.</p>
 */
public class BlobBrush implements Brush {

    /**
     * The fraction of the radius the surface may deviate by at roughness 100.
     */
    private static final double MAX_AMPLITUDE = 0.5;
    /**
     * Scale applied to the unit direction before sampling noise.
     */
    private static final double NOISE_SCALE = 1.5;

    private final double amplitude;
    private final Random random;

    /**
     * Create a new blob brush.
     *
     * @param roughness the roughness, between 0 (a sphere) and 100 (very lumpy)
     */
    public BlobBrush(double roughness) {
        this(roughness, new Random());
    }

    /**
     * Create a new blob brush with a given source of randomness.
     *
     * @param roughness the roughness, between 0 (a sphere) and 100 (very lumpy)
     * @param random the source of noise seeds
     */
    public BlobBrush(double roughness, Random random) {
        checkArgument(roughness >= 0 && roughness <= 100, "roughness must be between 0 and 100");
        this.amplitude = roughness / 100.0 * MAX_AMPLITUDE;
        this.random = checkNotNull(random);
    }

    /**
     * Get the maximum fraction of the radius by which the surface is moved by the noise.
     *
     * @return the amplitude, between 0 and 0.5
     */
    public double getAmplitude() {
        return amplitude;
    }

    @Override
    public void build(EditSession editSession, BlockVector3 position, Pattern pattern, double size) throws MaxChangedBlocksException {
        apply(editSession, position, pattern, size);
    }

    /**
     * Generate a blob in the given extent.
     *
     * @param extent the extent
     * @param position the center of the blob
     * @param pattern the pattern to use, or {@code null} for cobblestone
     * @param size the base radius
     * @throws MaxChangedBlocksException if the maximum block change limit is exceeded
     */
    public void apply(Extent extent, BlockVector3 position, Pattern pattern, double size) throws MaxChangedBlocksException {
        if (pattern == null) {
            pattern = BlockTypes.COBBLESTONE.getDefaultState();
        }
        PerlinNoise noise = new PerlinNoise();
        noise.setSeed(random.nextInt());

        int bound = (int) Math.ceil(size * (1 + amplitude) + 0.5);
        CuboidRegion region = new CuboidRegion(
            position.subtract(bound, bound, bound),
            position.add(bound, bound, bound)
        );
        Vector3 center = position.toVector3();
        double minFactor = 1 - amplitude;
        double noiseFactor = 2 * amplitude;

        Operations.completeLegacy(new RegionVisitor(region, new RegionMaskingFilter(
            point -> {
                Vector3 offset = point.toVector3().subtract(center);
                double distance = offset.length();
                if (distance == 0) {
                    return true;
                }
                double n = amplitude == 0 ? 0 : noise.noise(offset.divide(distance).multiply(NOISE_SCALE));
                return distance <= size * (minFactor + noiseFactor * n) + 0.5;
            },
            new BlockReplace(extent, pattern)
        )));
    }
}
