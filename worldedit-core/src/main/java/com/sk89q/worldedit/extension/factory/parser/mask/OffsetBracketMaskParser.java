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
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.OffsetsMask;

import java.util.List;
import java.util.stream.Stream;

/**
 * Parses {@code #offset[<x>][<y>][<z>][<mask>]}, matching blocks where the
 * block at the given offset matches the mask. This generalizes
 * {@code >mask} ({@code #offset[0][-1][0][mask]}) and {@code <mask}.
 */
public class OffsetBracketMaskParser extends BracketArgumentParser<Mask> {

    public OffsetBracketMaskParser(WorldEdit worldEdit) {
        super(worldEdit, "#offset", 4, 4);
    }

    @Override
    public String getUsage() {
        return "#offset[<x>][<y>][<z>][<mask>]";
    }

    @Override
    protected Mask parseArguments(List<String> arguments, ParserContext context) throws InputParseException {
        return OffsetsMask.single(parseMask(arguments.get(3), context), parseBlockVector(arguments, 0));
    }

    @Override
    protected Stream<String> getArgumentSuggestions(int index, String partial, ParserContext context) {
        if (index < 3) {
            return suggestFrom(partial, OFFSET_SUGGESTIONS);
        }
        return suggestMask(partial, context);
    }
}
