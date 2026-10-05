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

import com.sk89q.worldedit.BaseWorldEditTest;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector2;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.CylinderRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.registry.Registry;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Heightmap import/export and top view rendering")
class HeightmapTest extends BaseWorldEditTest {

    private static BlockState air;
    private static BlockState stone;
    private static BlockState grass;
    private static BlockState dirt;

    @BeforeAll
    static void registerBlocks() {
        air = block("minecraft:air");
        stone = block("minecraft:stone");
        grass = block("minecraft:grass_block");
        dirt = block("minecraft:dirt");
    }

    @AfterAll
    static void clearBlocks() throws Exception {
        Field map = Registry.class.getDeclaredField("map");
        map.setAccessible(true);
        ((Map<?, ?>) map.get(BlockType.REGISTRY)).clear();
    }

    private static BlockState block(String id) {
        BlockType type = BlockType.REGISTRY.get(id);
        if (type == null) {
            type = new BlockType(id);
            BlockType.REGISTRY.register(id, type);
        }
        return type.getDefaultState();
    }

    /**
     * Create an in-memory extent filled with air.
     */
    private static BlockArrayClipboard emptyExtent(Region region) throws WorldEditException {
        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
        for (BlockVector3 pos : region.getBoundingBox()) {
            clipboard.setBlock(pos, air);
        }
        return clipboard;
    }

    @Test
    @DisplayName("export then import on another extent reproduces the surface heights")
    void roundTrip(@TempDir Path dir) throws Exception {
        CuboidRegion region = new CuboidRegion(BlockVector3.at(-5, 10, 20), BlockVector3.at(14, 41, 31));
        int width = 20;
        int length = 12;
        int regionHeight = 32;

        // Random terrain, including empty columns and full-height columns
        BlockArrayClipboard source = emptyExtent(region);
        Random random = new Random(1234);
        int[] expected = new int[width * length];
        for (int z = 0; z < length; z++) {
            for (int x = 0; x < width; x++) {
                int h = random.nextInt(regionHeight + 1);
                expected[z * width + x] = h;
                for (int dy = 0; dy < h; dy++) {
                    source.setBlock(BlockVector3.at(-5 + x, 10 + dy, 20 + z), stone);
                }
            }
        }

        Heightmap exported = Heightmap.fromSurface(source, region);
        assertArrayEquals(expected, exported.toArray());

        // Through an actual PNG file
        File file = dir.resolve("heights.png").toFile();
        Images.writePng(exported.toImage(regionHeight), file);
        BufferedImage image = Images.read(file, Images.MAX_PIXELS);
        assertEquals(width, image.getWidth());
        assertEquals(length, image.getHeight());

        Heightmap imported = Heightmap.fromValues(Heightmap.readGrey(image), image.getWidth(), image.getHeight(),
            width, length, regionHeight);
        assertArrayEquals(expected, imported.toArray());

        BlockArrayClipboard target = emptyExtent(region);
        imported.apply(target, region, grass, dirt, 3, stone, false);
        assertArrayEquals(expected, Heightmap.fromSurface(target, region).toArray());

        // Layers: grass on top, then dirt, then stone
        for (int x = 0; x < width; x++) {
            int h = expected[x];
            if (h >= 5) {
                assertEquals(grass, target.getBlock(BlockVector3.at(-5 + x, 10 + h - 1, 20)));
                assertEquals(dirt, target.getBlock(BlockVector3.at(-5 + x, 10 + h - 2, 20)));
                assertEquals(dirt, target.getBlock(BlockVector3.at(-5 + x, 10 + h - 4, 20)));
                assertEquals(stone, target.getBlock(BlockVector3.at(-5 + x, 10 + h - 5, 20)));
            }
        }
    }

    @Test
    @DisplayName("8-bit images are scaled to the target size and height")
    void scaling() {
        BufferedImage image = new BufferedImage(2, 1, BufferedImage.TYPE_BYTE_GRAY);
        image.getRaster().setSample(0, 0, 0, 0);
        image.getRaster().setSample(1, 0, 0, 255);
        double[] grey = Heightmap.readGrey(image);
        assertArrayEquals(new double[] { 0, 1 }, grey, 1e-9);

        Heightmap heightmap = Heightmap.fromValues(grey, 2, 1, 8, 3, 70);
        for (int z = 0; z < 3; z++) {
            assertEquals(0, heightmap.getHeight(0, z));
            assertEquals(70, heightmap.getHeight(7, z));
            for (int x = 1; x < 8; x++) {
                assertTrue(heightmap.getHeight(x, z) >= heightmap.getHeight(x - 1, z), "Should be monotonic");
            }
        }
        // Halving the max height halves the heights
        Heightmap half = Heightmap.fromValues(grey, 2, 1, 2, 1, 35);
        assertArrayEquals(new int[] { 0, 35 }, half.toArray());

        // Colour images use their luma
        BufferedImage colour = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        colour.setRGB(0, 0, 0xFFFFFF);
        assertArrayEquals(new double[] { 1 }, Heightmap.readGrey(colour), 1e-9);
    }

    @Test
    @DisplayName("heights are clamped to the region and columns outside the footprint are untouched")
    void footprintAndClamp() throws Exception {
        CylinderRegion cylinder = new CylinderRegion(BlockVector3.at(0, 0, 0),
            Vector2.at(3, 3), 0, 4);
        BlockArrayClipboard target = emptyExtent(cylinder);
        BlockVector3 min = cylinder.getMinimumPoint();
        BlockVector3 max = cylinder.getMaximumPoint();
        int width = max.x() - min.x() + 1;
        int length = max.z() - min.z() + 1;
        int[] heights = new int[width * length];
        Arrays.fill(heights, 100);
        new Heightmap(width, length, heights).apply(target, cylinder, stone, stone, 0, stone, false);

        // Corner of the bounding box is outside the cylinder
        assertEquals(air, target.getBlock(min));
        // Centre is filled to the top of the region and no higher
        assertEquals(stone, target.getBlock(BlockVector3.at(0, 4, 0)));
        assertEquals(5, Heightmap.fromSurface(target, cylinder).getHeight(-min.x(), -min.z()));

        assertThrows(IllegalArgumentException.class,
            () -> new Heightmap(1, 1, new int[] { 1 }).apply(target, cylinder, stone, stone, 0, stone, false));
    }

    @Test
    @DisplayName("top view renders palette colours, north up, with empty columns transparent")
    void topView() throws WorldEditException {
        BlockState wool = block("minecraft:white_wool");
        CuboidRegion region = new CuboidRegion(BlockVector3.at(0, 0, 0), BlockVector3.at(2, 3, 1));
        BlockArrayClipboard extent = emptyExtent(region);
        extent.setBlock(BlockVector3.at(0, 0, 0), stone);
        extent.setBlock(BlockVector3.at(0, 2, 0), wool);
        extent.setBlock(BlockVector3.at(1, 0, 1), grass);

        BlockPalette palette = BlockPalette.getDefault();
        BufferedImage image = TopViewRenderer.render(extent, region, palette, false);
        assertEquals(3, image.getWidth());
        assertEquals(2, image.getHeight());
        assertEquals(0xFF000000 | palette.getRenderColor("minecraft:white_wool").orElseThrow(), image.getRGB(0, 0));
        assertEquals(0xFF000000 | palette.getRenderColor("minecraft:grass_block").orElseThrow(), image.getRGB(1, 1));
        assertEquals(0, image.getRGB(2, 0) >>> 24);
    }
}
