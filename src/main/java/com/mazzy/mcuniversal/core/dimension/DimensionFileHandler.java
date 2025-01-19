package com.mazzy.mcuniversal.core.dimension;

import com.mazzy.mcuniversal.McUniversal;
import com.mazzy.mcuniversal.config.DimensionConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.*;

public class DimensionFileHandler {

    public static void setupDimensionFiles(MinecraftServer server) {
        // Use the correct static config field name
        String externalPathStr = DimensionConfig.DIMENSION_PATH.get();
        if (externalPathStr == null || externalPathStr.isEmpty()) {
            McUniversal.LOGGER.error("No external dimension path specified. Dimension files cannot be accessed.");
            return;
        }

        Path externalPath = Paths.get(externalPathStr);
        if (!Files.exists(externalPath) || !Files.isDirectory(externalPath)) {
            McUniversal.LOGGER.error("External dimension path does not exist or is not a directory: {}", externalPath);
            return;
        }

        // Retrieve the server’s root world path using LevelResource.ROOT.
        Path worldFolder = server.getWorldPath(LevelResource.ROOT);

        // <world>/dimensions/mcuniversal/extra/region
        Path targetRegionFolder = worldFolder
                .resolve("dimensions")
                .resolve("mcuniversal")
                .resolve("extra")
                .resolve("region");

        try {
            Files.createDirectories(targetRegionFolder);

            // Copy only .mca files
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(externalPath, "*.mca")) {
                for (Path mcaFile : stream) {
                    Path targetFile = targetRegionFolder.resolve(mcaFile.getFileName());
                    try {
                        Files.copy(mcaFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
                        McUniversal.LOGGER.info("Copied {} to {}", mcaFile, targetFile);
                    } catch (IOException e) {
                        McUniversal.LOGGER.error("Failed to copy {} to {}. Dimension can't be accessed.",
                                mcaFile, targetFile, e);
                    }
                }
            }

        } catch (IOException e) {
            McUniversal.LOGGER.error("Error preparing dimension files. Dimension can't be accessed.", e);
        }
    }
}