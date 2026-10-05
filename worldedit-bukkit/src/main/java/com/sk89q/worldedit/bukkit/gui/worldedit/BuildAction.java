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

/**
 * The builder commands offered by the build menu.
 */
public enum BuildAction implements GuiAction {
    TEXT(ActionSpec.close("Text", "Writes text with your pattern (letter pixel = size / 3)", "OAK_SIGN",
        "//text -s {third} {pattern} {input}").ask("Type the text to write:", InputKind.TEXT)),
    SYMMETRY(ActionSpec.close("Symmetry", "Overwrites the half of the selection you look at with a mirror of the other",
        "GLASS_PANE", "//symmetry")),
    SYMMETRY_NO_AIR(ActionSpec.close("Symmetry (skip air)", "Like symmetry, without copying air", "WHITE_STAINED_GLASS_PANE",
        "//symmetry -a")),
    FLATTEN(ActionSpec.close("Flatten", "Flattens the selection to its average surface height", "SMOOTH_STONE_SLAB",
        "//flatten")),
    TERRAIN(ActionSpec.close("Terrain", "Generates hills in the selection (height = size)", "GRASS_BLOCK",
        "//terrain {pattern} 32 {size}")),
    PATH(ActionSpec.close("Path", "Lays a path through the selection's points (width = size)", "DIRT_PATH",
        "//path {pattern} {size}")),
    CAVE(ActionSpec.close("Cave", "Carves a winding cave in the selection (radius = size / 3)", "DEEPSLATE",
        "//cave {third}")),
    STAIRCASE(ActionSpec.close("Staircase", "Builds stairs up where you look (steps = size)", "COBBLESTONE_STAIRS",
        "//staircase {pattern} {size}")),
    SPIRAL_STAIRS(ActionSpec.close("Spiral stairs", "Builds a spiral staircase around you (radius = size, 2 turns)",
        "OAK_STAIRS", "//spiralstairs {pattern} {size} 32")),
    RANDOMIZE(ActionSpec.close("Randomize", "Replaces 20% of the exposed blocks of the selection with your pattern",
        "MOSSY_COBBLESTONE", "//randomize {pattern} 20"));

    private final ActionSpec spec;

    BuildAction(ActionSpec spec) {
        this.spec = spec;
    }

    @Override
    public ActionSpec spec() {
        return spec;
    }
}
