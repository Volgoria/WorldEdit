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

package com.sk89q.worldedit.extent.clipboard.io.export;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sk89q.worldedit.BaseWorldEditTest;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardIoTestSupport;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.block.BlockState;
import org.enginehub.linbus.tree.LinCompoundTag;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import static com.sk89q.worldedit.extent.clipboard.io.ClipboardIoTestSupport.CHEST;
import static com.sk89q.worldedit.extent.clipboard.io.ClipboardIoTestSupport.GLASS;
import static com.sk89q.worldedit.extent.clipboard.io.ClipboardIoTestSupport.STAIRS;
import static com.sk89q.worldedit.extent.clipboard.io.ClipboardIoTestSupport.STONE;
import static com.sk89q.worldedit.extent.clipboard.io.ClipboardIoTestSupport.state;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Export-only clipboard formats")
class ExportFormatsTest extends BaseWorldEditTest {

    @BeforeAll
    static void setUpRegistries() {
        ClipboardIoTestSupport.install(MOCKED_PLATFORM);
    }

    @AfterAll
    static void tearDownRegistries() {
        ClipboardIoTestSupport.uninstall();
    }

    private static BlockArrayClipboard clipboard(int width, int height, int length) {
        BlockVector3 min = BlockVector3.at(10, 20, 30);
        return new BlockArrayClipboard(new CuboidRegion(min, min.add(width - 1, height - 1, length - 1)));
    }

    private static void set(BlockArrayClipboard clipboard, int x, int y, int z, BlockState state) throws WorldEditException {
        clipboard.setBlock(clipboard.getMinimumPoint().add(x, y, z), state);
    }

    @Nested
    @DisplayName("Wavefront OBJ")
    class Obj {

        private record Mesh(List<String> obj, String mtl) {
            List<String> lines(String prefix) {
                return obj.stream().filter(line -> line.startsWith(prefix)).toList();
            }

            int faces() {
                return lines("f ").size();
            }

            int vertices() {
                return lines("v ").size();
            }
        }

        private Mesh export(BlockArrayClipboard clipboard, boolean greedy) throws IOException {
            return export(clipboard, greedy, state -> !state.getBlockType().id().equals(GLASS));
        }

        private Mesh export(BlockArrayClipboard clipboard, boolean greedy, Predicate<BlockState> occluding) throws IOException {
            ByteArrayOutputStream obj = new ByteArrayOutputStream();
            ByteArrayOutputStream mtl = new ByteArrayOutputStream();
            try (WavefrontObjWriter writer = new WavefrontObjWriter(obj, occluding)) {
                writer.setGreedyMeshing(greedy);
                writer.setMaterialLibrary("test.mtl", mtl);
                writer.write(clipboard);
            }
            return new Mesh(
                Arrays.asList(obj.toString(StandardCharsets.UTF_8).split("\n", -1)),
                mtl.toString(StandardCharsets.UTF_8)
            );
        }

        @Test
        @DisplayName("a single block has six faces and eight vertices")
        void singleBlock() throws Exception {
            BlockArrayClipboard clipboard = clipboard(1, 1, 1);
            set(clipboard, 0, 0, 0, state(STONE));
            Mesh mesh = export(clipboard, true);
            assertEquals(6, mesh.faces());
            assertEquals(8, mesh.vertices());
            assertEquals(List.of("usemtl minecraft_stone"), mesh.lines("usemtl"));
            assertTrue(mesh.obj().contains("mtllib test.mtl"));
        }

        @Test
        @DisplayName("an empty clipboard has no geometry")
        void empty() throws Exception {
            Mesh mesh = export(clipboard(2, 2, 2), true);
            assertEquals(0, mesh.faces());
            assertEquals(0, mesh.vertices());
        }

        @Test
        @DisplayName("hides faces between blocks and merges coplanar faces")
        void twoBlocks() throws Exception {
            BlockArrayClipboard clipboard = clipboard(2, 1, 1);
            set(clipboard, 0, 0, 0, state(STONE));
            set(clipboard, 1, 0, 0, state(STONE));
            // 2 hidden faces of 12; the 4 long sides are merged
            assertEquals(10, export(clipboard, false).faces());
            assertEquals(6, export(clipboard, true).faces());
            assertEquals(8, export(clipboard, true).vertices());
        }

