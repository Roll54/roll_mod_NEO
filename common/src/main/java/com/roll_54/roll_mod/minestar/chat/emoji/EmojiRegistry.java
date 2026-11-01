package com.roll_54.roll_mod.minestar.chat.emoji;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.Command;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.network.packet.EmojiPackets;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ported from Eden_LD (MIT, (c) 2026 LeDok; see docs/third-party-licenses.txt), without its picker,
 * favorites, groups, guest emojis and standard (Unicode) emojis.
 *
 * <p>The Discord server's custom emojis, usable in game chat as {@code :name:}. The list is
 * {@code config/roll_mod/emoji.json} (name → Discord emoji id, or its image link), optionally kept in
 * step with the Discord server by {@link DiscordEmojiSync}; images are downloaded once from Discord's
 * CDN into {@code config/roll_mod/emoji/}. Clients draw them from a runtime font ({@link #FONT},
 * {@code EmojiFont}) — no resource pack. Each emoji gets a private-use character (U+F4000 on, in
 * file order); animated emojis play ({@link GifStrip}).
 *
 * <p>Both ways with Discord: global chat goes out with {@code :name:} as {@code <:name:id>}
 * ({@link #toDiscord}), and Discord lines coming in get their {@code <:name:id>} and {@code :name:}
 * drawn as the emoji ({@link #render}). Links in chat become clickable; GIF links (Tenor, Giphy,
 * {@code .gif}) a short {@code [GIF]}.
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class EmojiRegistry {

    public static final ResourceLocation FONT = RollMod.id("emoji");
    /**
     * In Unicode's Supplementary Private Use Area-A (plane 15): fonts and resource packs put their
     * own glyphs in the BMP's private use area (U+E000–U+F8FF), so these never take theirs over.
     */
    private static final int FIRST_CODEPOINT = 0xF4000;
    private static final int LAST_CODEPOINT = 0xF7FFF;
    private static final int IMAGE_SIZE = 32;
    /** Most PNG bytes in one images packet; a client's whole request is split over several. */
    private static final int PACKET_BYTES = 512 * 1024;

    private static final Path FOLDER = FMLPaths.CONFIGDIR.get().resolve("roll_mod");
    private static final Path FILE = FOLDER.resolve("emoji.json");
    private static final Path CACHE = FOLDER.resolve("emoji");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    private static final Pattern ID_IN_LINK = Pattern.compile("/emojis/(\\d+)");
    public static final Pattern SHORTCODE = Pattern.compile(":([A-Za-z0-9_~+\\-]{1,40}):");
    /** {@code <:name:id>} / {@code <a:name:id>} as Discord writes custom emoji. */
    private static final Pattern DISCORD = Pattern.compile("<(a?):([A-Za-z0-9_~\\-]{1,32}):(\\d+)>");
    private static final Pattern URL = Pattern.compile("https?://[^\\s<>\"]+");
    private static final Pattern CHARACTER = Pattern.compile("([\\x{F4000}-\\x{F7FFF}])");
    /** Groups: 1 animated, 2 name, 3 id ({@code <:name:id>}); 4 {@code :name:}; 5 a link; 6 an emoji's character. */
    private static final Pattern TOKENS = Pattern.compile(
            DISCORD.pattern() + "|" + SHORTCODE.pattern() + "|(" + URL.pattern() + ")|" + CHARACTER.pattern());

    /**
     * The Minestar Discord's emojis: what a fresh emoji.json starts with (the list Eden_LD ships).
     * In-game names may differ from Discord's; Discord matches by id.
     */
    private static final Map<String, String> STARTER = new LinkedHashMap<>();

    static {
        String[][] starter = {
                {"AZOV", "981668123281330186"}, {"KEKNerv", "1233415406304104551"}, {"KEKW", "1233414671781068881"},
                {"KEKWait", "1233415321147281478"}, {"Ordo", "1235630282468495391"}, {"Sano", "1235630330153795716"},
                {"Thaumonomicon", "1235630114272841798"}, {"UPAOUN", "981667560862941204"},
                {"administrator", "1483920627651051620"}, {"ban", "1227744263299666012"},
                {"capybara", "997087526730866791"}, {"catallblack", "1227382149397741640"},
                {"catcalico", "1227381639546667149"}, {"catjellie", "1227381638275530832"},
                {"catocelot", "1227381661268836422"}, {"catpersian", "1227381648019034213"},
                {"catragdoll", "1227381656814354492"}, {"catred", "1227381631627563018"},
                {"catshorthair", "1227381632764477501"}, {"catsiamese", "1227382112714227812"},
                {"cattabby", "1227381883264827432"}, {"cattuxedo", "1227381628553400482"},
                {"catwhite", "1227381629723348995"}, {"concern", "1286368368886288455"}, {"cry", "1014803122255839292"},
                {"devsys", "1290512616124583968"}, {"diamondpickaxe", "1227720256190025820"},
                {"diamondsword", "1227381931973414963"}, {"catstare", "1014802905959780413"},
                {"ping", "1014802983139156018"}, {"pikachu", "1014803051871227905"},
                {"pepediamond", "1014803184004374638"}, {"bread", "1014803207484088410"}, {"tnt", "1014803232058507324"},
                {"cake", "1014803253898260562"}, {"heart", "1014803276492963870"}, {"goldsword", "1227381643535450133"},
                {"harold", "1279550727110398074"}, {"helper", "1483920544377077771"}, {"hmmm", "1014803375029768252"},
                {"idi", "1551465897271758859"}, {"ironsword", "1227381655375974460"}, {"likeee", "1014803348861501521"},
                {"lp_icon", "1520498345855156224"}, {"mcmissingtexture", "1279552057291833377"},
                {"minestar", "1241451269835460770"}, {"moderator", "1483920589575159912"},
                {"mt_neo_money", "1469584904802271387"}, {"netheresword", "1227381636870443090"},
                {"no", "1097478193197367486"}, {"obsc1", "1227383629039140926"}, {"obsc2", "1227383630473597070"},
                {"pokeball", "1236270881714737255"}, {"rage", "1014802946803908658"}, {"sadcat", "1014803324161249321"},
                {"stonesword", "1227381634106523748"}, {"totemofundying", "1227384055054471249"},
                {"woodensword", "1227382056699564132"}, {"wtfcat", "1239297480428880013"}, {"wut", "1014803093101228112"},
                {"yes", "1097478190701744189"},
        };
        for (String[] entry : starter) STARTER.put(entry[0], entry[1]);
    }

    /** One listed emoji. {@code discordName} drops a {@code ~1} suffix, which Discord uses for duplicates. */
    public record Emoji(String name, String discordName, String id, boolean animated, int codepoint) {}

    /** What clients get for one emoji: the PNG, and frame delays (ms) when it's animated. */
    private record Image(byte[] png, List<Integer> delays) {}

    private static volatile Map<String, Emoji> byName = Map.of();
    private static volatile Map<String, Emoji> byId = Map.of();
    private static volatile Map<Integer, Emoji> byCodepoint = Map.of();
    /** Emojis whose image is ready — every client gets these. */
    private static volatile List<EmojiPackets.EmojiRef> ready = List.of();
    private static final Map<String, Image> IMAGES = new ConcurrentHashMap<>();

    private EmojiRegistry() {}

    /* ------------------------------------------------- events ------------------------------------------------- */

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        reload(event.getServer());
    }

    /** With the Discord sync set up, emojis added or deleted on Discord arrive by themselves. */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        int minutes = DiscordEmojiSync.syncMinutes();
        if (minutes > 0 && DiscordEmojiSync.enabled() && server.getTickCount() % (minutes * 1200) == 0
                && server.getTickCount() > 0) {
            reload(server);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sendList(player);
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("emojis")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("reload").executes(ctx -> {
                    DiscordEmojiSync.reread();
                    reload(ctx.getSource().getServer());
                    ctx.getSource().sendSuccess(() -> Component.translatable("msg.roll_mod.emoji.reloading"), true);
                    return Command.SINGLE_SUCCESS;
                })));
    }

    /* ------------------------------------------------- loading ------------------------------------------------ */

    /** Re-reads the list, fetches missing images off-thread, then updates everyone online. */
    public static void reload(MinecraftServer server) {
        // Discord and the CDN are slow: all of it runs off the server thread.
        CompletableFuture.runAsync(() -> load(server)).exceptionally(e -> {
            RollMod.LOGGER.error("[Emoji] Loading the emoji list failed", e);
            return null;
        });
    }

    private static void load(MinecraftServer server) {
        Map<String, Emoji> names = new LinkedHashMap<>();
        Map<String, Emoji> ids = new HashMap<>();
        Map<Integer, Emoji> characters = new HashMap<>();
        int codepoint = FIRST_CODEPOINT;
        for (Map.Entry<String, JsonElement> entry : readList().entrySet()) {
            String name = entry.getKey();
            if (name.startsWith("_") || !entry.getValue().isJsonPrimitive()) continue;
            String value = entry.getValue().getAsString().trim();
            String id = idOf(value);
            // Every listed name keeps its character (file position), filled in or not, so pasting
            // an id later never shifts the others.
            int character = codepoint++;
            if (id == null || character > LAST_CODEPOINT) continue;
            boolean animated = value.startsWith("a:") || value.contains(".gif") || value.contains("animated=true");
            Emoji emoji = new Emoji(name, name.replaceAll("~\\d+$", ""), id, animated, character);
            names.put(name.toLowerCase(Locale.ROOT), emoji);
            ids.put(id, emoji);
            characters.put(character, emoji);
        }

        Map<String, Image> loaded = new HashMap<>();
        List<EmojiPackets.EmojiRef> list = new ArrayList<>();
        for (Emoji emoji : names.values()) {
            Image image = fetch(emoji);
            if (image == null) continue;
            loaded.put(emoji.id(), image);
            list.add(new EmojiPackets.EmojiRef(emoji.name(), emoji.codepoint(), emoji.id(), image.delays()));
        }

        server.execute(() -> {
            byName = names;
            byId = ids;
            byCodepoint = characters;
            IMAGES.clear();
            IMAGES.putAll(loaded);
            List<EmojiPackets.EmojiRef> previous = ready;
            ready = List.copyOf(list);
            // Players only hear about it when something changed (the hourly sync usually doesn't).
            if (!ready.equals(previous)) {
                server.getPlayerList().getPlayers().forEach(EmojiRegistry::sendList);
            }
            RollMod.LOGGER.info("[Emoji] {} of {} emojis ready.", list.size(), names.size());
        });
    }

    /**
     * The {@code emojis} object of emoji.json (written with the starter list when missing), merged
     * with the Discord server's emojis when the sync is set up — saved when that changed it.
     */
    private static JsonObject readList() {
        JsonObject list = readFile();
        if (!DiscordEmojiSync.enabled()) return list;
        JsonObject merged = DiscordEmojiSync.merge(list);
        if (!merged.equals(list)) {
            try {
                JsonObject root = JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();
                root.add("emojis", merged);
                Files.writeString(FILE, GSON.toJson(root));
            } catch (IOException | RuntimeException e) {
                RollMod.LOGGER.error("[Emoji] Can't save the synced list to {}.", FILE, e);
            }
        }
        return merged;
    }

    private static JsonObject readFile() {
        try {
            if (!Files.exists(FILE)) {
                JsonObject root = new JsonObject();
                root.addProperty("_comment", "Discord emojis for :name: in chat. Value: the emoji's id "
                        + "(Copy Emoji ID), or its image link (Copy Emoji Image Link; needed for animated ones). "
                        + "Empty = not used yet. Order sets each emoji's character: add new ones at the end. "
                        + "/emojis reload applies changes. With the Discord sync set up (config/roll_mod/discord.json) "
                        + "the list follows the Discord server by itself; renaming entries here is kept.");
                JsonObject list = new JsonObject();
                STARTER.forEach(list::addProperty);
                root.add("emojis", list);
                Files.createDirectories(FOLDER);
                Files.writeString(FILE, GSON.toJson(root));
                return list;
            }
            JsonObject root = JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();
            return root.has("emojis") && root.get("emojis").isJsonObject() ? root.getAsJsonObject("emojis") : new JsonObject();
        } catch (IOException | RuntimeException e) {
            RollMod.LOGGER.error("[Emoji] Can't read {}.", FILE, e);
            return new JsonObject();
        }
    }

    /** The Discord id in a list value: the id itself ({@code a:} in front for animated), or an image link. */
    @Nullable
    static String idOf(String value) {
        if (value.isEmpty()) return null;
        String bare = value.startsWith("a:") ? value.substring(2) : value;
        if (bare.chars().allMatch(Character::isDigit)) return bare;
        Matcher matcher = ID_IN_LINK.matcher(value);
        return matcher.find() ? matcher.group(1) : null;
    }

    /**
     * An emoji's image from the cache, downloading it when missing: a still PNG, or for animated
     * emojis the GIF (decoded into a frame strip on every load; the cache keeps the original). Null
     * when Discord doesn't have it. Blocking: off the server thread only.
     */
    @Nullable
    private static Image fetch(Emoji emoji) {
        String extension = emoji.animated() ? "gif" : "png";
        Path file = CACHE.resolve(emoji.id() + "." + extension);
        try {
            if (!Files.exists(file)) {
                HttpResponse<byte[]> response = HTTP.send(HttpRequest.newBuilder(URI.create(
                                "https://cdn.discordapp.com/emojis/" + emoji.id() + "." + extension + "?size=" + IMAGE_SIZE))
                        .timeout(Duration.ofSeconds(15)).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() != 200) {
                    RollMod.LOGGER.warn("[Emoji] :{}: ({}) — Discord answered {}.", emoji.name(), emoji.id(), response.statusCode());
                    return null;
                }
                Files.createDirectories(CACHE);
                Files.write(file, response.body());
            }
            byte[] bytes = Files.readAllBytes(file);
            if (emoji.animated()) {
                GifStrip strip = GifStrip.decode(bytes);
                return new Image(strip.png(), strip.delays());
            }
            return new Image(bytes, List.of());
        } catch (IOException | RuntimeException e) {
            RollMod.LOGGER.warn("[Emoji] Can't get :{}: ({}): {}", emoji.name(), emoji.id(), e.toString());
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /* ------------------------------------------------- players ------------------------------------------------ */

    private static void sendList(ServerPlayer player) {
        if (player.hasDisconnected()) return;
        PacketDistributor.sendToPlayer(player, new EmojiPackets.EmojiList(ready));
    }

    /** The images a client asked for, split into packets of about {@link #PACKET_BYTES}. */
    public static void sendImages(ServerPlayer player, List<String> ids) {
        List<EmojiPackets.EmojiImage> batch = new ArrayList<>();
        int bytes = 0;
        for (String id : ids) {
            Image image = IMAGES.get(id);
            if (image == null) continue;
            if (!batch.isEmpty() && bytes + image.png().length > PACKET_BYTES) {
                PacketDistributor.sendToPlayer(player, new EmojiPackets.EmojiImages(List.copyOf(batch)));
                batch.clear();
                bytes = 0;
            }
            batch.add(new EmojiPackets.EmojiImage(id, image.png()));
            bytes += image.png().length;
        }
        if (!batch.isEmpty()) PacketDistributor.sendToPlayer(player, new EmojiPackets.EmojiImages(List.copyOf(batch)));
    }

    /* --------------------------------------------------- text --------------------------------------------------- */

    @Nullable
    public static Emoji byName(String name) {
        return byName.get(name.toLowerCase(Locale.ROOT));
    }

    /**
     * The emoji as chat text: its character in the emoji font, {@code :name:} on hover. The insertion
     * is {@code :name:}, so Shift+click puts it in the chat box.
     */
    public static MutableComponent glyph(Emoji emoji) {
        return Component.literal(Character.toString(emoji.codepoint())).withStyle(style -> style
                .withFont(FONT)
                .withColor(ChatFormatting.WHITE)
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(":" + emoji.name() + ":")))
                .withInsertion(":" + emoji.name() + ":"));
    }

    /**
     * What a player typed, as chat text: known {@code :name:} (and Discord's {@code <:name:id>})
     * as emojis, links clickable. The caller styles the rest (the parts keep no style of their own).
     */
    public static MutableComponent text(String typed) {
        MutableComponent out = Component.empty();
        appendTokens(out, typed, Style.EMPTY);
        return out;
    }

    /**
     * The text as Discord should get it: known {@code :name:} and emoji characters as
     * {@code <:name:id>}, so Discord shows the real emoji.
     */
    public static String toDiscord(String text) {
        Matcher matcher = Pattern.compile(SHORTCODE.pattern() + "|" + CHARACTER.pattern()).matcher(text);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            Emoji emoji = matcher.group(1) != null ? byName(matcher.group(1)) : byCodepoint.get(matcher.group(2).codePointAt(0));
            String replacement = emoji == null ? matcher.group()
                    : "<" + (emoji.animated() ? "a" : "") + ":" + emoji.discordName() + ":" + emoji.id() + ">";
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    /**
     * A copy of {@code text} (a line from the Discord bridge) with its literal parts rendered:
     * emojis as images, links clickable, and legacy {@code §} colors turned into styles first (the
     * bridge sends them inside plain text, and splitting such text would lose them).
     */
    public static Component render(Component text) {
        MutableComponent out;
        if (text.getContents() instanceof PlainTextContents.LiteralContents literal) {
            out = Component.empty().withStyle(text.getStyle());
            appendLegacy(out, literal.text());
        } else if (text.getContents() instanceof TranslatableContents translatable) {
            // Vanilla chat ("chat.type.text": <name> message) carries the message as an argument.
            Object[] args = translatable.getArgs().clone();
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof Component component) {
                    args[i] = render(component);
                } else if (args[i] instanceof String string) {
                    args[i] = render(Component.literal(string));
                }
            }
            out = Component.translatableWithFallback(translatable.getKey(), translatable.getFallback(), args)
                    .withStyle(text.getStyle());
        } else {
            out = text.plainCopy().withStyle(text.getStyle());
        }
        for (Component sibling : text.getSiblings()) out.append(render(sibling));
        return out;
    }

    /** {@code text} with its {@code §} codes as styles, each run then rendered by {@link #appendTokens}. */
    private static void appendLegacy(MutableComponent out, String text) {
        Style style = Style.EMPTY;
        StringBuilder run = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c != '§' || i + 1 >= text.length()) {
                run.append(c);
                continue;
            }
            char code = Character.toLowerCase(text.charAt(i + 1));
            // §x§r§r§g§g§b§b: a hex color.
            if (code == 'x' && i + 13 < text.length()) {
                StringBuilder hex = new StringBuilder();
                for (int j = i + 3; j <= i + 13; j += 2) hex.append(text.charAt(j));
                try {
                    TextColor color = TextColor.fromRgb(Integer.parseInt(hex.toString(), 16));
                    appendTokens(out, run.toString(), style);
                    run.setLength(0);
                    style = Style.EMPTY.withColor(color);
                    i += 13;
                    continue;
                } catch (NumberFormatException ignored) {
                }
            }
            ChatFormatting formatting = ChatFormatting.getByCode(code);
            if (formatting == null) {
                run.append(c);
                continue;
            }
            appendTokens(out, run.toString(), style);
            run.setLength(0);
            style = formatting == ChatFormatting.RESET ? Style.EMPTY
                    : formatting.isColor() ? Style.EMPTY.withColor(formatting) : style.applyFormat(formatting);
            i++;
        }
        appendTokens(out, run.toString(), style);
    }

    /** {@code text} into {@code out} in {@code style}, with emoji codes, emoji characters and links rendered. */
    private static void appendTokens(MutableComponent out, String text, Style style) {
        if (text.isEmpty()) return;
        Matcher matcher = TOKENS.matcher(text);
        int last = 0;
        while (matcher.find()) {
            Component replacement;
            if (matcher.group(3) != null) {
                Emoji emoji = byId.get(matcher.group(3));
                replacement = emoji != null ? glyph(emoji) : Component.literal(":" + matcher.group(2) + ":").withStyle(style);
            } else if (matcher.group(4) != null) {
                Emoji emoji = byName(matcher.group(4));
                if (emoji == null) continue;
                replacement = glyph(emoji);
            } else if (matcher.group(5) != null) {
                replacement = ChatLinks.link(matcher.group(5));
            } else {
                Emoji emoji = byCodepoint.get(matcher.group(6).codePointAt(0));
                if (emoji == null) continue;
                replacement = glyph(emoji);
            }
            if (matcher.start() > last) out.append(Component.literal(text.substring(last, matcher.start())).withStyle(style));
            out.append(replacement);
            last = matcher.end();
        }
        if (last < text.length()) out.append(Component.literal(text.substring(last)).withStyle(style));
    }
}
