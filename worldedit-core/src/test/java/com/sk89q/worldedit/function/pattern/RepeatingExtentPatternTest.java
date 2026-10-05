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

import com.sk89q.worldedit.extension.factory.parser.TestBlockExtent;
import com.sk89q.worldedit.math.BlockVector3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class RepeatingExtentPatternTest {

    // a 2x2x2 extent at (10, 20, 30), with one distinct block per corner of interest
    private final TestBlockExtent extent = new TestBlockExtent(BlockVector3.at(10, 20, 30), BlockVector3.at(11, 21, 31))
        .set(10, 20, 30, TestBlockExtent.STONE)
        .set(11, 20, 30, TestBlockExtent.GLASS)
        .set(10, 21, 31, TestBlockExtent.WATER);

    @Test
    void repeatsFromOrigin() {
        Pattern pattern = new RepeatingExtentPattern(extent, extent.getMinimumPoint(), BlockVector3.ZERO);
        assertSame(TestBlockExtent.STONE.full(), pattern.applyBlock(BlockVector3.ZERO));
        assertSame(TestBlockExtent.GLASS.full(), pattern.applyBlock(BlockVector3.at(1, 0, 0)));
        assertSame(TestBlockExtent.STONE.full(), pattern.applyBlock(BlockVector3.at(2, 4, -6)));
        assertSame(TestBlockExtent.WATER.full(), pattern.applyBlock(BlockVector3.at(-2, -1, -1)));
    }

    @Test
    void appliesOffset() {
        Pattern pattern = new RepeatingExtentPattern(extent, extent.getMinimumPoint(), BlockVector3.at(1, 0, 0));
        assertSame(TestBlockExtent.GLASS.full(), pattern.applyBlock(BlockVector3.ZERO));
        assertSame(TestBlockExtent.STONE.full(), pattern.applyBlock(BlockVector3.at(-1, 0, 0)));
        assertSame(TestBlockExtent.WATER.full(), pattern.applyBlock(BlockVector3.at(-1, -1, -1)));
    }

    @Test
    void handlesExtremeCoordinates() {
        Pattern pattern = new RepeatingExtentPattern(extent, extent.getMinimumPoint(), BlockVector3.ZERO);
        // MIN_VALUE is even, so it maps to the first block on each axis
        assertSame(TestBlockExtent.STONE.full(),
            pattern.applyBlock(BlockVector3.at(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE)));
    }
}
