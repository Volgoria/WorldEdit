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

package com.sk89q.worldedit.util.test;

import com.sk89q.worldedit.world.block.BlockType;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

import java.util.List;

/**
 * Registers the air block types before any test class runs.
 *
 * <p>{@code BlockTypes} reads its constants from the block registry once, when
 * the class is first loaded, and keeps them for the life of the JVM. Tests do
 * not have a real registry; they register the types they need themselves.
 * Whichever test happens to load {@code BlockTypes} first (often indirectly,
 * e.g. through {@code ItemType.hasBlockType()} or a clipboard) used to decide
 * whether {@code BlockTypes.AIR} was set, so running a subset of the tests
 * could fail with a null {@code BlockTypes.AIR}. Registering the air types
 * when the test session opens, before any class can load {@code BlockTypes},
 * makes the outcome independent of the test order.</p>
 *
 * <p>Only the air types are registered: every test registry agrees that they
 * are air and have no properties, so sharing them cannot leak state between
 * tests.</p>
 */
public final class CommonBlockTypesSessionListener implements LauncherSessionListener {

    /**
     * The block types registered for every test.
     */
    public static final List<String> AIR_TYPES = List.of("minecraft:air", "minecraft:cave_air", "minecraft:void_air");

    @Override
    public void launcherSessionOpened(LauncherSession session) {
        registerAirTypes();
    }

    /**
     * Register the air types, unless they already are.
     */
    public static synchronized void registerAirTypes() {
        for (String id : AIR_TYPES) {
            // Not get(id): it refuses to work before a platform is registered
            if (!BlockType.REGISTRY.keySet().contains(id)) {
                BlockType.REGISTRY.register(id, new BlockType(id));
            }
        }
    }
}
