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
import com.sk89q.worldedit.function.mask.ExpressionMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.internal.expression.Expression;
import com.sk89q.worldedit.internal.expression.ExpressionException;
import com.sk89q.worldedit.math.transform.Identity;
import com.sk89q.worldedit.regions.shape.WorldEditExpressionEnvironment;
import com.sk89q.worldedit.session.SessionOwner;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;

import java.util.function.IntSupplier;

/**
 * Parses {@code =<expression>}, matching blocks for which the expression,
 * evaluated with the variables {@code x}, {@code y} and {@code z}, is positive.
 */
public class ExpressionMaskParser extends PrefixParser<Mask> {

    public ExpressionMaskParser(WorldEdit worldEdit) {
        super(worldEdit, "=");
    }

    @Override
    protected Mask parseRemainder(String prefix, String remainder, ParserContext context) throws InputParseException {
        if (remainder.isBlank()) {
            // Expression.compile rejects empty expressions with an IllegalArgumentException
            throw new InputParseException(TranslatableComponent.of(
                "worldedit.error.parser.invalid-expression", TextComponent.of(remainder)));
        }
        try {
            Expression exp = Expression.compile(remainder, "x", "y", "z");
            WorldEditExpressionEnvironment env = new WorldEditExpressionEnvironment(
                    context.requireExtent(), new Identity());
            exp.setEnvironment(env);
            if (context.getActor() != null) {
                SessionOwner owner = context.getActor();
                IntSupplier timeout = () -> WorldEdit.getInstance().getSessionManager().get(owner).getTimeout();
                return new ExpressionMask(exp, timeout);
            }
            return new ExpressionMask(exp);
        } catch (ExpressionException e) {
            throw new InputParseException(TranslatableComponent.of(
                "worldedit.error.parser.invalid-expression", TextComponent.of(e.getMessage() != null ? e.getMessage() : remainder)
            ), e);
        }
    }
}
