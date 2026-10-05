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
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.internal.registry.InputParser;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Base class for parsers of inputs of the form {@code #name[arg1][arg2]...}.
 *
 * <p>Arguments may themselves contain brackets (for example block states or
 * nested patterns), as long as they are balanced. Tab-completion is provided
 * for the name, for the argument currently being typed (delegated to
 * {@link #getArgumentSuggestions(int, String, ParserContext)}) and for the
 * opening bracket of the next argument.</p>
 *
 * @param <E> the type of the parsed element
 */
public abstract class BracketArgumentParser<E> extends InputParser<E> {

    private static final CharMatcher LIST_DELIMITER = CharMatcher.is(',');

    private final String name;
    private final int minArguments;
    private final int maxArguments;

    /**
     * Create a new parser.
     *
     * @param worldEdit the WorldEdit instance
     * @param name the name including its prefix, e.g. {@code #checker}; must be lower case
     * @param minArguments the minimum number of bracketed arguments
     * @param maxArguments the maximum number of bracketed arguments
     */
    protected BracketArgumentParser(WorldEdit worldEdit, String name, int minArguments, int maxArguments) {
        super(worldEdit);
        checkNotNull(name);
        checkArgument(name.equals(name.toLowerCase(Locale.ROOT)), "name must be lower case");
        checkArgument(minArguments >= 1 && minArguments <= maxArguments, "invalid argument count range");
        this.name = name;
        this.minArguments = minArguments;
        this.maxArguments = maxArguments;
    }

    /**
     * Get the name of this parser, including its prefix.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Get a short usage string, e.g. {@code #checker[<pattern>][<pattern>][size]}.
     *
     * @return the usage
     */
    public abstract String getUsage();

    /**
     * Create the element from the split arguments.
     *
     * @param arguments the arguments, between the minimum and maximum count
     * @param context the parser context
     * @return the element
     * @throws InputParseException if an argument is invalid
     */
    protected abstract E parseArguments(List<String> arguments, ParserContext context) throws InputParseException;

    /**
     * Get suggestions for a partially typed argument.
     *
     * @param index the index of the argument
     * @param partial the text typed so far for this argument
     * @param context the parser context
     * @return suggestions for the complete argument text
     */
    protected abstract Stream<String> getArgumentSuggestions(int index, String partial, ParserContext context);

    @Override
    public E parseFromInput(String input, ParserContext context) throws InputParseException {
        if (!matchesName(input)) {
            return null;
        }
        List<String> arguments = splitArguments(input, name.length());
        if (arguments.size() < minArguments || arguments.size() > maxArguments) {
            throw new InputParseException(TranslatableComponent.of(
                "worldedit.error.parser.bracket-args.wrong-count",
                TextComponent.of(name), TextComponent.of(getUsage())
            ));
        }
        return parseArguments(arguments, context);
    }

    private boolean matchesName(String input) {
        if (!input.regionMatches(true, 0, name, 0, name.length())) {
            return false;
        }
        return input.length() == name.length() || input.charAt(name.length()) == '[';
    }

    /**
     * Split {@code [a][b][c]} starting at the given index into its arguments.
     *
     * @param input the input
     * @param start the index of the first opening bracket
     * @return the arguments, without their brackets
     * @throws InputParseException if the brackets are malformed
     */
    public static List<String> splitArguments(String input, int start) throws InputParseException {
        List<String> arguments = new ArrayList<>();
        int i = start;
        while (i < input.length()) {
            if (input.charAt(i) != '[') {
                throw new InputParseException(TranslatableComponent.of(
                    "worldedit.error.parser.bracket-args.unexpected-text", TextComponent.of(input.substring(i))
                ));
            }
            int close = StringUtil.findMatchingCloseBracket(input, i);
            if (close == -1) {
                throw new InputParseException(TranslatableComponent.of(
                    "worldedit.error.parser.bracket-args.unclosed", TextComponent.of(input.substring(i))
                ));
            }
            arguments.add(input.substring(i + 1, close));
            i = close + 1;
        }
        return arguments;
    }

    @Override
    public Stream<String> getSuggestions(String input, ParserContext context) {
        String lower = input.toLowerCase(Locale.ROOT);
        if (name.startsWith(lower)) {
            return Stream.of(lower.length() == name.length() ? name + "[" : name);
        }
        if (!lower.startsWith(name + "[")) {
            return Stream.empty();
        }
        int count = 0;
        int i = name.length();
        while (i < input.length()) {
            if (input.charAt(i) != '[') {
                return Stream.empty();
            }
            int close = StringUtil.findMatchingCloseBracket(input, i);
            if (close == -1) {
                if (count >= maxArguments) {
                    return Stream.empty();
                }
                String prefix = input.substring(0, i + 1);
                return getArgumentSuggestions(count, input.substring(i + 1), context).map(s -> prefix + s);
            }
            count++;
            i = close + 1;
        }
        if (count < maxArguments) {
            return Stream.of(input + "[");
        }
        return Stream.empty();
    }

    // Helpers for subclasses

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
            throw new InputParseException(TranslatableComponent.of(
                "worldedit.error.invalid-number.matches", TextComponent.of(argument)
            ));
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
        double value;
        try {
            value = Double.parseDouble(argument.trim());
        } catch (NumberFormatException _) {
            throw new InputParseException(TranslatableComponent.of(
                "worldedit.error.invalid-number.matches", TextComponent.of(argument)
            ));
        }
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

    private static InputParseException outOfRange(String what, String argument, String min, String max) {
        return new InputParseException(TranslatableComponent.of(
            "worldedit.error.parser.bracket-args.out-of-range",
            TextComponent.of(what), TextComponent.of(argument), TextComponent.of(min), TextComponent.of(max)
        ));
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
            patterns.add(worldEdit.getPatternFactory().parseFromInput(part, context));
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
        return worldEdit.getPatternFactory().getSuggestions(last, context).stream().map(s -> prefix + s);
    }
}
