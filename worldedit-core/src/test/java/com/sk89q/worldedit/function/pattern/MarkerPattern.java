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

import java.util.List;

import static org.mockito.Mockito.mock;

/**
 * A pattern that always returns its own unique (mocked) block, so tests can
 * tell which pattern produced a block without needing block registries.
 */
final class MarkerPattern extends AbstractPattern {

    private final BaseBlock block = mock(BaseBlock.class);

    static List<MarkerPattern> create(int count) {
        MarkerPattern[] patterns = new MarkerPattern[count];
        for (int i = 0; i < count; i++) {
            patterns[i] = new MarkerPattern();
        }
        return List.of(patterns);
    }

    BaseBlock block() {
        return block;
    }

    @Override
    public BaseBlock applyBlock(BlockVector3 position) {
        return block;
    }

    /**
     * Find which of the given marker patterns produced the block.
     *
     * @param markers the markers
     * @param block the block
     * @return the index of the producing marker, or -1
     */
    static int indexOf(List<MarkerPattern> markers, BaseBlock block) {
        for (int i = 0; i < markers.size(); i++) {
            if (markers.get(i).block == block) {
                return i;
            }
        }
        return -1;
    }
}
