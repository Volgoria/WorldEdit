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

package com.sk89q.worldedit.bukkit.gui.worldedit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiCommandsTest {

    @Test
    void brushCommands() {
        assertEquals("/brush sphere stone 5", GuiCommands.brush(BrushType.SPHERE, "stone", 5, false));
        assertEquals("/brush sphere -h stone 5", GuiCommands.brush(BrushType.SPHERE, "stone", 5, true));
        assertEquals("/brush cylinder -h stone,dirt 4 4",
            GuiCommands.brush(BrushType.CYLINDER, "stone,dirt", 4, true));
        assertEquals("/brush smooth 3 4", GuiCommands.brush(BrushType.SMOOTH, "stone", 3, false));
        assertEquals("/brush overlay grass_block 6", GuiCommands.brush(BrushType.OVERLAY, "grass_block", 6, false));
        assertEquals("/brush fill water 2", GuiCommands.brush(BrushType.FILL, "water", 2, false));
        assertEquals("/brush drain 7", GuiCommands.brush(BrushType.DRAIN, "stone", 7, false));
        assertEquals("/brush blob stone 5", GuiCommands.brush(BrushType.BLOB, "stone", 5, false));
        assertEquals("/brush raise sphere 5", GuiCommands.brush(BrushType.RAISE, "stone", 5, false));
        assertEquals("/brush clipboard -a", GuiCommands.brush(BrushType.CLIPBOARD, "stone", 5, false));
        assertEquals("/brush none", GuiCommands.unbindBrush());
    }

    @Test
    void lineThicknessIsSizeMinusOne() {
        assertEquals("/brush line stone 0", GuiCommands.brush(BrushType.LINE, "stone", 1, false));
        assertEquals("/brush line -h stone 2", GuiCommands.brush(BrushType.LINE, "stone", 3, true));
    }

    @Test
    void hollowIsIgnoredWhenUnsupported() {
        assertEquals("/brush blob stone 5", GuiCommands.brush(BrushType.BLOB, "stone", 5, true));
        assertEquals("//helix stone 4 8 3", GuiCommands.generate(GenerationShape.HELIX, "stone", 4, true));
    }

    @Test
    void brushMetadataMatchesTemplates() {
        assertTrue(BrushType.SPHERE.usesPattern());
        assertTrue(BrushType.SPHERE.supportsHollow());
        assertFalse(BrushType.SMOOTH.usesPattern());
        assertFalse(BrushType.CLIPBOARD.usesSize());
        assertTrue(BrushType.DRAIN.usesSize());
    }

    @ParameterizedTest
    @EnumSource(BrushType.class)
    void everyBrushExpandsAllPlaceholders(BrushType type) {
        String command = GuiCommands.brush(type, "stone", 5, true);
        assertTrue(command.startsWith("/brush " + type.getSubCommand()), command);
        assertFalse(command.contains("{") || command.contains("}"), command);
        assertFalse(command.endsWith(" "), command);
    }

    @Test
    void generationCommands() {
        assertEquals("//sphere stone 5", GuiCommands.generate(GenerationShape.SPHERE, "stone", 5, false));
        assertEquals("//sphere -h glass 10", GuiCommands.generate(GenerationShape.SPHERE, "glass", 10, true));
        assertEquals("//cyl stone 5 5", GuiCommands.generate(GenerationShape.CYLINDER, "stone", 5, false));
        assertEquals("//cone -h stone 5 10", GuiCommands.generate(GenerationShape.CONE, "stone", 5, true));
        assertEquals("//pyramid sandstone 8", GuiCommands.generate(GenerationShape.PYRAMID, "sandstone", 8, false));
        assertEquals("//torus stone 9 3", GuiCommands.generate(GenerationShape.TORUS, "stone", 9, false));
        assertEquals("//torus stone 2 1", GuiCommands.generate(GenerationShape.TORUS, "stone", 2, false));
        assertEquals("//dome -h glass 6", GuiCommands.generate(GenerationShape.DOME, "glass", 6, true));
        assertEquals("//disk stone 4", GuiCommands.generate(GenerationShape.DISK, "stone", 4, false));
        assertEquals("//arch stone_bricks 10 5", GuiCommands.generate(GenerationShape.ARCH, "stone_bricks", 5, false));
    }

    @ParameterizedTest
    @EnumSource(GenerationShape.class)
    void everyShapeExpandsAllPlaceholders(GenerationShape shape) {
        String command = GuiCommands.generate(shape, "stone", 5, true);
        assertTrue(command.startsWith("//"), command);
        assertFalse(command.contains("{") || command.contains("}"), command);
    }

    @Test
    void selectionCommands() {
        assertEquals("//wand", GuiCommands.selection(SelectionAction.WAND, "stone", null));
        assertEquals("//expand vert", GuiCommands.selection(SelectionAction.EXPAND_VERT, "stone", null));
        assertEquals("//set oak_planks", GuiCommands.selection(SelectionAction.SET, "oak_planks", null));
        assertEquals("//replace stone", GuiCommands.selection(SelectionAction.REPLACE, "stone", null));
        assertEquals("//replace stone", GuiCommands.selection(SelectionAction.REPLACE, "stone", " "));
        assertEquals("//replace dirt,grass_block stone",
            GuiCommands.selection(SelectionAction.REPLACE, "stone", "dirt,grass_block"));
        assertEquals("//walls cobblestone", GuiCommands.selection(SelectionAction.WALLS, "cobblestone", null));
        assertEquals("//wireframe glass", GuiCommands.selection(SelectionAction.WIREFRAME, "glass", null));
        assertEquals("//undo", GuiCommands.selection(SelectionAction.UNDO, "stone", "dirt"));
        assertEquals("//history", GuiCommands.selection(SelectionAction.HISTORY, "stone", null));
    }

    @ParameterizedTest
    @EnumSource(SelectionAction.class)
    void everySelectionActionExpandsAllPlaceholders(SelectionAction action) {
        String command = GuiCommands.selection(action, "stone", "dirt");
        assertTrue(command.startsWith("//"), command);
        assertFalse(command.contains("{") || command.contains("}"), command);
    }

    @Test
    void schematicCommands() {
        assertEquals("/schem load castle.schem", GuiCommands.schematicLoad("castle.schem"));
        assertEquals("/schem load builds/castle.schem", GuiCommands.schematicLoad("builds/castle.schem"));
        assertEquals("/schem load \"my castle.schem\"", GuiCommands.schematicLoad("my castle.schem"));
        assertEquals("/schem save house", GuiCommands.schematicSave("house", false));
        assertEquals("/schem save -f house", GuiCommands.schematicSave("house", true));
    }

    @Test
    void quoting() {
        assertEquals("plain", GuiCommands.quote("plain"));
        assertEquals("\"\"", GuiCommands.quote(""));
        assertEquals("\"a b\"", GuiCommands.quote("a b"));
        assertEquals("\"say \\\"hi\\\"\"", GuiCommands.quote("say \"hi\""));
    }

    @Test
    void schematicNameValidation() {
        assertTrue(GuiCommands.isValidSchematicName("house"));
        assertTrue(GuiCommands.isValidSchematicName("my-house_2"));
        assertTrue(GuiCommands.isValidSchematicName("builds/house"));
        assertFalse(GuiCommands.isValidSchematicName(""));
        assertFalse(GuiCommands.isValidSchematicName("../secret"));
        assertFalse(GuiCommands.isValidSchematicName("/abs"));
        assertFalse(GuiCommands.isValidSchematicName("dir/"));
        assertFalse(GuiCommands.isValidSchematicName("a b"));
        assertFalse(GuiCommands.isValidSchematicName("house.schem"));
        assertFalse(GuiCommands.isValidSchematicName("x".repeat(65)));
    }

    @Test
    void singleArgument() {
        assertTrue(GuiCommands.isSingleArgument("50%stone,50%dirt"));
        assertTrue(GuiCommands.isSingleArgument("##wool"));
        assertFalse(GuiCommands.isSingleArgument(""));
        assertFalse(GuiCommands.isSingleArgument("stone dirt"));
    }

    @Test
    void clampSize() {
        assertEquals(1, GuiCommands.clampSize(0, 6));
        assertEquals(1, GuiCommands.clampSize(-4, 6));
        assertEquals(5, GuiCommands.clampSize(5, 6));
        assertEquals(6, GuiCommands.clampSize(9, 6));
        // Unlimited configuration falls back to the GUI's hard limit
        assertEquals(50, GuiCommands.clampSize(50, -1));
        assertEquals(GuiCommands.HARD_MAX_SIZE, GuiCommands.clampSize(1000, -1));
        assertEquals(GuiCommands.HARD_MAX_SIZE, GuiCommands.clampSize(1000, 500));
    }
}
