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

import com.google.common.collect.ImmutableList;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.EditSession.Stage;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extension.platform.Watchdog;
import com.sk89q.worldedit.extent.ChangeSetExtent;
import com.sk89q.worldedit.extent.Extent;
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
import com.sk89q.worldedit.function.mask.Masks;
import com.sk89q.worldedit.history.changeset.ChangeSet;
import com.sk89q.worldedit.util.eventbus.EventBus;
import com.sk89q.worldedit.world.NullWorld;
import com.sk89q.worldedit.world.World;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * The chain of {@link Extent}s behind an {@link EditSession}, from the world up
 * to the extent that block changes enter, with the entry points that bypass
 * history and re-ordering.
 *
 * <p>Plugins can wrap each {@link Stage} of the chain through the
 * {@link EditSessionEvent}, which this posts while building it.</p>
 */
public final class ExtentChain {

    private final @Nullable List<TracingExtent> tracingExtents;

    private final @Nullable SideEffectExtent sideEffectExtent;
    private final SurvivalModeExtent survivalExtent;
    private final @Nullable BatchingExtent batchingExtent;
    private final @Nullable ChunkBatchingExtent chunkBatchingExtent;
    private final BlockBagExtent blockBagExtent;
    @SuppressWarnings("deprecation")
    private final MultiStageReorder reorderExtent;
    private final MaskingExtent maskingExtent;
    private final BlockChangeLimiter changeLimiter;
    private final @Nullable ChangeSetExtent changeSetExtent;
    private final List<WatchdogTickingExtent> watchdogExtents = new ArrayList<>(2);

    private final Extent bypassReorderHistory;
    private final Extent bypassHistory;
    private final Extent bypassNone;

    /**
     * Build the chain.
     *
     * @param eventBus the event bus to post the {@link EditSessionEvent} to
     * @param world the world, or null to build a chain over a {@link NullExtent}
     * @param actor the actor that owns the session
     * @param maxBlocks the maximum number of blocks that can be changed, or -1 to use no limit
     * @param blockBag an optional {@link BlockBag} to use, otherwise null
     * @param changeSet the change set that records history
     * @param tracing whether to wrap every extent in a {@link TracingExtent}
     */
    // Suppressing AssignmentExpression: This provides clarity in the way that we use it.
    @SuppressWarnings({"AssignmentExpression", "deprecation"})
    public ExtentChain(EventBus eventBus, @Nullable World world, @Nullable Actor actor, int maxBlocks,
                       @Nullable BlockBag blockBag, ChangeSet changeSet, boolean tracing) {
        this.tracingExtents = tracing ? new ArrayList<>() : null;

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
            extent = traceIfNeeded(reorderExtent = new MultiStageReorder(extent, false));
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
            this.sideEffectExtent = null;
            this.batchingExtent = null;
            this.chunkBatchingExtent = null;
            this.changeSetExtent = null;

            Extent extent = new NullExtent();
            extent = traceIfNeeded(survivalExtent = new SurvivalModeExtent(extent, NullWorld.getInstance()));
            extent = traceIfNeeded(blockBagExtent = new BlockBagExtent(extent, blockBag));
            extent = traceIfNeeded(reorderExtent = new MultiStageReorder(extent, false));
            extent = traceIfNeeded(maskingExtent = new MaskingExtent(extent, Masks.alwaysTrue()));
            extent = traceIfNeeded(changeLimiter = new BlockChangeLimiter(extent, maxBlocks));
            this.bypassReorderHistory = extent;
            this.bypassHistory = extent;
            this.bypassNone = extent;
        }
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

    /**
     * Check whether any buffering extent holds changes that still need to be committed.
     */
    public boolean commitRequired() {
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
     * Check whether the chain is being traced.
     */
    public boolean isTracing() {
        return tracingExtents != null;
    }

    /**
     * Get the current list of active tracing extents.
     */
    public List<TracingExtent> activeTracingExtents() {
        if (tracingExtents == null) {
            return ImmutableList.of();
        }
        return tracingExtents.stream()
            .filter(TracingExtent::isActive)
            .toList();
    }

    public @Nullable SideEffectExtent sideEffectExtent() {
        return sideEffectExtent;
    }

    public SurvivalModeExtent survivalExtent() {
        return survivalExtent;
    }

    public @Nullable BatchingExtent batchingExtent() {
        return batchingExtent;
    }

    public @Nullable ChunkBatchingExtent chunkBatchingExtent() {
        return chunkBatchingExtent;
    }

    public BlockBagExtent blockBagExtent() {
        return blockBagExtent;
    }

    @SuppressWarnings("deprecation")
    public MultiStageReorder reorderExtent() {
        return reorderExtent;
    }

    public MaskingExtent maskingExtent() {
        return maskingExtent;
    }

    public BlockChangeLimiter changeLimiter() {
        return changeLimiter;
    }

    public @Nullable ChangeSetExtent changeSetExtent() {
        return changeSetExtent;
    }

    public List<WatchdogTickingExtent> watchdogExtents() {
        return watchdogExtents;
    }

    /**
     * Get the extent that bypasses history and block re-ordering.
     */
    public Extent bypassReorderHistory() {
        return bypassReorderHistory;
    }

    /**
     * Get the extent that bypasses history, but still re-orders blocks.
     */
    public Extent bypassHistory() {
        return bypassHistory;
    }

    /**
     * Get the extent at the top of the chain, which bypasses nothing.
     */
    public Extent bypassNone() {
        return bypassNone;
    }
}
