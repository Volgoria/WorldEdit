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

package com.sk89q.worldedit.util.image;

import com.google.common.collect.ImmutableSet;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.FlatRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;

import java.util.Set;

/**
 * Helpers to inspect the top surface of a region, column by column.
 */
public final class Surfaces {

    /**
     * Returned by {@link #topBlockY} when a column contains no block.
     */
    public static final int NO_BLOCK = Integer.MIN_VALUE;

    private static final Set<String> AIR_IDS = ImmutableSet.of("minecraft:air", "minecraft:cave_air", "minecraft:void_air");

    private Surfaces() {
    }

    /**
     * Check whether a column is part of the horizontal footprint of a region.
     *
     * <p>For flat regions (cuboids, cylinders, polygons) this is exact; for
     * other regions the whole bounding box is used.</p>
     *
     * @param region the region
     * @param x the X coordinate
     * @param z the Z coordinate
     * @return true if the column belongs to the footprint
     */
    public static boolean isInFootprint(Region region, int x, int z) {
        if (region instanceof FlatRegion flat) {
            return region.contains(BlockVector3.at(x, flat.getMinimumY(), z));
        }
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        return x >= min.x() && x <= max.x() && z >= min.z() && z <= max.z();
    }

    /**
     * Find the highest non-air block in a column.
     *
     * @param extent the extent
     * @param x the X coordinate
     * @param z the Z coordinate
     * @param minY the lowest Y to consider
     * @param maxY the highest Y to consider
     * @return the Y coordinate, or {@link #NO_BLOCK}
     */
    public static int topBlockY(Extent extent, int x, int z, int minY, int maxY) {
        for (int y = maxY; y >= minY; y--) {
            if (!isAir(extent.getBlock(BlockVector3.at(x, y, z)))) {
                return y;
            }
        }
        return NO_BLOCK;
    }

    /**
     * Check whether a block is air.
     *
     * @param state the block
     * @return true if air
     */
    public static boolean isAir(BlockState state) {
        if (state == null) {
            return true;
        }
        BlockType type = state.getBlockType();
        return AIR_IDS.contains(type.id()) || type.getMaterial().isAir();
    }
}
