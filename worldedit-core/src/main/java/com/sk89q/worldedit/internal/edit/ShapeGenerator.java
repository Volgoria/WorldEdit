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
import com.sk89q.worldedit.world.World;

import static com.sk89q.worldedit.internal.edit.EditSupport.lengthSq;
import static com.sk89q.worldedit.internal.edit.EditSupport.setMirroredXYZ;
import static com.sk89q.worldedit.internal.edit.EditSupport.setMirroredXZ;

/**
 * Generates the primitive shapes of {@link EditSession}: cylinders, cones,
 * spheres/ellipsoids and pyramids.
 *
 * <p>Each shape is symmetric, so only one octant (or quadrant) is computed and
 * mirrored into the others.</p>
 */
public final class ShapeGenerator {

    /**
     * Pre-computed values for one axis of a curved shape, whose radius is padded
     * by half a block so that the shape includes the blocks it touches.
     *
     * @param inverse the inverse of the padded radius
     * @param ceil the padded radius, rounded up
     */
    private record Axis(double inverse, int ceil) {
        static Axis padded(double radius) {
            double padded = radius + 0.5;
            return new Axis(1 / padded, (int) Math.ceil(padded));
        }
    }

    /**
     * Implementation of {@link EditSession#makeCylinder(BlockVector3, Pattern, double, double, int, boolean)}.
     *
     * @param world the world, whose height limits clamp the cylinder
     */
    public static int makeCylinder(EditSession session, World world, BlockVector3 pos, Pattern block,
                                   double radiusX, double radiusZ, int height, boolean filled)
        throws MaxChangedBlocksException {
        if (height == 0) {
            return 0;
        } else if (height < 0) {
            height = -height;
            pos = pos.subtract(0, height, 0);
        }

        if (pos.y() < world.getMinY()) {
            pos = pos.withY(world.getMinY());
        } else if (pos.y() + height - 1 > world.getMaxY()) {
            height = world.getMaxY() - pos.y() + 1;
        }

        final Axis axisX = Axis.padded(radiusX);
        final Axis axisZ = Axis.padded(radiusZ);

        int affected = 0;
        double nextXn = 0;
        forX: for (int x = 0; x <= axisX.ceil(); ++x) {
            final double xn = nextXn;
            nextXn = (x + 1) * axisX.inverse();
            double nextZn = 0;
            for (int z = 0; z <= axisZ.ceil(); ++z) {
                final double zn = nextZn;
                nextZn = (z + 1) * axisZ.inverse();

                if (lengthSq(xn, zn) > 1) {
                    if (z == 0) {
                        break forX;
                    }
                    break;
                }

                if (!filled && lengthSq(nextXn, zn) <= 1 && lengthSq(xn, nextZn) <= 1) {
                    continue;
                }

                for (int y = 0; y < height; ++y) {
                    affected += setMirroredXZ(session, pos, x, y, z, block);
                }
            }
        }

        return affected;
    }

    /**
     * Implementation of {@link EditSession#makeCone(BlockVector3, Pattern, double, double, int, boolean, double)}.
     */
    public static int makeCone(EditSession session, BlockVector3 pos, Pattern block, double radiusX, double radiusZ,
                               int height, boolean filled, double thickness) throws MaxChangedBlocksException {
        int affected = 0;

        final int ceilRadiusX = (int) Math.ceil(radiusX);
        final int ceilRadiusZ = (int) Math.ceil(radiusZ);
        final double radiusXPow = Math.pow(radiusX, 2);
        final double radiusZPow = Math.pow(radiusZ, 2);
        final double heightPow = Math.pow(height, 2);
        final int layers = Math.abs(height);

        for (int y = 0; y < layers; ++y) {
            double ySquaredMinusHeightOverHeightSquared = Math.pow(y - layers, 2) / heightPow;

            forX:
            for (int x = 0; x <= ceilRadiusX; ++x) {
                double xSquaredOverRadiusX = Math.pow(x, 2) / radiusXPow;

                for (int z = 0; z <= ceilRadiusZ; ++z) {
                    double zSquaredOverRadiusZ = Math.pow(z, 2) / radiusZPow;
                    double distanceFromOriginMinusHeightSquared = xSquaredOverRadiusX + zSquaredOverRadiusZ
                        - ySquaredMinusHeightOverHeightSquared;

                    if (distanceFromOriginMinusHeightSquared > 1) {
                        if (z == 0) {
                            break forX;
                        }
                        break;
                    }

                    if (!filled) {
                        double xNext = Math.pow(x + thickness, 2) / radiusXPow
                            + zSquaredOverRadiusZ - ySquaredMinusHeightOverHeightSquared;
                        double yNext = xSquaredOverRadiusX + zSquaredOverRadiusZ
                            - Math.pow(y + thickness - layers, 2) / heightPow;
                        double zNext = xSquaredOverRadiusX + Math.pow(z + thickness, 2)
                            / radiusZPow - ySquaredMinusHeightOverHeightSquared;
                        if (xNext <= 0 && zNext <= 0 && (yNext <= 0 && y + thickness != layers)) {
                            continue;
                        }
                    }

                    if (distanceFromOriginMinusHeightSquared <= 0) {
                        int yOffset = height < 0 ? -y : y;
                        affected += setMirroredXZ(session, pos, x, yOffset, z, block);
                    }
                }
            }
        }
        return affected;
    }

    /**
     * Implementation of {@link EditSession#makeSphere(BlockVector3, Pattern, double, double, double, boolean)}.
     */
    public static int makeSphere(EditSession session, BlockVector3 pos, Pattern block,
                                 double radiusX, double radiusY, double radiusZ, boolean filled)
        throws MaxChangedBlocksException {
        final Axis axisX = Axis.padded(radiusX);
        final Axis axisY = Axis.padded(radiusY);
        final Axis axisZ = Axis.padded(radiusZ);

        int affected = 0;
        double nextXn = 0;
        forX: for (int x = 0; x <= axisX.ceil(); ++x) {
            final double xn = nextXn;
            nextXn = (x + 1) * axisX.inverse();
            double nextYn = 0;
            forY: for (int y = 0; y <= axisY.ceil(); ++y) {
                final double yn = nextYn;
                nextYn = (y + 1) * axisY.inverse();
                double nextZn = 0;
                for (int z = 0; z <= axisZ.ceil(); ++z) {
                    final double zn = nextZn;
                    nextZn = (z + 1) * axisZ.inverse();

                    if (lengthSq(xn, yn, zn) > 1) {
                        if (z == 0) {
                            if (y == 0) {
                                break forX;
                            }
                            break forY;
                        }
                        break;
                    }

                    if (!filled && lengthSq(nextXn, yn, zn) <= 1 && lengthSq(xn, nextYn, zn) <= 1
                        && lengthSq(xn, yn, nextZn) <= 1) {
                        continue;
                    }

                    affected += setMirroredXYZ(session, pos, x, y, z, block);
                }
            }
        }

        return affected;
    }

    /**
     * Implementation of {@link EditSession#makePyramid(BlockVector3, Pattern, int, boolean)}.
     */
    public static int makePyramid(EditSession session, BlockVector3 position, Pattern block, int size, boolean filled)
        throws MaxChangedBlocksException {
        int affected = 0;

        int height = size;

        for (int y = 0; y <= height; ++y) {
            size--;
            for (int x = 0; x <= size; ++x) {
                for (int z = 0; z <= size; ++z) {
                    if ((filled && z <= size && x <= size) || z == size || x == size) {
                        affected += setMirroredXZ(session, position, x, y, z, block);
                    }
                }
            }
        }

        return affected;
    }

    private ShapeGenerator() {
    }
}
