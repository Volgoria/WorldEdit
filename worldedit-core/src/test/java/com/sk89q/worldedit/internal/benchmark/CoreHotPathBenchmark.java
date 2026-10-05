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
import com.sk89q.worldedit.extension.platform.Watchdog;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.function.block.BlockDistributionCounter;
import com.sk89q.worldedit.function.block.Counter;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.RegionMask;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.visitor.RecursiveVisitor;
import com.sk89q.worldedit.function.visitor.RegionVisitor;
import com.sk89q.worldedit.history.change.BlockChange;
import com.sk89q.worldedit.history.changeset.BlockOptimizedHistory;
import com.sk89q.worldedit.internal.block.BlockStateIdAccess;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.registry.Registry;
import com.sk89q.worldedit.util.SideEffectSet;
import com.sk89q.worldedit.util.collection.BlockMap;
import com.sk89q.worldedit.util.collection.LocatedBlockList;
import com.sk89q.worldedit.util.test.InMemoryWorld;
import com.sk89q.worldedit.util.test.SimpleMaterialRegistries;
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
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

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

    private static final int WARMUP = 10;
    private static final int MEASURE = 21;

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

    private static final com.sun.management.ThreadMXBean THREADS =
        (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();

    private static long allocatedBytes() {
        return THREADS.getCurrentThreadAllocatedBytes();
    }

    private static void bench(String name, Body body) throws Exception {
        long sink = 0;
        for (int i = 0; i < WARMUP; i++) {
            sink += body.run();
        }
        long[] times = new long[MEASURE];
        long[] allocs = new long[MEASURE];
        for (int i = 0; i < MEASURE; i++) {
            long startAlloc = allocatedBytes();
            long start = System.nanoTime();
            sink += body.run();
            times[i] = System.nanoTime() - start;
            allocs[i] = allocatedBytes() - startAlloc;
        }
        Arrays.sort(times);
        Arrays.sort(allocs);
        report(String.format("%-40s median %9.3f ms  (min %9.3f ms)  alloc %9.2f MB  [sink=%d]%n",
            name, times[MEASURE / 2] / 1e6, times[0] / 1e6, allocs[MEASURE / 2] / 1e6, sink));
    }

    /**
     * A benchmark made of several phases, each timed and measured separately.
     */
    @FunctionalInterface
    private interface PhasedBody {
        /**
         * Run one iteration, calling {@code phase.end(i)} after finishing phase {@code i}.
         */
        long run(PhaseClock phase) throws Exception;
    }

    private static final class PhaseClock {
        private final long[] times;
        private final long[] allocs;
        private long lastTime;
        private long lastAlloc;

        PhaseClock(int phases) {
            times = new long[phases];
            allocs = new long[phases];
        }

        void start() {
            lastAlloc = allocatedBytes();
            lastTime = System.nanoTime();
        }

        void end(int phase) {
            long now = System.nanoTime();
            long alloc = allocatedBytes();
            times[phase] = now - lastTime;
            allocs[phase] = alloc - lastAlloc;
            // exclude the measurement itself from the next phase as far as possible
            lastAlloc = allocatedBytes();
            lastTime = System.nanoTime();
        }
    }

    private static void benchPhases(String name, String[] phases, int warmup, int measure,
                                    PhasedBody body) throws Exception {
        long sink = 0;
        PhaseClock clock = new PhaseClock(phases.length);
        for (int i = 0; i < warmup; i++) {
            clock.start();
            sink += body.run(clock);
        }
        long[][] times = new long[phases.length + 1][measure];
        long[][] allocs = new long[phases.length + 1][measure];
        for (int i = 0; i < measure; i++) {
            System.gc();
            clock.start();
            sink += body.run(clock);
            for (int p = 0; p < phases.length; p++) {
                times[p][i] = clock.times[p];
                allocs[p][i] = clock.allocs[p];
                times[phases.length][i] += clock.times[p];
                allocs[phases.length][i] += clock.allocs[p];
            }
        }
        for (int p = 0; p <= phases.length; p++) {
            Arrays.sort(times[p]);
            Arrays.sort(allocs[p]);
            String phase = p < phases.length ? phases[p] : "total";
            report(String.format("%-40s median %9.3f ms  (min %9.3f ms)  alloc %9.2f MB  [sink=%d]%n",
                name + " [" + phase + "]", times[p][measure / 2] / 1e6, times[p][0] / 1e6,
                allocs[p][measure / 2] / 1e6, sink));
        }
    }

    private static void report(String line) {
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

    @Test
    void hollowOutRegion() throws Exception {
        when(MOCKED_PLATFORM.getRegistries()).thenReturn(SimpleMaterialRegistries.create());
        BlockState air = new BlockType("benchhollow:air").getDefaultState();
        BlockState solid = new BlockType("benchhollow:stone").getDefaultState();
        BlockState glass = new BlockType("benchhollow:glass").getDefaultState();
        CuboidRegion region = new CuboidRegion(BlockVector3.ZERO, BlockVector3.at(47, 47, 47));
        BlockVector3 center = BlockVector3.at(24, 24, 24);
        bench("EditSession.hollowOutRegion 48^3 sphere", () -> {
            InMemoryWorld world = new InMemoryWorld(air, -64, 319);
            for (BlockVector3 position : region) {
                if (position.distance(center) <= 20) {
                    world.blocks().put(position, solid);
                }
            }
            try (EditSession session = WorldEdit.getInstance().newEditSessionBuilder().world(world.world()).build()) {
                session.hollowOutRegion(region, 2, glass);
            }
            // world reads are the expensive part on a real server
            return world.getBlockReads();
        });
    }

    @Test
    void editSessionSetAndUndo() throws Exception {
        when(MOCKED_PLATFORM.getRegistries()).thenReturn(SimpleMaterialRegistries.create());
        BlockState air = new BlockType("benchset:air").getDefaultState();
        // Real platforms give every state an internal ID, which BlockMap relies on
        BlockStateIdAccess.register(air, BlockStateIdAccess.invalidId());
        if (!BlockStateIdAccess.isValidInternalId(BlockStateIdAccess.getBlockStateId(stone))) {
            BlockStateIdAccess.register(stone, BlockStateIdAccess.invalidId());
        }
        CuboidRegion region = cube(64);
        bench("EditSession set+flush+undo 64^3", () -> {
            InMemoryWorld world = new InMemoryWorld(air, -64, 319);
            EditSession session = WorldEdit.getInstance().newEditSessionBuilder().world(world.world()).build();
            int changed;
            try (session) {
                changed = session.setBlocks(region, stone);
            }
            try (EditSession undo = WorldEdit.getInstance().newEditSessionBuilder().world(world.world()).build()) {
                session.undo(undo);
            }
            return (long) changed + world.blocks().size();
        });
    }

    @Test
    void editSessionSetFlushUndo1M() throws Exception {
        when(MOCKED_PLATFORM.getRegistries()).thenReturn(SimpleMaterialRegistries.create());
        BlockState air = new BlockType("benchcycle:air").getDefaultState();
        // Real platforms give every state an internal ID
        BlockStateIdAccess.register(air, BlockStateIdAccess.invalidId());
        for (BlockState state : palette) {
            if (!BlockStateIdAccess.isValidInternalId(BlockStateIdAccess.getBlockStateId(state))) {
                BlockStateIdAccess.register(state, BlockStateIdAccess.invalidId());
            }
        }
        // 100 * 100 * 100 = 1M blocks
        CuboidRegion region = cube(100);
        Pattern pattern = new Pattern() {
            @Override
            public BaseBlock applyBlock(BlockVector3 pos) {
                return palette[(pos.x() * 31 + pos.y() * 7 + pos.z()) & (palette.length - 1)].toBaseBlock();
            }
        };
        String[] phases = {"set", "flush", "undo"};
        benchPhases("EditSession 1M set/flush/undo", phases, 3, 9, clock -> {
            InMemoryWorld world = new InMemoryWorld(air, -64, 319);
            EditSession session = WorldEdit.getInstance().newEditSessionBuilder().world(world.world()).build();
            int changed;
            try (session) {
                changed = session.setBlocks(region, pattern);
                clock.end(0);
            }
            clock.end(1);
            try (EditSession undo = WorldEdit.getInstance().newEditSessionBuilder().world(world.world()).build()) {
                session.undo(undo);
            }
            clock.end(2);
            assertEquals(region.getVolume(), changed);
            return (long) changed + world.blocks().size();
        });
    }

    @Test
    @SuppressWarnings("deprecation")
    void editSessionRealisticChain1M() throws Exception {
        when(MOCKED_PLATFORM.getRegistries()).thenReturn(SimpleMaterialRegistries.create());
        BlockState air = new BlockType("benchreal:air").getDefaultState();
        BlockStateIdAccess.register(air, BlockStateIdAccess.invalidId());
        for (BlockState state : palette) {
            if (!BlockStateIdAccess.isValidInternalId(BlockStateIdAccess.getBlockStateId(state))) {
                BlockStateIdAccess.register(state, BlockStateIdAccess.invalidId());
            }
        }
        long[] ticks = new long[1];
        Watchdog watchdog = () -> ticks[0]++;
        when(MOCKED_PLATFORM.getWatchdog()).thenReturn(watchdog);
        try {
            // 100 * 100 * 100 = 1M blocks
            CuboidRegion region = cube(100);
            // A mask that every block passes, but that is tested like a user's mask
            Mask mask = new RegionMask(new CuboidRegion(region.getMinimumPoint().subtract(1, 1, 1),
                region.getMaximumPoint().add(1, 1, 1)));
            Pattern pattern = new Pattern() {
                @Override
                public BaseBlock applyBlock(BlockVector3 pos) {
                    return palette[(pos.x() * 31 + pos.y() * 7 + pos.z()) & (palette.length - 1)].toBaseBlock();
                }
            };
            String[] phases = {"set", "flush", "undo"};
            // Configured like LocalSession#createEditSession for a non-op player with a mask
            benchPhases("EditSession realistic chain 1M", phases, 3, 9, clock -> {
                InMemoryWorld world = new InMemoryWorld(air, -64, 319);
                EditSession session = WorldEdit.getInstance().newEditSessionBuilder()
                    .world(world.world()).maxBlocks(2_000_000).build();
                session.setMask(mask);
                session.setSideEffectApplier(SideEffectSet.defaults());
                session.setReorderMode(EditSession.ReorderMode.FAST);
                session.getSurvivalExtent().setStripNbt(true);
                session.setTickingWatchdog(true);
                int changed;
                try (session) {
                    changed = session.setBlocks(region, pattern);
                    clock.end(0);
                }
                clock.end(1);
                try (EditSession undo = WorldEdit.getInstance().newEditSessionBuilder().world(world.world()).build()) {
                    session.undo(undo);
                }
                clock.end(2);
                assertEquals(region.getVolume(), changed);
                return (long) changed + world.blocks().size() + ticks[0];
            });
        } finally {
            when(MOCKED_PLATFORM.getWatchdog()).thenReturn(null);
        }
    }

    @Test
    void editSessionMoveRegion() throws Exception {
        when(MOCKED_PLATFORM.getRegistries()).thenReturn(SimpleMaterialRegistries.create());
        BlockState air = new BlockType("benchmove:air").getDefaultState();
        BlockStateIdAccess.register(air, BlockStateIdAccess.invalidId());
        for (BlockState state : palette) {
            if (!BlockStateIdAccess.isValidInternalId(BlockStateIdAccess.getBlockStateId(state))) {
                BlockStateIdAccess.register(state, BlockStateIdAccess.invalidId());
            }
        }
        CuboidRegion region = cube(64);
        String[] phases = {"move+flush", "undo"};
        benchPhases("EditSession.moveRegion 64^3", phases, 3, 9, clock -> {
            InMemoryWorld world = new InMemoryWorld(air, -64, 319);
            for (BlockVector3 pos : region) {
                world.blocks().put(pos, palette[(pos.x() * 31 + pos.y() * 7 + pos.z()) & (palette.length - 1)]);
            }
            clock.start();
            EditSession session = WorldEdit.getInstance().newEditSessionBuilder().world(world.world()).build();
            int moved;
            try (session) {
                moved = session.moveRegion(region, BlockVector3.at(1, 0, 0), 40, true, null);
            }
            clock.end(0);
            try (EditSession undo = WorldEdit.getInstance().newEditSessionBuilder().world(world.world()).build()) {
                session.undo(undo);
            }
            clock.end(1);
            return (long) moved + world.blocks().size();
        });
    }

    private static long usedHeapAfterGc() {
        long used = Long.MAX_VALUE;
        // a few rounds, as one GC does not always collect everything
        for (int i = 0; i < 3; i++) {
            System.gc();
            used = Math.min(used, ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed());
        }
        return used;
    }

    /**
     * Give the benchmark states internal IDs, as real platforms do.
     */
    private static void registerPaletteIds() {
        for (BlockState state : palette) {
            if (!BlockStateIdAccess.isValidInternalId(BlockStateIdAccess.getBlockStateId(state))) {
                BlockStateIdAccess.register(state, BlockStateIdAccess.invalidId());
            }
        }
        if (!BlockStateIdAccess.isValidInternalId(BlockStateIdAccess.getBlockStateId(stone))) {
            BlockStateIdAccess.register(stone, BlockStateIdAccess.invalidId());
        }
    }

    @Test
    void historyRetainedMemory1M() throws Exception {
        registerPaletteIds();
        CuboidRegion region = cube(100);
        BaseBlock previous = stone.toBaseBlock();
        long[] retained = new long[5];
        for (int round = 0; round < retained.length; round++) {
            long before = usedHeapAfterGc();
            BlockOptimizedHistory history = new BlockOptimizedHistory();
            for (BlockVector3 pos : region) {
                BlockState current = palette[(pos.x() * 31 + pos.y() * 7 + pos.z()) & (palette.length - 1)];
                history.add(new BlockChange(pos, previous, current.toBaseBlock()));
            }
            retained[round] = usedHeapAfterGc() - before;
            // also keeps the history reachable while measuring
            assertEquals(region.getVolume(), history.size());
        }
        Arrays.sort(retained);
        report(String.format("%-40s median retained %9.2f MB%n", "BlockOptimizedHistory 1M changes",
            retained[retained.length / 2] / 1e6));
    }

    @Test
    void blockStateIdLookup() throws Exception {
        // A realistic number of block states, with platform-assigned dense IDs
        int count = 30_000;
        int base = 1_000;
        for (int i = 0; i < count; i++) {
            BlockState state = new BlockType("benchid:block_" + i).getDefaultState();
            BlockStateIdAccess.register(state, base + i);
        }
        int[] lookups = new int[1 << 22];
        java.util.Random random = new java.util.Random(42);
        for (int i = 0; i < lookups.length; i++) {
            lookups[i] = base + random.nextInt(count);
        }
        bench("BlockStateIdAccess.getBlockStateById 4M", () -> {
            long sum = 0;
            for (int id : lookups) {
                sum += System.identityHashCode(BlockStateIdAccess.getBlockStateById(id)) & 1;
            }
            return sum;
        });
    }
}
