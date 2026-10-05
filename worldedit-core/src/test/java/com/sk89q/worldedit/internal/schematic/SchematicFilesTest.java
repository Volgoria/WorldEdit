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

package com.sk89q.worldedit.internal.schematic;

import com.sk89q.worldedit.BaseWorldEditTest;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardIoTestSupport;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.io.file.FilenameException;
import com.sk89q.worldedit.world.entity.EntityType;
import org.enginehub.linbus.tree.LinCompoundTag;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.sk89q.worldedit.extent.clipboard.io.ClipboardIoTestSupport.CHEST;
import static com.sk89q.worldedit.extent.clipboard.io.ClipboardIoTestSupport.STONE;
import static com.sk89q.worldedit.extent.clipboard.io.ClipboardIoTestSupport.state;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Schematic file helpers")
class SchematicFilesTest extends BaseWorldEditTest {

    @TempDir
    Path root;

    @BeforeAll
    static void setUpRegistries() {
        ClipboardIoTestSupport.install(MOCKED_PLATFORM);
    }

    @AfterAll
    static void tearDownRegistries() {
        ClipboardIoTestSupport.uninstall();
    }

    private File resolve(String source, String target) throws FilenameException {
        return SchematicFiles.resolveDestination(WorldEdit.getInstance(), null, root.toFile(),
            root.resolve(source).toFile(), target);
    }

    @Test
    @DisplayName("withExtension replaces the last extension and keeps the folder")
    void withExtension() {
        File folder = root.resolve("sub").toFile();
        assertEquals(new File(folder, "house.mtl"), SchematicFiles.withExtension(new File(folder, "house.obj"), "mtl"));
        assertEquals(new File(folder, "a.b.mtl"), SchematicFiles.withExtension(new File(folder, "a.b.obj"), "mtl"));
        assertEquals(new File(folder, "house.mtl"), SchematicFiles.withExtension(new File(folder, "house"), "mtl"));
        assertEquals(new File(folder, ".hidden.mtl"), SchematicFiles.withExtension(new File(folder, ".hidden"), "mtl"));
    }

    @Test
    @DisplayName("only the OBJ format writes a material library, which saving must check for overwrites")
    void materialLibrary() {
        assertTrue(SchematicFiles.writesMaterialLibrary(BuiltInClipboardFormat.WAVEFRONT_OBJ));
        assertFalse(SchematicFiles.writesMaterialLibrary(BuiltInClipboardFormat.JSON));
        assertFalse(SchematicFiles.writesMaterialLibrary(BuiltInClipboardFormat.SPONGE_V3_SCHEMATIC));
        assertFalse(SchematicFiles.writesMaterialLibrary(BuiltInClipboardFormat.MINECRAFT_STRUCTURE));
    }

    private Path create(String name, String content) throws IOException {
        Path path = root.resolve(name);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
        return path;
    }

    @Test
    @DisplayName("extracts lower case extensions")
    void extensions() {
        assertEquals("schem", SchematicFiles.getExtension(Path.of("a/b/house.SCHEM")));
        assertEquals("nbt", SchematicFiles.getExtension(Path.of("tower.v2.nbt")));
        assertEquals("", SchematicFiles.getExtension(Path.of("noext")));
        assertEquals("", SchematicFiles.getExtension(Path.of(".hidden")));
        assertEquals("", SchematicFiles.getExtension(Path.of("trailing.")));
    }

    @Test
    @DisplayName("keeps the source extension for destinations")
    void keepsExtension() throws Exception {
        assertEquals(root.resolve("castle.schem").toFile(), resolve("house.schem", "castle"));
        assertEquals(root.resolve("castle.schem").toFile(), resolve("house.schem", "castle.schem"));
        assertEquals(root.resolve("castle.txt.schem").toFile(), resolve("house.schem", "castle.txt"));
        assertEquals(root.resolve("sub/dir/castle.nbt").toFile(), resolve("house.nbt", "sub/dir/castle"));
    }

    @Test
    @DisplayName("rejects destinations outside of the schematics folder")
    void rejectsTraversal() {
        assertThrows(FilenameException.class, () -> resolve("house.schem", "../escape"));
        assertThrows(FilenameException.class, () -> resolve("house.schem", "sub/../../escape"));
        assertThrows(FilenameException.class, () -> resolve("house.schem", "sub/../../../etc/passwd"));
    }

