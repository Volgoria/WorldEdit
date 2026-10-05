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
 * Parses {@code #adjacent[<mask>][<min>][<max>]}, matching blocks where the
 * number of the six face-adjacent blocks matching the inner mask is in the
 * inclusive range. {@code min} defaults to 1 and {@code max} to 6.
 */
public class AdjacentMaskParser extends BracketArgumentParser<Mask> {

    private static final int FACES = 6;

    public AdjacentMaskParser(WorldEdit worldEdit) {
        super(worldEdit, "#adjacent", 1, 3);
    }

    @Override
    public String getUsage() {
        return "#adjacent[<mask>][min][max]";
    }

    @Override
    protected Mask parseArguments(List<String> arguments, ParserContext context) throws InputParseException {
        Mask mask = worldEdit.getMaskFactory().parseFromInput(arguments.get(0), context);
        int min = arguments.size() > 1 ? parseInt(arguments.get(1), "min", 0, FACES) : 1;
        int max = arguments.size() > 2 ? parseInt(arguments.get(2), "max", 0, FACES) : FACES;
        return OffsetsMask.builder(mask)
            .minMatches(Math.min(min, max))
            .maxMatches(Math.max(min, max))
            .build();
    }

    @Override
    protected Stream<String> getArgumentSuggestions(int index, String partial, ParserContext context) {
        if (index == 0) {
            return worldEdit.getMaskFactory().getSuggestions(partial, context).stream();
        }
        return suggestFrom(partial, "0", "1", "2", "3", "4", "5", "6");
    }
}
