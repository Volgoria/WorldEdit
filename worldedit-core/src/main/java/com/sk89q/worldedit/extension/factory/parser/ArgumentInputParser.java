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

package com.sk89q.worldedit.extension.factory.parser;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;
import com.sk89q.util.StringUtil;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.internal.registry.InputParser;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Base class for pattern and mask parsers that take arguments, with shared
 * helpers to parse numbers, nested patterns and masks, and to suggest
 * completions for them.
 *
 * @param <E> the type of the parsed element
 */
public abstract class ArgumentInputParser<E> extends InputParser<E> {

    private static final CharMatcher LIST_DELIMITER = CharMatcher.is(',');

    protected ArgumentInputParser(WorldEdit worldEdit) {
        super(worldEdit);
    }

    // Numbers

    /**
     * Filter the given options by the typed prefix.
     *
     * @param partial the typed text
     * @param options the options
     * @return the options starting with the typed text
     */
    protected static Stream<String> suggestFrom(String partial, String... options) {
        String lower = partial.toLowerCase(Locale.ROOT);
        return Stream.of(options).filter(s -> s.startsWith(lower));
    }

    /**
     * Parse an integer argument.
     *
     * @param argument the argument
     * @return the integer
     * @throws InputParseException if the argument is not an integer
     */
    protected static int parseInt(String argument) throws InputParseException {
        try {
            return Integer.parseInt(argument.trim());
        } catch (NumberFormatException _) {
            throw invalidNumber(argument);
        }
    }

    /**
     * Parse an integer argument that must lie within a range.
     *
     * @param argument the argument
     * @param what the name of the value, for error messages
     * @param min the minimum allowed value
     * @param max the maximum allowed value
     * @return the integer
     * @throws InputParseException if the argument is not an integer or out of range
     */
    protected static int parseInt(String argument, String what, int min, int max) throws InputParseException {
        int value = parseInt(argument);
        if (value < min || value > max) {
            throw outOfRange(what, argument, String.valueOf(min), String.valueOf(max));
        }
        return value;
    }

    /**
     * Parse a finite decimal argument.
     *
     * @param argument the argument
     * @return the number
     * @throws InputParseException if the argument is not a finite number
     */
    protected static double parseDouble(String argument) throws InputParseException {
        double value;
        try {
            value = Double.parseDouble(argument.trim());
        } catch (NumberFormatException _) {
            throw invalidNumber(argument);
        }
        if (!Double.isFinite(value)) {
            throw invalidNumber(argument);
        }
        return value;
    }

    /**
     * Parse a decimal argument that must lie within a range.
     *
     * @param argument the argument
     * @param what the name of the value, for error messages
     * @param min the minimum allowed value
     * @param max the maximum allowed value
     * @return the number
     * @throws InputParseException if the argument is not a number or out of range
     */
    protected static double parseDouble(String argument, String what, double min, double max) throws InputParseException {
        double value = parseDouble(argument);
        if (!(value >= min && value <= max)) {
            throw outOfRange(what, argument, formatNumber(min), formatNumber(max));
        }
        return value;
    }

    private static String formatNumber(double value) {
        if (value == Double.MAX_VALUE) {
            return "∞";
        }
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }

    private static InputParseException invalidNumber(String argument) {
        return new InputParseException(TranslatableComponent.of(
            "worldedit.error.invalid-number.matches", TextComponent.of(argument)
        ));
    }

    private static InputParseException outOfRange(String what, String argument, String min, String max) {
        return new InputParseException(TranslatableComponent.of(
            "worldedit.error.parser.bracket-args.out-of-range",
            TextComponent.of(what), TextComponent.of(argument), TextComponent.of(min), TextComponent.of(max)
        ));
    }

    // Nested patterns and masks

    /**
     * Parse a nested pattern.
     *
     * @param input the input
     * @param context the parser context
     * @return the pattern
     * @throws InputParseException if the pattern is invalid
     */
    protected Pattern parsePattern(String input, ParserContext context) throws InputParseException {
        return worldEdit.getPatternFactory().parseFromInput(input, context);
    }

    /**
     * Suggest completions for a nested pattern.
     *
     * @param partial the typed text
     * @param context the parser context
     * @return the suggestions
     */
    protected Stream<String> suggestPattern(String partial, ParserContext context) {
        return worldEdit.getPatternFactory().getSuggestions(partial, context).stream();
    }

    /**
     * Parse a nested mask, which may be an intersection of space separated masks.
     *
     * @param input the input
     * @param context the parser context
     * @return the mask
     * @throws InputParseException if the mask is invalid
     */
    protected Mask parseMask(String input, ParserContext context) throws InputParseException {
        return worldEdit.getMaskFactory().parseFromInput(input, context);
    }

    /**
     * Suggest completions for a nested mask.
     *
     * @param partial the typed text
     * @param context the parser context
     * @return the suggestions
     */
    protected Stream<String> suggestMask(String partial, ParserContext context) {
        return worldEdit.getMaskFactory().getSuggestions(partial, context).stream();
    }

    /**
     * Parse a comma separated list of patterns, e.g. {@code stone,dirt,#copy}.
     *
     * @param argument the argument
     * @param context the parser context
     * @return the patterns, never empty
     * @throws InputParseException if a pattern is invalid or the list is empty
     */
    protected List<Pattern> parsePatternList(String argument, ParserContext context) throws InputParseException {
        ImmutableList.Builder<Pattern> patterns = ImmutableList.builder();
        for (String part : StringUtil.splitOutsideBrackets(argument, LIST_DELIMITER)) {
            if (part.isEmpty()) {
                throw new InputParseException(TranslatableComponent.of(
                    "worldedit.error.parser.bracket-args.empty-entry", TextComponent.of(argument)
                ));
            }
            patterns.add(parsePattern(part, context));
        }
        return patterns.build();
    }

    /**
     * Suggest completions for a comma separated list of patterns.
     *
     * @param partial the typed text
     * @param context the parser context
     * @return suggestions for the whole list
     */
    protected Stream<String> suggestPatternList(String partial, ParserContext context) {
        // only commas outside of brackets separate entries
        List<String> parts = StringUtil.splitOutsideBrackets(partial, LIST_DELIMITER);
        String last = parts.get(parts.size() - 1);
        String prefix = partial.substring(0, partial.length() - last.length());
        return suggestPattern(last, context).map(s -> prefix + s);
    }
}
