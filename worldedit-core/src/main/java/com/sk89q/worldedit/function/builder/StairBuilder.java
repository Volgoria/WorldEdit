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

package com.sk89q.worldedit.function.builder;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.registry.state.DirectionalProperty;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.world.block.BaseBlock;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Builds straight and spiral staircases.
 *
 * <p>When the pattern places blocks that have a {@code facing} property, such
 * as stairs, they are turned to face the direction of ascent.</p>
 */
public final class StairBuilder {

    private StairBuilder() {
    }

    /**
     * Build a straight staircase.
     *
     * <p>Step {@code i} (starting at 0) is placed at
     * {@code origin + forward * i + up * i}, and is {@code width} blocks wide,
     * centred on the line of ascent.</p>
     *
     * @param extent the extent
     * @param origin the position of the first step
     * @param forward the horizontal unit vector in which the stairs go up
     * @param height the number of steps
     * @param width the width of the stairs
     * @param pattern the pattern of the steps
     * @param support the pattern used to fill under the steps, or null to leave it empty
     * @return the number of blocks changed
     * @throws WorldEditException on error, such as hitting the change limit
     */
    public static int straight(Extent extent, BlockVector3 origin, BlockVector3 forward, int height, int width,
                               Pattern pattern, @Nullable Pattern support) throws WorldEditException {
        checkNotNull(extent);
        checkNotNull(pattern);
        checkArgument(forward.y() == 0 && Math.abs(forward.x()) + Math.abs(forward.z()) == 1,
            "forward must be a horizontal unit vector");
        checkArgument(height >= 1, "height must be at least 1");
        checkArgument(width >= 1, "width must be at least 1");
        BlockVector3 right = BlockVector3.at(-forward.z(), 0, forward.x());
        Vector3 facing = forward.toVector3();
        int low = -(width - 1) / 2;
        int high = width / 2;
        int affected = 0;
        for (int step = 0; step < height; step++) {
            for (int w = low; w <= high; w++) {
                BlockVector3 pos = origin.add(forward.multiply(step)).add(right.multiply(w)).add(0, step, 0);
                if (extent.setBlock(pos, orient(pattern.applyBlock(pos), facing))) {
                    affected++;
                }
                if (support != null) {
                    for (int y = 1; y <= step; y++) {
                        BlockVector3 below = pos.subtract(0, y, 0);
                        if (extent.setBlock(below, support.applyBlock(below))) {
                            affected++;
                        }
                    }
                }
            }
        }
        return affected;
    }

    /**
     * Build a spiral staircase around a vertical axis.
     *
     * <p>Each turn is split into {@code stepsPerTurn} sectors. Step {@code i}
     * fills the sector {@code i} of the ring between the central column and
     * the radius, one block higher than the previous step.</p>
     *
     * @param extent the extent
     * @param center the bottom of the central column
     * @param radius the outer radius of the steps
     * @param height the number of steps
     * @param stepsPerTurn the number of steps in a full turn
     * @param clockwise true to go up clockwise (seen from above)
     * @param pattern the pattern of the steps
     * @param pillar the pattern of the central column, or null for no column
     * @return the number of blocks changed
     * @throws WorldEditException on error, such as hitting the change limit
     */
    public static int spiral(Extent extent, BlockVector3 center, int radius, int height, int stepsPerTurn,
                             boolean clockwise, Pattern pattern, @Nullable Pattern pillar) throws WorldEditException {
        checkNotNull(extent);
        checkNotNull(pattern);
        checkArgument(radius >= 1, "radius must be at least 1");
        checkArgument(height >= 1, "height must be at least 1");
        checkArgument(stepsPerTurn >= 2, "there must be at least 2 steps per turn");
        double radiusSq = (radius + 0.5) * (radius + 0.5);
        double sector = Math.PI * 2 / stepsPerTurn;
        int affected = 0;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x == 0 && z == 0) {
                    continue;
                }
                if (x * x + z * z > radiusSq) {
                    continue;
                }
                double angle = angleOf(x, z, clockwise);
                int sectorIndex = Math.min((int) (angle / sector), stepsPerTurn - 1);
                double middle = (sectorIndex + 0.5) * sector;
                Vector3 facing = tangent(middle, clockwise);
                for (int step = sectorIndex; step < height; step += stepsPerTurn) {
                    BlockVector3 pos = center.add(x, step, z);
                    if (extent.setBlock(pos, orient(pattern.applyBlock(pos), facing))) {
                        affected++;
                    }
                }
            }
        }
        if (pillar != null) {
            for (int y = 0; y < height; y++) {
                BlockVector3 pos = center.add(0, y, 0);
                if (extent.setBlock(pos, pillar.applyBlock(pos))) {
                    affected++;
                }
            }
        }
        return affected;
    }

    /**
     * Get the angle of a column around the axis, in [0, 2 pi), measured in
     * the direction of ascent.
     *
     * @param x the X offset
     * @param z the Z offset
     * @param clockwise true if the stairs go up clockwise
     * @return the angle
     */
    static double angleOf(int x, int z, boolean clockwise) {
        // With X east and Z south, increasing atan2(z, x) is clockwise seen from above
        double angle = Math.atan2(z, x);
        if (!clockwise) {
            angle = -angle;
        }
        if (angle < 0) {
            angle += Math.PI * 2;
        }
        return angle;
    }

    private static Vector3 tangent(double angle, boolean clockwise) {
        double actual = clockwise ? angle : -angle;
        double sign = clockwise ? 1 : -1;
        return Vector3.at(-Math.sin(actual) * sign, 0, Math.cos(actual) * sign);
    }

    /**
     * Turn a block with a {@code facing} property towards a direction.
     *
     * @param block the block
     * @param direction the direction
     * @return the oriented block, or the same block if it cannot be oriented
     */
    static BaseBlock orient(BaseBlock block, Vector3 direction) {
        Property<?> property = block.getBlockType().getPropertyMap().get("facing");
        if (property instanceof DirectionalProperty directional) {
            Direction closest = Direction.findClosest(direction, Direction.Flag.CARDINAL);
            if (closest != null && directional.values().contains(closest)) {
                return block.with(directional, closest);
            }
        }
        return block;
    }

}
