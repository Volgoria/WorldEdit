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

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Covers the top surface of the terrain within a circular area.
 *
 * <p>For every column within the brush radius, the highest exposed block
 * between {@code y - radius} and {@code y + radius} is located. Then either
 * {@code depth} layers of the pattern are placed on top of it, or, in replace
 * mode, the surface block and the blocks below it are replaced, down to
 * {@code depth} blocks. Columns whose top is obstructed, or which have no
 * ground in range, are left alone, so overhangs and caves are not affected.</p>
 */
public class OverlayBrush implements Brush {

    private final int depth;
    private final boolean replaceSurface;

    /**
     * Create a new overlay brush.
     *
     * @param depth the number of layers to place or replace, at least 1
     * @param replaceSurface true to replace the surface instead of placing on top of it
     */
    public OverlayBrush(int depth, boolean replaceSurface) {
        checkArgument(depth >= 1, "depth must be at least 1");
        this.depth = depth;
        this.replaceSurface = replaceSurface;
    }

    @Override
    public void build(EditSession editSession, BlockVector3 position, Pattern pattern, double size) throws MaxChangedBlocksException {
        apply(editSession, position, pattern, size);
    }

    /**
     * Overlay the surface in the given extent.
     *
     * @param extent the extent
     * @param position the center of the brush
     * @param pattern the pattern to use, or {@code null} for grass blocks
     * @param size the radius of the brush
     * @throws MaxChangedBlocksException if the maximum block change limit is exceeded
     */
    public void apply(Extent extent, BlockVector3 position, Pattern pattern, double size) throws MaxChangedBlocksException {
        if (pattern == null) {
            pattern = BlockTypes.GRASS_BLOCK.getDefaultState();
        }
        BrushHelper.setBlocks(extent, findPositions(extent, position, size), pattern);
    }

    /**
     * Compute the positions this brush would change, without modifying the extent.
     *
     * @param extent the extent
     * @param position the center of the brush
     * @param size the radius of the brush
     * @return the positions to change
     */
    public List<BlockVector3> findPositions(Extent extent, BlockVector3 position, double size) {
        List<BlockVector3> positions = new ArrayList<>();
        int radius = (int) Math.floor(size);
        double radiusSq = (size + 0.5) * (size + 0.5);
        int minY = Math.max(extent.getMinimumPoint().y(), position.y() - radius);
        int maxY = Math.min(extent.getMaximumPoint().y(), position.y() + radius);

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radiusSq) {
                    continue;
                }
                int x = position.x() + dx;
                int z = position.z() + dz;
                if (!BrushHelper.isAir(extent, BlockVector3.at(x, maxY, z))) {
                    // The top of this column is buried; its surface is out of range
                    continue;
                }
                for (int y = maxY - 1; y >= minY; y--) {
                    if (BrushHelper.isAir(extent, BlockVector3.at(x, y, z))) {
                        continue;
                    }
                    addColumn(extent, positions, x, y, z);
                    break;
                }
            }
        }
        return positions;
    }

    private void addColumn(Extent extent, List<BlockVector3> positions, int x, int surfaceY, int z) {
        if (replaceSurface) {
            int lowest = Math.max(extent.getMinimumPoint().y(), surfaceY - depth + 1);
            for (int y = surfaceY; y >= lowest; y--) {
                BlockVector3 pos = BlockVector3.at(x, y, z);
                if (BrushHelper.isAir(extent, pos)) {
                    break;
                }
                positions.add(pos);
            }
        } else {
            int highest = Math.min(extent.getMaximumPoint().y(), surfaceY + depth);
            for (int y = surfaceY + 1; y <= highest; y++) {
                BlockVector3 pos = BlockVector3.at(x, y, z);
                if (!BrushHelper.isAir(extent, pos)) {
                    break;
                }
                positions.add(pos);
            }
        }
    }
}
