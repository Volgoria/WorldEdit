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
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.Direction;

/**
 * Small helpers shared by the {@link EditSession} operation classes.
 */
final class EditSupport {

    /**
     * The six face-adjacent directions, in the order the operations visit them.
     */
    static final BlockVector3[] RECURSE_DIRECTIONS = {
        Direction.NORTH.toBlockVector(),
        Direction.EAST.toBlockVector(),
        Direction.SOUTH.toBlockVector(),
        Direction.WEST.toBlockVector(),
        Direction.UP.toBlockVector(),
        Direction.DOWN.toBlockVector(),
    };

    static double lengthSq(double x, double y, double z) {
        return (x * x) + (y * y) + (z * z);
    }

    static double lengthSq(double x, double z) {
        return (x * x) + (z * z);
    }

    /**
     * Set the given positions to the pattern.
     *
     * @return the number of positions for which the block set call returned true
     */
    static int setBlocks(EditSession session, Iterable<BlockVector3> positions, Pattern pattern)
        throws MaxChangedBlocksException {
        int affected = 0;
        for (BlockVector3 position : positions) {
            if (session.setBlock(position, pattern)) {
                ++affected;
            }
        }
        return affected;
    }

    /**
     * Set the four positions {@code center + (±x, y, ±z)}.
     *
     * @return the number of block set calls that returned true
     */
    static int setMirroredXZ(EditSession session, BlockVector3 center, int x, int y, int z, Pattern pattern)
        throws MaxChangedBlocksException {
        int affected = 0;
        if (session.setBlock(center.add(x, y, z), pattern)) {
            ++affected;
        }
        if (session.setBlock(center.add(-x, y, z), pattern)) {
            ++affected;
        }
        if (session.setBlock(center.add(x, y, -z), pattern)) {
            ++affected;
        }
        if (session.setBlock(center.add(-x, y, -z), pattern)) {
            ++affected;
        }
        return affected;
    }

    /**
     * Set the eight positions {@code center + (±x, ±y, ±z)}.
     *
     * @return the number of block set calls that returned true
     */
    static int setMirroredXYZ(EditSession session, BlockVector3 center, int x, int y, int z, Pattern pattern)
        throws MaxChangedBlocksException {
        int affected = 0;
        if (session.setBlock(center.add(x, y, z), pattern)) {
            ++affected;
        }
        if (session.setBlock(center.add(-x, y, z), pattern)) {
            ++affected;
        }
        if (session.setBlock(center.add(x, -y, z), pattern)) {
            ++affected;
        }
        if (session.setBlock(center.add(x, y, -z), pattern)) {
            ++affected;
        }
        if (session.setBlock(center.add(-x, -y, z), pattern)) {
            ++affected;
        }
        if (session.setBlock(center.add(x, -y, -z), pattern)) {
            ++affected;
        }
        if (session.setBlock(center.add(-x, y, -z), pattern)) {
            ++affected;
        }
        if (session.setBlock(center.add(-x, -y, -z), pattern)) {
            ++affected;
        }
        return affected;
    }

    private EditSupport() {
    }
}
