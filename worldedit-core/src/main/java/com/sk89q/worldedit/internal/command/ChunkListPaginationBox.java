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
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.formatting.component.PaginationBox;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkElementIndex;

/**
 * Lists the chunks of a region.
 *
 * <p>For cuboid regions, chunks are computed on demand from the chunk bounds
 * so that only the requested page is ever materialized. Other region types
 * fall back to collecting {@link Region#getChunks()}.</p>
 */
public final class ChunkListPaginationBox extends PaginationBox {
    // Cap the count so that PaginationBox page arithmetic cannot overflow.
    private static final int MAX_LISTED_CHUNKS = Integer.MAX_VALUE / 2;

    @Nullable
    private final List<BlockVector2> chunks;
    private final BlockVector2 minChunk;
    private final int lengthZ;
    private final int size;

    /**
     * Create a new instance listing the chunks of the given region.
     *
     * @param region the region
     */
    public ChunkListPaginationBox(Region region) {
        super("Selected Chunks", "/listchunks -p %page%");
        if (region instanceof CuboidRegion) {
            this.chunks = null;
            this.minChunk = region.getMinimumPoint().shr(4).toBlockVector2();
            BlockVector2 maxChunk = region.getMaximumPoint().shr(4).toBlockVector2();
            long lengthX = (long) maxChunk.x() - minChunk.x() + 1;
            this.lengthZ = maxChunk.z() - minChunk.z() + 1;
            this.size = (int) Math.min(lengthX * lengthZ, MAX_LISTED_CHUNKS);
        } else {
            this.chunks = new ArrayList<>(region.getChunks());
            this.minChunk = BlockVector2.ZERO;
            this.lengthZ = 0;
            this.size = chunks.size();
        }
    }

    /**
     * Get the chunk at the given index of the listing.
     *
     * @param number the index
     * @return the chunk position
     */
    public BlockVector2 getChunk(int number) {
        if (chunks != null) {
            return chunks.get(number);
        }
        checkElementIndex(number, size);
        return BlockVector2.at(minChunk.x() + number / lengthZ, minChunk.z() + number % lengthZ);
    }

    @Override
    public Component getComponent(int number) {
        return TextComponent.of(getChunk(number).toString());
    }

    @Override
    public int getComponentsSize() {
        return size;
    }
}
