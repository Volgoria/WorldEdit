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

package com.sk89q.worldedit.command.tool.brush;

import com.google.common.collect.ImmutableMap;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.RegionMaskingFilter;
import com.sk89q.worldedit.function.block.BlockReplace;
import com.sk89q.worldedit.function.mask.BlockStateMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.MaskUnion;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.pattern.WaterloggedRemover;
import com.sk89q.worldedit.function.visitor.RegionVisitor;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.EllipsoidRegion;
import com.sk89q.worldedit.world.block.BlockTypes;

/**
 * Removes every liquid within a sphere, optionally un-waterlogging blocks.
 *
 * <p>Unlike {@code //drain}, which flood-fills a connected body of liquid,
 * this brush only affects the area under the brush, which makes it suitable
 * for carefully shaping or clearing parts of lakes and oceans.</p>
 */
public class DrainBrush implements Brush {

    private final boolean waterlogged;

    /**
     * Create a new drain brush.
     *
     * @param waterlogged true to also remove water from waterlogged blocks
     */
    public DrainBrush(boolean waterlogged) {
        this.waterlogged = waterlogged;
    }

    @Override
    public void build(EditSession editSession, BlockVector3 position, Pattern pattern, double size) throws MaxChangedBlocksException {
        Mask mask = editSession.getWorld().createLiquidMask();
        Pattern replacement;
        if (waterlogged) {
            mask = new MaskUnion(mask, new BlockStateMask(editSession, ImmutableMap.of("waterlogged", "true"), true));
            replacement = new WaterloggedRemover(editSession);
        } else {
            replacement = BlockTypes.AIR.getDefaultState();
        }
        apply(editSession, mask, replacement, position, size);
    }

    /**
     * Replace every block matching the liquid mask within a sphere.
     *
     * @param extent the extent
     * @param liquidMask the mask matching the blocks to drain
     * @param replacement the pattern used to replace drained blocks
     * @param position the center of the sphere
     * @param size the radius of the sphere
     * @throws MaxChangedBlocksException if the maximum block change limit is exceeded
     */
    public static void apply(Extent extent, Mask liquidMask, Pattern replacement, BlockVector3 position, double size)
            throws MaxChangedBlocksException {
        Operations.completeLegacy(new RegionVisitor(
            new EllipsoidRegion(position, Vector3.at(size, size, size)),
            new RegionMaskingFilter(liquidMask, new BlockReplace(extent, replacement))
        ));
    }
}
