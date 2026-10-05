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
import com.sk89q.worldedit.function.mask.CoordinateRangeMask;
import com.sk89q.worldedit.function.mask.CoordinateRangeMask.Axis;
import com.sk89q.worldedit.function.mask.Mask;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Parses {@code #x[<min>][<max>]}, {@code #y[<min>][<max>]} and
 * {@code #z[<min>][<max>]}, matching blocks whose coordinate along the axis
 * is in the inclusive range. Either bound may be {@code *} for no limit; the
 * bounds may be given in any order.
 */
public class AxisRangeMaskParser extends BracketArgumentParser<Mask> {

    private static final String UNBOUNDED = "*";
    private static final String[] HEIGHT_SUGGESTIONS = {UNBOUNDED, "-64", "0", "62", "64", "128", "256", "320"};
    private static final String[] HORIZONTAL_SUGGESTIONS = {UNBOUNDED, "-1000", "-100", "0", "100", "1000"};

    private final Axis axis;

    /**
     * Create a parser for the given axis.
     *
     * @param worldEdit the WorldEdit instance
     * @param axis the axis
     */
    public AxisRangeMaskParser(WorldEdit worldEdit, Axis axis) {
        super(worldEdit, "#" + axis.name().toLowerCase(Locale.ROOT), 2, 2);
        this.axis = axis;
    }

    @Override
    public String getUsage() {
        return getName() + "[<min|*>][<max|*>]";
    }

    @Override
    protected Mask parseArguments(List<String> arguments, ParserContext context) throws InputParseException {
        int first = parseBound(arguments.get(0), Integer.MIN_VALUE);
        int second = parseBound(arguments.get(1), Integer.MAX_VALUE);
        int min = Math.min(first, second);
        int max = Math.max(first, second);
        if (axis == Axis.Y) {
            return new BoundedHeightMask(min, max);
        }
        return new CoordinateRangeMask(axis, min, max);
    }

    private static int parseBound(String argument, int unbounded) throws InputParseException {
        return argument.trim().equals(UNBOUNDED) ? unbounded : parseInt(argument);
    }

    @Override
    protected Stream<String> getArgumentSuggestions(int index, String partial, ParserContext context) {
        return suggestFrom(partial, axis == Axis.Y ? HEIGHT_SUGGESTIONS : HORIZONTAL_SUGGESTIONS);
    }
}
