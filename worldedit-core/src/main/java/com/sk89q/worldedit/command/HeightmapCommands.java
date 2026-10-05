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
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.command.util.CommandPermissions;
import com.sk89q.worldedit.command.util.CommandPermissionsConditionGenerator;
import com.sk89q.worldedit.command.util.Logging;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.internal.annotation.Selection;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;
import com.sk89q.worldedit.util.image.Heightmap;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.enginehub.piston.annotation.Command;
import org.enginehub.piston.annotation.CommandContainer;
import org.enginehub.piston.annotation.param.Arg;
import org.enginehub.piston.annotation.param.Switch;

import java.awt.image.BufferedImage;
import java.io.File;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.sk89q.worldedit.command.util.Logging.LogMode.REGION;
import static com.sk89q.worldedit.internal.command.CommandUtil.checkCommandArgument;

/**
 * Commands that convert between terrain and greyscale heightmap images.
 */
@CommandContainer(superTypes = CommandPermissionsConditionGenerator.Registration.class)
public class HeightmapCommands {

    /**
     * How many dirt blocks the default terrain has under its grass.
     */
    private static final int DEFAULT_DIRT_DEPTH = 3;

    private final WorldEdit worldEdit;

    /**
     * Create a new instance.
     *
     * @param worldEdit reference to WorldEdit
     */
    public HeightmapCommands(WorldEdit worldEdit) {
        checkNotNull(worldEdit);
        this.worldEdit = worldEdit;
    }

    @Command(
        name = "import",
        aliases = { "load" },
        desc = "Build terrain over the selection from a greyscale heightmap image",
        descFooter = "The image is stretched over the selection's horizontal footprint. Black is the bottom of the "
            + "selection, white is max-height blocks above it; columns are filled from the bottom of the selection "
            + "and never rise above its top."
    )
    @CommandPermissions("worldedit.image.heightmap.import")
    @Logging(REGION)
    public int importHeightmap(Actor actor, EditSession editSession,
                               @Selection Region region,
                               @Arg(desc = "Image file name, inside the images folder")
                                   String filename,
                               @Arg(desc = "Height of white pixels, in blocks; defaults to the selection height", def = "")
                                   Integer maxHeight,
                               @Arg(desc = "The pattern to fill columns with; defaults to grass over dirt over stone", def = "")
                                   Pattern pattern,
                               @Switch(name = 'c', desc = "Clear blocks above the terrain, within the selection")
                                   boolean clearAbove) throws WorldEditException {
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        int regionHeight = max.y() - min.y() + 1;
        int height = maxHeight == null ? regionHeight : maxHeight;
        checkCommandArgument(height >= 1, "Max height must be >= 1");
        if (!ImageCommands.checkExportSize(actor, region)) {
            return 0;
        }

        BufferedImage image = ImageCommands.readImage(worldEdit, actor, filename);
        if (image == null) {
            return 0;
        }
        Heightmap heightmap = Heightmap.fromValues(Heightmap.readGrey(image), image.getWidth(), image.getHeight(),
            max.x() - min.x() + 1, max.z() - min.z() + 1, height);

        Pattern top = pattern != null ? pattern : BlockTypes.GRASS_BLOCK.getDefaultState();
        Pattern under = pattern != null ? pattern : BlockTypes.DIRT.getDefaultState();
        Pattern fill = pattern != null ? pattern : BlockTypes.STONE.getDefaultState();
        int changed = heightmap.apply(editSession, region, top, under, DEFAULT_DIRT_DEPTH, fill, clearAbove);
        actor.printInfo(TranslatableComponent.of("worldedit.heightmap.imported",
            TextComponent.of(filename), TextComponent.of(changed)));
        return changed;
    }

    @Command(
        name = "export",
        aliases = { "save" },
        desc = "Save the surface heights of the selection as a greyscale PNG image",
        descFooter = "Writes a 16-bit greyscale image: black is an empty column, white is the top of the selection. "
            + "Importing it on the same selection rebuilds the same heights."
    )
    @CommandPermissions("worldedit.image.heightmap.export")
    public void exportHeightmap(Actor actor, World world,
                                @Selection Region region,
                                @Arg(desc = "Image file name, inside the images folder")
                                    String filename,
                                @Switch(name = 'f', desc = "Overwrite an existing file")
                                    boolean overwrite) throws WorldEditException {
        if (!ImageCommands.checkExportSize(actor, region)) {
            return;
        }
        File file = ImageCommands.resolveSave(worldEdit, actor, filename, overwrite);
        if (file == null) {
            return;
        }
        BlockVector3 min = region.getMinimumPoint();
        int regionHeight = region.getMaximumPoint().y() - min.y() + 1;
        Heightmap heightmap = Heightmap.fromSurface(world, region);
        BufferedImage image = heightmap.toImage(regionHeight);
        if (ImageCommands.writeImage(actor, image, file, filename)) {
            actor.printInfo(TranslatableComponent.of("worldedit.heightmap.exported",
                TextComponent.of(file.getName()), TextComponent.of(image.getWidth()), TextComponent.of(image.getHeight()),
                TextComponent.of(min.y()), TextComponent.of(regionHeight)));
        }
    }
}