        @Test
        @DisplayName("merges a whole cube face into one quad")
        void cube() throws Exception {
            BlockArrayClipboard clipboard = clipboard(3, 3, 3);
            for (BlockVector3 point : clipboard.getRegion()) {
                clipboard.setBlock(point, state(STONE));
            }
            assertEquals(6 * 9, export(clipboard, false).faces());
            assertEquals(6, export(clipboard, true).faces());

            // Hollow it out: the inner cavity adds 6 faces facing inwards
            set(clipboard, 1, 1, 1, state(ClipboardIoTestSupport.AIR));
            assertEquals(12, export(clipboard, true).faces());
        }

        @Test
        @DisplayName("groups faces per block type, without merging different types")
        void differentTypes() throws Exception {
            BlockArrayClipboard clipboard = clipboard(2, 1, 1);
            set(clipboard, 0, 0, 0, state(STONE));
            set(clipboard, 1, 0, 0, state(CHEST));
            Mesh mesh = export(clipboard, true);
            assertEquals(10, mesh.faces());
            assertEquals(Set.of("usemtl minecraft_stone", "usemtl minecraft_chest"), new HashSet<>(mesh.lines("usemtl")));
            assertEquals(2, mesh.lines("g ").size());
            assertTrue(mesh.mtl().contains("newmtl minecraft_stone"));
            assertTrue(mesh.mtl().contains("newmtl minecraft_chest"));
            assertEquals(2, mesh.mtl().lines().filter(line -> line.startsWith("Kd ")).count());
        }

        @Test
        @DisplayName("keeps faces next to transparent blocks, but culls between equal transparent blocks")
        void transparency() throws Exception {
            BlockArrayClipboard clipboard = clipboard(3, 1, 1);
            set(clipboard, 0, 0, 0, state(STONE));
            set(clipboard, 1, 0, 0, state(GLASS));
            set(clipboard, 2, 0, 0, state(GLASS));
            Mesh mesh = export(clipboard, false);
            // Stone: 6 faces (its +X face is visible through glass)
            // Glass: 2 blocks * 6 faces - 1 face hidden by stone - 2 faces between the glass blocks
            assertEquals(6 + 9, mesh.faces());
            assertTrue(mesh.mtl().contains("d 0.60"), "glass is translucent");
        }

        @Test
        @DisplayName("uses block materials to decide occlusion by default")
        void defaultOcclusion() throws Exception {
            BlockArrayClipboard clipboard = clipboard(2, 1, 1);
            set(clipboard, 0, 0, 0, state(STONE));
            set(clipboard, 1, 0, 0, state(STAIRS));
            ByteArrayOutputStream obj = new ByteArrayOutputStream();
            try (ClipboardWriter writer = BuiltInClipboardFormat.WAVEFRONT_OBJ.getWriter(obj)) {
                writer.write(clipboard);
            }
            List<String> lines = Arrays.asList(obj.toString(StandardCharsets.UTF_8).split("\n", -1));
            // Stairs are not full cubes, so the stone face behind them stays visible
            assertEquals(11, lines.stream().filter(line -> line.startsWith("f ")).count());
            assertFalse(lines.stream().anyMatch(line -> line.startsWith("mtllib")), "no material library configured");
        }

