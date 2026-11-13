package com.roll_54.roll_mod.minestar.hub.gui;

import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerScreen;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.data.MinestarFiles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Settings → Appearance → Hub language, ported from Eden_LD: the hub in English or Ukrainian whatever the game's
 * language is (the game, chat and other screens keep the game's). While the hub is on screen — its ticks and its
 * frames — the game's {@link Language} is swapped for the picked one and put back right after, so every
 * translatable text the hub draws resolves in the picked language. Client preference, in
 * {@code config/roll_mod/hub_language.json}.
 */
@EventBusSubscriber(modid = RollMod.MODID, value = Dist.CLIENT)
public final class HubLanguage {

    public enum Choice {
        GAME(null), ENGLISH("en_us"), UKRAINIAN("uk_ua");

        @Nullable private final String code;

        Choice(@Nullable String code) {
            this.code = code;
        }

        /** The names in their own language, as on Eden. */
        public Component title() {
            return switch (this) {
                case GAME -> Component.translatable("gui.roll_mod.hub.settings.hub_language.game");
                case ENGLISH -> Component.literal("English");
                case UKRAINIAN -> Component.literal("Українська");
            };
        }
    }

    private static Choice choice = Choice.GAME;
    private static boolean loaded;
    private static final Map<Choice, Language> LANGUAGES = new EnumMap<>(Choice.class);
    /** The game's language while the hub's is swapped in; null when nothing is swapped. */
    @Nullable private static Language swapped;

    private HubLanguage() {}

    public static synchronized Choice get() {
        load();
        return choice;
    }

    public static synchronized void set(Choice picked) {
        load();
        choice = picked;
        save();
    }

    /* ------------------------------------------------- swap -------------------------------------------------- */

    private static boolean hubOnScreen() {
        return Minecraft.getInstance().screen instanceof ModularUIContainerScreen screen
                && HubUI.isHub(screen.getMenu().getModularUI());
    }

    private static void swapIn() {
        if (swapped != null || get() == Choice.GAME || !hubOnScreen()) return;
        Language language = language(choice);
        if (language == null) return;
        swapped = Language.getInstance();
        Language.inject(language);
    }

    private static void swapOut() {
        if (swapped == null) return;
        Language.inject(swapped);
        swapped = null;
    }

    @Nullable
    private static Language language(Choice picked) {
        return LANGUAGES.computeIfAbsent(picked, c -> {
            try {
                // English under the picked one, so a key it lacks still reads as English, not as the key.
                List<String> codes = c.code.equals("en_us") ? List.of("en_us") : List.of("en_us", c.code);
                return ClientLanguage.loadFrom(Minecraft.getInstance().getResourceManager(), codes, false);
            } catch (RuntimeException e) {
                RollMod.LOGGER.warn("[Hub] could not load the {} hub language", c.code, e);
                return null;
            }
        });
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderPre(ScreenEvent.Render.Pre event) {
        swapIn();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderPost(ScreenEvent.Render.Post event) {
        swapOut();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onTickPre(ClientTickEvent.Pre event) {
        swapIn();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onTickPost(ClientTickEvent.Post event) {
        swapOut();
    }

    /* ---------------------------------------------- persistence ---------------------------------------------- */

    private static void load() {
        if (loaded) return;
        loaded = true;
        JsonObject json = MinestarFiles.readObject(file());
        try {
            if (json.has("language")) choice = Choice.valueOf(json.get("language").getAsString());
        } catch (RuntimeException e) {
            choice = Choice.GAME;
        }
    }

    private static void save() {
        JsonObject json = new JsonObject();
        json.addProperty("language", choice.name());
        Path path = file();
        Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            Files.createDirectories(path.getParent());
            MinestarFiles.write(tmp, json);
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            RollMod.LOGGER.error("[Hub] could not save {}", path, e);
        }
        // A resource reload changes the language files; the cache is rebuilt on the next swap.
        LANGUAGES.clear();
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("roll_mod").resolve("hub_language.json");
    }
}
