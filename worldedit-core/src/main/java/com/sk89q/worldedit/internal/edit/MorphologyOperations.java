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

package com.sk89q.worldedit.internal.edit;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.internal.util.BlockVector3Set;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.registry.BlockMaterial;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.sk89q.worldedit.internal.edit.EditSupport.RECURSE_DIRECTIONS;
import static com.sk89q.worldedit.internal.edit.EditSupport.lengthSq;

/**
 * The operations of {@link EditSession} that work on the neighbourhood of
 * blocks: hollowing out a region, and eroding/dilating (morphing) terrain.
 */
public final class MorphologyOperations {

    /**
     * Implementation of {@link EditSession#hollowOutRegion(Region, int, Pattern)}.
     */
    public static int hollowOutRegion(EditSession session, Region region, int thickness, Pattern pattern)
        throws MaxChangedBlocksException {
        int affected = 0;

        final BlockVector3Set outside = new BlockVector3Set();

        final BlockVector3 min = region.getMinimumPoint();
        final BlockVector3 max = region.getMaximumPoint();

        final int minX = min.x();
        final int minY = min.y();
        final int minZ = min.z();
        final int maxX = max.x();
        final int maxY = max.y();
        final int maxZ = max.z();

        for (int x = minX; x <= maxX; ++x) {
            for (int y = minY; y <= maxY; ++y) {
                recurseHollow(session, region, BlockVector3.at(x, y, minZ), outside);
                recurseHollow(session, region, BlockVector3.at(x, y, maxZ), outside);
            }
        }

        for (int y = minY; y <= maxY; ++y) {
            for (int z = minZ; z <= maxZ; ++z) {
                recurseHollow(session, region, BlockVector3.at(minX, y, z), outside);
                recurseHollow(session, region, BlockVector3.at(maxX, y, z), outside);
            }
        }

        for (int z = minZ; z <= maxZ; ++z) {
            for (int x = minX; x <= maxX; ++x) {
                recurseHollow(session, region, BlockVector3.at(x, minY, z), outside);
                recurseHollow(session, region, BlockVector3.at(x, maxY, z), outside);
            }
        }

        final List<BlockVector3> newOutside = new ArrayList<>();
        for (int i = 1; i < thickness; ++i) {
            for (BlockVector3 position : region) {
                if (touches(position, outside)) {
                    newOutside.add(position);
                }
            }

            for (BlockVector3 position : newOutside) {
                outside.add(position);
            }
            newOutside.clear();
        }

        for (BlockVector3 position : region) {
            if (touches(position, outside)) {
                continue;
            }

            if (session.setBlock(position, pattern.applyBlock(position))) {
                ++affected;
            }
        }

        return affected;
    }

