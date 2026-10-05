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
 * Fills holes and depressions up to the level of the targeted block.
 *
 * <p>For every column within the brush radius, air blocks are filled
 * downwards, starting at the targeted block's height, until solid ground is
 * reached. A column is only filled when ground is found within {@code depth}
 * blocks, so the brush never creates floating layers over cliffs or
 * bottomless pits.</p>
 */
public class FillBrush implements Brush {

    private final int depth;

    /**
     * Create a new fill brush.
     *
     * @param depth the maximum depth to fill down, at least 1
     */
    public FillBrush(int depth) {
        checkArgument(depth >= 1, "depth must be at least 1");
        this.depth = depth;
    }

    @Override
    public void build(EditSession editSession, BlockVector3 position, Pattern pattern, double size) throws MaxChangedBlocksException {
        apply(editSession, position, pattern, size);
    }

    /**
     * Fill depressions in the given extent.
     *
     * @param extent the extent
     * @param position the targeted block, which sets the fill level
     * @param pattern the pattern to use, or {@code null} for dirt
     * @param size the radius of the brush
     * @throws MaxChangedBlocksException if the maximum block change limit is exceeded
     */
    public void apply(Extent extent, BlockVector3 position, Pattern pattern, double size) throws MaxChangedBlocksException {
        BrushHelper.setBlocks(extent, findPositions(extent, position, size), BrushHelper.orDefault(pattern, BlockTypes.DIRT));
    }

    /**
     * Compute the positions this brush would fill, without modifying the extent.
     *
     * @param extent the extent
     * @param position the targeted block, which sets the fill level
     * @param size the radius of the brush
     * @return the positions to fill
     */
    public List<BlockVector3> findPositions(Extent extent, BlockVector3 position, double size) {
        List<BlockVector3> positions = new ArrayList<>();
        int topY = Math.min(extent.getMaximumPoint().y(), position.y());
        int bottomY = Math.max(extent.getMinimumPoint().y(), position.y() - depth);

        List<BlockVector3> column = new ArrayList<>();
        BrushHelper.forEachColumn(position, size, (x, z) -> {
            column.clear();
            for (int y = topY; y >= bottomY; y--) {
                BlockVector3 pos = BlockVector3.at(x, y, z);
                if (!BrushHelper.isAir(extent, pos)) {
                    // Reached the ground: everything above it gets filled
                    positions.addAll(column);
                    return;
                }
                column.add(pos);
            }
        });
        return positions;
    }
}
