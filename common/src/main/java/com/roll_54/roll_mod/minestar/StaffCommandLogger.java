package com.roll_54.roll_mod.minestar;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.hub.RankResolver;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.CommandEvent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Logs every command sent by a staff player — {@code helper} rank or higher, or an operator — to
 * minecraft_root/minestar/logs/op_users_check.txt.
 *
 * <p>Runs last and still sees cancelled commands, so an attempt another mod blocked is on record too.
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class StaffCommandLogger {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final String LOG_DIR = "minestar";
    private static final String LOG_SUBDIR = "logs";
    private static final String LOG_FILE_NAME = "op_users_check.txt";

    /** Vanilla's gamemaster level: the lowest at which {@code /op} grants anything worth watching. */
    private static final int OP_LEVEL = 2;

    private StaffCommandLogger() {}

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onCommand(CommandEvent event) {
        CommandSourceStack source = event.getParseResults().getContext().getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return;
        }

        boolean op = player.hasPermissions(OP_LEVEL);
        if (!op && !RankResolver.isHelperOrHigher(player)) {
            return;
        }

        try {
            writeToLog(String.format(
                    "[%s] %s (%s) | Rank: %s%s | Dimension: %s | Position:(X Y Z) %.1f %.1f %.1f | Command: /%s%s%n",
                    LocalDateTime.now().format(TIMESTAMP_FORMATTER),
                    player.getGameProfile().getName(),
                    player.getUUID(),
                    RankResolver.rankId(player),
                    op ? " [op]" : "",
                    player.level().dimension().location(),
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    event.getParseResults().getReader().getString(),
                    event.isCanceled() ? " (cancelled)" : ""
            ));
        } catch (IOException e) {
            RollMod.LOGGER.error("[StaffCommandLogger] Failed to log command", e);
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
