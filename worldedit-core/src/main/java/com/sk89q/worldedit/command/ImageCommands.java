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
import com.sk89q.worldedit.internal.annotation.Selection;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;
import com.sk89q.worldedit.util.image.BlockPalette;
import com.sk89q.worldedit.util.image.ImageFiles;
import com.sk89q.worldedit.util.image.ImageQuantizer;
import com.sk89q.worldedit.util.image.Images;
import com.sk89q.worldedit.util.image.Surfaces;
import com.sk89q.worldedit.util.image.TopViewRenderer;
import com.sk89q.worldedit.util.io.file.FilenameException;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;
import org.enginehub.piston.annotation.param.Switch;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.sk89q.worldedit.command.util.Logging.LogMode.PLACEMENT;
import static com.sk89q.worldedit.internal.command.CommandUtil.checkCommandArgument;

/**
 * Commands that turn images into blocks and blocks into images.
 */
@CommandContainer(superTypes = CommandPermissionsConditionGenerator.Registration.class)
public class ImageCommands {

    /**
     * The largest width or height, in blocks, of generated pixel art.
     */
    public static final int MAX_SIZE = 1024;

    /**
     * The largest width or length, in blocks, of an exported image.
     */
    public static final int MAX_EXPORT_SIZE = 4096;

    private final WorldEdit worldEdit;

    /**
     * Create a new instance.
     *
     * @param worldEdit reference to WorldEdit
     */
    public ImageCommands(WorldEdit worldEdit) {
        checkNotNull(worldEdit);
        this.worldEdit = worldEdit;
    }

    @Command(
        name = "/image",
        aliases = { "/pixelart" },
        desc = "Build pixel art from an image in the images folder",
        descFooter = "Each pixel becomes the closest-coloured block of a built-in palette; transparent pixels are skipped.\n"
            + "By default the image lies flat on the ground in front of you; use -v to stand it upright, "
            + "or -s to stretch it over your selection."
    )
    @CommandPermissions("worldedit.image.paste")
    @Logging(PLACEMENT)
    public int image(Actor actor, World world, LocalSession session, EditSession editSession,
                     @Arg(desc = "Image file name, inside the images folder")
                         String filename,
                     @Arg(desc = "Width in blocks (the height keeps the aspect ratio); defaults to the image width; ignored with -s", def = "")
                         Integer width,
                     @Switch(name = 'v', desc = "Build the image upright instead of flat")
                         boolean vertical,
                     @Switch(name = 'd', desc = "Apply Floyd-Steinberg dithering")
                         boolean dither,
                     @Switch(name = 's', desc = "Stretch the image over the selection instead of placing it near you")
                         boolean onSelection) throws WorldEditException {
        Region region = onSelection ? session.getSelection(world) : null;
        if (region == null && !(actor instanceof Player)) {
            actor.printError(TranslatableComponent.of("worldedit.image.need-player"));
            return 0;
        }
        checkCommandArgument(width == null || width >= 1, "Width must be >= 1");

        BlockPalette palette = placeablePalette();
        if (palette.size() == 0) {
            actor.printError(TranslatableComponent.of("worldedit.image.empty-palette"));
            return 0;
        }

        BufferedImage image = readImage(worldEdit, actor, filename);
        if (image == null) {
            return 0;
        }
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();

        int targetWidth;
        int targetHeight;
        if (region != null) {
            BlockVector3 size = region.getMaximumPoint().subtract(region.getMinimumPoint()).add(1, 1, 1);
            targetWidth = vertical ? Math.max(size.x(), size.z()) : size.x();
            targetHeight = vertical ? size.y() : size.z();
        } else {
            targetWidth = width == null ? imageWidth : width;
            targetHeight = Math.max(1, (int) Math.round((double) imageHeight * targetWidth / imageWidth));
        }
        if (targetWidth > MAX_SIZE || targetHeight > MAX_SIZE) {
            actor.printError(TranslatableComponent.of("worldedit.image.too-large",
                TextComponent.of(targetWidth), TextComponent.of(targetHeight), TextComponent.of(MAX_SIZE)));
            return 0;
        }

        int[] pixels = Images.resize(Images.toArgb(image), imageWidth, imageHeight, targetWidth, targetHeight);
        int[] indices = ImageQuantizer.quantize(pixels, targetWidth, targetHeight, palette, dither);
        BlockState[] states = new BlockState[palette.size()];
        for (int i = 0; i < states.length; i++) {
            states[i] = checkNotNull(BlockType.REGISTRY.get(palette.get(i).blockId())).getDefaultState();
        }

        PixelMapper mapper = region != null
            ? selectionMapper(region, vertical)
            : playerMapper(session.getPlacementPosition(actor), facing((Player) actor), targetWidth, targetHeight, vertical);

        int changed = 0;
        for (int py = 0; py < targetHeight; py++) {
            for (int px = 0; px < targetWidth; px++) {
                int index = indices[py * targetWidth + px];
                if (index == ImageQuantizer.TRANSPARENT) {
                    continue;
                }
                BlockVector3 pos = mapper.map(px, py);
                if (region != null && !vertical && !Surfaces.isInFootprint(region, pos.x(), pos.z())) {
                    continue;
                }
                if (editSession.setBlock(pos, states[index])) {
                    changed++;
                }
            }
        }
        actor.printInfo(TranslatableComponent.of("worldedit.image.created",
            TextComponent.of(filename), TextComponent.of(targetWidth), TextComponent.of(targetHeight),
            TextComponent.of(changed)));
        return changed;
    }

