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

package com.sk89q.worldedit.util.image;

/**
 * A colour in the CIE L*a*b* colour space (D65 white point).
 *
 * <p>Euclidean distance in this space approximates perceived colour
 * difference far better than distance in sRGB.</p>
 *
 * @param l lightness, 0 to 100
 * @param a green-red axis
 * @param b blue-yellow axis
 */
public record LabColor(double l, double a, double b) {

    // D65 reference white
    private static final double XN = 0.95047;
    private static final double YN = 1.0;
    private static final double ZN = 1.08883;

    private static final double[] SRGB_TO_LINEAR = new double[256];

    static {
        for (int i = 0; i < 256; i++) {
            double c = i / 255.0;
            SRGB_TO_LINEAR[i] = c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
        }
    }

    /**
     * Convert a packed sRGB colour ({@code 0xRRGGBB}; alpha is ignored) to Lab.
     *
     * @param rgb the colour
     * @return the Lab colour
     */
    public static LabColor fromRgb(int rgb) {
        return fromRgb((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
    }

    /**
     * Convert an sRGB colour to Lab.
     *
     * @param red red, 0-255
     * @param green green, 0-255
     * @param blue blue, 0-255
     * @return the Lab colour
     */
    public static LabColor fromRgb(int red, int green, int blue) {
        double r = SRGB_TO_LINEAR[red];
        double g = SRGB_TO_LINEAR[green];
        double bl = SRGB_TO_LINEAR[blue];

        double x = (0.4124564 * r + 0.3575761 * g + 0.1804375 * bl) / XN;
        double y = (0.2126729 * r + 0.7151522 * g + 0.0721750 * bl) / YN;
        double z = (0.0193339 * r + 0.1191920 * g + 0.9503041 * bl) / ZN;

        double fx = labCurve(x);
        double fy = labCurve(y);
        double fz = labCurve(z);
        return new LabColor(116 * fy - 16, 500 * (fx - fy), 200 * (fy - fz));
    }

    private static double labCurve(double t) {
        final double delta = 6.0 / 29.0;
        if (t > delta * delta * delta) {
            return Math.cbrt(t);
        }
        return t / (3 * delta * delta) + 4.0 / 29.0;
    }

    /**
     * Squared CIE76 colour difference to another colour.
     *
     * @param other the other colour
     * @return the squared distance
     */
    public double distanceSquared(LabColor other) {
        double dl = l - other.l;
        double da = a - other.a;
        double db = b - other.b;
        return dl * dl + da * da + db * db;
    }
}
