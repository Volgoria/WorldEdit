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

package com.sk89q.worldedit.command.util;

import com.sk89q.worldedit.BaseWorldEditTest;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.MaxRadiusException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;
import org.enginehub.piston.exception.CommandException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CommandHelperTest extends BaseWorldEditTest {

    @Test
    void printAffectedPrintsCountAndReturnsIt() {
        Actor actor = mock(Actor.class);
        assertEquals(42, CommandHelper.printAffected(actor, "worldedit.set.done", 42));

        ArgumentCaptor<Component> captor = ArgumentCaptor.forClass(Component.class);
        verify(actor).printInfo(captor.capture());
        TranslatableComponent message = (TranslatableComponent) captor.getValue();
        assertEquals("worldedit.set.done", message.key());
        assertEquals(List.of(TextComponent.of(42)), message.args());
    }

    @Test
    void findFreePositionOnlyMovesPlayers() {
        Player player = mock(Player.class);
        CommandHelper.findFreePosition(player);
        verify(player).findFreePosition();

        assertDoesNotThrow(() -> CommandHelper.findFreePosition(mock(Actor.class)));
    }

    @ParameterizedTest
    @ValueSource(doubles = { Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY })
    void checkFiniteRejectsNonFiniteNumbers(double value) {
        assertThrows(CommandException.class, () -> CommandHelper.checkFinite(value));
    }

    @ParameterizedTest
    @ValueSource(doubles = { 0, -3.5, 1e9 })
    void checkFiniteAcceptsFiniteNumbers(double value) {
        assertDoesNotThrow(() -> CommandHelper.checkFinite(value));
    }

    @Test
    void checkRadiiAppliesConfiguredMaximum() {
        LocalConfiguration config = WorldEdit.getInstance().getConfiguration();
        int previous = config.maxRadius;
        config.maxRadius = 10;
        try {
            assertDoesNotThrow(() -> CommandHelper.checkRadii(WorldEdit.getInstance(), 1, 10));
            assertThrows(MaxRadiusException.class, () -> CommandHelper.checkRadii(WorldEdit.getInstance(), 1, 11));
            assertThrows(CommandException.class,
                () -> CommandHelper.checkRadii(WorldEdit.getInstance(), Double.POSITIVE_INFINITY));
        } finally {
            config.maxRadius = previous;
        }
    }

    @Test
    void checkRadiiRejectsInfinityWithoutConfiguredMaximum() {
        assertThrows(CommandException.class,
            () -> CommandHelper.checkRadii(WorldEdit.getInstance(), 5, Double.POSITIVE_INFINITY));
    }

    @Test
    void expandRadiiRepeatsSingleRadius() {
        assertArrayEquals(new double[] { 4, 4, 4 }, CommandHelper.expandRadii(List.of(4.0), 3, 0));
    }

    @Test
    void expandRadiiKeepsOneRadiusPerAxis() {
        assertArrayEquals(new double[] { 1, 2, 3 }, CommandHelper.expandRadii(List.of(1.0, 2.0, 3.0), 3, 0));
    }

    @Test
    void expandRadiiAppliesMinimum() {
        assertArrayEquals(new double[] { 1, 5 }, CommandHelper.expandRadii(List.of(-2.0, 5.0), 2, 1));
        assertArrayEquals(new double[] { 0, 0, 0 }, CommandHelper.expandRadii(List.of(-1.0), 3, 0));
    }

    @Test
    void expandRadiiKeepsNaNSoItCanBeRejected() {
        double[] radii = CommandHelper.expandRadii(List.of(Double.NaN), 2, 1);
        assertNotNull(radii);
        assertEquals(2, radii.length);
        assertThrows(CommandException.class, () -> CommandHelper.checkFinite(radii[0]));
    }

    @Test
    void expandRadiiRejectsWrongCount() {
        assertNull(CommandHelper.expandRadii(List.of(1.0, 2.0), 3, 0));
        assertNull(CommandHelper.expandRadii(List.of(1.0, 2.0, 3.0), 2, 0));
        assertNull(CommandHelper.expandRadii(List.of(), 2, 0));
    }
}
