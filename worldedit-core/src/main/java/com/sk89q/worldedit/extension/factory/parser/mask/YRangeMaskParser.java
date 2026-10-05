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
import com.sk89q.worldedit.function.mask.BoundedHeightMask;
import com.sk89q.worldedit.function.mask.Mask;

import java.util.List;
import java.util.stream.Stream;

/**
 * Parses {@code #y[<min>][<max>]}, matching blocks whose Y coordinate is in
 * the inclusive range. Either bound may be {@code *} for no limit; the
 * bounds may be given in any order.
 */
public class YRangeMaskParser extends BracketArgumentParser<Mask> {

    private static final String UNBOUNDED = "*";

    public YRangeMaskParser(WorldEdit worldEdit) {
        super(worldEdit, "#y", 2, 2);
    }

    @Override
    public String getUsage() {
        return "#y[<min|*>][<max|*>]";
    }

    @Override
    protected Mask parseArguments(List<String> arguments, ParserContext context) throws InputParseException {
        String first = arguments.get(0).trim();
        String second = arguments.get(1).trim();
        int min = first.equals(UNBOUNDED) ? Integer.MIN_VALUE : parseInt(first);
        int max = second.equals(UNBOUNDED) ? Integer.MAX_VALUE : parseInt(second);
        return new BoundedHeightMask(Math.min(min, max), Math.max(min, max));
    }

    @Override
    protected Stream<String> getArgumentSuggestions(int index, String partial, ParserContext context) {
        return suggestFrom(partial, UNBOUNDED, "-64", "0", "62", "64", "128", "256", "320");
    }
}
