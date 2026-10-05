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
 * The {@code /tool} tools offered by the tools menu. Each one is bound to the
 * held item.
 */
public enum ToolAction implements GuiAction {
    SELECTION_WAND(ActionSpec.bind("Selection wand", "Left/right-click to set positions 1 and 2",
        "WOODEN_AXE", "/tool selwand")),
    NAVIGATION_WAND(ActionSpec.bind("Navigation wand", "Left-click: jump to target, right-click: pass through walls",
        "COMPASS", "/tool navwand")),
    FAR_WAND(ActionSpec.bind("Far wand", "Selects positions at a distance", "SPYGLASS", "/tool farwand")),
    INFO(ActionSpec.bind("Block info", "Shows the block you click", "BOOK", "/tool info")),
    INSPECT(ActionSpec.bind("Inspector", "Shows state, NBT data, biome and light of a block",
        "KNOWLEDGE_BOOK", "/tool inspect")),
    MEASURE(ActionSpec.bind("Measuring tape", "Click two points to measure the distance between them",
        "RECOVERY_COMPASS", "/tool measure")),
    TREE(ActionSpec.bind("Tree planter", "Grows a tree where you click", "OAK_SAPLING", "/tool tree")),
    REPLACER(ActionSpec.bind("Replacer", "Replaces the clicked block with your pattern", "SHEARS",
        "/tool repl {pattern}")),
    LONG_RANGE_BUILD(ActionSpec.bind("Long-range builder", "Left-click: your pattern, right-click: air, at range",
        "BOW", "/tool lrbuild {pattern} air")),
    CYCLER(ActionSpec.bind("Data cycler", "Cycles the block states of the clicked block", "REPEATER",
        "/tool cycler")),
    FLOOD_FILL(ActionSpec.bind("Flood fill", "Floods the clicked area with your pattern (range = size)",
        "WATER_BUCKET", "/tool floodfill {pattern} {size}")),
    DELETE_TREE(ActionSpec.bind("Floating tree remover", "Removes the floating tree you click",
        "IRON_AXE", "/tool deltree")),
    STACKER(ActionSpec.bind("Stacker", "Stacks the clicked block until it hits something (range = size)",
        "SCAFFOLDING", "/tool stacker {size}")),
    COPY_PASTE(ActionSpec.bind("Copy-paste tool", "Copies your selection, then pastes it where you click",
        "SHULKER_BOX", "/tool copypaste"));

    private final ActionSpec spec;

    ToolAction(ActionSpec spec) {
        this.spec = spec;
    }

    @Override
    public ActionSpec spec() {
        return spec;
    }
}
