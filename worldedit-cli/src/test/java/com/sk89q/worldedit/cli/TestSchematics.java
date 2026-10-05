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

package com.sk89q.worldedit.cli;

import org.enginehub.linbus.stream.LinBinaryIO;
import org.enginehub.linbus.tree.LinCompoundTag;
import org.enginehub.linbus.tree.LinRootEntry;
import org.enginehub.linbus.tree.LinTagType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Builds tiny Sponge v3 schematics by hand, without needing a running WorldEdit platform.
 */
final class TestSchematics {

    static final int DATA_VERSION = 3700;

    private TestSchematics() {
    }

    /**
     * Write a schematic of {@code width x 1 x 1} blocks, all of the given block.
     */
    static void writeFilled(Path path, int width, String block) throws IOException {
        // Every block uses palette index 0, which is encoded as a single VarInt byte
        byte[] data = new byte[width];
        LinCompoundTag schematic = LinCompoundTag.builder()
            .putInt("Version", 3)
            .putInt("DataVersion", DATA_VERSION)
            .putShort("Width", (short) width)
            .putShort("Height", (short) 1)
            .putShort("Length", (short) 1)
            .put("Blocks", LinCompoundTag.builder()
                .put("Palette", LinCompoundTag.builder().putInt(block, 0).build())
                .putByteArray("Data", data)
                .build())
            .build();
        try (OutputStream out = Files.newOutputStream(path);
             DataOutputStream stream = new DataOutputStream(new GZIPOutputStream(out))) {
            LinBinaryIO.write(stream, new LinRootEntry("", LinCompoundTag.builder().put("Schematic", schematic).build()));
        }
    }

    /**
     * Write a load-only Sponge v1 schematic of {@code width x 1 x 1} blocks, all of the given block.
     */
    static void writeSpongeV1Filled(Path path, int width, String block) throws IOException {
        LinCompoundTag schematic = LinCompoundTag.builder()
            .putInt("Version", 1)
            .putShort("Width", (short) width)
            .putShort("Height", (short) 1)
            .putShort("Length", (short) 1)
            .putInt("PaletteMax", 1)
            .put("Palette", LinCompoundTag.builder().putInt(block, 0).build())
            .putByteArray("BlockData", new byte[width])
            .build();
        try (OutputStream out = Files.newOutputStream(path);
             DataOutputStream stream = new DataOutputStream(new GZIPOutputStream(out))) {
            LinBinaryIO.write(stream, new LinRootEntry("Schematic", schematic));
        }
    }

    /**
     * Read the {@code Schematic} tag of a Sponge v3 schematic.
     */
    static LinCompoundTag readSchematicTag(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path);
             DataInputStream stream = new DataInputStream(new GZIPInputStream(in))) {
            return LinRootEntry.readFrom(LinBinaryIO.read(stream)).value()
                .getTag("Schematic", LinTagType.compoundTag());
        }
    }
}
