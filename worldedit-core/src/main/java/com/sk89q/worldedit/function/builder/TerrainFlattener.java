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
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.util.OptionalInt;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Flattens the terrain of a region to a given height.
 *
 * <p>For every column of the region, the surface is the highest block
 * matching the surface mask. Anything above the target height is removed,
 * and gaps below it are filled, either with the given pattern or with the
 * block found just under the surface. The surface block itself (such as
 * grass) is moved to the target height.</p>
 */
public final class TerrainFlattener {

    private final Extent extent;
    private final Region region;
    private final Mask surfaceMask;
    private final BaseBlock air;
    @Nullable
    private final Pattern fill;

    /**
     * Create a new flattener.
     *
     * @param extent the extent
     * @param region the region to flatten
     * @param surfaceMask the mask of blocks that are part of the terrain
     * @param air the block used to clear the space above the target height
     * @param fill the pattern used to fill below the target height, or null to use the column's own blocks
     */
    public TerrainFlattener(Extent extent, Region region, Mask surfaceMask, BlockStateHolder<?> air, @Nullable Pattern fill) {
        this.extent = checkNotNull(extent);
        this.region = checkNotNull(region);
        this.surfaceMask = checkNotNull(surfaceMask);
        this.air = air.toBaseBlock();
        this.fill = fill;
    }

    /**
     * Find the surface height of a column of the region.
     *
     * @param x the X coordinate
     * @param z the Z coordinate
     * @return the height of the highest terrain block, if any
     */
    public OptionalInt getSurfaceHeight(int x, int z) {
        int minY = region.getMinimumPoint().y();
        for (int y = region.getMaximumPoint().y(); y >= minY; y--) {
            BlockVector3 pos = BlockVector3.at(x, y, z);
            if (region.contains(pos) && surfaceMask.test(pos)) {
                return OptionalInt.of(y);
            }
        }
        return OptionalInt.empty();
    }

    /**
     * Get the average surface height of the region, rounded.
     *
     * @return the average height, or empty if the region has no terrain
     */
    public OptionalInt getAverageSurfaceHeight() {
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        long sum = 0;
        int count = 0;
        for (int x = min.x(); x <= max.x(); x++) {
            for (int z = min.z(); z <= max.z(); z++) {
                OptionalInt height = getSurfaceHeight(x, z);
                if (height.isPresent()) {
                    sum += height.getAsInt();
                    count++;
                }
            }
        }
        if (count == 0) {
            return OptionalInt.empty();
        }
        return OptionalInt.of((int) Math.round((double) sum / count));
    }

    /**
     * Flatten the region.
     *
     * @param height the target height
     * @return the number of blocks changed
     * @throws WorldEditException on error, such as hitting the change limit
     */
    public int flatten(int height) throws WorldEditException {
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        int affected = 0;
        for (int x = min.x(); x <= max.x(); x++) {
            for (int z = min.z(); z <= max.z(); z++) {
                affected += flattenColumn(x, z, height);
            }
        }
        return affected;
    }

    private int flattenColumn(int x, int z, int height) throws WorldEditException {
        OptionalInt surface = getSurfaceHeight(x, z);
        if (surface.isEmpty()) {
            return fill == null ? 0 : fillColumn(x, z, region.getMinimumPoint().y(), height, null, null);
        }
        int surfaceY = surface.getAsInt();
        BaseBlock surfaceBlock = extent.getFullBlock(BlockVector3.at(x, surfaceY, z));
        int affected = 0;
        if (surfaceY > height) {
            for (int y = height + 1; y <= surfaceY; y++) {
                affected += set(BlockVector3.at(x, y, z), air);
            }
            affected += set(BlockVector3.at(x, height, z), surfaceBlock);
        } else if (surfaceY < height) {
            BlockVector3 below = BlockVector3.at(x, surfaceY - 1, z);
            BaseBlock under = region.contains(below) ? extent.getFullBlock(below) : surfaceBlock;
            if (under.getBlockType().getMaterial().isAir()) {
                under = surfaceBlock;
            }
            affected += fillColumn(x, z, surfaceY, height, under, surfaceBlock);
        }
        return affected;
    }

    private int fillColumn(int x, int z, int fromY, int height, @Nullable BaseBlock under, @Nullable BaseBlock top)
        throws WorldEditException {
        int affected = 0;
        for (int y = fromY; y <= height; y++) {
            BlockVector3 pos = BlockVector3.at(x, y, z);
            if (!region.contains(pos)) {
                continue;
            }
            BaseBlock block;
            if (y == height && top != null) {
                block = top;
            } else if (fill != null) {
                block = fill.applyBlock(pos);
            } else {
                block = checkNotNull(under);
            }
            affected += set(pos, block);
        }
        return affected;
    }

    private int set(BlockVector3 pos, BaseBlock block) throws WorldEditException {
        if (!region.contains(pos)) {
            return 0;
        }
        BaseBlock existing = extent.getFullBlock(pos);
        if (existing.equalsFuzzy(block)) {
            return 0;
        }
        return extent.setBlock(pos, block) ? 1 : 0;
    }

}
