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

import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A pattern that chooses between two patterns depending on whether a mask
 * matches the position, e.g. stone on steep slopes and grass elsewhere.
 */
public class ConditionalPattern extends AbstractPattern {

    private final Mask mask;
    private final Pattern ifTrue;
    private final Pattern ifFalse;

    /**
     * Create a new pattern.
     *
     * @param mask the mask to test
     * @param ifTrue the pattern used where the mask matches
     * @param ifFalse the pattern used elsewhere
     */
    public ConditionalPattern(Mask mask, Pattern ifTrue, Pattern ifFalse) {
        checkNotNull(mask);
        checkNotNull(ifTrue);
        checkNotNull(ifFalse);
        this.mask = mask;
        this.ifTrue = ifTrue;
        this.ifFalse = ifFalse;
    }

    public Mask getMask() {
        return mask;
    }

    public Pattern getIfTrue() {
        return ifTrue;
    }

    public Pattern getIfFalse() {
        return ifFalse;
    }

    @Override
    public BaseBlock applyBlock(BlockVector3 position) {
        return (mask.test(position) ? ifTrue : ifFalse).applyBlock(position);
    }
}
