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

package com.sk89q.worldedit.internal.edit;

import com.sk89q.worldedit.BaseWorldEditTest;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.EditSessionBuilder;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Watchdog;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.extent.inventory.BlockBagException;
import com.sk89q.worldedit.extent.inventory.OutOfBlocksException;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.SideEffect;
import com.sk89q.worldedit.util.SideEffectSet;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.util.test.SimpleMaterialRegistries;
import com.sk89q.worldedit.world.NullWorld;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import org.enginehub.linbus.tree.LinCompoundTag;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.Invocation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;

/**
 * Pins the exact sequence of calls that every layer of the {@link EditSession}
 * extent chain makes, as seen by plugin extents installed through the
 * {@link EditSessionEvent} and by the {@link World}, for the main chain
 * configurations (buffering modes, masks, block bags, survival mode, side
 * effects, change limits, tracing and watchdog ticking).
 *
 * <p>The expected traces in {@code ExtentChainCallOrderTest.txt} were recorded
 * from the implementation before the per-block overhead of the chain was
 * optimised. Any change to the order, arguments or results of calls that an
 * extent or the world sees fails this test.</p>
 */
@DisplayName("Extent chain call order")
class ExtentChainCallOrderTest extends BaseWorldEditTest {

    private static final BlockVector3 P1 = BlockVector3.at(1, 2, 3);
    private static final BlockVector3 P2 = BlockVector3.at(17, 2, -5);
    private static final BlockVector3 P3 = BlockVector3.at(1, 3, 3);
    private static final BlockVector3 OUT_OF_WORLD = BlockVector3.at(0, 400, 0);

    private static BlockState air;
    private static BlockState stone;
    private static BlockState dirt;
    private static BlockState glass;
    private static BaseBlock chest;
    private static BiomeType plains;
    private static BiomeType ocean;

    @BeforeAll
    static void setUpBlocks() {
        when(MOCKED_PLATFORM.getRegistries()).thenReturn(SimpleMaterialRegistries.create());
        air = new BlockType("ordertest:air").getDefaultState();
        stone = new BlockType("ordertest:stone").getDefaultState();
        dirt = new BlockType("ordertest:dirt").getDefaultState();
        glass = new BlockType("ordertest:glass").getDefaultState();
        chest = new BlockType("ordertest:chest").getDefaultState()
            .toBaseBlock(LinCompoundTag.builder().putString("id", "ordertest:chest").build());
        plains = new BiomeType("ordertest:plains");
        ocean = new BiomeType("ordertest:ocean");
    }

    private static String describe(BlockStateHolder<?> block) {
        String kind = block instanceof BaseBlock base
            ? (base.getNbtReference() != null ? "B+nbt" : "B")
            : "S";
        return block.getBlockType().id() + "/" + kind;
    }

    private static String describe(BlockVector3 position) {
        return position.x() + "," + position.y() + "," + position.z();
    }

    private static String describe(SideEffectSet set) {
        return new TreeSet<>(set.getSideEffectsToApply().stream().map(SideEffect::name).toList()).toString();
    }

    /**
     * A world that records every call that reaches it.
     */
    private static final class RecordingWorld extends NullWorld {
        private final List<String> log;
        private final Map<BlockVector3, BaseBlock> blocks = new HashMap<>();
        private final Map<BlockVector3, BiomeType> biomes = new HashMap<>();

        RecordingWorld(List<String> log) {
            this.log = log;
            blocks.put(P3, glass.toBaseBlock());
        }

        @Override
        public String id() {
            return "recording";
        }

        @Override
        public int getMinY() {
            return -64;
        }

        @Override
        public int getMaxY() {
            return 319;
        }

        @Override
        public BlockState getBlock(BlockVector3 position) {
            BlockState block = getFullBlock0(position).toImmutableState();
            log.add("W getBlock " + describe(position) + " -> " + describe(block));
            return block;
        }

        @Override
        public BaseBlock getFullBlock(BlockVector3 position) {
            BaseBlock block = getFullBlock0(position);
            log.add("W getFullBlock " + describe(position) + " -> " + describe(block));
            return block;
        }

