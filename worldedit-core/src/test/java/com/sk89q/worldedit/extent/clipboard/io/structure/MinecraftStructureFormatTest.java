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

package com.sk89q.worldedit.extent.clipboard.io.structure;

import com.sk89q.worldedit.BaseWorldEditTest;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardIoTestSupport;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.concurrency.LazyReference;
import com.sk89q.worldedit.world.DataFixer;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.entity.EntityType;
import org.enginehub.linbus.stream.LinBinaryIO;
import org.enginehub.linbus.tree.LinCompoundTag;
import org.enginehub.linbus.tree.LinDoubleTag;
import org.enginehub.linbus.tree.LinIntTag;
import org.enginehub.linbus.tree.LinListTag;
import org.enginehub.linbus.tree.LinRootEntry;
import org.enginehub.linbus.tree.LinTagType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static com.sk89q.worldedit.extent.clipboard.io.ClipboardIoTestSupport.CHEST;
import static com.sk89q.worldedit.extent.clipboard.io.ClipboardIoTestSupport.GLASS;
import static com.sk89q.worldedit.extent.clipboard.io.ClipboardIoTestSupport.PIG;
import static com.sk89q.worldedit.extent.clipboard.io.ClipboardIoTestSupport.STAIRS;
import static com.sk89q.worldedit.extent.clipboard.io.ClipboardIoTestSupport.STONE;
import static com.sk89q.worldedit.extent.clipboard.io.ClipboardIoTestSupport.STRUCTURE_VOID;
import static com.sk89q.worldedit.extent.clipboard.io.ClipboardIoTestSupport.state;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@DisplayName("Minecraft structure format")
class MinecraftStructureFormatTest extends BaseWorldEditTest {

    private static final ClipboardFormat FORMAT = BuiltInClipboardFormat.MINECRAFT_STRUCTURE;

    @BeforeAll
    static void setUpRegistries() {
        ClipboardIoTestSupport.install(MOCKED_PLATFORM);
    }

    @AfterAll
    static void tearDownRegistries() {
        ClipboardIoTestSupport.uninstall();
    }

    @AfterEach
    void resetDataVersion() {
        when(MOCKED_PLATFORM.getDataVersion()).thenReturn(0);
        when(MOCKED_PLATFORM.getDataFixer()).thenReturn(null);
    }

    private static BlockState stairs(Direction facing, String half) {
        BlockType type = state(STAIRS).getBlockType();
        return state(STAIRS)
            .with(type.<Direction>getProperty("facing"), facing)
            .with(type.<String>getProperty("half"), half)
            .with(type.<Boolean>getProperty("waterlogged"), true);
    }

    private static LinCompoundTag chestNbt(BlockVector3 position) {
        return LinCompoundTag.builder()
            .putString("id", CHEST)
            .putString("CustomName", "{\"text\":\"Loot\"}")
            .put("Items", LinListTag.builder(LinTagType.compoundTag())
                .add(LinCompoundTag.builder()
                    .putByte("Slot", (byte) 3)
                    .putString("id", "minecraft:diamond")
                    .putInt("count", 5)
                    .build())
                .build())
            .putInt("x", position.x())
            .putInt("y", position.y())
            .putInt("z", position.z())
            .build();
    }

    /**
     * A 3x2x2 clipboard copied at (100, 64, -20) with its origin at (101, 64, -19).
     */
    private static BlockArrayClipboard createClipboard() throws WorldEditException {
        BlockVector3 min = BlockVector3.at(100, 64, -20);
        BlockArrayClipboard clipboard = new BlockArrayClipboard(new CuboidRegion(min, min.add(2, 1, 1)));
        clipboard.setOrigin(BlockVector3.at(101, 64, -19));
        clipboard.setBlock(min, state(STONE));
        clipboard.setBlock(min.add(1, 0, 0), stairs(Direction.EAST, "top"));
        BlockVector3 chestPos = min.add(2, 1, 1);
        BlockType chestType = state(CHEST).getBlockType();
        clipboard.setBlock(chestPos, state(CHEST)
            .with(chestType.<Direction>getProperty("facing"), Direction.WEST)
            .toBaseBlock(chestNbt(chestPos)));
        clipboard.setBlock(min.add(0, 1, 0), state(GLASS));
        return clipboard;
    }

