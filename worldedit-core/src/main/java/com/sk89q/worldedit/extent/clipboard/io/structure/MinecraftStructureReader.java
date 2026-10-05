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

import com.google.common.base.Splitter;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.concurrency.LazyReference;
import com.sk89q.worldedit.world.DataFixer;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.world.entity.EntityType;
import com.sk89q.worldedit.world.entity.EntityTypes;
import org.apache.logging.log4j.Logger;
import org.enginehub.linbus.stream.LinStream;
import org.enginehub.linbus.tree.LinCompoundTag;
import org.enginehub.linbus.tree.LinDoubleTag;
import org.enginehub.linbus.tree.LinFloatTag;
import org.enginehub.linbus.tree.LinIntArrayTag;
import org.enginehub.linbus.tree.LinIntTag;
import org.enginehub.linbus.tree.LinListTag;
import org.enginehub.linbus.tree.LinRootEntry;
import org.enginehub.linbus.tree.LinStringTag;
import org.enginehub.linbus.tree.LinTag;
import org.enginehub.linbus.tree.LinTagType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.OptionalInt;
import javax.annotation.Nullable;

/**
 * Reads vanilla Minecraft structure files ({@code .nbt}), as written by structure blocks,
 * the {@code /place template} command and data packs.
 *
 * <p>Positions that are absent from the structure (structure voids) are left as air.
 * When a structure contains multiple palettes (e.g. shipwrecks), the first palette is used.</p>
 */
public class MinecraftStructureReader implements ClipboardReader {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    /**
     * The data version vanilla assumes for structures that do not declare one.
     */
    static final int DEFAULT_DATA_VERSION = 500;

    /**
     * The largest size of a structure along one axis, as for Sponge schematics.
     */
    static final int MAX_AXIS_SIZE = 0xFFFF;

    /**
     * The largest number of positions of a structure. The clipboard is allocated from the
     * declared size before any block is read, so a tiny file could otherwise request an
     * allocation of many gigabytes.
     */
    static final long MAX_VOLUME = 1L << 26;

    private final LinStream rootStream;
    @Nullable
    private LinCompoundTag root;

    public MinecraftStructureReader(LinStream rootStream) {
        this.rootStream = rootStream;
    }

    /**
     * Checks whether the given root tag looks like a vanilla structure.
     *
     * @param root the root compound
     * @return true if it is a structure
     */
    public static boolean isStructure(LinCompoundTag root) {
        if (root.value().containsKey("Schematic") || root.value().containsKey("Materials")) {
            return false;
        }
        if (root.findListTag("size", LinTagType.intTag()) == null) {
            return false;
        }
        if (root.findListTag("blocks", LinTagType.compoundTag()) == null) {
            return false;
        }
        return root.value().get("palette") instanceof LinListTag<?>
            || root.value().get("palettes") instanceof LinListTag<?>;
    }

    private LinCompoundTag getRoot() throws IOException {
        if (root == null) {
            root = LinRootEntry.readFrom(rootStream).value();
            if (!isStructure(root)) {
                throw new IOException("Not a Minecraft structure file");
            }
        }
        return root;
    }

    @Override
    public OptionalInt getDataVersion() {
        try {
            LinIntTag tag = getRoot().findTag("DataVersion", LinTagType.intTag());
            return OptionalInt.of(tag == null ? DEFAULT_DATA_VERSION : tag.valueAsInt());
        } catch (IOException _) {
            return OptionalInt.empty();
        }
    }

    @Override
    public Clipboard read() throws IOException {
        LinCompoundTag structure = getRoot();
        try {
            return read(structure);
        } catch (IllegalArgumentException | IllegalStateException | ClassCastException
                 | IndexOutOfBoundsException | NoSuchElementException e) {
            // Missing or mistyped tags of a malformed file
            throw new IOException("Malformed Minecraft structure: " + e.getMessage(), e);
        }
    }

