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
 * The brushes offered by the GUI, mapped onto {@code /brush} sub-commands.
 */
public enum BrushType {
    SPHERE("sphere", "Sphere", "Places a ball of the pattern", "{h}{pattern} {size}"),
    CYLINDER("cylinder", "Cylinder", "Places a cylinder (height = size)", "{h}{pattern} {size} {size}"),
    SMOOTH("smooth", "Smooth", "Softens terrain", "{size} 4"),
    BLOB("blob", "Blob", "Places an organic, rough ball", "{pattern} {size}"),
    SPLATTER("splatter", "Splatter", "Scatters the pattern randomly", "{pattern} {size}"),
    OVERLAY("overlay", "Overlay", "Covers the surface with the pattern", "{pattern} {size}"),
    FILL("fill", "Fill", "Fills holes downwards with the pattern", "{pattern} {size}"),
    DRAIN("drain", "Drain", "Removes nearby liquids", "{size}"),
    LINE("line", "Line", "Draws a line (thickness = size - 1)", "{h}{pattern} {size-1}"),
    RAISE("raise", "Raise", "Raises terrain", "sphere {size}"),
    LOWER("lower", "Lower", "Lowers terrain", "sphere {size}"),
    ERODE("erode", "Erode", "Erodes terrain", "{size}"),
    DILATE("dilate", "Dilate", "Dilates terrain", "{size}"),
    GRAVITY("gravity", "Gravity", "Makes blocks fall", "{size}"),
    SNOW("snow", "Snow", "Lays down snow", "sphere {size}"),
    EXTINGUISH("extinguish", "Extinguish", "Puts out fires", "{size}"),
    FOREST("forest", "Forest", "Plants oak trees", "sphere {size} 20 oak"),
    BUTCHER("butcher", "Butcher", "Kills hostile mobs", "{size}"),
    CLIPBOARD("clipboard", "Clipboard", "Pastes your clipboard (skipping air)", "-a");

    private final String subCommand;
    private final String displayName;
    private final String description;
    private final String argumentTemplate;

    BrushType(String subCommand, String displayName, String description, String argumentTemplate) {
        this.subCommand = subCommand;
        this.displayName = displayName;
        this.description = description;
        this.argumentTemplate = argumentTemplate;
    }

    public String getSubCommand() {
        return subCommand;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getArgumentTemplate() {
        return argumentTemplate;
    }

    public boolean usesPattern() {
        return CommandTemplate.usesPattern(argumentTemplate);
    }

    public boolean usesSize() {
        return CommandTemplate.usesSize(argumentTemplate);
    }

    public boolean supportsHollow() {
        return CommandTemplate.supportsHollow(argumentTemplate);
    }
}
