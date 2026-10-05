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
import com.sk89q.worldedit.extension.factory.parser.PrefixParser;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.pattern.RandomStatePattern;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.FuzzyBlockState;

import java.util.stream.Stream;

/**
 * Parses {@code *<block>}, a block with a random value for every state that
 * is not given.
 */
public class RandomStatePatternParser extends PrefixParser<Pattern> {

    public RandomStatePatternParser(WorldEdit worldEdit) {
        super(worldEdit, "*");
    }

    @Override
    protected Stream<String> getRemainderSuggestions(String prefix, String remainder, ParserContext context) {
        return worldEdit.getBlockFactory().getSuggestions(remainder, context).stream();
    }

    @Override
    protected Pattern parseRemainder(String prefix, String remainder, ParserContext context) throws InputParseException {
        boolean wasFuzzy = context.isPreferringWildcard();
        context.setPreferringWildcard(true);
        BaseBlock block;
        try {
            block = worldEdit.getBlockFactory().parseFromInput(remainder, context);
        } finally {
            context.setPreferringWildcard(wasFuzzy);
        }
        if (block.getStates().size() == block.getBlockType().getPropertyMap().size()) {
            // they requested random with *, but didn't leave any states empty - simplify
            return block;
        } else if (block.toImmutableState() instanceof FuzzyBlockState fuzzy) {
            return new RandomStatePattern(fuzzy);
        } else {
            return null; // only should happen if parseLogic changes
        }
    }
}
