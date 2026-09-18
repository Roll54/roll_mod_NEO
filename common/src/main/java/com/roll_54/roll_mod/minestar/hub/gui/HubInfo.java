package com.roll_54.roll_mod.minestar.hub.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.util.LegacyText;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * The body of a hub section's information panel, read from
 * {@code assets/roll_mod/hub_info/<language>/<id>.json}.
 *
 * <p>The point of the file is that the explanations are wording, not code: a server owner can
 * rewrite what a section says — or a resource pack can override it per-pack — without a Java change
 * or a rebuild. The text uses the same {@code &a} colour codes players already type into warp and
 * home names, so there is one markup convention across the mod; {@link LegacyText} does the
 * converting, and no second parser exists.
 *
 * <p>One folder per language, rather than one lang key per line. These are paragraphs with headings,
 * rules and indentation, and flattening that into the lang file would have meant a key per line plus
 * a second scheme for the structure around them; a folder keeps each translation readable as the
 * document it is. A language with no folder, or with a file missing from one, falls back to
 * {@code en_us} rather than showing nothing — a half-translated pack is worth shipping.
 *
 * <p>The file is re-read on every open rather than cached. It is a few hundred bytes and this
 * happens once per click on the {@code I} button, which buys live {@code F3+T} reloading for free —
 * worth far more than the read it saves, and means a language change takes effect on the next open
 * with nothing listening for it.
 *
 * <p>Client-only: the hub's element tree is built on the dedicated server too, so this returns an
 * empty list there and the panel simply has no children. That is safe precisely because the panel
 * holds no sync values — see {@link HubUI}'s class javadoc for why anything positional could not be
 * built this way.
 */
public final class HubInfo {

    /**
     * The font every line of every explanation is set in: the player's own, whatever it currently
     * is. {@code minecraft:default} is the face a resource pack replaces, so pinning it here follows
     * the pack rather than fighting it.
     *
     * <p>Stated rather than left alone, because leaving it alone does not get you the default. The
     * floating window loads LdLib's {@code modern} stylesheet for its scroll bar and close button
     * (a window has no screen behind it to inherit a stylesheet from), and that sheet carries
     * {@code text, label { font: ldlib2:jetbrains_mono_bold; }} — so every line of prose came up in
     * JetBrains Mono. An inline style outranks a stylesheet, which is what this is.
     */
    public static final ResourceLocation FONT = net.minecraft.network.chat.Style.DEFAULT_FONT;

    /** Matches the panel's own inset, so an indented line still clears the border. */
    private static final int LINE_GAP = 2;
    private static final int RULE_COLOR = 0x40FFFFFF;
    private static final int RULE_MARGIN = 4;
    private static final int DEFAULT_SPACE = 6;

    private HubInfo() {}

    /**
     * The panel body for one tab id, or an empty list on the server and on any failure — a section
     * whose file is missing or malformed shows {@code gui.roll_mod.hub.info.missing} rather than
     * taking the screen down with it.
     */
    public static List<UIElement> read(String id) {
        if (FMLEnvironment.dist != Dist.CLIENT) return List.of();
        return ClientRead.read(id);
    }

    /** The language every section is guaranteed to have, and what an untranslated one falls back to. */
    private static final String FALLBACK_LANGUAGE = "en_us";

    /** Isolated holder for the client-only resource lookup, as {@code PlayerPreviewElement} does. */
    private static final class ClientRead {

        static List<UIElement> read(String id) {
            String language = net.minecraft.client.Minecraft.getInstance()
                    .getLanguageManager().getSelected();

            List<UIElement> lines = tryRead(language, id);
            if (lines == null && !FALLBACK_LANGUAGE.equals(language)) {
                lines = tryRead(FALLBACK_LANGUAGE, id);
            }
            return lines == null ? List.of(missing()) : lines;
        }

