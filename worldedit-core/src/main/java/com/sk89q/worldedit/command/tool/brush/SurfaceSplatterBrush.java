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
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Paints random irregular patches on the surface of the terrain.
 *
 * <p>A number of patch centers are picked among the surface blocks within
 * the brush radius. Each patch gets a random radius up to the patch size,
 * and its edge is roughened with per-block jitter. Surface blocks inside a
 * patch are replaced, which is useful to break up uniform ground with
 * gravel, coarse dirt, moss or flowers.</p>
 */
public class SurfaceSplatterBrush implements Brush {

    private final int patches;
    private final double patchSize;
    private final Random random;

    /**
     * Create a new surface splatter brush.
     *
     * @param patches the number of patches per use, at least 1
     * @param patchSize the maximum radius of a patch, at least 1
     */
    public SurfaceSplatterBrush(int patches, double patchSize) {
        this(patches, patchSize, new Random());
    }

    /**
     * Create a new surface splatter brush.
     *
     * @param patches the number of patches per use, at least 1
     * @param patchSize the maximum radius of a patch, at least 1
     * @param random the source of randomness
     */
    public SurfaceSplatterBrush(int patches, double patchSize, Random random) {
        checkArgument(patches >= 1, "patches must be at least 1");
        checkArgument(patchSize >= 1, "patchSize must be at least 1");
        this.patches = patches;
        this.patchSize = patchSize;
        this.random = random;
    }

    @Override
    public void build(EditSession editSession, BlockVector3 position, Pattern pattern, double size) throws MaxChangedBlocksException {
        apply(editSession, position, pattern, size);
    }

    /**
     * Splatter the surface in the given extent.
     *
     * @param extent the extent
     * @param position the center of the brush
     * @param pattern the pattern to paint, or {@code null} for gravel
     * @param size the radius of the brush
     * @throws MaxChangedBlocksException if the maximum block change limit is exceeded
     */
    public void apply(Extent extent, BlockVector3 position, Pattern pattern, double size) throws MaxChangedBlocksException {
        BrushHelper.setBlocks(extent, findPositions(extent, position, size), BrushHelper.orDefault(pattern, BlockTypes.GRAVEL));
    }

    /**
     * Compute the surface blocks to paint, without modifying the extent.
     *
     * @param extent the extent
     * @param position the center of the brush
     * @param size the radius of the brush
     * @return the positions to paint
     */
    public List<BlockVector3> findPositions(Extent extent, BlockVector3 position, double size) {
        List<BlockVector3> surface = BrushHelper.surfaceBlocks(extent, position, size);
        List<BlockVector3> positions = new ArrayList<>();
        if (surface.isEmpty()) {
            return positions;
        }
        List<BlockVector3> centers = new ArrayList<>(patches);
        List<Double> radii = new ArrayList<>(patches);
        for (int i = 0; i < patches; i++) {
            centers.add(surface.get(random.nextInt(surface.size())));
            radii.add(1 + random.nextDouble() * (patchSize - 1));
        }
        for (BlockVector3 block : surface) {
            // Jitter the patch edges so that patches look irregular
            double jitter = 0.75 + random.nextDouble() * 0.5;
            for (int i = 0; i < centers.size(); i++) {
                BlockVector3 center = centers.get(i);
                double dx = block.x() - center.x();
                double dz = block.z() - center.z();
                double radius = radii.get(i) * jitter;
                if (dx * dx + dz * dz <= radius * radius) {
                    positions.add(block);
                    break;
                }
            }
        }
        return positions;
    }
}
