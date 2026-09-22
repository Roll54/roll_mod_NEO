package com.roll_54.roll_mod.compat.ftb;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.data.MinestarFiles;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.ModList;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The homes a player set with FTB Essentials, read from the files it left behind.
 *
 * <p>FTB Essentials is gone, so there is no API to ask any more — but its player data is still in
 * the world folder, and a home somebody set there is still somewhere they want to get back to. This
 * reads {@code <world>/ftbessentials/playerdata/<uuid>.snbt} directly, with ftblibrary's SNBT
 * parser where that mod is present and vanilla's as a fallback.
 *
 * <p>Nothing is written back to those files. A migrated home is noted in
 * {@code minestar/migrated_homes.json} and filtered out here instead: editing another mod's data
 * would risk the rest of what it holds — mutes, the last-seen position, teleport history — for a
 * player who might yet reinstall it.
 */
public final class LegacyHomesFacade implements HomesFacade {

    private static final String MIGRATED_FILE = "migrated_homes.json";

    @Override
    public List<Legacy> homesOf(ServerPlayer player) {
        CompoundTag data = read(player);
        if (data == null || !data.contains("homes")) return List.of();

        Set<String> migrated = migrated(player.getUUID());
        CompoundTag homes = data.getCompound("homes");

        List<Legacy> found = new ArrayList<>();
        for (String name : homes.getAllKeys()) {
            if (migrated.contains(name.toLowerCase(Locale.ROOT))) continue;
            Legacy legacy = toLegacy(name, homes.getCompound(name));
            if (legacy != null) found.add(legacy);
        }
        return found;
    }

    @Override
    public boolean forget(ServerPlayer player, String name) {
        Set<String> migrated = migrated(player.getUUID());
        if (!migrated.add(name.toLowerCase(Locale.ROOT))) return false;

        JsonObject root = MinestarFiles.readObject(path());
        JsonArray names = new JsonArray();
        migrated.forEach(names::add);
        root.add(player.getUUID().toString(), names);
        MinestarFiles.write(path(), root);
        return true;
    }

    /**
     * {@code null} when the entry names a dimension that will not parse, which is the one field
     * that can be nonsense: FTB wrote it as a plain string and the world it named may be long gone.
     */
    private static Legacy toLegacy(String name, CompoundTag home) {
        ResourceLocation dimension = ResourceLocation.tryParse(home.getString("dim"));
        if (dimension == null) return null;

        // Block centre on the horizontal axes: a home pinned to a corner drops the player against
        // the neighbouring block's face, which is not where they stood when they set it.
        return new Legacy(name, dimension,
                home.getInt("x") + 0.5, home.getInt("y"), home.getInt("z") + 0.5,
                home.getFloat("yRot"), home.getFloat("xRot"));
    }

    /**
     * The player's FTB data, or {@code null} when there is none or it will not parse.
     *
     * <p>ftblibrary's parser first, because the file is in ftblibrary's dialect and not vanilla's:
     * it writes one entry per line with no commas between them, which vanilla's {@code TagParser}
     * rejects at the second key ("Expected '}'"). Every one of these files was written by that mod,
     * so its own parser is the authority on them.
     *
     * <p>Vanilla is still tried afterwards, for a server that no longer has ftblibrary installed.
     * Comment lines are stripped for it: the dialect allows {@code #} comments and vanilla does not.
     */
    private static CompoundTag read(ServerPlayer player) {
        Path file = playerData(player).resolve(player.getUUID() + ".snbt");
        if (!Files.isRegularFile(file)) return null;

        if (FTB_LIBRARY) {
            try {
                CompoundTag tag = Ftb.read(file);
                if (tag != null) return tag;
            } catch (Throwable ignored) {
                // Their parser is gone or changed shape; the vanilla attempt below still stands.
            }
        }

        try {
            StringBuilder text = new StringBuilder();
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (line.stripLeading().startsWith("#")) continue;
                text.append(line).append('\n');
            }
            return TagParser.parseTag(text.toString());
        } catch (Exception e) {
            // Once per file per run: this is read every time the homes tab opens, and a file that
            // will not parse now will not parse on the next click either.
            if (WARNED.add(file)) {
                RollMod.LOGGER.warn("[Homes] could not read legacy FTB data at {}", file, e);
            }
            return null;
        }
    }

    /** Files already reported as unreadable, so the warning is not repeated on every hub open. */
    private static final Set<Path> WARNED = ConcurrentHashMap.newKeySet();

    private static final boolean FTB_LIBRARY = ModList.get().isLoaded("ftblibrary");

    /**
     * Isolated holder for ftblibrary's parser, only ever touched when {@link #FTB_LIBRARY} is true,
     * so the mod still class-loads without it — the same shape {@code LuckPermsCompat} uses.
     */
    private static final class Ftb {
        static CompoundTag read(Path file) {
            return dev.ftb.mods.ftblibrary.snbt.SNBT.read(file);
        }
    }

    static Path playerData(ServerPlayer player) {
        return player.server.getWorldPath(LevelResource.ROOT).resolve("ftbessentials/playerdata");
    }

    private static Set<String> migrated(UUID player) {
        JsonObject root = MinestarFiles.readObject(path());
        Set<String> names = new LinkedHashSet<>();
        if (root.has(player.toString())) {
            for (JsonElement element : root.getAsJsonArray(player.toString())) {
                names.add(element.getAsString().toLowerCase(Locale.ROOT));
            }
        }
        return names;
    }

    private static Path path() {
        return MinestarFiles.resolve(MIGRATED_FILE);
    }
}
