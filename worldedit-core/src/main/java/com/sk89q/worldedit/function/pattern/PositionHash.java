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

package com.sk89q.worldedit.function.pattern;

import com.sk89q.worldedit.math.BlockVector3;

/**
 * Deterministic, well-distributed hashing of block positions, used by
 * patterns that need repeatable "random" decisions per block.
 */
final class PositionHash {

    private PositionHash() {
    }

    /**
     * Hash a position into a number in the range {@code [0, 1)}.
     *
     * <p>The same position always yields the same value.</p>
     *
     * @param position the position
     * @return a value in {@code [0, 1)}
     */
    static double unitHash(BlockVector3 position) {
        long h = position.x() * 0x9E3779B97F4A7C15L;
        h ^= position.y() * 0xC2B2AE3D27D4EB4FL;
        h ^= position.z() * 0x165667B19E3779F9L;
        // MurmurHash3 64-bit finalizer
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return (h >>> 11) * 0x1.0p-53;
    }
}
