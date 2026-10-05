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

package com.sk89q.worldedit.internal.edit;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.function.RegionMaskingFilter;
import com.sk89q.worldedit.function.block.BlockDistributionCounter;
import com.sk89q.worldedit.function.block.Counter;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.visitor.RegionVisitor;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Countable;
import com.sk89q.worldedit.world.block.BlockState;

import java.util.List;
import javax.annotation.Nullable;

/**
 * The read-only queries of {@link EditSession}: counting blocks, block
 * distributions and terrain height.
 */
public final class BlockAnalysis {

    /**
     * Implementation of {@link EditSession#getHighestTerrainBlock(int, int, int, int, Mask)}.
     */
    public static int getHighestTerrainBlock(EditSession session, int x, int z, int minY, int maxY,
                                             @Nullable Mask filter) {
        for (int y = maxY; y >= minY; --y) {
            BlockVector3 pt = BlockVector3.at(x, y, z);
            if (filter == null
                    ? session.getBlock(pt).getBlockType().getMaterial().isSolid()
                    : filter.test(pt)) {
                return y;
            }
        }

        return minY;
    }

    /**
     * Implementation of {@link EditSession#countBlocks(Region, Mask)}.
     */
    public static int countBlocks(Region region, Mask searchMask) {
        Counter count = new Counter();
        RegionMaskingFilter filter = new RegionMaskingFilter(searchMask, count);
        RegionVisitor visitor = new RegionVisitor(region, filter);
        Operations.completeBlindly(visitor); // We can't throw exceptions, nor do we expect any
        return count.getCount();
    }

    /**
     * Implementation of {@link EditSession#getBlockDistribution(Region, Mask, boolean)}.
     */
    public static List<Countable<BlockState>> getBlockDistribution(EditSession session, Region region,
                                                                   @Nullable Mask mask, boolean separateStates) {
        BlockDistributionCounter count = new BlockDistributionCounter(session, mask, separateStates);
        RegionVisitor visitor = new RegionVisitor(region, count);
        Operations.completeBlindly(visitor);
        return count.getDistribution();
    }

    private BlockAnalysis() {
    }
}
