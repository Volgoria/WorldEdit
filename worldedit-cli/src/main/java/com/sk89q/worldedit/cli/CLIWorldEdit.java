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

package com.sk89q.worldedit.cli;

import com.google.common.collect.ImmutableList;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.cli.data.DataFile;
import com.sk89q.worldedit.cli.data.FileRegistries;
import com.sk89q.worldedit.cli.schematic.ClipboardWorld;
import com.sk89q.worldedit.event.platform.CommandEvent;
import com.sk89q.worldedit.event.platform.ConfigurationLoadEvent;
import com.sk89q.worldedit.event.platform.PlatformReadyEvent;
import com.sk89q.worldedit.event.platform.PlatformsRegisteredEvent;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.extension.platform.PlatformCommandManager;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.util.FileDialogUtil;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BlockCategory;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.FuzzyBlockState;
import com.sk89q.worldedit.world.entity.EntityType;
import com.sk89q.worldedit.world.item.ItemCategory;
import com.sk89q.worldedit.world.item.ItemType;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Logger;

import java.awt.HeadlessException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import javax.annotation.Nullable;

/**
 * The CLI implementation of WorldEdit.
 */
public class CLIWorldEdit {

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    static final int EXIT_OK = 0;
    static final int EXIT_ERROR = 1;
    static final int EXIT_USAGE = 2;

    private static final Path DEFAULT_WORKING_DIR = Paths.get("worldedit");

    public static CLIWorldEdit inst;

    private CLIPlatform platform;
    private CLIConfiguration config;
    private final Path workingDir;
    private String version;
    private boolean started;

    private Actor commandSender;
    private int saveFailures;
    private final SaveFailureReporter saveFailureReporter = new SaveFailureReporter();

    private FileRegistries fileRegistries;

    public CLIWorldEdit() {
        this(DEFAULT_WORKING_DIR);
    }

    /**
     * Create a new instance using the given working directory.
     *
     * @param workingDir the directory to store WorldEdit's files in
     */
    public CLIWorldEdit(Path workingDir) {
        this.workingDir = workingDir;
        inst = this;
    }

    private void setupPlatform() {
        WorldEdit.getInstance().getPlatformManager().register(platform);

        registerCommands();

        config = new CLIConfiguration(this);

        // There's no other platforms, so fire this immediately
        WorldEdit.getInstance().getEventBus().post(new PlatformsRegisteredEvent());

        this.fileRegistries = new FileRegistries(this);
    }

    private void registerCommands() {
        PlatformCommandManager pcm = WorldEdit.getInstance().getPlatformManager()
            .getPlatformCommandManager();
        if (pcm.getCommandManager().containsCommand("cli")) {
            // Already registered by an earlier run in this JVM; the commands are stateless
            return;
        }
        pcm.registerSubCommands(
            "cli",
            ImmutableList.of(),
            "CLI-specific commands",
            CLIExtraCommandsRegistration.builder(),
            new CLIExtraCommands()
        );
    }

