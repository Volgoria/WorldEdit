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

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Ready-made patterns and masks built from the blocks a player picked.
 */
public final class Presets {

    /**
     * The largest value accepted for a preset parameter.
     */
    public static final int MAX_VALUE = 64;

    private Presets() {
    }

    /**
     * What presets are built from.
     *
     * @param blocks the picked blocks
     * @param value the preset parameter, e.g. a stripe thickness or a radius
     * @param x the player's block x coordinate
     * @param y the player's block y coordinate
     * @param z the player's block z coordinate
     */
    public record Context(List<String> blocks, int value, int x, int y, int z) {

        public Context {
            blocks = List.copyOf(blocks);
            checkArgument(value >= 1, "value must be positive");
        }

        /**
         * Create a context from a player's pattern selection.
         *
         * @param selection the selection
         * @param value the preset parameter
         * @param x the player's block x coordinate
         * @param y the player's block y coordinate
         * @param z the player's block z coordinate
         * @return the context
         */
        public static Context of(PatternSelection selection, int value, int x, int y, int z) {
            return new Context(selection.getPickedBlocks(), clampValue(value), x, y, z);
        }

        @Nullable
        String build(String template, int minBlocks) {
            if (blocks.size() < minBlocks) {
                return null;
            }
            Map<String, String> values = Map.of(
                "blocks", String.join(",", blocks),
                "b1", blocks.isEmpty() ? "" : blocks.get(0),
                "b2", blocks.size() < 2 ? "" : blocks.get(1),
                "value", Integer.toString(value),
                "x", Integer.toString(x),
                "y", Integer.toString(y),
                "y-1", Integer.toString(y - 1),
                "z", Integer.toString(z)
            );
            return CommandTemplate.substitute(template, values::get);
        }
    }

    /**
     * Clamp a preset parameter into {@code [1, MAX_VALUE]}.
     *
     * @param value the value
     * @return the clamped value
     */
    public static int clampValue(int value) {
        return Math.max(1, Math.min(MAX_VALUE, value));
    }

    /**
     * Pattern presets.
     */
    public enum PatternPreset {
        GRADIENT("Gradient", "Blends your blocks from the bottom to the top of the selection",
            "#gradient[{blocks}]", 2, "WHITE_GLAZED_TERRACOTTA"),
        LAYERS("Horizontal stripes", "Alternates your blocks in layers (thickness = value)",
            "#stripes[y][{blocks}][{value}]", 2, "SANDSTONE"),
        STRIPES("Vertical stripes", "Alternates your blocks along X (thickness = value)",
            "#stripes[x][{blocks}][{value}]", 2, "BIRCH_LOG"),
        DIAGONAL("Diagonal stripes", "Alternates your blocks diagonally (thickness = value)",
            "#stripes[xz][{blocks}][{value}]", 2, "QUARTZ_PILLAR"),
        CHECKER("Checkerboard", "Your first two blocks as a 3D checkerboard (cell = value)",
            "#checker[{b1}][{b2}][{value}]", 2, "CHISELED_QUARTZ_BLOCK"),
        NOISE("Noise", "Natural looking patches of your blocks (scale = value)",
            "#noise[{value}][{blocks}]", 2, "MOSSY_COBBLESTONE"),
        LINEAR("Alternating", "Cycles through your blocks block by block", "#linear[{blocks}]", 2, "REPEATER"),
        SLOPES("Slopes", "First block on steep slopes, second block elsewhere",
            "#mask[#angle[40][90]][{b1}][{b2}]", 2, "STONE");

        private final String displayName;
        private final String description;
        private final String template;
        private final int minBlocks;
        private final String icon;

        PatternPreset(String displayName, String description, String template, int minBlocks, String icon) {
            this.displayName = displayName;
            this.description = description;
            this.template = template;
            this.minBlocks = minBlocks;
            this.icon = icon;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }

        public String getIcon() {
            return icon;
        }

        public int getMinBlocks() {
            return minBlocks;
        }

        public boolean usesValue() {
            return template.contains("{value}");
        }

        /**
         * Build the pattern.
         *
         * @param context the player's choices
         * @return the pattern, or null if not enough blocks were picked
         */
        @Nullable
        public String build(Context context) {
            return context.build(template, minBlocks);
        }
    }

    /**
     * Mask presets.
     */
    public enum MaskPreset {
        BLOCKS("Your blocks", "Only the blocks you picked", "{blocks}", 1, "GRASS_BLOCK"),
        ABOVE("Above you", "Blocks at or above your feet", "#y[{y}][*]", 0, "LADDER"),
        BELOW("Below you", "Blocks below your feet", "#y[*][{y-1}]", 0, "DEEPSLATE"),
        EAST("East of you", "Blocks at or east of your X", "#x[{x}][*]", 0, "MAGENTA_GLAZED_TERRACOTTA"),
        SOUTH("South of you", "Blocks at or south of your Z", "#z[{z}][*]", 0, "LIGHT_BLUE_GLAZED_TERRACOTTA"),
        STEEP("Steep slopes", "Solid blocks on slopes of 40 to 90 degrees", "#angle[40][90]", 0, "STONE"),
        FLAT("Flat ground", "Solid blocks on slopes of 0 to 20 degrees", "#angle[0][20]", 0, "SMOOTH_STONE"),
        ADJACENT("Next to your blocks", "Blocks touching one of the blocks you picked", "#adjacent[{blocks}]", 1,
            "COBWEB"),
        EXPOSED("Next to air", "Blocks touching air", "#adjacent[air]", 0, "GLASS"),
        WALL("Walls", "Blocks with air on a side", "#wall", 0, "STONE_BRICK_WALL"),
        FLOOR("Floors", "Blocks with air above", "#floor", 0, "OAK_PRESSURE_PLATE"),
        CEILING("Ceilings", "Blocks with air below", "#ceiling", 0, "LANTERN"),
        LIQUID("Liquids", "Water, lava and other liquids", "#liquid", 0, "WATER_BUCKET"),
        RADIUS("Radius", "Blocks within value blocks of your placement position", "#radius[{value}]", 0,
            "TARGET");

        private final String displayName;
        private final String description;
        private final String template;
        private final int minBlocks;
        private final String icon;

        MaskPreset(String displayName, String description, String template, int minBlocks, String icon) {
            this.displayName = displayName;
            this.description = description;
            this.template = template;
            this.minBlocks = minBlocks;
            this.icon = icon;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }

        public String getIcon() {
            return icon;
        }

        public int getMinBlocks() {
            return minBlocks;
        }

        public boolean usesValue() {
            return template.contains("{value}");
        }

        /**
         * Build the mask.
         *
         * @param context the player's choices
         * @return the mask, or null if not enough blocks were picked
         */
        @Nullable
        public String build(Context context) {
            return context.build(template, minBlocks);
        }
    }
}
