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

import com.google.common.collect.ImmutableList;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.event.platform.CommandEvent;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Runs WorldEdit commands at the targeted block.
 *
 * <p>Several commands may be given, separated with {@code ;}. Before a
 * command runs, the placeholders {@code {x}}, {@code {y}}, {@code {z}}
 * (the targeted block), {@code {size}} (the brush size), {@code {world}} and
 * {@code {player}} are replaced. Commands run as the player using the brush,
 * so all the usual permission checks and limits apply to them.</p>
 */
public class CommandBrush implements PlayerBrush {

    /**
     * The largest number of commands one brush may run per use.
     */
    public static final int MAX_COMMANDS = 16;

    /**
     * Set while a command brush runs its commands on this thread, so that a command
     * which (directly or not) uses a command brush again cannot recurse.
     */
    private static final ThreadLocal<Boolean> RUNNING = ThreadLocal.withInitial(() -> false);

    private final List<String> commands;
    private final BiConsumer<Player, String> dispatcher;

    /**
     * Create a new command brush that dispatches commands to WorldEdit.
     *
     * @param commands the commands, separated with {@code ;}
     */
    public CommandBrush(String commands) {
        this(commands, (player, command) -> WorldEdit.getInstance().getEventBus().post(new CommandEvent(player, command)));
    }

    /**
     * Create a new command brush.
     *
     * @param commands the commands, separated with {@code ;}
     * @param dispatcher runs a command, starting with {@code /}, as the given player
     */
    public CommandBrush(String commands, BiConsumer<Player, String> dispatcher) {
        checkNotNull(commands);
        checkNotNull(dispatcher);
        this.commands = parseCommands(commands);
        checkArgument(!this.commands.isEmpty(), "at least one command is required");
        checkArgument(this.commands.size() <= MAX_COMMANDS, "at most " + MAX_COMMANDS + " commands are allowed");
        this.dispatcher = dispatcher;
    }

    /**
     * Get the command templates of this brush.
     *
     * @return the commands, each starting with {@code /}
     */
    public List<String> getCommands() {
        return commands;
    }

    /**
     * Split a list of commands separated with {@code ;}, and make every
     * command start with {@code /}.
     *
     * @param commands the commands
     * @return the parsed commands
     */
    public static List<String> parseCommands(String commands) {
        return Arrays.stream(commands.split(";"))
            .map(String::trim)
            .filter(command -> !command.isEmpty() && !command.equals("/"))
            .map(command -> command.startsWith("/") ? command : "/" + command)
            .collect(ImmutableList.toImmutableList());
    }

    /**
     * Replace the placeholders of a command.
     *
     * @param command the command template
     * @param position the targeted block
     * @param size the brush size
     * @param worldName the name of the world
     * @param playerName the name of the player
     * @return the command to run
     */
    public static String expand(String command, BlockVector3 position, double size, String worldName, String playerName) {
        String formattedSize = size == Math.rint(size)
            ? Long.toString((long) size)
            : String.format(Locale.ROOT, "%.2f", size);
        return command
            .replace("{x}", Integer.toString(position.x()))
            .replace("{y}", Integer.toString(position.y()))
            .replace("{z}", Integer.toString(position.z()))
            .replace("{size}", formattedSize)
            .replace("{world}", worldName)
            .replace("{player}", playerName);
    }

    @Override
    public void build(Player player, LocalSession session, EditSession editSession, BlockVector3 position,
                      @Nullable Pattern pattern, double size) {
        if (RUNNING.get()) {
            // A brush command ended up using a command brush again
            return;
        }
        String worldName = player.getWorld().getName();
        RUNNING.set(true);
        try {
            for (String command : commands) {
                dispatcher.accept(player, expand(command, position, size, worldName, player.getName()));
            }
        } finally {
            RUNNING.set(false);
        }
    }

    @Override
    public void build(EditSession editSession, BlockVector3 position, Pattern pattern, double size) {
        // Commands need a player to run as, see build(Player, ...)
    }
}
