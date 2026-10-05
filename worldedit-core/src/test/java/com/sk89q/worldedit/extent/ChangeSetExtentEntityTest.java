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

package com.sk89q.worldedit.extent;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.history.UndoContext;
import com.sk89q.worldedit.history.change.Change;
import com.sk89q.worldedit.history.change.EntityMove;
import com.sk89q.worldedit.history.changeset.ArrayListHistory;
import com.sk89q.worldedit.util.Location;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChangeSetExtentEntityTest {

    private Extent world;
    private Entity entity;
    private Location from;
    private Location to;
    private ArrayListHistory changeSet;
    private ChangeSetExtent extent;

    @BeforeEach
    void setUp() {
        world = mock(Extent.class);
        entity = mock(Entity.class);
        from = new Location(world, 1, 2, 3);
        to = new Location(world, 10, 20, 30);
        when(entity.getLocation()).thenReturn(from);
        doReturn(List.of(entity)).when(world).getEntities();
        changeSet = new ArrayListHistory();
        extent = new ChangeSetExtent(world, changeSet);
    }

    private Entity trackedEntity() {
        List<? extends Entity> entities = extent.getEntities();
        assertEquals(1, entities.size());
        return entities.getFirst();
    }

    @Test
    void successfulMoveIsRecorded() {
        when(entity.setLocation(to)).thenReturn(true);

        assertTrue(trackedEntity().setLocation(to));

        assertEquals(1, changeSet.size());
        Change change = changeSet.forwardIterator().next();
        EntityMove move = assertInstanceOf(EntityMove.class, change);
        assertSame(entity, move.getEntity());
        assertEquals(from, move.getPrevious());
        assertEquals(to, move.getCurrent());
    }

    @Test
    void failedMoveIsNotRecorded() {
        when(entity.setLocation(to)).thenReturn(false);

        assertFalse(trackedEntity().setLocation(to));

        assertEquals(0, changeSet.size());
    }

    @Test
    void undoAndRedoMoveEntity() throws WorldEditException {
        when(entity.setLocation(to)).thenReturn(true);
        trackedEntity().setLocation(to);

        UndoContext context = new UndoContext();
        context.setExtent(world);
        Iterator<Change> backward = changeSet.backwardIterator();
        backward.next().undo(context);
        verify(entity).setLocation(from);

        Iterator<Change> forward = changeSet.forwardIterator();
        forward.next().redo(context);
        // once from the original move, once from the redo
        verify(entity, times(2)).setLocation(to);
    }
}