    @Command(
        name = "/topview",
        aliases = { "/imageexport", "/mapexport" },
        desc = "Save a top-down map of the selection as a PNG image",
        descFooter = "One pixel per column, coloured after its highest block; north is up. "
            + "The image is saved in the images folder."
    )
    @CommandPermissions("worldedit.image.export")
    public void topView(Actor actor, World world,
                        @Selection Region region,
                        @Arg(desc = "Image file name, inside the images folder")
                            String filename,
                        @Switch(name = 's', desc = "Shade slopes like an in-game map")
                            boolean shade,
                        @Switch(name = 'f', desc = "Overwrite an existing file")
                            boolean overwrite) throws WorldEditException {
        if (!checkExportSize(actor, region)) {
            return;
        }
        File file = resolveSave(worldEdit, actor, filename, overwrite);
        if (file == null) {
            return;
        }
        BufferedImage image = TopViewRenderer.render(world, region, BlockPalette.getDefault(), shade);
        if (writeImage(actor, image, file, filename)) {
            actor.printInfo(TranslatableComponent.of("worldedit.image.exported",
                TextComponent.of(file.getName()), TextComponent.of(image.getWidth()), TextComponent.of(image.getHeight())));
        }
    }

    // ------------------------------------------------------------------
    // Shared helpers (also used by HeightmapCommands)
    // ------------------------------------------------------------------

    /**
     * Check that a region is small enough to export.
     *
     * @param actor the actor to notify
     * @param region the region
     * @return true if it may be exported
     */
    static boolean checkExportSize(Actor actor, Region region) {
        BlockVector3 size = region.getMaximumPoint().subtract(region.getMinimumPoint()).add(1, 1, 1);
        if (size.x() > MAX_EXPORT_SIZE || size.z() > MAX_EXPORT_SIZE) {
            actor.printError(TranslatableComponent.of("worldedit.image.too-large",
                TextComponent.of(size.x()), TextComponent.of(size.z()), TextComponent.of(MAX_EXPORT_SIZE)));
            return false;
        }
        return true;
    }

    /**
     * Read an image from the images folder, reporting problems to the actor.
     *
     * @param worldEdit the WorldEdit instance
     * @param actor the actor
     * @param filename the user-supplied file name
     * @return the image, or null if it could not be read
     * @throws FilenameException if the name is invalid
     */
    @Nullable
    static BufferedImage readImage(WorldEdit worldEdit, Actor actor, String filename) throws FilenameException {
        File dir = ImageFiles.getDirectory(worldEdit).toFile();
        File file = ImageFiles.resolveOpen(worldEdit, actor, dir, filename);
        if (!file.isFile()) {
            actor.printError(TranslatableComponent.of("worldedit.image.does-not-exist", TextComponent.of(filename)));
            return null;
        }
        try {
            return Images.read(file, Images.MAX_PIXELS);
        } catch (IOException e) {
            actor.printError(TranslatableComponent.of("worldedit.image.read-failed",
                TextComponent.of(filename), TextComponent.of(String.valueOf(e.getMessage()))));
            return null;
        }
    }

