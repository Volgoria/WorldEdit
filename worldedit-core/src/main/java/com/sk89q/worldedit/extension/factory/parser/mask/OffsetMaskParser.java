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
import com.sk89q.worldedit.extension.factory.parser.PrefixParser;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.OffsetsMask;
import com.sk89q.worldedit.math.BlockVector3;

import java.util.stream.Stream;

/**
 * Parses {@code >[mask]} (the block below matches), {@code <[mask]} (the
 * block above matches) and {@code ~[mask]} (any adjacent block matches).
 * Without a mask, {@code #existing} is used.
 */
public class OffsetMaskParser extends PrefixParser<Mask> {

    private static final BlockVector3 BELOW = BlockVector3.at(0, -1, 0);
    private static final BlockVector3 ABOVE = BlockVector3.at(0, 1, 0);

    public OffsetMaskParser(WorldEdit worldEdit) {
        super(worldEdit, ">", "<", "~");
    }

    @Override
    protected Stream<String> getRemainderSuggestions(String prefix, String remainder, ParserContext context) {
        return suggestMask(remainder, context);
    }

    @Override
    protected Mask parseRemainder(String prefix, String remainder, ParserContext context) throws InputParseException {
        Mask submask = remainder.isEmpty()
            ? new ExistingBlockMask(context.requireExtent())
            : parseMask(remainder, context);
        return switch (prefix) {
            case "~" -> OffsetsMask.adjacent(submask);
            case ">" -> OffsetsMask.single(submask, BELOW);
            default -> OffsetsMask.single(submask, ABOVE);
        };
    }
}
