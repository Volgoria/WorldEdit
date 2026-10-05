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
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.interpolation.Interpolation;
import com.sk89q.worldedit.math.interpolation.KochanekBartelsInterpolation;
import com.sk89q.worldedit.math.interpolation.Node;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.sk89q.worldedit.internal.edit.EditSupport.lengthSq;

/**
 * Draws lines and splines for {@link EditSession}.
 */
public final class LineGenerator {

    /**
     * Implementation of {@link EditSession#drawLine(Pattern, List, double, boolean)}.
     */
    public static int drawLine(EditSession session, Pattern pattern, List<BlockVector3> vectors, double radius,
                               boolean filled) throws MaxChangedBlocksException {
        Set<BlockVector3> vset = new HashSet<>();

        for (int i = 0; !vectors.isEmpty() && i < vectors.size() - 1; i++) {
            addSegment(vset, vectors.get(i), vectors.get(i + 1));
        }

        return setThickened(session, vset, radius, filled, pattern);
    }

    /**
     * Add the blocks of the straight line between two points, stepping along the
     * dominant axis.
     */
    private static void addSegment(Set<BlockVector3> vset, BlockVector3 pos1, BlockVector3 pos2) {
        int x1 = pos1.x();
        int y1 = pos1.y();
        int z1 = pos1.z();
        int x2 = pos2.x();
        int y2 = pos2.y();
        int z2 = pos2.z();
        int tipx = x1;
        int tipy = y1;
        int tipz = z1;
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int dz = Math.abs(z2 - z1);

        if (dx + dy + dz == 0) {
            vset.add(BlockVector3.at(tipx, tipy, tipz));
            return;
        }

        int dMax = Math.max(Math.max(dx, dy), dz);
        if (dMax == dx) {
            for (int domstep = 0; domstep <= dx; domstep++) {
                tipx = x1 + domstep * (x2 - x1 > 0 ? 1 : -1);
                tipy = (int) Math.round(y1 + domstep * ((double) dy) / ((double) dx) * (y2 - y1 > 0 ? 1 : -1));
                tipz = (int) Math.round(z1 + domstep * ((double) dz) / ((double) dx) * (z2 - z1 > 0 ? 1 : -1));

                vset.add(BlockVector3.at(tipx, tipy, tipz));
            }
        } else if (dMax == dy) {
            for (int domstep = 0; domstep <= dy; domstep++) {
                tipy = y1 + domstep * (y2 - y1 > 0 ? 1 : -1);
                tipx = (int) Math.round(x1 + domstep * ((double) dx) / ((double) dy) * (x2 - x1 > 0 ? 1 : -1));
                tipz = (int) Math.round(z1 + domstep * ((double) dz) / ((double) dy) * (z2 - z1 > 0 ? 1 : -1));

                vset.add(BlockVector3.at(tipx, tipy, tipz));
            }
        } else /* if (dMax == dz) */ {
            for (int domstep = 0; domstep <= dz; domstep++) {
                tipz = z1 + domstep * (z2 - z1 > 0 ? 1 : -1);
                tipy = (int) Math.round(y1 + domstep * ((double) dy) / ((double) dz) * (y2 - y1 > 0 ? 1 : -1));
                tipx = (int) Math.round(x1 + domstep * ((double) dx) / ((double) dz) * (x2 - x1 > 0 ? 1 : -1));

                vset.add(BlockVector3.at(tipx, tipy, tipz));
            }
        }
    }

    /**
     * Implementation of {@link EditSession#drawSpline(Pattern, List, double, double, double, double, double, boolean)}.
     */
    public static int drawSpline(EditSession session, Pattern pattern, List<BlockVector3> nodevectors, double tension,
                                 double bias, double continuity, double quality, double radius, boolean filled)
        throws MaxChangedBlocksException {
        Set<BlockVector3> vset = new HashSet<>();
        List<Node> nodes = new ArrayList<>(nodevectors.size());

        Interpolation interpol = new KochanekBartelsInterpolation();

        for (BlockVector3 nodevector : nodevectors) {
            Node n = new Node(nodevector.toVector3().add(Vector3.at(0.5D, 0.5D, 0.5D)));
            n.setTension(tension);
            n.setBias(bias);
            n.setContinuity(continuity);
            nodes.add(n);
        }

        interpol.setNodes(nodes);
        double splinelength = interpol.arcLength(0, 1);
        for (double loop = 0; loop <= 1; loop += 1D / splinelength / quality) {
            Vector3 tipv = interpol.getPosition(loop);

            vset.add(tipv.toBlockPoint());
        }

        return setThickened(session, vset, radius, filled, pattern);
    }

    /**
     * Balloon the given center line to the radius, hollow it out if requested,
     * then place the pattern.
     */
    private static int setThickened(EditSession session, Set<BlockVector3> vset, double radius, boolean filled,
                                     Pattern pattern) throws MaxChangedBlocksException {
        vset = getBallooned(vset, radius);
        if (!filled) {
            vset = getHollowed(vset);
        }
        return EditSupport.setBlocks(session, vset, pattern);
    }

    private static Set<BlockVector3> getBallooned(Set<BlockVector3> vset, double radius) {
        Set<BlockVector3> returnset = new HashSet<>();
        int ceilrad = (int) Math.ceil(radius);
        double radiusSquare = Math.pow(radius, 2);

        for (BlockVector3 v : vset) {
            int tipx = v.x();
            int tipy = v.y();
            int tipz = v.z();

            for (int loopx = tipx - ceilrad; loopx <= tipx + ceilrad; loopx++) {
                for (int loopy = tipy - ceilrad; loopy <= tipy + ceilrad; loopy++) {
                    for (int loopz = tipz - ceilrad; loopz <= tipz + ceilrad; loopz++) {
                        if (lengthSq(loopx - tipx, loopy - tipy, loopz - tipz) <= radiusSquare) {
                            returnset.add(BlockVector3.at(loopx, loopy, loopz));
                        }
                    }
                }
            }
        }
        return returnset;
    }

    private static Set<BlockVector3> getHollowed(Set<BlockVector3> vset) {
        Set<BlockVector3> returnset = new HashSet<>();
        for (BlockVector3 v : vset) {
            double x = v.x();
            double y = v.y();
            double z = v.z();
            if (!(vset.contains(BlockVector3.at(x + 1, y, z))
                && vset.contains(BlockVector3.at(x - 1, y, z))
                && vset.contains(BlockVector3.at(x, y + 1, z))
                && vset.contains(BlockVector3.at(x, y - 1, z))
                && vset.contains(BlockVector3.at(x, y, z + 1))
                && vset.contains(BlockVector3.at(x, y, z - 1)))) {
                returnset.add(v);
            }
        }
        return returnset;
    }

    private LineGenerator() {
    }
}