    public void setupRegistries() {
        this.fileRegistries.loadDataFiles();

        // Blocks
        BlockType.REGISTRY.clear();
        for (Map.Entry<String, DataFile.BlockManifest> manifestEntry : fileRegistries.getDataFile().blocks().entrySet()) {
            if (BlockType.REGISTRY.get(manifestEntry.getKey()) == null) {
                BlockType.REGISTRY.register(manifestEntry.getKey(), new BlockType(manifestEntry.getKey(), input -> {
                    ParserContext context = new ParserContext();
                    context.setPreferringWildcard(true);
                    context.setTryLegacy(false);
                    context.setRestricted(false);
                    try {
                        FuzzyBlockState state = (FuzzyBlockState) WorldEdit.getInstance().getBlockFactory().parseFromInput(
                            manifestEntry.getValue().defaultState(),
                            context
                        ).toImmutableState();
                        BlockState defaultState = input.getBlockType().getAllStates().get(0);
                        for (Map.Entry<Property<?>, Object> propertyObjectEntry : state.getStates().entrySet()) {
                            @SuppressWarnings("unchecked")
                            Property<Object> prop = (Property<Object>) propertyObjectEntry.getKey();
                            defaultState = defaultState.with(prop, propertyObjectEntry.getValue());
                        }
                        return defaultState;
                    } catch (InputParseException e) {
                        LOGGER.warn("Error loading block state for " + manifestEntry.getKey(), e);
                        return input;
                    }
                }));
            }
        }
        // Items
        ItemType.REGISTRY.clear();
        for (String name : fileRegistries.getDataFile().items()) {
            if (ItemType.REGISTRY.get(name) == null) {
                ItemType.REGISTRY.register(name, new ItemType(name));
            }
        }
        // Entities
        EntityType.REGISTRY.clear();
        for (String name : fileRegistries.getDataFile().entities()) {
            if (EntityType.REGISTRY.get(name) == null) {
                EntityType.REGISTRY.register(name, new EntityType(name));
            }
        }
        // Biomes
        BiomeType.REGISTRY.clear();
        for (String name : fileRegistries.getDataFile().biomes()) {
            if (BiomeType.REGISTRY.get(name) == null) {
                BiomeType.REGISTRY.register(name, new BiomeType(name));
            }
        }
        // Tags
        BlockCategory.REGISTRY.clear();
        for (String name : fileRegistries.getDataFile().blockTags().keySet()) {
            if (BlockCategory.REGISTRY.get(name) == null) {
                BlockCategory.REGISTRY.register(name, new BlockCategory(name));
            }
        }
        ItemCategory.REGISTRY.clear();
        for (String name : fileRegistries.getDataFile().itemTags().keySet()) {
            if (ItemCategory.REGISTRY.get(name) == null) {
                ItemCategory.REGISTRY.register(name, new ItemCategory(name));
            }
        }
    }

    public void onInitialized() {
        // Setup working directory
        try {
            Files.createDirectories(workingDir);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create working directory " + workingDir, e);
        }

        this.commandSender = new CLICommandSender(LOGGER);
        this.platform = new CLIPlatform(this);
        LOGGER.info("WorldEdit CLI (version " + getInternalVersion() + ") is loaded");
    }

    public void onStarted() {
        setupPlatform();
        started = true;

        setupRegistries();

        config.load();
        WorldEdit.getInstance().getEventBus().post(new ConfigurationLoadEvent(config));

        WorldEdit.getInstance().getEventBus().post(new PlatformReadyEvent(platform));
    }

    public void onStopped() {
        if (platform == null) {
            return;
        }
        platform.shutdown();
        if (!started) {
            // Nothing was registered with WorldEdit
            return;
        }
        started = false;
        WorldEdit worldEdit = WorldEdit.getInstance();
        worldEdit.getSessionManager().unload();
        worldEdit.getPlatformManager().unregister(platform);
    }

    public FileRegistries getFileRegistries() {
        return this.fileRegistries;
    }

    /**
     * Get the configuration.
     *
     * @return the CLI configuration
     */
    CLIConfiguration getConfig() {
        return this.config;
    }

    /**
     * Get the WorldEdit proxy for the platform.
     *
     * @return the WorldEdit platform
     */
    public Platform getPlatform() {
        return this.platform;
    }

    /**
     * Get the working directory where WorldEdit's files are stored.
     *
     * @return the working directory
     */
    public Path getWorkingDir() {
        return this.workingDir;
    }

    /**
     * Get the version of the WorldEdit-CLI implementation.
     *
     * @return a version string
     */
    String getInternalVersion() {
        if (version == null) {
            version = getClass().getPackage().getImplementationVersion();
        }
        return version;
    }

    /**
     * Save every modified world (or every world, if forced). Failures are
     * reported to the user and counted, see {@link #getSaveFailures()}.
     *
     * @param force whether to save unmodified worlds too
     */
    public void saveAllWorlds(boolean force) {
        saveAllWorlds(force, false);
    }

