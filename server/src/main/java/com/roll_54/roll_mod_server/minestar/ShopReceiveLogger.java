package com.roll_54.roll_mod_server.minestar;

import com.roll_54.roll_mod.RollMod;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLPaths;
import ua.com.minestar.model.ShopProduct;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Logs every shop product a player received through {@code /shop-receive} to
 * minecraft_root/minestar/logs/shop_recieved.txt.
 */
public final class ShopReceiveLogger {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final String LOG_DIR = "minestar";
    private static final String LOG_SUBDIR = "logs";
    private static final String LOG_FILE_NAME = "shop_recieved.txt";

    private ShopReceiveLogger() {}

    public static void logReceived(ServerPlayer player, ShopProduct product) {
        try {
            writeToLog(String.format(
                    "[%s] %s (%s) | Product ID: %d | Name: %s | Quantity: %d | Commands: %s%n",
                    LocalDateTime.now().format(TIMESTAMP_FORMATTER),
                    player.getGameProfile().getName(),
                    player.getUUID(),
                    product.id(),
                    product.name(),
                    product.quantity(),
                    String.join(" ; ", product.commands())
            ));
        } catch (IOException e) {
            RollMod.LOGGER.error("[ShopReceiveLogger] Failed to log received product {}", product.id(), e);
        }
    }

    private static void writeToLog(String logEntry) throws IOException {
        Path logDir = FMLPaths.GAMEDIR.get().resolve(LOG_DIR).resolve(LOG_SUBDIR);
        Files.createDirectories(logDir);

        Files.writeString(
                logDir.resolve(LOG_FILE_NAME),
                logEntry,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        );
    }
}