    /**
     * Check whether any face-adjacent neighbour of the position is in the set.
     */
    private static boolean touches(BlockVector3 position, BlockVector3Set set) {
        for (BlockVector3 recurseDirection : RECURSE_DIRECTIONS) {
            if (set.contains(position.add(recurseDirection))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Flood-fill the non-solid blocks reachable from the origin into {@code outside},
     * without leaving the region (blocks just outside it are included, but not
     * expanded).
     */
    private static void recurseHollow(EditSession session, Region region, BlockVector3 origin,
                                      BlockVector3Set outside) {
        var queue = new ArrayDeque<BlockVector3>();
        queue.addLast(origin);

        while (!queue.isEmpty()) {
            final BlockVector3 current = queue.removeFirst();
            // Only non-solid blocks are ever added, so a known position needs no lookup
            if (outside.contains(current)) {
                continue;
            }
            final BlockState block = session.getBlock(current);
            if (block.getBlockType().getMaterial().isSolid()) {
                continue;
            }

            outside.add(current);

            if (!region.contains(current)) {
                continue;
            }

            for (BlockVector3 recurseDirection : RECURSE_DIRECTIONS) {
                BlockVector3 neighbor = current.add(recurseDirection);
                if (!outside.contains(neighbor)) {
                    queue.addLast(neighbor);
                }
            }
        }
    }

    /**
     * Implementation of {@link EditSession#morph(BlockVector3, double, int, int, int, int)}.
     */
    public static int morph(EditSession session, BlockVector3 position, double brushSize, int minErodeFaces,
                            int numErodeIterations, int minDilateFaces, int numDilateIterations)
        throws MaxChangedBlocksException {
        int ceilBrushSize = (int) Math.ceil(brushSize);
        int bufferSize = ceilBrushSize * 2 + 3;  // + 1 due to checking the adjacent blocks, plus the 0th block
        // Store block states in a 3d array so we can do multiple mutations then commit.
        // Two are required as for each iteration, one is "current" and the other is "new"
        BlockState[][][] currentBuffer = new BlockState[bufferSize][bufferSize][bufferSize];
        BlockState[][][] nextBuffer = new BlockState[bufferSize][bufferSize][bufferSize];

        // Simply used for swapping the two
        BlockState[][][] tmp;

        // Load into buffer
        for (int x = 0; x < bufferSize; x++) {
            for (int y = 0; y < bufferSize; y++) {
                for (int z = 0; z < bufferSize; z++) {
                    BlockState blockState = session.getBlock(position.add(x - ceilBrushSize - 1, y - ceilBrushSize - 1, z - ceilBrushSize - 1));
                    currentBuffer[x][y][z] = blockState;
                    nextBuffer[x][y][z] = blockState;
                }
            }
        }

        double brushSizeSq = brushSize * brushSize;
        for (int i = 0; i < numErodeIterations; i++) {
            morphIteration(currentBuffer, nextBuffer, ceilBrushSize, brushSizeSq, false, minErodeFaces);
            // Swap current and next
            tmp = currentBuffer;
            currentBuffer = nextBuffer;
            nextBuffer = tmp;
        }

        for (int i = 0; i < numDilateIterations; i++) {
            morphIteration(currentBuffer, nextBuffer, ceilBrushSize, brushSizeSq, true, minDilateFaces);
            // Swap current and next
            tmp = currentBuffer;
            currentBuffer = nextBuffer;
            nextBuffer = tmp;
        }

        // Commit to world
        int changed = 0;
        for (int x = 0; x < bufferSize; x++) {
            for (int y = 0; y < bufferSize; y++) {
                for (int z = 0; z < bufferSize; z++) {
                    if (session.setBlock(position.add(x - ceilBrushSize - 1, y - ceilBrushSize - 1, z - ceilBrushSize - 1), currentBuffer[x][y][z])) {
                        changed++;
                    }
                }
            }
        }

        return changed;
    }

    private static boolean isEmpty(BlockState state) {
        BlockMaterial material = state.getBlockType().getMaterial();
        return material.isLiquid() || material.isAir();
    }

    /**
     * Perform one erosion or dilation step within the brush sphere, reading
     * {@code currentBuffer} and writing {@code nextBuffer}.
     *
     * <p>Erosion turns a filled block into the most common empty (air or liquid)
     * neighbour when it has at least {@code minFaces} empty neighbours. Dilation
     * is the reverse, filling an empty block with its most common filled
     * neighbour.</p>
     *
     * @param fillEmpty {@code true} to dilate, {@code false} to erode
     * @param minFaces the minimum number of neighbours of the other kind needed for a change
     */
    private static void morphIteration(BlockState[][][] currentBuffer, BlockState[][][] nextBuffer, int ceilBrushSize,
                                       double brushSizeSq, boolean fillEmpty, int minFaces) {
        Map<BlockState, Integer> blockStateFrequency = new HashMap<>();
        for (int x = 0; x <= ceilBrushSize * 2; x++) {
            for (int y = 0; y <= ceilBrushSize * 2; y++) {
                for (int z = 0; z <= ceilBrushSize * 2; z++) {
                    int realX = x - ceilBrushSize;
                    int realY = y - ceilBrushSize;
                    int realZ = z - ceilBrushSize;
                    if (lengthSq(realX, realY, realZ) > brushSizeSq) {
                        continue;
                    }

                    // Copy across changes
                    nextBuffer[x + 1][y + 1][z + 1] = currentBuffer[x + 1][y + 1][z + 1];

                    BlockState blockState = currentBuffer[x + 1][y + 1][z + 1];
                    // Erosion changes filled blocks, dilation changes empty ones
                    if (isEmpty(blockState) != fillEmpty) {
                        continue;
                    }

                    blockStateFrequency.clear();
                    int totalFaces = 0;
                    int highestFreq = 0;
                    BlockState highestState = blockState;
                    for (BlockVector3 vec3 : RECURSE_DIRECTIONS) {
                        BlockState adj = currentBuffer[x + 1 + vec3.x()][y + 1 + vec3.y()][z + 1 + vec3.z()];
                        // Only neighbours of the other kind count
                        if (isEmpty(adj) == fillEmpty) {
                            continue;
                        }

                        totalFaces++;
                        int newFreq = blockStateFrequency.getOrDefault(adj, 0) + 1;
                        blockStateFrequency.put(adj, newFreq);

                        if (newFreq > highestFreq) {
                            highestFreq = newFreq;
                            highestState = adj;
                        }
                    }

                    if (totalFaces >= minFaces) {
                        nextBuffer[x + 1][y + 1][z + 1] = highestState;
                    }
                }
            }
        }
    }

    private MorphologyOperations() {
    }
}
