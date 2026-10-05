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
import com.sk89q.worldedit.function.GroundFunction;
import com.sk89q.worldedit.function.LayerFunction;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.function.block.BlockReplace;
import com.sk89q.worldedit.function.block.Naturalizer;
import com.sk89q.worldedit.function.block.SnowSimulator;
import com.sk89q.worldedit.function.generator.ForestGenerator;
import com.sk89q.worldedit.function.generator.GardenPatchGenerator;
import com.sk89q.worldedit.function.generator.TreeGenerator;
import com.sk89q.worldedit.function.mask.BlockStateMask;
import com.sk89q.worldedit.function.mask.BlockTypeMask;
import com.sk89q.worldedit.function.mask.BoundedHeightMask;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.Mask2D;
import com.sk89q.worldedit.function.mask.MaskIntersection;
import com.sk89q.worldedit.function.mask.MaskUnion;
import com.sk89q.worldedit.function.mask.Masks;
import com.sk89q.worldedit.function.mask.NoiseFilter2D;
import com.sk89q.worldedit.function.mask.RegionMask;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.pattern.WaterloggedRemover;
import com.sk89q.worldedit.function.util.RegionOffset;
import com.sk89q.worldedit.function.visitor.DownwardVisitor;
import com.sk89q.worldedit.function.visitor.LayerVisitor;
import com.sk89q.worldedit.function.visitor.NonRisingVisitor;
import com.sk89q.worldedit.function.visitor.RecursiveVisitor;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.noise.RandomNoise;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.EllipsoidRegion;
import com.sk89q.worldedit.regions.FlatRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.generation.TreeType;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.sk89q.worldedit.internal.edit.EditSupport.clampedCuboid;
import static com.sk89q.worldedit.regions.Regions.asFlatRegion;
import static com.sk89q.worldedit.regions.Regions.maximumBlockY;
import static com.sk89q.worldedit.regions.Regions.minimumBlockY;

/**
 * The terrain and nature operations of {@link EditSession}: filling, draining
 * and fixing liquids, overlaying and naturalizing the ground, snow, ice and
 * grass, and generating plants and trees.
 */
public final class TerrainOperations {

    /**
     * Returned by a {@link ColumnFunction} to continue down the column.
     */
    private static final int CONTINUE = -1;

    /**
     * A function applied to the blocks of a column, from the top down, until it
     * decides to stop.
     */
    @FunctionalInterface
    private interface ColumnFunction {
        /**
         * Apply the function to a block.
         *
         * @return {@link #CONTINUE} to go on to the block below, otherwise the
         *     number of blocks affected in this column, which stops the column
         */
        int apply(BlockVector3 position, BlockState block) throws MaxChangedBlocksException;
    }

    /**
     * Implementation of {@link EditSession#fillXZ(BlockVector3, Pattern, double, int, boolean)}.
     */
    public static int fillXZ(EditSession session, BlockVector3 origin, Pattern pattern, double radius, int depth,
                             boolean recursive) throws MaxChangedBlocksException {
        checkNotNull(origin);
        checkNotNull(pattern);
        checkArgument(radius >= 0, "radius >= 0");
        checkArgument(depth >= 1, "depth >= 1");

        // Avoid int overflow (negative coordinate space allows for overflow back round to positive if the depth is large enough).
        // Depth is always 1 or greater, thus the lower bound should always be <= origin y.
        int lowerBound = origin.y() - depth + 1;
        if (lowerBound > origin.y()) {
            lowerBound = Integer.MIN_VALUE;
        }

        MaskIntersection mask = new MaskIntersection(
                sphereMask(origin, radius),
                new BoundedHeightMask(
                        Math.max(lowerBound, session.getWorld().getMinY()),
                        Math.min(session.getWorld().getMaxY(), origin.y())),
                Masks.negate(new ExistingBlockMask(session)));

        // Want to replace blocks
        BlockReplace replace = new BlockReplace(session, pattern);

        // Pick how we're going to visit blocks
        RecursiveVisitor visitor;
        if (recursive) {
            visitor = new RecursiveVisitor(mask, replace);
        } else {
            visitor = new DownwardVisitor(mask, replace, origin.y());
        }

        // Start at the origin
        visitor.visit(origin);

        // Execute
        Operations.completeLegacy(visitor);

        return visitor.getAffected();
    }