    private static byte[] write(Clipboard clipboard) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ClipboardWriter writer = FORMAT.getWriter(out)) {
            writer.write(clipboard);
        }
        return out.toByteArray();
    }

    private static Clipboard read(byte[] data) throws IOException {
        try (ClipboardReader reader = FORMAT.getReader(new ByteArrayInputStream(data))) {
            return reader.read();
        }
    }

    private static LinCompoundTag readRawNbt(byte[] data) throws IOException {
        return LinBinaryIO.readUsing(
            new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(data))),
            LinRootEntry::readFrom
        ).value();
    }

    private static byte[] gzipNbt(LinCompoundTag root) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (DataOutputStream stream = new DataOutputStream(new GZIPOutputStream(out))) {
            LinBinaryIO.write(stream, new LinRootEntry("", root));
        }
        return out.toByteArray();
    }

    private static LinListTag<LinIntTag> ints(int... values) {
        LinListTag.Builder<LinIntTag> builder = LinListTag.builder(LinTagType.intTag());
        for (int value : values) {
            builder.add(LinIntTag.of(value));
        }
        return builder.build();
    }

    @Test
    @DisplayName("round-trips blocks, properties, block entities and the origin")
    void roundTrip() throws Exception {
        BlockArrayClipboard original = createClipboard();
        Clipboard loaded = read(write(original));

        assertEquals(original.getRegion().getMinimumPoint(), loaded.getRegion().getMinimumPoint());
        assertEquals(original.getRegion().getMaximumPoint(), loaded.getRegion().getMaximumPoint());
        assertEquals(original.getOrigin(), loaded.getOrigin());

        for (BlockVector3 point : original.getRegion()) {
            assertEquals(original.getBlock(point), loaded.getBlock(point), "block at " + point);
        }

        BlockVector3 min = original.getMinimumPoint();
        BlockState loadedStairs = loaded.getBlock(min.add(1, 0, 0));
        BlockType stairsType = loadedStairs.getBlockType();
        assertEquals(Direction.EAST, loadedStairs.getState(stairsType.<Direction>getProperty("facing")));
        assertEquals("top", loadedStairs.getState(stairsType.<String>getProperty("half")));
        assertEquals(true, loadedStairs.getState(stairsType.<Boolean>getProperty("waterlogged")));

        BlockVector3 chestPos = min.add(2, 1, 1);
        BaseBlock chest = loaded.getFullBlock(chestPos);
        LinCompoundTag nbt = chest.getNbt();
        assertNotNull(nbt, "chest NBT");
        assertEquals(CHEST, chest.getNbtId());
        assertEquals("{\"text\":\"Loot\"}", nbt.getTag("CustomName", LinTagType.stringTag()).value());
        LinCompoundTag item = nbt.getListTag("Items", LinTagType.compoundTag()).get(0);
        assertEquals("minecraft:diamond", item.getTag("id", LinTagType.stringTag()).value());
        assertEquals(5, item.getTag("count", LinTagType.intTag()).valueAsInt());
        // Block entity coordinates are absolute again after loading
        assertEquals(chestPos.x(), nbt.getTag("x", LinTagType.intTag()).valueAsInt());
        assertEquals(chestPos.y(), nbt.getTag("y", LinTagType.intTag()).valueAsInt());
        assertEquals(chestPos.z(), nbt.getTag("z", LinTagType.intTag()).valueAsInt());
    }

    @Test
    @DisplayName("writes the vanilla layout")
    void writesVanillaLayout() throws Exception {
        when(MOCKED_PLATFORM.getDataVersion()).thenReturn(3953);
        BlockArrayClipboard clipboard = createClipboard();
        clipboard.setBlock(clipboard.getMinimumPoint().add(1, 1, 1), state(STRUCTURE_VOID));
        LinCompoundTag root = readRawNbt(write(clipboard));

        assertEquals(3953, root.getTag("DataVersion", LinTagType.intTag()).valueAsInt());
        LinListTag<LinIntTag> size = root.getListTag("size", LinTagType.intTag());
        assertEquals(List.of(3, 2, 2), size.value().stream().map(LinIntTag::valueAsInt).toList());

        List<LinCompoundTag> palette = root.getListTag("palette", LinTagType.compoundTag()).value();
        Map<String, LinCompoundTag> byName = new HashMap<>();
        for (LinCompoundTag entry : palette) {
            byName.put(entry.getTag("Name", LinTagType.stringTag()).value(), entry);
        }
        assertFalse(byName.containsKey(STRUCTURE_VOID), "structure voids are not saved");
        assertNull(byName.get(STONE).findTag("Properties", LinTagType.compoundTag()));
        LinCompoundTag stairsProperties = byName.get(STAIRS).getTag("Properties", LinTagType.compoundTag());
        assertEquals("east", stairsProperties.getTag("facing", LinTagType.stringTag()).value());
        assertEquals("top", stairsProperties.getTag("half", LinTagType.stringTag()).value());
        assertEquals("true", stairsProperties.getTag("waterlogged", LinTagType.stringTag()).value());

        List<LinCompoundTag> blocks = root.getListTag("blocks", LinTagType.compoundTag()).value();
        // 12 positions, one of them a structure void
        assertEquals(11, blocks.size());
        LinCompoundTag chestEntry = null;
        for (LinCompoundTag block : blocks) {
            int stateId = block.getTag("state", LinTagType.intTag()).valueAsInt();
            assertTrue(stateId >= 0 && stateId < palette.size());
            List<Integer> pos = block.getListTag("pos", LinTagType.intTag()).value().stream()
                .map(LinIntTag::valueAsInt).toList();
            if (pos.equals(List.of(2, 1, 1))) {
                chestEntry = block;
            }
        }
        assertNotNull(chestEntry, "chest is saved at its relative position");
        LinCompoundTag chestNbt = chestEntry.getTag("nbt", LinTagType.compoundTag());
        assertEquals(CHEST, chestNbt.getTag("id", LinTagType.stringTag()).value());
        assertNull(chestNbt.findTag("x", LinTagType.intTag()), "absolute coordinates are stripped");
        assertNotNull(chestNbt.findListTag("Items", LinTagType.compoundTag()));

        assertTrue(root.getListTag("entities", LinTagType.compoundTag()).value().isEmpty());
    }

    @Test
    @DisplayName("round-trips entities with relative positions")
    void roundTripEntities() throws Exception {
        BlockArrayClipboard clipboard = createClipboard();
        EntityType pig = EntityType.REGISTRY.get(PIG);
        LinCompoundTag pigNbt = LinCompoundTag.builder()
            .putString("id", PIG)
            .putIntArray("UUID", new int[] { 1, 2, 3, 4 })
            .putByte("Saddle", (byte) 1)
            .build();
        Location location = new Location(clipboard, Vector3.at(101.5, 65, -18.5), 90f, 10f);
        clipboard.createEntity(location, new BaseEntity(pig, LazyReference.computed(pigNbt)));

        byte[] data = write(clipboard);
        LinCompoundTag raw = readRawNbt(data).getListTag("entities", LinTagType.compoundTag()).get(0);
        List<Double> relative = raw.getListTag("pos", LinTagType.doubleTag()).value().stream()
            .map(LinDoubleTag::valueAsDouble).toList();
        assertEquals(List.of(1.5, 1.0, 1.5), relative);
        assertEquals(List.of(1, 1, 1), raw.getListTag("blockPos", LinTagType.intTag()).value().stream()
            .map(LinIntTag::valueAsInt).toList());
        LinCompoundTag rawNbt = raw.getTag("nbt", LinTagType.compoundTag());
        assertEquals(PIG, rawNbt.getTag("id", LinTagType.stringTag()).value());
        assertNull(rawNbt.findTag("UUID", LinTagType.intArrayTag()), "UUIDs are not saved");

        Clipboard loaded = read(data);
        List<? extends Entity> entities = loaded.getEntities();
        assertEquals(1, entities.size());
        Entity entity = entities.getFirst();
        assertEquals(pig, entity.getState().getType());
        assertEquals(location.toVector(), entity.getLocation().toVector());
        assertEquals(90f, entity.getLocation().getYaw());
        assertEquals(10f, entity.getLocation().getPitch());
        assertEquals(1, entity.getState().getNbt().getTag("Saddle", LinTagType.byteTag()).valueAsByte());
    }

    @Test
    @DisplayName("reads vanilla files with multiple palettes, voids and unknown blocks")
    void readsVanillaFile() throws Exception {
        LinCompoundTag stairsEntry = LinCompoundTag.builder()
            .putString("Name", STAIRS)
            .put("Properties", LinCompoundTag.builder()
                .putString("facing", "south")
                .putString("half", "bottom")
                .putString("shape", "straight")
                .putString("waterlogged", "nonsense")
                .build())
            .build();
        LinListTag<LinCompoundTag> palette = LinListTag.builder(LinTagType.compoundTag())
            .add(LinCompoundTag.builder().putString("Name", STONE).build())
            .add(stairsEntry)
            .add(LinCompoundTag.builder().putString("Name", "minecraft:not_a_block").build())
            .build();
        LinListTag<LinCompoundTag> secondPalette = LinListTag.builder(LinTagType.compoundTag())
            .add(LinCompoundTag.builder().putString("Name", GLASS).build())
            .add(LinCompoundTag.builder().putString("Name", GLASS).build())
            .add(LinCompoundTag.builder().putString("Name", GLASS).build())
            .build();
        LinCompoundTag root = LinCompoundTag.builder()
            .putInt("DataVersion", 3953)
            .put("size", ints(2, 1, 2))
            .put("palettes", LinListTag.builder(LinTagType.<LinCompoundTag>listTag())
                .add(palette)
                .add(secondPalette)
                .build())
            .put("blocks", LinListTag.builder(LinTagType.compoundTag())
                .add(LinCompoundTag.builder().putInt("state", 0).put("pos", ints(0, 0, 0)).build())
                .add(LinCompoundTag.builder().putInt("state", 1).put("pos", ints(1, 0, 0)).build())
                .add(LinCompoundTag.builder().putInt("state", 2).put("pos", ints(0, 0, 1)).build())
                .build())
            .put("entities", LinListTag.empty(LinTagType.compoundTag()))
            .build();
        byte[] data = gzipNbt(root);

        assertTrue(FORMAT.isFormat(new ByteArrayInputStream(data)));
        ClipboardReader reader = FORMAT.getReader(new ByteArrayInputStream(data));
        assertEquals(OptionalInt.of(3953), reader.getDataVersion());
        Clipboard clipboard = reader.read();

        assertEquals(BlockVector3.ZERO, clipboard.getMinimumPoint());
        assertEquals(BlockVector3.at(1, 0, 1), clipboard.getMaximumPoint());
        assertEquals(state(STONE), clipboard.getBlock(BlockVector3.at(0, 0, 0)));
        BlockState stairs = clipboard.getBlock(BlockVector3.at(1, 0, 0));
        BlockType stairsType = stairs.getBlockType();
        assertEquals(Direction.SOUTH, stairs.getState(stairsType.<Direction>getProperty("facing")));
        assertEquals("bottom", stairs.getState(stairsType.<String>getProperty("half")));
        // Unknown block becomes air, and so does the unlisted (structure void) position
        assertTrue(clipboard.getBlock(BlockVector3.at(0, 0, 1)).getBlockType().id().endsWith(":air"));
        assertTrue(clipboard.getBlock(BlockVector3.at(1, 0, 1)).getBlockType().id().endsWith(":air"));
    }

    @Test
    @DisplayName("applies the data fixer to older structures")
    void appliesDataFixer() throws Exception {
        when(MOCKED_PLATFORM.getDataVersion()).thenReturn(4000);
        DataFixer fixer = new DataFixer() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> T fixUp(FixType<T> type, T original, int srcVer) {
                if (type == FixTypes.BLOCK_STATE && srcVer == 1000) {
                    return (T) ((String) original).replace("minecraft:old_stairs", STAIRS).replace("dir=", "facing=");
                }
                return original;
            }
        };
        when(MOCKED_PLATFORM.getDataFixer()).thenReturn(fixer);

        LinCompoundTag root = LinCompoundTag.builder()
            .putInt("DataVersion", 1000)
            .put("size", ints(1, 1, 1))
            .put("palette", LinListTag.builder(LinTagType.compoundTag())
                .add(LinCompoundTag.builder()
                    .putString("Name", "minecraft:old_stairs")
                    .put("Properties", LinCompoundTag.builder().putString("dir", "west").build())
                    .build())
                .build())
            .put("blocks", LinListTag.builder(LinTagType.compoundTag())
                .add(LinCompoundTag.builder().putInt("state", 0).put("pos", ints(0, 0, 0)).build())
                .build())
            .build();

        BlockState fixed = read(gzipNbt(root)).getBlock(BlockVector3.ZERO);
        assertEquals(STAIRS, fixed.getBlockType().id());
        assertEquals(Direction.WEST, fixed.getState(fixed.getBlockType().<Direction>getProperty("facing")));
    }

    @Test
    @DisplayName("is detected by content and does not claim other formats")
    void detection() throws Exception {
        byte[] structure = write(createClipboard());
        assertSame(FORMAT, ClipboardFormats.findByInputStream(() -> new ByteArrayInputStream(structure)));

        when(MOCKED_PLATFORM.id()).thenReturn("enginehub:test");
        when(MOCKED_PLATFORM.getPlatformName()).thenReturn("Test");
        when(MOCKED_PLATFORM.getPlatformVersion()).thenReturn("1.0");
        ByteArrayOutputStream sponge = new ByteArrayOutputStream();
        try (ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_V3_SCHEMATIC.getWriter(sponge)) {
            writer.write(createClipboard());
        }
        assertFalse(FORMAT.isFormat(new ByteArrayInputStream(sponge.toByteArray())));
        assertSame(BuiltInClipboardFormat.SPONGE_V3_SCHEMATIC,
            ClipboardFormats.findByInputStream(() -> new ByteArrayInputStream(sponge.toByteArray())));

        assertFalse(FORMAT.isFormat(new ByteArrayInputStream("not nbt".getBytes(StandardCharsets.UTF_8))));
        assertSame(FORMAT, ClipboardFormats.findByAlias("structure"));
        assertSame(FORMAT, ClipboardFormats.findByAlias("nbt"));
        assertTrue(ClipboardFormats.getFileExtensionMap().get("nbt").contains(FORMAT));
    }

    @Test
    @DisplayName("rejects malformed structures")
    void rejectsMalformed() throws Exception {
        LinCompoundTag badState = LinCompoundTag.builder()
            .put("size", ints(1, 1, 1))
            .put("palette", LinListTag.builder(LinTagType.compoundTag())
                .add(LinCompoundTag.builder().putString("Name", STONE).build())
                .build())
            .put("blocks", LinListTag.builder(LinTagType.compoundTag())
                .add(LinCompoundTag.builder().putInt("state", 5).put("pos", ints(0, 0, 0)).build())
                .build())
            .build();
        assertThrows(IOException.class, () -> read(gzipNbt(badState)));

        LinCompoundTag emptySize = badState.toBuilder().put("size", ints(0, 1, 1)).build();
        assertThrows(IOException.class, () -> read(gzipNbt(emptySize)));
    }

    @Test
    @DisplayName("parses and formats state strings")
    void stateStrings() {
        Map<String, String> properties = new HashMap<>();
        assertEquals(STAIRS, MinecraftStructureReader.parseStateString(STAIRS + "[facing=north,half=top]", properties));
        assertEquals(Map.of("facing", "north", "half", "top"), properties);
        assertEquals(STAIRS + "[facing=north]", MinecraftStructureReader.toStateString(STAIRS, Map.of("facing", "north")));
        assertEquals(STONE, MinecraftStructureReader.toStateString(STONE, Map.of()));
    }

    @Test
    @DisplayName("export-only formats refuse loading")
    void exportOnlyFormatsRefuseLoading() {
        for (ClipboardFormat format : List.of(BuiltInClipboardFormat.WAVEFRONT_OBJ, BuiltInClipboardFormat.JSON)) {
            assertFalse(format.supportsReading(), format.getName());
            assertTrue(format.supportsWriting(), format.getName());
            assertFalse(format.isFormat(new ByteArrayInputStream(new byte[0])));
            IOException e = assertThrows(IOException.class, () -> format.getReader(new ByteArrayInputStream(new byte[0])));
            assertTrue(e.getMessage().contains("export-only"), e.getMessage());
        }
        assertTrue(FORMAT.supportsReading());
        assertTrue(FORMAT.supportsWriting());
        assertFalse(BuiltInClipboardFormat.MCEDIT_SCHEMATIC.supportsWriting());
        assertFalse(BuiltInClipboardFormat.SPONGE_V1_SCHEMATIC.supportsWriting());
    }
}
