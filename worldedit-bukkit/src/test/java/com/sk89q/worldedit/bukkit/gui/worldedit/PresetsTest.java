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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresetsTest {

    private static final Presets.Context TWO = new Presets.Context(List.of("stone", "andesite"), 3, 10, 64, -20);
    private static final Presets.Context THREE =
        new Presets.Context(List.of("stone", "andesite", "diorite"), 5, 0, -5, 0);
    private static final Presets.Context ONE = new Presets.Context(List.of("stone"), 4, 1, 2, 3);

    @Test
    void patternPresets() {
        // explicit levels, so the gradient does not need a selection: value blocks around the player
        assertEquals("#gradient[stone,andesite,diorite][-10][0]", Presets.PatternPreset.GRADIENT.build(THREE));
        // or the span of the player's selection
        assertEquals("#gradient[stone,andesite,diorite][40][70]",
            Presets.PatternPreset.GRADIENT.build(THREE.withGradientSpan(40, 70)));
        assertEquals("#stripes[y][stone,andesite][3]", Presets.PatternPreset.LAYERS.build(TWO));
        assertEquals("#stripes[x][stone,andesite][3]", Presets.PatternPreset.STRIPES.build(TWO));
        assertEquals("#stripes[xz][stone,andesite][3]", Presets.PatternPreset.DIAGONAL.build(TWO));
        assertEquals("#checker[stone][andesite][5]", Presets.PatternPreset.CHECKER.build(THREE));
        assertEquals("#noise[3][stone,andesite]", Presets.PatternPreset.NOISE.build(TWO));
        assertEquals("#linear[stone,andesite,diorite]", Presets.PatternPreset.LINEAR.build(THREE));
        assertEquals("#mask[#angle[40][90]][stone][andesite]", Presets.PatternPreset.SLOPES.build(TWO));
    }

    @ParameterizedTest
    @EnumSource(Presets.PatternPreset.class)
    void patternPresetsNeedTwoBlocks(Presets.PatternPreset preset) {
        assertNull(preset.build(ONE));
        String built = preset.build(TWO);
        assertNotNull(built);
        assertTrue(built.startsWith("#"), built);
        assertFalse(built.contains("{") || built.contains("}"), built);
        assertTrue(GuiCommands.isValidPatternOrMask(built), built);
    }

    @Test
    void maskPresets() {
        assertEquals("stone,andesite", Presets.MaskPreset.BLOCKS.build(TWO));
        assertEquals("#y[64][*]", Presets.MaskPreset.ABOVE.build(TWO));
        assertEquals("#y[*][63]", Presets.MaskPreset.BELOW.build(TWO));
        assertEquals("#y[*][-6]", Presets.MaskPreset.BELOW.build(THREE));
        assertEquals("#x[10][*]", Presets.MaskPreset.EAST.build(TWO));
        assertEquals("#z[-20][*]", Presets.MaskPreset.SOUTH.build(TWO));
        assertEquals("#angle[40][90]", Presets.MaskPreset.STEEP.build(ONE));
        assertEquals("#angle[0][20]", Presets.MaskPreset.FLAT.build(ONE));
        assertEquals("#adjacent[stone]", Presets.MaskPreset.ADJACENT.build(ONE));
        assertEquals("#adjacent[air]", Presets.MaskPreset.EXPOSED.build(ONE));
        assertEquals("#wall", Presets.MaskPreset.WALL.build(ONE));
        assertEquals("#floor", Presets.MaskPreset.FLOOR.build(ONE));
        assertEquals("#ceiling", Presets.MaskPreset.CEILING.build(ONE));
        assertEquals("#liquid", Presets.MaskPreset.LIQUID.build(ONE));
        assertEquals("#radius[4]", Presets.MaskPreset.RADIUS.build(ONE));
    }

    @ParameterizedTest
    @EnumSource(Presets.MaskPreset.class)
    void everyMaskPresetExpands(Presets.MaskPreset preset) {
        String built = preset.build(TWO);
        assertNotNull(built);
        assertFalse(built.contains("{") || built.contains("}"), built);
        assertTrue(GuiCommands.isValidPatternOrMask(built), built);
    }

    @Test
    void blockMasksNeedABlock() {
        Presets.Context none = new Presets.Context(List.of(), 1, 0, 0, 0);
        assertNull(Presets.MaskPreset.BLOCKS.build(none));
        assertNull(Presets.MaskPreset.ADJACENT.build(none));
        assertEquals("#wall", Presets.MaskPreset.WALL.build(none));
    }

    @Test
    void contextFromSelectionKeepsPickedBlocksAfterCustomPattern() {
        PatternSelection selection = new PatternSelection();
        selection.set("stone");
        selection.add("dirt");
        selection.setCustom("#gradient[stone,dirt]");
        Presets.Context context = Presets.Context.of(selection, 500, 1, 2, 3);
        assertEquals(List.of("stone", "dirt"), context.blocks());
        assertEquals(Presets.MAX_VALUE, context.value());
        assertEquals("#linear[stone,dirt]", Presets.PatternPreset.LINEAR.build(context));
    }

    @Test
    void valueIsClamped() {
        assertEquals(1, Presets.clampValue(-3));
        assertEquals(7, Presets.clampValue(7));
        assertEquals(Presets.MAX_VALUE, Presets.clampValue(1000));
        assertThrows(IllegalArgumentException.class, () -> new Presets.Context(List.of("stone"), 0, 0, 0, 0));

        PlayerGuiState state = new PlayerGuiState();
        state.setPresetValue(0);
        assertEquals(1, state.getPresetValue());
    }
}
