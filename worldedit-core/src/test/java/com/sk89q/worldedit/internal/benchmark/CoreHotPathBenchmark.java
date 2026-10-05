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

package com.sk89q.worldedit.internal.benchmark;

import com.sk89q.worldedit.BaseWorldEditTest;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.function.block.BlockDistributionCounter;
import com.sk89q.worldedit.function.block.Counter;
import com.sk89q.worldedit.function.mask.RegionMask;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.visitor.RecursiveVisitor;
import com.sk89q.worldedit.function.visitor.RegionVisitor;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.registry.Registry;
import com.sk89q.worldedit.util.collection.BlockMap;
import com.sk89q.worldedit.util.collection.LocatedBlockList;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Micro-benchmarks for core hot paths, run against in-memory extents.
 *
 * <p>Disabled by default. Run with
 * {@code WORLDEDIT_BENCH=true ./gradlew :worldedit-core:test --tests '*CoreHotPathBenchmark'}.
 * Results (median wall time per operation) are appended to
 * {@code worldedit-core/build/benchmark-results.txt}.</p>
 */
@EnabledIfEnvironmentVariable(named = "WORLDEDIT_BENCH", matches = "true")
@Execution(ExecutionMode.SAME_THREAD)
class CoreHotPathBenchmark extends BaseWorldEditTest {

    private static final int WARMUP = 5;
    private static final int MEASURE = 9;

    private static BlockState stone;
    private static BlockState dirt;
    private static BlockState[] palette;
    private static boolean registeredAir;

    @BeforeAll
    static void registerBlocks() {
        // EditSession-based benchmarks need air; harmless if already registered
        registeredAir = BlockType.REGISTRY.get("minecraft:air") == null;
        register("minecraft:air");
        stone = register("bench:stone");
        dirt = register("bench:dirt");
        palette = new BlockState[16];
        for (int i = 0; i < palette.length; i++) {
            palette[i] = register("bench:block_" + i);
        }
    }

    @AfterAll
    static void unregisterBlocks() throws Exception {
        Field map = Registry.class.getDeclaredField("map");
        map.setAccessible(true);
        Map<?, ?> registered = (Map<?, ?>) map.get(BlockType.REGISTRY);
        registered.keySet().removeIf(key -> key.toString().startsWith("bench:")
            || (registeredAir && key.toString().equals("minecraft:air")));
    }

    private static BlockState register(String id) {
        BlockType existing = BlockType.REGISTRY.get(id);
        if (existing != null) {
            return existing.getDefaultState();
        }
        return BlockType.REGISTRY.register(id, new BlockType(id)).getDefaultState();
    }

    @FunctionalInterface
    private interface Body {
        long run() throws Exception;
    }

