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

package com.sk89q.worldedit.function.generator.shape;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Generated shapes")
class GeneratedShapeTest {

    private static Set<BlockVector3> offsets(GeneratedShape shape) {
        Set<BlockVector3> offsets = shape.getOffsets();
        BlockVector3 min = shape.getMinimumOffset();
        BlockVector3 max = shape.getMaximumOffset();
        for (BlockVector3 offset : offsets) {
            assertTrue(offset.containedWithin(min, max), () -> offset + " is outside the bounding box");
        }
        return offsets;
    }

    private static Set<BlockVector3> mirrorX(Set<BlockVector3> offsets) {
        return offsets.stream().map(v -> BlockVector3.at(-v.x(), v.y(), v.z())).collect(Collectors.toSet());
    }

    private static Set<BlockVector3> mirrorY(Set<BlockVector3> offsets) {
        return offsets.stream().map(v -> BlockVector3.at(v.x(), -v.y(), v.z())).collect(Collectors.toSet());
    }

    private static Set<BlockVector3> mirrorZ(Set<BlockVector3> offsets) {
        return offsets.stream().map(v -> BlockVector3.at(v.x(), v.y(), -v.z())).collect(Collectors.toSet());
    }

    @Nested
    @DisplayName("ShapeAxis")
    class Axis {
        @Test
        void fromDirectionPicksDominantComponent() {
            assertEquals(ShapeAxis.Y, ShapeAxis.fromDirection(BlockVector3.UNIT_Y));
            assertEquals(ShapeAxis.Y, ShapeAxis.fromDirection(BlockVector3.UNIT_MINUS_Y));
            assertEquals(ShapeAxis.X, ShapeAxis.fromDirection(BlockVector3.UNIT_MINUS_X));
            assertEquals(ShapeAxis.Z, ShapeAxis.fromDirection(BlockVector3.UNIT_Z));
            assertEquals(ShapeAxis.Z, ShapeAxis.fromDirection(BlockVector3.at(1, 0, -2)));
        }

        @Test
        void fromHorizontalDirectionIgnoresVertical() {
            assertNull(ShapeAxis.fromHorizontalDirection(BlockVector3.UNIT_Y));
            assertEquals(ShapeAxis.X, ShapeAxis.fromHorizontalDirection(BlockVector3.at(1, 5, 0)));
            assertEquals(ShapeAxis.Z, ShapeAxis.fromHorizontalDirection(BlockVector3.UNIT_MINUS_Z));
        }

        @Test
        void localAndWorldRoundTrip() {
            for (ShapeAxis axis : ShapeAxis.values()) {
                BlockVector3 world = axis.toWorld(1, 2, 3);
                assertEquals(BlockVector3.at(1, 2, 3), axis.toLocal(world.x(), world.y(), world.z()));
            }
            assertEquals(BlockVector3.at(3, 2, 1), ShapeAxis.X.toWorld(1, 2, 3));
            assertEquals(BlockVector3.at(1, 3, 2), ShapeAxis.Y.toWorld(1, 2, 3));
            assertEquals(BlockVector3.at(1, 2, 3), ShapeAxis.Z.toWorld(1, 2, 3));
        }
    }

    @Nested
    @DisplayName("Torus")
    class Torus {
        @Test
        void flatRingLiesInHorizontalPlane() {
            Set<BlockVector3> ring = offsets(new TorusShape(3, 0, ShapeAxis.Y, false));
            assertFalse(ring.isEmpty());
            assertTrue(ring.stream().allMatch(v -> v.y() == 0));
            assertTrue(ring.contains(BlockVector3.at(3, 0, 0)));
            assertTrue(ring.contains(BlockVector3.at(0, 0, -3)));
            assertFalse(ring.contains(BlockVector3.ZERO));
            assertFalse(ring.contains(BlockVector3.at(1, 0, 0)));
            assertEquals(ring, mirrorX(ring));
            assertEquals(ring, mirrorZ(ring));
        }