        private BaseBlock getFullBlock0(BlockVector3 position) {
            return blocks.getOrDefault(position, air.toBaseBlock());
        }

        @Override
        public <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 position, B block, SideEffectSet sideEffects) {
            log.add("W setBlock " + describe(position) + " " + describe(block) + " " + describe(sideEffects));
            blocks.put(position, block.toBaseBlock());
            return true;
        }

        @Override
        public Set<SideEffect> applySideEffects(BlockVector3 position, BlockState previousType, SideEffectSet sideEffectSet) {
            log.add("W applySideEffects " + describe(position) + " " + describe(previousType) + " " + describe(sideEffectSet));
            return Set.of();
        }

        @Override
        public void simulateBlockMine(BlockVector3 position) {
            log.add("W simulateBlockMine " + describe(position));
            blocks.remove(position);
        }

        @Override
        public void checkLoadedChunk(BlockVector3 position) {
            log.add("W checkLoadedChunk " + describe(position));
        }

        @Override
        public BiomeType getBiome(BlockVector3 position) {
            BiomeType biome = biomes.getOrDefault(position, ocean);
            log.add("W getBiome " + describe(position) + " -> " + biome);
            return biome;
        }

        @Override
        public boolean setBiome(BlockVector3 position, BiomeType biome) {
            log.add("W setBiome " + describe(position) + " " + biome.id());
            biomes.put(position, biome);
            return true;
        }

        @Override
        public void sendBiomeUpdates(Iterable<BlockVector2> chunks) {
            List<String> list = new ArrayList<>();
            chunks.forEach(c -> list.add(c.x() + "," + c.z()));
            log.add("W sendBiomeUpdates " + list);
        }

