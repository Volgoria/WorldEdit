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

package com.sk89q.worldedit.function.block;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.function.LayerFunction;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector3;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Applies a function to the top {@code depth} layers of ground in each column
 * visited by a {@link com.sk89q.worldedit.function.visitor.LayerVisitor}.
 *
 * <p>Unlike an overlay, which places blocks on top of the ground, this
 * replaces the ground itself, e.g. to turn the top three layers of
 * terrain into sand. Only positions matching the ground mask are passed
 * to the function, so caves or overhangs within the depth are left
 * untouched.</p>
 */
public class SurfaceLayerFunction implements LayerFunction {

    private final Mask groundMask;
    private final int depth;
    private final RegionFunction function;
    private int affected;

    /**
     * Create a new instance.
     *
     * @param groundMask the mask that determines what counts as ground
     * @param depth the number of layers to apply the function to, at least 1
     * @param function the function to apply to each ground position
     */
    public SurfaceLayerFunction(Mask groundMask, int depth, RegionFunction function) {
        checkNotNull(groundMask);
        checkNotNull(function);
        checkArgument(depth >= 1, "depth must be >= 1");
        this.groundMask = groundMask;
        this.depth = depth;
        this.function = function;
    }

    /**
     * Get the number of positions the function successfully applied to.
     *
     * @return the number of affected positions
     */
    public int getAffected() {
        return affected;
    }

    @Override
    public boolean isGround(BlockVector3 position) {
        return groundMask.test(position);
    }

    @Override
    public boolean apply(BlockVector3 position, int depth) throws WorldEditException {
        if (depth >= this.depth) {
            return false;
        }
        if (groundMask.test(position) && function.apply(position)) {
            affected++;
        }
        return depth + 1 < this.depth;
    }
}
