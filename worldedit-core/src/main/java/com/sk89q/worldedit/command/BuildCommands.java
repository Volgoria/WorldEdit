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

package com.sk89q.worldedit.command;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.command.util.CommandPermissions;
import com.sk89q.worldedit.command.util.CommandPermissionsConditionGenerator;
import com.sk89q.worldedit.command.util.Logging;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.function.builder.BlockFont;
import com.sk89q.worldedit.function.builder.BlockText;
import com.sk89q.worldedit.function.builder.CaveCarver;
import com.sk89q.worldedit.function.builder.NoiseTerrain;
import com.sk89q.worldedit.function.builder.PathBuilder;
import com.sk89q.worldedit.function.builder.RegionMirror;
import com.sk89q.worldedit.function.builder.StairBuilder;
import com.sk89q.worldedit.function.builder.SurfaceTexturizer;
import com.sk89q.worldedit.function.builder.TerrainFlattener;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.internal.annotation.Direction;
import com.sk89q.worldedit.internal.annotation.Selection;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.noise.PerlinNoise;
import com.sk89q.worldedit.regions.ConvexPolyhedralRegion;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;
import org.enginehub.piston.annotation.param.ArgFlag;
import org.enginehub.piston.annotation.param.Switch;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.sk89q.worldedit.command.util.CommandHelper.findFreePosition;
import static com.sk89q.worldedit.command.util.CommandHelper.printAffected;
import static com.sk89q.worldedit.command.util.Logging.LogMode.ORIENTATION_REGION;
import static com.sk89q.worldedit.command.util.Logging.LogMode.PLACEMENT;
import static com.sk89q.worldedit.command.util.Logging.LogMode.REGION;
import static com.sk89q.worldedit.internal.command.CommandUtil.checkCommandArgument;

/**
 * Builder commands: text, mirroring, terrain shaping, paths, caves and stairs.
 */
@CommandContainer(superTypes = CommandPermissionsConditionGenerator.Registration.class)
public class BuildCommands {

    private static final int PATH_SEARCH_RANGE = 16;

    private final WorldEdit worldEdit;

    /**
     * Create a new instance.
     *
     * @param worldEdit reference to WorldEdit
     */
    public BuildCommands(WorldEdit worldEdit) {
        checkNotNull(worldEdit);
        this.worldEdit = worldEdit;
    }

    private static BlockState air() {
        BlockType air = checkNotNull(BlockTypes.AIR, "minecraft:air is not registered");
        return air.getDefaultState();
    }

    private static long seedOf(Integer seed) {
        return seed == null ? ThreadLocalRandom.current().nextLong() : seed;
    }

    private static boolean isHorizontalUnit(BlockVector3 vector) {
        return vector.y() == 0 && Math.abs(vector.x()) + Math.abs(vector.z()) == 1;
    }

    @Command(
        name = "/text",
        desc = "Writes text with blocks, using a bundled 5x7 font",
        descFooter = "The text is written from your position, reading towards the given direction "
            + "(by default, to your right). Use \\n to start a new line."
    )
    @CommandPermissions("worldedit.build.text")
    @Logging(PLACEMENT)
    public int text(Actor actor, LocalSession session, EditSession editSession,
                    @Arg(desc = "The pattern of blocks to write with")
                        Pattern pattern,
                    @Arg(desc = "The text to write", variable = true)
                        List<String> words,
                    @ArgFlag(name = 's', desc = "The size of a font pixel, in blocks", def = "1")
                        int size,
                    @ArgFlag(name = 't', desc = "The thickness of the letters", def = "1")
                        int thickness,
                    @ArgFlag(name = 'd', desc = "The reading direction", def = "right")
                        String direction,
                    @Switch(name = 'f', desc = "Lay the text flat on the ground instead of upright")
                        boolean flat) throws WorldEditException {
        String text = String.join(" ", words);
        if (text.isBlank()) {
            actor.printError(TranslatableComponent.of("worldedit.text.empty"));
            return 0;
        }
        checkCommandArgument(size >= 1, "Size must be at least 1");
        checkCommandArgument(thickness >= 1, "Thickness must be at least 1");
        BlockVector3 right = worldEdit.getDirection(actor instanceof Player player ? player : null, direction);
        if (!isHorizontalUnit(right)) {
            actor.printError(TranslatableComponent.of("worldedit.text.invalid-direction"));
            return 0;
        }
        BlockText writer = new BlockText(BlockFont.getDefault(), size, thickness);
        worldEdit.checkMaxRadius(writer.getTextWidth(text));
        worldEdit.checkMaxRadius(writer.getTextHeight(text));
        worldEdit.checkMaxRadius(thickness);

        // The direction pointing away from a reader standing in front of the text
        BlockVector3 forward = BlockVector3.at(right.z(), 0, -right.x());
        BlockVector3 up = flat ? forward : BlockVector3.UNIT_Y;
        BlockVector3 depth = flat ? BlockVector3.UNIT_Y : forward;

        BlockVector3 origin = session.getPlacementPosition(actor);
        int affected = writer.write(editSession, origin, pattern, text, right, up, depth);
        return printAffected(actor, "worldedit.text.created", affected);
    }

