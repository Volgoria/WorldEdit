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

package com.sk89q.worldedit.math.transform;

import com.sk89q.worldedit.math.Vector3;

/**
 * Makes a transformation of {@link Vector3}s.
 */
public interface Transform {

    /**
     * Return whether this transform is an identity.
     *
     * <p>If it is not known, then {@code false} must be returned.</p>
     *
     * @return true if identity
     */
    boolean isIdentity();

    /**
     * Returns the result of applying the function to the input.
     *
     * @param input the input
     * @return the result
     */
    Vector3 apply(Vector3 input);

    /**
     * Create a new inverse transform.
     *
     * @return a new inverse transform
     */
    Transform inverse();

    /**
     * Create a new {@link Transform} that combines this transform with another.
     *
     * <p>The order in which the two transforms are applied is
     * implementation-dependent:</p>
     *
     * <ul>
     *     <li>{@link CombinedTransform}, {@link ScaleAndTranslateTransform}
     *     and {@link AffineTransform} combined with a transform that is
     *     <em>not</em> an {@code AffineTransform} apply this transform first
     *     and {@code other} second.</li>
     *     <li>{@link AffineTransform} combined with another
     *     {@code AffineTransform} applies {@code other} <em>first</em> and this
     *     transform second (matrix product {@code this * other}, see
     *     {@link AffineTransform#concatenate(AffineTransform)}).</li>
     *     <li>{@link Identity} returns {@code other}, so the order does not
     *     matter.</li>
     * </ul>
     *
     * <p>The affine behaviour is relied upon by clipboard transforms:
     * {@code //rotate}, {@code //flip} and randomly rotated clipboard brushes
     * combine the clipboard's current transform with the new one, so a newly
     * added rotation or flip is applied in the clipboard's original frame,
     * before the transforms that were added earlier. Changing it would change
     * the result of stacking a flip on a rotation, or of rotations around
     * different axes. Callers that need a specific order should use
     * {@link AffineTransform#concatenate(AffineTransform)} /
     * {@link AffineTransform#preConcatenate(AffineTransform)} or construct a
     * {@link CombinedTransform}, which always applies its transforms in
     * order.</p>
     *
     * @param other the other transform
     * @return a new transform
     */
    Transform combine(Transform other);

}