        Map<String, String> contents() {
            Map<String, String> result = new TreeMap<>();
            blocks.forEach((pos, block) -> result.put(describe(pos), describe(block)));
            return result;
        }
    }

    /**
     * A plugin-style extent that records every call made through it.
     */
    private static final class RecordingExtent extends AbstractDelegateExtent {
        private final String name;
        private final List<String> log;

        RecordingExtent(Extent extent, String name, List<String> log) {
            super(extent);
            this.name = name;
            this.log = log;
        }

        @Override
        public BlockState getBlock(BlockVector3 position) {
            log.add(name + " getBlock " + describe(position));
            BlockState block = super.getBlock(position);
            log.add(name + " getBlock " + describe(position) + " -> " + describe(block));
            return block;
        }

        @Override
        public BaseBlock getFullBlock(BlockVector3 position) {
            log.add(name + " getFullBlock " + describe(position));
            BaseBlock block = super.getFullBlock(position);
            log.add(name + " getFullBlock " + describe(position) + " -> " + describe(block));
            return block;
        }

        @Override
        public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 location, T block) throws WorldEditException {
            log.add(name + " setBlock " + describe(location) + " " + describe(block));
            boolean result = super.setBlock(location, block);
            log.add(name + " setBlock " + describe(location) + " -> " + result);
            return result;
        }

        @Override
        public BiomeType getBiome(BlockVector3 position) {
            log.add(name + " getBiome " + describe(position));
            return super.getBiome(position);
        }

        @Override
        public boolean setBiome(BlockVector3 position, BiomeType biome) {
            log.add(name + " setBiome " + describe(position) + " " + biome.id());
            boolean result = super.setBiome(position, biome);
            log.add(name + " setBiome " + describe(position) + " -> " + result);
            return result;
        }
    }

    /**
     * Wraps every stage of sessions on the recording world, as a plugin would.
     */
    public static final class Wrapper {
        private final World world;
        private final List<String> log;

        Wrapper(World world, List<String> log) {
            this.world = world;
            this.log = log;
        }

        @Subscribe
        public void onEditSession(EditSessionEvent event) {
            if (event.getWorld() != world) {
                return;
            }
            String name = switch (event.getStage()) {
                case BEFORE_HISTORY -> "H";
                case BEFORE_REORDER -> "R";
                case BEFORE_CHANGE -> "C";
            };
            log.add("event " + event.getStage() + " extent=" + event.getExtent().getClass().getSimpleName());
            event.setExtent(new RecordingExtent(event.getExtent(), name, log));
        }
    }

    /**
     * A block bag with one stone, that records what it is asked for.
     */
    private static final class RecordingBlockBag extends BlockBag {
        private final List<String> log;
        private int stones = 1;

        RecordingBlockBag(List<String> log) {
            this.log = log;
        }

        @Override
        public void fetchBlock(BlockState blockState) throws BlockBagException {
            log.add("bag fetch " + describe(blockState));
            if (blockState.getBlockType() == stone.getBlockType() && stones > 0) {
                stones--;
                return;
            }
            throw new OutOfBlocksException();
        }

        @Override
        public void storeBlock(BlockState blockState, int amount) {
            log.add("bag store " + describe(blockState) + " x" + amount);
        }

        @Override
        public void flushChanges() {
            log.add("bag flush");
        }

        @Override
        public void addSourcePosition(Location pos) {
        }

        @Override
        public void addSingleSourcePosition(Location pos) {
        }
    }

    @FunctionalInterface
    private interface Script {
        void run(EditSession session, List<String> log) throws Exception;
    }

    @FunctionalInterface
    private interface Configure {
        void configure(EditSessionBuilder builder, List<String> log);
    }

    private static String trace(Configure configure, Script script) throws Exception {
        List<String> log = new ArrayList<>();
        RecordingWorld world = new RecordingWorld(log);
        Wrapper wrapper = new Wrapper(world, log);
        WorldEdit.getInstance().getEventBus().register(wrapper);
        EditSession session;
        try {
            EditSessionBuilder builder = WorldEdit.getInstance().newEditSessionBuilder().world(world);
            configure.configure(builder, log);
            session = builder.build();
            log.add("-- script");
            try (session) {
                script.run(session, log);
                log.add("-- close");
            }
            log.add("changeSet size=" + session.getChangeSet().size()
                + " blockChangeCount=" + session.getBlockChangeCount());
            log.add("world " + world.contents());
            log.add("-- undo");
            try (EditSession undo = WorldEdit.getInstance().newEditSessionBuilder().world(world).build()) {
                session.undo(undo);
            }
            log.add("world " + world.contents());
        } finally {
            WorldEdit.getInstance().getEventBus().unregister(wrapper);
        }
        return String.join("\n", log) + "\n";
    }

    private static String trace(Script script) throws Exception {
        return trace((_, _) -> {
        }, script);
    }

    private static void set(EditSession session, List<String> log, BlockVector3 pos, BlockStateHolder<?> block)
        throws WorldEditException {
        log.add("> setBlock " + describe(pos) + " " + describe(block));
        log.add("< " + setUnchecked(session, pos, block));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean setUnchecked(EditSession session, BlockVector3 pos, BlockStateHolder block)
        throws WorldEditException {
        return session.setBlock(pos, block);
    }

    /**
     * The common edit: new blocks, an overwrite, a removal, buffered reads and a biome.
     */
    private static void standardEdit(EditSession session, List<String> log) throws Exception {
        set(session, log, P1, stone);
        set(session, log, P2, dirt.toBaseBlock());
        set(session, log, P1, dirt);
        set(session, log, P3, air);
        set(session, log, OUT_OF_WORLD, stone);
        log.add("> getBlockWithBuffer " + describe(P1));
        log.add("< " + describe(session.getBlockWithBuffer(P1)));
        log.add("> getFullBlockWithBuffer " + describe(P2));
        log.add("< " + describe(session.getFullBlockWithBuffer(P2)));
        log.add("> setBiome " + describe(P1));
        log.add("< " + session.setBiome(P1, plains));
    }

    private static String missing(EditSession session) {
        Map<String, Integer> missing = new TreeMap<>();
        session.popMissingBlocks().forEach((type, count) -> missing.put(type.id(), count));
        return missing.toString();
    }

    private static Map<String, String> expected;

    private static synchronized String expected(String name) throws IOException {
        if (expected == null) {
            Map<String, String> sections = new LinkedHashMap<>();
            String text;
            try (InputStream in = ExtentChainCallOrderTest.class.getResourceAsStream("ExtentChainCallOrderTest.txt")) {
                assertNotNull(in, "missing expected traces");
                text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            String current = null;
            StringBuilder body = new StringBuilder();
            for (String line : text.split("\n", -1)) {
                if (line.startsWith("### ")) {
                    if (current != null) {
                        sections.put(current, body.toString());
                    }
                    current = line.substring(4);
                    body.setLength(0);
                } else if (current != null && !line.isEmpty()) {
                    body.append(line).append('\n');
                }
            }
            if (current != null) {
                sections.put(current, body.toString());
            }
            expected = sections;
        }
        String section = expected.get(name);
        assertNotNull(section, "no expected trace for " + name);
        return section;
    }

    private static void check(String name, String actual) throws IOException {
        String update = System.getenv("WORLDEDIT_UPDATE_TRACES");
        if (update != null && !update.isEmpty()) {
            try {
                java.nio.file.Files.writeString(java.nio.file.Path.of(update), "### " + name + "\n" + actual + "\n",
                    StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
            } catch (IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
        }
        assertEquals(expected(name), actual, name);
    }

    @Test
    void defaultChain() throws Exception {
        check("defaultChain", trace(ExtentChainCallOrderTest::standardEdit));
    }

    @Test
    @SuppressWarnings("deprecation")
    void multiStageReorder() throws Exception {
        check("multiStageReorder", trace((session, log) -> {
            session.setReorderMode(EditSession.ReorderMode.MULTI_STAGE);
            standardEdit(session, log);
        }));
    }

    @Test
    @SuppressWarnings("deprecation")
    void noReorderWithChunkBatching() throws Exception {
        check("noReorderWithChunkBatching", trace((session, log) -> {
            session.setReorderMode(EditSession.ReorderMode.NONE);
            session.setBatchingChunks(true);
            standardEdit(session, log);
        }));
    }

    @Test
    void noBuffering() throws Exception {
        check("noBuffering", trace((session, log) -> {
            session.disableBuffering();
            standardEdit(session, log);
        }));
    }

    @Test
    void maskSetThenCleared() throws Exception {
        check("maskSetThenCleared", trace((session, log) -> {
            Mask mask = pos -> {
                log.add("mask test " + describe(pos));
                return !pos.equals(P2);
            };
            session.setMask(mask);
            standardEdit(session, log);
            session.setMask(null);
            set(session, log, P2, glass);
        }));
    }

    @Test
    @SuppressWarnings("deprecation")
    void blockBag() throws Exception {
        check("blockBag", trace((builder, log) -> builder.blockBag(new RecordingBlockBag(log)), (session, log) -> {
            standardEdit(session, log);
            log.add("-- flush");
            session.flushSession();
            log.add("missing " + missing(session));
            session.setBlockBag(null);
            set(session, log, P2, glass);
        }));
    }

    @Test
    void blockBagUnbuffered() throws Exception {
        check("blockBagUnbuffered", trace((builder, log) -> builder.blockBag(new RecordingBlockBag(log)), (session, log) -> {
            session.disableBuffering();
            standardEdit(session, log);
            log.add("missing " + missing(session));
            session.setBlockBag(null);
            set(session, log, P2, glass);
        }));
    }

    @Test
    void blockBagSetLater() throws Exception {
        check("blockBagSetLater", trace((session, log) -> {
            set(session, log, P2, glass);
            session.setBlockBag(new RecordingBlockBag(log));
            standardEdit(session, log);
            log.add("missing " + missing(session));
        }));
    }

    @Test
    void survivalToolUseAndStripNbt() throws Exception {
        check("survivalToolUseAndStripNbt", trace((session, log) -> {
            session.disableBuffering();
            session.getSurvivalExtent().setToolUse(true);
            session.getSurvivalExtent().setStripNbt(true);
            set(session, log, P2, chest);
            standardEdit(session, log);
            session.getSurvivalExtent().setToolUse(false);
            session.getSurvivalExtent().setStripNbt(false);
            set(session, log, P1, chest);
            set(session, log, P1, air);
        }));
    }

    @Test
    void survivalStripNbtBuffered() throws Exception {
        check("survivalStripNbtBuffered", trace((session, log) -> {
            session.getSurvivalExtent().setStripNbt(true);
            set(session, log, P2, chest);
            set(session, log, P1, chest);
        }));
    }

    @Test
    @SuppressWarnings("deprecation")
    void sideEffects() throws Exception {
        check("sideEffects", trace((session, log) -> {
            session.setSideEffectApplier(SideEffectSet.none().with(SideEffect.LIGHTING, SideEffect.State.ON));
            standardEdit(session, log);
            session.flushSession();
            session.setReorderMode(EditSession.ReorderMode.NONE);
            session.setSideEffectApplier(SideEffectSet.none());
            set(session, log, P2, glass);
            session.setFastMode(false);
            set(session, log, P3, glass);
        }));
    }

    @Test
    void stagesAndBypasses() throws Exception {
        check("stagesAndBypasses", trace((session, log) -> {
            log.add("> smartSetBlock");
            log.add("< " + session.smartSetBlock(P1, stone));
            log.add("> rawSetBlock");
            log.add("< " + session.rawSetBlock(P2, dirt));
            log.add("> setBlock BEFORE_REORDER");
            log.add("< " + session.setBlock(P3, glass, EditSession.Stage.BEFORE_REORDER));
            log.add("> setBlock BEFORE_HISTORY out of world");
            log.add("< " + session.setBlock(OUT_OF_WORLD, glass, EditSession.Stage.BEFORE_HISTORY));
            log.add("> getBiome");
            log.add("< " + session.getBiome(P1));
        }));
    }

    @Test
    void changeLimit() throws Exception {
        check("changeLimit", trace((builder, _) -> builder.maxBlocks(2), (session, log) -> {
            set(session, log, P1, stone);
            set(session, log, P2, stone);
            try {
                set(session, log, P3, stone);
            } catch (MaxChangedBlocksException e) {
                log.add("! MaxChangedBlocksException " + e.getBlockLimit());
            }
            session.setBlockChangeLimit(-1);
            set(session, log, P3, stone);
        }));
    }

    @Test
    void changeSetDisabled() throws Exception {
        check("changeSetDisabled", trace((session, log) -> {
            set(session, log, P1, stone);
            session.getChangeSet().setRecordChanges(false);
            set(session, log, P2, stone);
        }));
    }

    @Test
    void tracing() throws Exception {
        Actor actor = mock(Actor.class);
        when(actor.getName()).thenReturn("tracer");
        String trace = trace((builder, _) -> builder.actor(actor).tracing(true), (session, log) -> {
            session.setMask(pos -> !pos.equals(P2));
            standardEdit(session, log);
        });
        // The report is not ordered, so sort it
        Set<String> messages = new TreeSet<>();
        for (Invocation invocation : mockingDetails(actor).getInvocations()) {
            StringBuilder message = new StringBuilder("actor ").append(invocation.getMethod().getName());
            for (Object argument : invocation.getArguments()) {
                message.append(' ').append(argument);
            }
            messages.add(message.append('\n').toString());
        }
        check("tracing", trace + String.join("", messages));
    }

    @Test
    @SuppressWarnings("deprecation")
    void watchdog() throws Exception {
        int[] ticks = new int[1];
        Watchdog watchdog = () -> ticks[0]++;
        when(MOCKED_PLATFORM.getWatchdog()).thenReturn(watchdog);
        try {
            check("watchdog", trace((session, log) -> {
                session.setTickingWatchdog(true);
                for (int i = 0; i < 260; i++) {
                    session.setBlock(BlockVector3.at(i, 0, 0), stone);
                }
                session.getBlockWithBuffer(P1);
                log.add("ticks " + ticks[0]);
                session.flushSession();
                log.add("ticks " + ticks[0]);
                session.setTickingWatchdog(false);
                set(session, log, P1, dirt);
                session.flushSession();
                log.add("ticks " + ticks[0]);
            }).lines().filter(l -> !l.startsWith("W ") && !l.matches("[HRC] .*")).reduce("", (a, b) -> a + b + "\n"));
        } finally {
            when(MOCKED_PLATFORM.getWatchdog()).thenReturn(null);
        }
    }
}
