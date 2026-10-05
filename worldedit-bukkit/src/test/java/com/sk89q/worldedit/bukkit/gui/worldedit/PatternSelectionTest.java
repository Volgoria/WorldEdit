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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatternSelectionTest {

    @Test
    void defaultsToStone() {
        PatternSelection selection = new PatternSelection();
        assertEquals("stone", selection.toPattern());
        assertEquals(List.of("stone"), selection.getBlocks());
        assertFalse(selection.isCustom());
    }

    @Test
    void setReplacesAndNormalizes() {
        PatternSelection selection = new PatternSelection();
        selection.set("minecraft:OAK_PLANKS");
        assertEquals("oak_planks", selection.toPattern());
    }

    @Test
    void addBuildsARandomMix() {
        PatternSelection selection = new PatternSelection();
        assertTrue(selection.add("dirt"));
        assertTrue(selection.add("gravel"));
        assertFalse(selection.add("dirt"), "duplicates are rejected");
        assertEquals("stone,dirt,gravel", selection.toPattern());
    }

    @Test
    void mixIsCapped() {
        PatternSelection selection = new PatternSelection();
        for (int i = 1; i < PatternSelection.MAX_MIX; i++) {
            assertTrue(selection.add("block" + i));
        }
        assertFalse(selection.add("one_too_many"));
        assertEquals(PatternSelection.MAX_MIX, selection.getBlocks().size());
    }

    @Test
    void customPatternOverridesBlocks() {
        PatternSelection selection = new PatternSelection();
        selection.setCustom("50%stone,50%andesite");
        assertTrue(selection.isCustom());
        assertEquals("50%stone,50%andesite", selection.toPattern());
        assertTrue(selection.getBlocks().isEmpty());

        // Picking a block afterwards starts a fresh selection
        selection.add("dirt");
        assertFalse(selection.isCustom());
        assertEquals("dirt", selection.toPattern());

        selection.setCustom("##wool");
        selection.set("glass");
        assertEquals("glass", selection.toPattern());
    }

    @Test
    void resetGoesBackToDefault() {
        PatternSelection selection = new PatternSelection();
        selection.setCustom("##wool");
        selection.reset();
        assertEquals(PatternSelection.DEFAULT_BLOCK, selection.toPattern());
    }

    @Test
    void rejectsInvalidInput() {
        PatternSelection selection = new PatternSelection();
        assertThrows(IllegalArgumentException.class, () -> selection.set(" "));
        assertThrows(IllegalArgumentException.class, () -> selection.setCustom("stone dirt"));
        assertEquals("stone", selection.toPattern());
    }
}
