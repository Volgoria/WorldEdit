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
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.function.RegionMaskingFilter;
import com.sk89q.worldedit.function.block.BlockReplace;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.MathUtils;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.shape.ArbitraryShape;
import com.sk89q.worldedit.regions.shape.RegionShape;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockTypes;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.sk89q.worldedit.internal.edit.EditSupport.clampedCuboid;

/**
 * The region-filling operations of {@link EditSession}: setting, replacing and
 * removing blocks, and making the faces, walls or center of a region.
 */
public final class RegionOperations {

    /**
     * Implementation of {@link EditSession#removeAbove(BlockVector3, int, int)}.
     */
    public static int removeAbove(EditSession session, BlockVector3 position, int apothem, int height)
        throws MaxChangedBlocksException {
        checkNotNull(position);
        checkArgument(apothem >= 1, "apothem >= 1");
        checkArgument(height >= 1, "height >= 1");

        return removeColumn(session, position, apothem, height - 1);
    }

    /**
     * Implementation of {@link EditSession#removeBelow(BlockVector3, int, int)}.
     */
    public static int removeBelow(EditSession session, BlockVector3 position, int apothem, int height)
        throws MaxChangedBlocksException {
        checkNotNull(position);
        checkArgument(apothem >= 1, "apothem >= 1");
        checkArgument(height >= 1, "height >= 1");

        return removeColumn(session, position, apothem, -height + 1);
    }

    /**
     * Remove the cuboid of the given apothem on the XZ plane, spanning from the
     * position's Y to the position's Y plus {@code heightOffset}.
     */
    private static int removeColumn(EditSession session, BlockVector3 position, int apothem, int heightOffset)
        throws MaxChangedBlocksException {
        Region region = clampedCuboid(session,
                position.add(-apothem + 1, 0, -apothem + 1),
                position.add(apothem - 1, heightOffset, apothem - 1));
        return session.setBlocks(region, BlockTypes.AIR.getDefaultState());
    }

    /**
     * Implementation of {@link EditSession#removeNear(BlockVector3, Mask, int)}.
     */
    public static int removeNear(EditSession session, BlockVector3 position, Mask mask, int apothem)
        throws MaxChangedBlocksException {
        checkNotNull(position);
        checkArgument(apothem >= 1, "apothem >= 1");

        BlockVector3 adjustment = BlockVector3.ONE.multiply(apothem - 1);
        Region region = clampedCuboid(session,
                position.add(adjustment.multiply(-1)),
                position.add(adjustment));
        return session.replaceBlocks(region, mask, BlockTypes.AIR.getDefaultState());
    }

    /**
     * Implementation of {@link EditSession#setBlocks(Region, Pattern)}.
     */
    public static int setBlocks(EditSession session, Region region, Pattern pattern) throws MaxChangedBlocksException {
        checkNotNull(region);
        checkNotNull(pattern);

        return EditSupport.apply(region, new BlockReplace(session, pattern));
    }

    /**
     * Implementation of {@link EditSession#replaceBlocks(Region, Mask, Pattern)}.
     */
    public static int replaceBlocks(EditSession session, Region region, Mask mask, Pattern pattern)
        throws MaxChangedBlocksException {
        checkNotNull(region);
        checkNotNull(mask);
        checkNotNull(pattern);

        BlockReplace replace = new BlockReplace(session, pattern);
        return EditSupport.apply(region, new RegionMaskingFilter(mask, replace));
    }

    /**
     * Implementation of {@link EditSession#center(Region, Pattern)}.
     */
    public static int center(EditSession session, Region region, Pattern pattern) throws MaxChangedBlocksException {
        checkNotNull(region);
        checkNotNull(pattern);

        Vector3 center = region.getCenter();
        Region centerRegion = clampedCuboid(session,
                BlockVector3.at(((int) center.x()), ((int) center.y()), ((int) center.z())),
                BlockVector3.at(
                        MathUtils.roundHalfUp(center.x()),
                        MathUtils.roundHalfUp(center.y()),
                        MathUtils.roundHalfUp(center.z())));
        return session.setBlocks(centerRegion, pattern);
    }

    /**
     * Implementation of {@link EditSession#makeCuboidFaces(Region, Pattern)}.
     */
    public static int makeCuboidFaces(EditSession session, Region region, Pattern pattern)
        throws MaxChangedBlocksException {
        checkNotNull(region);
        checkNotNull(pattern);

        CuboidRegion cuboid = CuboidRegion.makeCuboid(region);
        return session.setBlocks(cuboid.getFaces(), pattern);
    }

    /**
     * Implementation of {@link EditSession#makeFaces(Region, Pattern)}.
     */
    public static int makeFaces(EditSession session, final Region region, Pattern pattern)
        throws MaxChangedBlocksException {
        checkNotNull(region);
        checkNotNull(pattern);

        if (region instanceof CuboidRegion) {
            return session.makeCuboidFaces(region, pattern);
        } else {
            return new RegionShape(region).generate(session, pattern, true);
        }
    }

    /**
     * Implementation of {@link EditSession#makeCuboidWalls(Region, Pattern)}.
     */
    public static int makeCuboidWalls(EditSession session, Region region, Pattern pattern)
        throws MaxChangedBlocksException {
        checkNotNull(region);
        checkNotNull(pattern);

        CuboidRegion cuboid = CuboidRegion.makeCuboid(region);
        return session.setBlocks(cuboid.getWalls(), pattern);
    }

    /**
     * Implementation of {@link EditSession#makeWalls(Region, Pattern)}.
     */
    public static int makeWalls(EditSession session, final Region region, Pattern pattern)
        throws MaxChangedBlocksException {
        checkNotNull(region);
        checkNotNull(pattern);

        if (region instanceof CuboidRegion) {
            return session.makeCuboidWalls(region, pattern);
        } else {
            final int minY = region.getMinimumPoint().y();
            final int maxY = region.getMaximumPoint().y();
            final ArbitraryShape shape = new RegionShape(region) {
                @Override
                protected BaseBlock getMaterial(int x, int y, int z, BaseBlock defaultMaterial) {
                    if (y > maxY || y < minY) {
                        // Put holes into the floor and ceiling by telling ArbitraryShape that the shape goes on outside the region
                        return defaultMaterial;
                    }

                    return super.getMaterial(x, y, z, defaultMaterial);
                }
            };
            return shape.generate(session, pattern, true);
        }
    }

    private RegionOperations() {
    }
}
