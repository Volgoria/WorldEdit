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
 * Expands the small placeholder language used by the GUI's command templates.
 *
 * <p>Supported placeholders:</p>
 * <ul>
 *     <li>{@code {h}} - {@code "-h "} when hollow is requested, otherwise nothing</li>
 *     <li>{@code {pattern}} - the selected pattern</li>
 *     <li>{@code {mask}} - the selected mask followed by a space, or nothing</li>
 *     <li>{@code {size}} - the size</li>
 *     <li>{@code {size-1}} - the size minus one, at least 0</li>
 *     <li>{@code {double}} - twice the size</li>
 *     <li>{@code {third}} - a third of the size, at least 1</li>
 * </ul>
 */
final class CommandTemplate {

    private CommandTemplate() {
    }

    static String expand(String template, String pattern, @Nullable String mask, int size, boolean hollow) {
        return template
            .replace("{h}", hollow ? "-h " : "")
            .replace("{pattern}", pattern)
            .replace("{mask}", mask == null || mask.isBlank() ? "" : mask + " ")
            .replace("{size-1}", Integer.toString(Math.max(0, size - 1)))
            .replace("{size}", Integer.toString(size))
            .replace("{double}", Integer.toString(size * 2))
            .replace("{third}", Integer.toString(Math.max(1, size / 3)));
    }

    static boolean usesPattern(String template) {
        return template.contains("{pattern}");
    }

    static boolean usesSize(String template) {
        return template.contains("{size") || template.contains("{double}") || template.contains("{third}");
    }

    static boolean supportsHollow(String template) {
        return template.contains("{h}");
    }
}