        @Test
        void tubeHasMinorRadius() {
            Set<BlockVector3> torus = offsets(new TorusShape(5, 2, ShapeAxis.Y, false));
            assertTrue(torus.contains(BlockVector3.at(5, 2, 0)));
            assertTrue(torus.contains(BlockVector3.at(7, 0, 0)));
            assertTrue(torus.contains(BlockVector3.at(3, 0, 0)));
            assertFalse(torus.contains(BlockVector3.at(5, 3, 0)));
            assertFalse(torus.contains(BlockVector3.at(2, 0, 0)));
            assertFalse(torus.contains(BlockVector3.at(8, 0, 0)));
            assertEquals(torus, mirrorY(torus));
        }

        @Test
        void axisRotatesTheTorus() {
            Set<BlockVector3> flat = offsets(new TorusShape(4, 1, ShapeAxis.Y, false));
            Set<BlockVector3> standing = offsets(new TorusShape(4, 1, ShapeAxis.Z, false));
            assertEquals(flat.size(), standing.size());
            // Y-axis local (u, v, w) = world (x, z, y); Z-axis local = world (x, y, z)
            Set<BlockVector3> rotated = flat.stream()
                .map(v -> BlockVector3.at(v.x(), v.z(), v.y()))
                .collect(Collectors.toSet());
            assertEquals(rotated, standing);
            assertTrue(standing.contains(BlockVector3.at(0, 4, 0)));
        }

        @Test
        void hollowKeepsOnlySurface() {
            Set<BlockVector3> solid = offsets(new TorusShape(6, 2, ShapeAxis.Y, false));
            Set<BlockVector3> hollow = offsets(new TorusShape(6, 2, ShapeAxis.Y, true));
            assertTrue(solid.containsAll(hollow));
            assertTrue(hollow.size() < solid.size());
            assertFalse(hollow.contains(BlockVector3.at(6, 0, 0)));
            assertTrue(hollow.contains(BlockVector3.at(8, 0, 0)));
        }
    }

    @Nested
    @DisplayName("Disk")
    class Disk {
        @Test
        void verticalDiskMatchesCircle() {
            Set<BlockVector3> disk = offsets(new DiskShape(2, 2, 1, ShapeAxis.Z, false));
            assertTrue(disk.stream().allMatch(v -> v.z() == 0));
            // Same rounding as //cyl: radius + 0.5
            assertEquals(21, disk.size());
            assertTrue(disk.contains(BlockVector3.at(0, 2, 0)));
            assertTrue(disk.contains(BlockVector3.at(0, -2, 0)));
            assertFalse(disk.contains(BlockVector3.at(2, 2, 0)));
        }

        @Test
        void thicknessExtendsAlongAxis() {
            Set<BlockVector3> disk = offsets(new DiskShape(2, 2, 3, ShapeAxis.X, false));
            assertEquals(63, disk.size());
            assertEquals(Set.of(-1, 0, 1), disk.stream().map(BlockVector3::x).collect(Collectors.toSet()));
        }

        @Test
        void ellipticalRadii() {
            Set<BlockVector3> disk = offsets(new DiskShape(4, 1, 1, ShapeAxis.Y, false));
            assertTrue(disk.contains(BlockVector3.at(4, 0, 0)));
            assertTrue(disk.contains(BlockVector3.at(0, 0, 1)));
            assertFalse(disk.contains(BlockVector3.at(0, 0, 2)));
        }

        @Test
        void hollowDiskIsRing() {
            Set<BlockVector3> ring = offsets(new DiskShape(3, 3, 2, ShapeAxis.Y, true));
            assertFalse(ring.contains(BlockVector3.ZERO));
            assertFalse(ring.contains(BlockVector3.at(0, 1, 0)));
            assertTrue(ring.contains(BlockVector3.at(3, 0, 0)));
            assertTrue(ring.contains(BlockVector3.at(3, 1, 0)));
        }
    }

