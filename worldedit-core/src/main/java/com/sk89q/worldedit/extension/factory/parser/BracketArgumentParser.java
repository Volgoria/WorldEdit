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

import com.sk89q.util.StringUtil;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.math.BlockVector3;
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
public abstract class BracketArgumentParser<E> extends ArgumentInputParser<E> {

    /**
     * Suggestions for a small block offset along one axis.
     */
    protected static final String[] OFFSET_SUGGESTIONS = {"-2", "-1", "0", "1", "2"};

    private final String name;
    private final int minArguments;
    private final int maxArguments;

    /**
     * Create a new parser.
     *
     * @param worldEdit the WorldEdit instance
     * @param name the name including its prefix, e.g. {@code #checker}; must be lower case
     * @param minArguments the minimum number of bracketed arguments, may be 0
     * @param maxArguments the maximum number of bracketed arguments
     */
    protected BracketArgumentParser(WorldEdit worldEdit, String name, int minArguments, int maxArguments) {
        super(worldEdit);
        checkNotNull(name);
        checkArgument(name.equals(name.toLowerCase(Locale.ROOT)), "name must be lower case");
        checkArgument(minArguments >= 0 && minArguments <= maxArguments, "invalid argument count range");
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
            throw wrongArgumentCount();
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
            if (lower.length() == name.length()) {
                return maxArguments == 0 ? Stream.of(name) : Stream.of(name + "[");
            }
            return Stream.of(name);
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

    /**
     * Create the exception thrown when the number of arguments is wrong.
     *
     * @return the exception
     */
    protected InputParseException wrongArgumentCount() {
        return new InputParseException(TranslatableComponent.of(
            "worldedit.error.parser.bracket-args.wrong-count",
            TextComponent.of(name), TextComponent.of(getUsage())
        ));
    }

    /**
     * Parse three integer arguments, starting at the given index, into a vector.
     *
     * @param arguments the arguments
     * @param start the index of the X argument
     * @return the vector
     * @throws InputParseException if an argument is not an integer
     */
    protected static BlockVector3 parseBlockVector(List<String> arguments, int start) throws InputParseException {
        return BlockVector3.at(
            parseInt(arguments.get(start)),
            parseInt(arguments.get(start + 1)),
            parseInt(arguments.get(start + 2))
        );
    }
}
