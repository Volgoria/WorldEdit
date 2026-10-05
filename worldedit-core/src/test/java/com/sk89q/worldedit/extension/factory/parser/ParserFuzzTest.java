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

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.factory.MaskFactory;
import com.sk89q.worldedit.extension.factory.PatternFactory;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import com.sk89q.worldedit.world.World;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Feeds prefixes and mutations of valid patterns and masks to the parsers and
 * their suggestions: parsing may only fail with an {@link InputParseException},
 * and suggestions must never fail.
 */
class ParserFuzzTest extends ParserTestBase {

    private static final List<String> PATTERNS = List.of(
        "stone", "50%stone,50%dirt", "#gradient[stone,dirt,sand][0][10]", "#gradient[stone,dirt][-64]",
        "#linear[stone,dirt,sand]", "#checker[stone][dirt,sand][4]", "#checker[#stripes[y][stone,dirt]][glass]",
        "#stripes[X-Z][stone,dirt]", "#stripes[y][stone,dirt][2]", "#noise[12.5][stone,dirt,sand]",
        "#noise[stone,dirt]", "#offset[-3][2][7][sand]", "#offset[1][0][0][#linear[stone,dirt]]",
        "#mask[#x[0][0] #z[0][0]][#mask[#y[0][0]][stone][dirt]][50%sand,50%glass]",
        "#mask[#y[0][5]][stone][dirt]", "#copy@[1,-2,3]", "#clipboard@[x,y,z]", "^[facing=north]",
        "^=[facing=north,half=top]", "^stone[facing=north]", "*stone", "#existing", "#randomstate[stone]"
    );

    private static final List<String> MASKS = List.of(
        "stone", "!stone", "#existing", "!#existing", "#y[0][5] #angle[15][45]", "#y[*][62]", "#y[62][*]",
        "#x[0][5] #z[0][5]", "#z[*][-5]", "#angle[0][22.5][3]", "#angle[-5][30]", "#adjacent[#y[0][0]][1][2]",
        "#radius[5][2.5]", "#offset[0][-1][0][#x[0][5] #z[0][5]] #y[0][3]", "#offset[0][1][0][#existing]",
        "~#y[0][0]", "<#y[0][0]", ">#y[0][0]", "#solid", "#wall", "#surface", "#exposed", "#liquid",
        "#ceiling", "#floor", "#opaque", "#fullcube", "#noise[16][0.5]", "#region", "#dregion", "#dsel",
        "=y<64", "%50", "$plains", "^[facing=north]", "#biome[plains]", "#air", "##minecraft:logs"
    );

    private static final String NOISE = "[]#!^%@&*,;=-+.~<>$:0123456789xyzXYZ stone";

    private final PatternFactory patterns = WorldEdit.getInstance().getPatternFactory();
    private final MaskFactory masks = WorldEdit.getInstance().getMaskFactory();

    private static List<Supplier<ParserContext>> contexts() {
        return List.of(ParserTestBase::newContext, () -> {
            World world = mock(World.class);
            when(world.getMinY()).thenReturn(-64);
            when(world.getMaxY()).thenReturn(319);
            LocalSession session = new LocalSession();
            session.setRegionSelector(world,
                new CuboidRegionSelector(world, BlockVector3.at(0, -10, 0), BlockVector3.at(5, 40, 5)));
            ParserContext context = newContext();
            context.setSession(session);
            context.setWorld(world);
            return context;
        });
    }

    private static Set<String> inputs(List<String> seeds) {
        Set<String> inputs = new LinkedHashSet<>();
        for (String seed : seeds) {
            for (int i = 0; i <= seed.length(); i++) {
                inputs.add(seed.substring(0, i));
            }
        }
        Random random = new Random(42);
        for (int i = 0; i < 3000; i++) {
            StringBuilder input = new StringBuilder(seeds.get(random.nextInt(seeds.size())));
            int edits = 1 + random.nextInt(3);
            for (int edit = 0; edit < edits; edit++) {
                int at = random.nextInt(input.length() + 1);
                switch (random.nextInt(3)) {
                    case 0 -> input.insert(at, NOISE.charAt(random.nextInt(NOISE.length())));
                    case 1 -> {
                        if (at < input.length()) {
                            input.deleteCharAt(at);
                        }
                    }
                    default -> input.setLength(at);
                }
            }
            inputs.add(input.toString());
        }
        return inputs;
    }

    private interface Parser {
        void parse(String input, ParserContext context) throws InputParseException;
    }

    private interface Suggester {
        List<String> suggest(String input, ParserContext context);
    }

    private static List<String> fuzz(Set<String> inputs, Parser parser, Suggester suggester) {
        List<String> problems = new ArrayList<>();
        for (Supplier<ParserContext> context : contexts()) {
            for (String input : inputs) {
                try {
                    suggester.suggest(input, context.get());
                } catch (RuntimeException e) {
                    problems.add("suggest '" + input + "': " + e);
                }
                try {
                    parser.parse(input, context.get());
                } catch (InputParseException _) {
                    // Expected for invalid input
                } catch (RuntimeException e) {
                    problems.add("parse '" + input + "': " + e);
                }
            }
        }
        return problems;
    }

    @Test
    void patternParsingAndSuggestionsNeverCrash() {
        Set<String> inputs = inputs(PATTERNS);
        assertEquals(List.of(), fuzz(inputs, patterns::parseFromInput, patterns::getSuggestions));
    }

    @Test
    void emptyExpressionAndUpperCaseBiomeAreParseErrors() {
        assertThrows(InputParseException.class, () -> masks.parseFromInput("=", newContext()));
        assertThrows(InputParseException.class, () -> masks.parseFromInput("= ", newContext()));
        assertThrows(InputParseException.class, () -> masks.parseFromInput("$NOT_A_BIOME", newContext()));
    }

    @Test
    void maskParsingAndSuggestionsNeverCrash() {
        Set<String> inputs = inputs(MASKS);
        assertEquals(List.of(), fuzz(inputs, masks::parseFromInput, masks::getSuggestions));
    }
}
