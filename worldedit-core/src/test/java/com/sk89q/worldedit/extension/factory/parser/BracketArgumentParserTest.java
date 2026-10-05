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

package com.sk89q.worldedit.extension.factory.parser;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BracketArgumentParserTest extends ParserTestBase {

    /**
     * A parser that returns its arguments, with fixed suggestions per argument.
     */
    private static final class EchoParser extends BracketArgumentParser<List<String>> {

        EchoParser() {
            super(WorldEdit.getInstance(), "#echo", 1, 2);
        }

        @Override
        public String getUsage() {
            return "#echo[a][b]";
        }

        @Override
        protected List<String> parseArguments(List<String> arguments, ParserContext context) {
            return arguments;
        }

        @Override
        protected Stream<String> getArgumentSuggestions(int index, String partial, ParserContext context) {
            return suggestFrom(partial, "arg" + index + "a", "arg" + index + "b");
        }
    }

    private final EchoParser parser = new EchoParser();
    private final ParserContext context = new ParserContext();

    private List<String> suggest(String input) {
        return parser.getSuggestions(input, context).collect(Collectors.toList());
    }

    @Test
    void splitsArguments() throws InputParseException {
        assertEquals(List.of("a"), parser.parseFromInput("#echo[a]", context));
        assertEquals(List.of("a", "b,c"), parser.parseFromInput("#echo[a][b,c]", context));
        assertEquals(List.of("", "x"), parser.parseFromInput("#ECHO[][x]", context));
    }

    @Test
    void keepsNestedBrackets() throws InputParseException {
        assertEquals(
            List.of("oak_stairs[facing=north]", "#echo[x][y]"),
            parser.parseFromInput("#echo[oak_stairs[facing=north]][#echo[x][y]]", context)
        );
    }

    @Test
    void ignoresOtherInput() throws InputParseException {
        assertNull(parser.parseFromInput("stone", context));
        assertNull(parser.parseFromInput("#echoes[a]", context));
        assertNull(parser.parseFromInput("#ech", context));
    }

    @Test
    void rejectsMalformedInput() {
        assertThrows(InputParseException.class, () -> parser.parseFromInput("#echo", context));
        assertThrows(InputParseException.class, () -> parser.parseFromInput("#echo[a][b][c]", context));
        assertThrows(InputParseException.class, () -> parser.parseFromInput("#echo[a", context));
        assertThrows(InputParseException.class, () -> parser.parseFromInput("#echo[a]b", context));
        assertThrows(InputParseException.class, () -> parser.parseFromInput("#echo[a[b]", context));
    }

    @Test
    void suggestsName() {
        assertEquals(List.of("#echo"), suggest(""));
        assertEquals(List.of("#echo"), suggest("#ec"));
        assertEquals(List.of("#echo["), suggest("#echo"));
        assertEquals(List.of(), suggest("stone"));
    }

    @Test
    void suggestsArguments() {
        assertEquals(List.of("#echo[arg0a", "#echo[arg0b"), suggest("#echo["));
        assertEquals(List.of("#echo[arg0b"), suggest("#echo[arg0b"));
        assertEquals(List.of("#echo[x]["), suggest("#echo[x]"));
        assertEquals(List.of("#echo[x][arg1a", "#echo[x][arg1b"), suggest("#echo[x]["));
        // no more arguments after the maximum
        assertEquals(List.of(), suggest("#echo[x][y]"));
        assertEquals(List.of(), suggest("#echo[x][y]["));
    }
}
