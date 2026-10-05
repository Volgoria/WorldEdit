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

package com.sk89q.worldedit.function.mask;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.registry.BlockMaterial;

import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Matches blocks whose {@link BlockMaterial} satisfies a predicate, for
 * example {@code BlockMaterial::isLiquid}.
 */
public class BlockMaterialMask extends AbstractExtentMask {

    private final Predicate<? super BlockMaterial> predicate;

    /**
     * Create a new mask.
     *
     * @param extent the extent
     * @param predicate the predicate on the material of the block
     */
    public BlockMaterialMask(Extent extent, Predicate<? super BlockMaterial> predicate) {
        super(extent);
        checkNotNull(predicate);
        this.predicate = predicate;
    }

    @Override
    public boolean test(BlockVector3 vector) {
        return predicate.test(getExtent().getBlock(vector).getBlockType().getMaterial());
    }
}
