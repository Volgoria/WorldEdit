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
import com.sk89q.worldedit.world.block.BlockType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Shared helpers for brushes that compute their affected positions up front.
 */
final class BrushHelper {

    private BrushHelper() {
    }

    /**
     * Visitor for the columns of a horizontal disc.
     */
    @FunctionalInterface
    interface ColumnVisitor {
        /**
         * Visit a column.
         *
         * @param x the X coordinate of the column
         * @param z the Z coordinate of the column
         */
        void visit(int x, int z);
    }

    /**
     * Returns the given pattern, or the default state of the given block type
     * if the pattern is {@code null}.
     *
     * @param pattern the pattern, may be null
     * @param fallback the block type to use when no pattern is given
     * @return a pattern
     */
    static Pattern orDefault(@Nullable Pattern pattern, BlockType fallback) {
        return pattern != null ? pattern : fallback.getDefaultState();
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
     * Visit every column of the disc of the given radius around a center.
     *
     * <p>A column is part of the disc when its horizontal distance to the
     * center is at most {@code size + 0.5}, which matches how the other
     * WorldEdit shapes round their radius.</p>
     *
     * @param center the center of the disc
     * @param size the radius of the disc
     * @param visitor the visitor
     */
    static void forEachColumn(BlockVector3 center, double size, ColumnVisitor visitor) {
        // Every column within size + 0.5, e.g. 3 blocks away for a size of 2.6
        int radius = (int) Math.floor(size + 0.5);
        double radiusSq = (size + 0.5) * (size + 0.5);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz <= radiusSq) {
                    visitor.visit(center.x() + dx, center.z() + dz);
                }
            }
        }
    }

    /**
     * Compute every position of the ball of the given radius around a center.
     *
     * @param center the center of the ball
     * @param size the radius of the ball
     * @return the positions within the ball
     */
    static List<BlockVector3> ballPositions(BlockVector3 center, double size) {
        List<BlockVector3> positions = new ArrayList<>();
        int radius = (int) Math.ceil(size);
        double radiusSq = (size + 0.5) * (size + 0.5);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz <= radiusSq) {
                        positions.add(center.add(dx, dy, dz));
                    }
                }
            }
        }
        return positions;
    }

    /**
     * Find the topmost exposed block of every column within the given radius.
     *
     * <p>Only blocks between {@code y - radius} and {@code y + radius} are
     * considered. Columns whose top is buried (non-air at the highest
     * considered height), or which contain no ground in range, are skipped,
     * so overhangs and caves do not count as surface.</p>
     *
     * @param extent the extent
     * @param center the center of the brush
     * @param size the radius of the brush
     * @return the surface blocks, one per column at most
     */
    static List<BlockVector3> surfaceBlocks(Extent extent, BlockVector3 center, double size) {
        List<BlockVector3> surface = new ArrayList<>();
        int radius = (int) Math.floor(size);
        int minY = Math.max(extent.getMinimumPoint().y(), center.y() - radius);
        int maxY = Math.min(extent.getMaximumPoint().y(), center.y() + radius);
        forEachColumn(center, size, (x, z) -> {
            if (!isAir(extent, BlockVector3.at(x, maxY, z))) {
                // The top of this column is buried; its surface is out of range
                return;
            }
            for (int y = maxY - 1; y >= minY; y--) {
                BlockVector3 pos = BlockVector3.at(x, y, z);
                if (!isAir(extent, pos)) {
                    surface.add(pos);
                    return;
                }
            }
        });
        return surface;
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

    /**
     * Apply a pattern chosen per position.
     *
     * <p>Positions should be computed before calling this method, so that
     * the changes made here do not influence which blocks are selected.</p>
     *
     * @param extent the extent to modify
     * @param changes the pattern to apply at each position
     * @return the number of changed blocks
     * @throws MaxChangedBlocksException if the maximum block change limit is exceeded
     */
    static int setBlocks(Extent extent, Map<BlockVector3, ? extends Pattern> changes) throws MaxChangedBlocksException {
        int affected = 0;
        try {
            for (Map.Entry<BlockVector3, ? extends Pattern> entry : changes.entrySet()) {
                BlockVector3 position = entry.getKey();
                if (extent.setBlock(position, entry.getValue().applyBlock(position))) {
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
