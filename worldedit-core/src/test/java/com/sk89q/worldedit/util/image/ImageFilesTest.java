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

package com.sk89q.worldedit.util.image;

import com.sk89q.worldedit.BaseWorldEditTest;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.util.io.file.FilenameException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@DisplayName("Image file resolution")
class ImageFilesTest extends BaseWorldEditTest {

    private final Actor actor = mock(Actor.class);

    @TempDir
    Path tempDir;

    private File imagesDir() throws IOException {
        Path dir = tempDir.toRealPath().resolve("images");
        Files.createDirectories(dir);
        return dir.toFile();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "../escape.png",
        "../../escape.png",
        "sub/../../escape.png",
        "bad|name.png",
        "",
    })
    @DisplayName("rejects names that are invalid or leave the images folder")
    void rejectsUnsafeNames(String name) throws IOException {
        File dir = imagesDir();
        WorldEdit worldEdit = WorldEdit.getInstance();
        assertThrows(FilenameException.class, () -> ImageFiles.resolveSave(worldEdit, actor, dir, name));
        assertThrows(FilenameException.class, () -> ImageFiles.resolveOpen(worldEdit, actor, dir, name));
    }

    @Test
    @DisplayName("resolves names inside the images folder, adding a .png extension when saving")
    void resolvesSafeNames() throws Exception {
        File dir = imagesDir();
        WorldEdit worldEdit = WorldEdit.getInstance();

        File saved = ImageFiles.resolveSave(worldEdit, actor, dir, "art");
        assertEquals(new File(dir, "art.png"), saved);

        File nested = ImageFiles.resolveSave(worldEdit, actor, dir, "maps/spawn.png");
        assertTrue(nested.toPath().startsWith(dir.toPath()));

        // Absolute-looking names are still resolved inside the folder
        File absolute = ImageFiles.resolveSave(worldEdit, actor, dir, "/etc/passwd.png");
        assertTrue(absolute.toPath().startsWith(dir.toPath()), absolute.toString());

        // An existing JPEG is found when opening by its full name
        File jpeg = new File(dir, "photo.jpg");
        Images.writePng(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), jpeg);
        assertEquals(jpeg, ImageFiles.resolveOpen(worldEdit, actor, dir, "photo.jpg"));
        // ... and a PNG without its extension
        Images.writePng(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), saved);
        assertEquals(saved, ImageFiles.resolveOpen(worldEdit, actor, dir, "art"));
    }

    @Test
    @DisplayName("refuses to decode images that are too large")
    void refusesHugeImages() throws Exception {
        File file = new File(imagesDir(), "big.png");
        Images.writePng(new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB), file);
        assertThrows(IOException.class, () -> Images.read(file, 64 * 63));
        assertEquals(64, Images.read(file, 64 * 64).getWidth());
    }
}
