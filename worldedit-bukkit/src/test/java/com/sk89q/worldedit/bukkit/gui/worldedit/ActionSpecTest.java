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

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionSpecTest {

    private static List<GuiAction> allActions() {
        List<GuiAction> actions = new ArrayList<>();
        actions.addAll(List.of(BrushType.values()));
        actions.addAll(List.of(ToolAction.values()));
        actions.addAll(List.of(BuildAction.values()));
        actions.addAll(List.of(SelectionAction.values()));
        actions.addAll(List.of(GenerationShape.values()));
        return actions;
    }

    @Test
    void inputTemplatesNeedAQuestion() {
        assertFalse(ActionSpec.run("a", "b", "STONE", "//text {input}").isConsistent());
        assertTrue(ActionSpec.run("a", "b", "STONE", "//set stone").isConsistent());
        assertThrows(IllegalArgumentException.class,
            () -> ActionSpec.run("a", "b", "STONE", "//set stone").ask("?", InputKind.TEXT));
        assertThrows(IllegalArgumentException.class, () -> ActionSpec.run("a", "b", "STONE", "set stone"));
        ActionSpec spec = ActionSpec.close("a", "b", "STONE", "//text {pattern} {input}").ask("?", InputKind.TEXT);
        assertTrue(spec.needsInput());
        assertTrue(spec.usesPattern());
        assertFalse(spec.usesSize());
    }

    @Test
    void everyActionAsksForItsInput() {
        for (GuiAction action : allActions()) {
            assertTrue(action.spec().isConsistent(), action.toString());
        }
    }

    @Test
    void everyIconIsAMaterial() {
        for (GuiAction action : allActions()) {
            Material material = Material.getMaterial(action.spec().icon());
            assertNotNull(material, action + " icon " + action.spec().icon());
        }
        Stream.concat(
            Stream.of(Presets.PatternPreset.values()).map(Presets.PatternPreset::getIcon),
            Stream.of(Presets.MaskPreset.values()).map(Presets.MaskPreset::getIcon)
        ).forEach(icon -> assertNotNull(Material.getMaterial(icon), icon));
    }

    @Test
    void actionMenusFitTheirGrid() {
        // ActionMenu shows at most 28 actions in its default layout
        assertTrue(BrushType.values().length <= 28);
        assertTrue(ToolAction.values().length <= 28);
        assertTrue(BuildAction.values().length <= 28);
    }

    @Test
    void longRangeBuilderDescribesTheClicksItBinds() {
        // /tool lrbuild <a> <b> places <a> on right-click and <b> on left-click
        ActionSpec spec = ToolAction.LONG_RANGE_BUILD.spec();
        assertTrue(spec.template().endsWith("{pattern} air"), spec.template());
        assertTrue(spec.description().startsWith("Right-click: your pattern, left-click: air"), spec.description());
    }

    @Test
    void measuringTapeDescribesItsClicks() {
        // left-click starts a new measurement, right-click adds points to it
        assertEquals("Left-click: start measuring, right-click: add a point",
            ToolAction.MEASURE.spec().description());
    }

    @Test
    void textInput() {
        assertEquals("\"Hello world\"", InputKind.TEXT.toArgument("  Hello world "));
        assertEquals("\"-h\"", InputKind.TEXT.toArgument("-h"));
        assertNull(InputKind.TEXT.toArgument("say \"hi\""));
        assertNull(InputKind.TEXT.toArgument(""));
    }

    @Test
    void fileInputs() {
        assertEquals("castle", InputKind.FILE_NAME.toArgument(" castle "));
        assertEquals("builds/castle", InputKind.FILE_NAME.toArgument("builds/castle"));
        assertNull(InputKind.FILE_NAME.toArgument("castle.schem"));
        assertNull(InputKind.FILE_NAME.toArgument("../castle"));
        assertEquals("logo.png", InputKind.FILE.toArgument("logo.png"));
        assertNull(InputKind.FILE.toArgument("../logo.png"));
        assertEquals("trees,#clipboard", InputKind.FILE_LIST.toArgument("trees,#clipboard"));
        assertNull(InputKind.FILE_LIST.toArgument("trees, rocks"));
    }

    @Test
    void numberAndArgumentInputs() {
        assertEquals("-1", InputKind.NUMBER.toArgument("-1"));
        assertEquals("250", InputKind.NUMBER.toArgument(" 250 "));
        assertNull(InputKind.NUMBER.toArgument("1e5"));
        assertNull(InputKind.NUMBER.toArgument("-7"));
        assertEquals("#wall", InputKind.ARGUMENT.toArgument("#wall"));
        assertNull(InputKind.ARGUMENT.toArgument("stone dirt"));
    }

    @Test
    void everyInputKindHasAnErrorMessage() {
        for (InputKind kind : InputKind.values()) {
            assertFalse(kind.errorMessage().isBlank(), kind.name());
        }
    }
}
