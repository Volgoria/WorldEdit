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

import com.google.common.collect.ImmutableList;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;

import java.util.List;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Base class for parsers of inputs that start with a fixed prefix followed
 * by an argument, such as {@code !<mask>}, {@code %<percent>} or
 * {@code ^<pattern>}.
 *
 * <p>When the input is empty, the prefixes are suggested. Once a prefix has
 * been typed, suggestions for the rest of the input are delegated to
 * {@link #getRemainderSuggestions(String, String, ParserContext)}.</p>
 *
 * @param <E> the type of the parsed element
 */
public abstract class PrefixParser<E> extends ArgumentInputParser<E> {

    private final List<String> prefixes;

    /**
     * Create a new parser.
     *
     * @param worldEdit the WorldEdit instance
     * @param prefixes the prefixes, checked in order
     */
    protected PrefixParser(WorldEdit worldEdit, String... prefixes) {
        super(worldEdit);
        checkArgument(prefixes.length > 0, "at least one prefix is required");
        this.prefixes = ImmutableList.copyOf(prefixes);
    }

    /**
     * Create the element from the input after the prefix.
     *
     * @param prefix the prefix that matched
     * @param remainder the input after the prefix, may be empty
     * @param context the parser context
     * @return the element, or {@code null} to let another parser handle the input
     * @throws InputParseException if the input is invalid
     */
    protected abstract E parseRemainder(String prefix, String remainder, ParserContext context) throws InputParseException;

    /**
     * Get suggestions for the input after the prefix. Returns nothing by default.
     *
     * @param prefix the prefix that matched
     * @param remainder the input after the prefix, may be empty
     * @param context the parser context
     * @return suggestions for the complete remainder
     */
    protected Stream<String> getRemainderSuggestions(String prefix, String remainder, ParserContext context) {
        return Stream.empty();
    }

    private String matchingPrefix(String input) {
        for (String prefix : prefixes) {
            if (input.startsWith(prefix)) {
                return prefix;
            }
        }
        return null;
    }

    @Override
    public E parseFromInput(String input, ParserContext context) throws InputParseException {
        String prefix = matchingPrefix(input);
        if (prefix == null) {
            return null;
        }
        return parseRemainder(prefix, input.substring(prefix.length()), context);
    }

    @Override
    public Stream<String> getSuggestions(String input, ParserContext context) {
        if (input.isEmpty()) {
            return prefixes.stream();
        }
        String prefix = matchingPrefix(input);
        if (prefix == null) {
            return Stream.empty();
        }
        return getRemainderSuggestions(prefix, input.substring(prefix.length()), context).map(s -> prefix + s);
    }
}