    @Nested
    @DisplayName("Dome")
    class Dome {
        @Test
        void upperHalfOfSphere() {
            Set<BlockVector3> dome = offsets(new DomeShape(3, 3, 3, false, false));
            assertTrue(dome.stream().allMatch(v -> v.y() >= 0));
            assertTrue(dome.contains(BlockVector3.at(0, 3, 0)));
            assertTrue(dome.contains(BlockVector3.at(3, 0, 0)));
            assertTrue(dome.contains(BlockVector3.ZERO));
            assertFalse(dome.contains(BlockVector3.at(0, 4, 0)));
            assertEquals(dome, mirrorX(dome));
            assertEquals(dome, mirrorZ(dome));
        }

        @Test
        void invertedIsMirrored() {
            Set<BlockVector3> dome = offsets(new DomeShape(4, 2, 3, false, false));
            Set<BlockVector3> bowl = offsets(new DomeShape(4, 2, 3, true, false));
            assertEquals(mirrorY(dome), bowl);
        }

        @Test
        void hollowDomeHasOpenFloor() {
            Set<BlockVector3> solid = offsets(new DomeShape(4, 4, 4, false, false));
            Set<BlockVector3> hollow = offsets(new DomeShape(4, 4, 4, false, true));
            assertTrue(solid.containsAll(hollow));
            assertFalse(hollow.contains(BlockVector3.ZERO));
            assertFalse(hollow.contains(BlockVector3.at(2, 0, 0)));
            assertTrue(hollow.contains(BlockVector3.at(4, 0, 0)));
            assertTrue(hollow.contains(BlockVector3.at(0, 4, 0)));
        }
    }

    @Nested
    @DisplayName("Helix")
    class Helix {
        @Test
        void singleStrandClimbsEveryLayer() {
            Set<BlockVector3> helix = offsets(new HelixShape(4, 12, 2, 1, 1));
            assertTrue(helix.contains(BlockVector3.at(4, 0, 0)));
            assertEquals(0, shapeMinY(helix));
            assertEquals(11, shapeMaxY(helix));
            for (int y = 0; y < 12; y++) {
                int layer = y;
                assertTrue(helix.stream().anyMatch(v -> v.y() == layer), "layer " + y + " is empty");
            }
            for (BlockVector3 v : helix) {
                double distance = Math.sqrt(v.x() * v.x() + v.z() * v.z());
                assertTrue(Math.abs(distance - 4) <= 0.75, () -> v + " is not on the helix");
            }
        }

        @Test
        void doubleHelixHasOpposedStrands() {
            Set<BlockVector3> single = offsets(new HelixShape(3, 8, 1, 1, 1));
            Set<BlockVector3> helix = offsets(new HelixShape(3, 8, 1, 1, 2));
            assertTrue(helix.containsAll(single));
            assertTrue(helix.contains(BlockVector3.at(3, 0, 0)));
            assertTrue(helix.contains(BlockVector3.at(-3, 0, 0)));
        }

        @Test
        void thicknessWidensStrand() {
            Set<BlockVector3> thin = offsets(new HelixShape(5, 10, 1, 1, 1));
            Set<BlockVector3> thick = offsets(new HelixShape(5, 10, 1, 3, 1));
            assertTrue(thick.containsAll(thin));
            assertTrue(thick.contains(BlockVector3.at(6, 0, 0)));
            assertTrue(thick.contains(BlockVector3.at(5, -1, 0)));
        }

        private int shapeMinY(Set<BlockVector3> offsets) {
            return offsets.stream().mapToInt(BlockVector3::y).min().orElseThrow();
        }

        private int shapeMaxY(Set<BlockVector3> offsets) {
            return offsets.stream().mapToInt(BlockVector3::y).max().orElseThrow();
        }
    }