    private Clipboard read(LinCompoundTag structure) throws IOException {
        int[] size = readIntTriple(structure.getListTag("size", LinTagType.intTag()), "size");
        checkSize(size);

        FixerContext fixer = createFixer(structure);

        BlockVector3 origin = BlockVector3.ZERO;
        BlockVector3 offset = BlockVector3.ZERO;
        LinCompoundTag worldEditMeta = structure.findTag(MinecraftStructureWriter.METADATA_TAG, LinTagType.compoundTag());
        if (worldEditMeta != null) {
            origin = readBlockVector(worldEditMeta.findTag("Origin", LinTagType.intArrayTag()));
            offset = readBlockVector(worldEditMeta.findTag("Offset", LinTagType.intArrayTag()));
        }
        BlockVector3 min = checkedAdd(origin, offset);
        // Reject positions whose maximum would overflow
        checkedAdd(min, BlockVector3.at(size[0] - 1, size[1] - 1, size[2] - 1));

        BlockArrayClipboard clipboard = new BlockArrayClipboard(
            new CuboidRegion(min, min.add(size[0] - 1, size[1] - 1, size[2] - 1))
        );
        clipboard.setOrigin(origin);

        List<BlockState> palette = readPalette(structure, fixer);

        for (LinCompoundTag blockTag : structure.getListTag("blocks", LinTagType.compoundTag()).value()) {
            int stateId = blockTag.getTag("state", LinTagType.intTag()).valueAsInt();
            if (stateId < 0 || stateId >= palette.size()) {
                throw new IOException("Block references unknown palette entry " + stateId);
            }
            int[] pos = readIntTriple(blockTag.getListTag("pos", LinTagType.intTag()), "pos");
            if (pos[0] < 0 || pos[1] < 0 || pos[2] < 0 || pos[0] >= size[0] || pos[1] >= size[1] || pos[2] >= size[2]) {
                LOGGER.warn("Skipping structure block outside of the structure bounds at {},{},{}", pos[0], pos[1], pos[2]);
                continue;
            }
            BlockVector3 point = min.add(pos[0], pos[1], pos[2]);
            BlockState state = palette.get(stateId);
            BaseBlock block;
            LinCompoundTag nbt = blockTag.findTag("nbt", LinTagType.compoundTag());
            if (nbt != null) {
                LinCompoundTag.Builder data = nbt.toBuilder();
                data.putInt("x", point.x());
                data.putInt("y", point.y());
                data.putInt("z", point.z());
                block = state.toBaseBlock(fixer.fixUp(DataFixer.FixTypes.BLOCK_ENTITY, data.build()));
            } else {
                block = state.toBaseBlock();
            }
            clipboard.setBlock(point, block);
        }

        LinListTag<LinCompoundTag> entities = structure.findListTag("entities", LinTagType.compoundTag());
        if (entities != null) {
            readEntities(clipboard, min, entities, fixer);
        }

        return clipboard;
    }

    private static List<BlockState> readPalette(LinCompoundTag structure, FixerContext fixer) throws IOException {
        LinListTag<LinCompoundTag> paletteTag = structure.findListTag("palette", LinTagType.compoundTag());
        if (paletteTag == null) {
            LinListTag<LinListTag<LinCompoundTag>> palettes = structure.getListTag(
                "palettes", LinTagType.<LinCompoundTag>listTag()
            );
            if (palettes.value().isEmpty()) {
                throw new IOException("Structure has no palette");
            }
            paletteTag = palettes.value().getFirst().asTypeChecked(LinTagType.compoundTag());
        }
        List<BlockState> palette = new ArrayList<>(paletteTag.value().size());
        for (LinCompoundTag entry : paletteTag.value()) {
            String name = entry.getTag("Name", LinTagType.stringTag()).value();
            Map<String, String> properties = new LinkedHashMap<>();
            LinCompoundTag propertiesTag = entry.findTag("Properties", LinTagType.compoundTag());
            if (propertiesTag != null) {
                for (Map.Entry<String, LinTag<?>> property : propertiesTag.value().entrySet()) {
                    if (property.getValue() instanceof LinStringTag stringTag) {
                        properties.put(property.getKey(), stringTag.value());
                    }
                }
            }
            if (fixer.isActive()) {
                String fixed = fixer.fixUp(DataFixer.FixTypes.BLOCK_STATE, toStateString(name, properties));
                properties.clear();
                name = parseStateString(fixed, properties);
            }
            palette.add(toBlockState(name, properties));
        }
        return palette;
    }