    private static void bench(String name, Body body) throws Exception {
        long sink = 0;
        for (int i = 0; i < WARMUP; i++) {
            sink += body.run();
        }
        long[] times = new long[MEASURE];
        for (int i = 0; i < MEASURE; i++) {
            long start = System.nanoTime();
            sink += body.run();
            times[i] = System.nanoTime() - start;
        }
        Arrays.sort(times);
        String line = String.format("%-40s median %9.3f ms  (min %9.3f ms)  [sink=%d]%n",
            name, times[MEASURE / 2] / 1e6, times[0] / 1e6, sink);
        System.out.print(line);
        try {
            Files.writeString(Path.of("build", "benchmark-results.txt"), line, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static CuboidRegion cube(int size) {
        return new CuboidRegion(BlockVector3.at(-size / 2, 0, -size / 2),
            BlockVector3.at(size / 2 - 1, size - 1, size / 2 - 1));
    }

    private static BlockArrayClipboard filledClipboard(CuboidRegion region) throws WorldEditException {
        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
        for (BlockVector3 pos : region) {
            int idx = Math.floorMod(pos.x() * 31 + pos.y() * 7 + pos.z(), palette.length);
            clipboard.setBlock(pos, palette[idx]);
        }
        return clipboard;
    }

    @Test
    void recursiveVisitorFloodFill() throws Exception {
        CuboidRegion region = cube(96);
        bench("RecursiveVisitor flood fill 96^3", () -> {
            Counter counter = new Counter();
            RecursiveVisitor visitor = new RecursiveVisitor(new RegionMask(region), counter);
            visitor.visit(BlockVector3.at(0, 10, 0));
            Operations.completeBlindly(visitor);
            assertEquals(region.getVolume(), counter.getCount());
            return counter.getCount();
        });
    }

    @Test
    void cuboidRegionIterate() throws Exception {
        CuboidRegion region = cube(128);
        bench("CuboidRegion iterate+contains 128^3", () -> {
            long sum = 0;
            for (BlockVector3 pos : region) {
                if (region.contains(pos)) {
                    sum += pos.x();
                }
            }
            return sum;
        });
    }

    @Test
    void clipboardSetGet() throws Exception {
        CuboidRegion region = cube(128);
        BlockArrayClipboard clipboard = filledClipboard(region);
        bench("BlockArrayClipboard set 128^3", () -> filledClipboard(region).getDimensions().x());
        bench("BlockArrayClipboard getFullBlock 128^3", () -> {
            long sum = 0;
            for (BlockVector3 pos : region) {
                sum += clipboard.getFullBlock(pos).hashCode();
            }
            return sum;
        });
    }

    @Test
    void forwardExtentCopy() throws Exception {
        CuboidRegion region = cube(96);
        BlockArrayClipboard source = filledClipboard(region);
        bench("ForwardExtentCopy clipboard 96^3", () -> {
            BlockArrayClipboard target = new BlockArrayClipboard(region);
            ForwardExtentCopy copy = new ForwardExtentCopy(source, region, target, region.getMinimumPoint());
            Operations.completeBlindly(copy);
            return copy.getAffected();
        });
    }

    @Test
    void blockDistribution() throws Exception {
        CuboidRegion region = cube(128);
        BlockArrayClipboard clipboard = filledClipboard(region);
        bench("BlockDistributionCounter 128^3", () -> {
            BlockDistributionCounter counter = new BlockDistributionCounter(clipboard, true);
            Operations.completeBlindly(new RegionVisitor(region, counter));
            return counter.getDistribution().size();
        });
    }

    @Test
    void blockMapPutGet() throws Exception {
        CuboidRegion region = cube(96);
        BaseBlock block = stone.toBaseBlock();
        bench("BlockMap put+get+iterate 96^3", () -> {
            BlockMap<BaseBlock> map = BlockMap.createForBaseBlock();
            for (BlockVector3 pos : region) {
                map.put(pos, block);
            }
            long sum = 0;
            for (BlockVector3 pos : region) {
                sum += map.get(pos) == block ? 1 : 0;
            }
            for (Map.Entry<BlockVector3, BaseBlock> entry : map.entrySet()) {
                sum += entry.getKey().y();
            }
            return sum;
        });
    }

    @Test
    void locatedBlockList() throws Exception {
        CuboidRegion region = cube(96);
        bench("LocatedBlockList add+iterate 96^3", () -> {
            LocatedBlockList list = new LocatedBlockList();
            for (BlockVector3 pos : region) {
                if (!list.containsLocation(pos)) {
                    list.add(pos, dirt);
                }
            }
            long sum = 0;
            var it = list.reverseIterator();
            while (it.hasNext()) {
                sum += it.next().location().y();
            }
            return sum;
        });
    }

    @Test
    void drawLine() throws Exception {
        bench("EditSession.drawLine r=6 len=300", () -> {
            try (EditSession session = WorldEdit.getInstance().newEditSessionBuilder().world(null).build()) {
                return session.drawLine(stone, List.of(BlockVector3.at(0, 0, 0), BlockVector3.at(300, 40, 120)), 6, true);
            }
        });
        bench("EditSession.drawLine r=6 len=300 hollow", () -> {
            try (EditSession session = WorldEdit.getInstance().newEditSessionBuilder().world(null).build()) {
                return session.drawLine(stone, List.of(BlockVector3.at(0, 0, 0), BlockVector3.at(300, 40, 120)), 6, false);
            }
        });
    }
}
