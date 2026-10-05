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

import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.registry.BlockMaterial;
import com.sk89q.worldedit.world.registry.BlockRegistry;
import com.sk89q.worldedit.world.registry.BundledRegistries;
import com.sk89q.worldedit.world.registry.Registries;

import java.util.Collections;
import java.util.Map;
import java.util.OptionalInt;

/**
 * Registries whose block materials are derived from the block ID, since the
 * bundled block data is unavailable in tests: IDs ending in {@code :air} are
 * air, IDs ending in {@code :water} or {@code :lava} are liquids, and
 * everything else is a solid full block.
 */
public final class SimpleMaterialRegistries extends BundledRegistries {

    private static final BlockMaterial AIR = new Material(true, false, false);
    private static final BlockMaterial LIQUID = new Material(false, true, false);
    private static final BlockMaterial SOLID = new Material(false, false, true);

    private record Material(boolean isAir, boolean isLiquid, boolean isSolid) implements BlockMaterial {
        @Override
        public boolean isFullCube() {
            return isSolid;
        }

        @Override
        public boolean isOpaque() {
            return isSolid;
        }

        @Override
        public boolean isPowerSource() {
            return false;
        }

        @Override
        public float getHardness() {
            return isSolid ? 1 : 0;
        }

        @Override
        public float getResistance() {
            return isSolid ? 1 : 0;
        }

        @Override
        public float getSlipperiness() {
            return 0.6f;
        }

        @Override
        public int getLightValue() {
            return 0;
        }

        @Override
        public boolean isFragileWhenPushed() {
            return false;
        }

        @Override
        public boolean isUnpushable() {
            return false;
        }

        @Override
        public boolean isTicksRandomly() {
            return false;
        }

        @Override
        @Deprecated
        public boolean isMovementBlocker() {
            return isSolid;
        }

        @Override
        public boolean isBurnable() {
            return false;
        }

        @Override
        public boolean isToolRequired() {
            return false;
        }

        @Override
        public boolean isReplacedDuringPlacement() {
            return isAir || isLiquid;
        }

        @Override
        public boolean isTranslucent() {
            return !isSolid;
        }

        @Override
        public boolean hasContainer() {
            return false;
        }
    }

    private final BlockRegistry blockRegistry = new BlockRegistry() {
        @Override
        public Component getRichName(BlockType blockType) {
            return TextComponent.of(blockType.id());
        }

        @Override
        public Map<String, ? extends Property<?>> getProperties(BlockType blockType) {
            return Collections.emptyMap();
        }

        @Override
        public OptionalInt getInternalBlockStateId(BlockState state) {
            return OptionalInt.empty();
        }

        @Override
        public BlockMaterial getMaterial(BlockType blockType) {
            String id = blockType.id();
            if (id.endsWith(":air")) {
                return AIR;
            }
            if (id.endsWith(":water") || id.endsWith(":lava")) {
                return LIQUID;
            }
            return SOLID;
        }
    };

    public static Registries create() {
        return new SimpleMaterialRegistries();
    }

    private SimpleMaterialRegistries() {
    }

    @Override
    public BlockRegistry getBlockRegistry() {
        return blockRegistry;
    }
}
