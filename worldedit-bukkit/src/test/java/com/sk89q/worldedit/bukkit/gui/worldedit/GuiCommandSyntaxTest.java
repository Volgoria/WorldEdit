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

package com.sk89q.worldedit.bukkit.gui.worldedit;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.blocks.BaseItem;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.internal.annotation.ClipboardMask;
import com.sk89q.worldedit.internal.annotation.SchematicPath;
import com.sk89q.worldedit.internal.command.CommandArgParser;
import com.sk89q.worldedit.internal.util.Substring;
import com.sk89q.worldedit.world.generation.TreeType;
import org.enginehub.piston.ArgBinding;
import org.enginehub.piston.CommandManager;
import org.enginehub.piston.CommandParseResult;
import org.enginehub.piston.converter.ArgumentConverter;
import org.enginehub.piston.converter.SimpleArgumentConverter;
import org.enginehub.piston.converter.SuccessfulConversion;
import org.enginehub.piston.inject.InjectedValueStore;
import org.enginehub.piston.inject.Key;
import org.enginehub.piston.inject.MapBackedValueStore;
import org.enginehub.piston.part.ArgAcceptingCommandPart;
import org.enginehub.piston.part.CommandPart;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Parses every command the GUI can run with WorldEdit's real command tree, so
 * that a template with a wrong sub-command, flag, argument count or number
 * fails here rather than in game.
 */
class GuiCommandSyntaxTest {

    private static final Set<Class<?>> NUMBER_TYPES = Set.of(int.class, Integer.class, double.class, Double.class);

    private static CommandManager commandManager;
    private static InjectedValueStore context;

    @BeforeAll
    static void setUp() {
        commandManager = WorldEdit.getInstance().getPlatformManager().getPlatformCommandManager().getCommandManager();
        Actor actor = mock(Actor.class);
        when(actor.hasPermission(anyString())).thenReturn(true);
        context = MapBackedValueStore.create();
        context.injectValue(Key.of(Actor.class), _ -> Optional.of(actor));
        // Patterns and masks need block registries and a session, which this test
        // does not have: accept any value, their syntax is tested in the core
        ArgumentConverter<Object> anything = SimpleArgumentConverter.from(
            (input, _) -> SuccessfulConversion.fromSingle(input), "anything");
        stubConverter(Key.of(Pattern.class), anything);
        stubConverter(Key.of(Mask.class), anything);
        stubConverter(Key.of(Mask.class, ClipboardMask.class), anything);
        stubConverter(Key.of(BaseItem.class), anything);
        // The game fills this registry
        if (TreeType.REGISTRY.get("minecraft:oak") == null) {
            TreeType.REGISTRY.register("minecraft:oak", new TreeType("minecraft:oak"));
        }
        // Schematic paths are looked up in the schematics folder
        stubConverter(Key.of(Path.class, SchematicPath.class),
            SimpleArgumentConverter.from((input, _) -> SuccessfulConversion.fromSingle(Path.of(input)), "path"));
    }

    @SuppressWarnings("unchecked")
    private static <T> void stubConverter(Key<T> key, ArgumentConverter<?> converter) {
        commandManager.registerConverter(key, (ArgumentConverter<T>) (ArgumentConverter<?>) converter);
    }

    private static List<GuiAction> allActions() {
        List<GuiAction> actions = new ArrayList<>();
        actions.addAll(List.of(BrushType.values()));
        actions.addAll(List.of(ToolAction.values()));
        actions.addAll(List.of(BuildAction.values()));
        actions.addAll(List.of(SelectionAction.values()));
        actions.addAll(List.of(GenerationShape.values()));
        return actions;
    }

    private static String sampleInput(InputKind kind) {
        return switch (kind) {
            case TEXT -> "Hello world";
            case FILE_NAME -> "builds/house";
            case FILE -> "builds/house.schem";
            case FILE_LIST -> "trees,#clipboard";
            case NUMBER -> "-1";
            case PATTERN_OR_MASK -> "stone !air";
        };
    }

