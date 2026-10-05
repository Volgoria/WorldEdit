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
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CheckerPatternTest {

    private final List<MarkerPattern> markers = MarkerPattern.create(2);
    private final BaseBlock first = markers.get(0).block();
    private final BaseBlock second = markers.get(1).block();

    @Test
    void neighboursAlternate() {
        CheckerPattern pattern = new CheckerPattern(markers.get(0), markers.get(1), 1);
        assertSame(first, pattern.applyBlock(BlockVector3.ZERO));
        for (int x = -5; x <= 5; x++) {
            for (int y = -5; y <= 5; y++) {
                for (int z = -5; z <= 5; z++) {
                    BlockVector3 position = BlockVector3.at(x, y, z);
                    BaseBlock here = pattern.applyBlock(position);
                    assertNotSame(here, pattern.applyBlock(position.add(1, 0, 0)));
                    assertNotSame(here, pattern.applyBlock(position.add(0, 1, 0)));
                    assertNotSame(here, pattern.applyBlock(position.add(0, 0, 1)));
                    assertSame(here, pattern.applyBlock(position.add(1, 1, 0)));
                }
            }
        }
    }

    @Test
    void sizeScalesCells() {
        CheckerPattern pattern = new CheckerPattern(markers.get(0), markers.get(1), 2);
        assertSame(first, pattern.applyBlock(BlockVector3.at(0, 0, 0)));
        assertSame(first, pattern.applyBlock(BlockVector3.at(1, 1, 1)));
        assertSame(second, pattern.applyBlock(BlockVector3.at(2, 0, 0)));
        assertSame(second, pattern.applyBlock(BlockVector3.at(-1, 0, 0)));
        assertSame(second, pattern.applyBlock(BlockVector3.at(-2, 0, 0)));
        assertSame(first, pattern.applyBlock(BlockVector3.at(-3, 0, 0)));
    }

    @Test
    void rejectsInvalidSize() {
        assertThrows(IllegalArgumentException.class, () -> new CheckerPattern(markers.get(0), markers.get(1), 0));
    }
}
