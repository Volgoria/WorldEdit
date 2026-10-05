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

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Exports a clipboard as a Wavefront OBJ mesh, for use in 3D modelling and rendering tools.
 *
 * <p>Only exposed faces are emitted: a face is hidden when the neighbouring block occludes it
 * (an opaque full cube) or is of the same block type (e.g. glass next to glass). Faces are grouped
 * into one object group and material per block type, and coplanar faces of the same type are
 * merged into larger rectangles (greedy meshing) unless disabled.</p>
 *
 * <p>Materials are written to a separate {@code .mtl} material library when one is configured with
 * {@link #setMaterialLibrary(String, OutputStream)}; colours are approximations from {@link BlockColors}.</p>
 *
 * <p>Coordinates are in blocks, relative to the clipboard minimum point, with Y up.</p>
 */
public class WavefrontObjWriter implements ClipboardWriter {

    /**
     * Unit normals, indexed by face id: +X, -X, +Y, -Y, +Z, -Z.
     */
    private static final int[][] NORMALS = {
        { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 }, { 0, 0, -1 },
    };

    private final Writer writer;
    private final Predicate<BlockState> occluding;
    private boolean greedyMeshing = true;
    @Nullable
    private String materialLibraryName;
    @Nullable
    private OutputStream materialLibraryStream;

    /**
     * Create a new writer using block materials to decide which blocks hide their neighbours' faces.
     *
     * @param outputStream the stream the OBJ data is written to
     */
    public WavefrontObjWriter(OutputStream outputStream) {
        this(outputStream, WavefrontObjWriter::isOccludingByMaterial);
    }

    /**
     * Create a new writer.
     *
     * @param outputStream the stream the OBJ data is written to
     * @param occluding whether a block fully hides the faces of the blocks next to it
     */
    public WavefrontObjWriter(OutputStream outputStream, Predicate<BlockState> occluding) {
        checkNotNull(outputStream);
        checkNotNull(occluding);
        this.writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
        this.occluding = occluding;
    }

    /**
     * Set whether coplanar faces of the same block type are merged. Enabled by default.
     *
     * @param greedyMeshing true to merge faces
     */
    public void setGreedyMeshing(boolean greedyMeshing) {
        this.greedyMeshing = greedyMeshing;
    }

    /**
     * Also write a material library. The OBJ file references it by the given file name, so it
     * should be stored next to the OBJ file. The stream is closed when this writer is closed.
     *
     * @param fileName the material library file name, e.g. {@code house.mtl}
     * @param outputStream the stream the material library is written to
     */
    public void setMaterialLibrary(String fileName, OutputStream outputStream) {
        checkNotNull(fileName);
        checkNotNull(outputStream);
        this.materialLibraryName = fileName;
        this.materialLibraryStream = outputStream;
    }

    /**
     * Whether a block type is a kind of air, which is never rendered.
     *
     * @param type the block type
     * @return true for air
     */
    static boolean isAir(BlockType type) {
        String id = type.id();
        if (id.equals("minecraft:air") || id.equals("minecraft:cave_air") || id.equals("minecraft:void_air")) {
            return true;
        }
        try {
            return type.getMaterial().isAir();
        } catch (RuntimeException _) {
            return false;
        }
    }

    private static boolean isOccludingByMaterial(BlockState state) {
        try {
            BlockMaterial material = state.getBlockType().getMaterial();
            return material.isFullCube() && material.isOpaque() && !material.isAir();
        } catch (RuntimeException _) {
            return true;
        }
    }

    @Override
    public void write(Clipboard clipboard) throws IOException {
        Region region = clipboard.getRegion();
        BlockVector3 min = region.getMinimumPoint();
        int[] dims = { region.getWidth(), region.getHeight(), region.getLength() };

        // Material per cell: 0 is empty, otherwise index + 1 into the materials list
        List<BlockType> materials = new ArrayList<>();
        Map<BlockType, Integer> materialIds = new HashMap<>();
        int[] cells = new int[dims[0] * dims[1] * dims[2]];
        boolean[] occluders = new boolean[cells.length];
        Map<BlockState, Boolean> occluderCache = new HashMap<>();
        for (int y = 0; y < dims[1]; y++) {
            for (int z = 0; z < dims[2]; z++) {
                for (int x = 0; x < dims[0]; x++) {
                    BlockVector3 point = min.add(x, y, z);
                    if (!region.contains(point)) {
                        continue;
                    }
                    BlockState state = clipboard.getBlock(point);
                    BlockType type = state.getBlockType();
                    if (isAir(type)) {
                        continue;
                    }
                    int index = index(dims, x, y, z);
                    cells[index] = materialIds.computeIfAbsent(type, t -> {
                        materials.add(t);
                        return materials.size();
                    });
                    occluders[index] = occluderCache.computeIfAbsent(state, occluding::test);
                }
            }
        }

        // Collect quads per material: each quad is 12 ints (4 corners) followed by the face id
        List<List<int[]>> quads = new ArrayList<>();
        for (int i = 0; i < materials.size(); i++) {
            quads.add(new ArrayList<>());
        }
        for (int face = 0; face < 6; face++) {
            buildFaces(dims, cells, occluders, face, quads);
        }

        writeObj(clipboard, materials, quads);
        if (materialLibraryStream != null) {
            writeMaterialLibrary(materials);
        }
    }

    private void buildFaces(int[] dims, int[] cells, boolean[] occluders, int face, List<List<int[]>> quads) {
        int axis = face / 2;
        int sign = face % 2 == 0 ? 1 : -1;
        int axisU = (axis + 1) % 3;
        int axisV = (axis + 2) % 3;
        int nu = dims[axisU];
        int nv = dims[axisV];
        int[] mask = new int[nu * nv];
        int[] pos = new int[3];
        int[] neighbour = new int[3];

        for (int slice = 0; slice < dims[axis]; slice++) {
            // Build the mask of exposed faces for this slice
            boolean any = false;
            for (int v = 0; v < nv; v++) {
                for (int u = 0; u < nu; u++) {
                    pos[axis] = slice;
                    pos[axisU] = u;
                    pos[axisV] = v;
                    int cell = cells[index(dims, pos[0], pos[1], pos[2])];
                    int value = 0;
                    if (cell != 0) {
                        neighbour[0] = pos[0];
                        neighbour[1] = pos[1];
                        neighbour[2] = pos[2];
                        neighbour[axis] += sign;
                        if (!isHidden(dims, cells, occluders, neighbour, cell)) {
                            value = cell;
                            any = true;
                        }
                    }
                    mask[u + v * nu] = value;
                }
            }
            if (!any) {
                continue;
            }
            // The plane the faces of this slice lie in
            int plane = sign > 0 ? slice + 1 : slice;
            for (int v = 0; v < nv; v++) {
                for (int u = 0; u < nu; u++) {
                    int material = mask[u + v * nu];
                    if (material == 0) {
                        continue;
                    }
                    int width = 1;
                    int height = 1;
                    if (greedyMeshing) {
                        while (u + width < nu && mask[u + width + v * nu] == material) {
                            width++;
                        }
                        grow:
                        while (v + height < nv) {
                            for (int k = 0; k < width; k++) {
                                if (mask[u + k + (v + height) * nu] != material) {
                                    break grow;
                                }
                            }
                            height++;
                        }
                    }
                    for (int dv = 0; dv < height; dv++) {
                        for (int du = 0; du < width; du++) {
                            mask[u + du + (v + dv) * nu] = 0;
                        }
                    }
                    quads.get(material - 1).add(makeQuad(axis, axisU, axisV, sign, plane, u, v, width, height, face));
                }
            }
        }
    }

    private static int[] makeQuad(int axis, int axisU, int axisV, int sign, int plane,
                                  int u, int v, int width, int height, int face) {
        // Corners in counter-clockwise order when seen from the side the normal points to
        int[][] uv = sign > 0
            ? new int[][] { { u, v }, { u + width, v }, { u + width, v + height }, { u, v + height } }
            : new int[][] { { u, v }, { u, v + height }, { u + width, v + height }, { u + width, v } };
        int[] quad = new int[13];
        for (int corner = 0; corner < 4; corner++) {
            quad[corner * 3 + axis] = plane;
            quad[corner * 3 + axisU] = uv[corner][0];
            quad[corner * 3 + axisV] = uv[corner][1];
        }
        quad[12] = face;
        return quad;
    }

    private static boolean isHidden(int[] dims, int[] cells, boolean[] occluders, int[] pos, int material) {
        if (pos[0] < 0 || pos[1] < 0 || pos[2] < 0 || pos[0] >= dims[0] || pos[1] >= dims[1] || pos[2] >= dims[2]) {
            return false;
        }
        int index = index(dims, pos[0], pos[1], pos[2]);
        return occluders[index] || cells[index] == material;
    }

    private static int index(int[] dims, int x, int y, int z) {
        return (y * dims[2] + z) * dims[0] + x;
    }

    private void writeObj(Clipboard clipboard, List<BlockType> materials, List<List<int[]>> quads) throws IOException {
        Region region = clipboard.getRegion();
        writer.write("# Exported by WorldEdit " + WorldEdit.getVersion() + "\n");
        writer.write("# Size: " + region.getWidth() + " x " + region.getHeight() + " x " + region.getLength() + "\n");
        if (materialLibraryName != null) {
            writer.write("mtllib " + materialLibraryName + "\n");
        }
        for (int[] normal : NORMALS) {
            writer.write("vn " + normal[0] + " " + normal[1] + " " + normal[2] + "\n");
        }

        // Vertices are shared between all faces, keyed by their packed integer position
        Long2IntOpenHashMap vertexIds = new Long2IntOpenHashMap();
        vertexIds.defaultReturnValue(-1);
        StringBuilder faces = new StringBuilder();
        int faceCount = 0;
        for (int material = 0; material < materials.size(); material++) {
            List<int[]> materialQuads = quads.get(material);
            if (materialQuads.isEmpty()) {
                continue;
            }
            String name = materialName(materials.get(material));
            faces.append("g ").append(name).append('\n');
            faces.append("usemtl ").append(name).append('\n');
            for (int[] quad : materialQuads) {
                faces.append('f');
                for (int corner = 0; corner < 4; corner++) {
                    int x = quad[corner * 3];
                    int y = quad[corner * 3 + 1];
                    int z = quad[corner * 3 + 2];
                    long key = ((long) (x & 0x1FFFFF) << 42) | ((long) (y & 0x1FFFFF) << 21) | (z & 0x1FFFFF);
                    int id = vertexIds.get(key);
                    if (id == -1) {
                        id = vertexIds.size() + 1;
                        vertexIds.put(key, id);
                        writer.write("v " + x + " " + y + " " + z + "\n");
                    }
                    faces.append(' ').append(id).append("//").append(quad[12] + 1);
                }
                faces.append('\n');
                faceCount++;
            }
        }
        writer.write("# Faces: " + faceCount + "\n");
        writer.write(faces.toString());
        writer.flush();
    }

    private void writeMaterialLibrary(List<BlockType> materials) throws IOException {
        Writer mtl = new BufferedWriter(new OutputStreamWriter(materialLibraryStream, StandardCharsets.UTF_8));
        mtl.write("# Exported by WorldEdit " + WorldEdit.getVersion() + "\n");
        for (BlockType type : materials) {
            int color = BlockColors.getColor(type);
            mtl.write("\nnewmtl " + materialName(type) + "\n");
            mtl.write(String.format(Locale.ROOT, "Kd %.4f %.4f %.4f\n",
                ((color >> 16) & 0xFF) / 255.0, ((color >> 8) & 0xFF) / 255.0, (color & 0xFF) / 255.0));
            mtl.write("Ka 0.0000 0.0000 0.0000\n");
            mtl.write("Ks 0.0000 0.0000 0.0000\n");
            mtl.write(String.format(Locale.ROOT, "d %.2f\n", BlockColors.getOpacity(type)));
            mtl.write("illum 1\n");
        }
        mtl.flush();
    }

    /**
     * Get the OBJ/MTL material name for a block type.
     *
     * @param type the block type
     * @return a name without whitespace
     */
    static String materialName(BlockType type) {
        return type.id().replace(':', '_').replaceAll("\\s", "_");
    }

    @Override
    public void close() throws IOException {
        try {
            writer.close();
        } finally {
            if (materialLibraryStream != null) {
                materialLibraryStream.close();
            }
        }
    }
}
