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
    /** The order the moderation tab put the kits in; a name no kit file may take (ids have no dot). */
    private static final String ORDER_FILE = ".order.json";

    private static final Map<String, Kit> KITS = new LinkedHashMap<>();
    private static volatile boolean loaded;

    private KitStore() {}

    /** Every kit, in the order the moderation tab set ({@link #move}); kits it never placed after, by file name. */
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
            if (stem.startsWith(".")) continue; // the order file

            JsonObject json = MinestarFiles.readObject(file);
            Kit kit = Kit.load(json, stem, registries);
            if (kit == null) {
                RollMod.LOGGER.warn("[Kits] {} is not a usable kit; skipping it", file);
                continue;
            }
            KITS.put(kit.name(), kit);
        }
        // The moderation tab's order first; whatever it never placed (a kit added by hand) after, by file name.
        JsonObject orderJson = MinestarFiles.readObject(MinestarFiles.resolve(FOLDER, ORDER_FILE));
        if (orderJson.has("order") && orderJson.get("order").isJsonArray()) {
            Map<String, Kit> ordered = new LinkedHashMap<>();
            for (com.google.gson.JsonElement element : orderJson.getAsJsonArray("order")) {
                if (!element.isJsonPrimitive()) continue;
                Kit kit = KITS.get(element.getAsString());
                if (kit != null) ordered.put(kit.name(), kit);
            }
            for (Map.Entry<String, Kit> entry : KITS.entrySet()) ordered.putIfAbsent(entry.getKey(), entry.getValue());
            KITS.clear();
            KITS.putAll(ordered);
        }
        loaded = true;
    }

    /**
     * Moves a kit one place up ({@code delta} -1) or down (+1) in the list everyone sees, and writes the order out.
     * {@code false} when there is no such kit or it is already at that end.
     */
    public static synchronized boolean move(MinecraftServer server, String name, int delta) {
        load(server.registryAccess());
        List<String> order = new ArrayList<>(KITS.keySet());
        int from = order.indexOf(Kit.clampName(name));
        int to = from + Integer.signum(delta);
        if (from < 0 || to < 0 || to >= order.size()) return false;
        java.util.Collections.swap(order, from, to);
        Map<String, Kit> reordered = new LinkedHashMap<>();
        for (String id : order) reordered.put(id, KITS.get(id));
        KITS.clear();
        KITS.putAll(reordered);
        com.google.gson.JsonArray array = new com.google.gson.JsonArray();
        order.forEach(array::add);
        JsonObject json = new JsonObject();
        json.add("order", array);
        MinestarFiles.write(MinestarFiles.resolve(FOLDER, ORDER_FILE), json);
        return true;
    }

    static Path path(String name) {
        return MinestarFiles.resolve(FOLDER, name + ".json");
    }
}
