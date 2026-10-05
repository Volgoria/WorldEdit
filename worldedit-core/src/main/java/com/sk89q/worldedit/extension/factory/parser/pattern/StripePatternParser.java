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
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Parses {@code #stripes[<axes>][<patterns>]} and {@code #stripes[<axes>][<patterns>][<thickness>]}.
 *
 * <p>The axes argument is a combination of {@code x}, {@code y} and
 * {@code z}, each optionally preceded by {@code -}: {@code y} creates
 * horizontal layers, {@code xz} and {@code x-z} diagonal stripes.</p>
 */
public class StripePatternParser extends BracketArgumentParser<Pattern> {

    public StripePatternParser(WorldEdit worldEdit) {
        super(worldEdit, "#stripes", 2, 3);
    }

    @Override
    public String getUsage() {
        return "#stripes[<x|y|z|xz|...>][<pattern>,<pattern>,...][thickness]";
    }

    @Override
    protected Pattern parseArguments(List<String> arguments, ParserContext context) throws InputParseException {
        BlockVector3 direction = parseAxes(arguments.get(0));
        List<Pattern> patterns = parsePatternList(arguments.get(1), context);
        int thickness = arguments.size() > 2
            ? parseInt(arguments.get(2), "thickness", 1, Integer.MAX_VALUE)
            : 1;
        return new StripePattern(patterns, direction, thickness);
    }

    /**
     * Parse an axes specification such as {@code y}, {@code xz} or {@code x-z}.
     *
     * @param input the input
     * @return the direction vector
     * @throws InputParseException if the input is not a valid axes specification
     */
    static BlockVector3 parseAxes(String input) throws InputParseException {
        String lower = input.toLowerCase(Locale.ROOT);
        int x = 0;
        int y = 0;
        int z = 0;
        int sign = 1;
        boolean pendingSign = false;
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if (c == '-' && !pendingSign) {
                sign = -1;
                pendingSign = true;
                continue;
            }
            switch (c) {
                case 'x' -> {
                    if (x != 0) {
                        throw invalidAxes(input);
                    }
                    x = sign;
                }
                case 'y' -> {
                    if (y != 0) {
                        throw invalidAxes(input);
                    }
                    y = sign;
                }
                case 'z' -> {
                    if (z != 0) {
                        throw invalidAxes(input);
                    }
                    z = sign;
                }
                default -> throw invalidAxes(input);
            }
            sign = 1;
            pendingSign = false;
        }
        if (pendingSign || (x == 0 && y == 0 && z == 0)) {
            throw invalidAxes(input);
        }
        return BlockVector3.at(x, y, z);
    }

    private static InputParseException invalidAxes(String input) {
        return new InputParseException(TranslatableComponent.of(
            "worldedit.error.parser.bracket-args.invalid-axes", TextComponent.of(input)
        ));
    }

    @Override
    protected Stream<String> getArgumentSuggestions(int index, String partial, ParserContext context) {
        return switch (index) {
            case 0 -> suggestFrom(partial, "x", "y", "z", "xz", "x-z", "xy", "yz", "xyz");
            case 1 -> suggestPatternList(partial, context);
            default -> suggestFrom(partial, "1", "2", "3", "4", "8");
        };
    }
}