    /**
     * Converts a block id and properties into a state string such as {@code minecraft:chest[facing=north]}.
     *
     * @param name the block id
     * @param properties the properties
     * @return the state string
     */
    static String toStateString(String name, Map<String, String> properties) {
        if (properties.isEmpty()) {
            return name;
        }
        StringBuilder builder = new StringBuilder(name).append('[');
        boolean first = true;
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            if (!first) {
                builder.append(',');
            }
            first = false;
            builder.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return builder.append(']').toString();
    }

    /**
     * Parses a state string, filling the given property map.
     *
     * @param state the state string
     * @param properties the property map to fill
     * @return the block id
     */
    static String parseStateString(String state, Map<String, String> properties) {
        int open = state.indexOf('[');
        if (open < 0 || !state.endsWith("]")) {
            return state;
        }
        String body = state.substring(open + 1, state.length() - 1);
        for (String part : Splitter.on(',').split(body)) {
            int eq = part.indexOf('=');
            if (eq > 0) {
                properties.put(part.substring(0, eq).trim(), part.substring(eq + 1).trim());
            }
        }
        return state.substring(0, open);
    }

    /**
     * Resolves a block state from its id and string properties. Unknown blocks become air,
     * unknown properties or values are ignored.
     *
     * @param name the block id
     * @param properties the properties
     * @return the block state
     */
    static BlockState toBlockState(String name, Map<String, String> properties) {
        BlockType type = BlockTypes.get(name.toLowerCase(Locale.ROOT));
        if (type == null) {
            LOGGER.warn("Unknown block type {} in structure palette. Block will be replaced with air.", name);
            return BlockTypes.AIR.getDefaultState();
        }
        BlockState state = type.getDefaultState();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            Property<Object> property = type.getPropertyMap().containsKey(entry.getKey())
                ? type.getProperty(entry.getKey())
                : null;
            if (property == null) {
                LOGGER.debug("Ignoring unknown property {} on {}", entry.getKey(), name);
                continue;
            }
            Object value;
            try {
                value = property.getValueFor(entry.getValue());
            } catch (IllegalArgumentException _) {
                value = null;
            }
            if (value == null) {
                LOGGER.warn("Ignoring invalid value {} for property {} on {}", entry.getValue(), entry.getKey(), name);
                continue;
            }
            state = state.with(property, value);
        }
        return state;
    }

    private static void readEntities(BlockArrayClipboard clipboard, BlockVector3 min,
                                     LinListTag<LinCompoundTag> entities, FixerContext fixer) throws IOException {
        for (LinCompoundTag entityTag : entities.value()) {
            LinCompoundTag nbt = entityTag.findTag("nbt", LinTagType.compoundTag());
            LinListTag<LinDoubleTag> posTag = entityTag.findListTag("pos", LinTagType.doubleTag());
            if (nbt == null || posTag == null || posTag.value().size() != 3) {
                LOGGER.warn("Skipping malformed entity in structure");
                continue;
            }
            LinStringTag idTag = nbt.findTag("id", LinTagType.stringTag());
            if (idTag == null) {
                LOGGER.warn("Skipping entity without an id in structure");
                continue;
            }
            nbt = fixer.fixUp(DataFixer.FixTypes.ENTITY, nbt);
            LinStringTag fixedId = nbt.findTag("id", LinTagType.stringTag());
            String id = fixedId != null ? fixedId.value() : idTag.value();
            EntityType type = EntityTypes.get(id);
            if (type == null) {
                LOGGER.warn("Unknown entity when loading structure: {}", id);
                continue;
            }
            float yaw = 0;
            float pitch = 0;
            LinListTag<LinFloatTag> rotation = nbt.findListTag("Rotation", LinTagType.floatTag());
            if (rotation != null && rotation.value().size() == 2) {
                yaw = rotation.get(0).valueAsFloat();
                pitch = rotation.get(1).valueAsFloat();
            }
            Vector3 position = Vector3.at(
                posTag.get(0).valueAsDouble(),
                posTag.get(1).valueAsDouble(),
                posTag.get(2).valueAsDouble()
            ).add(min.toVector3());
            if (!Double.isFinite(position.x()) || !Double.isFinite(position.y()) || !Double.isFinite(position.z())) {
                LOGGER.warn("Skipping entity with an invalid position in structure");
                continue;
            }
            Location location = new Location(clipboard, position, yaw, pitch);
            clipboard.createEntity(location, new BaseEntity(type, LazyReference.computed(nbt)));
        }
    }

    /**
     * Check the declared size of a structure before anything is allocated from it.
     *
     * @param size the size
     * @throws IOException if the size is empty, negative or too large
     */
    static void checkSize(int[] size) throws IOException {
        String described = size[0] + "x" + size[1] + "x" + size[2];
        if (size[0] <= 0 || size[1] <= 0 || size[2] <= 0) {
            throw new IOException("Structure has an empty or invalid size: " + described);
        }
        if (size[0] > MAX_AXIS_SIZE || size[1] > MAX_AXIS_SIZE || size[2] > MAX_AXIS_SIZE
            || (long) size[0] * size[1] * size[2] > MAX_VOLUME) {
            throw new IOException("Structure is too large: " + described + " (at most " + MAX_VOLUME + " blocks)");
        }
    }

    private static BlockVector3 checkedAdd(BlockVector3 a, BlockVector3 b) throws IOException {
        try {
            return BlockVector3.at(
                Math.addExact(a.x(), b.x()), Math.addExact(a.y(), b.y()), Math.addExact(a.z(), b.z())
            );
        } catch (ArithmeticException _) {
            throw new IOException("Structure position is out of range");
        }
    }

    private static int[] readIntTriple(LinListTag<LinIntTag> tag, String name) throws IOException {
        if (tag.value().size() != 3) {
            throw new IOException("Invalid structure " + name + ": expected 3 values");
        }
        return new int[] {
            tag.get(0).valueAsInt(), tag.get(1).valueAsInt(), tag.get(2).valueAsInt(),
        };
    }

    private static BlockVector3 readBlockVector(@Nullable LinIntArrayTag tag) throws IOException {
        if (tag == null) {
            return BlockVector3.ZERO;
        }
        int[] parts = tag.value();
        if (parts.length != 3) {
            throw new IOException("Invalid vector in structure metadata");
        }
        return BlockVector3.at(parts[0], parts[1], parts[2]);
    }

    private static FixerContext createFixer(LinCompoundTag structure) {
        LinIntTag dataVersionTag = structure.findTag("DataVersion", LinTagType.intTag());
        int dataVersion = dataVersionTag == null ? DEFAULT_DATA_VERSION : dataVersionTag.valueAsInt();
        Platform platform = WorldEdit.getInstance().getPlatformManager().queryCapability(Capability.WORLD_EDITING);
        int liveDataVersion = platform.getDataVersion();
        DataFixer fixer = null;
        if (dataVersion > liveDataVersion) {
            LOGGER.warn("Structure was made in a newer Minecraft version ({} > {}). Data may be incompatible.",
                dataVersion, liveDataVersion);
        } else if (dataVersion < liveDataVersion) {
            fixer = platform.getDataFixer();
            if (fixer == null) {
                LOGGER.info("Structure was made in an older Minecraft version ({} < {}), but DFU is not available."
                    + " Data may be incompatible.", dataVersion, liveDataVersion);
            }
        }
        return new FixerContext(dataVersion, fixer);
    }

    private record FixerContext(int dataVersion, @Nullable DataFixer fixer) {
        boolean isActive() {
            return fixer != null;
        }

        <T> T fixUp(DataFixer.FixType<T> type, T original) {
            return fixer == null ? original : fixer.fixUp(type, original, dataVersion);
        }
    }

    @Override
    public void close() throws IOException {
    }
}
