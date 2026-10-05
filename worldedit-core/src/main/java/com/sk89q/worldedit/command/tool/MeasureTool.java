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

package com.sk89q.worldedit.command.tool;

import com.google.common.collect.ImmutableList;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Measures distances, areas and volumes between targeted blocks.
 *
 * <p>Left-click starts a new measurement at the targeted block, and
 * right-click adds the targeted block to the current path. After each new
 * point, the length of the last segment and of the whole path are shown,
 * along with the size and volume of the bounding box of all points, and the
 * horizontal area of the polygon they form once there are three or more.</p>
 */
public class MeasureTool extends BrushTool implements DoubleActionTraceTool {

    /**
     * The maximum number of points of a measurement.
     */
    public static final int MAX_POINTS = 256;

    private final List<BlockVector3> points = new ArrayList<>();

    public MeasureTool() {
        super("worldedit.tool.measure");
    }

    @Override
    public boolean canUse(Actor player) {
        return player.hasPermission("worldedit.tool.measure");
    }

    /**
     * Get the points of the current measurement.
     *
     * @return an immutable copy of the points
     */
    public List<BlockVector3> getPoints() {
        return ImmutableList.copyOf(points);
    }

    @Override
    public boolean actSecondary(Platform server, LocalConfiguration config, Player player, LocalSession session) {
        Location target = getTarget(player);
        if (target == null) {
            return true;
        }
        BlockVector3 point = target.toVector().toBlockPoint();
        points.clear();
        points.add(point);
        player.printInfo(TranslatableComponent.of("worldedit.tool.measure.start", TextComponent.of(point.toString())));
        return true;
    }

    @Override
    public boolean actPrimary(Platform server, LocalConfiguration config, Player player, LocalSession session) {
        if (points.isEmpty()) {
            return actSecondary(server, config, player, session);
        }
        Location target = getTarget(player);
        if (target == null) {
            return true;
        }
        if (points.size() >= MAX_POINTS) {
            player.printError(TranslatableComponent.of("worldedit.tool.measure.too-many-points", TextComponent.of(MAX_POINTS)));
            return true;
        }
        BlockVector3 point = target.toVector().toBlockPoint();
        BlockVector3 previous = points.getLast();
        points.add(point);

        BlockVector3 delta = point.subtract(previous).abs();
        player.printInfo(TranslatableComponent.of("worldedit.tool.measure.segment",
            TextComponent.of(points.size()),
            TextComponent.of(point.toString()),
            TextComponent.of(format(previous.distance(point))),
            TextComponent.of(delta.x()),
            TextComponent.of(delta.y()),
            TextComponent.of(delta.z()),
            TextComponent.of(format(pathLength(points)))
        ));
        BlockVector3 size = boundingSize(points);
        player.printInfo(TranslatableComponent.of("worldedit.tool.measure.bounds",
            TextComponent.of(size.x()),
            TextComponent.of(size.y()),
            TextComponent.of(size.z()),
            TextComponent.of(volume(points))
        ));
        if (points.size() >= 3) {
            player.printInfo(TranslatableComponent.of("worldedit.tool.measure.area",
                TextComponent.of(format(polygonArea(points)))));
        }
        return true;
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    /**
     * Compute the length of a path going through the given points, in order.
     *
     * @param points the points
     * @return the length of the path
     */
    public static double pathLength(List<BlockVector3> points) {
        double length = 0;
        for (int i = 1; i < points.size(); i++) {
            length += points.get(i - 1).distance(points.get(i));
        }
        return length;
    }

    /**
     * Compute the size, in blocks, of the bounding box of the given points.
     *
     * @param points the points, at least one
     * @return the size along each axis
     */
    public static BlockVector3 boundingSize(List<BlockVector3> points) {
        BlockVector3 min = points.getFirst();
        BlockVector3 max = points.getFirst();
        for (BlockVector3 point : points) {
            min = min.getMinimum(point);
            max = max.getMaximum(point);
        }
        return max.subtract(min).add(1, 1, 1);
    }

    /**
     * Compute the number of blocks in the bounding box of the given points.
     *
     * @param points the points, at least one
     * @return the volume of the bounding box
     */
    public static long volume(List<BlockVector3> points) {
        BlockVector3 size = boundingSize(points);
        return (long) size.x() * size.y() * size.z();
    }

    /**
     * Compute the horizontal area of the polygon formed by the given points.
     *
     * <p>The points are projected on the X/Z plane and the polygon is closed
     * automatically. The area is measured between block centers, using the
     * shoelace formula.</p>
     *
     * @param points the vertices of the polygon, in order
     * @return the area of the polygon
     */
    public static double polygonArea(List<BlockVector3> points) {
        if (points.size() < 3) {
            return 0;
        }
        long twiceArea = 0;
        for (int i = 0; i < points.size(); i++) {
            BlockVector3 a = points.get(i);
            BlockVector3 b = points.get((i + 1) % points.size());
            twiceArea += (long) a.x() * b.z() - (long) b.x() * a.z();
        }
        return Math.abs(twiceArea) / 2.0;
    }
}