    @Test
    @DisplayName("rejects invalid destination names")
    void rejectsInvalidNames() {
        assertThrows(FilenameException.class, () -> resolve("house.schem", "#"));
        assertThrows(FilenameException.class, () -> resolve("house.schem", "bad<name>"));
        assertThrows(FilenameException.class, () -> resolve("house.schem", "bad|name"));
        assertThrows(FilenameException.class, () -> resolve("house.schem", ""));
    }

    @Test
    @DisplayName("moves files and creates parent folders")
    void move() throws Exception {
        Path source = create("house.schem", "data");
        Path target = root.resolve("archive/2024/house.schem");
        SchematicFiles.transfer(source, target, true, false);
        assertFalse(Files.exists(source));
        assertEquals("data", Files.readString(target, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("copies files")
    void copy() throws Exception {
        Path source = create("house.schem", "data");
        Path target = root.resolve("house-copy.schem");
        SchematicFiles.transfer(source, target, false, false);
        assertEquals("data", Files.readString(source));
        assertEquals("data", Files.readString(target));
    }

    @Test
    @DisplayName("only overwrites when allowed")
    void overwrite() throws Exception {
        Path source = create("a.schem", "new");
        Path target = create("b.schem", "old");
        assertThrows(FileAlreadyExistsException.class, () -> SchematicFiles.transfer(source, target, true, false));
        assertEquals("old", Files.readString(target));
        assertTrue(Files.exists(source));

        SchematicFiles.transfer(source, target, true, true);
        assertEquals("new", Files.readString(target));
        assertFalse(Files.exists(source));
    }

    @Test
    @DisplayName("refuses to transfer a file onto itself or a missing file")
    void invalidTransfers() throws Exception {
        Path source = create("a.schem", "data");
        assertThrows(IOException.class, () -> SchematicFiles.transfer(source, root.resolve("./a.schem"), true, true));
        assertEquals("data", Files.readString(source));
        assertThrows(IOException.class, () -> SchematicFiles.transfer(root.resolve("missing.schem"), root.resolve("b.schem"), false, false));
        Files.createDirectories(root.resolve("folder"));
        assertThrows(IOException.class, () -> SchematicFiles.transfer(root.resolve("folder"), root.resolve("b.schem"), false, false));
    }

    private static final String OBJ = "# Exported by WorldEdit\nmtllib house.mtl\nvn 0 1 0\nusemtl stone\n";

    @Test
    @DisplayName("finds the material library of OBJ exports only")
    void materialLibraryOf() throws Exception {
        Path obj = create("house.obj", OBJ);
        assertNull(SchematicFiles.materialLibraryOf(obj));
        Path mtl = create("house.mtl", "newmtl stone\n");
        assertEquals(mtl, SchematicFiles.materialLibraryOf(obj));
        create("other.schem", "data");
        create("other.mtl", "newmtl stone\n");
        assertNull(SchematicFiles.materialLibraryOf(root.resolve("other.schem")));
    }

    @Test
    @DisplayName("renames an OBJ export with its material library and relinks it")
    void moveObjWithMaterialLibrary() throws Exception {
        Path obj = create("house.obj", OBJ);
        Path mtl = create("house.mtl", "newmtl stone\n");
        Path target = root.resolve("archive/villa.obj");
        SchematicFiles.transferWithMaterialLibrary(obj, target, true, false, false);

        assertFalse(Files.exists(obj));
        assertFalse(Files.exists(mtl));
        assertEquals("newmtl stone\n", Files.readString(root.resolve("archive/villa.mtl")));
        assertEquals(OBJ.replace("mtllib house.mtl", "mtllib villa.mtl"), Files.readString(target));
    }

    @Test
    @DisplayName("copies an OBJ export with its material library")
    void copyObjWithMaterialLibrary() throws Exception {
        Path obj = create("house.obj", OBJ);
        Path mtl = create("house.mtl", "newmtl stone\n");
        Path target = root.resolve("house2.obj");
        SchematicFiles.transferWithMaterialLibrary(obj, target, false, false, false);

        assertEquals(OBJ, Files.readString(obj));
        assertEquals("newmtl stone\n", Files.readString(mtl));
        assertEquals("newmtl stone\n", Files.readString(root.resolve("house2.mtl")));
        assertEquals(OBJ.replace("mtllib house.mtl", "mtllib house2.mtl"), Files.readString(target));
    }

    @Test
    @DisplayName("keeps the reference when the OBJ export only changes folder")
    void moveObjToFolderKeepsName() throws Exception {
        Path obj = create("house.obj", OBJ);
        create("house.mtl", "newmtl stone\n");
        Path target = root.resolve("old/house.obj");
        SchematicFiles.transferWithMaterialLibrary(obj, target, true, false, false);
        assertEquals(OBJ, Files.readString(target));
        assertTrue(Files.exists(root.resolve("old/house.mtl")));
    }

    @Test
    @DisplayName("touches nothing if the destination material library may not be replaced")
    void existingMaterialLibraryIsNotReplaced() throws Exception {
        Path obj = create("house.obj", OBJ);
        Path mtl = create("house.mtl", "newmtl stone\n");
        Path otherMtl = create("villa.mtl", "old\n");
        Path target = root.resolve("villa.obj");
        assertThrows(FileAlreadyExistsException.class,
            () -> SchematicFiles.transferWithMaterialLibrary(obj, target, true, false, false));
        assertTrue(Files.exists(obj));
        assertTrue(Files.exists(mtl));
        assertFalse(Files.exists(target));
        assertEquals("old\n", Files.readString(otherMtl));

        SchematicFiles.transferWithMaterialLibrary(obj, target, true, false, true);
        assertEquals("newmtl stone\n", Files.readString(otherMtl));
        assertFalse(Files.exists(mtl));
    }

    @Test
    @DisplayName("transfers other files as usual")
    void transferWithoutMaterialLibrary() throws Exception {
        Path source = create("house.schem", "data");
        create("house.mtl", "unrelated");
        SchematicFiles.transferWithMaterialLibrary(source, root.resolve("villa.schem"), true, false, false);
        assertEquals("data", Files.readString(root.resolve("villa.schem")));
        assertTrue(Files.exists(root.resolve("house.mtl")));
        assertFalse(Files.exists(root.resolve("villa.mtl")));
    }

    @Test
    @DisplayName("filters schematic listings by name and format")
    void filters() {
        Path house = root.resolve("builds/House.schem");
        Path tower = root.resolve("tower.nbt");
        assertTrue(SchematicFiles.matchesFilter(root, house, null, null));
        assertTrue(SchematicFiles.matchesFilter(root, house, "", null));
        assertTrue(SchematicFiles.matchesFilter(root, house, "house", null));
        assertTrue(SchematicFiles.matchesFilter(root, house, "builds", null));
        assertFalse(SchematicFiles.matchesFilter(root, tower, "house", null));

        assertTrue(SchematicFiles.matchesFilter(root, tower, null, BuiltInClipboardFormat.MINECRAFT_STRUCTURE));
        assertFalse(SchematicFiles.matchesFilter(root, house, null, BuiltInClipboardFormat.MINECRAFT_STRUCTURE));
        assertTrue(SchematicFiles.matchesFilter(root, house, "HOUSE", BuiltInClipboardFormat.SPONGE_V3_SCHEMATIC));
        assertFalse(SchematicFiles.matchesFilter(root, tower, "tower", BuiltInClipboardFormat.SPONGE_V3_SCHEMATIC));
    }

    @Test
    @DisplayName("summarizes clipboards")
    void summary() throws Exception {
        BlockArrayClipboard clipboard = new BlockArrayClipboard(
            new CuboidRegion(BlockVector3.ZERO, BlockVector3.at(3, 1, 2))
        );
        clipboard.setBlock(BlockVector3.at(0, 0, 0), state(STONE));
        clipboard.setBlock(BlockVector3.at(1, 0, 0), state(STONE));
        clipboard.setBlock(BlockVector3.at(2, 1, 2), state(CHEST).toBaseBlock(
            LinCompoundTag.builder().putString("id", CHEST).build()
        ));
        EntityType pig = EntityType.REGISTRY.get(ClipboardIoTestSupport.PIG);
        clipboard.createEntity(new Location(clipboard, 1, 1, 1), new BaseEntity(pig));

        SchematicFiles.Summary summary = SchematicFiles.summarize(clipboard);
        assertEquals(BlockVector3.at(4, 2, 3), summary.size());
        assertEquals(24, summary.volume());
        assertEquals(3, summary.nonAirBlocks());
        assertEquals(1, summary.blockEntities());
        assertEquals(1, summary.entities());
    }
}
