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

package com.sk89q.worldedit.function.generator.shape;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A shape that can be generated around an origin.
 *
 * <p>Shapes are described by a membership test on block offsets relative to
 * the origin, within a bounding box. Blocks are placed with
 * {@link Extent#setBlock(BlockVector3, com.sk89q.worldedit.world.block.BlockStateHolder)},
 * so when the extent is an {@link com.sk89q.worldedit.EditSession}, history
 * and change limits are respected.</p>
 */
public abstract class GeneratedShape {

    private final boolean hollow;

    /**
     * Create a new shape.
     *
     * @param hollow true to only generate the outer surface of the shape
     */
    protected GeneratedShape(boolean hollow) {
        this.hollow = hollow;
    }

    /**
     * Get whether this shape is hollow.
     *
     * @return true if only the surface is generated
     */
    public boolean isHollow() {
        return hollow;
    }

    /**
     * Get the minimum offset (inclusive) of the bounding box of this shape.
     *
     * @return the minimum offset
     */
    public abstract BlockVector3 getMinimumOffset();

    /**
     * Get the maximum offset (inclusive) of the bounding box of this shape.
     *
     * @return the maximum offset
     */
    public abstract BlockVector3 getMaximumOffset();

    /**
     * Test whether the block at the given offset is part of the solid shape.
     *
     * @param x the x offset
     * @param y the y offset
     * @param z the z offset
     * @return true if the offset is inside the shape
     */
    protected abstract boolean contains(int x, int y, int z);

    /**
     * Test used to decide whether a block is on the surface when the shape is
     * hollow. A block is on the surface when one of its six neighbours does
     * not satisfy this test.
     *
     * <p>Override this to leave a face of the shape open (for example the
     * floor of a dome).</p>
     *
     * @param x the x offset
     * @param y the y offset
     * @param z the z offset
     * @return true if the offset is considered inside for the hollow test
     */
    protected boolean containsForHollow(int x, int y, int z) {
        return contains(x, y, z);
    }

    /**
     * Test whether a block is generated at the given offset, taking hollowness
     * into account.
     *
     * @param x the x offset
     * @param y the y offset
     * @param z the z offset
     * @return true if a block is generated at this offset
     */
    public boolean isGenerated(int x, int y, int z) {
        if (!contains(x, y, z)) {
            return false;
        }
        if (!hollow) {
            return true;
        }
        return !containsForHollow(x + 1, y, z) || !containsForHollow(x - 1, y, z)
            || !containsForHollow(x, y + 1, z) || !containsForHollow(x, y - 1, z)
            || !containsForHollow(x, y, z + 1) || !containsForHollow(x, y, z - 1);
    }

    /**
     * Get all offsets, relative to the origin, at which blocks are generated.
     *
     * @return a set of offsets, in generation order
     */
    public Set<BlockVector3> getOffsets() {
        Set<BlockVector3> offsets = new LinkedHashSet<>();
        BlockVector3 min = getMinimumOffset();
        BlockVector3 max = getMaximumOffset();
        for (int x = min.x(); x <= max.x(); x++) {
            for (int y = min.y(); y <= max.y(); y++) {
                for (int z = min.z(); z <= max.z(); z++) {
                    if (isGenerated(x, y, z)) {
                        offsets.add(BlockVector3.at(x, y, z));
                    }
                }
            }
        }
        return offsets;
    }

    /**
     * Generate this shape.
     *
     * @param extent the extent to place blocks in
     * @param origin the origin of the shape
     * @param pattern the pattern of blocks to place
     * @return the number of blocks changed
     * @throws WorldEditException thrown on error, such as hitting the change limit
     */
    public int generate(Extent extent, BlockVector3 origin, Pattern pattern) throws WorldEditException {
        int affected = 0;
        BlockVector3 min = getMinimumOffset();
        BlockVector3 max = getMaximumOffset();
        for (int x = min.x(); x <= max.x(); x++) {
            for (int y = min.y(); y <= max.y(); y++) {
                for (int z = min.z(); z <= max.z(); z++) {
                    if (isGenerated(x, y, z)) {
                        BlockVector3 position = origin.add(x, y, z);
                        if (extent.setBlock(position, pattern.applyBlock(position))) {
                            affected++;
                        }
                    }
                }
            }
        }
        return affected;
    }

}
