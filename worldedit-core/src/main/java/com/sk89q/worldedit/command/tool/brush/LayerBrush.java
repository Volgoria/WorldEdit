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
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.Direction;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Applies a series of patterns as layers.
 *
 * <p>In surface mode (the default), the non-air blocks within the brush
 * sphere are layered by their depth below the nearest air block: blocks
 * touching air get the first pattern, blocks one block deeper get the
 * second one, and so on. Blocks deeper than the number of patterns are left
 * alone, so {@code grass_block dirt dirt} re-skins terrain with a realistic
 * profile.</p>
 *
 * <p>In concentric mode, every block within the sphere is set, and the
 * pattern depends on the distance to the center: the first pattern makes
 * the core and the last one the outer shell, like the layers of a
 * planet.</p>
 */
public class LayerBrush implements Brush {

    private static final List<BlockVector3> NEIGHBOURS = Direction.valuesOf(Direction.Flag.CARDINAL | Direction.Flag.UPRIGHT)
        .stream().map(Direction::toBlockVector).collect(ImmutableList.toImmutableList());

    private final List<Pattern> layers;
    private final boolean concentric;

    /**
     * Create a new layer brush.
     *
     * @param layers the patterns of the layers, at least one
     * @param concentric true to layer by distance to the center instead of depth below the surface
     */
    public LayerBrush(List<? extends Pattern> layers, boolean concentric) {
        checkArgument(!layers.isEmpty(), "at least one layer is required");
        this.layers = ImmutableList.copyOf(layers);
        this.concentric = concentric;
    }

    @Override
    public void build(EditSession editSession, BlockVector3 position, Pattern pattern, double size) throws MaxChangedBlocksException {
        apply(editSession, position, size);
    }

    /**
     * Apply the layers in the given extent.
     *
     * @param extent the extent
     * @param position the center of the brush
     * @param size the radius of the brush
     * @throws MaxChangedBlocksException if the maximum block change limit is exceeded
     */
    public void apply(Extent extent, BlockVector3 position, double size) throws MaxChangedBlocksException {
        Map<BlockVector3, Pattern> changes = new LinkedHashMap<>();
        findLayers(extent, position, size).forEach((pos, layer) -> changes.put(pos, layers.get(layer)));
        BrushHelper.setBlocks(extent, changes);
    }

    /**
     * Compute the layer index of each position to change, without modifying the extent.
     *
     * @param extent the extent
     * @param position the center of the brush
     * @param size the radius of the brush
     * @return the index of the layer for each position to change
     */
    public Map<BlockVector3, Integer> findLayers(Extent extent, BlockVector3 position, double size) {
        return concentric ? concentricLayers(position, size) : surfaceLayers(extent, position, size);
    }

    private Map<BlockVector3, Integer> concentricLayers(BlockVector3 position, double size) {
        Map<BlockVector3, Integer> result = new LinkedHashMap<>();
        double outer = size + 0.5;
        for (BlockVector3 pos : BrushHelper.ballPositions(position, size)) {
            int layer = (int) (pos.distance(position) / outer * layers.size());
            result.put(pos, Math.min(layer, layers.size() - 1));
        }
        return result;
    }

    private Map<BlockVector3, Integer> surfaceLayers(Extent extent, BlockVector3 position, double size) {
        Set<BlockVector3> solid = new HashSet<>();
        for (BlockVector3 pos : BrushHelper.ballPositions(position, size)) {
            if (!BrushHelper.isAir(extent, pos)) {
                solid.add(pos);
            }
        }

        // Breadth-first search from the exposed blocks inwards
        Map<BlockVector3, Integer> depth = new HashMap<>();
        Queue<BlockVector3> queue = new ArrayDeque<>();
        for (BlockVector3 pos : solid) {
            if (isExposed(extent, pos)) {
                depth.put(pos, 0);
                queue.add(pos);
            }
        }
        while (!queue.isEmpty()) {
            BlockVector3 pos = queue.remove();
            int next = depth.get(pos) + 1;
            if (next >= layers.size()) {
                continue;
            }
            for (BlockVector3 offset : NEIGHBOURS) {
                BlockVector3 neighbour = pos.add(offset);
                if (solid.contains(neighbour) && !depth.containsKey(neighbour)) {
                    depth.put(neighbour, next);
                    queue.add(neighbour);
                }
            }
        }
        return depth;
    }

    private static boolean isExposed(Extent extent, BlockVector3 pos) {
        for (BlockVector3 offset : NEIGHBOURS) {
            if (BrushHelper.isAir(extent, pos.add(offset))) {
                return true;
            }
        }
        return false;
    }
}
