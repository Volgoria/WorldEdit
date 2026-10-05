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

package com.sk89q.worldedit;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.InlineMe;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extension.platform.Watchdog;
import com.sk89q.worldedit.extent.ChangeSetExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.InputExtent;
import com.sk89q.worldedit.extent.MaskingExtent;
import com.sk89q.worldedit.extent.NullExtent;
import com.sk89q.worldedit.extent.TracingExtent;
import com.sk89q.worldedit.extent.buffer.internal.BatchingExtent;
import com.sk89q.worldedit.extent.cache.LastAccessExtentCache;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.extent.inventory.BlockBagExtent;
import com.sk89q.worldedit.extent.reorder.ChunkBatchingExtent;
import com.sk89q.worldedit.extent.reorder.MultiStageReorder;
import com.sk89q.worldedit.extent.validation.BlockChangeLimiter;
import com.sk89q.worldedit.extent.validation.DataValidatorExtent;
import com.sk89q.worldedit.extent.world.ChunkLoadingExtent;
import com.sk89q.worldedit.extent.world.SideEffectExtent;
import com.sk89q.worldedit.extent.world.SurvivalModeExtent;
import com.sk89q.worldedit.extent.world.WatchdogTickingExtent;
import com.sk89q.worldedit.function.RegionMaskingFilter;
import com.sk89q.worldedit.function.block.BlockDistributionCounter;
import com.sk89q.worldedit.function.block.Counter;
import com.sk89q.worldedit.function.mask.BlockMask;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.Masks;
import com.sk89q.worldedit.function.operation.ChangeSetExecutor;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.visitor.RegionVisitor;
import com.sk89q.worldedit.history.UndoContext;
import com.sk89q.worldedit.history.changeset.BlockOptimizedHistory;
import com.sk89q.worldedit.history.changeset.ChangeSet;
import com.sk89q.worldedit.internal.edit.ExpressionOperations;
import com.sk89q.worldedit.internal.edit.LineGenerator;
import com.sk89q.worldedit.internal.edit.MorphologyOperations;
import com.sk89q.worldedit.internal.edit.RegionCopyOperations;
import com.sk89q.worldedit.internal.edit.RegionOperations;
import com.sk89q.worldedit.internal.edit.ShapeGenerator;
import com.sk89q.worldedit.internal.edit.TerrainOperations;
import com.sk89q.worldedit.internal.expression.Expression;
import com.sk89q.worldedit.internal.expression.ExpressionException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector2;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.transform.ScaleAndTranslateTransform;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.CylinderRegion;
import com.sk89q.worldedit.regions.FlatRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionOperationException;
import com.sk89q.worldedit.util.Countable;
import com.sk89q.worldedit.util.SideEffectSet;
import com.sk89q.worldedit.util.TreeGenerator;
import com.sk89q.worldedit.util.collection.BlockMap;
import com.sk89q.worldedit.util.eventbus.EventBus;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;
import com.sk89q.worldedit.world.NullWorld;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.generation.TreeType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.sk89q.worldedit.internal.util.SwitchEnhancements.dummyValue;
import static com.sk89q.worldedit.internal.util.SwitchEnhancements.exhaustive;

/**
 * An {@link Extent} that handles history, {@link BlockBag}s, change limits,
 * block re-ordering, and much more. Most operations in WorldEdit use this class.
 *
 * <p>Most of the actual functionality is implemented with a number of other
 * {@link Extent}s that are chained together. For example, history is logged
 * using the {@link ChangeSetExtent}.</p>
 */
@SuppressWarnings({"FieldCanBeLocal"})
public class EditSession implements Extent, AutoCloseable {

    /**
     * Used by {@link EditSession#setBlock(BlockVector3, BlockStateHolder, Stage)} to
     * determine which {@link Extent}s should be bypassed.
     */
    public enum Stage {
        BEFORE_HISTORY,
        BEFORE_REORDER,
        BEFORE_CHANGE
    }

    /**
     * Reorder mode for {@link EditSession#setReorderMode(ReorderMode)}.
     *
     * <p>
     * MULTI_STAGE = Multi stage reorder, may not be great with mods.
     * FAST = Use the fast mode. Good for mods.
     * NONE = Place blocks without worrying about placement order.
     * </p>
     *
     * @deprecated Only "fast" will be implemented in the future, as it works flawlessly now.
     */
    @Deprecated
    public enum ReorderMode {
        MULTI_STAGE("multi"),
        FAST("fast"),
        NONE("none");

        private final String displayName;

        ReorderMode(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return this.displayName;
        }
    }

    @SuppressWarnings("ProtectedField")
    protected final World world;
    private final @Nullable Actor actor;
    private final ChangeSet changeSet = new BlockOptimizedHistory();

    private @Nullable SideEffectExtent sideEffectExtent;
    private final SurvivalModeExtent survivalExtent;
    private @Nullable BatchingExtent batchingExtent;
    private @Nullable ChunkBatchingExtent chunkBatchingExtent;
    private final BlockBagExtent blockBagExtent;
    @SuppressWarnings("deprecation")
    private final MultiStageReorder reorderExtent;
    private final MaskingExtent maskingExtent;
    private final BlockChangeLimiter changeLimiter;
    private @Nullable ChangeSetExtent changeSetExtent;
    private final List<WatchdogTickingExtent> watchdogExtents = new ArrayList<>(2);

    private final Extent bypassReorderHistory;
    private final Extent bypassHistory;
    private final Extent bypassNone;

    private final @Nullable List<TracingExtent> tracingExtents;

    @Deprecated
    private ReorderMode reorderMode = ReorderMode.FAST;

    private Mask oldMask;

    /**
     * Construct the object with a maximum number of blocks and a block bag.
     *
     * @param eventBus the event bus
     * @param world the world
     * @param maxBlocks the maximum number of blocks that can be changed, or -1 to use no limit
     * @param blockBag an optional {@link BlockBag} to use, otherwise null
     * @param actor the actor that owns the session
     * @param tracing if tracing is enabled. An actor is required if this is {@code true}
     */
    // Suppressing AssignmentExpression: This provides clarity in the way that we use it.
    @SuppressWarnings("AssignmentExpression")
    EditSession(EventBus eventBus, World world, int maxBlocks, @Nullable BlockBag blockBag,
                @Nullable Actor actor,
                boolean tracing) {
        checkNotNull(eventBus);
        checkArgument(maxBlocks >= -1, "maxBlocks >= -1 required");

        if (tracing) {
            this.tracingExtents = new ArrayList<>();
            checkNotNull(actor, "An actor is required while tracing");
        } else {
            this.tracingExtents = null;
        }

        this.world = world;
        this.actor = actor;

        if (world != null) {
            EditSessionEvent event = new EditSessionEvent(world, actor, maxBlocks, null);
            Watchdog watchdog = WorldEdit.getInstance().getPlatformManager()
                .queryCapability(Capability.GAME_HOOKS).getWatchdog();
            Extent extent;

            // These extents are ALWAYS used
            extent = traceIfNeeded(sideEffectExtent = new SideEffectExtent(world));
            if (watchdog != null) {
                // Reset watchdog before world placement
                WatchdogTickingExtent watchdogExtent = new WatchdogTickingExtent(extent, watchdog);
                extent = traceIfNeeded(watchdogExtent);
                watchdogExtents.add(watchdogExtent);
            }
            extent = traceIfNeeded(survivalExtent = new SurvivalModeExtent(extent, world));
            extent = traceIfNeeded(new ChunkLoadingExtent(extent, world));
            extent = traceIfNeeded(new LastAccessExtentCache(extent));
            extent = traceIfNeeded(blockBagExtent = new BlockBagExtent(extent, blockBag));
            extent = wrapExtent(extent, eventBus, event, Stage.BEFORE_CHANGE);
            this.bypassReorderHistory = traceIfNeeded(new DataValidatorExtent(extent, world));

            // This extent can be skipped by calling rawSetBlock()
            extent = traceIfNeeded(batchingExtent = new BatchingExtent(extent));
            @SuppressWarnings("deprecation")
            MultiStageReorder reorder = new MultiStageReorder(extent, false);
            extent = traceIfNeeded(reorderExtent = reorder);
            extent = traceIfNeeded(chunkBatchingExtent = new ChunkBatchingExtent(extent, false));
            extent = wrapExtent(extent, eventBus, event, Stage.BEFORE_REORDER);
            if (watchdog != null) {
                // reset before buffering extents, since they may buffer all changes
                // before the world-placement reset can happen, and still cause halts
                WatchdogTickingExtent watchdogExtent = new WatchdogTickingExtent(extent, watchdog);
                extent = traceIfNeeded(watchdogExtent);
                watchdogExtents.add(watchdogExtent);
            }
            this.bypassHistory = traceIfNeeded(new DataValidatorExtent(extent, world));

            // These extents can be skipped by calling smartSetBlock()
            extent = traceIfNeeded(changeSetExtent = new ChangeSetExtent(extent, changeSet));
            extent = traceIfNeeded(maskingExtent = new MaskingExtent(extent, Masks.alwaysTrue()));
            extent = traceIfNeeded(changeLimiter = new BlockChangeLimiter(extent, maxBlocks));
            extent = wrapExtent(extent, eventBus, event, Stage.BEFORE_HISTORY);
            this.bypassNone = traceIfNeeded(new DataValidatorExtent(extent, world));
        } else {
            Extent extent = new NullExtent();
            extent = traceIfNeeded(survivalExtent = new SurvivalModeExtent(extent, NullWorld.getInstance()));
            extent = traceIfNeeded(blockBagExtent = new BlockBagExtent(extent, blockBag));
            @SuppressWarnings("deprecation")
            MultiStageReorder reorder = new MultiStageReorder(extent, false);
            extent = traceIfNeeded(reorderExtent = reorder);
            extent = traceIfNeeded(maskingExtent = new MaskingExtent(extent, Masks.alwaysTrue()));
            extent = traceIfNeeded(changeLimiter = new BlockChangeLimiter(extent, maxBlocks));
            this.bypassReorderHistory = extent;
            this.bypassHistory = extent;
            this.bypassNone = extent;
        }

        setReorderMode(this.reorderMode);
    }

