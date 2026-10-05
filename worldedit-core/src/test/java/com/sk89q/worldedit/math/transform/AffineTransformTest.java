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

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("An affine transform")
public class AffineTransformTest {

    private static final double EPSILON = 1e-9;

    private static final List<Vector3> SAMPLE_POINTS = List.of(
        Vector3.ZERO,
        Vector3.at(1, 0, 0),
        Vector3.at(0, 1, 0),
        Vector3.at(0, 0, 1),
        Vector3.at(3, -7, 11.5),
        Vector3.at(-100, 64, 2048.25)
    );

    private static void assertVectorEquals(Vector3 expected, Vector3 actual) {
        assertVectorEquals(expected, actual, EPSILON);
    }

    private static void assertVectorEquals(Vector3 expected, Vector3 actual, double delta) {
        assertEquals(expected.x(), actual.x(), delta, () -> "x of " + actual + ", expected " + expected);
        assertEquals(expected.y(), actual.y(), delta, () -> "y of " + actual + ", expected " + expected);
        assertEquals(expected.z(), actual.z(), delta, () -> "z of " + actual + ", expected " + expected);
    }

    private static void assertVectorExact(Vector3 expected, Vector3 actual) {
        // a zero delta still treats 0.0 and -0.0 as equal
        assertVectorEquals(expected, actual, 0);
    }

    @Test
    void identity() {
        AffineTransform identity = new AffineTransform();
        assertTrue(identity.isIdentity());
        assertSame(identity, identity.inverse());
        for (Vector3 point : SAMPLE_POINTS) {
            assertEquals(point, identity.apply(point));
        }
        assertFalse(identity.translate(0, 0, 1).isIdentity());
        assertTrue(identity.rotateY(360).isIdentity());
        assertTrue(identity.scale(1).isIdentity());
    }

    @Test
    void translate() {
        AffineTransform t = new AffineTransform().translate(1, -2, 3);
        assertEquals(Vector3.at(4, 3, 9), t.apply(Vector3.at(3, 5, 6)));
        assertEquals(t, new AffineTransform().translate(Vector3.at(1, -2, 3)));
        assertEquals(t, new AffineTransform().translate(BlockVector3.at(1, -2, 3)));
    }

    @Test
    void scale() {
        AffineTransform t = new AffineTransform().scale(2, -1, 0.5);
        assertEquals(Vector3.at(2, -1, 0.5), t.apply(Vector3.ONE));
        assertEquals(Vector3.at(6, 6, 6), new AffineTransform().scale(3).apply(Vector3.at(2, 2, 2)));
        assertEquals(t, new AffineTransform().scale(Vector3.at(2, -1, 0.5)));
    }

    @Test
    void rightAngleRotationsAreExact() {
        // with dCos/dSin, right angles must not leave floating point residue
        assertVectorExact(Vector3.at(0, 0, -1), new AffineTransform().rotateY(90).apply(Vector3.UNIT_X));
        assertVectorExact(Vector3.at(0, 1, 0), new AffineTransform().rotateZ(90).apply(Vector3.UNIT_X));
        assertVectorExact(Vector3.at(0, 0, 1), new AffineTransform().rotateX(90).apply(Vector3.UNIT_Y));
        assertVectorExact(Vector3.at(-1, 0, 0), new AffineTransform().rotateY(180).apply(Vector3.UNIT_X));
        assertVectorExact(Vector3.at(0, 0, 1), new AffineTransform().rotateY(-90).apply(Vector3.UNIT_X));
        assertVectorExact(Vector3.at(-5, 3, -2), new AffineTransform().rotateY(270).rotateY(-90).apply(Vector3.at(5, 3, 2)));
    }

    @Test
    void fourQuarterTurnsAreIdentity() {
        AffineTransform t = new AffineTransform();
        for (int i = 0; i < 4; i++) {
            t = t.rotateY(90);
        }
        assertTrue(t.isIdentity());
    }

    @Test
    void rotationPreservesLength() {
        AffineTransform t = new AffineTransform().rotateX(17).rotateY(-33.3).rotateZ(123);
        for (Vector3 point : SAMPLE_POINTS) {
            assertEquals(point.length(), t.apply(point).length(), EPSILON);
        }
    }

