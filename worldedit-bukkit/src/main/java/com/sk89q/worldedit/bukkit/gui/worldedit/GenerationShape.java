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
 * The shapes offered by the generation menu, mapped onto generation commands.
 */
public enum GenerationShape implements GuiAction {
    SPHERE("Sphere", "Radius = size", "//sphere {h}{pattern} {size}", "SLIME_BLOCK"),
    CYLINDER("Cylinder", "Radius = size, height = size", "//cyl {h}{pattern} {size} {size}", "CAULDRON"),
    CONE("Cone", "Radius = size, height = 2 x size", "//cone {h}{pattern} {size} {double}", "POINTED_DRIPSTONE"),
    PYRAMID("Pyramid", "Size = size", "//pyramid {h}{pattern} {size}", "SANDSTONE_STAIRS"),
    TORUS("Torus", "Ring radius = size, tube = size / 3", "//torus {h}{pattern} {size} {third}", "HEART_OF_THE_SEA"),
    DOME("Dome", "Radius = size", "//dome {h}{pattern} {size}", "TURTLE_HELMET"),
    DISK("Disk", "Radius = size, facing where you look", "//disk {h}{pattern} {size}",
        "HEAVY_WEIGHTED_PRESSURE_PLATE"),
    HELIX("Helix", "Radius = size, height = 2 x size, 3 turns", "//helix {pattern} {size} {double} 3",
        "TWISTING_VINES"),
    ARCH("Arch", "Width = 2 x size, height = size", "//arch {pattern} {double} {size}", "STONE_BRICK_STAIRS");

    private final ActionSpec spec;

    GenerationShape(String displayName, String description, String commandTemplate, String icon) {
        this.spec = ActionSpec.close(displayName, description, icon, commandTemplate);
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

    public boolean supportsHollow() {
        return spec.supportsHollow();
    }
}