    private Extent traceIfNeeded(Extent input) {
        Extent output = input;
        if (tracingExtents != null) {
            TracingExtent newExtent = new TracingExtent(input);
            output = newExtent;
            tracingExtents.add(newExtent);
        }
        return output;
    }

    private Extent wrapExtent(Extent extent, EventBus eventBus, EditSessionEvent event, Stage stage) {
        // NB: the event does its own tracing
        event = event.clone(stage);
        event.setExtent(extent);
        boolean tracing = tracingExtents != null;
        event.setTracing(tracing);
        eventBus.post(event);
        if (tracing) {
            tracingExtents.addAll(event.getTracingExtents());
        }
        return event.getExtent();
    }

    private boolean commitRequired() {
        if (reorderExtent != null && reorderExtent.commitRequired()) {
            return true;
        }
        if (chunkBatchingExtent != null && chunkBatchingExtent.commitRequired()) {
            return true;
        }
        if (sideEffectExtent != null && sideEffectExtent.commitRequired()) {
            return true;
        }
        return false;
    }

    /**
     * Get the current list of active tracing extents.
     */
    private List<TracingExtent> getActiveTracingExtents() {
        if (tracingExtents == null) {
            return ImmutableList.of();
        }
        return tracingExtents.stream()
            .filter(TracingExtent::isActive)
            .toList();
    }

    /**
     * Turns on specific features for a normal WorldEdit session, such as
     * {@link #setBatchingChunks(boolean)
     * chunk batching}.
     */
    public void enableStandardMode() {
    }

    /**
     * Sets the {@link ReorderMode} of this EditSession, and flushes the session.
     *
     * @param reorderMode The reorder mode
     * @deprecated See {@link ReorderMode} for details.
     */
    @Deprecated
    public void setReorderMode(ReorderMode reorderMode) {
        if (world == null && reorderMode == ReorderMode.FAST) {
            // Fast requires a world, for now we can fallback to multi stage, but use "none" in the future.
            reorderMode = ReorderMode.MULTI_STAGE;
        }
        if (reorderMode == ReorderMode.FAST && sideEffectExtent == null) {
            throw new IllegalArgumentException("An EditSession without a fast mode tried to use it for reordering!");
        }
        if (reorderMode == ReorderMode.MULTI_STAGE && reorderExtent == null) {
            throw new IllegalArgumentException("An EditSession without a reorder extent tried to use it for reordering!");
        }
        if (commitRequired()) {
            internalFlushSession();
        }

        this.reorderMode = reorderMode;
        exhaustive(switch (reorderMode) {
            case MULTI_STAGE -> {
                if (sideEffectExtent != null) {
                    sideEffectExtent.setPostEditSimulationEnabled(false);
                }
                reorderExtent.setEnabled(true);
                yield dummyValue();
            }
            case FAST -> {
                sideEffectExtent.setPostEditSimulationEnabled(true);
                if (reorderExtent != null) {
                    reorderExtent.setEnabled(false);
                }
                yield dummyValue();
            }
            case NONE -> {
                if (sideEffectExtent != null) {
                    sideEffectExtent.setPostEditSimulationEnabled(false);
                }
                if (reorderExtent != null) {
                    reorderExtent.setEnabled(false);
                }
                yield dummyValue();
            }
        });
    }

    /**
     * Get the reorder mode.
     *
     * @return the reorder mode
     * @deprecated See {@link ReorderMode} for details.
     */
    @Deprecated
    public ReorderMode getReorderMode() {
        return reorderMode;
    }

    /**
     * Get the world.
     *
     * @return the world
     */
    public World getWorld() {
        return world;
    }

    /**
     * Get the underlying {@link ChangeSet}.
     *
     * @return the change set
     */
    public ChangeSet getChangeSet() {
        return changeSet;
    }

    /**
     * Get the maximum number of blocks that can be changed. -1 will be returned
     * if it the limit disabled.
     *
     * @return the limit (&gt;= 0) or -1 for no limit
     */
    public int getBlockChangeLimit() {
        return changeLimiter.getLimit();
    }

    /**
     * Set the maximum number of blocks that can be changed.
     *
     * @param limit the limit (&gt;= 0) or -1 for no limit
     */
    public void setBlockChangeLimit(int limit) {
        changeLimiter.setLimit(limit);
    }

    /**
     * Returns queue status.
     *
     * @return whether the queue is enabled
     * @deprecated Use {@link EditSession#isBufferingEnabled()} instead.
     */
    @Deprecated
    public boolean isQueueEnabled() {
        return reorderMode == ReorderMode.MULTI_STAGE && reorderExtent.isEnabled();
    }

    /**
     * Queue certain types of block for better reproduction of those blocks.
     *
     * @deprecated There is no specific replacement, instead enable what you want specifically.
     */
    @Deprecated
    public void enableQueue() {
        setReorderMode(ReorderMode.MULTI_STAGE);
    }

    /**
     * Disable the queue. This will flush the session.
     *
     * @deprecated Use {@link EditSession#disableBuffering()} instead.
     */
    @Deprecated
    public void disableQueue() {
        if (isQueueEnabled()) {
            internalFlushSession();
        }
        setReorderMode(ReorderMode.NONE);
    }

    /**
     * Get the mask.
     *
     * @return mask, may be null
     */
    public Mask getMask() {
        return oldMask;
    }

    /**
     * Set a mask.
     *
     * @param mask mask or null
     */
    public void setMask(Mask mask) {
        this.oldMask = mask;
        if (mask == null) {
            maskingExtent.setMask(Masks.alwaysTrue());
        } else {
            maskingExtent.setMask(mask);
        }
    }

    /**
     * Get the {@link SurvivalModeExtent}.
     *
     * @return the survival simulation extent
     */
    public SurvivalModeExtent getSurvivalExtent() {
        return survivalExtent;
    }

    /**
     * Set whether fast mode is enabled.
     *
     * <p>Fast mode may skip lighting checks or adjacent block
     * notification.</p>
     *
     * @param enabled true to enable
     * @deprecated Prefer to pick specific side-effects to enable or disable and call
     *     {@link #setSideEffectApplier(SideEffectSet)}.
     */
    @Deprecated
    public void setFastMode(boolean enabled) {
        if (sideEffectExtent != null) {
            sideEffectExtent.setSideEffectSet(enabled ? SideEffectSet.defaults() : SideEffectSet.none());
        }
    }

    /**
     * Set which block updates should occur.
     *
     * @param sideEffectSet side effects to enable
     */
    public void setSideEffectApplier(SideEffectSet sideEffectSet) {
        if (sideEffectExtent != null) {
            sideEffectExtent.setSideEffectSet(sideEffectSet);
        }
    }

    /**
     * Return fast mode status.
     *
     * <p>Fast mode may skip lighting checks or adjacent block
     * notification.</p>
     *
     * @return true if enabled
     * @deprecated Prefer to query {@link #getSideEffectApplier()} and check specific side-effects.
     */
    @Deprecated
    public boolean hasFastMode() {
        return sideEffectExtent != null && !this.sideEffectExtent.getSideEffectSet().doesApplyAny();
    }

    public SideEffectSet getSideEffectApplier() {
        if (sideEffectExtent == null) {
            return SideEffectSet.defaults();
        }
        return sideEffectExtent.getSideEffectSet();
    }