    /**
     * Implementation of {@link EditSession#drainArea(BlockVector3, double, boolean)}.
     */
    public static int drainArea(EditSession session, BlockVector3 origin, double radius, boolean waterlogged)
        throws MaxChangedBlocksException {
        checkNotNull(origin);
        checkArgument(radius >= 0, "radius >= 0 required");

        World world = session.getWorld();
        Mask waterloggedMask = null;
        if (waterlogged) {
            Map<String, String> stateMap = new HashMap<>();
            stateMap.put("waterlogged", "true");
            waterloggedMask = new BlockStateMask(session, stateMap, true);
        }
        MaskIntersection mask = new MaskIntersection(
                new BoundedHeightMask(world.getMinY(), world.getMaxY()),
                sphereMask(origin, radius),
                waterlogged ? new MaskUnion(world.createLiquidMask(), waterloggedMask)
                            : world.createLiquidMask());

        BlockReplace replace;
        if (waterlogged) {
            replace = new BlockReplace(session, new WaterloggedRemover(session));
        } else {
            replace = new BlockReplace(session, BlockTypes.AIR.getDefaultState());
        }
        RecursiveVisitor visitor = new RecursiveVisitor(mask, replace);
        visitAround(visitor, origin, mask);
        Operations.completeLegacy(visitor);

        return visitor.getAffected();
    }

    /**
     * Implementation of {@link EditSession#fixLiquid(BlockVector3, double, BlockType)}.
     */
    public static int fixLiquid(EditSession session, BlockVector3 origin, double radius, BlockType fluid)
        throws MaxChangedBlocksException {
        checkNotNull(origin);
        checkArgument(radius >= 0, "radius >= 0 required");

        // Our origins can only be liquids
        Mask liquidMask = new BlockTypeMask(session, fluid);

        // But we will also visit air blocks
        MaskIntersection blockMask = new MaskUnion(liquidMask, Masks.negate(new ExistingBlockMask(session)));

        // There are boundaries that the routine needs to stay in
        MaskIntersection mask = new MaskIntersection(
                new BoundedHeightMask(session.getWorld().getMinY(), Math.min(origin.y(), session.getWorld().getMaxY())),
                sphereMask(origin, radius),
                blockMask
        );

        BlockReplace replace = new BlockReplace(session, fluid.getDefaultState());
        NonRisingVisitor visitor = new NonRisingVisitor(mask, replace);
        visitAround(visitor, origin, liquidMask);
        Operations.completeLegacy(visitor);

        return visitor.getAffected();
    }

    private static Mask sphereMask(BlockVector3 origin, double radius) {
        return new RegionMask(new EllipsoidRegion(null, origin, Vector3.at(radius, radius, radius)));
    }

    /**
     * Start a visitor from the blocks of the 3x3x3 cube around the origin that
     * match the mask.
     */
    private static void visitAround(RecursiveVisitor visitor, BlockVector3 origin, Mask startMask) {
        for (BlockVector3 position : CuboidRegion.fromCenter(origin, 1)) {
            if (startMask.test(position)) {
                visitor.visit(position);
            }
        }
    }

    /**
     * Implementation of {@link EditSession#thaw(BlockVector3, double, int)}.
     */
    public static int thaw(EditSession session, BlockVector3 position, double radius, int height)
        throws MaxChangedBlocksException {
        BlockState air = BlockTypes.AIR.getDefaultState();
        BlockState water = BlockTypes.WATER.getDefaultState();

        return applyToColumns(session, position, radius, height, (pt, block) -> {
            BlockType id = block.getBlockType();
            if (id == BlockTypes.ICE) {
                return session.setBlock(pt, water) ? 1 : 0;
            } else if (id == BlockTypes.SNOW) {
                return session.setBlock(pt, air) ? 1 : 0;
            } else if (id.getMaterial().isAir()) {
                return CONTINUE;
            }
            return 0;
        });
    }

    /**
     * Implementation of {@link EditSession#green(BlockVector3, double, int, boolean)}.
     */
    public static int green(EditSession session, BlockVector3 position, double radius, int height,
                            boolean onlyNormalDirt) throws MaxChangedBlocksException {
        final BlockState grass = BlockTypes.GRASS_BLOCK.getDefaultState();

        return applyToColumns(session, position, radius, height, (pt, block) -> {
            BlockType type = block.getBlockType();
            if (type == BlockTypes.DIRT || (!onlyNormalDirt && type == BlockTypes.COARSE_DIRT)) {
                return session.setBlock(pt, grass) ? 1 : 0;
            } else if (type == BlockTypes.WATER || type == BlockTypes.LAVA) {
                return 0;
            } else if (type.getMaterial().isSolid()) {
                return 0;
            }
            return CONTINUE;
        });
    }

    /**
     * Apply a function down each column of a vertical cylinder, clamped to the
     * world's height limits. Each column is walked from the top down to, but
     * excluding, its lowest block.
     *
     * @param position the center of the cylinder
     * @param radius the radius of the cylinder
     * @param height how far the cylinder extends up and down from the center
     * @return the total number of blocks affected
     */
    private static int applyToColumns(EditSession session, BlockVector3 position, double radius, int height,
                                      ColumnFunction function) throws MaxChangedBlocksException {
        int affected = 0;
        final double radiusSq = radius * radius;

        final int ox = position.x();
        final int oy = position.y();
        final int oz = position.z();

        final World world = session.getWorld();
        final int centerY = Math.max(world.getMinY(), Math.min(world.getMaxY(), oy));
        final int minY = Math.max(world.getMinY(), centerY - height);
        final int maxY = Math.min(world.getMaxY(), centerY + height);

        final int ceilRadius = (int) Math.ceil(radius);
        for (int x = ox - ceilRadius; x <= ox + ceilRadius; ++x) {
            for (int z = oz - ceilRadius; z <= oz + ceilRadius; ++z) {
                if (BlockVector3.at(x, oy, z).distanceSq(position) > radiusSq) {
                    continue;
                }

                for (int y = maxY; y > minY; --y) {
                    final BlockVector3 pt = BlockVector3.at(x, y, z);
                    int result = function.apply(pt, session.getBlock(pt));
                    if (result != CONTINUE) {
                        affected += result;
                        break;
                    }
                }
            }
        }

        return affected;
    }

