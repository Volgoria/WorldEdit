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
import com.sk89q.worldedit.function.pattern.NoisePattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.Vector2;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.noise.NoiseGenerator;
import com.sk89q.worldedit.math.noise.PerlinNoise;

import java.util.List;
import java.util.stream.Stream;

/**
 * Parses {@code #noise[<scale>][<patterns>]}, choosing between the patterns
 * using Perlin noise with a random seed.
 *
 * <p>The noise is equalized so that every pattern covers roughly the same
 * share of the blocks.</p>
 */
public class NoisePatternParser extends BracketArgumentParser<Pattern> {

    public NoisePatternParser(WorldEdit worldEdit) {
        super(worldEdit, "#noise", 2, 2);
    }

    @Override
    public String getUsage() {
        return "#noise[<scale>][<pattern>,<pattern>,...]";
    }

    @Override
    protected Pattern parseArguments(List<String> arguments, ParserContext context) throws InputParseException {
        double scale = parseDouble(arguments.get(0), "scale", 0.01, 10000);
        List<Pattern> patterns = parsePatternList(arguments.get(1), context);
        return new NoisePattern(new EqualizedNoise(new PerlinNoise()), scale, patterns);
    }

    @Override
    protected Stream<String> getArgumentSuggestions(int index, String partial, ParserContext context) {
        if (index == 0) {
            return suggestFrom(partial, "4", "8", "16", "32", "64");
        }
        return suggestPatternList(partial, context);
    }

    /**
     * Maps the roughly normally distributed output of Perlin noise to an
     * approximately uniform distribution over {@code [0, 1]}.
     */
    static final class EqualizedNoise implements NoiseGenerator {

        // Measured over large samples of PerlinNoise with default settings
        private static final double MEAN = 0.5;
        private static final double STANDARD_DEVIATION = 0.22;

        private final NoiseGenerator delegate;

        EqualizedNoise(NoiseGenerator delegate) {
            this.delegate = delegate;
        }

        @Override
        public float noise(Vector2 position) {
            return equalize(delegate.noise(position));
        }

        @Override
        public float noise(Vector3 position) {
            return equalize(delegate.noise(position));
        }

        static float equalize(double value) {
            return (float) normalCdf((value - MEAN) / STANDARD_DEVIATION);
        }

        /**
         * Standard normal cumulative distribution function, using the
         * Abramowitz and Stegun approximation of the error function.
         */
        private static double normalCdf(double x) {
            double z = Math.abs(x) / Math.sqrt(2);
            double t = 1 / (1 + 0.3275911 * z);
            double poly = t * (0.254829592 + t * (-0.284496736 + t * (1.421413741 + t * (-1.453152027 + t * 1.061405429))));
            double erf = 1 - poly * Math.exp(-z * z);
            return x >= 0 ? 0.5 * (1 + erf) : 0.5 * (1 - erf);
        }
    }
}
