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

package com.sk89q.worldedit.session;

import com.sk89q.worldedit.math.transform.AffineTransform;

import java.util.random.RandomGenerator;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Helpers for applying a random rotation around the Y axis to a clipboard.
 */
public final class RandomRotation {

    private RandomRotation() {
    }

    /**
     * Pick a random clockwise rotation of 0, 90, 180 or 270 degrees.
     *
     * @param random the random generator
     * @return the angle in degrees
     */
    public static int randomQuarterTurn(RandomGenerator random) {
        checkNotNull(random);
        return random.nextInt(4) * 90;
    }

    /**
     * Create a new holder for the same clipboard, with its transform
     * additionally rotated clockwise around the Y axis. The original
     * holder is not modified.
     *
     * @param holder the holder
     * @param degrees the clockwise angle, a multiple of 90
     * @return a new holder
     */
    public static ClipboardHolder rotated(ClipboardHolder holder, int degrees) {
        checkNotNull(holder);
        checkArgument(degrees % 90 == 0, "degrees must be a multiple of 90");
        ClipboardHolder result = new ClipboardHolder(holder.getClipboard());
        // Negated to match //rotate, where a positive angle is clockwise
        result.setTransform(holder.getTransform().combine(new AffineTransform().rotateY(-degrees)));
        return result;
    }
}
