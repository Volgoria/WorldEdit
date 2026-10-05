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
import com.sk89q.worldedit.world.block.BaseBlock;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A pattern that cycles through a list of patterns in parallel stripes.
 *
 * <p>The stripe of a position is the dot product of the position with a
 * direction vector, divided by the stripe thickness. With a direction of
 * {@code (0, 1, 0)} this creates horizontal layers, with {@code (1, 0, 1)}
 * diagonal stripes, and so on.</p>
 */
public class StripePattern extends AbstractPattern {

    private final List<Pattern> patterns;
    private final BlockVector3 direction;
    private final int thickness;

    /**
     * Create a new stripe pattern.
     *
     * @param patterns the patterns to cycle through; must not be empty
     * @param direction the direction across which the stripes alternate; must not be zero
     * @param thickness the thickness of a single stripe, at least 1
     */
    public StripePattern(List<? extends Pattern> patterns, BlockVector3 direction, int thickness) {
        checkNotNull(patterns);
        checkNotNull(direction);
        checkArgument(!patterns.isEmpty(), "at least one pattern is required");
        checkArgument(!direction.equals(BlockVector3.ZERO), "direction must not be zero");
        checkArgument(thickness >= 1, "thickness must be at least 1");
        this.patterns = ImmutableList.copyOf(patterns);
        this.direction = direction;
        this.thickness = thickness;
    }

    public List<Pattern> getPatterns() {
        return patterns;
    }

    public BlockVector3 getDirection() {
        return direction;
    }

    public int getThickness() {
        return thickness;
    }

    @Override
    public BaseBlock applyBlock(BlockVector3 position) {
        long coordinate = (long) position.x() * direction.x()
            + (long) position.y() * direction.y()
            + (long) position.z() * direction.z();
        long stripe = Math.floorDiv(coordinate, thickness);
        int index = Math.floorMod(stripe, patterns.size());
        return patterns.get(index).applyBlock(position);
    }
}
