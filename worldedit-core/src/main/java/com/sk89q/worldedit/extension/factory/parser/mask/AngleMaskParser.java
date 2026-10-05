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

package com.sk89q.worldedit.extension.factory.parser.mask;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.factory.parser.BracketArgumentParser;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.mask.AngleMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.SolidBlockMask;

import java.util.List;
import java.util.stream.Stream;

/**
 * Parses {@code #angle[<min>][<max>]} and {@code #angle[<min>][<max>][<distance>]},
 * matching solid blocks whose terrain slope, in degrees, is in the range.
 */
public class AngleMaskParser extends BracketArgumentParser<Mask> {

    public AngleMaskParser(WorldEdit worldEdit) {
        super(worldEdit, "#angle", 2, 3);
    }

    @Override
    public String getUsage() {
        return "#angle[<min degrees>][<max degrees>][distance]";
    }

    @Override
    protected Mask parseArguments(List<String> arguments, ParserContext context) throws InputParseException {
        double first = parseDouble(arguments.get(0), "angle", 0, 90);
        double second = parseDouble(arguments.get(1), "angle", 0, 90);
        int distance = arguments.size() > 2
            ? parseInt(arguments.get(2), "distance", 1, 16)
            : 1;
        return new AngleMask(
            new SolidBlockMask(context.requireExtent()),
            Math.min(first, second), Math.max(first, second), distance
        );
    }

    @Override
    protected Stream<String> getArgumentSuggestions(int index, String partial, ParserContext context) {
        if (index < 2) {
            return suggestFrom(partial, "0", "15", "30", "45", "60", "75", "90");
        }
        return suggestFrom(partial, "1", "2", "3", "4");
    }
}
