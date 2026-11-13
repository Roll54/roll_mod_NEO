package com.roll_54.roll_mod.minestar.theme;

import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib2.gui.ui.style.Stylesheet;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.data.MinestarFiles;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.loading.FMLPaths;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

/**
 * The font the mod's LdLib screens are written in, picked apart from the theme: "as the theme" leaves each theme's
 * own (the modern themes write in JetBrains Mono, the vanilla-looking ones in Minecraft's), any other choice is laid
 * over every theme by one extra stylesheet ({@link #sheet}) after the theme's own. Client state, in
 * {@code config/roll_mod/ui_font.json}.
 */
public final class UiFont {

    public enum Choice {
        /** @deprecated No longer offered; read as {@link #MINECRAFT}. */
        @Deprecated
        THEME(null), MINECRAFT("minecraft:default"), MONO("ldlib2:jetbrains_mono_bold"), UNIFORM("minecraft:uniform");

        @Nullable private final String font;

        Choice(@Nullable String font) {
            this.font = font;
        }

        public Component title() {
            return Component.translatable("gui.roll_mod.hub.settings.font." + name().toLowerCase(Locale.ROOT));
        }
    }

    /** The choices Settings offers. {@link Choice#THEME} is deprecated: every theme writes in Minecraft's font unless another is picked. */
    public static java.util.List<Choice> offered() {
        return java.util.List.of(Choice.MINECRAFT, Choice.MONO, Choice.UNIFORM);
    }

    /** Every selector the bundled themes set a font on. */
    private static final String SELECTORS = "text, label, text-field, text-area, button:host .__button_text__, "
            + "text-field:host, .__tag-field_text-field__, .__search-component_text-field__, progress-bar:host label, "
            + "color-selector:host text-field";

    private static Choice choice;
    @Nullable private static Stylesheet sheet;
    @Nullable private static Choice sheetFor;

    private UiFont() {}

    public static synchronized Choice get() {
        if (choice == null) load();
        return choice;
    }

    public static synchronized void set(Choice picked) {
        choice = picked;
        save();
    }

    /** The stylesheet that lays the picked font over the theme, or null for "as the theme". Built once per choice. */
    @Nullable
    public static synchronized Stylesheet sheet() {
        Choice current = get();
        if (current.font == null) return null;
        if (sheet == null || sheetFor != current) {
            sheet = Stylesheet.parse(SELECTORS + " {\n  font: " + current.font + ";\n}\n");
            sheetFor = current;
        }
        return sheet;
    }

    private static void load() {
        // Vanilla's font on every theme until the player picks another in Settings.
        choice = Choice.MINECRAFT;
        JsonObject json = MinestarFiles.readObject(file());
        try {
            if (json.has("font")) choice = Choice.valueOf(json.get("font").getAsString());
            if (choice == Choice.THEME) choice = Choice.MINECRAFT;
        } catch (RuntimeException e) {
            choice = Choice.MINECRAFT;
        }
    }

    private static void save() {
        JsonObject json = new JsonObject();
        json.addProperty("font", choice.name());
        Path path = file();
        Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            Files.createDirectories(path.getParent());
            MinestarFiles.write(tmp, json);
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            RollMod.LOGGER.error("[Theme] could not save {}", path, e);
        }
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("roll_mod").resolve("ui_font.json");
    }
}
