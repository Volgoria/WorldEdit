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
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.NoiseFilter;
import com.sk89q.worldedit.math.noise.RandomNoise;

import java.util.stream.Stream;

/**
 * Parses {@code %<percentage>}, randomly matching the given percentage of
 * blocks. The percentage is between 0 and 100 and may have decimals.
 */
public class NoiseMaskParser extends PrefixParser<Mask> {

    public NoiseMaskParser(WorldEdit worldEdit) {
        super(worldEdit, "%");
    }

    @Override
    protected Stream<String> getRemainderSuggestions(String prefix, String remainder, ParserContext context) {
        return suggestFrom(remainder, "10", "25", "50", "75");
    }

    @Override
    protected Mask parseRemainder(String prefix, String remainder, ParserContext context) throws InputParseException {
        double percentage = parseDouble(remainder, "percentage", 0, 100);
        return new NoiseFilter(new RandomNoise(), percentage / 100);
    }
}
