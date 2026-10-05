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
 * The selection and region actions offered by the selection menu.
 */
public enum SelectionAction implements GuiAction {
    WAND(ActionSpec.close("Wand", "Get the selection wand", "WOODEN_AXE", "//wand")),
    POS1(ActionSpec.run("Position 1 here", "Set position 1 to where you stand", "LIME_BANNER", "//pos1")),
    POS2(ActionSpec.run("Position 2 here", "Set position 2 to where you stand", "RED_BANNER", "//pos2")),
    HPOS1(ActionSpec.run("Position 1 (target)", "Set position 1 to the block you look at", "LIME_DYE", "//hpos1")),
    HPOS2(ActionSpec.run("Position 2 (target)", "Set position 2 to the block you look at", "RED_DYE", "//hpos2")),
    SEL_CUBOID(ActionSpec.run("Cuboid selection", "Switch to cuboid selection mode", "CHEST", "//sel cuboid")),
    SEL_POLY(ActionSpec.run("Polygon selection", "Switch to polygon selection mode", "PRISMARINE_SHARD", "//sel poly")),
    DESELECT(ActionSpec.run("Clear selection", "Clear your current selection", "BARRIER", "//desel")),
    EXPAND_VERT(ActionSpec.run("Expand vertically", "Expand the selection to the world's height limits", "LADDER",
        "//expand vert")),
    SIZE(ActionSpec.close("Selection size", "Show the size of the selection", "COMPASS", "//size")),
    DISTR(ActionSpec.close("Block distribution", "List the blocks in the selection", "MAP", "//distr")),
    SET(ActionSpec.run("Set", "Fill the selection with the pattern", "GRASS_BLOCK", "//set {pattern}")),
    REPLACE(ActionSpec.run("Replace", "Replace the mask (or any non-air block) with the pattern", "SHEARS",
        "//replace {mask}{pattern}")),
    WALLS(ActionSpec.run("Walls", "Build walls around the selection", "STONE_BRICK_WALL", "//walls {pattern}")),
    FACES(ActionSpec.run("Faces", "Build all six faces of the selection", "GLASS", "//faces {pattern}")),
    WIREFRAME(ActionSpec.run("Wireframe", "Build the edges of the selection", "IRON_BARS", "//wireframe {pattern}")),
    UNDO(ActionSpec.run("Undo", "Undo your last edit", "CLOCK", "//undo")),
    REDO(ActionSpec.run("Redo", "Redo your last undone edit", "RECOVERY_COMPASS", "//redo")),
    HISTORY(ActionSpec.close("History", "List your edit history", "BOOK", "//history"));

    private final ActionSpec spec;

    SelectionAction(ActionSpec spec) {
        this.spec = spec;
    }

    @Override
    public ActionSpec spec() {
        return spec;
    }

    public String getDisplayName() {
        return spec.displayName();
    }

    public String getDescription() {
        return spec.description();
    }

    public String getCommandTemplate() {
        return spec.template();
    }

    /**
     * Whether the menu should close after running the action, so the player
     * can read the chat output.
     *
     * @return true to close
     */
    public boolean closesMenu() {
        return spec.behavior() != ActionSpec.Behavior.RUN;
    }

    public boolean usesPattern() {
        return spec.usesPattern();
    }
}
