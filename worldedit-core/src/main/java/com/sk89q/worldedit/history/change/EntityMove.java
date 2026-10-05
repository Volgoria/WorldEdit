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

package com.sk89q.worldedit.history.change;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.history.UndoContext;
import com.sk89q.worldedit.util.Location;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Tracks the movement of an entity from one location to another.
 *
 * <p>Undoing the change moves the entity back to its previous location,
 * and redoing it moves the entity to its new location again. If the
 * entity no longer exists, the change has no effect.</p>
 */
public class EntityMove implements Change {

    private final Entity entity;
    private final Location previous;
    private final Location current;

    /**
     * Create a new instance.
     *
     * @param entity the entity that was moved
     * @param previous the location of the entity before the move
     * @param current the location of the entity after the move
     */
    public EntityMove(Entity entity, Location previous, Location current) {
        checkNotNull(entity);
        checkNotNull(previous);
        checkNotNull(current);
        this.entity = entity;
        this.previous = previous;
        this.current = current;
    }

    /**
     * Get the entity that was moved.
     *
     * @return the entity
     */
    public Entity getEntity() {
        return entity;
    }

    /**
     * Get the location of the entity before the move.
     *
     * @return the previous location
     */
    public Location getPrevious() {
        return previous;
    }

    /**
     * Get the location of the entity after the move.
     *
     * @return the new location
     */
    public Location getCurrent() {
        return current;
    }

    @Override
    public void undo(UndoContext context) throws WorldEditException {
        entity.setLocation(previous);
    }

    @Override
    public void redo(UndoContext context) throws WorldEditException {
        entity.setLocation(current);
    }

}
