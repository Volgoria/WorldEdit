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

package com.sk89q.worldedit.extension.platform;

import com.sk89q.worldedit.BaseWorldEditTest;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.util.PermissionCondition;
import org.enginehub.piston.Command;
import org.enginehub.piston.CommandManager;
import org.enginehub.piston.part.SubCommandPart;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the whole command tree registered by {@link PlatformCommandManager}.
 */
class CommandTreeTest extends BaseWorldEditTest {

    private static final Pattern PERMISSION = Pattern.compile("worldedit(\\.[a-z0-9]+(-[a-z0-9]+)*)+");

    /**
     * Commands that are deliberately usable by anyone, such as those only
     * clearing a tool or showing information.
     */
    private static final Set<String> WITHOUT_PERMISSION = Set.of(
        "/sel", "toggleplace", "brush paint forest", "brush paint set", "brush none", "brush apply forest",
        "brush apply set", "worldedit version", "worldedit trace", "worldedit cui", "worldedit tz", "/expand vert",
        "none", "tool none"
    );

    private static CommandManager commandManager() {
        return WorldEdit.getInstance().getPlatformManager().getPlatformCommandManager().getCommandManager();
    }

    private static List<Command> subCommands(Command command) {
        List<Command> result = new ArrayList<>();
        command.getParts().stream()
            .filter(SubCommandPart.class::isInstance)
            .forEach(part -> result.addAll(((SubCommandPart) part).getCommands()));
        return result;
    }

    /**
     * Platforms look commands up case-insensitively, so two commands of a level
     * must not share a name or alias, whatever the case.
     */
    private static void checkLevel(String path, Collection<Command> commands, List<String> problems) {
        Map<String, String> owners = new HashMap<>();
        for (Command command : commands) {
            List<String> names = new ArrayList<>();
            names.add(command.getName());
            names.addAll(command.getAliases());
            for (String name : names) {
                String key = name.toLowerCase(Locale.ROOT);
                String previous = owners.putIfAbsent(key, command.getName());
                if (previous != null && !previous.equals(command.getName())) {
                    problems.add(path + ": '" + name + "' is used by both " + previous + " and " + command.getName());
                }
            }
            checkLevel(path + " " + command.getName(), subCommands(command), problems);
        }
    }

    @Test
    void noNameOrAliasIsSharedByTwoCommands() {
        List<String> problems = new ArrayList<>();
        checkLevel("", commandManager().getAllCommands().toList(), problems);
        assertEquals(List.of(), problems);
    }

    @Test
    void everyNameAndAliasResolvesToItsCommand() {
        CommandManager manager = commandManager();
        List<String> problems = new ArrayList<>();
        manager.getAllCommands().forEach(command -> {
            List<String> names = new ArrayList<>(command.getAliases());
            names.add(command.getName());
            for (String name : names) {
                Optional<Command> found = manager.getCommand(name);
                if (found.isEmpty() || found.get() != command) {
                    problems.add(name + " does not resolve to " + command.getName());
                }
            }
        });
        assertEquals(List.of(), problems);
    }

    private static void checkPermissions(String path, Command command, List<String> problems) {
        String fullName = path + " " + command.getName();
        if (command.getCondition() instanceof PermissionCondition permissionCondition) {
            for (String permission : permissionCondition.getPermissions()) {
                if (!PERMISSION.matcher(permission).matches()) {
                    problems.add(fullName + ": malformed permission " + permission);
                }
            }
        } else if (!WITHOUT_PERMISSION.contains(fullName.strip())) {
            problems.add(fullName + ": no permission check");
        }
        for (Command sub : subCommands(command)) {
            checkPermissions(fullName, sub, problems);
        }
    }

    @Test
    void everyCommandHasAWellFormedPermission() {
        List<String> problems = new ArrayList<>();
        commandManager().getAllCommands().forEach(command -> checkPermissions("", command, problems));
        assertEquals(List.of(), problems);
    }

    @Test
    void newCommandsAreRegistered() {
        CommandManager manager = commandManager();
        for (String name : List.of("/heightmap", "/hmap", "/image", "/topview", "/text", "/symmetry", "/arch",
            "/helix", "/torus", "/dome", "/disk")) {
            assertTrue(manager.containsCommand(name), name);
        }
    }
}
