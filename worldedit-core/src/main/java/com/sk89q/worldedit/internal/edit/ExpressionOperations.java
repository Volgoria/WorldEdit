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

package com.sk89q.worldedit.internal.edit;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.extent.InputExtent;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.internal.expression.Expression;
import com.sk89q.worldedit.internal.expression.ExpressionException;
import com.sk89q.worldedit.internal.expression.ExpressionTimeoutException;
import com.sk89q.worldedit.internal.expression.LocalSlot.Variable;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.shape.ArbitraryBiomeShape;
import com.sk89q.worldedit.regions.shape.ArbitraryShape;
import com.sk89q.worldedit.regions.shape.WorldEditExpressionEnvironment;
import com.sk89q.worldedit.util.collection.DoubleArrayList;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.registry.LegacyMapper;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The expression-driven operations of {@link EditSession}: generating block
 * and biome shapes, and deforming regions.
 */
public final class ExpressionOperations {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    /**
     * Compile and optimize an expression.
     *
     * @param expressionString the expression
     * @param variableNames the names of the variables the expression is given
     * @return the expression
     * @throws ExpressionException if the expression is invalid
     */
    public static Expression compile(String expressionString, String... variableNames) throws ExpressionException {
        final Expression expression = Expression.compile(expressionString, variableNames);
        expression.optimize();
        return expression;
    }

    private static Variable getRequiredVariable(String name, Expression expression) {
        return expression.getSlots().getVariable(name)
            .orElseThrow(() -> new IllegalStateException("Expression is missing required variable: " + name));
    }

