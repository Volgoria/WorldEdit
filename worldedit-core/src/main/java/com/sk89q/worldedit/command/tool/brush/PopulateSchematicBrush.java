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
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Scatters random schematics over the surface of the terrain.
 *
 * <p>Every surface column within the brush radius has a chance, given by
 * the density, to receive a schematic picked at random from the brush's
 * list. Placements are kept at least {@code spacing} blocks apart. Each
 * schematic is pasted centered horizontally on its column, with its lowest
 * layer resting on the surface, and can be rotated randomly by a multiple
 * of 90 degrees, which makes this brush ideal for trees, rocks and other
 * decorations.</p>
 */
public class PopulateSchematicBrush implements Brush {

    /**
     * A planned schematic placement.
     *
     * @param position the block the bottom center of the schematic is placed at
     * @param schematic the schematic to paste
     * @param rotation the rotation around the Y axis, in degrees
     */
    public record Placement(BlockVector3 position, ClipboardHolder schematic, int rotation) {
    }

    private final List<ClipboardHolder> schematics;
    private final double density;
    private final int spacing;
    private final boolean randomRotation;
    private final boolean ignoreAir;
    private final Random random;

    /**
     * Create a new schematic population brush.
     *
     * @param schematics the schematics to pick from, at least one
     * @param density the chance of a surface column to receive a schematic, between 0 and 100
     * @param spacing the minimum horizontal distance between two placements, at least 0
     * @param randomRotation true to randomly rotate each schematic
     * @param ignoreAir true to not paste the air blocks of the schematics
     */
    public PopulateSchematicBrush(List<ClipboardHolder> schematics, double density, int spacing,
                                  boolean randomRotation, boolean ignoreAir) {
        this(schematics, density, spacing, randomRotation, ignoreAir, new Random());
    }

    /**
     * Create a new schematic population brush.
     *
     * @param schematics the schematics to pick from, at least one
     * @param density the chance of a surface column to receive a schematic, between 0 and 100
     * @param spacing the minimum horizontal distance between two placements, at least 0
     * @param randomRotation true to randomly rotate each schematic
     * @param ignoreAir true to not paste the air blocks of the schematics
     * @param random the source of randomness
     */
    public PopulateSchematicBrush(List<ClipboardHolder> schematics, double density, int spacing,
                                  boolean randomRotation, boolean ignoreAir, Random random) {
        checkArgument(!schematics.isEmpty(), "at least one schematic is required");
        checkArgument(density >= 0 && density <= 100, "density must be between 0 and 100");
        checkArgument(spacing >= 0, "spacing must not be negative");
        this.schematics = ImmutableList.copyOf(schematics);
        this.density = density;
        this.spacing = spacing;
        this.randomRotation = randomRotation;
        this.ignoreAir = ignoreAir;
        this.random = random;
    }

    @Override
    public void build(EditSession editSession, BlockVector3 position, Pattern pattern, double size) throws MaxChangedBlocksException {
        apply(editSession, position, size);
    }

    /**
     * Populate the surface in the given extent.
     *
     * @param extent the extent
     * @param position the center of the brush
     * @param size the radius of the brush
     * @return the number of pasted schematics
     * @throws MaxChangedBlocksException if the maximum block change limit is exceeded
     */
    public int apply(Extent extent, BlockVector3 position, double size) throws MaxChangedBlocksException {
        List<Placement> placements = findPlacements(extent, position, size);
        for (Placement placement : placements) {
            paste(extent, placement, ignoreAir);
        }
        return placements.size();
    }

    /**
     * Plan the placements of the schematics, without modifying the extent.
     *
     * @param extent the extent
     * @param position the center of the brush
     * @param size the radius of the brush
     * @return the placements
     */
    public List<Placement> findPlacements(Extent extent, BlockVector3 position, double size) {
        List<BlockVector3> surface = new ArrayList<>(BrushHelper.surfaceBlocks(extent, position, size));
        Collections.shuffle(surface, random);
        List<Placement> placements = new ArrayList<>();
        int spacingSq = spacing * spacing;
        for (BlockVector3 ground : surface) {
            if (random.nextDouble() * 100 >= density) {
                continue;
            }
            BlockVector3 target = ground.add(0, 1, 0);
            boolean crowded = false;
            for (Placement other : placements) {
                int dx = other.position().x() - target.x();
                int dz = other.position().z() - target.z();
                if (dx * dx + dz * dz < spacingSq) {
                    crowded = true;
                    break;
                }
            }
            if (crowded) {
                continue;
            }
            ClipboardHolder schematic = schematics.get(random.nextInt(schematics.size()));
            int rotation = randomRotation ? 90 * random.nextInt(4) : 0;
            placements.add(new Placement(target, schematic, rotation));
        }
        return placements;
    }

    /**
     * Paste a planned placement.
     *
     * @param extent the extent
     * @param placement the placement
     * @param ignoreAir true to not paste air blocks
     * @throws MaxChangedBlocksException if the maximum block change limit is exceeded
     */
    public static void paste(Extent extent, Placement placement, boolean ignoreAir) throws MaxChangedBlocksException {
        Clipboard clipboard = placement.schematic().getClipboard();
        Transform transform = placement.schematic().getTransform();
        if (placement.rotation() != 0) {
            transform = transform.combine(new AffineTransform().rotateY(placement.rotation()));
        }
        ClipboardHolder holder = new ClipboardHolder(clipboard);
        holder.setTransform(transform);

        // Put the bottom center of the schematic on the target block
        Region region = clipboard.getRegion();
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        Vector3 bottomCenter = Vector3.at((min.x() + max.x()) / 2.0, min.y(), (min.z() + max.z()) / 2.0);
        Vector3 offset = transform.apply(bottomCenter.subtract(clipboard.getOrigin().toVector3()));
        BlockVector3 to = placement.position().subtract(offset.round().toBlockPoint());

        Operations.completeLegacy(holder.createPaste(extent)
            .to(to)
            .ignoreAirBlocks(ignoreAir)
            // Schematic entities would be spawned on every click, outside of any block limit
            .copyEntities(false)
            .build());
    }
}
