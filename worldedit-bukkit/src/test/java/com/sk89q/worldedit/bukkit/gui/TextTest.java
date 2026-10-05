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

package com.sk89q.worldedit.bukkit.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextTest {

    @Test
    void humanize() {
        assertEquals("Oak Planks", Text.humanize("oak_planks"));
        assertEquals("Stone", Text.humanize("minecraft:STONE"));
        assertEquals("", Text.humanize(""));
    }

    @Test
    void formatSize() {
        assertEquals("512 B", Text.formatSize(512));
        assertEquals("1.5 KB", Text.formatSize(1536));
        assertEquals("2.0 MB", Text.formatSize(2L * 1024 * 1024));
    }
}
