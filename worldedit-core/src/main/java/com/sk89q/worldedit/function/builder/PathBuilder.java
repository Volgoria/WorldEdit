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
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Lays a path (or road) through a list of points.
 *
 * <p>The path is a band of the given width following the straight segments
 * between consecutive points. When following the terrain, the top block of
 * each column of the band is replaced; otherwise the path is laid at the
 * height interpolated between the points, like a bridge. Blocks above the
 * path can be cleared to give headroom.</p>
 */
public final class PathBuilder {

    private final int width;
    private final int clearance;
    private final boolean followTerrain;
    private final int searchRange;

    /**
     * Create a new path builder.
     *
     * @param width the width of the path, in blocks
     * @param clearance the number of blocks to clear above the path
     * @param followTerrain true to lay the path on the terrain surface
     * @param searchRange how far up and down to search for the surface, when following the terrain
     */
    public PathBuilder(int width, int clearance, boolean followTerrain, int searchRange) {
        checkArgument(width >= 1, "width must be at least 1");
        checkArgument(clearance >= 0, "clearance must not be negative");
        checkArgument(searchRange >= 0, "searchRange must not be negative");
        this.width = width;
        this.clearance = clearance;
        this.followTerrain = followTerrain;
        this.searchRange = searchRange;
    }

    /**
     * Get the columns covered by a path, mapped to the height of the path
     * line above them.
     *
     * @param points the points, in order
     * @param closed true to also connect the last point to the first one
     * @return the columns, in path order
     */
    public Map<BlockVector2, Integer> getColumns(List<BlockVector3> points, boolean closed) {
        checkArgument(points.size() >= 2, "a path needs at least 2 points");
        List<BlockVector3> nodes = new ArrayList<>(points);
        if (closed && points.size() > 2) {
            nodes.add(points.get(0));
        }
        int low = -(width - 1) / 2;
        int high = width / 2;
        double center = (low + high) / 2.0;
        double radiusSq = (width / 2.0) * (width / 2.0);
        List<BlockVector2> offsets = new ArrayList<>();
        for (int dx = low; dx <= high; dx++) {
            for (int dz = low; dz <= high; dz++) {
                double ox = dx - center;
                double oz = dz - center;
                if (ox * ox + oz * oz <= radiusSq) {
                    offsets.add(BlockVector2.at(dx, dz));
                }
            }
        }

        Map<BlockVector2, Integer> columns = new LinkedHashMap<>();
        for (int i = 0; i + 1 < nodes.size(); i++) {
            BlockVector3 from = nodes.get(i);
            BlockVector3 to = nodes.get(i + 1);
            BlockVector3 delta = to.subtract(from);
            int steps = Math.max(Math.max(Math.abs(delta.x()), Math.abs(delta.z())), 1);
            for (int step = 0; step <= steps; step++) {
                double t = (double) step / steps;
                double cx = from.x() + delta.x() * t;
                double cy = from.y() + delta.y() * t;
                double cz = from.z() + delta.z() * t;
                long baseX = Math.round(cx);
                long baseZ = Math.round(cz);
                int height = (int) Math.round(cy);
                for (BlockVector2 offset : offsets) {
                    columns.putIfAbsent(BlockVector2.at((int) baseX + offset.x(), (int) baseZ + offset.z()), height);
                }
            }
        }
        return columns;
    }

    /**
     * Lay a path.
     *
     * @param extent the extent
     * @param points the points, in order
     * @param closed true to also connect the last point to the first one
     * @param pattern the pattern of the path
     * @param air the block used to clear the space above the path
     * @return the number of blocks changed
     * @throws WorldEditException on error, such as hitting the change limit
     */
    public int build(Extent extent, List<BlockVector3> points, boolean closed, Pattern pattern,
                     BlockStateHolder<?> air) throws WorldEditException {
        checkNotNull(extent);
        checkNotNull(pattern);
        BaseBlock airBlock = air.toBaseBlock();
        int affected = 0;
        for (Map.Entry<BlockVector2, Integer> entry : getColumns(points, closed).entrySet()) {
            BlockVector2 column = entry.getKey();
            int y = entry.getValue();
            if (followTerrain) {
                y = findSurface(extent, column, y);
            }
            BlockVector3 pos = column.toBlockVector3(y);
            if (extent.setBlock(pos, pattern.applyBlock(pos))) {
                affected++;
            }
            for (int c = 1; c <= clearance; c++) {
                BlockVector3 above = pos.add(0, c, 0);
                if (!extent.getBlock(above).getBlockType().getMaterial().isAir() && extent.setBlock(above, airBlock)) {
                    affected++;
                }
            }
        }
        return affected;
    }

    private int findSurface(Extent extent, BlockVector2 column, int y) {
        int minY = Math.max(y - searchRange, extent.getMinimumPoint().y());
        int maxY = Math.min(y + searchRange, extent.getMaximumPoint().y());
        for (int checkY = maxY; checkY >= minY; checkY--) {
            if (!extent.getBlock(column.toBlockVector3(checkY)).getBlockType().getMaterial().isAir()) {
                return checkY;
            }
        }
        return y;
    }

}
