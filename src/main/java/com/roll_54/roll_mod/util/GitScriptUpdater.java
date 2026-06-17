package com.roll_54.roll_mod.util;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.config.MyConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Pulls the {@code server_scripts} folder from a public GitHub repository (via the
 * codeload ZIP endpoint) into the running server's {@code kubejs/server_scripts} folder,
 * then schedules a full {@code /reload} after a grace period, warning all online players
 * with a red title shortly beforehand.
 *
 * All network and zip I/O happens off the server thread; the filesystem swap, the title
 * broadcast, and the reload are posted back onto the server thread.
 */
public final class GitScriptUpdater {

    private GitScriptUpdater() {}

    /** Prevents two updates from running at the same time. */
    private static final AtomicBoolean BUSY = new AtomicBoolean(false);

    /** Single shared scheduler for the warning + reload tasks. */
    private static final ScheduledExecutorService SCHEDULER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "roll_mod-git-updater");
                t.setDaemon(true);
                return t;
            });

    public static boolean isBusy() {
        return BUSY.get();
    }

    /**
     * Kicks off an update. Returns {@code false} immediately if an update is already in
     * progress, otherwise {@code true} and the work proceeds asynchronously.
     */
    public static boolean run(MinecraftServer server, ServerPlayer initiator) {
        if (!BUSY.compareAndSet(false, true)) {
            return false;
        }

        MyConfig.GitUpdater cfg = MyConfig.INSTANCE.gitUpdater;
        String url = "https://codeload.github.com/"
                + cfg.repoOwner + "/" + cfg.repoName
                + "/zip/refs/heads/" + cfg.branch;

        CompletableFuture
                .supplyAsync(() -> downloadAndExtract(url, cfg.repoName, cfg.branch, cfg.sourceFolder))
                .whenComplete((extracted, err) -> {
                    if (err != null || extracted == null) {
                        String reason = err != null
                                ? (err.getCause() != null ? err.getCause().getMessage() : err.getMessage())
                                : "could not locate '" + cfg.sourceFolder + "' in the repository";
                        RollMod.LOGGER.error("[git_copy] download/extract failed", err);
                        server.execute(() -> {
                            sendFail(initiator, reason);
                            BUSY.set(false);
                        });
                        return;
                    }

                    // Back on the server thread for the filesystem swap + scheduling.
                    server.execute(() -> {
                        try {
                            Path target = FMLPaths.GAMEDIR.get()
                                    .resolve("kubejs")
                                    .resolve("server_scripts");
                            replaceContents(target, extracted);
                            cleanupTemp(extracted);

                            initiator.sendSystemMessage(
                                    Component.translatable("message.roll_mod.git.copied")
                                            .withStyle(ChatFormatting.GREEN));

                            scheduleReload(server, cfg);
                        } catch (IOException e) {
                            RollMod.LOGGER.error("[git_copy] file swap failed", e);
                            sendFail(initiator, e.getMessage());
                            BUSY.set(false);
                            cleanupTemp(extracted);
                        }
                    });
                });

        return true;
    }

    // ------------------------------------------------------------------
    // Download + extract (background thread)
    // ------------------------------------------------------------------

    /**
     * Downloads the repo ZIP and extracts only the requested sub-folder into a fresh temp
     * directory. Returns the path to the extracted sub-folder, or {@code null} if the
     * sub-folder is not present in the archive.
     */
    private static Path downloadAndExtract(String url, String repoName, String branch, String sourceFolder) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(30))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMinutes(5))
                    .header("Accept", "application/zip")
                    .header("User-Agent", "roll_mod-git-updater")
                    .GET()
                    .build();

            Path tempDir = Files.createTempDirectory("roll_mod_git_");
            // The archive root entry is "<repoName>-<branch>/", so the sub-folder lives at
            // "<repoName>-<branch>/<sourceFolder>/".
            String prefix = repoName + "-" + branch + "/" + sourceFolder + "/";

            HttpResponse<InputStream> response =
                    client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                cleanupTemp(tempDir);
                throw new IOException("HTTP " + response.statusCode() + " from " + url);
            }

            boolean found = false;
            try (ZipInputStream zis = new ZipInputStream(response.body())) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    String name = entry.getName();
                    if (!name.startsWith(prefix)) {
                        continue;
                    }
                    found = true;
                    String relative = name.substring(prefix.length());
                    if (relative.isEmpty()) {
                        continue; // the sub-folder root entry itself
                    }
                    Path out = safeResolve(tempDir, relative);
                    if (entry.isDirectory()) {
                        Files.createDirectories(out);
                    } else {
                        Files.createDirectories(out.getParent());
                        Files.copy(zis, out, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }

            if (!found) {
                cleanupTemp(tempDir);
                return null;
            }
            return tempDir;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /** Guards against zip-slip path traversal. */
    private static Path safeResolve(Path base, String relative) throws IOException {
        Path resolved = base.resolve(relative).normalize();
        if (!resolved.startsWith(base)) {
            throw new IOException("Illegal entry path: " + relative);
        }
        return resolved;
    }

    // ------------------------------------------------------------------
    // Filesystem swap (server thread)
    // ------------------------------------------------------------------

    /** Deletes the contents of {@code target} (keeping the dir) and copies {@code source} into it. */
    private static void replaceContents(Path target, Path source) throws IOException {
        Files.createDirectories(target);

        // Wipe existing contents, but keep the server_scripts directory itself so KubeJS
        // keeps watching the same path.
        try (var children = Files.list(target)) {
            for (Path child : (Iterable<Path>) children::iterator) {
                deleteRecursively(child);
            }
        }

        try (var walk = Files.walk(source)) {
            for (Path p : (Iterable<Path>) walk::iterator) {
                Path rel = source.relativize(p);
                Path dest = target.resolve(rel.toString());
                if (Files.isDirectory(p)) {
                    Files.createDirectories(dest);
                } else {
                    Files.createDirectories(dest.getParent());
                    Files.copy(p, dest, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) return;
        try (var walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private static void cleanupTemp(Path tempDir) {
        try {
            // tempDir is ".../roll_mod_git_XXXX/<contents>"; delete the whole temp root.
            Path root = tempDir;
            while (root.getParent() != null
                    && root.getParent().getFileName() != null
                    && root.getFileName() != null
                    && !root.getFileName().toString().startsWith("roll_mod_git_")) {
                root = root.getParent();
            }
            deleteRecursively(root);
        } catch (IOException | RuntimeException e) {
            RollMod.LOGGER.warn("[git_copy] failed to clean up temp dir", e);
        }
    }

    // ------------------------------------------------------------------
    // Warning + reload scheduling
    // ------------------------------------------------------------------

    private static void scheduleReload(MinecraftServer server, MyConfig.GitUpdater cfg) {
        int reloadDelay = Math.max(1, cfg.reloadDelaySeconds);
        int warnBefore = Math.max(0, Math.min(cfg.warnBeforeReloadSeconds, reloadDelay - 1));
        int warnAt = reloadDelay - warnBefore;

        SCHEDULER.schedule(
                () -> server.execute(() -> broadcastWarning(server)),
                warnAt, TimeUnit.SECONDS);

        SCHEDULER.schedule(
                () -> server.execute(() -> {
                    try {
                        server.getCommands().performPrefixedCommand(
                                server.createCommandSourceStack(), "reload");
                        RollMod.LOGGER.info("[git_copy] /reload executed after script update");
                    } catch (Exception e) {
                        RollMod.LOGGER.error("[git_copy] /reload failed", e);
                    } finally {
                        BUSY.set(false);
                    }
                }),
                reloadDelay, TimeUnit.SECONDS);
    }

    private static void broadcastWarning(MinecraftServer server) {
        Component title = Component.translatable("message.roll_mod.git.reload_warning_title")
                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        Component subtitle = Component.translatable("message.roll_mod.git.reload_warning_subtitle")
                .withStyle(ChatFormatting.RED);

        // fadeIn=10t, stay=200t (10s), fadeOut=20t
        ClientboundSetTitlesAnimationPacket timing =
                new ClientboundSetTitlesAnimationPacket(10, 200, 20);

        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.connection.send(timing);
            p.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
            p.connection.send(new ClientboundSetTitleTextPacket(title));
        }
    }

    private static void sendFail(ServerPlayer initiator, String reason) {
        initiator.sendSystemMessage(
                Component.translatable("message.roll_mod.git.failed",
                                reason == null ? "unknown error" : reason)
                        .withStyle(ChatFormatting.RED));
    }
}
