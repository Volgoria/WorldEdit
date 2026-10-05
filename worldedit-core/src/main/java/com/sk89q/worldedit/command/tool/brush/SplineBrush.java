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

import com.google.common.collect.ImmutableList;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Builds a smooth tube through a series of clicked points.
 *
 * <p>Every use adds the targeted block as a control point. Using the brush
 * on the last control point again (or a block next to it) draws a
 * Catmull-Rom spline through all control points, with the brush size as the
 * tube radius, and starts a new spline. Using it again with a single
 * control point clears that point.</p>
 */
public class SplineBrush implements Brush {

    /**
     * The maximum number of control points of a single spline.
     */
    public static final int MAX_POINTS = 64;
    private static final double QUALITY = 10;

    private final boolean hollow;
    private final double tension;
    private final List<BlockVector3> points = new ArrayList<>();

    /**
     * Create a new spline brush.
     *
     * @param hollow true to only draw the shell of the tube
     * @param tension the tension of the curve, between -1 and 1; 0 gives a Catmull-Rom spline
     */
    public SplineBrush(boolean hollow, double tension) {
        checkArgument(tension >= -1 && tension <= 1, "tension must be between -1 and 1");
        this.hollow = hollow;
        this.tension = tension;
    }

    /**
     * Get the control points marked so far.
     *
     * @return an immutable copy of the control points
     */
    public List<BlockVector3> getPoints() {
        return ImmutableList.copyOf(points);
    }

    @Override
    public void build(EditSession editSession, BlockVector3 position, Pattern pattern, double size) throws MaxChangedBlocksException {
        if (points.isEmpty() || !isFinishingClick(position)) {
            if (points.size() >= MAX_POINTS) {
                points.remove(0);
            }
            points.add(position);
            return;
        }
        List<BlockVector3> nodes = List.copyOf(points);
        points.clear();
        if (nodes.size() < 2) {
            return;
        }
        editSession.drawSpline(BrushHelper.orDefault(pattern, BlockTypes.COBBLESTONE), nodes,
            tension, 0, 0, QUALITY, size, !hollow);
    }

    private boolean isFinishingClick(BlockVector3 position) {
        BlockVector3 last = points.getLast();
        BlockVector3 delta = position.subtract(last).abs();
        return delta.x() <= 1 && delta.y() <= 1 && delta.z() <= 1;
    }
}