    static List<Arguments> invertibleTransforms() {
        return List.of(
            Arguments.of(new AffineTransform().translate(5, -3, 2)),
            Arguments.of(new AffineTransform().rotateY(90)),
            Arguments.of(new AffineTransform().rotateX(33).rotateZ(-71)),
            Arguments.of(new AffineTransform().scale(2, 3, -4)),
            Arguments.of(new AffineTransform().translate(10, 0, -4).rotateY(45).scale(1.5)),
            Arguments.of(new AffineTransform().scale(-1, 1, 1).translate(-7, 8, 9).rotateZ(12)),
            Arguments.of(new AffineTransform(
                1, 2, 0, 4,
                0, 1, 3, -2,
                5, 0, 1, 7
            ))
        );
    }

    @ParameterizedTest
    @MethodSource("invertibleTransforms")
    @DisplayName("inverse undoes the transform")
    void inverseRoundTrip(AffineTransform t) {
        AffineTransform inverse = t.inverse();
        for (Vector3 point : SAMPLE_POINTS) {
            assertVectorEquals(point, inverse.apply(t.apply(point)));
            assertVectorEquals(point, t.apply(inverse.apply(point)));
        }
        double[] product = t.concatenate(inverse).coefficients();
        assertArrayEquals(new AffineTransform().coefficients(), product, EPSILON);
    }

    @Test
    void concatenateAppliesArgumentFirst() {
        AffineTransform translate = new AffineTransform().translate(10, 0, 0);
        AffineTransform rotate = new AffineTransform().rotateY(90);
        // rotate.concatenate(translate) == rotate * translate: translate first, then rotate
        assertVectorEquals(Vector3.at(0, 0, -10), rotate.concatenate(translate).apply(Vector3.ZERO));
        // preConcatenate is the other order: rotate first, then translate
        assertVectorEquals(Vector3.at(10, 0, 0), rotate.preConcatenate(translate).apply(Vector3.ZERO));
        assertEquals(rotate.concatenate(translate), translate.preConcatenate(rotate));
    }

    @Test
    void chainedBuilderAppliesLastCallFirst() {
        // translate(...).rotateY(...) is translate * rotate, so the rotation happens first
        AffineTransform t = new AffineTransform().translate(10, 0, 0).rotateY(90);
        assertVectorEquals(Vector3.at(10, 0, -1), t.apply(Vector3.UNIT_X));
    }

    @Test
    void combineWithAffineMatchesConcatenate() {
        // As documented on Transform#combine and AffineTransform#combine, combining two affine
        // transforms applies `other` first (this * other), unlike the non-affine case below.
        // Clipboard commands such as //rotate and //flip rely on this order.
        AffineTransform translate = new AffineTransform().translate(10, 0, 0);
        AffineTransform rotate = new AffineTransform().rotateY(90);
        assertEquals(rotate.concatenate(translate), rotate.combine(translate));
        assertEquals(rotate.concatenate(translate), rotate.combine((Transform) translate));
        // translate first, then rotate
        assertVectorEquals(Vector3.at(0, 0, -10), rotate.combine(translate).apply(Vector3.ZERO));
        assertVectorEquals(Vector3.at(0, 0, -10), rotate.combine((Transform) translate).apply(Vector3.ZERO));
    }

    @Test
    void clipboardStyleStackingAppliesNewestTransformFirst() {
        // //rotate 90 followed by //flip (east-west) stacks as holder.getTransform().combine(flip),
        // so the flip happens in the clipboard's original frame, before the rotation.
        Transform rotated = new Identity().combine(new AffineTransform().rotateY(-90));
        AffineTransform flipX = new AffineTransform().scale(-1, 1, 1);
        Transform stacked = rotated.combine(flipX);
        // (1, 0, 0) -> flip -> (-1, 0, 0) -> rotateY(-90) -> (0, 0, -1)
        assertVectorExact(Vector3.at(0, 0, -1), stacked.apply(Vector3.UNIT_X));
        // applying the flip after the rotation would instead give (1, 0, 0) -> (0, 0, 1) -> (0, 0, 1)
        assertVectorExact(Vector3.at(0, 0, 1), flipX.apply(rotated.apply(Vector3.UNIT_X)));
    }

