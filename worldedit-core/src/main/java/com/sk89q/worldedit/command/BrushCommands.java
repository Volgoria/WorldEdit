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

import com.google.common.base.Splitter;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.MaxBrushRadiusException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.command.argument.HeightConverter;
import com.sk89q.worldedit.command.factory.FeatureGeneratorFactory;
import com.sk89q.worldedit.command.factory.ReplaceFactory;
import com.sk89q.worldedit.command.factory.TreeGeneratorFactory;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.command.tool.InvalidToolBindException;
import com.sk89q.worldedit.command.tool.brush.BlobBrush;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.command.tool.brush.ButcherBrush;
import com.sk89q.worldedit.command.tool.brush.ClipboardBrush;
import com.sk89q.worldedit.command.tool.brush.CommandBrush;
import com.sk89q.worldedit.command.tool.brush.CopyPasteBrush;
import com.sk89q.worldedit.command.tool.brush.CylinderBrush;
import com.sk89q.worldedit.command.tool.brush.DrainBrush;
import com.sk89q.worldedit.command.tool.brush.FillBrush;
import com.sk89q.worldedit.command.tool.brush.GravityBrush;
import com.sk89q.worldedit.command.tool.brush.HollowCylinderBrush;
import com.sk89q.worldedit.command.tool.brush.HollowSphereBrush;
import com.sk89q.worldedit.command.tool.brush.ImageHeightmapBrush;
import com.sk89q.worldedit.command.tool.brush.LayerBrush;
import com.sk89q.worldedit.command.tool.brush.LineBrush;
import com.sk89q.worldedit.command.tool.brush.MorphBrush;
import com.sk89q.worldedit.command.tool.brush.OperationFactoryBrush;
import com.sk89q.worldedit.command.tool.brush.OverlayBrush;
import com.sk89q.worldedit.command.tool.brush.PopulateSchematicBrush;
import com.sk89q.worldedit.command.tool.brush.ShatterBrush;
import com.sk89q.worldedit.command.tool.brush.SmoothBrush;
import com.sk89q.worldedit.command.tool.brush.SnowSmoothBrush;
import com.sk89q.worldedit.command.tool.brush.SphereBrush;
import com.sk89q.worldedit.command.tool.brush.SplatterBrush;
import com.sk89q.worldedit.command.tool.brush.SplineBrush;
import com.sk89q.worldedit.command.tool.brush.SurfaceSplatterBrush;
import com.sk89q.worldedit.command.util.AsyncCommandBuilder;
import com.sk89q.worldedit.command.util.CommandHelper;
import com.sk89q.worldedit.command.util.CommandPermissions;
import com.sk89q.worldedit.command.util.CommandPermissionsConditionGenerator;
import com.sk89q.worldedit.command.util.CreatureButcher;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.Contextual;
import com.sk89q.worldedit.function.factory.ApplyLayer;
import com.sk89q.worldedit.function.factory.ApplyRegion;
import com.sk89q.worldedit.function.factory.BiomeFactory;
import com.sk89q.worldedit.function.factory.Deform;
import com.sk89q.worldedit.function.factory.Paint;
import com.sk89q.worldedit.function.factory.Snow;
import com.sk89q.worldedit.function.mask.BlockTypeMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.internal.annotation.ClipboardMask;
import com.sk89q.worldedit.internal.annotation.VertHeight;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.factory.CuboidRegionFactory;
import com.sk89q.worldedit.regions.factory.CylinderRegionFactory;
import com.sk89q.worldedit.regions.factory.FixedHeightCuboidRegionFactory;
import com.sk89q.worldedit.regions.factory.FixedHeightCylinderRegionFactory;
import com.sk89q.worldedit.regions.factory.RegionFactory;
import com.sk89q.worldedit.regions.factory.SphereRegionFactory;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.session.request.RequestExtent;
import com.sk89q.worldedit.util.HandSide;
import com.sk89q.worldedit.util.asset.AssetLoadTask;
import com.sk89q.worldedit.util.asset.AssetLoader;
import com.sk89q.worldedit.util.asset.holder.ImageHeightmap;
import com.sk89q.worldedit.util.auth.AuthorizationException;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;
import com.sk89q.worldedit.util.formatting.text.event.ClickEvent;
import com.sk89q.worldedit.util.formatting.text.format.TextColor;
import com.sk89q.worldedit.util.io.file.FilenameException;
import com.sk89q.worldedit.util.io.file.FilenameResolutionException;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.generation.ConfiguredFeatureType;
import com.sk89q.worldedit.world.generation.TreeType;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;
import org.enginehub.piston.annotation.param.ArgFlag;
import org.enginehub.piston.annotation.param.Switch;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Stream;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Commands to set brush shape.
 */
@CommandContainer(superTypes = CommandPermissionsConditionGenerator.Registration.class)
public class BrushCommands {

    private static final int MAX_FRAGMENTS = 64;
    private static final int MAX_PATCHES = 64;
    private static final int MAX_LAYERS = 64;
    private static final int MAX_SCHEMATICS = 64;

    private final WorldEdit worldEdit;

    private static final Component UNBIND_COMMAND_COMPONENT = TextComponent.builder("/brush unbind", TextColor.AQUA)
                                                                   .clickEvent(ClickEvent.suggestCommand("/brush unbind"))
                                                                   .build();

    /**
     * Create a new instance.
     *
     * @param worldEdit reference to WorldEdit
     */
    public BrushCommands(WorldEdit worldEdit) {
        checkNotNull(worldEdit);
        this.worldEdit = worldEdit;
    }

    @Command(
        name = "none",
        aliases = "unbind",
        desc = "Unbind a bound brush from your current item"
    )
    void none(Player player, LocalSession session) throws WorldEditException {
        ToolCommands.setToolNone(player, session, true);
    }

