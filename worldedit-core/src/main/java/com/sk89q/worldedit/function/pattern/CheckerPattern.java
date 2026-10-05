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

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A three-dimensional checkerboard of two patterns.
 *
 * <p>Space is divided into cubes of {@code size} blocks, and neighbouring
 * cubes alternate between the two patterns. On any flat surface this looks
 * like a regular checkerboard.</p>
 */
public class CheckerPattern extends AbstractPattern {

    private final Pattern first;
    private final Pattern second;
    private final int size;

    /**
     * Create a new checker pattern.
     *
     * @param first the pattern used for the cube containing the origin
     * @param second the alternating pattern
     * @param size the edge length of a single cube, at least 1
     */
    public CheckerPattern(Pattern first, Pattern second, int size) {
        checkNotNull(first);
        checkNotNull(second);
        checkArgument(size >= 1, "size must be at least 1");
        this.first = first;
        this.second = second;
        this.size = size;
    }

    public Pattern getFirst() {
        return first;
    }

    public Pattern getSecond() {
        return second;
    }

    public int getSize() {
        return size;
    }

    @Override
    public BaseBlock applyBlock(BlockVector3 position) {
        long cell = (long) Math.floorDiv(position.x(), size)
            + Math.floorDiv(position.y(), size)
            + Math.floorDiv(position.z(), size);
        return (Math.floorMod(cell, 2) == 0 ? first : second).applyBlock(position);
    }
}
