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
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector2;
import com.sk89q.worldedit.math.noise.NoiseGenerator;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Generates a noise-based heightmap terrain inside a region.
 *
 * <p>Each column of the region is filled from the bottom of the region up to
 * {@code bottom + noise(x / scale, z / scale) * amplitude}, where the noise
 * is between 0 and 1. The space above the terrain can optionally be cleared.</p>
 */
public final class NoiseTerrain {

    private final NoiseGenerator noise;
    private final double scale;
    private final int amplitude;

    /**
     * Create a new terrain generator.
     *
     * @param noise the noise generator, returning values between 0 and 1
     * @param scale the horizontal scale of the noise, in blocks
     * @param amplitude the maximum height of the terrain above the bottom of the region
     */
    public NoiseTerrain(NoiseGenerator noise, double scale, int amplitude) {
        checkNotNull(noise);
        checkArgument(scale > 0, "scale must be positive");
        checkArgument(amplitude >= 0, "amplitude must not be negative");
        this.noise = noise;
        this.scale = scale;
        this.amplitude = amplitude;
    }

    /**
     * Get the height of the terrain at a column, relative to the bottom.
     *
     * @param x the X coordinate
     * @param z the Z coordinate
     * @return the height, between 0 and the amplitude
     */
    public int getHeight(int x, int z) {
        double value = noise.noise(Vector2.at(x / scale, z / scale));
        value = Math.max(0, Math.min(1, value));
        return (int) Math.round(value * amplitude);
    }

    /**
     * Generate the terrain.
     *
     * @param extent the extent
     * @param region the region
     * @param pattern the pattern of the terrain
     * @param topPattern the pattern of the top layer, or null to use the main pattern
     * @param air the block placed above the terrain, or null to leave it untouched
     * @return the number of blocks changed
     * @throws WorldEditException on error, such as hitting the change limit
     */
    public int generate(Extent extent, Region region, Pattern pattern, @Nullable Pattern topPattern,
                        @Nullable BlockStateHolder<?> air) throws WorldEditException {
        checkNotNull(extent);
        checkNotNull(region);
        checkNotNull(pattern);
        BaseBlock airBlock = air == null ? null : air.toBaseBlock();
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        int affected = 0;
        for (int x = min.x(); x <= max.x(); x++) {
            for (int z = min.z(); z <= max.z(); z++) {
                int top = min.y() + getHeight(x, z);
                for (int y = min.y(); y <= max.y(); y++) {
                    BlockVector3 pos = BlockVector3.at(x, y, z);
                    if (!region.contains(pos)) {
                        continue;
                    }
                    BaseBlock block;
                    if (y < top) {
                        block = pattern.applyBlock(pos);
                    } else if (y == top) {
                        block = (topPattern == null ? pattern : topPattern).applyBlock(pos);
                    } else if (airBlock != null) {
                        block = airBlock;
                    } else {
                        break;
                    }
                    if (extent.setBlock(pos, block)) {
                        affected++;
                    }
                }
            }
        }
        return affected;
    }

}
