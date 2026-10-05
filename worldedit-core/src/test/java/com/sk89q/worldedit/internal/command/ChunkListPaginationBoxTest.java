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

package com.sk89q.worldedit.internal.command;

import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector2;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.CylinderRegion;
import com.sk89q.worldedit.regions.Region;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChunkListPaginationBoxTest {

    private static Set<BlockVector2> listAll(ChunkListPaginationBox box) {
        Set<BlockVector2> listed = new HashSet<>();
        for (int i = 0; i < box.getComponentsSize(); i++) {
            listed.add(box.getChunk(i));
        }
        return listed;
    }

    private static void assertListsSameChunks(Region region) {
        ChunkListPaginationBox box = new ChunkListPaginationBox(region);
        Set<BlockVector2> expected = region.getChunks();
        assertEquals(expected.size(), box.getComponentsSize());
        assertEquals(expected, listAll(box));
    }

    @Test
    void cuboidListsSameChunksAsRegion() {
        assertListsSameChunks(new CuboidRegion(BlockVector3.at(-40, 0, -5), BlockVector3.at(70, 10, 33)));
    }

    @Test
    void singleChunkCuboid() {
        assertListsSameChunks(new CuboidRegion(BlockVector3.at(1, 0, 1), BlockVector3.at(2, 0, 2)));
    }

    @Test
    void nonCuboidListsSameChunksAsRegion() {
        assertListsSameChunks(new CylinderRegion(BlockVector3.at(5, 0, -7), Vector2.at(30, 20), 0, 10));
    }

    @Test
    void hugeCuboidIsNotMaterialized() {
        CuboidRegion region = new CuboidRegion(
            BlockVector3.at(-16_000_000, 0, -16_000_000), BlockVector3.at(16_000_000, 0, 16_000_000));
        ChunkListPaginationBox box = new ChunkListPaginationBox(region);

        assertEquals(Integer.MAX_VALUE / 2, box.getComponentsSize());
        assertEquals(BlockVector2.at(-1_000_000, -1_000_000), box.getChunk(0));
        assertEquals(BlockVector2.at(-1_000_000, -999_999), box.getChunk(1));
    }

    @Test
    void outOfRangeIndexIsRejected() {
        ChunkListPaginationBox box = new ChunkListPaginationBox(
            new CuboidRegion(BlockVector3.ZERO, BlockVector3.at(31, 0, 31)));
        assertEquals(4, box.getComponentsSize());
        assertThrows(IndexOutOfBoundsException.class, () -> box.getChunk(4));
        assertThrows(IndexOutOfBoundsException.class, () -> box.getChunk(-1));
    }
}
