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

import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.gui.ChatPromptRegistry;
import com.sk89q.worldedit.bukkit.gui.ChatPrompts;
import com.sk89q.worldedit.bukkit.gui.GuiScheduler;
import com.sk89q.worldedit.bukkit.gui.Menu;
import com.sk89q.worldedit.bukkit.gui.MenuListener;
import com.sk89q.worldedit.bukkit.gui.Text;
import com.sk89q.worldedit.event.platform.CommandEvent;
import com.sk89q.worldedit.util.image.ImageFiles;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.PluginManager;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Entry point of the in-game WorldEdit menus ({@code /wegui}).
 */
public final class WorldEditGui implements Listener {

    /**
     * The permission needed to open the menus. Each action still checks the
     * permission of the WorldEdit command it runs.
     */
    public static final String PERMISSION = "worldedit.gui";

    /**
     * The name of the command declared in {@code plugin.yml}.
     */
    public static final String COMMAND = "wegui";

    private static final Duration PROMPT_TIMEOUT = Duration.ofSeconds(60);

    private final WorldEditPlugin plugin;
    private final GuiScheduler scheduler;
    private final ChatPrompts prompts;
    private final Map<UUID, PlayerGuiState> states = new ConcurrentHashMap<>();
    private List<Material> blockMaterials;

    private WorldEditGui(WorldEditPlugin plugin) {
        this.plugin = plugin;
        this.scheduler = new GuiScheduler(plugin, plugin.isFolia());
        this.prompts = new ChatPrompts(new ChatPromptRegistry(), scheduler, PROMPT_TIMEOUT);
    }

    /**
     * Register the GUI listeners and command.
     *
     * @param plugin the plugin
     * @return the GUI
     */
    public static WorldEditGui enable(WorldEditPlugin plugin) {
        WorldEditGui gui = new WorldEditGui(plugin);
        PluginManager pluginManager = plugin.getServer().getPluginManager();
        pluginManager.registerEvents(new MenuListener(gui.scheduler), plugin);
        pluginManager.registerEvents(gui.prompts, plugin);
        pluginManager.registerEvents(gui, plugin);

        PluginCommand command = plugin.getCommand(COMMAND);
        if (command != null) {
            WeGuiCommand executor = new WeGuiCommand(gui);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }
        return gui;
    }

    /**
     * Close every open menu so no menu inventory outlives the plugin, and
     * forget all pending prompts.
     */
    public void disable() {
        prompts.clear();
        states.clear();
        if (plugin.isFolia()) {
            // Inventories may only be touched from their owner's region thread.
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof Menu) {
                player.closeInventory();
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        states.remove(event.getPlayer().getUniqueId());
    }

    /**
     * Get the GUI choices of a player.
     *
     * @param player the player
     * @return the state
     */
    public PlayerGuiState state(Player player) {
        return states.computeIfAbsent(player.getUniqueId(), _ -> new PlayerGuiState());
    }

    public ChatPrompts prompts() {
        return prompts;
    }

    public GuiScheduler scheduler() {
        return scheduler;
    }

    /**
     * Ask the player for a value in chat, validate it and turn it into a
     * command argument. Invalid answers are reported and the menu reopened.
     *
     * @param player the player
     * @param question the question
     * @param kind how to validate the answer
     * @param onArgument called with the validated argument
     * @param reopen reopens the menu on cancel or invalid input
     */
    public void ask(Player player, String question, InputKind kind, Consumer<String> onArgument,
                    Consumer<Player> reopen) {
        prompts.ask(player, question, input -> {
            String argument = kind.toArgument(input);
            if (argument == null) {
                player.sendMessage(Text.PREFIX + Text.RED + kind.errorMessage());
                reopen.accept(player);
                return;
            }
            onArgument.accept(argument);
        }, () -> reopen.accept(player));
    }

    /**
     * Get the player's WorldEdit session, which holds history, clipboard
     * and settings.
     *
     * @param player the player
     * @return the session
     */
    public LocalSession session(Player player) {
        return plugin.getSession(player);
    }

    /**
     * Run a WorldEdit command as the player, through WorldEdit's own command
     * manager so that permissions, limits and history apply as usual.
     *
     * @param player the player
     * @param command the command, starting with a slash, e.g. {@code //set stone}
     */
    public void run(Player player, String command) {
        // Typed commands go through this event first; firing it here lets command blockers
        // (e.g. region flags denying //set) and command loggers see GUI actions too
        PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(player, command);
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }
        String message = event.getMessage();
        if (!message.equals(command)) {
            // Rewritten by another plugin: run it exactly as if it had been typed
            if (message.startsWith("/") && message.length() > 1) {
                plugin.getServer().dispatchCommand(player, message.substring(1));
            }
            return;
        }
        WorldEdit.getInstance().getEventBus().post(new CommandEvent(plugin.wrapPlayer(player), command));
    }

    /**
     * Get WorldEdit's configuration.
     *
     * @return the configuration
     */
    public LocalConfiguration config() {
        return WorldEdit.getInstance().getConfiguration();
    }

    /**
     * Get the configured schematics folder.
     *
     * @return the folder
     */
    public Path schematicsFolder() {
        return WorldEdit.getInstance().getWorkingDirectoryPath(config().saveDir);
    }

    /**
     * Get WorldEdit's image folder, used by {@code //image} and {@code //heightmap}.
     *
     * @return the folder
     */
    public Path imagesFolder() {
        return ImageFiles.getDirectory(WorldEdit.getInstance());
    }

    /**
     * Get the materials offered by the block picker: placeable blocks that
     * have an item form, sorted by name.
     *
     * @return the materials
     */
    public List<Material> blockMaterials() {
        if (blockMaterials == null) {
            blockMaterials = Arrays.stream(Material.values())
                .filter(m -> !m.name().startsWith("LEGACY_"))
                .filter(m -> m.isBlock() && m.isItem() && !m.isAir())
                .sorted(Comparator.comparing(Material::name))
                .toList();
        }
        return blockMaterials;
    }

    /**
     * Open the main menu.
     *
     * @param player the player
     */
    public void openMain(Player player) {
        new MainMenu(this).open(player);
    }
}