        /**
         * One language's file, or {@code null} if it is absent or unreadable — which is the whole
         * reason this does not return {@link #missing()} itself. The caller has to be able to tell
         * "nothing here, try English" apart from "give up", and a missing panel body is a legitimate
         * answer only after both have been tried.
         */
        @Nullable
        private static List<UIElement> tryRead(String language, String id) {
            ResourceLocation path = ResourceLocation.fromNamespaceAndPath(
                    RollMod.MODID, "hub_info/" + language + "/" + id + ".json");

            try {
                var resource = net.minecraft.client.Minecraft.getInstance()
                        .getResourceManager().getResource(path);
                if (resource.isEmpty()) return null;

                try (InputStream in = resource.get().open();
                     InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    return parse(root);
                }
            } catch (Exception e) {
                RollMod.LOGGER.debug("Could not read hub info {}", path, e);
                return null;
            }
        }

        /** {@code null} rather than {@link #missing()}, for the reason {@link #tryRead} gives. */
        @Nullable
        private static List<UIElement> parse(JsonObject root) {
            List<UIElement> out = new ArrayList<>();
            if (!root.has("lines") || !root.get("lines").isJsonArray()) return null;

            JsonArray lines = root.getAsJsonArray("lines");
            for (JsonElement element : lines) {
                if (!element.isJsonObject()) continue;
                UIElement built = line(element.getAsJsonObject());
                if (built != null) out.add(built);
            }
            return out.isEmpty() ? null : out;
        }

        private static UIElement line(JsonObject line) {
            // "text" is the default type, so the common case needs no type at all.
            String type = line.has("type") ? line.get("type").getAsString() : "text";
            return switch (type) {
                case "rule" -> rule(line);
                case "space" -> space(line);
                case "text" -> text(line);
                default -> null;
            };
        }

        private static UIElement text(JsonObject line) {
            String raw = line.has("text") ? line.get("text").getAsString() : "";
            boolean wrap = !line.has("wrap") || line.get("wrap").getAsBoolean();
            boolean center = line.has("center") && line.get("center").getAsBoolean();
            int indent = line.has("indent") ? line.get("indent").getAsInt() : 0;

            Label label = new Label();
            label.setText(LegacyText.display(raw));
            // A Label sizes to its content by default, so WRAP does nothing without a width to wrap
            // against — the percentage is what makes the flag mean anything.
            //
            // Padding rather than a margin for the indent: sizing is border-box, so padding insets
            // the text inside the 100% while a margin would have made the line 100% + indent wide.
            // With horizontal scrolling off, that overhang was silently clipped — indented lines lost
            // their last word or two.
            label.layout(l -> l.widthPercent(100).paddingLeft(indent).marginBottom(LINE_GAP));
            label.textStyle(t -> t.font(FONT)
                    .textWrap(wrap ? TextWrap.WRAP : TextWrap.NONE)
                    .adaptiveHeight(true)
                    .textAlignHorizontal(center ? Horizontal.CENTER : Horizontal.LEFT));
            return label;
        }

        private static UIElement rule(JsonObject line) {
            int color = line.has("color") ? line.get("color").getAsInt() : RULE_COLOR;
            UIElement divider = new UIElement();
            divider.layout(l -> l.widthPercent(100).height(1)
                    .marginTop(RULE_MARGIN).marginBottom(RULE_MARGIN));
            divider.style(s -> s.background(new ColorRectTexture(color)));
            return divider;
        }

        private static UIElement space(JsonObject line) {
            int height = line.has("height") ? line.get("height").getAsInt() : DEFAULT_SPACE;
            UIElement gap = new UIElement();
            gap.layout(l -> l.widthPercent(100).height(height));
            return gap;
        }

        private static UIElement missing() {
            Label label = new Label();
            label.setText(Component.translatable("gui." + RollMod.MODID + ".hub.info.missing")
                    .withStyle(ChatFormatting.GRAY));
            label.layout(l -> l.widthPercent(100));
            label.textStyle(t -> t.font(FONT).textWrap(TextWrap.WRAP).adaptiveHeight(true));
            return label;
        }
    }
}