    /**
     * Get the {@link BlockBag} is used.
     *
     * @return a block bag or null
     */
    public BlockBag getBlockBag() {
        return blockBagExtent.getBlockBag();
    }

    /**
     * Set a {@link BlockBag} to use.
     *
     * @param blockBag the block bag to set, or null to use none
     */
    public void setBlockBag(BlockBag blockBag) {
        blockBagExtent.setBlockBag(blockBag);
    }

    /**
     * Gets the list of missing blocks and clears the list for the next
     * operation.
     *
     * @return a map of missing blocks
     */
    public Map<BlockType, Integer> popMissingBlocks() {
        return blockBagExtent.popMissing();
    }

    /**
     * Returns chunk batching status.
     *
     * @return whether chunk batching is enabled
     */
    public boolean isBatchingChunks() {
        return chunkBatchingExtent != null && chunkBatchingExtent.isEnabled();
    }

    /**
     * Enable or disable chunk batching. Disabling will flush the session.
     *
     * @param batchingChunks {@code true} to enable, {@code false} to disable
     */
    public void setBatchingChunks(boolean batchingChunks) {
        if (chunkBatchingExtent == null) {
            if (batchingChunks) {
                throw new UnsupportedOperationException("Chunk batching not supported by this session.");
            }
            return;
        }
        assert batchingExtent != null : "same nullness as chunkBatchingExtent";
        if (!batchingChunks && isBatchingChunks()) {
            internalFlushSession();
        }
        chunkBatchingExtent.setEnabled(batchingChunks);
        batchingExtent.setEnabled(!batchingChunks);
    }

    /**
     * Check if this session has any buffering extents enabled.
     *
     * @return {@code true} if any extents are buffering
     */
    public boolean isBufferingEnabled() {
        return isBatchingChunks() || (sideEffectExtent != null && sideEffectExtent.isPostEditSimulationEnabled());
    }

    /**
     * Disable all buffering extents.
     *
     * @see #setReorderMode(ReorderMode)
     * @see #setBatchingChunks(boolean)
     */
    public void disableBuffering() {
        // We optimize here to avoid repeated calls to flushSession.
        if (commitRequired()) {
            internalFlushSession();
        }
        if (sideEffectExtent != null) {
            sideEffectExtent.setPostEditSimulationEnabled(false);
        }
        setReorderMode(ReorderMode.NONE);
        if (chunkBatchingExtent != null) {
            chunkBatchingExtent.setEnabled(false);
            assert batchingExtent != null : "same nullness as chunkBatchingExtent";
            batchingExtent.setEnabled(true);
        }
    }

    /**
     * Check if this session will tick the watchdog.
     *
     * @return {@code true} if any watchdog extent is enabled
     */
    public boolean isTickingWatchdog() {
        return watchdogExtents.stream().anyMatch(WatchdogTickingExtent::isEnabled);
    }

    /**
     * Set all watchdog extents to the given mode.
     */
    public void setTickingWatchdog(boolean active) {
        for (WatchdogTickingExtent extent : watchdogExtents) {
            extent.setEnabled(active);
        }
    }

    /**
     * Get the number of blocks changed, including repeated block changes.
     *
     * <p>This number may not be accurate.</p>
     *
     * @return the number of block changes
     */
    public int getBlockChangeCount() {
        return changeSet.size();
    }

    @Override
    public BiomeType getBiome(BlockVector3 position) {
        return bypassNone.getBiome(position);
    }

    @Override
    public boolean setBiome(BlockVector3 position, BiomeType biome) {
        return bypassNone.setBiome(position, biome);
    }

    @Override
    public BlockState getBlock(BlockVector3 position) {
        return world.getBlock(position);
    }

    @Override
    public BaseBlock getFullBlock(BlockVector3 position) {
        return world.getFullBlock(position);
    }

    /**
     * As with {@link #getBlock(BlockVector3)}, gets the block at the given position.
     * However, this may return blocks not yet set to the world (i.e., buffered) by
     * the current EditSession.
     *
     * @param position position of the block
     * @return the block
     */
    public BlockState getBlockWithBuffer(BlockVector3 position) {
        return this.bypassNone.getBlock(position);
    }

    /**
     * As with {@link #getFullBlock(BlockVector3)}, gets the block at the given position,
     * but as with {@link #getBlockWithBuffer(BlockVector3)}, this may return a block in
     * the current EditSession's buffer rather than from the world.
     *
     * @param position position of the block
     * @return the block
     */
    public BaseBlock getFullBlockWithBuffer(BlockVector3 position) {
        return this.bypassNone.getFullBlock(position);
    }

    /**
     * Returns the highest solid 'terrain' block.
     *
     * @param x the X coordinate
     * @param z the Z coordinate
     * @param minY minimal height
     * @param maxY maximal height
     * @return height of highest block found or 'minY'
     */
    public int getHighestTerrainBlock(int x, int z, int minY, int maxY) {
        return getHighestTerrainBlock(x, z, minY, maxY, null);
    }

    /**
     * Returns the highest solid 'terrain' block.
     *
     * @param x the X coordinate
     * @param z the Z coordinate
     * @param minY minimal height
     * @param maxY maximal height
     * @param filter a mask of blocks to consider, or null to consider any solid (movement-blocking) block
     * @return height of highest block found or 'minY'
     */
    public int getHighestTerrainBlock(int x, int z, int minY, int maxY, Mask filter) {
        for (int y = maxY; y >= minY; --y) {
            BlockVector3 pt = BlockVector3.at(x, y, z);
            if (filter == null
                    ? getBlock(pt).getBlockType().getMaterial().isSolid()
                    : filter.test(pt)) {
                return y;
            }
        }

        return minY;
    }

