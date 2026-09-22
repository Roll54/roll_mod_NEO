package com.roll_54.roll_mod.minestar.kits;

import com.google.gson.JsonObject;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.data.MinestarFiles;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The kits on disk: {@code minestar/kits/<name>.json}, one file each.
 *
 * <p>A file per kit rather than one big file, so an operator can hand a colleague a kit by sending
 * them a file, and a mistake in one kit cannot cost them the others.
 *
 * <p>Loaded on first use and on {@code /rollmod reload}; nothing watches the folder, because a kit
 * changes when somebody runs a command, not on its own.
 */
public final class KitStore {

    private static final String FOLDER = "kits";

    private static final Map<String, Kit> KITS = new LinkedHashMap<>();
    private static volatile boolean loaded;

    private KitStore() {}

    /** Every kit, in file-name order. */
    public static synchronized List<Kit> all(MinecraftServer server) {
        load(server.registryAccess());
        return List.copyOf(KITS.values());
    }

    public static synchronized @Nullable Kit byName(MinecraftServer server, String name) {
        load(server.registryAccess());
        return KITS.get(Kit.clampName(name));
    }

    /** Writes the kit out and takes it into the cache. */
    public static synchronized void put(MinecraftServer server, Kit kit) {
        load(server.registryAccess());
        KITS.put(kit.name(), kit);
        MinestarFiles.write(path(kit.name()), kit.save(server.registryAccess()));
    }

    /** Deletes the kit and its file. {@code false} when there was no such kit. */
    public static synchronized boolean remove(MinecraftServer server, String name) {
        load(server.registryAccess());
        String id = Kit.clampName(name);
        if (KITS.remove(id) == null) return false;
        try {
            Files.deleteIfExists(path(id));
        } catch (IOException e) {
            RollMod.LOGGER.error("[Kits] could not delete {}", path(id), e);
        }
        return true;
    }

    public static synchronized void reload() {
        KITS.clear();
        loaded = false;
    }

    private static void load(RegistryAccess registries) {
        if (loaded) return;
        KITS.clear();

        Path dir = MinestarFiles.folder(FOLDER);
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.json")) {
            stream.forEach(files::add);
        } catch (IOException e) {
            RollMod.LOGGER.error("[Kits] could not list {}", dir, e);
        }
        files.sort(Path::compareTo);

        for (Path file : files) {
            String stem = file.getFileName().toString();
            stem = stem.substring(0, stem.length() - ".json".length()).toLowerCase(Locale.ROOT);
            if (stem.equals("cooldowns")) continue; // the cooldown ledger shares the folder

            JsonObject json = MinestarFiles.readObject(file);
            Kit kit = Kit.load(json, stem, registries);
            if (kit == null) {
                RollMod.LOGGER.warn("[Kits] {} is not a usable kit; skipping it", file);
                continue;
            }
            KITS.put(kit.name(), kit);
        }
        loaded = true;
    }

    static Path path(String name) {
        return MinestarFiles.resolve(FOLDER, name + ".json");
    }
}
