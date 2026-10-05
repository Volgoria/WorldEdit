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
 * A pattern that blends through a list of patterns along the Y axis.
 *
 * <p>The first pattern is used at {@code fromY}, the last one at {@code toY},
 * and the others are spread evenly in between. Between two neighbouring
 * patterns the transition is dithered, so that the share of the next
 * pattern grows smoothly with the height. Dithering is deterministic: the
 * same position always produces the same choice.</p>
 *
 * <p>{@code fromY} may be greater than {@code toY}, which inverts the
 * gradient. Positions outside of the range use the nearest end pattern.</p>
 */
public class GradientPattern extends AbstractPattern {

    private final List<Pattern> patterns;
    private final int fromY;
    private final int toY;

    /**
     * Create a new gradient pattern.
     *
     * @param patterns the patterns, from {@code fromY} to {@code toY}; must not be empty
     * @param fromY the Y level at which only the first pattern is used
     * @param toY the Y level at which only the last pattern is used
     */
    public GradientPattern(List<? extends Pattern> patterns, int fromY, int toY) {
        checkNotNull(patterns);
        checkArgument(!patterns.isEmpty(), "at least one pattern is required");
        this.patterns = ImmutableList.copyOf(patterns);
        this.fromY = fromY;
        this.toY = toY;
    }

    /**
     * Get the patterns of this gradient.
     *
     * @return the patterns, from {@code fromY} to {@code toY}
     */
    public List<Pattern> getPatterns() {
        return patterns;
    }

    public int getFromY() {
        return fromY;
    }

    public int getToY() {
        return toY;
    }

    @Override
    public BaseBlock applyBlock(BlockVector3 position) {
        return patterns.get(indexAt(position)).applyBlock(position);
    }

    private int indexAt(BlockVector3 position) {
        int last = patterns.size() - 1;
        if (last == 0 || fromY == toY) {
            return 0;
        }
        double progress = (position.y() - fromY) / (double) (toY - fromY);
        progress = Math.max(0, Math.min(1, progress));
        double scaled = progress * last;
        int index = (int) Math.floor(scaled);
        double fraction = scaled - index;
        if (fraction > 0 && PositionHash.unitHash(position) < fraction) {
            index++;
        }
        return Math.min(index, last);
    }
}