    @Command(
        name = "/symmetry",
        aliases = { "/mirrorhalf" },
        desc = "Mirrors one half of the selection onto the other half",
        descFooter = "The mirror plane goes through the centre of the selection. The half in the given "
            + "direction is overwritten with the mirror image of the opposite half."
    )
    @CommandPermissions("worldedit.build.symmetry")
    @Logging(ORIENTATION_REGION)
    public int symmetry(Actor actor, EditSession editSession,
                      @Selection Region region,
                      @Arg(desc = "The half of the selection to overwrite", def = Direction.AIM)
                      @Direction
                          BlockVector3 direction,
                      @Switch(name = 'a', desc = "Do not copy air blocks")
                          boolean skipAir) throws WorldEditException {
        int affected = RegionMirror.mirror(editSession, region, direction, skipAir);
        return printAffected(actor, "worldedit.symmetry.mirrored", affected);
    }

    @Command(
        name = "/flatten",
        desc = "Flattens the terrain in the selection to a height",
        descFooter = "Terrain above the height is removed and gaps below it are filled. "
            + "Without a height, the average surface height of the selection is used."
    )
    @CommandPermissions("worldedit.build.flatten")
    @Logging(REGION)
    public int flatten(Actor actor, EditSession editSession,
                       @Selection Region region,
                       @Arg(desc = "The height to flatten to", def = "")
                           Integer height,
                       @ArgFlag(name = 'm', desc = "The mask of blocks that make up the terrain")
                           Mask mask,
                       @ArgFlag(name = 'p', desc = "The pattern used to fill holes, instead of the column's blocks")
                           Pattern fill) throws WorldEditException {
        Mask surfaceMask = mask == null ? new ExistingBlockMask(editSession) : mask;
        TerrainFlattener flattener = new TerrainFlattener(editSession, region, surfaceMask, air(), fill);
        int minY = region.getMinimumPoint().y();
        int maxY = region.getMaximumPoint().y();
        int target;
        if (height == null) {
            OptionalInt average = flattener.getAverageSurfaceHeight();
            if (average.isEmpty()) {
                actor.printError(TranslatableComponent.of("worldedit.flatten.no-surface"));
                return 0;
            }
            target = average.getAsInt();
        } else {
            target = height;
        }
        if (target < minY || target > maxY) {
            actor.printError(TranslatableComponent.of("worldedit.flatten.invalid-height",
                TextComponent.of(minY), TextComponent.of(maxY)));
            return 0;
        }
        int affected = flattener.flatten(target);
        actor.printInfo(TranslatableComponent.of("worldedit.flatten.flattened",
            TextComponent.of(affected), TextComponent.of(target)));
        return affected;
    }