    @Command(
        name = "sphere",
        aliases = { "s" },
        desc = "Choose the sphere brush"
    )
    @CommandPermissions("worldedit.brush.sphere")
    public void sphereBrush(Player player, LocalSession session,
                            @Arg(desc = "The pattern of blocks to set")
                                Pattern pattern,
                            @Arg(desc = "The radius of the sphere", def = "2")
                                double radius,
                            @Switch(name = 'h', desc = "Create hollow spheres instead")
                                boolean hollow) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);

        Brush brush = hollow ? new HollowSphereBrush() : new SphereBrush();

        equip(player, session, brush, "worldedit.brush.sphere", pattern, radius);

        announce(player, "worldedit.brush.sphere.equip", TextComponent.of(String.format("%.0f", radius)));
    }

    @Command(
        name = "cylinder",
        aliases = { "cyl", "c" },
        desc = "Choose the cylinder brush"
    )
    @CommandPermissions("worldedit.brush.cylinder")
    public void cylinderBrush(Player player, LocalSession session,
                              @Arg(desc = "The pattern of blocks to set")
                                  Pattern pattern,
                              @Arg(desc = "The radius of the cylinder", def = "2")
                                  double radius,
                              @Arg(desc = "The height of the cylinder", def = "1")
                                  int height,
                              @Switch(name = 'h', desc = "Create hollow cylinders instead")
                                  boolean hollow) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        worldEdit.checkMaxBrushRadius(height);

        Brush brush = hollow ? new HollowCylinderBrush(height) : new CylinderBrush(height);

        equip(player, session, brush, "worldedit.brush.cylinder", pattern, radius);

        announce(player, "worldedit.brush.cylinder.equip", TextComponent.of((int) radius), TextComponent.of(height));
    }

    @Command(
        name = "splatter",
        aliases = { "splat" },
        desc = "Choose the splatter brush"
    )
    @CommandPermissions("worldedit.brush.splatter")
    public void splatterBrush(Player player, LocalSession session,
                              @Arg(desc = "The pattern of blocks to set")
                                  Pattern pattern,
                              @Arg(desc = "The radius of the splatter", def = "2")
                                  double radius,
                              @Arg(desc = "The decay of the splatter between 0 and 10", def = "1")
                                  int decay) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);

        if (decay < 0 || decay > 10) {
            player.printError(TranslatableComponent.of("worldedit.brush.splatter.decay-out-of-range", TextComponent.of(decay)));
            return;
        }

        equip(player, session, new SplatterBrush(decay), "worldedit.brush.splatter", pattern, radius);

        announce(player, "worldedit.brush.splatter.equip", TextComponent.of((int) radius), TextComponent.of(decay));
    }


    @Command(
        name = "clipboard",
        aliases = { "copy" },
        desc = "Choose the clipboard brush"
    )
    @CommandPermissions("worldedit.brush.clipboard")
    public void clipboardBrush(Player player, LocalSession session,
                               @Switch(name = 'a', desc = "Don't paste air from the clipboard")
                                   boolean ignoreAir,
                               @Switch(name = 'v', desc = "Include structure void blocks")
                                   boolean pasteStructureVoid,
                               @Switch(name = 'o', desc = "Paste starting at the target location, instead of centering on it")
                                   boolean usingOrigin,
                               @Switch(name = 'e', desc = "Paste entities if available")
                                   boolean pasteEntities,
                               @Switch(name = 'b', desc = "Paste biomes if available")
                                   boolean pasteBiomes,
                               @ArgFlag(name = 'm', desc = "Skip blocks matching this mask in the clipboard")
                               @ClipboardMask
                                   Mask sourceMask) throws WorldEditException {
        ClipboardHolder holder = session.getClipboard();

        Clipboard clipboard = holder.getClipboard();
        ClipboardHolder newHolder = new ClipboardHolder(clipboard);
        newHolder.setTransform(holder.getTransform());

        BlockVector3 size = clipboard.getDimensions();

        worldEdit.checkMaxBrushRadius(size.x() / 2D - 1);
        worldEdit.checkMaxBrushRadius(size.y() / 2D - 1);
        worldEdit.checkMaxBrushRadius(size.z() / 2D - 1);

        bind(player, session,
            new ClipboardBrush(newHolder, ignoreAir, !pasteStructureVoid, usingOrigin, pasteEntities, pasteBiomes, sourceMask),
            "worldedit.brush.clipboard");

        announce(player, "worldedit.brush.clipboard.equip");
    }

    @Command(
        name = "smooth",
        desc = "Choose the terrain softener brush",
        descFooter = "Example: '/brush smooth 2 4 grass_block,dirt,stone'"
    )
    @CommandPermissions("worldedit.brush.smooth")
    public void smoothBrush(Player player, LocalSession session,
                            @Arg(desc = "The radius to sample for softening", def = "2")
                                double radius,
                            @Arg(desc = "The number of iterations to perform", def = "4")
                                int iterations,
                            @Arg(desc = "The mask of blocks to use for the heightmap", def = "")
                                Mask mask) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);

        equip(player, session, new SmoothBrush(iterations, mask), "worldedit.brush.smooth", radius);

        announce(player, "worldedit.brush.smooth.equip",
            TextComponent.of((int) radius),
            TextComponent.of(iterations),
            TranslatableComponent.of("worldedit.brush.smooth." + (mask == null ? "no" : "") + "filter")
        );
    }

    @Command(
        name = "snowsmooth",
        desc = "Choose the snow terrain softener brush",
        descFooter = "Example: '/brush snowsmooth 5 1 -l 3'"
    )
    @CommandPermissions("worldedit.brush.snowsmooth")
    public void snowSmoothBrush(Player player, LocalSession session,
                                @Arg(desc = "The radius to sample for softening", def = "2")
                                    double radius,
                                @Arg(desc = "The number of iterations to perform", def = "4")
                                    int iterations,
                                @ArgFlag(name = 'l', desc = "The number of snow blocks under snow", def = "1")
                                    int snowBlockCount,
                                @ArgFlag(name = 'm', desc = "The mask of blocks to use for the heightmap")
                                    Mask mask) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);

        equip(player, session, new SnowSmoothBrush(iterations, snowBlockCount, mask), "worldedit.brush.snowsmooth", radius);

        announce(player, "worldedit.brush.snowsmooth.equip",
            TextComponent.of((int) radius),
            TextComponent.of(iterations),
            TranslatableComponent.of("worldedit.brush.snowsmooth." + (mask == null ? "no" : "") + "filter"),
            TextComponent.of(snowBlockCount)
        );
    }

    @Command(
        name = "extinguish",
        aliases = { "ex" },
        desc = "Shortcut fire extinguisher brush"
    )
    @CommandPermissions("worldedit.brush.ex")
    public void extinguishBrush(Player player, LocalSession session,
                                @Arg(desc = "The radius to extinguish", def = "5")
                                    double radius) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);

        BrushTool tool = equip(player, session, new SphereBrush(), "worldedit.brush.ex", BlockTypes.AIR.getDefaultState(), radius);
        tool.setMask(new BlockTypeMask(new RequestExtent(), BlockTypes.FIRE));

        announce(player, "worldedit.brush.extinguish.equip", TextComponent.of((int) radius));
    }

    @Command(
        name = "gravity",
        aliases = { "grav" },
        desc = "Gravity brush, simulates the effect of gravity"
    )
    @CommandPermissions("worldedit.brush.gravity")
    public void gravityBrush(Player player, LocalSession session,
                             @Arg(desc = "The radius to apply gravity in", def = "5")
                                 double radius,
                             @ArgFlag(
                                 name = 'h',
                                 desc = "Affect blocks between the given height, "
                                     + "upwards and downwards, "
                                     + "rather than the target location Y + radius",
                                 def = HeightConverter.DEFAULT_VALUE
                             )
                             @VertHeight
                                 Integer height) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);

        equip(player, session, new GravityBrush(height), "worldedit.brush.gravity", radius);

        announce(player, "worldedit.brush.gravity.equip", TextComponent.of((int) radius));
    }

    @Command(
        name = "butcher",
        aliases = { "kill" },
        desc = "Butcher brush, kills mobs within a radius"
    )
    @CommandPermissions("worldedit.brush.butcher")
    public void butcherBrush(Player player, LocalSession session,
                             @Arg(desc = "Radius to kill mobs in", def = "5")
                                 double radius,
                             @Switch(name = 'p', desc = "Also kill pets")
                                 boolean killPets,
                             @Switch(name = 'n', desc = "Also kill NPCs")
                                 boolean killNpcs,
                             @Switch(name = 'g', desc = "Also kill golems")
                                 boolean killGolems,
                             @Switch(name = 'a', desc = "Also kill animals")
                                 boolean killAnimals,
                             @Switch(name = 'b', desc = "Also kill ambient mobs")
                                 boolean killAmbient,
                             @Switch(name = 't', desc = "Also kill mobs with name tags")
                                 boolean killWithName,
                             @Switch(name = 'f', desc = "Also kill all friendly mobs (Applies the flags `-abgnpt`)")
                                 boolean killFriendly,
                             @Switch(name = 'r', desc = "Also destroy armor stands")
                                 boolean killArmorStands,
                             @Switch(name = 'w', desc = "Also kill water mobs")
                                 boolean killWater) throws WorldEditException {
        LocalConfiguration config = worldEdit.getConfiguration();

        double maxRadius = config.maxBrushRadius;
        // hmmmm not horribly worried about this because -1 is still rather efficient,
        // the problem arises when butcherMaxRadius is some really high number but not infinite
        // - original idea taken from https://github.com/sk89q/worldedit/pull/198#issuecomment-6463108
        if (player.hasPermission("worldedit.butcher")) {
            maxRadius = Math.max(config.maxBrushRadius, config.butcherMaxRadius);
        }
        if (radius > maxRadius) {
            player.printError(TranslatableComponent.of("worldedit.brush.radius-too-large", TextComponent.of(maxRadius)));
            return;
        }

        CreatureButcher flags = new CreatureButcher(player);
        flags.or(CreatureButcher.Flags.FRIENDLY, killFriendly); // No permission check here. Flags will instead be filtered by the subsequent calls.
        flags.or(CreatureButcher.Flags.PETS, killPets, "worldedit.butcher.pets");
        flags.or(CreatureButcher.Flags.NPCS, killNpcs, "worldedit.butcher.npcs");
        flags.or(CreatureButcher.Flags.GOLEMS, killGolems, "worldedit.butcher.golems");
        flags.or(CreatureButcher.Flags.ANIMALS, killAnimals, "worldedit.butcher.animals");
        flags.or(CreatureButcher.Flags.AMBIENT, killAmbient, "worldedit.butcher.ambient");
        flags.or(CreatureButcher.Flags.TAGGED, killWithName, "worldedit.butcher.tagged");
        flags.or(CreatureButcher.Flags.ARMOR_STAND, killArmorStands, "worldedit.butcher.armorstands");
        flags.or(CreatureButcher.Flags.WATER, killWater, "worldedit.butcher.water");

        equip(player, session, new ButcherBrush(flags), "worldedit.brush.butcher", radius);

        announce(player, "worldedit.brush.butcher.equip", TextComponent.of((int) radius));
    }

    @Command(
        name = "heightmap",
        desc = "Heightmap brush, raises or lowers terrain using an image heightmap"
    )
    @CommandPermissions("worldedit.brush.heightmap")
    void heightmapBrush(Player player, LocalSession session,
                    @Arg(desc = "The name of the image")
                        String imageName,
                    @Arg(desc = "The size of the brush", def = "5")
                        double radius,
                    @Arg(desc = "The intensity of the brush", def = "5")
                        double intensity,
                    @Switch(name = 'e', desc = "Erase blocks instead of filling them")
                        boolean erase,
                    @Switch(name = 'f', desc = "Don't change blocks above the selected height")
                        boolean flatten,
                    @Switch(name = 'r', desc = "Randomizes the brush's height slightly.")
                        boolean randomize) throws WorldEditException {
        Optional<AssetLoader<ImageHeightmap>> loader = worldEdit.getAssetLoaders().getAssetLoader(ImageHeightmap.class, imageName);

        if (loader.isPresent()) {
            worldEdit.checkMaxBrushRadius(radius);

            AssetLoadTask<ImageHeightmap> task = new AssetLoadTask<>(loader.get(), imageName);
            AsyncCommandBuilder.wrap(task, player)
                .registerWithSupervisor(worldEdit.getSupervisor(), "Loading asset " + imageName)
                .setDelayMessage(TranslatableComponent.of("worldedit.asset.load.loading"))
                .setWorkingMessage(TranslatableComponent.of("worldedit.asset.load.still-loading"))
                .onSuccess(TranslatableComponent.of("worldedit.brush.heightmap.equip", TextComponent.of((int) radius)), heightmap -> {
                    try {
                        equip(player, session, new ImageHeightmapBrush(heightmap, intensity, erase, flatten, randomize),
                            "worldedit.brush.heightmap", radius);
                    } catch (InvalidToolBindException e) {
                        throw new RuntimeException(e);
                    }
                    ToolCommands.sendUnbindInstruction(player, UNBIND_COMMAND_COMPONENT);
                })
                .onFailure(TranslatableComponent.of("worldedit.asset.load.failed"), worldEdit.getPlatformManager().getPlatformCommandManager().getExceptionConverter())
                .buildAndExecNoReturnValue(worldEdit.getExecutorService());
        } else {
            player.printError(TranslatableComponent.of("worldedit.brush.heightmap.unknown", TextComponent.of(imageName)));
        }
    }

    @Command(
        name = "deform",
        desc = "Deform brush, applies an expression to an area"
    )
    @CommandPermissions("worldedit.brush.deform")
    public void deform(Player player, LocalSession localSession,
                       @Arg(desc = "The shape of the region")
                           RegionFactory shape,
                       @Arg(desc = "The size of the brush", def = "5")
                           double radius,
                       @Arg(desc = "Expression to apply", def = "y-=0.2")
                           String expression,
                       @Switch(name = 'r', desc = "Use the game's coordinate origin")
                           boolean useRawCoords,
                       @Switch(name = 'o', desc = "Use the placement position as the origin")
                           boolean usePlacement) throws WorldEditException {
        Deform deform = new Deform(expression);
        if (useRawCoords) {
            deform.setMode(Deform.Mode.RAW_COORD);
        } else if (usePlacement) {
            deform.setMode(Deform.Mode.OFFSET);
            deform.setOffset(localSession.getPlacementPosition(player).toVector3());
        }
        setOperationBasedBrush(player, localSession, radius,
            deform, shape, "worldedit.brush.deform");
    }

    @Command(
        name = "set",
        desc = "Set brush, sets all blocks in the area"
    )
    @CommandPermissions("worldedit.brush.set")
    public void set(Player player, LocalSession localSession,
                    @Arg(desc = "The shape of the region")
                        RegionFactory shape,
                    @Arg(desc = "The size of the brush", def = "5")
                        double radius,
                    @Arg(desc = "The pattern of blocks to set")
                        Pattern pattern) throws WorldEditException {
        setOperationBasedBrush(player, localSession, radius,
            new ApplyRegion(new ReplaceFactory(pattern)), shape, "worldedit.brush.set");
    }

    @Command(
        name = "forest",
        desc = "Forest brush, creates a forest in the area"
    )
    @CommandPermissions("worldedit.brush.forest")
    public void forest(Player player, LocalSession localSession,
                       @Arg(desc = "The shape of the region")
                           RegionFactory shape,
                       @Arg(desc = "The size of the brush", def = "5")
                           double radius,
                       @Arg(desc = "The density of the brush", def = "20")
                           double density,
                       @Arg(desc = "The type of tree to use")
                           TreeType type) throws WorldEditException {
        setOperationBasedBrush(player, localSession, radius,
            new Paint(new TreeGeneratorFactory(type), density / 100), shape, "worldedit.brush.forest");
    }

    @Command(
        name = "feature",
        desc = "Feature brush, paints Minecraft generation features"
    )
    @CommandPermissions("worldedit.brush.feature")
    public void feature(Player player, LocalSession localSession,
                       @Arg(desc = "The shape of the region")
                       RegionFactory shape,
                       @Arg(desc = "The size of the brush", def = "5")
                       double radius,
                       @Arg(desc = "The density of the brush", def = "5")
                       double density,
                       @Arg(desc = "The type of feature to use")
                       ConfiguredFeatureType type) throws WorldEditException {
        setOperationBasedBrush(player, localSession, radius,
            new ApplyRegion(new FeatureGeneratorFactory(type, density / 100)), shape, "worldedit.brush.feature");
    }

    @Command(
        name = "raise",
        desc = "Raise brush, raise all blocks by one"
    )
    @CommandPermissions("worldedit.brush.raise")
    public void raise(Player player, LocalSession localSession,
                      @Arg(desc = "The shape of the region")
                          RegionFactory shape,
                      @Arg(desc = "The size of the brush", def = "5")
                          double radius) throws WorldEditException {
        setOperationBasedBrush(player, localSession, radius,
            new Deform("y-=1", Deform.Mode.RAW_COORD), shape, "worldedit.brush.raise");
    }

    @Command(
        name = "lower",
        desc = "Lower brush, lower all blocks by one"
    )
    @CommandPermissions("worldedit.brush.lower")
    public void lower(Player player, LocalSession localSession,
                      @Arg(desc = "The shape of the region")
                          RegionFactory shape,
                      @Arg(desc = "The size of the brush", def = "5")
                          double radius) throws WorldEditException {
        setOperationBasedBrush(player, localSession, radius,
            new Deform("y+=1", Deform.Mode.RAW_COORD), shape, "worldedit.brush.lower");
    }

    @Command(
        name = "snow",
        desc = "Snow brush, sets snow in the area"
    )
    @CommandPermissions("worldedit.brush.snow")
    public void snow(Player player, LocalSession localSession,
                     @Arg(desc = "The shape of the region")
                         RegionFactory shape,
                     @Arg(desc = "The size of the brush", def = "5")
                         double radius,
                     @Switch(name = 's', desc = "Whether to stack snow")
                         boolean stack) throws WorldEditException {

        if (shape instanceof CylinderRegionFactory) {
            shape = new CylinderRegionFactory(radius);
        }

        setOperationBasedBrush(player, localSession, radius,
            new ApplyLayer(new Snow(stack)), shape, "worldedit.brush.snow");
    }

    @Command(
        name = "biome",
        desc = "Biome brush, sets biomes in the area"
    )
    @CommandPermissions("worldedit.brush.biome")
    public void biome(Player player, LocalSession localSession,
                      @Arg(desc = "The shape of the region")
                          RegionFactory shape,
                      @Arg(desc = "The size of the brush", def = "5")
                          double radius,
                      @Arg(desc = "The biome type")
                          BiomeType biomeType,
                      @Switch(name = 'c', desc = "Whether to set the full column")
                          boolean column) throws WorldEditException {

        if (column) {
            // Convert this shape factory to a column-based one, if possible
            if (shape instanceof CylinderRegionFactory || shape instanceof SphereRegionFactory) {
                // Sphere regions that are Y-expended are just cylinders
                shape = new FixedHeightCylinderRegionFactory(player.getWorld().getMinY(), player.getWorld().getMaxY());
            } else if (shape instanceof CuboidRegionFactory) {
                shape = new FixedHeightCuboidRegionFactory(player.getWorld().getMinY(), player.getWorld().getMaxY());
            } else {
                player.printError(TranslatableComponent.of("worldedit.brush.biome.column-supported-types"));
                return;
            }
        }

        setOperationBasedBrush(player, localSession, radius,
            new ApplyRegion(new BiomeFactory(biomeType)), shape, "worldedit.brush.biome");
    }

    @Command(
        name = "morph",
        desc = "Morph brush, morphs blocks in the area"
    )
    @CommandPermissions("worldedit.brush.morph")
    public void morph(Player player, LocalSession session,
                      @Arg(desc = "The size of the brush", def = "5")
                          double brushSize,
                      @Arg(desc = "Minimum number of faces for erosion", def = "3")
                          int minErodeFaces,
                      @Arg(desc = "Erode iterations", def = "1")
                          int numErodeIterations,
                      @Arg(desc = "Minimum number of faces for dilation", def = "3")
                          int minDilateFaces,
                      @Arg(desc = "Dilate iterations", def = "1")
                          int numDilateIterations) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(brushSize);
        equip(player, session, new MorphBrush(minErodeFaces, numErodeIterations, minDilateFaces, numDilateIterations),
            "worldedit.brush.morph", brushSize);

        announce(player, "worldedit.brush.morph.equip", TextComponent.of((int) brushSize));
    }

    @Command(
        name = "erode",
        desc = "Erode preset for morph brush, erodes blocks in the area"
    )
    @CommandPermissions("worldedit.brush.morph")
    public void erode(Player player, LocalSession session,
                      @Arg(desc = "The size of the brush", def = "5")
                          double brushSize) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(brushSize);
        equip(player, session, new MorphBrush(2, 1, 5, 1), "worldedit.brush.morph", brushSize);

        announce(player, "worldedit.brush.morph.equip", TextComponent.of((int) brushSize));
    }

    @Command(
        name = "dilate",
        desc = "Dilate preset for morph brush, dilates blocks in the area"
    )
    @CommandPermissions("worldedit.brush.morph")
    public void dilate(Player player, LocalSession session,
                       @Arg(desc = "The size of the brush", def = "5")
                           double brushSize) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(brushSize);
        equip(player, session, new MorphBrush(5, 1, 2, 1), "worldedit.brush.morph", brushSize);

        announce(player, "worldedit.brush.morph.equip", TextComponent.of((int) brushSize));
    }

    @Command(
        name = "blob",
        desc = "Blob brush, creates organic noise-deformed spheres",
        descFooter = "Example: '/brush blob stone 6 60'"
    )
    @CommandPermissions("worldedit.brush.blob")
    public void blobBrush(Player player, LocalSession session,
                          @Arg(desc = "The pattern of blocks to set")
                              Pattern pattern,
                          @Arg(desc = "The base radius of the blob", def = "5")
                              double radius,
                          @Arg(desc = "The roughness of the surface, between 0 and 100", def = "50")
                              double roughness) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        if (roughness < 0 || roughness > 100) {
            player.printError(TranslatableComponent.of("worldedit.brush.blob.roughness-out-of-range", TextComponent.of(roughness)));
            return;
        }

        equip(player, session, new BlobBrush(roughness), "worldedit.brush.blob", pattern, radius);

        announce(player, "worldedit.brush.blob.equip",
            TextComponent.of((int) radius), TextComponent.of((int) roughness));
    }

    @Command(
        name = "line",
        desc = "Line brush, draws lines between the blocks you target",
        descFooter = "The first click marks the start point, the second click draws the line.\n"
            + "Example: '/brush line -c oak_fence'"
    )
    @CommandPermissions("worldedit.brush.line")
    public void lineBrush(Player player, LocalSession session,
                          @Arg(desc = "The pattern of blocks to set")
                              Pattern pattern,
                          @Arg(desc = "The thickness (radius) of the line", def = "0")
                              double radius,
                          @Switch(name = 'c', desc = "Chain lines, starting each line where the previous one ended")
                              boolean chain,
                          @Switch(name = 'h', desc = "Only draw the shell of thick lines")
                              boolean hollow) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);

        equip(player, session, new LineBrush(chain, hollow), "worldedit.brush.line", pattern, radius);

        announce(player, "worldedit.brush.line.equip", TextComponent.of((int) radius));
    }

    @Command(
        name = "overlay",
        aliases = { "cover" },
        desc = "Overlay brush, covers the top surface of the terrain",
        descFooter = "Example: '/brush overlay -r grass_block 6' re-surfaces terrain with grass"
    )
    @CommandPermissions("worldedit.brush.overlay")
    public void overlayBrush(Player player, LocalSession session,
                             @Arg(desc = "The pattern of blocks to set")
                                 Pattern pattern,
                             @Arg(desc = "The radius of the brush", def = "5")
                                 double radius,
                             @Arg(desc = "The number of layers to place or replace", def = "1")
                                 int depth,
                             @Switch(name = 'r', desc = "Replace the surface blocks instead of placing on top of them")
                                 boolean replace) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        worldEdit.checkMaxBrushRadius(depth);
        if (depth < 1) {
            player.printError(TranslatableComponent.of("worldedit.brush.overlay.depth-too-small", TextComponent.of(depth)));
            return;
        }

        equip(player, session, new OverlayBrush(depth, replace), "worldedit.brush.overlay", pattern, radius);

        announce(player, "worldedit.brush.overlay.equip",
            TextComponent.of((int) radius), TextComponent.of(depth));
    }

    @Command(
        name = "fill",
        aliases = { "filldown" },
        desc = "Fill brush, fills holes and depressions up to the targeted block's level",
        descFooter = "Example: '/brush fill water 8 10' turns a crater into a lake"
    )
    @CommandPermissions("worldedit.brush.fill")
    public void fillBrush(Player player, LocalSession session,
                          @Arg(desc = "The pattern of blocks to set")
                              Pattern pattern,
                          @Arg(desc = "The radius of the brush", def = "5")
                              double radius,
                          @Arg(desc = "The maximum depth to fill down", def = "5")
                              int depth) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        worldEdit.checkMaxBrushRadius(depth);
        if (depth < 1) {
            player.printError(TranslatableComponent.of("worldedit.brush.fill.depth-too-small", TextComponent.of(depth)));
            return;
        }

        equip(player, session, new FillBrush(depth), "worldedit.brush.fill", pattern, radius);

        announce(player, "worldedit.brush.fill.equip",
            TextComponent.of((int) radius), TextComponent.of(depth));
    }

    @Command(
        name = "drain",
        desc = "Drain brush, removes liquids within a sphere"
    )
    @CommandPermissions("worldedit.brush.drain")
    public void drainBrush(Player player, LocalSession session,
                           @Arg(desc = "The radius to drain", def = "5")
                               double radius,
                           @Switch(name = 'w', desc = "Also un-waterlog blocks")
                               boolean waterlogged) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);

        equip(player, session, new DrainBrush(waterlogged), "worldedit.brush.drain", null, radius);

        announce(player, "worldedit.brush.drain.equip", TextComponent.of((int) radius));
    }

    @Command(
        name = "spline",
        aliases = { "curve" },
        desc = "Spline brush, builds a smooth tube through the points you click",
        descFooter = "Click to add control points, then click the last point again to build the curve.\n"
            + "Example: '/brush spline stone 2'"
    )
    @CommandPermissions("worldedit.brush.spline")
    public void splineBrush(Player player, LocalSession session,
                            @Arg(desc = "The pattern of blocks to set")
                                Pattern pattern,
                            @Arg(desc = "The radius (thickness) of the tube", def = "0")
                                double radius,
                            @ArgFlag(name = 't', desc = "The tension of the curve, between -1 and 1", def = "0")
                                double tension,
                            @Switch(name = 'h', desc = "Only draw the shell of the tube")
                                boolean hollow) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        if (tension < -1 || tension > 1) {
            player.printError(TranslatableComponent.of("worldedit.brush.spline.tension-out-of-range", TextComponent.of(tension)));
            return;
        }

        equip(player, session, new SplineBrush(hollow, tension), "worldedit.brush.spline", pattern, radius);

        announce(player, "worldedit.brush.spline.equip", TextComponent.of((int) radius));
    }

    @Command(
        name = "copypaste",
        aliases = { "cp" },
        desc = "Copy-paste brush, copies a sphere at the first click and pastes it at the next one"
    )
    @CommandPermissions("worldedit.brush.copypaste")
    public void copyPasteBrush(Player player, LocalSession session,
                               @Arg(desc = "The radius of the copied sphere", def = "5")
                                   double radius,
                               @Switch(name = 'a', desc = "Don't paste air blocks")
                                   boolean ignoreAir,
                               @Switch(name = 'k', desc = "Keep the copy to paste it several times")
                                   boolean keepCopy) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);

        equip(player, session, new CopyPasteBrush(ignoreAir, keepCopy), "worldedit.brush.copypaste", null, radius);

        announce(player, "worldedit.brush.copypaste.equip", TextComponent.of((int) radius));
    }

    @Command(
        name = "shatter",
        aliases = { "crack" },
        desc = "Shatter brush, cracks the terrain along the borders of random fragments",
        descFooter = "Example: '/brush shatter air 8 10' cracks the ground open"
    )
    @CommandPermissions("worldedit.brush.shatter")
    public void shatterBrush(Player player, LocalSession session,
                             @Arg(desc = "The pattern of blocks to set in the cracks")
                                 Pattern pattern,
                             @Arg(desc = "The radius of the brush", def = "6")
                                 double radius,
                             @Arg(desc = "The number of fragments, between 2 and 64", def = "8")
                                 int fragments,
                             @ArgFlag(name = 'w', desc = "The width of the cracks", def = "1")
                                 double width) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        if (fragments < 2 || fragments > MAX_FRAGMENTS) {
            player.printError(TranslatableComponent.of("worldedit.brush.shatter.fragments-out-of-range",
                TextComponent.of(fragments), TextComponent.of(MAX_FRAGMENTS)));
            return;
        }
        if (width <= 0) {
            player.printError(TranslatableComponent.of("worldedit.brush.shatter.width-too-small", TextComponent.of(width)));
            return;
        }

        equip(player, session, new ShatterBrush(fragments, width), "worldedit.brush.shatter", pattern, radius);

        announce(player, "worldedit.brush.shatter.equip", TextComponent.of((int) radius), TextComponent.of(fragments));
    }

    @Command(
        name = "surfacesplatter",
        aliases = { "ssplatter" },
        desc = "Surface splatter brush, paints random patches on the surface",
        descFooter = "Example: '/brush surfacesplatter gravel,coarse_dirt 10 6 3'"
    )
    @CommandPermissions("worldedit.brush.surfacesplatter")
    public void surfaceSplatterBrush(Player player, LocalSession session,
                                     @Arg(desc = "The pattern of blocks to paint")
                                         Pattern pattern,
                                     @Arg(desc = "The radius of the brush", def = "8")
                                         double radius,
                                     @Arg(desc = "The number of patches per click", def = "6")
                                         int patches,
                                     @Arg(desc = "The maximum radius of a patch", def = "3")
                                         double patchSize) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        worldEdit.checkMaxBrushRadius(patchSize);
        if (patches < 1 || patches > MAX_PATCHES || patchSize < 1) {
            player.printError(TranslatableComponent.of("worldedit.brush.surfacesplatter.invalid", TextComponent.of(MAX_PATCHES)));
            return;
        }

        equip(player, session, new SurfaceSplatterBrush(patches, patchSize), "worldedit.brush.surfacesplatter", pattern, radius);

        announce(player, "worldedit.brush.surfacesplatter.equip",
            TextComponent.of((int) radius), TextComponent.of(patches), TextComponent.of((int) patchSize));
    }

    @Command(
        name = "layer",
        aliases = { "layers" },
        desc = "Layer brush, applies patterns as layers below the surface, or around the center",
        descFooter = "Example: '/brush layer 6 grass_block dirt dirt' re-skins terrain;\n"
            + "'/brush layer -c 8 magma_block stone dirt grass_block' builds a small planet"
    )
    @CommandPermissions("worldedit.brush.layer")
    public void layerBrush(Player player, LocalSession session,
                           @Arg(desc = "The radius of the brush")
                               double radius,
                           @Switch(name = 'c', desc = "Layer by distance to the center instead of depth below the surface")
                               boolean concentric,
                           @Arg(desc = "The patterns of the layers: from the surface down, or from the center out with -c", variable = true)
                               List<Pattern> layers) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        if (layers.isEmpty() || layers.size() > MAX_LAYERS) {
            player.printError(TranslatableComponent.of("worldedit.brush.layer.invalid", TextComponent.of(MAX_LAYERS)));
            return;
        }

        equip(player, session, new LayerBrush(layers, concentric), "worldedit.brush.layer", null, radius);

        announce(player, "worldedit.brush.layer.equip", TextComponent.of((int) radius), TextComponent.of(layers.size()));
    }

    @Command(
        name = "command",
        aliases = { "cmd" },
        desc = "Command brush, runs WorldEdit commands at the targeted block",
        descFooter = "Separate several commands with ';'. The placeholders {x}, {y}, {z}, {size}, {world} and {player} "
            + "are replaced before each command runs. Quote the commands if they contain flags.\n"
            + "Example: '/brush command -s 3 \"//pos1 {x},{y},{z}; //pos2 {x},{y},{z}; //outset {size}\"'"
    )
    @CommandPermissions("worldedit.brush.command")
    public void commandBrush(Player player, LocalSession session,
                             @ArgFlag(name = 's', desc = "The brush size, available as {size}", def = "0")
                                 double size,
                             @Arg(desc = "The commands to run", variable = true)
                                 List<String> commands) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(size);
        String joined = String.join(" ", commands);
        int count = CommandBrush.parseCommands(joined).size();
        if (count == 0) {
            player.printError(TranslatableComponent.of("worldedit.brush.command.empty"));
            return;
        }
        if (count > CommandBrush.MAX_COMMANDS) {
            player.printError(TranslatableComponent.of("worldedit.brush.command.too-many",
                TextComponent.of(count), TextComponent.of(CommandBrush.MAX_COMMANDS)));
            return;
        }
        CommandBrush brush = new CommandBrush(joined);

        equip(player, session, brush, "worldedit.brush.command", null, size);

        announce(player, "worldedit.brush.command.equip", TextComponent.of(brush.getCommands().size()));
    }

    @Command(
        name = "populateschem",
        aliases = { "popschem" },
        desc = "Schematic population brush, scatters random schematics over the surface",
        descFooter = "Give schematic names or folders of the schematics directory, separated with ','. "
            + "Use #clipboard for your clipboard.\n"
            + "Example: '/brush populateschem -r trees 12 5' plants rotated trees from the 'trees' folder"
    )
    @CommandPermissions("worldedit.brush.populateschem")
    public void populateSchematicBrush(Player player, LocalSession session,
                                       @Arg(desc = "The schematics or schematic folders to use, separated with ','")
                                           String schematics,
                                       @Arg(desc = "The radius of the brush", def = "8")
                                           double radius,
                                       @Arg(desc = "The chance of a surface column to get a schematic, between 0 and 100", def = "5")
                                           double density,
                                       @ArgFlag(name = 's', desc = "The minimum distance between two schematics", def = "4")
                                           int spacing,
                                       @Switch(name = 'r', desc = "Randomly rotate each schematic")
                                           boolean randomRotation,
                                       @Switch(name = 'a', desc = "Don't paste air from the schematics")
                                           boolean ignoreAir) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        if (density < 0 || density > 100) {
            player.printError(TranslatableComponent.of("worldedit.brush.populateschem.density-out-of-range", TextComponent.of(density)));
            return;
        }
        if (spacing < 0) {
            player.printError(TranslatableComponent.of("worldedit.brush.populateschem.spacing-negative", TextComponent.of(spacing)));
            return;
        }

        List<ClipboardHolder> clipboards = new ArrayList<>();
        List<File> files = new ArrayList<>();
        for (String name : Splitter.on(',').trimResults().omitEmptyStrings().split(schematics)) {
            if (name.equalsIgnoreCase("#clipboard")) {
                clipboards.add(session.getClipboard());
            } else if (!resolveSchematics(player, name, files)) {
                player.printError(TranslatableComponent.of("worldedit.brush.populateschem.not-found", TextComponent.of(name)));
                return;
            }
        }
        if (!files.isEmpty() && !CommandHelper.canLoadSchematics(player)) {
            // Pasting a file is loading it: don't let the brush bypass the load permission
            throw new AuthorizationException();
        }
        int count = clipboards.size() + files.size();
        if (count == 0) {
            player.printError(TranslatableComponent.of("worldedit.brush.populateschem.not-found", TextComponent.of(schematics)));
            return;
        }
        if (count > MAX_SCHEMATICS) {
            player.printError(TranslatableComponent.of("worldedit.brush.populateschem.too-many",
                TextComponent.of(count), TextComponent.of(MAX_SCHEMATICS)));
            return;
        }

        double maxRadius = worldEdit.getConfiguration().maxBrushRadius;
        Callable<List<ClipboardHolder>> task = () -> {
            List<ClipboardHolder> loaded = new ArrayList<>(clipboards);
            for (File file : files) {
                loaded.add(loadSchematic(file));
            }
            for (ClipboardHolder holder : loaded) {
                checkClipboardSize(holder.getClipboard(), maxRadius);
            }
            return loaded;
        };
        AsyncCommandBuilder.wrap(task, player)
            .registerWithSupervisor(worldEdit.getSupervisor(), "Loading schematics for a brush")
            .setDelayMessage(TranslatableComponent.of("worldedit.schematic.load.loading"))
            .setWorkingMessage(TranslatableComponent.of("worldedit.schematic.load.still-loading"))
            .onSuccess(TranslatableComponent.of("worldedit.brush.populateschem.equip",
                TextComponent.of((int) radius), TextComponent.of(count), TextComponent.of(density)), loaded -> {
                    try {
                        equip(player, session, new PopulateSchematicBrush(loaded, density, spacing, randomRotation, ignoreAir),
                            "worldedit.brush.populateschem", null, radius);
                    } catch (InvalidToolBindException e) {
                        throw new RuntimeException(e);
                    }
                    ToolCommands.sendUnbindInstruction(player, UNBIND_COMMAND_COMPONENT);
                })
            .onFailure(TranslatableComponent.of("worldedit.brush.populateschem.failed"),
                worldEdit.getPlatformManager().getPlatformCommandManager().getExceptionConverter())
            .buildAndExecNoReturnValue(worldEdit.getExecutorService());
    }

    /**
     * Add the schematic file, or the schematic files of the folder, with the given name.
     *
     * @return false if nothing with that name exists
     */
    private boolean resolveSchematics(Player player, String name, List<File> files) throws FilenameException {
        if (name.startsWith("#")) {
            // Don't open file dialogs
            return false;
        }
        Path root = worldEdit.getSchematicsManager().getRoot().toAbsolutePath().normalize();
        Path folder;
        try {
            folder = root.resolve(name).normalize();
        } catch (InvalidPathException _) {
            throw new FilenameResolutionException(name, TranslatableComponent.of("worldedit.error.file-resolution.resolve-failed"));
        }
        boolean allowSymlinks = worldEdit.getConfiguration().allowSymlinks;
        if (folder.startsWith(root) && Files.isDirectory(folder)) {
            try {
                if (!folder.toRealPath().startsWith(root.toRealPath()) && !allowSymlinks) {
                    throw new FilenameResolutionException(name, TranslatableComponent.of("worldedit.error.file-resolution.outside-root"));
                }
                Set<String> extensions = Set.of(ClipboardFormats.getFileExtensionArray());
                try (Stream<Path> stream = Files.list(folder)) {
                    // Symbolic links to files could point outside of the schematics folder
                    stream.filter(path -> allowSymlinks ? Files.isRegularFile(path) : Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                        .filter(path -> extensions.contains(getExtension(path)))
                        .sorted()
                        .limit(MAX_SCHEMATICS + 1)
                        .forEach(path -> files.add(path.toFile()));
                }
            } catch (IOException _) {
                throw new FilenameResolutionException(name, TranslatableComponent.of("worldedit.error.file-resolution.resolve-failed"));
            }
            return true;
        }
        File file = worldEdit.getSafeOpenFile(player, root.toFile(), name,
            BuiltInClipboardFormat.SPONGE_V3_SCHEMATIC.getPrimaryFileExtension(),
            ClipboardFormats.getFileExtensionArray());
        if (!file.isFile()) {
            return false;
        }
        files.add(file);
        return true;
    }

    private static String getExtension(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static ClipboardHolder loadSchematic(File file) throws IOException {
        ClipboardFormat format = ClipboardFormats.findByPath(file.toPath());
        if (format == null) {
            throw new IOException("Unknown schematic format: " + file.getName());
        }
        try (InputStream in = new BufferedInputStream(new FileInputStream(file));
             ClipboardReader reader = format.getReader(in)) {
            return new ClipboardHolder(reader.read());
        }
    }

    private static void checkClipboardSize(Clipboard clipboard, double maxRadius) throws MaxBrushRadiusException {
        BlockVector3 size = clipboard.getDimensions();
        if (maxRadius > 0 && Math.max(size.x(), Math.max(size.y(), size.z())) / 2D - 1 > maxRadius) {
            throw new MaxBrushRadiusException();
        }
    }

    /**
     * Bind a brush to the item in the main hand of the player.
     */
    private static BrushTool bind(Player player, LocalSession session, Brush brush, String permission) throws InvalidToolBindException {
        return session.forceBrush(player.getItemInHand(HandSide.MAIN_HAND).getType(), brush, permission);
    }

    /**
     * Bind a brush to the item in the main hand of the player, and set its size.
     */
    private static BrushTool equip(Player player, LocalSession session, Brush brush, String permission,
                                   double size) throws InvalidToolBindException {
        BrushTool tool = bind(player, session, brush, permission);
        tool.setSize(size);
        return tool;
    }

    /**
     * Bind a brush to the item in the main hand of the player, and set its material and size.
     */
    private static BrushTool equip(Player player, LocalSession session, Brush brush, String permission,
                                   @Nullable Pattern fill, double size) throws InvalidToolBindException {
        BrushTool tool = equip(player, session, brush, permission, size);
        tool.setFill(fill);
        return tool;
    }

    /**
     * Tell the player that a brush was equipped, and how to unbind it.
     */
    private static void announce(Player player, String translationKey, Component... args) {
        player.printInfo(TranslatableComponent.of(translationKey, args));
        ToolCommands.sendUnbindInstruction(player, UNBIND_COMMAND_COMPONENT);
    }

    static void setOperationBasedBrush(Player player, LocalSession session, double radius,
                                        Contextual<? extends Operation> factory,
                                        RegionFactory shape,
                                        String permission) throws WorldEditException {
        WorldEdit.getInstance().checkMaxBrushRadius(radius);
        equip(player, session, new OperationFactoryBrush(factory, shape, session), permission, null, radius);

        announce(player, "worldedit.brush.operation.equip", TextComponent.of(factory.toString()));
    }
}
