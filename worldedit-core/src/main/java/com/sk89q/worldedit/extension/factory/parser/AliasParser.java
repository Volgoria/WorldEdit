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
import com.sk89q.worldedit.internal.registry.SimpleInputParser;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A parser for argument-less inputs such as {@code #wall}, creating the
 * element with a function, so that simple masks and patterns need no
 * dedicated parser class.
 *
 * @param <E> the type of the parsed element
 */
public final class AliasParser<E> extends SimpleInputParser<E> {

    /**
     * Creates an element from the parser context.
     *
     * @param <E> the type of the element
     */
    @FunctionalInterface
    public interface Factory<E> {

        /**
         * Create the element.
         *
         * @param context the parser context
         * @return the element
         * @throws InputParseException if the context lacks something required
         */
        E create(ParserContext context) throws InputParseException;
    }

    private final Factory<? extends E> factory;
    private final List<String> aliases;

    /**
     * Create a new parser.
     *
     * @param worldEdit the WorldEdit instance
     * @param factory the function creating the element
     * @param aliases the aliases, the first being the primary one
     */
    public AliasParser(WorldEdit worldEdit, Factory<? extends E> factory, String... aliases) {
        super(worldEdit);
        checkNotNull(factory);
        checkArgument(aliases.length > 0, "at least one alias is required");
        this.factory = factory;
        this.aliases = ImmutableList.copyOf(aliases);
    }

    @Override
    public List<String> getMatchedAliases() {
        return aliases;
    }

    @Override
    public E parseFromSimpleInput(String input, ParserContext context) throws InputParseException {
        return factory.create(context);
    }
}
