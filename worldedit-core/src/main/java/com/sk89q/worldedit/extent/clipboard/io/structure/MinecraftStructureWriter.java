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

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import org.enginehub.linbus.stream.LinBinaryIO;
import org.enginehub.linbus.tree.LinCompoundTag;
import org.enginehub.linbus.tree.LinDoubleTag;
import org.enginehub.linbus.tree.LinFloatTag;
import org.enginehub.linbus.tree.LinIntTag;
import org.enginehub.linbus.tree.LinListTag;
import org.enginehub.linbus.tree.LinRootEntry;
import org.enginehub.linbus.tree.LinTagType;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Writes vanilla Minecraft structure files ({@code .nbt}) that can be loaded by structure blocks,
 * the {@code /place template} command and data packs.
 *
 * <p>Structure void blocks are omitted, as vanilla does. Biomes are not part of the format.
 * The WorldEdit origin is kept in an extra {@value #METADATA_TAG} compound which vanilla ignores.</p>
 */
public class MinecraftStructureWriter implements ClipboardWriter {

    /**
     * Name of the root compound holding WorldEdit specific metadata.
     */
    public static final String METADATA_TAG = "WorldEdit";

    private static final String STRUCTURE_VOID_ID = "minecraft:structure_void";

    private final DataOutputStream outputStream;

    public MinecraftStructureWriter(DataOutputStream outputStream) {
        this.outputStream = outputStream;
    }

    @Override
    public void write(Clipboard clipboard) throws IOException {
        LinBinaryIO.write(outputStream, new LinRootEntry("", toTag(clipboard)));
    }

    /**
     * Encode the clipboard as a structure compound.
     *
     * @param clipboard the clipboard
     * @return the structure tag
     */
    static LinCompoundTag toTag(Clipboard clipboard) {
        Region region = clipboard.getRegion();
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 origin = clipboard.getOrigin();
        int width = region.getWidth();
        int height = region.getHeight();
        int length = region.getLength();

        Map<BlockState, Integer> palette = new LinkedHashMap<>();
        LinListTag.Builder<LinCompoundTag> blocks = LinListTag.builder(LinTagType.compoundTag());

        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    BlockVector3 point = min.add(x, y, z);
                    BaseBlock block = clipboard.getFullBlock(point);
                    if (block.getBlockType().id().equals(STRUCTURE_VOID_ID)) {
                        continue;
                    }
                    BlockState state = block.toImmutableState();
                    int stateId = palette.computeIfAbsent(state, _ -> palette.size());

                    LinCompoundTag.Builder blockTag = LinCompoundTag.builder()
                        .putInt("state", stateId)
                        .put("pos", intList(x, y, z));
                    LinCompoundTag nbt = block.getNbt();
                    if (nbt != null) {
                        LinCompoundTag.Builder data = nbt.toBuilder();
                        data.remove("x");
                        data.remove("y");
                        data.remove("z");
                        blockTag.put("nbt", data.build());
                    }
                    blocks.add(blockTag.build());
                }
            }
        }

        LinListTag.Builder<LinCompoundTag> paletteTag = LinListTag.builder(LinTagType.compoundTag());
        for (BlockState state : palette.keySet()) {
            paletteTag.add(encodeState(state));
        }

        LinListTag.Builder<LinCompoundTag> entities = LinListTag.builder(LinTagType.compoundTag());
        for (Entity entity : clipboard.getEntities()) {
            LinCompoundTag encoded = encodeEntity(entity, min);
            if (encoded != null) {
                entities.add(encoded);
            }
        }

        BlockVector3 offset = min.subtract(origin);
        return LinCompoundTag.builder()
            .putInt("DataVersion", WorldEdit.getInstance().getPlatformManager()
                .queryCapability(Capability.WORLD_EDITING).getDataVersion())
            .put("size", intList(width, height, length))
            .put("palette", paletteTag.build())
            .put("blocks", blocks.build())
            .put("entities", entities.build())
            .put(METADATA_TAG, LinCompoundTag.builder()
                .putString("Version", WorldEdit.getVersion())
                .putIntArray("Origin", new int[] { origin.x(), origin.y(), origin.z() })
                .putIntArray("Offset", new int[] { offset.x(), offset.y(), offset.z() })
                .build())
            .build();
    }

    private static LinCompoundTag encodeState(BlockState state) {
        LinCompoundTag.Builder entry = LinCompoundTag.builder().putString("Name", state.getBlockType().id());
        Map<Property<?>, Object> states = state.getStates();
        if (!states.isEmpty()) {
            LinCompoundTag.Builder properties = LinCompoundTag.builder();
            for (Map.Entry<Property<?>, Object> property : states.entrySet()) {
                properties.putString(property.getKey().name(), String.valueOf(property.getValue()).toLowerCase(Locale.ROOT));
            }
            entry.put("Properties", properties.build());
        }
        return entry.build();
    }

    private static LinCompoundTag encodeEntity(Entity entity, BlockVector3 min) {
        BaseEntity state = entity.getState();
        if (state == null) {
            return null;
        }
        Location location = entity.getLocation();
        Vector3 relative = location.toVector().subtract(min.toVector3());

        LinCompoundTag.Builder nbt = LinCompoundTag.builder();
        LinCompoundTag raw = state.getNbt();
        if (raw != null) {
            nbt.putAll(raw.value());
        }
        // Vanilla drops the UUID so that every placement creates fresh entities
        nbt.remove("UUID");
        nbt.putString("id", state.getType().id());
        nbt.put("Pos", LinListTag.builder(LinTagType.doubleTag())
            .add(LinDoubleTag.of(relative.x()))
            .add(LinDoubleTag.of(relative.y()))
            .add(LinDoubleTag.of(relative.z()))
            .build());
        nbt.put("Rotation", LinListTag.builder(LinTagType.floatTag())
            .add(LinFloatTag.of(location.getYaw()))
            .add(LinFloatTag.of(location.getPitch()))
            .build());

        BlockVector3 blockPos = relative.toBlockPoint();
        return LinCompoundTag.builder()
            .put("pos", LinListTag.builder(LinTagType.doubleTag())
                .add(LinDoubleTag.of(relative.x()))
                .add(LinDoubleTag.of(relative.y()))
                .add(LinDoubleTag.of(relative.z()))
                .build())
            .put("blockPos", intList(blockPos.x(), blockPos.y(), blockPos.z()))
            .put("nbt", nbt.build())
            .build();
    }

    private static LinListTag<LinIntTag> intList(int x, int y, int z) {
        return LinListTag.builder(LinTagType.intTag())
            .add(LinIntTag.of(x))
            .add(LinIntTag.of(y))
            .add(LinIntTag.of(z))
            .build();
    }

    @Override
    public void close() throws IOException {
        outputStream.close();
    }
}
