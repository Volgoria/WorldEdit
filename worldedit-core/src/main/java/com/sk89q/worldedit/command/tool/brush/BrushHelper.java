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

import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;

/**
 * Shared helpers for brushes that compute their affected positions up front.
 */
final class BrushHelper {

    private BrushHelper() {
    }

    /**
     * Returns whether the block at the given position is air.
     *
     * @param extent the extent
     * @param position the position
     * @return true if the block is air
     */
    static boolean isAir(Extent extent, BlockVector3 position) {
        return extent.getBlock(position).getBlockType().getMaterial().isAir();
    }

    /**
     * Apply a pattern to every given position.
     *
     * <p>Positions should be computed before calling this method, so that
     * the changes made here do not influence which blocks are selected.</p>
     *
     * @param extent the extent to modify
     * @param positions the positions to set
     * @param pattern the pattern to apply
     * @return the number of changed blocks
     * @throws MaxChangedBlocksException if the maximum block change limit is exceeded
     */
    static int setBlocks(Extent extent, Iterable<BlockVector3> positions, Pattern pattern) throws MaxChangedBlocksException {
        int affected = 0;
        try {
            for (BlockVector3 position : positions) {
                if (extent.setBlock(position, pattern.applyBlock(position))) {
                    affected++;
                }
            }
        } catch (MaxChangedBlocksException e) {
            throw e;
        } catch (WorldEditException e) {
            throw new RuntimeException(e);
        }
        return affected;
    }
}