    @Test
    void combineWithNonAffineAppliesThisFirst() {
        AffineTransform rotate = new AffineTransform().rotateY(90);
        Transform offset = new OffsetTransform(Vector3.at(10, 0, 0));
        Transform combined = rotate.combine(offset);
        assertInstanceOf(CombinedTransform.class, combined);
        assertVectorEquals(Vector3.at(10, 0, -1), combined.apply(Vector3.UNIT_X));
        assertVectorEquals(Vector3.UNIT_X, combined.inverse().apply(combined.apply(Vector3.UNIT_X)));
    }

    @Test
    void combinedTransformAppliesInOrderAndInverts() {
        Transform a = new AffineTransform().scale(2);
        Transform b = new OffsetTransform(Vector3.at(1, 2, 3));
        CombinedTransform combined = new CombinedTransform(a, b);
        assertEquals(Vector3.at(3, 4, 5), combined.apply(Vector3.ONE));
        assertVectorEquals(Vector3.ONE, combined.inverse().apply(Vector3.at(3, 4, 5)));
        assertFalse(combined.isIdentity());
        assertTrue(new CombinedTransform(new Identity(), new AffineTransform()).isIdentity());

        Transform flattened = combined.combine(new CombinedTransform(b));
        assertEquals(Vector3.at(4, 6, 8), flattened.apply(Vector3.ONE));
    }

    @Test
    void identityTransform() {
        Identity identity = new Identity();
        assertTrue(identity.isIdentity());
        assertSame(identity, identity.inverse());
        Transform other = new AffineTransform().translate(1, 1, 1);
        assertSame(other, identity.combine(other));
        assertEquals(Vector3.at(1, 2, 3), identity.apply(Vector3.at(1, 2, 3)));
    }

    @Test
    void flips() {
        AffineTransform identity = new AffineTransform();
        assertFalse(identity.isHorizontalFlip());
        assertFalse(identity.isVerticalFlip());

        assertTrue(identity.scale(-1, 1, 1).isHorizontalFlip());
        assertTrue(identity.scale(1, 1, -1).isHorizontalFlip());
        assertFalse(identity.scale(-1, 1, -1).isHorizontalFlip());
        assertFalse(identity.rotateY(90).isHorizontalFlip());

        assertTrue(identity.scale(1, -1, 1).isVerticalFlip());
        assertFalse(identity.scale(1, -1, 1).isHorizontalFlip());
        assertTrue(identity.rotateX(180).isVerticalFlip());
    }

    @Test
    @SuppressWarnings("deprecation")
    void deprecatedArrayConstructor() {
        double[] twelve = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
        assertArrayEquals(twelve, new AffineTransform(twelve).coefficients());

        assertThrows(IllegalArgumentException.class, () -> new AffineTransform(new double[10]));
    }

    @Test
    @SuppressWarnings("deprecation")
    void deprecatedNineElementArrayConstructor() {
        double[] nine = {1, 2, 3, 5, 6, 7, 9, 10, 11};
        assertArrayEquals(
            new double[]{1, 2, 3, 0, 5, 6, 7, 0, 9, 10, 11, 0},
            new AffineTransform(nine).coefficients()
        );
    }

    /**
     * A simple non-affine {@link Transform} implementation, to exercise the
     * {@link CombinedTransform} code paths.
     */
    private record OffsetTransform(Vector3 offset) implements Transform {
        @Override
        public boolean isIdentity() {
            return offset.equals(Vector3.ZERO);
        }

        @Override
        public Vector3 apply(Vector3 input) {
            return input.add(offset);
        }

        @Override
        public Transform inverse() {
            return new OffsetTransform(offset.multiply(-1));
        }

        @Override
        public Transform combine(Transform other) {
            return new CombinedTransform(this, other);
        }
    }
}
