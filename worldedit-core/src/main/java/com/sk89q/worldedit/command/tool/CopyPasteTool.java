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

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.EmptyClipboardException;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.TranslatableComponent;

/**
 * Copies the selection and pastes the clipboard at a distance.
 *
 * <p>Left-click copies the current selection to the clipboard, using the
 * targeted block as the clipboard origin. Right-click pastes the clipboard
 * so that its origin lands on the targeted block. Pastes can be undone like
 * any other edit.</p>
 */
public class CopyPasteTool extends BrushTool implements DoubleActionTraceTool {

    private final boolean ignoreAir;

    /**
     * Create a new copy-paste tool.
     *
     * @param ignoreAir true to not paste the air blocks of the clipboard
     */
    public CopyPasteTool(boolean ignoreAir) {
        super("worldedit.tool.copypaste");
        this.ignoreAir = ignoreAir;
    }

    @Override
    public boolean canUse(Actor player) {
        return player.hasPermission("worldedit.tool.copypaste");
    }

    @Override
    public boolean actSecondary(Platform server, LocalConfiguration config, Player player, LocalSession session) {
        Location target = getTarget(player);
        if (target == null) {
            return true;
        }
        BlockVector3 origin = target.toVector().toBlockPoint();
        try (EditSession editSession = session.createEditSession(player)) {
            Region region = session.getSelection(player.getWorld());
            int limit = session.getBlockChangeLimit();
            if (limit >= 0 && region.getBoundingBox().getVolume() >= limit) {
                throw new MaxChangedBlocksException(limit);
            }
            BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
            clipboard.setOrigin(origin);
            ForwardExtentCopy copy = new ForwardExtentCopy(editSession, region, clipboard, region.getMinimumPoint());
            Operations.complete(copy);
            session.setClipboard(new ClipboardHolder(clipboard));
            player.printInfo(TranslatableComponent.of("worldedit.tool.copypaste.copied",
                TextComponent.of(region.getVolume()), TextComponent.of(origin.toString())));
        } catch (WorldEditException e) {
            printError(player, e);
        }
        return true;
    }

    @Override
    public boolean actPrimary(Platform server, LocalConfiguration config, Player player, LocalSession session) {
        Location target = getTarget(player);
        if (target == null) {
            return true;
        }
        BlockVector3 to = target.toVector().toBlockPoint();
        BlockBag bag = session.getBlockBag(player);
        try (EditSession editSession = session.createEditSession(player)) {
            try {
                ClipboardHolder holder = session.getClipboard();
                Operations.complete(holder.createPaste(editSession)
                    .to(to)
                    .ignoreAirBlocks(ignoreAir)
                    .build());
                player.printInfo(TranslatableComponent.of("worldedit.tool.copypaste.pasted", TextComponent.of(to.toString())));
            } catch (WorldEditException e) {
                printError(player, e);
            } finally {
                session.remember(editSession);
            }
        } finally {
            if (bag != null) {
                bag.flushChanges();
            }
        }
        return true;
    }

    private static void printError(Player player, WorldEditException e) {
        if (e instanceof IncompleteRegionException) {
            player.printError(TranslatableComponent.of("worldedit.error.incomplete-region"));
        } else if (e instanceof EmptyClipboardException) {
            player.printError(TranslatableComponent.of("worldedit.error.empty-clipboard"));
        } else if (e instanceof MaxChangedBlocksException) {
            player.printError(TranslatableComponent.of("worldedit.tool.max-block-changes"));
        } else if (e.getRichMessage() != null) {
            player.printError(e.getRichMessage());
        } else {
            player.printError(TextComponent.of(String.valueOf(e.getMessage())));
        }
    }
}