        @Test
        @DisplayName("writes outward facing, counter-clockwise faces")
        void winding() throws Exception {
            BlockArrayClipboard clipboard = clipboard(1, 1, 1);
            set(clipboard, 0, 0, 0, state(STONE));
            Mesh mesh = export(clipboard, true);
            List<double[]> vertices = new ArrayList<>();
            for (String line : mesh.lines("v ")) {
                String[] parts = line.split(" ", -1);
                vertices.add(new double[] {
                    Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]),
                });
            }
            List<double[]> normals = new ArrayList<>();
            for (String line : mesh.lines("vn ")) {
                String[] parts = line.split(" ", -1);
                normals.add(new double[] {
                    Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]),
                });
            }
            for (String face : mesh.lines("f ")) {
                String[] parts = face.split(" ", -1);
                double[] a = vertices.get(Integer.parseInt(parts[1].split("//", -1)[0]) - 1);
                double[] b = vertices.get(Integer.parseInt(parts[2].split("//", -1)[0]) - 1);
                double[] c = vertices.get(Integer.parseInt(parts[3].split("//", -1)[0]) - 1);
                double[] normal = normals.get(Integer.parseInt(parts[1].split("//", -1)[1]) - 1);
                double[] ab = { b[0] - a[0], b[1] - a[1], b[2] - a[2] };
                double[] ac = { c[0] - a[0], c[1] - a[1], c[2] - a[2] };
                double[] cross = {
                    ab[1] * ac[2] - ab[2] * ac[1],
                    ab[2] * ac[0] - ab[0] * ac[2],
                    ab[0] * ac[1] - ab[1] * ac[0],
                };
                double dot = cross[0] * normal[0] + cross[1] * normal[1] + cross[2] * normal[2];
                assertTrue(dot > 0, "face " + face + " winds against its normal");
            }
        }
    }

    @Nested
    @DisplayName("JSON")
    class Json {

        @Test
        @DisplayName("writes size, palette, blocks and block entities")
        void layout() throws Exception {
            BlockArrayClipboard clipboard = clipboard(2, 1, 2);
            clipboard.setOrigin(BlockVector3.at(10, 20, 29));
            set(clipboard, 1, 0, 0, state(STONE));
            BlockVector3 chestPos = clipboard.getMinimumPoint().add(0, 0, 1);
            clipboard.setBlock(chestPos, state(CHEST).toBaseBlock(LinCompoundTag.builder()
                .putString("id", CHEST)
                .putString("Lock", "secret")
                .putInt("x", chestPos.x())
                .putInt("y", chestPos.y())
                .putInt("z", chestPos.z())
                .build()));

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (ClipboardWriter writer = BuiltInClipboardFormat.JSON.getWriter(out)) {
                writer.write(clipboard);
            }
            JsonObject json = JsonParser.parseString(out.toString(StandardCharsets.UTF_8)).getAsJsonObject();

            assertEquals("worldedit:json", json.get("format").getAsString());
            assertEquals(JsonClipboardWriter.VERSION, json.get("version").getAsInt());
            assertEquals("[2,1,2]", json.get("size").toString());
            assertEquals("[10,20,29]", json.get("origin").toString());
            assertEquals("[0,0,1]", json.get("offset").toString());

            List<String> palette = new ArrayList<>();
            json.getAsJsonArray("palette").forEach(element -> palette.add(element.getAsString()));
            JsonArray blocks = json.getAsJsonArray("blocks");
            assertEquals(4, blocks.size());
            // Index order is Y, then Z, then X
            assertTrue(palette.get(blocks.get(0).getAsInt()).endsWith(":air"));
            assertEquals(STONE, palette.get(blocks.get(1).getAsInt()));
            assertTrue(palette.get(blocks.get(2).getAsInt()).startsWith(CHEST));
            assertTrue(palette.get(blocks.get(3).getAsInt()).endsWith(":air"));
            assertEquals(3, palette.size());

            JsonArray blockEntities = json.getAsJsonArray("blockEntities");
            assertEquals(1, blockEntities.size());
            JsonObject chest = blockEntities.get(0).getAsJsonObject();
            assertEquals("[0,0,1]", chest.get("pos").toString());
            assertEquals(CHEST, chest.get("id").getAsString());
            String snbt = chest.get("snbt").getAsString();
            assertTrue(snbt.contains("Lock"), snbt);
            assertFalse(snbt.contains("x:"), "absolute coordinates are stripped: " + snbt);
            assertEquals(0, json.getAsJsonArray("entities").size());
        }
    }

    @Test
    @DisplayName("block colours are stable and recognise common blocks")
    void colors() {
        assertEquals(BlockColors.getColor("minecraft:red_wool"), BlockColors.getColor("minecraft:red_concrete"));
        assertEquals(0x3AAFD9, BlockColors.getColor("minecraft:light_blue_wool"));
        assertEquals(BlockColors.getColor("minecraft:stone"), BlockColors.getColor("stone"));
        assertEquals(BlockColors.getColor("mod:unknown_thing"), BlockColors.getColor("mod:unknown_thing"));
        int fallback = BlockColors.getColor("mod:unknown_thing");
        assertTrue(fallback >= 0 && fallback <= 0xFFFFFF);
    }
}
