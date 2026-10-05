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

package com.sk89q.worldedit.function.pattern;

import com.google.common.collect.ImmutableList;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.noise.NoiseGenerator;
import com.sk89q.worldedit.world.block.BaseBlock;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A pattern that picks one of several patterns based on a coherent noise
 * function, producing organic patches instead of per-block randomness.
 *
 * <p>The noise value range {@code [0, 1]} is divided into equal intervals,
 * one per pattern. Positions are divided by {@code scale} before sampling the
 * noise, so larger scales produce larger patches.</p>
 */
public class NoisePattern extends AbstractPattern {

    private final NoiseGenerator noise;
    private final double scale;
    private final List<Pattern> patterns;

    /**
     * Create a new noise pattern.
     *
     * @param noise the noise generator
     * @param scale the scale of the noise, greater than 0
     * @param patterns the patterns to choose from; must not be empty
     */
    public NoisePattern(NoiseGenerator noise, double scale, List<? extends Pattern> patterns) {
        checkNotNull(noise);
        checkNotNull(patterns);
        checkArgument(scale > 0, "scale must be greater than 0");
        checkArgument(!patterns.isEmpty(), "at least one pattern is required");
        this.noise = noise;
        this.scale = scale;
        this.patterns = ImmutableList.copyOf(patterns);
    }

    public NoiseGenerator getNoise() {
        return noise;
    }

    public double getScale() {
        return scale;
    }

    public List<Pattern> getPatterns() {
        return patterns;
    }

    @Override
    public BaseBlock applyBlock(BlockVector3 position) {
        float value = noise.noise(Vector3.at(
            position.x() / scale, position.y() / scale, position.z() / scale
        ));
        int index = (int) Math.floor(value * patterns.size());
        index = Math.max(0, Math.min(patterns.size() - 1, index));
        return patterns.get(index).applyBlock(position);
    }
}
