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

import com.google.common.collect.ImmutableSet;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.AbstractPlatform;
import com.sk89q.worldedit.extension.platform.Capability;
import com.sk89q.worldedit.extension.platform.Preference;
import com.sk89q.worldedit.internal.util.LogManagerCompat;
import com.sk89q.worldedit.util.SideEffect;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.entity.EntityTypes;
import com.sk89q.worldedit.world.registry.Registries;
import org.apache.logging.log4j.Logger;
import org.enginehub.piston.CommandManager;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import javax.annotation.Nullable;

class CLIPlatform extends AbstractPlatform {

    private final CLIWorldEdit app;
    private int dataVersion = -1;

    /**
     * Scheduled tasks are expressed in game ticks, of which there are 20 per second.
     */
    static final long MILLIS_PER_TICK = 50;

    private static final Logger LOGGER = LogManagerCompat.getLogger();

    private final List<World> worlds = new ArrayList<>();
    // Daemon, so that a forgotten task can never keep the JVM alive
    private final Timer timer = new Timer("WorldEdit CLI Scheduler", true);
    private int lastTimerId = 0;
    private boolean shutdown;

    CLIPlatform(CLIWorldEdit app) {
        this.app = app;
    }

    @Override
    public Registries getRegistries() {
        return CLIRegistries.getInstance();
    }

    @Override
    public int getDataVersion() {
        return this.dataVersion;
    }

    public void setDataVersion(int dataVersion) {
        this.dataVersion = dataVersion;
    }

    @Override
    public boolean isValidMobType(String type) {
        return EntityTypes.get(type) != null;
    }

    @Override
    public void reload() {
        getConfiguration().load();
        super.reload();
    }

    @Override
    public synchronized int schedule(long delay, long period, Runnable task) {
        if (shutdown) {
            return -1;
        }
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    task.run();
                } catch (RuntimeException e) {
                    // Don't let one failing task kill the timer thread, and with it every other task
                    LOGGER.error("Error running scheduled task", e);
                }
            }
        };
        long delayMillis = Math.max(0, delay) * MILLIS_PER_TICK;
        if (period > 0) {
            this.timer.scheduleAtFixedRate(timerTask, delayMillis, period * MILLIS_PER_TICK);
        } else {
            this.timer.schedule(timerTask, delayMillis);
        }
        return this.lastTimerId++;
    }

    /**
     * Cancel all scheduled tasks. No further tasks can be scheduled afterwards.
     */
    synchronized void shutdown() {
        shutdown = true;
        this.timer.cancel();
    }

    @Override
    public List<? extends World> getWorlds() {
        return this.worlds;
    }

    @Nullable
    @Override
    public Player matchPlayer(Player player) {
        return null;
    }

    @Nullable
    @Override
    public World matchWorld(World world) {
        return this.worlds.stream()
                .filter(w -> w.id().equals(world.id()))
                .findAny()
                .orElse(null);
    }

    @Override
    public void registerCommands(CommandManager manager) {
    }

    @Override
    public void setGameHooksEnabled(boolean enabled) {
    }

    @Override
    public CLIConfiguration getConfiguration() {
        return app.getConfig();
    }

    @Override
    public String getVersion() {
        if (app.getInternalVersion() == null) {
            return "unknown"; // Run from IDE
        }
        return app.getInternalVersion();
    }

    @Override
    public String getPlatformName() {
        return "CLI-Official";
    }

    @Override
    public String getPlatformVersion() {
        return this.getVersion();
    }

    @Override
    public String id() {
        return "enginehub:cli";
    }

    @Override
    public Map<Capability, Preference> getCapabilities() {
        Map<Capability, Preference> capabilities = new EnumMap<>(Capability.class);
        capabilities.put(Capability.CONFIGURATION, Preference.PREFER_OTHERS);
        capabilities.put(Capability.GAME_HOOKS, Preference.NORMAL);
        capabilities.put(Capability.PERMISSIONS, Preference.NORMAL);
        capabilities.put(Capability.USER_COMMANDS, Preference.NORMAL);
        capabilities.put(Capability.WORLD_EDITING, Preference.PREFERRED);
        return capabilities;
    }

    @Override
    public Set<SideEffect> getSupportedSideEffects() {
        return ImmutableSet.of();
    }

    public void addWorld(World world) {
        worlds.add(world);
    }
}
