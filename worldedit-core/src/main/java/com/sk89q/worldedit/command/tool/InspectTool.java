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

package com.sk89q.worldedit.command.tool;

import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.util.Direction;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;
import com.sk89q.worldedit.util.formatting.text.event.ClickEvent;
import com.sk89q.worldedit.util.formatting.text.event.HoverEvent;
import com.sk89q.worldedit.util.formatting.text.format.TextColor;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BaseBlock;
import org.enginehub.linbus.format.snbt.LinStringIO;
import org.enginehub.linbus.tree.LinCompoundTag;
import org.enginehub.linbus.tree.LinListTag;
import org.enginehub.linbus.tree.LinTag;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * Shows detailed information about a block: its state properties, a summary
 * of its NBT data, its biome and its light levels.
 */
public class InspectTool implements BlockTool {

    /**
     * The maximum number of NBT keys listed in the summary.
     */
    public static final int MAX_LISTED_KEYS = 12;

    @Override
    public boolean canUse(Actor player) {
        return player.hasPermission("worldedit.tool.inspect");
    }

    @Override
    public boolean actPrimary(Platform server, LocalConfiguration config, Player player, LocalSession session,
                              Location clicked, @Nullable Direction face) {
        World world = BlockTool.requireWorld(clicked);
        BlockVector3 position = clicked.toVector().toBlockPoint();
        BaseBlock block = world.getFullBlock(position);

        player.print(TranslatableComponent.of("worldedit.tool.inspect.header",
            block.getBlockType().getRichName().color(TextColor.YELLOW),
            TextComponent.of(block.getBlockType().id(), TextColor.GRAY),
            TextComponent.of(position.toString(), TextColor.BLUE)
        ));

        String properties = describeProperties(block.getStates());
        if (properties.isEmpty()) {
            player.printInfo(TranslatableComponent.of("worldedit.tool.inspect.no-properties"));
        } else {
            player.printInfo(TranslatableComponent.of("worldedit.tool.inspect.properties", TextComponent.of(properties, TextColor.WHITE)));
        }

        LinCompoundTag nbt = block.getNbt();
        if (nbt == null) {
            player.printInfo(TranslatableComponent.of("worldedit.tool.inspect.no-nbt"));
        } else {
            String snbt = LinStringIO.writeToString(nbt);
            player.printInfo(TranslatableComponent.of("worldedit.tool.inspect.nbt",
                TextComponent.of(nbt.value().size()),
                TextComponent.of(summarizeNbt(nbt, MAX_LISTED_KEYS), TextColor.WHITE)
                    .hoverEvent(HoverEvent.of(HoverEvent.Action.SHOW_TEXT, TranslatableComponent.of("worldedit.tool.inspect.nbt.hover")))
                    .clickEvent(ClickEvent.of(ClickEvent.Action.COPY_TO_CLIPBOARD, snbt))
            ));
        }

        player.printInfo(TranslatableComponent.of("worldedit.tool.inspect.environment",
            world.getBiome(position).getRichName(),
            TextComponent.of(world.getBlockLightLevel(position)),
            TextComponent.of(world.getBlockLightLevel(position.add(0, 1, 0)))
        ));
        return true;
    }

    /**
     * Describe block state properties as {@code name=value} pairs, sorted by name.
     *
     * @param states the properties and their values
     * @return the description, empty if there are no properties
     */
    public static String describeProperties(Map<Property<?>, Object> states) {
        return states.entrySet().stream()
            .sorted(Comparator.comparing(entry -> entry.getKey().name()))
            .map(entry -> entry.getKey().name() + "=" + entry.getValue())
            .collect(Collectors.joining(", "));
    }

    /**
     * Summarize the top-level entries of a compound tag, as {@code key (type)}
     * pairs sorted by key, with the size of lists and compounds.
     *
     * @param tag the tag
     * @param maxKeys the maximum number of keys to list
     * @return the summary
     */
    public static String summarizeNbt(LinCompoundTag tag, int maxKeys) {
        List<String> entries = new ArrayList<>();
        tag.value().entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .limit(maxKeys)
            .forEach(entry -> entries.add(entry.getKey() + " (" + describeTag(entry.getValue()) + ")"));
        int remaining = tag.value().size() - entries.size();
        if (remaining > 0) {
            entries.add("+" + remaining + " more");
        }
        return String.join(", ", entries);
    }

    private static String describeTag(LinTag<?> tag) {
        String type = tag.type().id().name().toLowerCase(Locale.ROOT);
        if (tag instanceof LinListTag<?> list) {
            return type + "[" + list.value().size() + "]";
        } else if (tag instanceof LinCompoundTag compound) {
            return type + "{" + compound.value().size() + "}";
        }
        return type;
    }
}
