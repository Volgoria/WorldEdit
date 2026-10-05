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

package com.sk89q.worldedit.cli.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.cli.CLIWorldEdit;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class FileRegistries {

    private static final int CLI_DATA_VERSION = 1;
    private static final String DATA_FILE_DOWNLOAD_URL = "https://services.enginehub.org/cassette-deck/we-cli-data/";
    private static final Gson GSON = new GsonBuilder().create();

    /**
     * Opens a stream to download a data file from.
     */
    @FunctionalInterface
    interface Downloader {
        InputStream open(URL url) throws IOException;
    }

    private final CLIWorldEdit app;

    private DataFile dataFile;

    public FileRegistries(CLIWorldEdit app) {
        this.app = app;
    }

    public void loadDataFiles() {
        Path outputFolder = WorldEdit.getInstance().getWorkingDirectoryPath("cli-data");
        this.dataFile = loadDataFile(outputFolder, app.getPlatform().getDataVersion(), URL::openStream);
    }

    /**
     * Load the data file for the given data version from the cache folder,
     * downloading it first if it isn't cached yet.
     *
     * @param cacheFolder the folder to cache data files in
     * @param dataVersion the Minecraft data version
     * @param downloader opens the download stream
     * @return the data file
     */
    static DataFile loadDataFile(Path cacheFolder, int dataVersion, Downloader downloader) {
        Path checkPath = cacheFolder.resolve(dataVersion + "_" + CLI_DATA_VERSION + ".json");

        if (!Files.exists(checkPath)) {
            URL url;
            try {
                url = URI.create(DATA_FILE_DOWNLOAD_URL + dataVersion + "/" + CLI_DATA_VERSION).toURL();
            } catch (IOException e) {
                throw new IllegalStateException("Invalid data file URL", e);
            }
            try {
                Files.createDirectories(cacheFolder);
                // Download to a temporary file first, so an interrupted download can't leave a partial file behind
                Path temp = Files.createTempFile(cacheFolder, checkPath.getFileName().toString(), ".part");
                try {
                    try (InputStream stream = downloader.open(url)) {
                        Files.copy(stream, temp, StandardCopyOption.REPLACE_EXISTING);
                    }
                    Files.move(temp, checkPath, StandardCopyOption.REPLACE_EXISTING);
                } finally {
                    Files.deleteIfExists(temp);
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to download block/item data for Minecraft data version "
                    + dataVersion + " from " + url + ". Check your internet connection;"
                    + " if this persists, this data version may not be supported by WorldEdit-CLI yet.", e);
            }
        }

        DataFile dataFile;
        try {
            dataFile = GSON.fromJson(Files.readString(checkPath), DataFile.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read data file " + checkPath, e);
        } catch (JsonParseException _) {
            dataFile = null;
        }
        if (dataFile == null || dataFile.blocks().isEmpty()) {
            try {
                Files.deleteIfExists(checkPath);
            } catch (IOException _) {
                // We tried; the error below tells the user what to do
            }
            throw new RuntimeException("The data file " + checkPath + " for Minecraft data version " + dataVersion
                + " is not compatible with this version of WorldEdit-CLI and has been removed."
                + " Run again to re-download it; if this persists, please update or report this.");
        }
        return dataFile;
    }

    public DataFile getDataFile() {
        return this.dataFile;
    }

}
