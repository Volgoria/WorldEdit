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

package com.sk89q.worldedit.math;

import com.sk89q.worldedit.util.test.VariedVectorGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("A 3D block vector")
public class BlockVector3Test {

    @Test
    @DisplayName("survives a round-trip through long-packing")
    void longPackingRoundTrip() {
        new VariedVectorGenerator(true, 25, 25).makeVectorsStream()
            .forEach(vec ->
                assertEquals(
                    vec,
                    BlockVector3.fromLongPackedForm(vec.toLongPackedForm())
                )
            );
    }

    @Test
    @DisplayName("long-packs the extreme corners of the packable range")
    void longPackingBounds() {
        BlockVector3 min = BlockVector3.at(-30_000_000, -2048, -30_000_000);
        BlockVector3 max = BlockVector3.at(30_000_000, 2047, 30_000_000);
        assertTrue(BlockVector3.isLongPackable(min));
        assertTrue(BlockVector3.isLongPackable(max));
        assertEquals(min, BlockVector3.fromLongPackedForm(min.toLongPackedForm()));
        assertEquals(max, BlockVector3.fromLongPackedForm(max.toLongPackedForm()));
    }

    @Test
    @DisplayName("refuses to long-pack vectors outside of the packable range")
    void longPackingRejectsOutOfRange() {
        for (BlockVector3 vec : new BlockVector3[] {
            BlockVector3.at(30_000_001, 0, 0),
            BlockVector3.at(0, 0, -30_000_001),
            BlockVector3.at(0, 2048, 0),
            BlockVector3.at(0, -2049, 0),
        }) {
            assertFalse(BlockVector3.isLongPackable(vec));
            assertThrows(IllegalArgumentException.class, vec::toLongPackedForm);
        }
    }

    @Test
    @DisplayName("returns shared constants from the factory")
    void factoryReturnsSharedConstants() {
        assertSame(BlockVector3.ZERO, BlockVector3.at(0, 0, 0));
        assertSame(BlockVector3.ONE, BlockVector3.at(1, 1, 1));
        assertEquals(new BlockVector3(0, 1, 0), BlockVector3.at(0, 1, 0));
    }

    @Test
    @DisplayName("floors doubles when created from them")
    void factoryFloorsDoubles() {
        assertEquals(BlockVector3.at(-1, 0, 2), BlockVector3.at(-0.5, 0.99, 2.0));
        assertEquals(BlockVector3.at(-3, -1, 0), BlockVector3.at(-2.0001, -1e-9, 1e-9));
    }

    @Test
    @DisplayName("does integer arithmetic")
    void arithmetic() {
        BlockVector3 a = BlockVector3.at(7, -9, 4);
        BlockVector3 b = BlockVector3.at(2, 3, -4);
        assertEquals(BlockVector3.at(9, -6, 0), a.add(b));
        assertEquals(BlockVector3.at(5, -12, 8), a.subtract(b));
        assertEquals(BlockVector3.at(14, -27, -16), a.multiply(b));
        assertEquals(BlockVector3.at(11, -3, -4), a.add(b, b));
        assertEquals(BlockVector3.at(3, -15, 12), a.subtract(b, b));
        // integer division truncates towards zero...
        assertEquals(BlockVector3.at(3, -4, 2), a.divide(2));
        assertEquals(BlockVector3.at(3, -3, -1), a.divide(b));
        // ...while shifting rounds towards negative infinity
        assertEquals(BlockVector3.at(3, -5, 2), a.shr(1));
        assertEquals(BlockVector3.at(0, -1, 0), a.shr(4));
        assertEquals(BlockVector3.at(14, -18, 8), a.shl(1));
        assertEquals(BlockVector3.at(7, 9, 4), a.abs());
    }

    @Test
    @DisplayName("computes lengths, dot and cross products")
    void geometry() {
        BlockVector3 v = BlockVector3.at(2, 3, 6);
        assertEquals(49, v.lengthSq());
        assertEquals(7, v.length(), 0);
        assertEquals(49, BlockVector3.ZERO.distanceSq(v));
        assertEquals(7, v.distance(BlockVector3.ZERO), 0);
        assertEquals(1 * 2 + 2 * 3 + 3 * 6, BlockVector3.at(1, 2, 3).dot(v), 0);
        assertEquals(BlockVector3.UNIT_Z, BlockVector3.UNIT_X.cross(BlockVector3.UNIT_Y));
        assertEquals(BlockVector3.UNIT_MINUS_Z, BlockVector3.UNIT_Y.cross(BlockVector3.UNIT_X));
        assertEquals(BlockVector3.UNIT_Y, BlockVector3.at(0, 5, 0).normalize());
    }

    @Test
    @DisplayName("compares, clamps and converts")
    void minMaxClampAndConvert() {
        BlockVector3 a = BlockVector3.at(-1, 5, -3);
        BlockVector3 b = BlockVector3.at(2, -6, -3);
        assertEquals(BlockVector3.at(-1, -6, -3), a.getMinimum(b));
        assertEquals(BlockVector3.at(2, 5, -3), a.getMaximum(b));
        assertTrue(BlockVector3.at(0, 0, -3).containedWithin(a.getMinimum(b), a.getMaximum(b)));
        assertFalse(BlockVector3.at(0, 0, -2).containedWithin(a.getMinimum(b), a.getMaximum(b)));

        assertEquals(BlockVector3.at(-1, 0, -3), a.clampY(-10, 0));
        assertSame(a, a.clampY(0, 10));
        assertThrows(IllegalArgumentException.class, () -> a.clampY(1, 0));

        assertEquals(BlockVector2.at(-1, -3), a.toBlockVector2());
        assertEquals(Vector3.at(-1, 5, -3), a.toVector3());
        assertEquals("-1,5,-3", a.toParserString());
    }

    @Test
    @DisplayName("rotates exactly by right angles in transform2D")
    void transform2DRightAngles() {
        BlockVector3 v = BlockVector3.at(2, 64, 0);
        assertEquals(BlockVector3.at(0, 64, 2), v.transform2D(90, 0, 0, 0, 0));
        assertEquals(BlockVector3.at(-2, 64, 0), v.transform2D(180, 0, 0, 0, 0));
        assertEquals(BlockVector3.at(0, 64, -2), v.transform2D(270, 0, 0, 0, 0));
        assertEquals(BlockVector3.at(11, 64, -2), v.transform2D(270, 0, 0, 11, 0));
        assertEquals(v, v.transform2D(360, 0, 0, 0, 0));
    }

    @Test
    @DisplayName("sorts by Y, then Z, then X")
    void yzxComparator() {
        List<BlockVector3> list = new ArrayList<>(List.of(
            BlockVector3.at(0, 1, 0), BlockVector3.at(1, 0, 1), BlockVector3.at(0, 0, 1), BlockVector3.at(5, 0, 0)
        ));
        list.sort(BlockVector3.sortByCoordsYzx());
        assertEquals(List.of(
            BlockVector3.at(5, 0, 0), BlockVector3.at(0, 0, 1), BlockVector3.at(1, 0, 1), BlockVector3.at(0, 1, 0)
        ), list);
    }
}
