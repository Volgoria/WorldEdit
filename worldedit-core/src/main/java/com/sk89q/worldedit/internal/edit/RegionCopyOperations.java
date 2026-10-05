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
import com.sk89q.worldedit.extent.buffer.ForgetfulExtentBuffer;
import com.sk89q.worldedit.function.biome.BiomeReplace;
import com.sk89q.worldedit.function.block.BlockReplace;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.RegionMask;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.OperationQueue;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.visitor.RegionVisitor;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.regions.FlatRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionOperationException;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;
import com.sk89q.worldedit.world.block.BlockTypes;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.sk89q.worldedit.internal.edit.EditSupport.size;

/**
 * The operations of {@link EditSession} that copy the contents of a region
 * elsewhere: stacking and moving.
 */
public final class RegionCopyOperations {

    /**
     * Implementation of {@link EditSession#stackCuboidRegion(Region, BlockVector3, int, boolean, boolean, Mask)}.
     */
    public static int stackCuboidRegion(EditSession session, Region region, BlockVector3 offset, int count,
                                        boolean copyEntities, boolean copyBiomes, Mask mask)
        throws MaxChangedBlocksException {
        checkNotNull(region);
        checkNotNull(offset);

        try {
            return session.stackRegionBlockUnits(region, offset.multiply(size(region)), count,
                copyEntities, copyBiomes, mask);
        } catch (RegionOperationException e) {
            // Should never be able to happen
            throw new AssertionError(e);
        }
    }

    /**
     * Implementation of {@link EditSession#stackRegionBlockUnits(Region, BlockVector3, int, boolean, boolean, Mask)}.
     */
    public static int stackRegionBlockUnits(EditSession session, Region region, BlockVector3 offset, int count,
                                            boolean copyEntities, boolean copyBiomes, Mask mask)
        throws MaxChangedBlocksException, RegionOperationException {
        checkNotNull(region);
        checkNotNull(offset);
        checkArgument(count >= 1, "count >= 1 required");

        BlockVector3 size = size(region);
        BlockVector3 offsetAbs = offset.abs();
        if (offsetAbs.x() < size.x() && offsetAbs.y() < size.y() && offsetAbs.z() < size.z()) {
            throw new RegionOperationException(TranslatableComponent.of("worldedit.stack.intersecting-region"));
        }
        BlockVector3 to = region.getMinimumPoint();
        ForwardExtentCopy copy = new ForwardExtentCopy(session, region, session, to);
        copy.setRepetitions(count);
        copy.setTransform(new AffineTransform().translate(offset));
        copy.setCopyingEntities(copyEntities);
        copy.setCopyingBiomes(copyBiomes);
        if (mask != null) {
            copy.setSourceMask(mask);
        }
        Operations.completeLegacy(copy);
        return copy.getAffected();
    }

    /**
     * Implementation of {@link EditSession#moveRegion(Region, BlockVector3, int, boolean, boolean, Mask, Pattern)}.
     */
    public static int moveRegion(EditSession session, Region region, BlockVector3 offset, int multiplier,
                                 boolean moveEntities, boolean copyBiomes, Mask mask, Pattern replacement)
        throws MaxChangedBlocksException {
        checkNotNull(region);
        checkNotNull(offset);
        checkArgument(multiplier >= 1, "multiplier >= 1 required");
        checkArgument(!copyBiomes || region instanceof FlatRegion, "can't copy biomes from non-flat region");

        BlockVector3 to = region.getMinimumPoint();

        // Remove the original blocks
        Pattern pattern = replacement != null
            ? replacement
            : BlockTypes.AIR.getDefaultState();
        BlockReplace remove = new BlockReplace(session, pattern);

        // Copy to a buffer so we don't destroy our original before we can copy all the blocks from it
        ForgetfulExtentBuffer buffer = new ForgetfulExtentBuffer(session, new RegionMask(region));
        ForwardExtentCopy copy = new ForwardExtentCopy(session, region, buffer, to);
        copy.setTransform(new AffineTransform().translate(offset.multiply(multiplier)));
        copy.setSourceFunction(remove); // Remove

        copy.setCopyingEntities(moveEntities);
        copy.setRemovingEntities(moveEntities);
        copy.setCopyingBiomes(copyBiomes);

        if (mask != null) {
            copy.setSourceMask(mask);
        }

        // Then we need to copy the buffer to the world
        BlockReplace replace = new BlockReplace(session, buffer);
        RegionVisitor visitor = new RegionVisitor(buffer.asRegion(), replace);

        OperationQueue operation = new OperationQueue(copy, visitor);

        if (copyBiomes) {
            BiomeReplace biomeReplace = new BiomeReplace(session, buffer);
            RegionVisitor biomeVisitor = new RegionVisitor(buffer.asRegion(), biomeReplace);
            operation.offer(biomeVisitor);
        }

        Operations.completeLegacy(operation);

        return copy.getAffected();
    }

    private RegionCopyOperations() {
    }
}