    /**
     * Implementation of {@link EditSession#simulateSnow(FlatRegion, boolean)}.
     */
    public static int simulateSnow(EditSession session, FlatRegion region, boolean stack)
        throws MaxChangedBlocksException {
        checkNotNull(region);

        SnowSimulator snowSimulator = new SnowSimulator(session, stack);
        LayerVisitor layerVisitor = new LayerVisitor(region, region.getMinimumY(), region.getMaximumY(), snowSimulator);
        Operations.completeLegacy(layerVisitor);
        return snowSimulator.getAffected();
    }

    /**
     * Implementation of {@link EditSession#overlayCuboidBlocks(Region, Pattern)}.
     */
    public static int overlayCuboidBlocks(EditSession session, Region region, Pattern pattern)
        throws MaxChangedBlocksException {
        checkNotNull(region);
        checkNotNull(pattern);

        BlockReplace replace = new BlockReplace(session, pattern);
        RegionOffset offset = new RegionOffset(BlockVector3.UNIT_Y, replace);
        return applyOnGround(session, region, offset, null);
    }

    /**
     * Implementation of {@link EditSession#naturalizeCuboidBlocks(Region)}.
     */
    public static int naturalizeCuboidBlocks(EditSession session, Region region) throws MaxChangedBlocksException {
        checkNotNull(region);

        Naturalizer naturalizer = new Naturalizer(session);
        visitLayers(region, naturalizer, null);
        return naturalizer.getAffected();
    }

    /**
     * Implementation of {@link EditSession#makePumpkinPatches(BlockVector3, int)}.
     */
    public static int makePumpkinPatches(EditSession session, BlockVector3 position, int apothem)
        throws MaxChangedBlocksException {
        // We want to generate pumpkins
        GardenPatchGenerator generator = new GardenPatchGenerator(session);
        generator.setPlant(GardenPatchGenerator.getPumpkinPattern());

        // In a region of the given radius
        FlatRegion region = clampedCuboid(session,
                position.add(-apothem, -5, -apothem),
                position.add(apothem, 10, apothem));
        double density = 0.02;

        return applyOnGround(session, region, generator, new NoiseFilter2D(new RandomNoise(), density));
    }

    /**
     * Implementation of {@link EditSession#makeForest(Region, double, com.sk89q.worldedit.util.TreeGenerator.TreeType)}.
     */
    @SuppressWarnings("deprecation")
    public static int makeForest(EditSession session, Region region, double density,
                                 com.sk89q.worldedit.util.TreeGenerator.TreeType treeType)
        throws MaxChangedBlocksException {
        ForestGenerator generator = new ForestGenerator(session, treeType);
        return applyOnGround(session, region, generator, new NoiseFilter2D(new RandomNoise(), density));
    }

    /**
     * Implementation of {@link EditSession#makeForest(Region, double, TreeType)}.
     */
    public static int makeForest(EditSession session, Region region, double density, TreeType treeType)
        throws MaxChangedBlocksException {
        TreeGenerator generator = new TreeGenerator(session, treeType);
        return applyOnGround(session, region, generator, new NoiseFilter2D(new RandomNoise(), density));
    }

    /**
     * Apply a function to the block above the ground in each column of a region.
     *
     * @param columnMask a mask of the columns to visit, or null to visit all of them
     * @return the number of columns the function affected
     */
    private static int applyOnGround(EditSession session, Region region, RegionFunction function,
                                     @Nullable Mask2D columnMask) throws MaxChangedBlocksException {
        GroundFunction ground = new GroundFunction(new ExistingBlockMask(session), function);
        visitLayers(region, ground, columnMask);
        return ground.getAffected();
    }

    /**
     * Visit the layers of a region (as if it were flat) from the top down.
     *
     * @param columnMask a mask of the columns to visit, or null to visit all of them
     */
    private static void visitLayers(Region region, LayerFunction function, @Nullable Mask2D columnMask)
        throws MaxChangedBlocksException {
        LayerVisitor visitor = new LayerVisitor(asFlatRegion(region), minimumBlockY(region), maximumBlockY(region), function);
        if (columnMask != null) {
            visitor.setMask(columnMask);
        }
        Operations.completeLegacy(visitor);
    }

    private TerrainOperations() {
    }
}
