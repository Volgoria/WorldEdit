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
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Replaces a random fraction of the exposed blocks of a region, to add
 * texture to walls, floors and terrain.
 *
 * <p>A block is exposed when it is not air and at least one of its six
 * neighbours is air.</p>
 */
public final class SurfaceTexturizer {

    private static final BlockVector3[] NEIGHBOURS = {
        BlockVector3.UNIT_X, BlockVector3.UNIT_MINUS_X,
        BlockVector3.UNIT_Y, BlockVector3.UNIT_MINUS_Y,
        BlockVector3.UNIT_Z, BlockVector3.UNIT_MINUS_Z,
    };

    private SurfaceTexturizer() {
    }

    /**
     * Test whether a block is exposed to air.
     *
     * @param extent the extent
     * @param pos the position
     * @return true if the block is solid and touches air
     */
    public static boolean isExposed(Extent extent, BlockVector3 pos) {
        if (isAir(extent, pos)) {
            return false;
        }
        for (BlockVector3 offset : NEIGHBOURS) {
            if (isAir(extent, pos.add(offset))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the exposed blocks of a region.
     *
     * @param extent the extent
     * @param region the region
     * @param mask a mask the blocks must match, or null
     * @return the exposed positions
     */
    public static List<BlockVector3> getExposed(Extent extent, Region region, @Nullable Mask mask) {
        List<BlockVector3> exposed = new ArrayList<>();
        for (BlockVector3 pos : region) {
            if ((mask == null || mask.test(pos)) && isExposed(extent, pos)) {
                exposed.add(pos);
            }
        }
        return exposed;
    }

    /**
     * Replace a fraction of the exposed blocks of a region.
     *
     * @param extent the extent
     * @param region the region
     * @param pattern the pattern to place
     * @param fraction the chance for each exposed block to be replaced, between 0 and 1
     * @param mask a mask the blocks must match, or null
     * @param random the random generator
     * @return the number of blocks changed
     * @throws WorldEditException on error, such as hitting the change limit
     */
    public static int apply(Extent extent, Region region, Pattern pattern, double fraction,
                            @Nullable Mask mask, Random random) throws WorldEditException {
        checkNotNull(extent);
        checkNotNull(pattern);
        checkNotNull(random);
        checkArgument(fraction >= 0 && fraction <= 1, "fraction must be between 0 and 1");
        int affected = 0;
        // Collect first, so that changes do not affect which blocks are exposed
        for (BlockVector3 pos : getExposed(extent, region, mask)) {
            if (random.nextDouble() < fraction && extent.setBlock(pos, pattern.applyBlock(pos))) {
                affected++;
            }
        }
        return affected;
    }

    private static boolean isAir(Extent extent, BlockVector3 pos) {
        return extent.getBlock(pos).getBlockType().getMaterial().isAir();
    }

}