    /**
     * Implementation of {@link EditSession#makeShape(Region, Transform, Pattern, Expression, boolean, int)}.
     */
    public static int makeShape(EditSession session, final Region region, Transform transform,
                                final Pattern pattern, final Expression expression, final boolean hollow, final int timeout)
        throws ExpressionException, MaxChangedBlocksException {
        getRequiredVariable("x", expression);
        getRequiredVariable("y", expression);
        getRequiredVariable("z", expression);

        final Variable typeVariable = getRequiredVariable("type", expression);
        final Variable dataVariable = getRequiredVariable("data", expression);

        final WorldEditExpressionEnvironment environment = new WorldEditExpressionEnvironment(session, transform);
        expression.setEnvironment(environment);

        final int[] timedOut = {0};
        final Transform transformInverse = transform.inverse();
        final ArbitraryShape shape = new ArbitraryShape(region) {
            @Override
            protected BaseBlock getMaterial(int x, int y, int z, BaseBlock defaultMaterial) {
                final Vector3 current = Vector3.at(x, y, z);
                environment.setCurrentBlock(current);
                final Vector3 inputPosition = transformInverse.apply(current);

                try {
                    int[] legacy = LegacyMapper.getInstance().getLegacyFromBlock(defaultMaterial.toImmutableState());
                    int typeVar = -1;
                    int dataVar = -1;
                    if (legacy != null) {
                        typeVar = legacy[0];
                        if (legacy.length > 1) {
                            dataVar = legacy[1];
                        }
                    }
                    if (expression.evaluate(new double[]{ inputPosition.x(), inputPosition.y(), inputPosition.z(), typeVar, dataVar}, timeout) <= 0) {
                        return null;
                    }
                    int newType = (int) typeVariable.value();
                    int newData = (int) dataVariable.value();
                    if (newType != typeVar || newData != dataVar) {
                        BlockState state = LegacyMapper.getInstance().getBlockFromLegacy(newType, newData);
                        return state == null ? defaultMaterial : state.toBaseBlock();
                    } else {
                        return defaultMaterial;
                    }
                } catch (ExpressionTimeoutException _) {
                    timedOut[0] = timedOut[0] + 1;
                    return null;
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        int changed = shape.generate(session, pattern, hollow);
        if (timedOut[0] > 0) {
            throw new ExpressionTimeoutException(
                    String.format("%d blocks changed. %d blocks took too long to evaluate (increase with //timeout).",
                            changed, timedOut[0]));
        }
        return changed;
    }

    /**
     * Implementation of {@link EditSession#deformRegion(Region, Transform, Expression, int, InputExtent, Transform)}.
     */
    public static int deformRegion(EditSession session, final Region region, final Transform targetTransform,
                                   final Expression expression, final int timeout, InputExtent sourceExtent,
                                   final Transform sourceTransform) throws ExpressionException, MaxChangedBlocksException {
        final Variable x = getRequiredVariable("x", expression);
        final Variable y = getRequiredVariable("y", expression);
        final Variable z = getRequiredVariable("z", expression);

        final WorldEditExpressionEnvironment environment = new WorldEditExpressionEnvironment(session, targetTransform);
        expression.setEnvironment(environment);

        final DoubleArrayList<BlockVector3, BaseBlock> queue = new DoubleArrayList<>(false);

        final Transform targetTransformInverse = targetTransform.inverse();
        for (BlockVector3 targetBlockPosition : region) {
            final Vector3 targetPosition = targetBlockPosition.toVector3();
            environment.setCurrentBlock(targetPosition);

            // transform from target coordinates
            final Vector3 inputPosition = targetTransformInverse.apply(targetPosition);

            // deform
            expression.evaluate(new double[]{ inputPosition.x(), inputPosition.y(), inputPosition.z() }, timeout);
            final Vector3 outputPosition = Vector3.at(x.value(), y.value(), z.value());

            // transform to source coordinates, round-nearest
            final BlockVector3 sourcePosition = sourceTransform.apply(outputPosition).add(0.5, 0.5, 0.5).toBlockPoint();

            // read block from source extent (e.g. world/clipboard)
            final BaseBlock material = sourceExtent.getFullBlock(sourcePosition);

            // queue operation
            queue.put(targetBlockPosition, material);
        }

        int affected = 0;
        for (Map.Entry<BlockVector3, BaseBlock> entry : queue) {
            BlockVector3 targetPosition = entry.getKey();
            BaseBlock material = entry.getValue();

            // set at new targetPosition
            if (session.setBlock(targetPosition, material)) {
                ++affected;
            }
        }

        return affected;
    }

    /**
     * Implementation of {@link EditSession#makeBiomeShape(Region, Transform, BiomeType, String, boolean, int)}.
     */
    public static int makeBiomeShape(EditSession session, final Region region, Transform transform,
                                     final BiomeType biomeType, final String expressionString, final boolean hollow,
                                     final int timeout) throws ExpressionException {
        final Expression expression = compile(expressionString, "x", "y", "z");

        final WorldEditExpressionEnvironment environment = new WorldEditExpressionEnvironment(session, transform);
        expression.setEnvironment(environment);

        AtomicInteger timedOut = new AtomicInteger();
        final Transform transformInverse = transform.inverse();
        final ArbitraryBiomeShape shape = new ArbitraryBiomeShape(region) {
            @Override
            protected BiomeType getBiome(int x, int y, int z, BiomeType defaultBiomeType) {
                final Vector3 current = Vector3.at(x, y, z);
                environment.setCurrentBlock(current);
                final Vector3 inputPosition = transformInverse.apply(current);

                try {
                    if (expression.evaluate(new double[]{ inputPosition.x(), inputPosition.y(), inputPosition.z() }, timeout) <= 0) {
                        return null;
                    }

                    // TODO: Allow biome setting via a script variable (needs BiomeType<->int mapping)
                    return defaultBiomeType;
                } catch (ExpressionTimeoutException _) {
                    timedOut.getAndIncrement();
                    return null;
                } catch (Exception e) {
                    LOGGER.warn("Failed to create shape", e);
                    return null;
                }
            }
        };
        int changed = shape.generate(session, biomeType, hollow);
        if (timedOut.get() > 0) {
            throw new ExpressionTimeoutException(
                    String.format("%d biomes changed. %d biomes took too long to evaluate (increase time with //timeout)",
                            changed, timedOut.get()));
        }
        return changed;
    }

    private ExpressionOperations() {
    }
}
