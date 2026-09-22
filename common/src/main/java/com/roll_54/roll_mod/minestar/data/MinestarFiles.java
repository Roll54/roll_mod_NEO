package com.roll_54.roll_mod.minestar.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.roll_54.roll_mod.RollMod;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The {@code minestar/} folder beside the game directory, and the JSON in it.
 *
 * <p>Everything this mod keeps for an operator to read or edit by hand lives here rather than in
 * world data: kits, cooldowns, mutes, the operator roster. {@code StaffCommandLogger} and the
 * cosmetics database already write to the same folder, so this only formalises where it is.
 *
 * <p>Pretty-printed with HTML escaping off, because these files are meant to be opened in an editor
 * and a legacy colour code should read as {@code &6}, not {@code & 6}.
 */
public final class MinestarFiles {

    private static final String FOLDER = "minestar";

    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private MinestarFiles() {}

    /** The folder itself, created if this is the first call. */
    public static Path dir() {
        Path dir = FMLPaths.GAMEDIR.get().resolve(FOLDER);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            RollMod.LOGGER.error("[Minestar] could not create {}", dir, e);
        }
        return dir;
    }

    /** A sub-folder of {@code minestar/}, created if this is the first call. */
    public static Path folder(String name) {
        Path dir = dir().resolve(name);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            RollMod.LOGGER.error("[Minestar] could not create {}", dir, e);
        }
        return dir;
    }

    /** A path inside the folder; parent directories are created, the file itself is not. */
    public static Path resolve(String... parts) {
        Path path = dir();
        for (String part : parts) {
            path = path.resolve(part);
        }
        try {
            Files.createDirectories(path.getParent());
        } catch (IOException e) {
            RollMod.LOGGER.error("[Minestar] could not create {}", path.getParent(), e);
        }
        return path;
    }

    /**
     * Reads one JSON object, or an empty one when the file is missing or unreadable.
     *
     * <p>A hand-edited file that no longer parses must not take the server down with it, so a
     * broken file reads as empty and says so in the log. It is not overwritten until something
     * actually changes, which leaves the operator their typo to fix.
     */
    public static JsonObject readObject(Path path) {
        if (!Files.isRegularFile(path)) return new JsonObject();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            return parsed != null && parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
        } catch (Exception e) {
            RollMod.LOGGER.error("[Minestar] {} could not be read; treating it as empty", path, e);
            return new JsonObject();
        }
    }

    /** Reads any JSON, or {@code null} when the file is missing or unreadable. */
    public static JsonElement read(Path path) {
        if (!Files.isRegularFile(path)) return null;
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader);
        } catch (Exception e) {
            RollMod.LOGGER.error("[Minestar] {} could not be read", path, e);
            return null;
        }
    }

    /** Writes JSON, replacing whatever was there. */
    public static void write(Path path, JsonElement json) {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(json, writer);
        } catch (IOException e) {
            RollMod.LOGGER.error("[Minestar] could not write {}", path, e);
        }
    }
}
