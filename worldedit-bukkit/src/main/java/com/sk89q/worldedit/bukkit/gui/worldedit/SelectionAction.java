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
public enum SelectionAction {
    WAND("Wand", "Get the selection wand", "//wand", true),
    POS1("Position 1 here", "Set position 1 to where you stand", "//pos1", false),
    POS2("Position 2 here", "Set position 2 to where you stand", "//pos2", false),
    HPOS1("Position 1 (target)", "Set position 1 to the block you look at", "//hpos1", false),
    HPOS2("Position 2 (target)", "Set position 2 to the block you look at", "//hpos2", false),
    SEL_CUBOID("Cuboid selection", "Switch to cuboid selection mode", "//sel cuboid", false),
    SEL_POLY("Polygon selection", "Switch to polygon selection mode", "//sel poly", false),
    DESELECT("Clear selection", "Clear your current selection", "//desel", false),
    EXPAND_VERT("Expand vertically", "Expand the selection to the world's height limits", "//expand vert", false),
    SIZE("Selection size", "Show the size of the selection", "//size", true),
    DISTR("Block distribution", "List the blocks in the selection", "//distr", true),
    SET("Set", "Fill the selection with the pattern", "//set {pattern}", false),
    REPLACE("Replace", "Replace the mask (or any non-air block) with the pattern", "//replace {mask}{pattern}", false),
    WALLS("Walls", "Build walls around the selection", "//walls {pattern}", false),
    FACES("Faces", "Build all six faces of the selection", "//faces {pattern}", false),
    WIREFRAME("Wireframe", "Build the edges of the selection", "//wireframe {pattern}", false),
    UNDO("Undo", "Undo your last edit", "//undo", false),
    REDO("Redo", "Redo your last undone edit", "//redo", false),
    HISTORY("History", "List your edit history", "//history", true);

    private final String displayName;
    private final String description;
    private final String commandTemplate;
    private final boolean closeAfter;

    SelectionAction(String displayName, String description, String commandTemplate, boolean closeAfter) {
        this.displayName = displayName;
        this.description = description;
        this.commandTemplate = commandTemplate;
        this.closeAfter = closeAfter;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getCommandTemplate() {
        return commandTemplate;
    }

    /**
     * Whether the menu should close after running the action, so the player
     * can read the chat output.
     *
     * @return true to close
     */
    public boolean closesMenu() {
        return closeAfter;
    }

    public boolean usesPattern() {
        return CommandTemplate.usesPattern(commandTemplate);
    }
}
