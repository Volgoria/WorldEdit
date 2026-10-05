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

package com.sk89q.worldedit.command.tool;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.registry.state.BooleanProperty;
import com.sk89q.worldedit.registry.state.Property;
import org.enginehub.linbus.tree.LinCompoundTag;
import org.enginehub.linbus.tree.LinIntTag;
import org.enginehub.linbus.tree.LinListTag;
import org.enginehub.linbus.tree.LinTagType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Tool algorithms")
class ToolAlgorithmsTest {

    @Test
    @DisplayName("measure computes path lengths")
    void measurePathLength() {
        assertEquals(0, MeasureTool.pathLength(List.of(BlockVector3.at(1, 2, 3))));
        assertEquals(5 + 10, MeasureTool.pathLength(List.of(
            BlockVector3.at(0, 0, 0),
            BlockVector3.at(3, 4, 0),
            BlockVector3.at(3, 4, 10)
        )), 1e-9);
    }

    @Test
    @DisplayName("measure computes bounding boxes and volumes")
    void measureBoundingBox() {
        List<BlockVector3> points = List.of(
            BlockVector3.at(0, 0, 0),
            BlockVector3.at(-4, 9, 2),
            BlockVector3.at(5, 3, -1)
        );
        assertEquals(BlockVector3.at(10, 10, 4), MeasureTool.boundingSize(points));
        assertEquals(400, MeasureTool.volume(points));
        assertEquals(1, MeasureTool.volume(List.of(BlockVector3.at(7, 7, 7))));
    }

    @Test
    @DisplayName("measure computes horizontal polygon areas")
    void measurePolygonArea() {
        assertEquals(0, MeasureTool.polygonArea(List.of(BlockVector3.ZERO, BlockVector3.at(5, 0, 0))));
        // A 10 x 4 rectangle, whatever the heights and winding
        assertEquals(40, MeasureTool.polygonArea(List.of(
            BlockVector3.at(0, 0, 0),
            BlockVector3.at(10, 5, 0),
            BlockVector3.at(10, 2, 4),
            BlockVector3.at(0, 1, 4)
        )), 1e-9);
        assertEquals(12.5, MeasureTool.polygonArea(List.of(
            BlockVector3.at(0, 0, 0),
            BlockVector3.at(0, 0, 5),
            BlockVector3.at(5, 0, 0)
        )), 1e-9);
    }

    @Test
    @DisplayName("inspect describes block properties")
    void inspectDescribesProperties() {
        Map<Property<?>, Object> states = new LinkedHashMap<>();
        states.put(new BooleanProperty("waterlogged", List.of(true, false)), false);
        states.put(new BooleanProperty("lit", List.of(true, false)), true);
        assertEquals("lit=true, waterlogged=false", InspectTool.describeProperties(states));
        assertEquals("", InspectTool.describeProperties(Map.of()));
    }

    @Test
    @DisplayName("inspect summarizes NBT data")
    void inspectSummarizesNbt() {
        LinCompoundTag tag = LinCompoundTag.builder()
            .putString("id", "minecraft:chest")
            .putInt("x", 4)
            .put("Items", LinListTag.of(LinTagType.intTag(), List.of(LinIntTag.of(1), LinIntTag.of(2))))
            .putCompound("components", Map.of("a", LinIntTag.of(1)))
            .build();
        assertEquals("Items (list[2]), components (compound{1}), id (string), x (int)", InspectTool.summarizeNbt(tag, 10));
        assertEquals("Items (list[2]), components (compound{1}), +2 more", InspectTool.summarizeNbt(tag, 2));
    }

    @Test
    @DisplayName("inspect only offers NBT text small enough for a chat packet")
    void inspectBoundsCopiedNbt() {
        assertTrue(InspectTool.isCopyable("{id:'minecraft:chest'}"));
        assertTrue(InspectTool.isCopyable("x".repeat(InspectTool.MAX_COPIED_NBT_LENGTH)));
        assertFalse(InspectTool.isCopyable("x".repeat(InspectTool.MAX_COPIED_NBT_LENGTH + 1)));
    }
}
