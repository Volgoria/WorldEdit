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

package com.sk89q.worldedit.function.factory;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.function.EditContext;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.RunContext;
import com.sk89q.worldedit.internal.expression.Expression;
import com.sk89q.worldedit.internal.expression.ExpressionTimeoutException;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeformTest {

    private static Operation createOperation(EditSession editSession) {
        EditContext context = new EditContext();
        context.setDestination(editSession);
        context.setRegion(new CuboidRegion(BlockVector3.ZERO, BlockVector3.at(2, 2, 2)));
        context.setSession(new LocalSession());
        return new Deform("y-=1", Deform.Mode.RAW_COORD).createFromContext(context);
    }

    @Test
    void expressionFailureIsPropagatedUnwrapped() throws Exception {
        EditSession editSession = mock(EditSession.class);
        ExpressionTimeoutException failure = new ExpressionTimeoutException("Calculations exceeded time limit.");
        when(editSession.deformRegion(any(), any(), any(Expression.class), anyInt(), any(), any()))
            .thenThrow(failure);

        Operation operation = createOperation(editSession);

        ExpressionTimeoutException thrown = assertThrows(ExpressionTimeoutException.class,
            () -> operation.resume(new RunContext()));
        assertSame(failure, thrown);
    }

    @Test
    void successfulDeformCompletes() throws Exception {
        EditSession editSession = mock(EditSession.class);
        when(editSession.deformRegion(any(), any(), any(Expression.class), anyInt(), any(), any()))
            .thenReturn(27);

        assertNull(createOperation(editSession).resume(new RunContext()));
    }
}