    @Nested
    @DisplayName("Arch")
    class Arch {
        @Test
        void archAcrossZAxis() {
            Set<BlockVector3> arch = offsets(new ArchShape(7, 5, 1, 1, ShapeAxis.Z));
            assertTrue(arch.stream().allMatch(v -> v.z() == 0 && v.y() >= 0 && v.y() <= 4));
            assertTrue(arch.contains(BlockVector3.at(3, 0, 0)));
            assertTrue(arch.contains(BlockVector3.at(-3, 0, 0)));
            assertTrue(arch.contains(BlockVector3.at(0, 4, 0)));
            assertFalse(arch.contains(BlockVector3.ZERO));
            assertFalse(arch.contains(BlockVector3.at(0, 3, 0)));
            assertFalse(arch.contains(BlockVector3.at(2, 0, 0)));
            assertEquals(arch, mirrorX(arch));
        }

        @Test
        void archAcrossXAxisWithDepth() {
            Set<BlockVector3> arch = offsets(new ArchShape(7, 5, 1, 3, ShapeAxis.X));
            Set<BlockVector3> flat = offsets(new ArchShape(7, 5, 1, 1, ShapeAxis.X));
            assertEquals(flat.size() * 3, arch.size());
            assertEquals(Set.of(-1, 0, 1), arch.stream().map(BlockVector3::x).collect(Collectors.toSet()));
            assertTrue(arch.contains(BlockVector3.at(0, 0, 3)));
            assertTrue(arch.contains(BlockVector3.at(0, 0, -3)));
        }

        @Test
        void thickArchIsSolidBand() {
            Set<BlockVector3> thin = offsets(new ArchShape(9, 6, 1, 1, ShapeAxis.Z));
            Set<BlockVector3> thick = offsets(new ArchShape(9, 6, 2, 1, ShapeAxis.Z));
            assertTrue(thick.containsAll(thin));
            assertTrue(thick.contains(BlockVector3.at(3, 0, 0)));
            assertFalse(thick.contains(BlockVector3.at(2, 0, 0)));
        }

        @Test
        void verticalAxisIsRejected() {
            assertThrows(IllegalArgumentException.class, () -> new ArchShape(5, 5, 1, 1, ShapeAxis.Y));
        }
    }

    @Nested
    @DisplayName("Placement")
    class Placement {
        @Test
        void placesEveryOffsetRelativeToOrigin() throws WorldEditException {
            GeneratedShape shape = new TorusShape(3, 1, ShapeAxis.Y, true);
            BlockVector3 origin = BlockVector3.at(100, 64, -20);
            Extent extent = mock(Extent.class);
            Pattern pattern = mock(Pattern.class);
            when(extent.setBlock(any(BlockVector3.class), any())).thenReturn(true);

            int affected = shape.generate(extent, origin, pattern);

            Set<BlockVector3> expected = new HashSet<>();
            for (BlockVector3 offset : shape.getOffsets()) {
                expected.add(origin.add(offset));
            }
            assertEquals(expected.size(), affected);

            ArgumentCaptor<BlockVector3> positions = ArgumentCaptor.forClass(BlockVector3.class);
            verify(extent, atLeastOnce()).setBlock(positions.capture(), any());
            List<BlockVector3> placed = positions.getAllValues();
            assertEquals(expected.size(), placed.size());
            assertEquals(expected, new HashSet<>(placed));
            verify(pattern, atLeastOnce()).applyBlock(origin.add(4, 0, 0));
        }

        @Test
        void unchangedBlocksAreNotCounted() throws WorldEditException {
            GeneratedShape shape = new DiskShape(2, 2, 1, ShapeAxis.Y, false);
            Extent extent = mock(Extent.class);
            Pattern pattern = mock(Pattern.class);
            when(extent.setBlock(any(BlockVector3.class), any())).thenReturn(false);
            when(extent.setBlock(BlockVector3.ZERO, (BaseBlock) null)).thenReturn(true);

            assertEquals(1, shape.generate(extent, BlockVector3.ZERO, pattern));
        }
    }
}