    /**
     * Save every modified world (or every world, if forced).
     *
     * <p>Failures are counted, see {@link #getSaveFailures()}. The full error
     * is shown the first time a world fails to save; repeated failures of the
     * same world are only logged at debug level, with a short reminder on the
     * last save before exiting.</p>
     *
     * @param force whether to save unmodified worlds too
     * @param exiting whether this is the last save before the CLI exits
     */
    private void saveAllWorlds(boolean force, boolean exiting) {
        for (World world : platform.getWorlds()) {
            if (!(world instanceof CLIWorld cliWorld)) {
                continue;
            }
            try {
                cliWorld.save(force);
                saveFailureReporter.saved(world.getName());
            } catch (IOException e) {
                saveFailures++;
                LOGGER.debug("Failed to save " + world.getName(), e);
                switch (saveFailureReporter.failed(world.getName(), exiting)) {
                    case FULL -> commandSender.printError(TranslatableComponent.of("worldedit.cli.save-failed",
                        TextComponent.of(world.getName()), TextComponent.of(String.valueOf(e.getMessage()))));
                    case REMINDER -> commandSender.printError(TranslatableComponent.of(
                        "worldedit.cli.save-failed-reminder", TextComponent.of(world.getName())));
                    default -> {
                        // Already reported
                    }
                }
            }
        }
    }

    /**
     * Get how many times saving a world failed.
     *
     * @return the number of failed saves
     */
    public int getSaveFailures() {
        return saveFailures;
    }

    /**
     * Load the given file as the world to edit, starting up the platform.
     *
     * @param file the file
     * @param format the detected format of the file
     * @throws IOException if the file could not be read
     */
    void loadWorld(Path file, ClipboardFormat format) throws IOException {
        LOGGER.info(() -> "Loading '" + file + "'...");
        platform.setDataVersion(CLIFiles.readDataVersion(format, file));
        onStarted();
        ClipboardWorld world;
        try (InputStream stream = Files.newInputStream(file);
             ClipboardReader clipboardReader = format.getReader(stream)) {
            world = new ClipboardWorld(
                file.toFile(),
                format,
                clipboardReader.read(),
                String.valueOf(file.getFileName())
            );
        }
        platform.addWorld(world);
        WorldEdit.getInstance().getSessionManager().get(commandSender).setWorldOverride(world);
        LOGGER.info(() -> "Loaded '" + file + "'");
        if (!world.canSave()) {
            commandSender.printError(TranslatableComponent.of("worldedit.cli.load-only-format",
                TextComponent.of(world.getName()), TextComponent.of(world.getFormatName())));
        }
    }

    /**
     * Run commands from the given stream until it ends or {@code stop} is entered.
     *
     * @param inputStream the stream to read commands from
     */
    public void run(InputStream inputStream) {
        run(List.of(), inputStream);
    }

