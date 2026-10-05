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

import com.google.common.base.CharMatcher;
import com.sk89q.util.StringUtil;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.factory.parser.ArgumentInputParser;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.pattern.RandomPattern;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;

import java.util.List;
import java.util.regex.Matcher;
import java.util.stream.Stream;

/**
 * Parses a comma separated list of patterns, each optionally preceded by a
 * weight and a percent sign, e.g. {@code 70%stone,30%dirt}. Entries without
 * a weight have a weight of 1.
 */
public class RandomPatternParser extends ArgumentInputParser<Pattern> {

    private static final CharMatcher PATTERN_DELIMITER = CharMatcher.is(',');

    /**
     * Matches {@code <weight>%<pattern>}; the pattern may be empty, which is an error when parsing.
     */
    private static final java.util.regex.Pattern WEIGHTED = java.util.regex.Pattern.compile("([0-9]+(?:\\.[0-9]*)?)%(.*)");

    public RandomPatternParser(WorldEdit worldEdit) {
        super(worldEdit);
    }

    @Override
    public Stream<String> getSuggestions(String input, ParserContext context) {
        List<String> patterns = StringUtil.splitOutsideBrackets(input, PATTERN_DELIMITER);
        // get suggestions for the last token only
        String percent = null;
        String token = patterns.get(patterns.size() - 1);
        Matcher matcher = WEIGHTED.matcher(token);
        if (matcher.matches()) {
            percent = matcher.group(1);
            token = matcher.group(2);
        } else if (patterns.size() == 1) {
            return Stream.empty(); // handled by DefaultBlockParser
        }
        String previous = patterns.size() == 1 ? "" : String.join(",", patterns.subList(0, patterns.size() - 1)) + ",";
        String prefix = previous + (percent == null ? "" : percent + "%");
        return suggestPattern(token, context).map(s -> prefix + s);
    }

    @Override
    public Pattern parseFromInput(String input, ParserContext context) throws InputParseException {
        List<String> patterns = StringUtil.splitOutsideBrackets(input, PATTERN_DELIMITER);
        if (patterns.size() == 1) {
            return null; // let a 'single'-pattern parser handle it
        }
        RandomPattern randomPattern = new RandomPattern();
        for (String token : patterns) {
            Matcher matcher = WEIGHTED.matcher(token);
            if (matcher.matches()) {
                String inner = matcher.group(2);
                if (inner.isEmpty()) {
                    throw new InputParseException(TranslatableComponent.of(
                        "worldedit.error.parser.missing-random-type", TextComponent.of(input)
                    ));
                }
                randomPattern.add(parsePattern(inner, context), Double.parseDouble(matcher.group(1)));
            } else {
                randomPattern.add(parsePattern(token, context), 1);
            }
        }
        return randomPattern;
    }
}
