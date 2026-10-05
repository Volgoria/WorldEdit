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
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.pattern.StripePattern;
import com.sk89q.worldedit.math.BlockVector3;

import java.util.List;
import java.util.stream.Stream;

/**
 * Parses {@code #linear[<pattern>,<pattern>,...]}, cycling through the
 * patterns block by block: the pattern used at a position is chosen by
 * {@code x + y + z} modulo the number of patterns, so that neighbouring
 * blocks always differ and the result does not depend on the order in
 * which blocks are placed.
 */
public class LinearPatternParser extends BracketArgumentParser<Pattern> {

    private static final BlockVector3 DIRECTION = BlockVector3.ONE;

    public LinearPatternParser(WorldEdit worldEdit) {
        super(worldEdit, "#linear", 1, 1);
    }

    @Override
    public String getUsage() {
        return "#linear[<pattern>,<pattern>,...]";
    }

    @Override
    protected Pattern parseArguments(List<String> arguments, ParserContext context) throws InputParseException {
        return new StripePattern(parsePatternList(arguments.get(0), context), DIRECTION, 1);
    }

    @Override
    protected Stream<String> getArgumentSuggestions(int index, String partial, ParserContext context) {
        return suggestPatternList(partial, context);
    }
}
