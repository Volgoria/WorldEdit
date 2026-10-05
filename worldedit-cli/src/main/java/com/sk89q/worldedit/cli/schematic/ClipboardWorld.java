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

package com.sk89q.worldedit.cli.schematic;

import com.google.common.collect.ImmutableSet;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.cli.CLIWorld;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.SideEffect;
import com.sk89q.worldedit.util.SideEffectSet;
import com.sk89q.worldedit.world.AbstractWorld;
import com.sk89q.worldedit.world.RegenOptions;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.generation.TreeType;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.annotation.Nullable;

public class ClipboardWorld extends AbstractWorld implements Clipboard, CLIWorld {

    private final File file;
    private final ClipboardFormat format;
    private final Clipboard clipboard;
    private final String name;

    private boolean dirty = false;

    public ClipboardWorld(File file, ClipboardFormat format, Clipboard clipboard, String name) {
        this.file = file;
        this.format = format;
        this.clipboard = clipboard;
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String id() {
        return name.replace(" ", "_").toLowerCase(Locale.ROOT);
    }

    @Override
    public <B extends BlockStateHolder<B>> boolean setBlock(BlockVector3 position, B block, SideEffectSet sideEffects) throws WorldEditException {
        boolean retValue = clipboard.setBlock(position, block);
        if (retValue) {
            dirty = true;
        }
        return retValue;
    }

    @Override
    public Set<SideEffect> applySideEffects(BlockVector3 position, BlockState previousType, SideEffectSet sideEffectSet) throws WorldEditException {
        return ImmutableSet.of();
    }

    @Override
    public int getBlockLightLevel(BlockVector3 position) {
        return 0;
    }

    @Override
    public boolean clearContainerBlockContents(BlockVector3 position) {
        return false;
    }

    @Override
    public void dropItem(Vector3 position, BaseItemStack item) {
    }

    @Override
    public void simulateBlockMine(BlockVector3 position) {
    }

    @Override
    public boolean regenerate(Region region, Extent extent, RegenOptions options) {
        return false;
    }

    @Override
    public boolean generateTree(TreeType type, EditSession editSession, BlockVector3 position) throws MaxChangedBlocksException {
        return false;
    }

    @Override
    public BlockVector3 getSpawnPosition() {
        return clipboard.getOrigin();
    }

    @Override
    public List<? extends Entity> getEntities(Region region) {
        return clipboard.getEntities(region);
    }

    @Override
    public List<? extends Entity> getEntities() {
        return clipboard.getEntities();
    }

    @Nullable
    @Override
    public Entity createEntity(Location location, BaseEntity entity) {
        Entity createdEntity = clipboard.createEntity(location, entity);
        if (createdEntity != null) {
            dirty = true;
        }
        return createdEntity;
    }

    @Override
    public BlockState getBlock(BlockVector3 position) {
        return clipboard.getBlock(position);
    }

    @Override
    public BaseBlock getFullBlock(BlockVector3 position) {
        return clipboard.getFullBlock(position);
    }

    @Override
    public BiomeType getBiome(BlockVector3 position) {
        return clipboard.getBiome(position);
    }

    @Override
    public boolean setBiome(BlockVector3 position, BiomeType biome) {
        boolean retValue = clipboard.setBiome(position, biome);
        if (retValue) {
            dirty = true;
        }
        return retValue;
    }

    @Override
    public Region getRegion() {
        return clipboard.getRegion();
    }

    @Override
    public BlockVector3 getDimensions() {
        return clipboard.getDimensions();
    }

    @Override
    public BlockVector3 getOrigin() {
        return clipboard.getOrigin();
    }

    @Override
    public void setOrigin(BlockVector3 origin) {
        clipboard.setOrigin(origin);
        dirty = true;
    }

    @Override
    public boolean hasBiomes() {
        return clipboard.hasBiomes();
    }

    @Override
    public BlockVector3 getMaximumPoint() {
        return clipboard.getMaximumPoint();
    }

    @Override
    public BlockVector3 getMinimumPoint() {
        return clipboard.getMinimumPoint();
    }

    /**
     * Get whether changes can be written back to the file. Legacy formats
     * such as MCEdit schematics and Sponge v1 schematics can only be read.
     *
     * @return true if the file's format supports writing
     */
    public boolean canSave() {
        return format.supportsWriting();
    }

    /**
     * Get a short, user-facing name of the file's format, e.g. {@code mcedit}.
     *
     * @return the name
     */
    public String getFormatName() {
        return format.getAliases().stream().findFirst().orElse(format.getName());
    }

    @Override
    public void save(boolean force) throws IOException {
        if (dirty || force) {
            if (!canSave()) {
                throw new IOException("the " + getFormatName() + " format can only be read, not written");
            }
            writeAtomically();
            dirty = false;
        }
    }

    /**
     * Write to a temporary file next to the target, then move it into place,
     * so that a failed save never leaves a truncated or corrupt schematic behind.
     */
    private void writeAtomically() throws IOException {
        Path target = file.toPath().toAbsolutePath();
        Path temp = Files.createTempFile(target.getParent(), target.getFileName().toString(), ".tmp");
        try {
            try (OutputStream out = Files.newOutputStream(temp);
                 ClipboardWriter writer = format.getWriter(out)) {
                writer.write(this);
            }
            copyPermissions(target, temp);
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException _) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    /**
     * Temporary files are created owner-only; keep the original file's permissions instead.
     */
    private static void copyPermissions(Path from, Path to) {
        if (!Files.exists(from)) {
            return;
        }
        try {
            Files.setPosixFilePermissions(to, Files.getPosixFilePermissions(from));
        } catch (UnsupportedOperationException | IOException _) {
            // Not a POSIX file system, or not permitted; the defaults will do
        }
    }

    @Override
    public boolean isDirty() {
        return this.dirty;
    }

    @Override
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }
}