    private static List<String> commands() {
        List<String> commands = new ArrayList<>();
        List<CommandContext> contexts = List.of(
            new CommandContext("stone", List.of(), null, 1, false),
            new CommandContext("50%stone,50%dirt", List.of("stone", "dirt"), "grass_block", 6, true),
            new CommandContext("stone !air", List.of("stone"), "dirt grass_block", 100, true)
        );
        for (GuiAction action : allActions()) {
            ActionSpec spec = action.spec();
            String input = spec.needsInput() ? spec.input().toArgument(sampleInput(spec.input())) : null;
            for (CommandContext commandContext : contexts) {
                commands.add(GuiCommands.action(action, commandContext, input));
            }
        }
        commands.add(GuiCommands.unbindBrush());
        commands.add(GuiCommands.unbindTool());
        for (String format : GuiCommands.SAVE_FORMATS) {
            commands.add(GuiCommands.schematicSave("builds/house", format, false));
            commands.add(GuiCommands.schematicSave("builds/house", format, true));
        }
        commands.add(GuiCommands.schematicLoad("builds/my house.schem"));
        commands.add(GuiCommands.schematicInfo("house.schem"));
        commands.add(GuiCommands.schematicRename("house.schem", "builds/castle"));
        commands.add(GuiCommands.schematicCopy("house.schem", "castle"));
        commands.add(GuiCommands.image("art/logo.png", false, false));
        commands.add(GuiCommands.image("art/logo.png", true, true));
        commands.add(GuiCommands.topView("map"));
        commands.add(GuiCommands.heightmapImport("hills.png"));
        commands.add(GuiCommands.heightmapExport("hills"));
        commands.add(GuiCommands.globalMask(null));
        commands.add(GuiCommands.globalMask("stone !air"));
        commands.add(GuiCommands.brushMask(null));
        commands.add(GuiCommands.brushMask("stone"));
        commands.add(GuiCommands.limit(null));
        commands.add(GuiCommands.limit(-1));
        commands.add(GuiCommands.limit(5000));
        commands.add(GuiCommands.timeout(null));
        commands.add(GuiCommands.timeout(100));
        commands.add(GuiCommands.fast(true));
        commands.add(GuiCommands.fast(false));
        commands.add(GuiCommands.sideEffect("LIGHTING", true));
        commands.add(GuiCommands.sideEffect("NEIGHBORS", false));
        commands.add("//perf -h");
        commands.add("//history");
        for (int angle : GuiCommands.ROTATIONS) {
            commands.add(GuiCommands.rotate(angle));
        }
        commands.add(GuiCommands.flip());
        commands.add(GuiCommands.clearClipboard());
        commands.add(GuiCommands.copy());
        commands.add(GuiCommands.paste());
        commands.add(GuiCommands.history(false, 1));
        commands.add(GuiCommands.history(true, 3));
        commands.add(GuiCommands.clearHistory());
        return commands;
    }

    private static List<String> split(String command) {
        assertFalse(command.isEmpty());
        assertEquals('/', command.charAt(0), command);
        // As PlatformCommandManager does: drop the leading slash, then split
        return CommandArgParser.forArgString(command.substring(1)).parseArgs().map(Substring::getSubstring).toList();
    }

    @Test
    void everyGuiCommandParses() {
        List<String> problems = new ArrayList<>();
        for (String command : commands()) {
            try {
                CommandParseResult result = commandManager.parse(context, split(command));
                checkNumbers(command, result, problems);
            } catch (RuntimeException e) {
                problems.add(command + " -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
        assertEquals(List.of(), problems);
    }

    /**
     * Numbers are only converted when a command runs, so convert them here.
     */
    private static void checkNumbers(String command, CommandParseResult result, List<String> problems) {
        for (ArgBinding binding : result.getBoundArguments()) {
            for (CommandPart part : binding.getParts()) {
                if (!(part instanceof ArgAcceptingCommandPart argPart)) {
                    continue;
                }
                for (Key<?> type : argPart.getTypes()) {
                    if (type.getAnnotationType() == null && NUMBER_TYPES.contains(type.getTypeToken().getRawType())) {
                        try {
                            result.getParameters().valueOf(argPart).asSingle(type);
                        } catch (RuntimeException e) {
                            problems.add(command + " -> " + binding.getInput() + " is not a number: " + e.getMessage());
                        }
                    }
                }
            }
        }
    }

    @Test
    void pluginCommandsDoNotShadowWorldEditCommands() throws IOException {
        Map<String, Object> pluginYml;
        try (InputStream in = getClass().getResourceAsStream("/plugin.yml")) {
            assertNotNull(in, "plugin.yml");
            pluginYml = new Yaml().load(in);
        }
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> commands = (Map<String, Map<String, Object>>) pluginYml.get("commands");
        List<String> problems = new ArrayList<>();
        commands.forEach((name, spec) -> {
            List<String> names = new ArrayList<>();
            names.add(name);
            Object aliases = spec.get("aliases");
            if (aliases instanceof List<?> list) {
                list.forEach(alias -> names.add(alias.toString()));
            } else if (aliases != null) {
                names.add(aliases.toString());
            }
            for (String alias : names) {
                if (commandManager.containsCommand(alias)) {
                    problems.add("plugin.yml command " + alias + " is also a WorldEdit command");
                }
            }
        });
        assertEquals(List.of(), problems);
    }
}
