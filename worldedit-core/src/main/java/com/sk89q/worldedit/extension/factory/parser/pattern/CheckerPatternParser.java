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
import com.sk89q.worldedit.function.pattern.CheckerPattern;
import com.sk89q.worldedit.function.pattern.Pattern;

import java.util.List;
import java.util.stream.Stream;

/**
 * Parses {@code #checker[<pattern>][<pattern>]} and {@code #checker[<pattern>][<pattern>][<size>]}.
 */
public class CheckerPatternParser extends BracketArgumentParser<Pattern> {

    public CheckerPatternParser(WorldEdit worldEdit) {
        super(worldEdit, "#checker", 2, 3);
    }

    @Override
    public String getUsage() {
        return "#checker[<pattern>][<pattern>][size]";
    }

    @Override
    protected Pattern parseArguments(List<String> arguments, ParserContext context) throws InputParseException {
        Pattern first = worldEdit.getPatternFactory().parseFromInput(arguments.get(0), context);
        Pattern second = worldEdit.getPatternFactory().parseFromInput(arguments.get(1), context);
        int size = arguments.size() > 2
            ? parseInt(arguments.get(2), "size", 1, Integer.MAX_VALUE)
            : 1;
        return new CheckerPattern(first, second, size);
    }

    @Override
    protected Stream<String> getArgumentSuggestions(int index, String partial, ParserContext context) {
        if (index < 2) {
            return worldEdit.getPatternFactory().getSuggestions(partial, context).stream();
        }
        return suggestFrom(partial, "1", "2", "4", "8");
    }
}
