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

package com.sk89q.worldedit.command.util;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.MaxRadiusException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;

import java.util.List;
import javax.annotation.Nullable;

import static com.sk89q.worldedit.internal.command.CommandUtil.checkCommandArgument;

/**
 * Small helpers shared by the command classes: argument validation and the
 * usual "N blocks changed" feedback.
 */
public final class CommandHelper {

    private CommandHelper() {
    }

    /**
     * Print an info message whose only argument is the number of affected
     * blocks, and return that number.
     *
     * @param actor the actor to notify
     * @param translationKey the translation key of the message
     * @param affected the number of affected blocks
     * @return {@code affected}
     */
    public static int printAffected(Actor actor, String translationKey, int affected) {
        actor.printInfo(TranslatableComponent.of(translationKey, TextComponent.of(affected)));
        return affected;
    }

    /**
     * Move the actor to a free position if it is a player, so it is not stuck
     * inside blocks that were just generated around it.
     *
     * @param actor the actor
     */
    public static void findFreePosition(Actor actor) {
        if (actor instanceof Player player) {
            player.findFreePosition();
        }
    }

    /**
     * Check that a number is finite, i.e. neither infinite nor NaN.
     *
     * @param value the value
     * @throws org.enginehub.piston.exception.CommandException if the value is not finite
     */
    public static void checkFinite(double value) {
        checkCommandArgument(Double.isFinite(value),
            TranslatableComponent.of("worldedit.error.not-finite", TextComponent.of(String.valueOf(value))));
    }

    /**
     * Check that every radius is finite and within the configured maximum radius.
     *
     * @param worldEdit the WorldEdit instance
     * @param radii the radii to check
     * @throws MaxRadiusException if a radius is bigger than the configured maximum
     * @throws org.enginehub.piston.exception.CommandException if a radius is not finite
     */
    public static void checkRadii(WorldEdit worldEdit, double... radii) throws MaxRadiusException {
        for (double radius : radii) {
            checkFinite(radius);
            worldEdit.checkMaxRadius(radius);
        }
    }

    /**
     * Check that a region is small enough to be read in one go, as {@code //copy} does.
     * Commands that read every block of a region (e.g. image exports) do so synchronously,
     * and no edit session limit applies to reads.
     *
     * @param region the region
     * @param session the session, whose block change limit applies
     * @throws MaxChangedBlocksException if the region is larger than the limit
     */
    public static void checkReadLimit(Region region, LocalSession session) throws MaxChangedBlocksException {
        int limit = session.getBlockChangeLimit();
        if (limit >= 0 && region.getBoundingBox().getVolume() >= limit) {
            throw new MaxChangedBlocksException(limit);
        }
    }

    /**
     * Check whether an actor may load schematic files, as with {@code //schem load}.
     *
     * @param actor the actor
     * @return true if the actor has one of the load permissions
     */
    public static boolean canLoadSchematics(Actor actor) {
        return actor.hasPermission("worldedit.clipboard.load") || actor.hasPermission("worldedit.schematic.load");
    }

    /**
     * Check whether an actor may rename schematic files. Renaming removes the
     * original name, which is a deletion, so the delete permission is needed
     * on top of the rename permission (copying does not need it).
     *
     * @param actor the actor
     * @return true if the actor has both permissions
     */
    public static boolean canRenameSchematics(Actor actor) {
        return actor.hasPermission("worldedit.schematic.rename") && actor.hasPermission("worldedit.schematic.delete");
    }

    /**
     * Expand the radii given to a shape command to one radius per axis.
     *
     * <p>Either a single radius, used for every axis, or exactly one radius
     * per axis is accepted. Each radius is raised to at least {@code minimum}.</p>
     *
     * @param radii the radii given by the user
     * @param axes the number of axes of the shape
     * @param minimum the smallest allowed radius
     * @return one radius per axis, or {@code null} if the number of radii is wrong
     */
    @Nullable
    public static double[] expandRadii(List<Double> radii, int axes, double minimum) {
        if (radii.size() != 1 && radii.size() != axes) {
            return null;
        }
        double[] result = new double[axes];
        for (int i = 0; i < axes; i++) {
            result[i] = Math.max(minimum, radii.get(radii.size() == 1 ? 0 : i));
        }
        return result;
    }
}
