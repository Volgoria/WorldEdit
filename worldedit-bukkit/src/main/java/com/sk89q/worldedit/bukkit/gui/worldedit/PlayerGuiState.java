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

import javax.annotation.Nullable;

/**
 * What a player has picked in the GUI. Kept in memory until they log out.
 */
public final class PlayerGuiState {

    private final PatternSelection pattern = new PatternSelection();
    @Nullable
    private String mask;
    private int brushSize = 3;
    private boolean brushHollow;
    private int generationSize = 5;
    private boolean generationHollow;
    private String blockSearch = "";
    private String schematicSearch = "";

    public PatternSelection getPattern() {
        return pattern;
    }

    @Nullable
    public String getMask() {
        return mask;
    }

    public void setMask(@Nullable String mask) {
        this.mask = mask == null || mask.isBlank() ? null : mask;
    }

    public int getBrushSize() {
        return brushSize;
    }

    public void setBrushSize(int brushSize) {
        this.brushSize = brushSize;
    }

    public boolean isBrushHollow() {
        return brushHollow;
    }

    public void setBrushHollow(boolean brushHollow) {
        this.brushHollow = brushHollow;
    }

    public int getGenerationSize() {
        return generationSize;
    }

    public void setGenerationSize(int generationSize) {
        this.generationSize = generationSize;
    }

    public boolean isGenerationHollow() {
        return generationHollow;
    }

    public void setGenerationHollow(boolean generationHollow) {
        this.generationHollow = generationHollow;
    }

    public String getBlockSearch() {
        return blockSearch;
    }

    public void setBlockSearch(String blockSearch) {
        this.blockSearch = blockSearch == null ? "" : blockSearch.trim();
    }

    public String getSchematicSearch() {
        return schematicSearch;
    }

    public void setSchematicSearch(String schematicSearch) {
        this.schematicSearch = schematicSearch == null ? "" : schematicSearch.trim();
    }
}
