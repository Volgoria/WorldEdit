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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

/**
 * Expands the small placeholder language used by the GUI's command templates.
 *
 * <p>Supported placeholders:</p>
 * <ul>
 *     <li>{@code {h}} - {@code "-h "} when hollow is requested, otherwise nothing</li>
 *     <li>{@code {pattern}} - the selected pattern, quoted if it contains spaces</li>
 *     <li>{@code {layers}} - the selected blocks separated by spaces, or the
 *     pattern if it was typed by hand</li>
 *     <li>{@code {mask}} - the selected mask (quoted if it contains spaces)
 *     followed by a space, or nothing</li>
 *     <li>{@code {size}} - the size</li>
 *     <li>{@code {size-1}} - the size minus one, at least 0</li>
 *     <li>{@code {double}} - twice the size</li>
 *     <li>{@code {third}} - a third of the size, at least 1</li>
 *     <li>{@code {input}} - the text the player typed in chat, already
 *     turned into a command argument</li>
 * </ul>
 *
 * <p>Expansion is done in a single pass, so values (for example a pattern or
 * text typed by the player) are never expanded again.</p>
 */
final class CommandTemplate {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([a-z0-9-]+)}");

    /**
     * Shown in previews in place of {@code {input}}.
     */
    static final String INPUT_PREVIEW = "<...>";

    private CommandTemplate() {
    }

    static String expand(String template, CommandContext context, @Nullable String input) {
        Map<String, String> values = new HashMap<>();
        values.put("h", context.hollow() ? "-h " : "");
        values.put("pattern", GuiCommands.quote(context.pattern()));
        values.put("layers", context.blocks().isEmpty() ? GuiCommands.quote(context.pattern()) : context.layers());
        String mask = context.mask();
        values.put("mask", mask == null || mask.isBlank() ? "" : GuiCommands.quote(mask) + " ");
        int size = context.size();
        values.put("size", Integer.toString(size));
        values.put("size-1", Integer.toString(Math.max(0, size - 1)));
        values.put("double", Integer.toString(size * 2));
        values.put("third", Integer.toString(Math.max(1, size / 3)));
        values.put("input", input == null ? INPUT_PREVIEW : input);
        return substitute(template, values::get);
    }

    /**
     * Replace every {@code {name}} for which the lookup returns a value.
     * Unknown placeholders are kept as they are.
     *
     * @param template the template
     * @param lookup the placeholder values
     * @return the expanded string
     */
    static String substitute(String template, Function<String, String> lookup) {
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder sb = new StringBuilder(template.length() + 16);
        while (matcher.find()) {
            String value = lookup.apply(matcher.group(1));
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value == null ? matcher.group() : value));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    static boolean usesPattern(String template) {
        return template.contains("{pattern}") || template.contains("{layers}");
    }

    static boolean usesMask(String template) {
        return template.contains("{mask}");
    }

    static boolean usesSize(String template) {
        return template.contains("{size") || template.contains("{double}") || template.contains("{third}");
    }

    static boolean supportsHollow(String template) {
        return template.contains("{h}");
    }

    static boolean usesInput(String template) {
        return template.contains("{input}");
    }
}
