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

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.factory.parser.BracketArgumentParser;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.pattern.GradientPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;

import java.util.List;
import java.util.stream.Stream;

/**
 * Parses {@code #gradient[<patterns>]}, {@code #gradient[<patterns>][<fromY>]}
 * and {@code #gradient[<patterns>][<fromY>][<toY>]}.
 *
 * <p>Without explicit Y levels, the gradient spans the current selection,
 * from its lowest to its highest block. With only {@code fromY}, it ends at
 * the top of the current selection, or at the top of the world when there is
 * no selection.</p>
 */
public class GradientPatternParser extends BracketArgumentParser<Pattern> {

    public GradientPatternParser(WorldEdit worldEdit) {
        super(worldEdit, "#gradient", 1, 3);
    }

    @Override
    public String getUsage() {
        return "#gradient[<pattern>,<pattern>,...][fromY][toY] (no Y: the selection; fromY only: up to the top of "
            + "the selection, or of the world without one)";
    }

    @Override
    protected Pattern parseArguments(List<String> arguments, ParserContext context) throws InputParseException {
        List<Pattern> patterns = parsePatternList(arguments.get(0), context);
        int fromY;
        int toY;
        if (arguments.size() == 3) {
            fromY = parseInt(arguments.get(1));
            toY = parseInt(arguments.get(2));
        } else if (arguments.size() == 2) {
            fromY = parseInt(arguments.get(1));
            toY = topOfSelectionOrWorld(context);
        } else if (arguments.size() == 1) {
            Region selection = context.requireSelection();
            fromY = selection.getMinimumPoint().y();
            toY = selection.getMaximumPoint().y();
        } else {
            throw wrongArgumentCount();
        }
        return new GradientPattern(patterns, fromY, toY);
    }

    private static int topOfSelectionOrWorld(ParserContext context) throws InputParseException {
        World world = context.requireWorld();
        LocalSession session = context.getSession();
        if (session != null && session.isSelectionDefined(world)) {
            try {
                return session.getSelection(world).getMaximumPoint().y();
            } catch (IncompleteRegionException _) {
                // fall back to the top of the world
            }
        }
        return world.getMaxY();
    }

    @Override
    protected Stream<String> getArgumentSuggestions(int index, String partial, ParserContext context) {
        if (index == 0) {
            return suggestPatternList(partial, context);
        }
        return suggestFrom(partial, "-64", "0", "64", "128", "256", "320");
    }
}