    @Command(
        name = "/terrain",
        desc = "Generates noise-based terrain in the selection",
        descFooter = "Columns are filled from the bottom of the selection up to a height given by Perlin noise. "
            + "The amplitude defaults to the height of the selection."
    )
    @CommandPermissions("worldedit.build.terrain")
    @Logging(REGION)
    public int terrain(Actor actor, EditSession editSession,
                       @Selection Region region,
                       @Arg(desc = "The pattern of the terrain")
                           Pattern pattern,
                       @Arg(desc = "The horizontal scale of the hills, in blocks", def = "32")
                           double scale,
                       @Arg(desc = "The maximum height of the terrain", def = "")
                           Integer amplitude,
                       @ArgFlag(name = 't', desc = "The pattern of the top layer")
                           Pattern topPattern,
                       @ArgFlag(name = 'o', desc = "The number of noise octaves (roughness)", def = "4")
                           int octaves,
                       @ArgFlag(name = 's', desc = "The seed of the noise")
                           Integer seed,
                       @Switch(name = 'k', desc = "Keep the blocks above the terrain instead of clearing them")
                           boolean keepAbove) throws WorldEditException {
        checkCommandArgument(scale > 0, "Scale must be positive");
        checkCommandArgument(octaves >= 1 && octaves <= 30, "Octaves must be between 1 and 30");
        int regionHeight = region.getMaximumPoint().y() - region.getMinimumPoint().y();
        int actualAmplitude = amplitude == null ? regionHeight : amplitude;
        checkCommandArgument(actualAmplitude >= 0, "Amplitude must be at least 0");

        PerlinNoise noise = new PerlinNoise();
        noise.setSeed((int) seedOf(seed));
        noise.setOctaveCount(octaves);
        NoiseTerrain terrain = new NoiseTerrain(noise, scale, actualAmplitude);
        int affected = terrain.generate(editSession, region, pattern, topPattern, keepAbove ? null : air());
        return printAffected(actor, "worldedit.terrain.generated", affected);
    }

    @Command(
        name = "/path",
        aliases = { "/road" },
        desc = "Lays a path through the points of the selection",
        descFooter = "Uses the points of a convex (//sel convex) or polygonal (//sel poly) selection, in the order "
            + "they were selected. By default the path follows the terrain surface."
    )
    @CommandPermissions("worldedit.build.path")
    @Logging(REGION)
    public int path(Actor actor, EditSession editSession,
                    @Selection Region region,
                    @Arg(desc = "The pattern of the path")
                        Pattern pattern,
                    @Arg(desc = "The width of the path", def = "3")
                        int width,
                    @ArgFlag(name = 'h', desc = "The number of blocks to clear above the path", def = "0")
                        int headroom,
                    @Switch(name = 'l', desc = "Connect the last point back to the first one")
                        boolean loop,
                    @Switch(name = 'f', desc = "Do not follow the terrain; lay the path between the points like a bridge")
                        boolean floating) throws WorldEditException {
        checkCommandArgument(width >= 1, "Width must be at least 1");
        checkCommandArgument(headroom >= 0, "Headroom must be at least 0");
        worldEdit.checkMaxRadius(width);
        worldEdit.checkMaxRadius(headroom);

        List<BlockVector3> points = new ArrayList<>();
        if (region instanceof ConvexPolyhedralRegion convex) {
            points.addAll(convex.getVertices());
        } else if (region instanceof Polygonal2DRegion polygon) {
            int y = polygon.getMinimumY();
            for (BlockVector2 point : polygon.getPoints()) {
                points.add(point.toBlockVector3(y));
            }
        } else {
            actor.printError(TranslatableComponent.of("worldedit.path.invalid-type"));
            return 0;
        }
        if (points.size() < 2) {
            actor.printError(TranslatableComponent.of("worldedit.path.too-few-points"));
            return 0;
        }

        PathBuilder builder = new PathBuilder(width, headroom, !floating, PATH_SEARCH_RANGE);
        int affected = builder.build(editSession, points, loop, pattern, air());
        return printAffected(actor, "worldedit.path.created", affected);
    }

    @Command(
        name = "/cave",
        aliases = { "/caves" },
        desc = "Carves winding caves inside the selection",
        descFooter = "Tunnels are carved by Perlin worms. The first one starts at the centre of the selection, "
            + "the others at random positions."
    )
    @CommandPermissions("worldedit.build.cave")
    @Logging(REGION)
    public int cave(Actor actor, EditSession editSession,
                    @Selection Region region,
                    @Arg(desc = "The radius of the tunnels", def = "3")
                        double size,
                    @Arg(desc = "The length of each tunnel, in blocks", def = "64")
                        int length,
                    @ArgFlag(name = 'n', desc = "The number of tunnels", def = "1")
                        int count,
                    @ArgFlag(name = 's', desc = "The seed of the random generator")
                        Integer seed) throws WorldEditException {
        checkCommandArgument(size >= 0.5, "Size must be at least 0.5");
        checkCommandArgument(length >= 1, "Length must be at least 1");
        checkCommandArgument(count >= 1 && count <= 64, "Count must be between 1 and 64");
        worldEdit.checkMaxRadius(size);

        CaveCarver carver = new CaveCarver(size, length, seedOf(seed));
        int affected = carver.carve(editSession, region, count, air());
        return printAffected(actor, "worldedit.cave.carved", affected);
    }

