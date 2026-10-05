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

package com.sk89q.worldedit.extension.factory;

import com.google.common.base.CharMatcher;
import com.sk89q.util.StringUtil;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.factory.parser.AliasParser;
import com.sk89q.worldedit.extension.factory.parser.mask.AdjacentMaskParser;
import com.sk89q.worldedit.extension.factory.parser.mask.AirMaskParser;
import com.sk89q.worldedit.extension.factory.parser.mask.AngleMaskParser;
import com.sk89q.worldedit.extension.factory.parser.mask.AxisRangeMaskParser;
import com.sk89q.worldedit.extension.factory.parser.mask.BiomeMaskParser;
import com.sk89q.worldedit.extension.factory.parser.mask.BlockCategoryMaskParser;
import com.sk89q.worldedit.extension.factory.parser.mask.BlockStateMaskParser;
import com.sk89q.worldedit.extension.factory.parser.mask.BlocksMaskParser;
import com.sk89q.worldedit.extension.factory.parser.mask.ClipboardMaskParser;
import com.sk89q.worldedit.extension.factory.parser.mask.ExistingMaskParser;
import com.sk89q.worldedit.extension.factory.parser.mask.ExposedMaskParser;
import com.sk89q.worldedit.extension.factory.parser.mask.ExpressionMaskParser;
import com.sk89q.worldedit.extension.factory.parser.mask.FullCubeMaskParser;
import com.sk89q.worldedit.extension.factory.parser.mask.LazyRegionMaskParser;
import com.sk89q.worldedit.extension.factory.parser.mask.NegateMaskParser;
import com.sk89q.worldedit.extension.factory.parser.mask.NoiseMaskParser;
import com.sk89q.worldedit.extension.factory.parser.mask.OffsetBracketMaskParser;
import com.sk89q.worldedit.extension.factory.parser.mask.OffsetMaskParser;
import com.sk89q.worldedit.extension.factory.parser.mask.RadiusMaskParser;
import com.sk89q.worldedit.extension.factory.parser.mask.RegionMaskParser;
import com.sk89q.worldedit.extension.factory.parser.mask.SolidMaskParser;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.NoMatchException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.mask.BlockMaterialMask;
import com.sk89q.worldedit.function.mask.CoordinateRangeMask.Axis;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.MaskIntersection;
import com.sk89q.worldedit.function.mask.Masks;
import com.sk89q.worldedit.internal.registry.AbstractFactory;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;
import com.sk89q.worldedit.world.registry.BlockMaterial;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A registry of known {@link Mask}s. Provides methods to instantiate
 * new masks from input.
 *
 * <p>Instances of this class can be taken from
 * {@link WorldEdit#getMaskFactory()}.</p>
 */
public final class MaskFactory extends AbstractFactory<Mask> {

    private static final CharMatcher COMPONENT_DELIMITER = CharMatcher.is(' ');

    /**
     * Create a new mask registry.
     *
     * @param worldEdit the WorldEdit instance
     */
    public MaskFactory(WorldEdit worldEdit) {
        super(worldEdit, new BlocksMaskParser(worldEdit));

        register(new ClipboardMaskParser(worldEdit));
        register(new ExistingMaskParser(worldEdit));
        register(new AirMaskParser(worldEdit));
        register(new ExposedMaskParser(worldEdit));
        register(new SolidMaskParser(worldEdit));
        register(new FullCubeMaskParser(worldEdit));
        register(new LazyRegionMaskParser(worldEdit));
        register(new RegionMaskParser(worldEdit));
        register(new OffsetMaskParser(worldEdit));
        register(new NoiseMaskParser(worldEdit));
        register(new BlockStateMaskParser(worldEdit));
        register(new NegateMaskParser(worldEdit));
        register(new ExpressionMaskParser(worldEdit));

        register(new BlockCategoryMaskParser(worldEdit));
        register(new BiomeMaskParser(worldEdit));
        for (Axis axis : Axis.values()) {
            register(new AxisRangeMaskParser(worldEdit, axis));
        }
        register(new AngleMaskParser(worldEdit));
        register(new AdjacentMaskParser(worldEdit));
        register(new OffsetBracketMaskParser(worldEdit));
        register(new RadiusMaskParser(worldEdit));

        register(new AliasParser<>(worldEdit, c -> Masks.wall(c.requireExtent()), "#wall"));
        register(new AliasParser<>(worldEdit, c -> Masks.floor(c.requireExtent()), "#floor", "#top"));
        register(new AliasParser<>(worldEdit, c -> Masks.ceiling(c.requireExtent()), "#ceiling", "#roof"));
        register(materialParser(worldEdit, BlockMaterial::isLiquid, "#liquid"));
        register(materialParser(worldEdit, BlockMaterial::isOpaque, "#opaque"));
        register(materialParser(worldEdit, m -> !m.isOpaque(), "#transparent"));
    }

    private static AliasParser<Mask> materialParser(WorldEdit worldEdit, Predicate<BlockMaterial> predicate, String alias) {
        return new AliasParser<>(worldEdit, c -> new BlockMaterialMask(c.requireExtent(), predicate), alias);
    }

    /**
     * Split an intersection of masks at the spaces that are not inside brackets.
     */
    private static List<String> splitComponents(String input) {
        return StringUtil.splitOutsideBrackets(input, COMPONENT_DELIMITER);
    }

    @Override
    public List<String> getSuggestions(String input, ParserContext context) {
        List<String> components = splitComponents(input);
        if (components.size() > 1) {
            String last = components.get(components.size() - 1);
            String prev = input.substring(0, input.length() - last.length());
            return super.getSuggestions(last, context).stream()
                .map(s -> prev + s)
                .collect(Collectors.toList());
        }
        return super.getSuggestions(input, context);
    }

    @Override
    public Mask parseFromInput(String input, ParserContext context) throws InputParseException {
        List<Mask> masks = new ArrayList<>();

        for (String component : splitComponents(input)) {
            if (!component.isEmpty()) {
                masks.add(super.parseFromInput(component, context));
            }
        }

        return switch (masks.size()) {
            case 0 ->
                throw new NoMatchException(TranslatableComponent.of("worldedit.error.no-match", TextComponent.of(input)));
            case 1 -> masks.get(0);
            default -> new MaskIntersection(masks);
        };
    }

}
