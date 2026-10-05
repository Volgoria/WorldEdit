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
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockTypes;

import javax.annotation.Nullable;

/**
 * Draws lines between successively targeted blocks.
 *
 * <p>The first use marks the start point without changing anything; the
 * second use draws a line from the start point to the targeted block. In
 * chain mode, the end of each line becomes the start of the next one, which
 * allows drawing connected paths such as fences, cables or walls.</p>
 */
public class LineBrush implements Brush {

    private final boolean chain;
    private final boolean hollow;
    @Nullable
    private BlockVector3 anchor;

    /**
     * Create a new line brush.
     *
     * @param chain true to start each line where the previous one ended
     * @param hollow true to only draw the shell of thick lines
     */
    public LineBrush(boolean chain, boolean hollow) {
        this.chain = chain;
        this.hollow = hollow;
    }

    /**
     * Get the start point of the next line, if one has been marked.
     *
     * @return the start point, or {@code null} if the next use marks one
     */
    @Nullable
    public BlockVector3 getAnchor() {
        return anchor;
    }

    @Override
    public void build(EditSession editSession, BlockVector3 position, Pattern pattern, double size) throws MaxChangedBlocksException {
        BlockVector3 start = anchor;
        if (start == null) {
            anchor = position;
            return;
        }
        anchor = chain ? position : null;
        if (pattern == null) {
            pattern = BlockTypes.COBBLESTONE.getDefaultState();
        }
        editSession.drawLine(pattern, start, position, size, !hollow);
    }
}
