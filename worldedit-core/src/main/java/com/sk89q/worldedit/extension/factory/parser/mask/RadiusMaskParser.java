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

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.factory.parser.BracketArgumentParser;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.RadiusMask;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;

import java.util.List;
import java.util.stream.Stream;

/**
 * Parses {@code #radius[<max>]} and {@code #radius[<min>][<max>]}, matching
 * blocks whose distance to the placement position (as for {@code //sphere},
 * see {@code //placement}) is within the inclusive range. The center is fixed
 * when the mask is parsed.
 */
public class RadiusMaskParser extends BracketArgumentParser<Mask> {

    public RadiusMaskParser(WorldEdit worldEdit) {
        super(worldEdit, "#radius", 1, 2);
    }

    @Override
    public String getUsage() {
        return "#radius[min][<max>]";
    }

    @Override
    protected Mask parseArguments(List<String> arguments, ParserContext context) throws InputParseException {
        double first = parseDouble(arguments.get(0), "radius", 0, Double.MAX_VALUE);
        double second = arguments.size() > 1
            ? parseDouble(arguments.get(1), "radius", 0, Double.MAX_VALUE)
            : 0;
        BlockVector3 center;
        try {
            center = context.requireSession().getPlacementPosition(context.requireActor());
        } catch (IncompleteRegionException _) {
            throw new InputParseException(TranslatableComponent.of("worldedit.error.incomplete-region"));
        }
        return new RadiusMask(center, Math.min(first, second), Math.max(first, second));
    }

    @Override
    protected Stream<String> getArgumentSuggestions(int index, String partial, ParserContext context) {
        return suggestFrom(partial, "0", "5", "10", "16", "32", "64");
    }
}