    /**
     * Resolve a PNG file to write in the images folder.
     *
     * @param worldEdit the WorldEdit instance
     * @param actor the actor
     * @param filename the user-supplied file name
     * @param overwrite whether an existing file may be replaced
     * @return the file, or null if it exists and may not be replaced
     * @throws FilenameException if the name is invalid
     */
    @Nullable
    static File resolveSave(WorldEdit worldEdit, Actor actor, String filename, boolean overwrite) throws FilenameException {
        File dir = ImageFiles.getDirectory(worldEdit).toFile();
        File file = ImageFiles.resolveSave(worldEdit, actor, dir, filename);
        if (file.exists() && !overwrite) {
            actor.printError(TranslatableComponent.of("worldedit.image.already-exists", TextComponent.of(file.getName())));
            return null;
        }
        return file;
    }

    /**
     * Write a PNG image, reporting failures to the actor.
     *
     * @param actor the actor
     * @param image the image
     * @param file the destination
     * @param filename the name to show
     * @return true on success
     */
    static boolean writeImage(Actor actor, BufferedImage image, File file, String filename) {
        try {
            Images.writePng(image, file);
            return true;
        } catch (IOException e) {
            actor.printError(TranslatableComponent.of("worldedit.image.write-failed",
                TextComponent.of(filename), TextComponent.of(String.valueOf(e.getMessage()))));
            return false;
        }
    }

    /**
     * Get the default palette, restricted to blocks that exist on this
     * platform and are opaque full cubes without block entities.
     *
     * @return the palette
     */
    static BlockPalette placeablePalette() {
        return BlockPalette.getDefault().filter(id -> {
            BlockType type = BlockType.REGISTRY.get(id);
            if (type == null) {
                return false;
            }
            BlockMaterial material = type.getMaterial();
            return material.isFullCube() && material.isOpaque() && !material.hasContainer();
        });
    }

    private static Direction facing(Player player) {
        double yaw = Math.toRadians(player.getLocation().getYaw());
        Direction direction = Direction.findClosest(Vector3.at(-Math.sin(yaw), 0, Math.cos(yaw)), Direction.Flag.CARDINAL);
        return direction == null ? Direction.NORTH : direction;
    }

    @FunctionalInterface
    private interface PixelMapper {
        BlockVector3 map(int px, int py);
    }

    private static PixelMapper selectionMapper(Region region, boolean vertical) {
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        if (!vertical) {
            // Same orientation as //topview: image X is world X, image Y is world Z
            return (px, py) -> BlockVector3.at(min.x() + px, min.y(), min.z() + py);
        }
        boolean alongX = max.x() - min.x() >= max.z() - min.z();
        return alongX
            ? (px, py) -> BlockVector3.at(min.x() + px, max.y() - py, min.z())
            : (px, py) -> BlockVector3.at(min.x(), max.y() - py, min.z() + px);
    }

    private static PixelMapper playerMapper(BlockVector3 origin, Direction facing, int width, int height, boolean vertical) {
        BlockVector3 forward = facing.toBlockVector();
        // Clockwise of forward, i.e. to the player's right
        BlockVector3 right = BlockVector3.at(-forward.z(), 0, forward.x());
        int left = width / 2;
        if (vertical) {
            BlockVector3 base = origin.add(forward.multiply(2));
            return (px, py) -> base.add(right.multiply(px - left)).add(0, height - 1 - py, 0);
        }
        BlockVector3 base = origin.subtract(0, 1, 0);
        return (px, py) -> base.add(right.multiply(px - left)).add(forward.multiply(height - py));
    }
}
