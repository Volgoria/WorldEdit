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
import com.sk89q.worldedit.extent.transform.BlockTransformExtent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BaseBlock;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Mirrors one half of a region onto the other half, in place.
 *
 * <p>The mirror plane goes through the centre of the bounding box of the
 * region and is perpendicular to the given direction. Blocks on the side
 * opposite to the direction are copied onto the side the direction points
 * to, with their orientation (stairs, doors, ...) flipped accordingly.</p>
 */
public final class RegionMirror {

    private RegionMirror() {
    }

    /**
     * Mirror a region.
     *
     * @param extent the extent to read from and write to
     * @param region the region
     * @param direction an axis-aligned unit vector pointing to the half that is overwritten
     * @param skipAir true to not copy air blocks from the source half
     * @return the number of blocks changed
     * @throws WorldEditException on error, such as hitting the change limit
     */
    public static int mirror(Extent extent, Region region, BlockVector3 direction, boolean skipAir)
        throws WorldEditException {
        checkNotNull(extent);
        checkNotNull(region);
        checkNotNull(direction);
        int axis = axisOf(direction);
        int sign = direction.x() + direction.y() + direction.z();

        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        // Twice the coordinate of the mirror plane, to stay in integers
        int doubleCenter = component(min, axis) + component(max, axis);

        Transform flip = new AffineTransform().scale(
            axis == 0 ? -1 : 1, axis == 1 ? -1 : 1, axis == 2 ? -1 : 1);

        int affected = 0;
        for (BlockVector3 source : region) {
            int coordinate = component(source, axis);
            // The source is strictly on the side opposite to the direction
            if (Integer.signum(2 * coordinate - doubleCenter) != -sign) {
                continue;
            }
            BlockVector3 target = withComponent(source, axis, doubleCenter - coordinate);
            if (!region.contains(target)) {
                continue;
            }
            BaseBlock block = extent.getFullBlock(source);
            if (skipAir && block.getBlockType().getMaterial().isAir()) {
                continue;
            }
            if (extent.setBlock(target, BlockTransformExtent.transform(block, flip))) {
                affected++;
            }
        }
        return affected;
    }

    private static int axisOf(BlockVector3 direction) {
        int x = Math.abs(direction.x());
        int y = Math.abs(direction.y());
        int z = Math.abs(direction.z());
        checkArgument(x + y + z == 1, "The direction must be an axis-aligned unit vector");
        return x == 1 ? 0 : y == 1 ? 1 : 2;
    }

    private static int component(BlockVector3 vector, int axis) {
        return switch (axis) {
            case 0 -> vector.x();
            case 1 -> vector.y();
            default -> vector.z();
        };
    }

    private static BlockVector3 withComponent(BlockVector3 vector, int axis, int value) {
        return switch (axis) {
            case 0 -> vector.withX(value);
            case 1 -> vector.withY(value);
            default -> vector.withZ(value);
        };
    }

}