    @Command(
        name = "/staircase",
        aliases = { "/stairs" },
        desc = "Builds a straight staircase going up from your position",
        descFooter = "Blocks with a facing property, such as stairs, are turned to face the direction of ascent."
    )
    @CommandPermissions("worldedit.build.staircase")
    @Logging(PLACEMENT)
    public int staircase(Actor actor, LocalSession session, EditSession editSession,
                         @Arg(desc = "The pattern of the steps")
                             Pattern pattern,
                         @Arg(desc = "The number of steps")
                             int height,
                         @Arg(desc = "The width of the stairs", def = "1")
                             int width,
                         @Arg(desc = "The direction in which the stairs go up", def = Direction.AIM)
                         @Direction
                             BlockVector3 direction,
                         @ArgFlag(name = 's', desc = "The pattern used to fill under the steps")
                             Pattern support) throws WorldEditException {
        checkCommandArgument(height >= 1, "Height must be at least 1");
        checkCommandArgument(width >= 1, "Width must be at least 1");
        if (!isHorizontalUnit(direction)) {
            actor.printError(TranslatableComponent.of("worldedit.staircase.invalid-direction"));
            return 0;
        }
        worldEdit.checkMaxRadius(height);
        worldEdit.checkMaxRadius(width);

        BlockVector3 origin = session.getPlacementPosition(actor);
        int affected = StairBuilder.straight(editSession, origin, direction, height, width, pattern, support);
        return printAffected(actor, "worldedit.staircase.created", affected);
    }

    @Command(
        name = "/spiralstairs",
        aliases = { "/spiralstaircase" },
        desc = "Builds a spiral staircase around your position"
    )
    @CommandPermissions("worldedit.build.spiralstairs")
    @Logging(PLACEMENT)
    public int spiralStairs(Actor actor, LocalSession session, EditSession editSession,
                            @Arg(desc = "The pattern of the steps")
                                Pattern pattern,
                            @Arg(desc = "The outer radius of the staircase")
                                int radius,
                            @Arg(desc = "The number of steps")
                                int height,
                            @ArgFlag(name = 'p', desc = "The pattern of the central pillar")
                                Pattern pillar,
                            @ArgFlag(name = 't', desc = "The number of steps in a full turn", def = "16")
                                int stepsPerTurn,
                            @Switch(name = 'c', desc = "Go up counter-clockwise instead of clockwise")
                                boolean counterClockwise) throws WorldEditException {
        checkCommandArgument(radius >= 1, "Radius must be at least 1");
        checkCommandArgument(height >= 1, "Height must be at least 1");
        checkCommandArgument(stepsPerTurn >= 2, "There must be at least 2 steps per turn");
        worldEdit.checkMaxRadius(radius);
        worldEdit.checkMaxRadius(height);

        BlockVector3 center = session.getPlacementPosition(actor);
        int affected = StairBuilder.spiral(editSession, center, radius, height, stepsPerTurn,
            !counterClockwise, pattern, pillar);
        findFreePosition(actor);
        return printAffected(actor, "worldedit.spiralstairs.created", affected);
    }

    @Command(
        name = "/randomize",
        aliases = { "/texture" },
        desc = "Replaces a random part of the exposed blocks in the selection",
        descFooter = "A block is exposed when it touches air. Useful to add texture to walls and terrain."
    )
    @CommandPermissions("worldedit.build.randomize")
    @Logging(REGION)
    public int randomize(Actor actor, EditSession editSession,
                         @Selection Region region,
                         @Arg(desc = "The pattern of blocks to place")
                             Pattern pattern,
                         @Arg(desc = "The percentage of exposed blocks to replace", def = "20")
                             double percent,
                         @ArgFlag(name = 'm', desc = "Only replace blocks matching this mask")
                             Mask mask,
                         @ArgFlag(name = 's', desc = "The seed of the random generator")
                             Integer seed) throws WorldEditException {
        checkCommandArgument(percent >= 0 && percent <= 100, "Percent must be between 0 and 100");
        int affected = SurfaceTexturizer.apply(editSession, region, pattern, percent / 100.0, mask,
            new Random(seedOf(seed)));
        return printAffected(actor, "worldedit.randomize.changed", affected);
    }

}