    /**
     * Run the given script commands, then (if {@code inputStream} is non-null) commands
     * read from the stream, until the input ends or {@code stop} is entered.
     * Modified worlds are saved after each command and at the end.
     *
     * @param scriptCommands commands to run first
     * @param inputStream the stream to read further commands from, or {@code null} to only run the script
     * @return the number of commands that were not recognised
     */
    int run(List<String> scriptCommands, @Nullable InputStream inputStream) {
        int unknownCommands = 0;
        try {
            for (String command : scriptCommands) {
                CommandResult result = handleLine(command);
                if (result == CommandResult.STOP) {
                    return unknownCommands;
                }
                if (result == CommandResult.UNKNOWN) {
                    unknownCommands++;
                }
            }
            if (inputStream == null) {
                return unknownCommands;
            }
            // Not closed: that would close the given stream, which is usually System.in
            Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8);
            while (true) {
                System.err.print("> ");
                if (!scanner.hasNextLine()) {
                    break;
                }
                CommandResult result = handleLine(scanner.nextLine());
                if (result == CommandResult.STOP) {
                    break;
                }
                if (result == CommandResult.UNKNOWN) {
                    unknownCommands++;
                }
            }
            return unknownCommands;
        } finally {
            saveAllWorlds(false, true);
        }
    }

    private enum CommandResult {
        SKIPPED,
        HANDLED,
        UNKNOWN,
        STOP
    }

    private CommandResult handleLine(String line) {
        String command = CLIFiles.normalizeCommand(line);
        if (command == null || command.startsWith("#")) {
            return CommandResult.SKIPPED;
        }
        if (CLIFiles.isStopCommand(command)) {
            commandSender.printInfo(TranslatableComponent.of("worldedit.cli.stopping"));
            return CommandResult.STOP;
        }
        CommandEvent event = new CommandEvent(commandSender, command);
        WorldEdit.getInstance().getEventBus().post(event);
        if (!event.isCancelled()) {
            commandSender.printError(TranslatableComponent.of("worldedit.cli.unknown-command"));
            return CommandResult.UNKNOWN;
        }
        saveAllWorlds(false);
        return CommandResult.HANDLED;
    }

    public static void main(String[] args) {
        System.exit(launch(args, System.in, System.out, DEFAULT_WORKING_DIR));
    }

    /**
     * Run the CLI.
     *
     * @param args the command line arguments
     * @param stdin the stream to read interactive commands from
     * @param out the stream to print usage information to
     * @param workingDir the WorldEdit working directory
     * @return the process exit code
     */
    static int launch(String[] args, InputStream stdin, PrintStream out, Path workingDir) {
        CLIArguments arguments;
        try {
            arguments = CLIArguments.parse(args);
        } catch (ParseException e) {
            out.println("Error: " + e.getMessage());
            out.print(CLIArguments.usage());
            out.flush();
            return EXIT_USAGE;
        }
        if (arguments.help()) {
            out.print(CLIArguments.usage());
            out.flush();
            return EXIT_OK;
        }

        // Validate the inputs before starting anything up
        Path file;
        ClipboardFormat format;
        List<String> scriptCommands;
        try {
            file = arguments.file();
            if (file == null) {
                file = askForFile();
            }
            format = CLIFiles.detectFormat(file);
            scriptCommands = arguments.script() == null ? List.of() : CLIFiles.readScript(arguments.script());
        } catch (IllegalArgumentException e) {
            LOGGER.error(e.getMessage());
            return EXIT_ERROR;
        } catch (IOException e) {
            LOGGER.error("Failed to read script file '" + arguments.script() + "'", e);
            return EXIT_ERROR;
        }

        CLIWorldEdit app = new CLIWorldEdit(workingDir);
        try {
            app.onInitialized();
            app.loadWorld(file, format);
            int unknownCommands = app.run(scriptCommands, arguments.nonInteractive() ? null : stdin);
            int exitCode = EXIT_OK;
            if (app.getSaveFailures() > 0) {
                LOGGER.error("Changes to '" + file + "' could not be saved; see the errors above.");
                exitCode = EXIT_ERROR;
            }
            if (arguments.nonInteractive() && unknownCommands > 0) {
                LOGGER.error(unknownCommands + " command(s) in the script were not recognised.");
                exitCode = EXIT_ERROR;
            }
            return exitCode;
        } catch (Exception e) {
            LOGGER.error("An error occurred", e);
            return EXIT_ERROR;
        } finally {
            app.onStopped();
        }
    }

    private static Path askForFile() {
        File chosen;
        try {
            chosen = FileDialogUtil.showOpenDialog(CLIFiles.openableExtensions());
        } catch (HeadlessException _) {
            throw new IllegalArgumentException("No file was given and no file chooser can be shown;"
                + " use --file <path> to specify the file to load.");
        }
        if (chosen == null) {
            throw new IllegalArgumentException("A file must be provided! Use --file <path> to specify one.");
        }
        return chosen.toPath();
    }
}
