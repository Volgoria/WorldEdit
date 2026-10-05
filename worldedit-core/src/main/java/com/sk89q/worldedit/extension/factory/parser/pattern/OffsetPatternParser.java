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
import com.sk89q.worldedit.function.pattern.OffsetPattern;
import com.sk89q.worldedit.function.pattern.Pattern;

import java.util.List;
import java.util.stream.Stream;

/**
 * Parses {@code #offset[<x>][<y>][<z>][<pattern>]}, using the block the
 * pattern gives at the offset position. For example,
 * {@code #offset[0][1][0][#existing]} copies the block above.
 */
public class OffsetPatternParser extends BracketArgumentParser<Pattern> {

    public OffsetPatternParser(WorldEdit worldEdit) {
        super(worldEdit, "#offset", 4, 4);
    }

    @Override
    public String getUsage() {
        return "#offset[<x>][<y>][<z>][<pattern>]";
    }

    @Override
    protected Pattern parseArguments(List<String> arguments, ParserContext context) throws InputParseException {
        return new OffsetPattern(parsePattern(arguments.get(3), context), parseBlockVector(arguments, 0));
    }

    @Override
    protected Stream<String> getArgumentSuggestions(int index, String partial, ParserContext context) {
        if (index < 3) {
            return suggestFrom(partial, OFFSET_SUGGESTIONS);
        }
        return suggestPattern(partial, context);
    }
}
