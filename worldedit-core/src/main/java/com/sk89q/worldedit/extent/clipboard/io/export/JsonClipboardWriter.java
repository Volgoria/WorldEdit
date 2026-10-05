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

import com.google.gson.stream.JsonWriter;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import org.enginehub.linbus.format.snbt.LinStringIO;
import org.enginehub.linbus.tree.LinCompoundTag;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Exports a clipboard as a simple JSON document, intended for web viewers and other tools.
 *
 * <p>The document looks like:</p>
 * <pre>{@code
 * {
 *   "format": "worldedit:json", "version": 1, "dataVersion": 3953,
 *   "size": [w, h, l], "origin": [x, y, z], "offset": [x, y, z],
 *   "palette": ["minecraft:air", "minecraft:oak_stairs[facing=north,half=bottom,...]"],
 *   "blocks": [0, 1, ...],
 *   "blockEntities": [{"pos": [x, y, z], "id": "minecraft:chest", "snbt": "{...}"}],
 *   "entities": [{"id": "minecraft:pig", "pos": [x, y, z], "rotation": [yaw, pitch], "snbt": "{...}"}]
 * }
 * }</pre>
 *
 * <p>{@code blocks} holds one palette index per block, ordered by Y, then Z, then X, i.e. the index of
 * relative position {@code (x, y, z)} is {@code (y * l + z) * w + x}. Positions are relative to the
 * clipboard minimum point; {@code offset} is the minimum point relative to the origin.</p>
 */
public class JsonClipboardWriter implements ClipboardWriter {

    /**
     * The current version of the JSON layout.
     */
    public static final int VERSION = 1;

    private final JsonWriter writer;

    public JsonClipboardWriter(OutputStream outputStream) {
        this.writer = new JsonWriter(new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)));
    }

    @Override
    public void write(Clipboard clipboard) throws IOException {
        Region region = clipboard.getRegion();
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 origin = clipboard.getOrigin();
        int width = region.getWidth();
        int height = region.getHeight();
        int length = region.getLength();

        writer.beginObject();
        writer.name("format").value("worldedit:json");
        writer.name("version").value(VERSION);
        writer.name("dataVersion").value(WorldEdit.getInstance().getPlatformManager()
            .queryCapability(Capability.WORLD_EDITING).getDataVersion());
        writeVector(writer.name("size"), BlockVector3.at(width, height, length));
        writeVector(writer.name("origin"), origin);
        writeVector(writer.name("offset"), min.subtract(origin));

        Map<String, Integer> palette = new LinkedHashMap<>();
        int[] blocks = new int[width * height * length];
        Map<BlockVector3, BaseBlock> blockEntities = new LinkedHashMap<>();
        int index = 0;
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    BlockVector3 point = min.add(x, y, z);
                    BaseBlock block = clipboard.getFullBlock(point);
                    BlockState state = block.toImmutableState();
                    blocks[index++] = palette.computeIfAbsent(state.getAsString(), _ -> palette.size());
                    if (block.getNbt() != null) {
                        blockEntities.put(BlockVector3.at(x, y, z), block);
                    }
                }
            }
        }

        writer.name("palette").beginArray();
        for (String key : palette.keySet()) {
            writer.value(key);
        }
        writer.endArray();

        writer.name("blocks").beginArray();
        for (int block : blocks) {
            writer.value(block);
        }
        writer.endArray();

        writer.name("blockEntities").beginArray();
        for (Map.Entry<BlockVector3, BaseBlock> entry : blockEntities.entrySet()) {
            writer.beginObject();
            writeVector(writer.name("pos"), entry.getKey());
            writer.name("id").value(entry.getValue().getNbtId());
            LinCompoundTag nbt = entry.getValue().getNbt();
            if (nbt != null) {
                LinCompoundTag.Builder data = nbt.toBuilder();
                data.remove("x");
                data.remove("y");
                data.remove("z");
                writer.name("snbt").value(LinStringIO.writeToString(data.build()));
            }
            writer.endObject();
        }
        writer.endArray();

        writer.name("entities").beginArray();
        for (Entity entity : clipboard.getEntities()) {
            BaseEntity state = entity.getState();
            if (state == null) {
                continue;
            }
            Location location = entity.getLocation();
            Vector3 relative = location.toVector().subtract(min.toVector3());
            writer.beginObject();
            writer.name("id").value(state.getType().id());
            writer.name("pos").beginArray()
                .value(relative.x()).value(relative.y()).value(relative.z())
                .endArray();
            writer.name("rotation").beginArray()
                .value(location.getYaw()).value(location.getPitch())
                .endArray();
            LinCompoundTag nbt = state.getNbt();
            if (nbt != null) {
                writer.name("snbt").value(LinStringIO.writeToString(nbt));
            }
            writer.endObject();
        }
        writer.endArray();

        writer.endObject();
        writer.flush();
    }

    private static void writeVector(JsonWriter writer, BlockVector3 vector) throws IOException {
        writer.beginArray().value(vector.x()).value(vector.y()).value(vector.z()).endArray();
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
