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
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Copies a sphere of blocks at the first use and pastes it at the next use.
 *
 * <p>The copy is centered on the targeted block and keeps full block data,
 * including NBT. Unless the brush keeps its copy, every paste clears it, so
 * the following use copies again.</p>
 */
public class CopyPasteBrush implements Brush {

    private final boolean ignoreAir;
    private final boolean keepCopy;
    @Nullable
    private Map<BlockVector3, BaseBlock> copied;

    /**
     * Create a new copy-paste brush.
     *
     * @param ignoreAir true to not paste the air blocks of the copy
     * @param keepCopy true to keep pasting the first copy instead of copying again after each paste
     */
    public CopyPasteBrush(boolean ignoreAir, boolean keepCopy) {
        this.ignoreAir = ignoreAir;
        this.keepCopy = keepCopy;
    }

    /**
     * Returns whether the brush currently holds a copy, so that its next use pastes.
     *
     * @return true if the next use pastes
     */
    public boolean hasCopy() {
        return copied != null;
    }

    @Override
    public void build(EditSession editSession, BlockVector3 position, Pattern pattern, double size) throws MaxChangedBlocksException {
        apply(editSession, position, size);
    }

    /**
     * Copy or paste at the given position.
     *
     * @param extent the extent
     * @param position the targeted block
     * @param size the radius of the copied sphere
     * @return the number of changed blocks, 0 when copying
     * @throws MaxChangedBlocksException if the maximum block change limit is exceeded
     */
    public int apply(Extent extent, BlockVector3 position, double size) throws MaxChangedBlocksException {
        Map<BlockVector3, BaseBlock> copy = copied;
        if (copy == null) {
            copied = copy(extent, position, size);
            return 0;
        }
        if (!keepCopy) {
            copied = null;
        }
        return paste(extent, position, copy);
    }

    private static Map<BlockVector3, BaseBlock> copy(Extent extent, BlockVector3 center, double size) {
        Map<BlockVector3, BaseBlock> copy = new LinkedHashMap<>();
        for (BlockVector3 pos : BrushHelper.ballPositions(center, size)) {
            copy.put(pos.subtract(center), extent.getFullBlock(pos));
        }
        return copy;
    }

    private int paste(Extent extent, BlockVector3 center, Map<BlockVector3, BaseBlock> copy) throws MaxChangedBlocksException {
        int affected = 0;
        try {
            for (Map.Entry<BlockVector3, BaseBlock> entry : copy.entrySet()) {
                BaseBlock block = entry.getValue();
                if (ignoreAir && block.getBlockType().getMaterial().isAir()) {
                    continue;
                }
                if (extent.setBlock(center.add(entry.getKey()), block)) {
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
