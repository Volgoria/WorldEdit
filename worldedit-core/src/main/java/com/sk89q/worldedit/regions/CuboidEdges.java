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

package com.sk89q.worldedit.regions;

import com.sk89q.worldedit.math.BlockVector3;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Computes the twelve edges (the "wireframe") of a cuboid.
 */
public final class CuboidEdges {

    private CuboidEdges() {
    }

    /**
     * A consumer of positions that may throw.
     *
     * @param <E> the exception type
     */
    @FunctionalInterface
    public interface PositionConsumer<E extends Exception> {

        /**
         * Accept a position.
         *
         * @param position the position
         * @throws E on error
         */
        void accept(BlockVector3 position) throws E;
    }

    /**
     * Get every position lying on an edge of the cuboid spanned by the two
     * corners. Each position is returned exactly once, even where edges meet
     * or where the cuboid is flat along one or more axes.
     *
     * @param pos1 one corner
     * @param pos2 the opposite corner
     * @return the edge positions
     */
    public static List<BlockVector3> getEdgePositions(BlockVector3 pos1, BlockVector3 pos2) {
        List<BlockVector3> positions = new ArrayList<>();
        forEachEdgePosition(pos1, pos2, positions::add);
        return positions;
    }

    /**
     * Visit every position lying on an edge of the cuboid spanned by the two
     * corners, in the order of {@link #getEdgePositions(BlockVector3, BlockVector3)},
     * without collecting them, so huge cuboids do not need a huge list.
     *
     * @param pos1 one corner
     * @param pos2 the opposite corner
     * @param consumer the consumer of the positions
     * @param <E> the exception type thrown by the consumer
     * @throws E if the consumer throws
     */
    public static <E extends Exception> void forEachEdgePosition(BlockVector3 pos1, BlockVector3 pos2,
                                                                 PositionConsumer<E> consumer) throws E {
        checkNotNull(pos1);
        checkNotNull(pos2);
        checkNotNull(consumer);
        BlockVector3 min = pos1.getMinimum(pos2);
        BlockVector3 max = pos1.getMaximum(pos2);
        int[] xs = bounds(min.x(), max.x());
        int[] ys = bounds(min.y(), max.y());
        int[] zs = bounds(min.z(), max.z());

        // Edges along X, including the corners
        for (int y : ys) {
            for (int z : zs) {
                for (int x = min.x(); x <= max.x(); x++) {
                    consumer.accept(BlockVector3.at(x, y, z));
                }
            }
        }
        // Edges along Y, excluding positions already on an X edge
        for (int x : xs) {
            for (int z : zs) {
                for (int y = min.y() + 1; y < max.y(); y++) {
                    consumer.accept(BlockVector3.at(x, y, z));
                }
            }
        }
        // Edges along Z, excluding positions already on an X edge
        for (int x : xs) {
            for (int y : ys) {
                for (int z = min.z() + 1; z < max.z(); z++) {
                    consumer.accept(BlockVector3.at(x, y, z));
                }
            }
        }
    }

    /**
     * Get the edge positions of the bounding box of a region.
     *
     * @param region the region
     * @return the edge positions
     */
    public static List<BlockVector3> getEdgePositions(Region region) {
        checkNotNull(region);
        return getEdgePositions(region.getMinimumPoint(), region.getMaximumPoint());
    }

    private static int[] bounds(int min, int max) {
        return IntStream.of(min, max).distinct().toArray();
    }
}