    /**
     * Set a block, bypassing both history and block re-ordering.
     *
     * @param position the position to set the block at
     * @param block the block
     * @param stage the level
     * @return whether the block changed
     * @throws WorldEditException thrown on a set error
     */
    public <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 position, B block, Stage stage) throws WorldEditException {
        return switch (stage) {
            case BEFORE_HISTORY -> bypassNone.setBlock(position, block);
            case BEFORE_CHANGE -> bypassHistory.setBlock(position, block);
            case BEFORE_REORDER -> bypassReorderHistory.setBlock(position, block);
            default -> throw new RuntimeException("New enum entry added that is unhandled here");
        };
    }

    /**
     * Set a block, bypassing both history and block re-ordering.
     *
     * @param position the position to set the block at
     * @param block the block
     * @return whether the block changed
     */
    public <B extends BlockStateHolder<B>> boolean rawSetBlock(BlockVector3 position, B block) {
        try {
            return setBlock(position, block, Stage.BEFORE_CHANGE);
        } catch (WorldEditException e) {
            throw new RuntimeException("Unexpected exception", e);
        }
    }

    /**
     * Set a block, bypassing history but still utilizing block re-ordering.
     *
     * @param position the position to set the block at
     * @param block the block
     * @return whether the block changed
     */
    public <B extends BlockStateHolder<B>> boolean smartSetBlock(BlockVector3 position, B block) {
        try {
            return setBlock(position, block, Stage.BEFORE_REORDER);
        } catch (WorldEditException e) {
            throw new RuntimeException("Unexpected exception", e);
        }
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 position, B block) throws MaxChangedBlocksException {
        try {
            return setBlock(position, block, Stage.BEFORE_HISTORY);
        } catch (MaxChangedBlocksException e) {
            throw e;
        } catch (WorldEditException e) {
            throw new RuntimeException("Unexpected exception", e);
        }
    }

    /**
     * Sets the block at a position, subject to both history and block re-ordering.
     *
     * @param position the position
     * @param pattern a pattern to use
     * @return Whether the block changed -- not entirely dependable
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public boolean setBlock(BlockVector3 position, Pattern pattern) throws MaxChangedBlocksException {
        return setBlock(position, pattern.applyBlock(position));
    }

    @Override
    @Nullable
    public Entity createEntity(com.sk89q.worldedit.util.Location location, BaseEntity entity) {
        return bypassNone.createEntity(location, entity);
    }

    /**
     * Restores all blocks to their initial state.
     *
     * @param editSession a new {@link EditSession} to perform the undo in
     */
    public void undo(EditSession editSession) {
        UndoContext context = new UndoContext();
        context.setExtent(editSession.bypassHistory);
        Operations.completeBlindly(ChangeSetExecutor.createUndo(changeSet, context));
        editSession.internalFlushSession();
    }

    /**
     * Sets to new state.
     *
     * @param editSession a new {@link EditSession} to perform the redo in
     */
    public void redo(EditSession editSession) {
        UndoContext context = new UndoContext();
        context.setExtent(editSession.bypassHistory);
        Operations.completeBlindly(ChangeSetExecutor.createRedo(changeSet, context));
        editSession.internalFlushSession();
    }

    /**
     * Gets whether this EditSession will track history.
     *
     * @return whether history is tracked
     */
    public boolean isTrackingHistory() {
        return changeSetExtent != null && changeSetExtent.isEnabled();
    }

    /**
     * Sets whether this EditSession will track history.
     *
     * @param trackHistory whether to track history
     */
    public void setTrackingHistory(boolean trackHistory) {
        if (changeSetExtent != null) {
            changeSetExtent.setEnabled(trackHistory);
        } else if (trackHistory) {
            throw new IllegalStateException("No ChangeSetExtent is available");
        }
    }

    /**
     * Get the number of changed blocks.
     *
     * @return the number of changes
     */
    public int size() {
        return getBlockChangeCount();
    }

    @Override
    public BlockVector3 getMinimumPoint() {
        return getWorld().getMinimumPoint();
    }

    @Override
    public BlockVector3 getMaximumPoint() {
        return getWorld().getMaximumPoint();
    }

    @Override
    public List<? extends Entity> getEntities(Region region) {
        return bypassNone.getEntities(region);
    }

    @Override
    public List<? extends Entity> getEntities() {
        return bypassNone.getEntities();
    }

    /**
     * Closing an EditSession flushes its buffers to the world, and performs other
     * cleanup tasks.
     */
    @Override
    public void close() {
        internalFlushSession();
        dumpTracingInformation();
    }

    private void dumpTracingInformation() {
        if (this.tracingExtents == null) {
            return;
        }
        List<TracingExtent> tracingExtents = getActiveTracingExtents();
        assert actor != null;
        if (tracingExtents.isEmpty()) {
            actor.printError(TranslatableComponent.of("worldedit.trace.no-tracing-extents"));
            return;
        }
        // find the common stacks
        Set<List<TracingExtent>> stacks = new LinkedHashSet<>();
        Map<List<TracingExtent>, BlockVector3> stackToPosition = new HashMap<>();
        Set<BlockVector3> touchedLocations = Collections.newSetFromMap(BlockMap.create());
        for (TracingExtent tracingExtent : tracingExtents) {
            touchedLocations.addAll(tracingExtent.getTouchedLocations());
        }
        for (BlockVector3 loc : touchedLocations) {
            List<TracingExtent> stack = tracingExtents.stream()
                    .filter(it -> it.getTouchedLocations().contains(loc))
                    .toList();
            boolean anyFailed = stack.stream()
                .anyMatch(it -> it.getFailedActions().containsKey(loc));
            if (anyFailed && stacks.add(stack)) {
                stackToPosition.put(stack, loc);
            }
        }
        stackToPosition.forEach((stack, position) -> {
            // stack can never be empty, something has to have touched the position
            TracingExtent failure = stack.get(0);
            actor.printDebug(TranslatableComponent.builder("worldedit.trace.action-failed")
                .args(
                    TextComponent.of(failure.getFailedActions().get(position).toString()),
                    TextComponent.of(position.toString()),
                    TextComponent.of(failure.getExtent().getClass().getName())
                )
                .build());
        });
    }

    /**
     * Communicate to the EditSession that all block changes are complete,
     * and that it should apply them to the world.
     *
     * @deprecated Replace with {@link #close()} for proper cleanup behavior.
     */
    @Deprecated
    public void flushSession() {
        internalFlushSession();
    }

    private void internalFlushSession() {
        Operations.completeBlindly(commit());
    }

    @Override
    public @Nullable Operation commit() {
        return bypassNone.commit();
    }

    /**
     * Count the number of blocks of a list of types in a region.
     *
     * @param region the region
     * @param searchBlocks the list of blocks to search
     * @return the number of blocks that matched the block
     */
    public int countBlocks(Region region, Set<BaseBlock> searchBlocks) {
        BlockMask mask = new BlockMask(this, searchBlocks);
        return countBlocks(region, mask);
    }

    /**
     * Count the number of blocks of a list of types in a region.
     *
     * @param region the region
     * @param searchMask mask to match
     * @return the number of blocks that matched the mask
     */
    public int countBlocks(Region region, Mask searchMask) {
        Counter count = new Counter();
        RegionMaskingFilter filter = new RegionMaskingFilter(searchMask, count);
        RegionVisitor visitor = new RegionVisitor(region, filter);
        Operations.completeBlindly(visitor); // We can't throw exceptions, nor do we expect any
        return count.getCount();
    }

    /**
     * Fills an area recursively in the X/Z directions.
     *
     * @param origin the location to start from
     * @param block the block to fill with
     * @param radius the radius of the spherical area to fill
     * @param depth the maximum depth, starting from the origin
     * @param recursive whether a breadth-first search should be performed
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public <B extends BlockStateHolder<B>> int fillXZ(BlockVector3 origin, B block, double radius, int depth, boolean recursive) throws MaxChangedBlocksException {
        return fillXZ(origin, (Pattern) block, radius, depth, recursive);
    }

    /**
     * Fills an area recursively in the X/Z directions.
     *
     * @param origin the origin to start the fill from
     * @param pattern the pattern to fill with
     * @param radius the radius of the spherical area to fill, with 0 as the smallest radius
     * @param depth the maximum depth, starting from the origin, with 1 as the smallest depth
     * @param recursive whether a breadth-first search should be performed
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int fillXZ(BlockVector3 origin, Pattern pattern, double radius, int depth, boolean recursive) throws MaxChangedBlocksException {
        return TerrainOperations.fillXZ(this, origin, pattern, radius, depth, recursive);
    }

    /**
     * Remove a cuboid above the given position with a given apothem and a given height.
     *
     * @param position base position
     * @param apothem an apothem of the cuboid (on the XZ plane), where the minimum is 1
     * @param height the height of the cuboid, where the minimum is 1
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int removeAbove(BlockVector3 position, int apothem, int height) throws MaxChangedBlocksException {
        return RegionOperations.removeAbove(this, position, apothem, height);
    }

    /**
     * Remove a cuboid below the given position with a given apothem and a given height.
     *
     * @param position base position
     * @param apothem an apothem of the cuboid (on the XZ plane), where the minimum is 1
     * @param height the height of the cuboid, where the minimum is 1
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int removeBelow(BlockVector3 position, int apothem, int height) throws MaxChangedBlocksException {
        return RegionOperations.removeBelow(this, position, apothem, height);
    }

    /**
     * Remove blocks of a certain type nearby a given position.
     *
     * @param position center position of cuboid
     * @param mask the mask to match
     * @param apothem an apothem of the cuboid, where the minimum is 1
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int removeNear(BlockVector3 position, Mask mask, int apothem) throws MaxChangedBlocksException {
        return RegionOperations.removeNear(this, position, mask, apothem);
    }

    /**
     * Sets all the blocks inside a region to a given block type.
     *
     * @param region the region
     * @param block the block
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public <B extends BlockStateHolder<B>> int setBlocks(Region region, B block) throws MaxChangedBlocksException {
        return setBlocks(region, (Pattern) block);
    }

    /**
     * Sets all the blocks inside a region to a given pattern.
     *
     * @param region the region
     * @param pattern the pattern that provides the replacement block
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int setBlocks(Region region, Pattern pattern) throws MaxChangedBlocksException {
        return RegionOperations.setBlocks(this, region, pattern);
    }

    /**
     * Replaces all the blocks matching a given filter, within a given region, to a block
     * returned by a given pattern.
     *
     * @param region the region to replace the blocks within
     * @param filter a list of block types to match, or null to use {@link com.sk89q.worldedit.function.mask.ExistingBlockMask}
     * @param replacement the replacement block
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public <B extends BlockStateHolder<B>> int replaceBlocks(Region region, Set<BaseBlock> filter, B replacement) throws MaxChangedBlocksException {
        return replaceBlocks(region, filter, (Pattern) replacement);
    }

    /**
     * Replaces all the blocks matching a given filter, within a given region, to a block
     * returned by a given pattern.
     *
     * @param region the region to replace the blocks within
     * @param filter a list of block types to match, or null to use {@link com.sk89q.worldedit.function.mask.ExistingBlockMask}
     * @param pattern the pattern that provides the new blocks
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int replaceBlocks(Region region, Set<BaseBlock> filter, Pattern pattern) throws MaxChangedBlocksException {
        Mask mask = filter == null ? new ExistingBlockMask(this) : new BlockMask(this, filter);
        return replaceBlocks(region, mask, pattern);
    }

    /**
     * Replaces all the blocks matching a given mask, within a given region, to a block
     * returned by a given pattern.
     *
     * @param region the region to replace the blocks within
     * @param mask the mask that blocks must match
     * @param pattern the pattern that provides the new blocks
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int replaceBlocks(Region region, Mask mask, Pattern pattern) throws MaxChangedBlocksException {
        return RegionOperations.replaceBlocks(this, region, mask, pattern);
    }

    /**
     * Sets the blocks at the center of the given region to the given pattern.
     * If the center sits between two blocks on a certain axis, then two blocks
     * will be placed to mark the center.
     *
     * @param region the region to find the center of
     * @param pattern the replacement pattern
     * @return the number of blocks placed
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int center(Region region, Pattern pattern) throws MaxChangedBlocksException {
        return RegionOperations.center(this, region, pattern);
    }

    /**
     * Make the faces of the given region as if it was a {@link CuboidRegion}.
     *
     * @param region the region
     * @param block the block to place
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     * @deprecated Use {@link EditSession#makeCuboidFaces(Region, Pattern)}.
     */
    @InlineMe(
        replacement = "this.makeCuboidFaces(region, (Pattern) block)",
        imports = "com.sk89q.worldedit.function.pattern.Pattern"
    )
    @Deprecated
    public final <B extends BlockStateHolder<B>> int makeCuboidFaces(Region region, B block) throws MaxChangedBlocksException {
        return makeCuboidFaces(region, (Pattern) block);
    }

    /**
     * Make the faces of the given region as if it was a {@link CuboidRegion}.
     *
     * @param region the region
     * @param pattern the pattern to place
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int makeCuboidFaces(Region region, Pattern pattern) throws MaxChangedBlocksException {
        return RegionOperations.makeCuboidFaces(this, region, pattern);
    }

    /**
     * Make the faces of the given region. The method by which the faces are found
     * may be inefficient, because there may not be an efficient implementation supported
     * for that specific shape.
     *
     * @param region the region
     * @param pattern the pattern to place
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int makeFaces(final Region region, Pattern pattern) throws MaxChangedBlocksException {
        return RegionOperations.makeFaces(this, region, pattern);
    }


    /**
     * Make the walls (all faces but those parallel to the X-Z plane) of the given region
     * as if it was a {@link CuboidRegion}.
     *
     * @param region the region
     * @param block the block to place
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public <B extends BlockStateHolder<B>> int makeCuboidWalls(Region region, B block) throws MaxChangedBlocksException {
        return makeCuboidWalls(region, (Pattern) block);
    }

    /**
     * Make the walls (all faces but those parallel to the X-Z plane) of the given region
     * as if it was a {@link CuboidRegion}.
     *
     * @param region the region
     * @param pattern the pattern to place
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int makeCuboidWalls(Region region, Pattern pattern) throws MaxChangedBlocksException {
        return RegionOperations.makeCuboidWalls(this, region, pattern);
    }

    /**
     * Make the walls of the given region. The method by which the walls are found
     * may be inefficient, because there may not be an efficient implementation supported
     * for that specific shape.
     *
     * @param region the region
     * @param pattern the pattern to place
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int makeWalls(final Region region, Pattern pattern) throws MaxChangedBlocksException {
        return RegionOperations.makeWalls(this, region, pattern);
    }

    /**
     * Places a layer of blocks on top of ground blocks in the given region
     * (as if it were a cuboid).
     *
     * @param region the region
     * @param block the placed block
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     * @deprecated Use {@link EditSession#overlayCuboidBlocks(Region, Pattern)}.
     */
    @Deprecated
    public <B extends BlockStateHolder<B>> int overlayCuboidBlocks(Region region, B block) throws MaxChangedBlocksException {
        checkNotNull(block);

        return overlayCuboidBlocks(region, (Pattern) block);
    }

    /**
     * Places a layer of blocks on top of ground blocks in the given region
     * (as if it were a cuboid).
     *
     * @param region the region
     * @param pattern the placed block pattern
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int overlayCuboidBlocks(Region region, Pattern pattern) throws MaxChangedBlocksException {
        return TerrainOperations.overlayCuboidBlocks(this, region, pattern);
    }

    /**
     * Turns the first 3 layers into dirt/grass and the bottom layers
     * into rock, like a natural Minecraft mountain.
     *
     * @param region the region to affect
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int naturalizeCuboidBlocks(Region region) throws MaxChangedBlocksException {
        return TerrainOperations.naturalizeCuboidBlocks(this, region);
    }

    /**
     * Stack a cuboid region. For compatibility, entities are copied by biomes are not.
     * Use {@link #stackCuboidRegion(Region, BlockVector3, int, boolean, boolean, Mask)} to fine tune.
     *
     * @param region the region to stack
     * @param dir the direction to stack
     * @param count the number of times to stack
     * @param copyAir true to also copy air blocks
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int stackCuboidRegion(Region region, BlockVector3 dir, int count, boolean copyAir) throws MaxChangedBlocksException {
        return stackCuboidRegion(region, dir, count, true, false, copyAir ? null : new ExistingBlockMask(this));
    }

    /**
     * Stack a cuboid region.
     *
     * @param region the region to stack
     * @param offset how far to move the contents each stack
     * @param count the number of times to stack
     * @param copyEntities true to copy entities
     * @param copyBiomes true to copy biomes
     * @param mask source mask for the operation (only matching blocks are copied)
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int stackCuboidRegion(Region region, BlockVector3 offset, int count,
                                 boolean copyEntities, boolean copyBiomes, Mask mask) throws MaxChangedBlocksException {
        return RegionCopyOperations.stackCuboidRegion(this, region, offset, count, copyEntities, copyBiomes, mask);
    }

    /**
     * Stack a region using block units.
     *
     * @param region the region to stack
     * @param offset how far to move the contents each stack in block units
     * @param count the number of times to stack
     * @param copyEntities true to copy entities
     * @param copyBiomes true to copy biomes
     * @param mask source mask for the operation (only matching blocks are copied)
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     * @throws RegionOperationException thrown if the region operation is invalid
     */
    public int stackRegionBlockUnits(Region region, BlockVector3 offset, int count,
                                     boolean copyEntities, boolean copyBiomes, Mask mask) throws MaxChangedBlocksException, RegionOperationException {
        return RegionCopyOperations.stackRegionBlockUnits(this, region, offset, count, copyEntities, copyBiomes, mask);
    }

    /**
     * Move the blocks in a region a certain direction.
     *
     * @param region the region to move
     * @param offset the offset
     * @param multiplier the number to multiply the offset by
     * @param copyAir true to copy air blocks
     * @param replacement the replacement pattern to fill in after moving, or null to use air
     * @return number of blocks moved
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int moveRegion(Region region, BlockVector3 offset, int multiplier, boolean copyAir, Pattern replacement) throws MaxChangedBlocksException {
        return moveRegion(region, offset, multiplier, true, false, copyAir ? new ExistingBlockMask(this) : null, replacement);
    }

    /**
     * Move the blocks in a region a certain direction.
     *
     * @param region the region to move
     * @param offset the offset
     * @param multiplier the number to multiply the offset by
     * @param moveEntities true to move entities
     * @param copyBiomes true to copy biomes (source biome is unchanged)
     * @param mask source mask for the operation (only matching blocks are moved)
     * @param replacement the replacement pattern to fill in after moving, or null to use air
     * @return number of blocks moved
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     * @throws IllegalArgumentException thrown if the region is not a flat region, but copyBiomes is true
     */
    public int moveRegion(Region region, BlockVector3 offset, int multiplier,
                          boolean moveEntities, boolean copyBiomes, Mask mask, Pattern replacement) throws MaxChangedBlocksException {
        return RegionCopyOperations.moveRegion(this, region, offset, multiplier, moveEntities, copyBiomes, mask, replacement);
    }

    /**
     * Move the blocks in a region a certain direction.
     *
     * @param region the region to move
     * @param dir the direction
     * @param distance the distance to move
     * @param copyAir true to copy air blocks
     * @param replacement the replacement pattern to fill in after moving, or null to use air
     * @return number of blocks moved
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int moveCuboidRegion(Region region, BlockVector3 dir, int distance, boolean copyAir, Pattern replacement) throws MaxChangedBlocksException {
        return moveRegion(region, dir, distance, copyAir, replacement);
    }

    /**
     * Drain nearby pools of water or lava.
     *
     * @param origin the origin to drain from, which will search a 3x3 area
     * @param radius the radius of the removal, where a value should be 0 or greater
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int drainArea(BlockVector3 origin, double radius) throws MaxChangedBlocksException {
        return drainArea(origin, radius, false);
    }

    /**
     * Drain nearby pools of water or lava, optionally removed waterlogged states from blocks.
     *
     * @param origin the origin to drain from, which will search a 3x3 area
     * @param radius the radius of the removal, where a value should be 0 or greater
     * @param waterlogged true to make waterlogged blocks non-waterlogged as well
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int drainArea(BlockVector3 origin, double radius, boolean waterlogged) throws MaxChangedBlocksException {
        return TerrainOperations.drainArea(this, origin, radius, waterlogged);
    }

    /**
     * Fix liquids so that they turn into stationary blocks and extend outward.
     *
     * @param origin the original position
     * @param radius the radius to fix
     * @param fluid the type of the fluid
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int fixLiquid(BlockVector3 origin, double radius, BlockType fluid) throws MaxChangedBlocksException {
        return TerrainOperations.fixLiquid(this, origin, radius, fluid);
    }

    /**
     * Makes a cylinder.
     *
     * @param pos Center of the cylinder
     * @param block The block pattern to use
     * @param radius The cylinder's radius
     * @param height The cylinder's up/down extent. If negative, extend downward.
     * @param filled If false, only a shell will be generated.
     * @return number of blocks changed
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int makeCylinder(BlockVector3 pos, Pattern block, double radius, int height, boolean filled) throws MaxChangedBlocksException {
        return makeCylinder(pos, block, radius, radius, height, filled);
    }

    /**
     * Makes a cylinder.
     *
     * @param pos Center of the cylinder
     * @param block The block pattern to use
     * @param radiusX The cylinder's largest north/south extent
     * @param radiusZ The cylinder's largest east/west extent
     * @param height The cylinder's up/down extent. If negative, extend downward.
     * @param filled If false, only a shell will be generated.
     * @return number of blocks changed
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int makeCylinder(BlockVector3 pos, Pattern block, double radiusX, double radiusZ, int height, boolean filled) throws MaxChangedBlocksException {
        return ShapeGenerator.makeCylinder(this, world, pos, block, radiusX, radiusZ, height, filled);
    }

    /**
     * Makes a cone.
     *
     * @param pos Center of the cone
     * @param block The block pattern to use
     * @param radiusX The cone's largest north/south extent
     * @param radiusZ The cone's largest east/west extent
     * @param height The cone's up/down extent. If negative, extend downward.
     * @param filled If false, only a shell will be generated.
     * @param thickness The cone's wall thickness, if it's hollow.
     * @return number of blocks changed
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int makeCone(BlockVector3 pos, Pattern block, double radiusX, double radiusZ, int height, boolean filled,
                        double thickness) throws MaxChangedBlocksException {
        return ShapeGenerator.makeCone(this, pos, block, radiusX, radiusZ, height, filled, thickness);
    }

    /**
    * Makes a sphere.
    *
    * @param pos Center of the sphere or ellipsoid
    * @param block The block pattern to use
    * @param radius The sphere's radius
    * @param filled If false, only a shell will be generated.
    * @return number of blocks changed
    * @throws MaxChangedBlocksException thrown if too many blocks are changed
    */
    public int makeSphere(BlockVector3 pos, Pattern block, double radius, boolean filled) throws MaxChangedBlocksException {
        return makeSphere(pos, block, radius, radius, radius, filled);
    }

    /**
     * Makes a sphere or ellipsoid.
     *
     * @param pos Center of the sphere or ellipsoid
     * @param block The block pattern to use
     * @param radiusX The sphere/ellipsoid's largest north/south extent
     * @param radiusY The sphere/ellipsoid's largest up/down extent
     * @param radiusZ The sphere/ellipsoid's largest east/west extent
     * @param filled If false, only a shell will be generated.
     * @return number of blocks changed
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int makeSphere(BlockVector3 pos, Pattern block, double radiusX, double radiusY, double radiusZ, boolean filled) throws MaxChangedBlocksException {
        return ShapeGenerator.makeSphere(this, pos, block, radiusX, radiusY, radiusZ, filled);
    }

    /**
     * Makes a pyramid.
     *
     * @param position a position
     * @param block a block
     * @param size size of pyramid
     * @param filled true if filled
     * @return number of blocks changed
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int makePyramid(BlockVector3 position, Pattern block, int size, boolean filled) throws MaxChangedBlocksException {
        return ShapeGenerator.makePyramid(this, position, block, size, filled);
    }

    /**
     * Thaw blocks in a radius.
     *
     * @param position the position
     * @param radius the radius
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     * @deprecated Use {@link #thaw(BlockVector3, double, int)}.
     */
    @InlineMe(
        replacement = "this.thaw(position, radius, WorldEdit.getInstance().getConfiguration().defaultVerticalHeight)",
        imports = "com.sk89q.worldedit.WorldEdit"
    )
    @Deprecated
    public final int thaw(BlockVector3 position, double radius)
        throws MaxChangedBlocksException {
        return thaw(position, radius,
            WorldEdit.getInstance().getConfiguration().defaultVerticalHeight);
    }

    /**
     * Thaw blocks in a cylinder.
     *
     * @param position the position
     * @param radius the radius
     * @param height the height (upwards and downwards)
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int thaw(BlockVector3 position, double radius, int height)
        throws MaxChangedBlocksException {
        return TerrainOperations.thaw(this, position, radius, height);
    }

    /**
     * Make snow in a radius.
     *
     * @param position a position
     * @param radius a radius
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     * @deprecated Use {@link #simulateSnow(BlockVector3, double, int)}.
     */
    @InlineMe(
        replacement = "this.simulateSnow(position, radius, WorldEdit.getInstance().getConfiguration().defaultVerticalHeight)",
        imports = "com.sk89q.worldedit.WorldEdit"
    )
    @Deprecated
    public final int simulateSnow(BlockVector3 position, double radius) throws MaxChangedBlocksException {
        return simulateSnow(position, radius,
            WorldEdit.getInstance().getConfiguration().defaultVerticalHeight);
    }

    /**
     * Make snow in a cylinder.
     *
     * @param position a position
     * @param radius a radius
     * @param height the height (upwards and downwards)
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int simulateSnow(BlockVector3 position, double radius, int height)
        throws MaxChangedBlocksException {

        return simulateSnow(new CylinderRegion(position, Vector2.at(radius, radius), position.y(), height), false);
    }


    /**
     * Make snow in a region.
     *
     * @param region the region to simulate snow in
     * @param stack whether it should stack existing snow
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int simulateSnow(FlatRegion region, boolean stack)
            throws MaxChangedBlocksException {
        return TerrainOperations.simulateSnow(this, region, stack);
    }

    /**
     * Make dirt green.
     *
     * @param position a position
     * @param radius a radius
     * @param onlyNormalDirt only affect normal dirt (all default properties)
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     * @deprecated Use {@link #green(BlockVector3, double, int, boolean)}.
     */
    @InlineMe(
        replacement = "this.green(position, radius, WorldEdit.getInstance().getConfiguration().defaultVerticalHeight, onlyNormalDirt)",
        imports = "com.sk89q.worldedit.WorldEdit"
    )
    @Deprecated
    public final int green(BlockVector3 position, double radius, boolean onlyNormalDirt)
        throws MaxChangedBlocksException {
        return green(position, radius,
            WorldEdit.getInstance().getConfiguration().defaultVerticalHeight, onlyNormalDirt);
    }

    /**
     * Make dirt green in a cylinder.
     *
     * @param position the position
     * @param radius the radius
     * @param height the height
     * @param onlyNormalDirt only affect normal dirt (all default properties)
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int green(BlockVector3 position, double radius, int height, boolean onlyNormalDirt)
        throws MaxChangedBlocksException {
        return TerrainOperations.green(this, position, radius, height, onlyNormalDirt);
    }

    /**
     * Makes pumpkin patches randomly in an area around the given position.
     *
     * @param position the base position
     * @param apothem the apothem of the (square) area
     * @return number of patches created
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int makePumpkinPatches(BlockVector3 position, int apothem) throws MaxChangedBlocksException {
        return TerrainOperations.makePumpkinPatches(this, position, apothem);
    }

    /**
     * Makes a forest.
     *
     * @param basePosition a position
     * @param size a size
     * @param density between 0 and 1, inclusive
     * @param treeType the tree type
     * @return number of trees created
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     * @deprecated Use {@link #makeForest(Region, double, TreeType)}.
     */
    @Deprecated
    public int makeForest(BlockVector3 basePosition, int size, double density, TreeGenerator.TreeType treeType) throws MaxChangedBlocksException {
        return makeForest(CuboidRegion.fromCenter(basePosition, size), density, treeType);
    }

    /**
     * Makes a forest.
     *
     * @param region the region to generate trees in
     * @param density between 0 and 1, inclusive
     * @param treeType the tree type
     * @return number of trees created
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     * @deprecated Use {@link #makeForest(Region, double, TreeType)}.
     */
    @Deprecated
    @SuppressWarnings("InlineMeSuggester") // inlining would expose the internal implementation
    public int makeForest(Region region, double density, TreeGenerator.TreeType treeType) throws MaxChangedBlocksException {
        return TerrainOperations.makeForest(this, region, density, treeType);
    }

    /**
     * Makes a forest.
     *
     * @param basePosition a position
     * @param size a size
     * @param density between 0 and 1, inclusive
     * @param treeType the tree type
     * @return number of trees created
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int makeForest(BlockVector3 basePosition, int size, double density, TreeType treeType) throws MaxChangedBlocksException {
        return makeForest(CuboidRegion.fromCenter(basePosition, size), density, treeType);
    }

    /**
     * Makes a forest.
     *
     * @param region the region to generate trees in
     * @param density between 0 and 1, inclusive
     * @param treeType the tree type
     * @return number of trees created
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int makeForest(Region region, double density, TreeType treeType) throws MaxChangedBlocksException {
        return TerrainOperations.makeForest(this, region, density, treeType);
    }

    /**
     * Get the block distribution inside a region.
     *
     * @param region a region
     * @return the results
     */
    public List<Countable<BlockState>> getBlockDistribution(Region region, boolean separateStates) {
        return getBlockDistribution(region, Masks.alwaysTrue(), separateStates);
    }

    /**
     * Get the block distribution inside a region.
     *
     * @param region a region
     * @param mask a mask to filter the blocks to count
     * @return the results
     */
    public List<Countable<BlockState>> getBlockDistribution(Region region, @Nullable Mask mask, boolean separateStates) {
        BlockDistributionCounter count = new BlockDistributionCounter(this, mask, separateStates);
        RegionVisitor visitor = new RegionVisitor(region, count);
        Operations.completeBlindly(visitor);
        return count.getDistribution();
    }

    /**
     * Generate a shape for the given expression.
     *
     * @param region the region to generate the shape in
     * @param zero the coordinate origin for x/y/z variables
     * @param unit the scale of the x/y/z/ variables
     * @param pattern the default material to make the shape from
     * @param expressionString the expression defining the shape
     * @param hollow whether the shape should be hollow
     * @return number of blocks changed
     * @throws ExpressionException if there is a problem with the expression
     * @throws MaxChangedBlocksException if the maximum block change limit is exceeded
     * @deprecated Use {@link EditSession#makeShape(Region, Transform, Pattern, String, boolean, int)} and pass a {@link ScaleAndTranslateTransform}.
     */
    @InlineMe(replacement = "this.makeShape(region, new ScaleAndTranslateTransform(zero, unit), pattern, expressionString, hollow, WorldEdit.getInstance().getConfiguration().calculationTimeout)", imports = {"com.sk89q.worldedit.WorldEdit", "com.sk89q.worldedit.math.transform.ScaleAndTranslateTransform"})
    @Deprecated
    public final int makeShape(final Region region, final Vector3 zero, final Vector3 unit,
                         final Pattern pattern, final String expressionString, final boolean hollow)
            throws ExpressionException, MaxChangedBlocksException {
        return makeShape(region, new ScaleAndTranslateTransform(zero, unit), pattern, expressionString, hollow, WorldEdit.getInstance().getConfiguration().calculationTimeout);
    }

    /**
     * Generate a shape for the given expression.
     *
     * @param region the region to generate the shape in
     * @param zero the coordinate origin for x/y/z variables
     * @param unit the scale of the x/y/z/ variables
     * @param pattern the default material to make the shape from
     * @param expressionString the expression defining the shape
     * @param hollow whether the shape should be hollow
     * @param timeout the time, in milliseconds, to wait for each expression evaluation before halting it. -1 to disable
     * @return number of blocks changed
     * @throws ExpressionException if there is a problem with the expression
     * @throws MaxChangedBlocksException if the maximum block change limit is exceeded
     * @deprecated Use {@link EditSession#makeShape(Region, Transform, Pattern, String, boolean, int)} and pass a {@link ScaleAndTranslateTransform}.
     */
    @InlineMe(replacement = "this.makeShape(region, new ScaleAndTranslateTransform(zero, unit), pattern, expressionString, hollow, timeout)", imports = "com.sk89q.worldedit.math.transform.ScaleAndTranslateTransform")
    @Deprecated
    public final int makeShape(final Region region, final Vector3 zero, final Vector3 unit,
                         final Pattern pattern, final String expressionString, final boolean hollow, final int timeout)
            throws ExpressionException, MaxChangedBlocksException {
        return makeShape(region, new ScaleAndTranslateTransform(zero, unit), pattern, expressionString, hollow, timeout);
    }

    /**
     * Generate a shape for the given expression.
     *
     * @param region the region to generate the shape in
     * @param zero the coordinate origin for x/y/z variables
     * @param unit the scale of the x/y/z/ variables
     * @param pattern the default material to make the shape from
     * @param expression the expression defining the shape
     * @param hollow whether the shape should be hollow
     * @param timeout the time, in milliseconds, to wait for each expression evaluation before halting it. -1 to disable
     * @return number of blocks changed
     * @throws ExpressionException if there is a problem with the expression
     * @throws MaxChangedBlocksException if the maximum block change limit is exceeded
     * @deprecated Use {@link EditSession#makeShape(Region, Transform, Pattern, String, boolean, int)} and pass a {@link ScaleAndTranslateTransform}.
     */
    @InlineMe(replacement = "this.makeShape(region, new ScaleAndTranslateTransform(zero, unit), pattern, expression, hollow, timeout)", imports = "com.sk89q.worldedit.math.transform.ScaleAndTranslateTransform")
    @Deprecated
    public final int makeShape(final Region region, final Vector3 zero, final Vector3 unit,
                         final Pattern pattern, final Expression expression, final boolean hollow, final int timeout)
            throws ExpressionException, MaxChangedBlocksException {
        return makeShape(region, new ScaleAndTranslateTransform(zero, unit), pattern, expression, hollow, timeout);
    }

    /**
     * Generate a shape for the given expression.
     *
     * @param region           the region to generate the shape in
     * @param transform        the transformation for x/y/z variables
     * @param pattern          the default material to make the shape from
     * @param expressionString the expression defining the shape
     * @param hollow           whether the shape should be hollow
     * @param timeout          the time, in milliseconds, to wait for each expression evaluation before halting it. -1 to disable
     * @return number of blocks changed
     * @throws ExpressionException       if there is a problem with the expression
     * @throws MaxChangedBlocksException if the maximum block change limit is exceeded
     */
    public int makeShape(final Region region,
                         Transform transform, final Pattern pattern, final String expressionString, final boolean hollow, final int timeout)
            throws ExpressionException, MaxChangedBlocksException {
        final Expression expression = ExpressionOperations.compile(expressionString, "x", "y", "z", "type", "data");
        return makeShape(region, transform, pattern, expression, hollow, timeout);
    }

    /**
     * Internal version of {@link EditSession#makeShape(Region, Vector3, Vector3, Pattern, String, boolean, int)}.
     *
     * <p>
     * The Expression class is subject to change. Expressions should be provided via the string overload.
     * </p>
     */
    public int makeShape(final Region region, Transform transform,
                         final Pattern pattern, final Expression expression, final boolean hollow, final int timeout)
            throws ExpressionException, MaxChangedBlocksException {
        return ExpressionOperations.makeShape(this, region, transform, pattern, expression, hollow, timeout);
    }

    /**
     * Deforms the region by a given expression. A deform provides a block's x, y, and z coordinates (possibly scaled)
     * to an expression, and then sets the block to the block given by the resulting values of the variables, if they
     * have changed.
     *
     * @param region the region to deform
     * @param zero the origin of the coordinate system
     * @param unit the scale of the coordinate system
     * @param expressionString the expression to evaluate for each block
     *
     * @return number of blocks changed
     *
     * @throws ExpressionException thrown on invalid expression input
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     * @deprecated Use {@link EditSession#deformRegion(Region, Transform, String, int, InputExtent, Transform)} and pass a {@link ScaleAndTranslateTransform}.
     */
    @Deprecated
    public int deformRegion(final Region region, final Vector3 zero, final Vector3 unit, final String expressionString)
            throws ExpressionException, MaxChangedBlocksException {
        return deformRegion(region, zero, unit, expressionString, WorldEdit.getInstance().getConfiguration().calculationTimeout);
    }

    /**
     * Deforms the region by a given expression. A deform provides a block's x, y, and z coordinates (possibly scaled)
     * to an expression, and then sets the block to the block given by the resulting values of the variables, if they
     * have changed.
     *
     * @param region the region to deform
     * @param zero the origin of the coordinate system
     * @param unit the scale of the coordinate system
     * @param expressionString the expression to evaluate for each block
     * @param timeout maximum time for the expression to evaluate for each block. -1 for unlimited.
     *
     * @return number of blocks changed
     *
     * @throws ExpressionException thrown on invalid expression input
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     * @deprecated Use {@link EditSession#deformRegion(Region, Transform, String, int, InputExtent, Transform)} and pass a {@link ScaleAndTranslateTransform}.
     */
    @Deprecated
    public int deformRegion(final Region region, final Vector3 zero, final Vector3 unit, final String expressionString,
                            final int timeout) throws ExpressionException, MaxChangedBlocksException {
        final Expression expression = ExpressionOperations.compile(expressionString, "x", "y", "z");
        return deformRegion(region, zero, unit, expression, timeout);
    }

    /**
     * Deforms the region by a given expression. A deform provides a block's x, y, and z coordinates (possibly scaled)
     * to an expression, and then sets the block to the block given by the resulting values of the variables, if they
     * have changed.
     *
     * @param region           the region to deform
     * @param targetTransform  the target coordinate system
     * @param expressionString the expression to evaluate for each block
     * @param timeout          maximum time for the expression to evaluate for each block. -1 for unlimited.
     * @param sourceExtent     the InputExtent to fetch blocks from, for instance a World or a Clipboard
     * @param sourceTransform  the source coordinate system
     * @return number of blocks changed
     * @throws ExpressionException       thrown on invalid expression input
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int deformRegion(final Region region, final Transform targetTransform, final String expressionString,
                            final int timeout, InputExtent sourceExtent, Transform sourceTransform) throws ExpressionException, MaxChangedBlocksException {
        final Expression expression = ExpressionOperations.compile(expressionString, "x", "y", "z");
        return deformRegion(region, targetTransform, expression, timeout, sourceExtent, sourceTransform);
    }

    /**
     * Deforms the region by a given expression. A deform provides a block's x, y, and z coordinates (possibly scaled)
     * to an expression, and then sets the block to the block given by the resulting values of the variables, if they
     * have changed.
     *
     * @param region           the region to deform
     * @param zero the coordinate origin for x/y/z variables
     * @param unit the scale of the x/y/z/ variables
     * @param expression the expression to evaluate for each block
     * @param timeout          maximum time for the expression to evaluate for each block. -1 for unlimited.
     * @return number of blocks changed
     * @throws ExpressionException       thrown on invalid expression input
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     * @deprecated Use {@link EditSession#deformRegion(Region, Transform, String, int, InputExtent, Transform)}.
     */
    @Deprecated
    public int deformRegion(final Region region, final Vector3 zero, final Vector3 unit, final Expression expression,
                            final int timeout) throws ExpressionException, MaxChangedBlocksException {
        var transform = new ScaleAndTranslateTransform(zero, unit);
        return deformRegion(region, transform, expression, timeout, world, transform);
    }

    /**
     * Internal version of {@link EditSession#deformRegion(Region, Vector3, Vector3, String, int)}.
     *
     * <p>
     * The Expression class is subject to change. Expressions should be provided via the string overload.
     * </p>
     */
    public int deformRegion(final Region region, final Transform targetTransform, final Expression expression,
                            final int timeout, InputExtent sourceExtent, final Transform sourceTransform) throws ExpressionException, MaxChangedBlocksException {
        return ExpressionOperations.deformRegion(this, region, targetTransform, expression, timeout, sourceExtent, sourceTransform);
    }

    /**
     * Hollows out the region (Semi-well-defined for non-cuboid selections).
     *
     * @param region the region to hollow out.
     * @param thickness the thickness of the shell to leave (manhattan distance)
     * @param pattern The block pattern to use
     *
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int hollowOutRegion(Region region, int thickness, Pattern pattern) throws MaxChangedBlocksException {
        return MorphologyOperations.hollowOutRegion(this, region, thickness, pattern);
    }

    /**
     * Draws a line (out of blocks) between two vectors.
     *
     * @param pattern The block pattern used to draw the line.
     * @param pos1 One of the points that define the line.
     * @param pos2 The other point that defines the line.
     * @param radius The radius (thickness) of the line.
     * @param filled If false, only a shell will be generated.
     *
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     *
     * @see #drawLine(Pattern, List, double, boolean)
     */
    public int drawLine(Pattern pattern, BlockVector3 pos1, BlockVector3 pos2, double radius, boolean filled)
            throws MaxChangedBlocksException {
        return drawLine(pattern, ImmutableList.of(pos1, pos2), radius, filled);
    }

    /**
     * Draws a line (out of blocks) between two or more vectors.
     *
     * @param pattern The block pattern used to draw the line.
     * @param vectors the list of vectors to draw the line between
     * @param radius The radius (thickness) of the line.
     * @param filled If false, only a shell will be generated.
     *
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int drawLine(Pattern pattern, List<BlockVector3> vectors, double radius, boolean filled)
            throws MaxChangedBlocksException {
        return LineGenerator.drawLine(this, pattern, vectors, radius, filled);
    }

    /**
     * Draws a spline (out of blocks) between specified vectors.
     *
     * @param pattern The block pattern used to draw the spline.
     * @param nodevectors The list of vectors to draw through.
     * @param tension The tension of every node.
     * @param bias The bias of every node.
     * @param continuity The continuity of every node.
     * @param quality The quality of the spline. Must be greater than 0.
     * @param radius The radius (thickness) of the spline.
     * @param filled If false, only a shell will be generated.
     *
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int drawSpline(Pattern pattern, List<BlockVector3> nodevectors, double tension, double bias,
                          double continuity, double quality, double radius, boolean filled)
            throws MaxChangedBlocksException {
        return LineGenerator.drawSpline(this, pattern, nodevectors, tension, bias, continuity, quality, radius, filled);
    }

    /**
     * Generate a biome shape for the given expression.
     *
     * @deprecated Use {@link EditSession#makeBiomeShape(Region, Transform, BiomeType, String, boolean, int)} and pass a {@link ScaleAndTranslateTransform}.
     */
    @InlineMe(replacement = "this.makeBiomeShape(region, new ScaleAndTranslateTransform(zero, unit), biomeType, expressionString, hollow, WorldEdit.getInstance().getConfiguration().calculationTimeout)", imports = {"com.sk89q.worldedit.WorldEdit", "com.sk89q.worldedit.math.transform.ScaleAndTranslateTransform"})
    @Deprecated
    public final int makeBiomeShape(final Region region, final Vector3 zero, final Vector3 unit, final BiomeType biomeType,
                              final String expressionString, final boolean hollow) throws ExpressionException {
        return makeBiomeShape(region, new ScaleAndTranslateTransform(zero, unit), biomeType, expressionString, hollow, WorldEdit.getInstance().getConfiguration().calculationTimeout);
    }

    /**
     * Generate a biome shape for the given expression.
     *
     * @deprecated Use {@link EditSession#makeBiomeShape(Region, Transform, BiomeType, String, boolean, int)} and pass a {@link ScaleAndTranslateTransform}.
     */
    @InlineMe(replacement = "this.makeBiomeShape(region, new ScaleAndTranslateTransform(zero, unit), biomeType, expressionString, hollow, timeout)", imports = "com.sk89q.worldedit.math.transform.ScaleAndTranslateTransform")
    @Deprecated
    public final int makeBiomeShape(final Region region, final Vector3 zero, final Vector3 unit, final BiomeType biomeType,
                              final String expressionString, final boolean hollow, final int timeout) throws ExpressionException {
        return makeBiomeShape(region, new ScaleAndTranslateTransform(zero, unit), biomeType, expressionString, hollow, timeout);
    }

    /**
     * Generate a biome shape for the given expression.
     *
     * @param region the region to generate the shape in
     * @param transform the transformation for x/y/z variables
     * @param biomeType the biome to make the shape from
     * @param expressionString the expression defining the shape
     * @param hollow whether the shape should be hollow
     * @param timeout maximum time for the expression to evaluate for each block. -1 for unlimited.
     * @return number of blocks changed
     * @throws ExpressionException if there is a problem with the expression
     */
    public int makeBiomeShape(final Region region, Transform transform, final BiomeType biomeType,
                              final String expressionString, final boolean hollow, final int timeout) throws ExpressionException {
        return ExpressionOperations.makeBiomeShape(this, region, transform, biomeType, expressionString, hollow, timeout);
    }

    /**
     * Erode, then dilate, the blocks in a sphere.
     *
     * <p>Eroding replaces a filled block that has at least {@code minErodeFaces} empty (air or
     * liquid) neighbours with its most common empty neighbour; dilating replaces an empty block
     * that has at least {@code minDilateFaces} filled neighbours with its most common filled
     * neighbour.</p>
     *
     * @param position the center of the sphere
     * @param brushSize the radius of the sphere
     * @param minErodeFaces the minimum number of empty neighbours for a block to erode
     * @param numErodeIterations the number of erosion passes
     * @param minDilateFaces the minimum number of filled neighbours for an empty block to be filled
     * @param numDilateIterations the number of dilation passes
     * @return number of blocks changed
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int morph(BlockVector3 position, double brushSize, int minErodeFaces, int numErodeIterations, int minDilateFaces, int numDilateIterations) throws MaxChangedBlocksException {
        return MorphologyOperations.morph(this, position, brushSize, minErodeFaces, numErodeIterations, minDilateFaces, numDilateIterations);
    }

}
