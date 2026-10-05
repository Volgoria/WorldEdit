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

package com.sk89q.worldedit.function.block;

import com.sk89q.worldedit.BaseWorldEditTest;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.math.transform.Identity;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockType;
import org.enginehub.linbus.tree.LinCompoundTag;
import org.enginehub.linbus.tree.LinTagType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Extent block copy")
class ExtentBlockCopyTest extends BaseWorldEditTest {

    private static BaseBlock block;

    @BeforeAll
    static void setUpBlock() {
        BlockType type = new BlockType("blockcopytest:sign");
        block = type.getDefaultState().toBaseBlock(LinCompoundTag.builder().putByte("Rot", (byte) 4).build());
    }

    @Test
    @DisplayName("copies the block unchanged with an identity transform")
    void identityCopy() throws Exception {
        Extent source = mock(Extent.class);
        Extent destination = mock(Extent.class);
        BlockVector3 position = BlockVector3.at(5, 6, 7);
        when(source.getFullBlock(position)).thenReturn(block);
        when(destination.setBlock(BlockVector3.at(104, 205, 306), block)).thenReturn(true);

        ExtentBlockCopy copy = new ExtentBlockCopy(source, BlockVector3.at(1, 1, 1),
            destination, BlockVector3.at(100, 200, 300), new Identity());
        assertTrue(copy.apply(position));
        verify(destination).setBlock(BlockVector3.at(104, 205, 306), block);
    }

    @Test
    @DisplayName("copies with an identity affine transform like the identity transform")
    void identityAffineCopy() throws Exception {
        Extent source = mock(Extent.class);
        Extent destination = mock(Extent.class);
        BlockVector3 position = BlockVector3.at(-5, 6, -7);
        when(source.getFullBlock(position)).thenReturn(block);

        new ExtentBlockCopy(source, BlockVector3.ZERO, destination, BlockVector3.at(1, 2, 3), new AffineTransform())
            .apply(position);
        verify(destination).setBlock(BlockVector3.at(-4, 8, -4), block);
    }

    @Test
    @DisplayName("rotates position and NBT rotation with a rotating transform")
    void rotatedCopy() throws Exception {
        Extent source = mock(Extent.class);
        Extent destination = mock(Extent.class);
        BlockVector3 position = BlockVector3.at(2, 0, 0);
        when(source.getFullBlock(position)).thenReturn(block);
        ArgumentCaptor<BlockVector3> positionCaptor = ArgumentCaptor.forClass(BlockVector3.class);
        ArgumentCaptor<BaseBlock> blockCaptor = ArgumentCaptor.forClass(BaseBlock.class);

        new ExtentBlockCopy(source, BlockVector3.ZERO, destination, BlockVector3.ZERO,
            new AffineTransform().rotateY(90)).apply(position);
        verify(destination).setBlock(positionCaptor.capture(), blockCaptor.capture());

        BlockVector3 rotated = positionCaptor.getValue();
        assertEquals(0, rotated.x());
        assertEquals(2, Math.abs(rotated.z()));
        assertEquals(block.getBlockType(), blockCaptor.getValue().getBlockType());
        LinCompoundTag nbt = blockCaptor.getValue().getNbt();
        assertNotNull(nbt);
        assertNotEquals(4, nbt.getTag("Rot", LinTagType.byteTag()).valueAsByte(), "Rot should have changed");
    }
}
