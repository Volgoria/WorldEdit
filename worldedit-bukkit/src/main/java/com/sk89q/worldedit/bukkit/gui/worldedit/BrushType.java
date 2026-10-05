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

import javax.annotation.Nullable;

/**
 * The brushes offered by the GUI, mapped onto {@code /brush} sub-commands.
 */
public enum BrushType implements GuiAction {
    SPHERE("sphere", "Sphere", "Places a ball of the pattern", "{h}{pattern} {size}", "SLIME_BALL"),
    CYLINDER("cylinder", "Cylinder", "Places a cylinder (height = size)", "{h}{pattern} {size} {size}", "CAULDRON"),
    SMOOTH("smooth", "Smooth", "Softens terrain", "{size} 4", "FEATHER"),
    BLOB("blob", "Blob", "Places an organic, rough ball", "{pattern} {size}", "MAGMA_CREAM"),
    SPLATTER("splatter", "Splatter", "Scatters the pattern randomly", "{pattern} {size}", "INK_SAC"),
    OVERLAY("overlay", "Overlay", "Covers the surface with the pattern", "{pattern} {size}", "MOSS_CARPET"),
    FILL("fill", "Fill", "Fills holes downwards with the pattern", "{pattern} {size}", "BUCKET"),
    DRAIN("drain", "Drain", "Removes nearby liquids", "{size}", "SPONGE"),
    LINE("line", "Line", "Draws a line (thickness = size - 1)", "{h}{pattern} {size-1}", "STRING"),
    RAISE("raise", "Raise", "Raises terrain", "sphere {size}", "PISTON"),
    LOWER("lower", "Lower", "Lowers terrain", "sphere {size}", "STICKY_PISTON"),
    ERODE("erode", "Erode", "Erodes terrain", "{size}", "GRAVEL"),
    DILATE("dilate", "Dilate", "Dilates terrain", "{size}", "CLAY_BALL"),
    GRAVITY("gravity", "Gravity", "Makes blocks fall", "{size}", "SAND"),
    SNOW("snow", "Snow", "Lays down snow", "sphere {size}", "SNOWBALL"),
    EXTINGUISH("extinguish", "Extinguish", "Puts out fires", "{size}", "WATER_BUCKET"),
    FOREST("forest", "Forest", "Plants oak trees", "sphere {size} 20 oak", "OAK_SAPLING"),
    BUTCHER("butcher", "Butcher", "Kills hostile mobs", "{size}", "IRON_SWORD"),
    CLIPBOARD("clipboard", "Clipboard", "Pastes your clipboard (skipping air)", "-a", "PAPER"),
    SPLINE("spline", "Spline", "Click points, then the last one again, to build a tube (thickness = size - 1)",
        "{h}{pattern} {size-1}", "LEAD"),
    COPYPASTE("copypaste", "Copy-paste", "Copies a sphere on the first click, pastes it on the next (skipping air)",
        "-a {size}", "SHULKER_SHELL"),
    SHATTER("shatter", "Shatter", "Cracks the terrain into 8 fragments, filling the cracks with the pattern",
        "{pattern} {size} 8", "CRACKED_STONE_BRICKS"),
    SURFACE_SPLATTER("surfacesplatter", "Surface splatter", "Paints 6 random patches of the pattern on the surface",
        "{pattern} {size} 6 3", "BROWN_DYE"),
    LAYER("layer", "Layer", "Re-skins terrain: one layer per picked block, from the surface down",
        "{size} {layers}", "SANDSTONE"),
    POPULATE_SCHEMATIC("populateschem", "Populate schematics",
        "Scatters rotated schematics over the surface (density 5%)", "-r -a {input} {size} 5", "SPRUCE_SAPLING",
        "Schematic files or folders to scatter, separated with ',' (or #clipboard):", InputKind.FILE_LIST),
    COMMAND("command", "Command", "Runs WorldEdit commands where you click; {x} {y} {z} {size} are replaced",
        "-s {size} {input}", "COMMAND_BLOCK",
        "Commands to run, separated with ';', e.g. '//pos1 {x},{y},{z}; //pos2 {x},{y},{z}':", InputKind.TEXT);

    private final String subCommand;
    private final String displayName;
    private final String argumentTemplate;
    private final ActionSpec spec;

    BrushType(String subCommand, String displayName, String description, String argumentTemplate, String icon) {
        this(subCommand, displayName, description, argumentTemplate, icon, null, null);
    }

    BrushType(String subCommand, String displayName, String description, String argumentTemplate, String icon,
              @Nullable String question, @Nullable InputKind input) {
        this.subCommand = subCommand;
        this.displayName = displayName;
        this.argumentTemplate = argumentTemplate;
        this.spec = new ActionSpec(displayName + " brush", description, icon,
            "/brush " + subCommand + " " + argumentTemplate, ActionSpec.Behavior.BIND, question, input);
    }

    @Override
    public ActionSpec spec() {
        return spec;
    }

    public String getSubCommand() {
        return subCommand;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return spec.description();
    }

    public String getArgumentTemplate() {
        return argumentTemplate;
    }

    public boolean usesPattern() {
        return spec.usesPattern();
    }

    public boolean usesSize() {
        return spec.usesSize();
    }

    public boolean supportsHollow() {
        return spec.supportsHollow();
    }
}
