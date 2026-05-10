package com.roll_54.roll_mod.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CommandBlockLogger {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final String LOG_DIR = "minestar";
    private static final String LOG_SUBDIR = "logs";
    private static final String LOG_FILE_NAME = "commandBlockUsed.txt";

    public static void logBlockCommand(Level level, BlockPos pos, String command) throws IOException {
        String logEntry = formatBlockLogEntry(level, pos, command);
        writeToLog(logEntry);
    }
    public static void logMinecartCommand(Level level, double x, double y, double z, String command) throws IOException {
        String logEntry = formatMinecartLogEntry(level, x, y, z, command);
        writeToLog(logEntry);
    }

    private static String formatBlockLogEntry(Level level, BlockPos pos, String command) {
        String timestamp = getCurrentTimestamp();

        return String.format(
                "[%s] Block | Dimension: %s | BlockPos: X=%d Y=%d Z=%d | Command: %s%n",
                timestamp,
                level.dimension().location(),
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                command
        );
    }


    private static String formatMinecartLogEntry(Level level, double x, double y, double z, String command) {
        String timestamp = getCurrentTimestamp();

        return String.format(
                "[%s] Minecart | Dimension: %s | Position:(X Y Z) %.1f %.1f %.1f | Command: %s%n",
                timestamp,
                level.dimension().location(),
                x,
                y,
                z,
                command
        );
    }


    private static String getCurrentTimestamp() {
        LocalDateTime now = LocalDateTime.now();
        return now.format(TIMESTAMP_FORMATTER);
    }

    private static Path getLogFile() throws IOException {
        Path logDir = FMLPaths.GAMEDIR.get()
                .resolve(LOG_DIR)
                .resolve(LOG_SUBDIR);

        Files.createDirectories(logDir);
        return logDir.resolve(LOG_FILE_NAME);
    }

    private static void writeToLog(String logEntry) throws IOException {
        Path logFile = getLogFile();

        Files.writeString(
                logFile,
                logEntry,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        );
    }
}
