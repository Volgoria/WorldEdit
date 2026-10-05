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

package com.sk89q.worldedit.extension.factory.parser.pattern;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.factory.parser.BracketArgumentParser;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.pattern.ConditionalPattern;
import com.sk89q.worldedit.function.pattern.ExistingBlockPattern;
import com.sk89q.worldedit.function.pattern.Pattern;

import java.util.List;
import java.util.stream.Stream;

/**
 * Parses {@code #mask[<mask>][<pattern>]} and
 * {@code #mask[<mask>][<pattern>][<else pattern>]}, using the first pattern
 * where the mask matches and the second one elsewhere. Without a second
 * pattern, other blocks are left unchanged.
 *
 * <p>For example, {@code #mask[#angle[40][90]][stone][grass_block]} textures
 * steep slopes with stone.</p>
 */
public class ConditionalPatternParser extends BracketArgumentParser<Pattern> {

    public ConditionalPatternParser(WorldEdit worldEdit) {
        super(worldEdit, "#mask", 2, 3);
    }

    @Override
    public String getUsage() {
        return "#mask[<mask>][<pattern>][else pattern]";
    }

    @Override
    protected Pattern parseArguments(List<String> arguments, ParserContext context) throws InputParseException {
        return new ConditionalPattern(
            parseMask(arguments.get(0), context),
            parsePattern(arguments.get(1), context),
            arguments.size() > 2
                ? parsePattern(arguments.get(2), context)
                : new ExistingBlockPattern(context.requireExtent())
        );
    }

    @Override
    protected Stream<String> getArgumentSuggestions(int index, String partial, ParserContext context) {
        if (index == 0) {
            return suggestMask(partial, context);
        }
        return suggestPattern(partial, context);
    }
}
