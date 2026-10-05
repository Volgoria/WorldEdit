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

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.TracingExtent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.collection.BlockMap;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reports to an actor which extents of a traced {@link EditSession} made block
 * changes fail.
 */
public final class TracingReport {

    /**
     * Tell the actor, for each distinct stack of extents that touched a failed
     * position, which extent failed and how.
     *
     * @param actor the actor to report to
     * @param tracingExtents the active tracing extents of the session
     */
    public static void report(Actor actor, List<TracingExtent> tracingExtents) {
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

    private TracingReport() {
    }
}
