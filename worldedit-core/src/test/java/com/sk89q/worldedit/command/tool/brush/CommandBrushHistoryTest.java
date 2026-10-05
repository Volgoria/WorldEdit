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

package com.sk89q.worldedit.command.tool.brush;

import com.sk89q.worldedit.BaseWorldEditTest;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.eventbus.EventBus;
import com.sk89q.worldedit.util.test.InMemoryWorld;
import com.sk89q.worldedit.util.test.SimpleMaterialRegistries;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Checks that a command brush leaves exactly the history of the commands it runs.
 */
class CommandBrushHistoryTest extends BaseWorldEditTest {

    private static BlockState air;
    private static BlockState stone;

    @BeforeAll
    static void setUpBlocks() {
        when(MOCKED_PLATFORM.getRegistries()).thenReturn(SimpleMaterialRegistries.create());
        air = new BlockType("cmdbrushtest:air").getDefaultState();
        stone = new BlockType("cmdbrushtest:stone").getDefaultState();
    }

    @Test
    void undoAfterCommandBrushUndoesTheCommand() throws Exception {
        InMemoryWorld world = new InMemoryWorld(air, -64, 319);
        BlockVector3 target = BlockVector3.at(3, 10, -2);
        Player player = mock(Player.class, Answers.RETURNS_SMART_NULLS);
        doReturn(world).when(player).getWorld();
        doReturn(world).when(player).getExtent();
        doReturn(true).when(player).isPlayer();
        doReturn("Steve").when(player).getName();
        doReturn(true).when(player).hasPermission(anyString());
        doReturn(new Location(world, target.toVector3())).when(player).getBlockTrace(anyInt(), anyBoolean(), any());
        LocalSession session = spy(new LocalSession(WorldEdit.getInstance().getConfiguration()));

        // Behaves like a command run through the platform: its own edit session, remembered once done
        CommandBrush brush = new CommandBrush("//setblock {x},{y},{z}", (actor, _) -> {
            EditSession editSession = session.createEditSession(actor);
            try (editSession) {
                editSession.setBlock(target, stone);
            } catch (Exception e) {
                throw new AssertionError(e);
            }
            session.remember(editSession);
        }, new EventBus());
        BrushTool tool = new BrushTool(brush, "worldedit.brush.command");

        tool.actPrimary(MOCKED_PLATFORM, WorldEdit.getInstance().getConfiguration(), player, session);

        assertSame(stone, world.blocks().get(target));
        // Only the command opened an edit session, so only its edit is in the history
        verify(session, times(1)).createEditSession(any());
        assertEquals(1, session.getHistory().size());

        assertNotNull(session.undo(null, player));
        assertEquals(air, world.getBlock(target));
        assertEquals(0, session.getHistoryPointer());
    }
}
