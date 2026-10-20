package com.roll_54.roll_mod.blocks;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.blocks.entity.HueriStatueBlockEntity;
import com.roll_54.roll_mod.network.packet.HueriStatuePackets;
import com.roll_54.roll_mod.util.ItemMentions;
import com.roll_54.roll_mod.util.TextTags;
import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * The Hueri statue's texts, from {@code assets/roll_mod/texts/hueri_statue/<language>.json} and,
 * for the participants tab, {@code texts/hueri_statue_participants/<language>.json} — the player's
 * language, else {@code en_us}. Read on every open, so {@code F3+T} shows an edit at once.
 *
 * <p>The file is vanilla JSON text, the {@code /tellraw} format, in one of two shapes:
 * <ul>
 *   <li>{@code {"title": <text>, "body": [<text>, …]}} — the page's header, and its paragraphs;</li>
 *   <li>{@code [<text>, …]} — the paragraphs alone, under the default header (the block's name).</li>
 * </ul>
 * Either way the body <b>array is a list of paragraphs</b>, each drawn on its own wrapping line,
 * rather than one component run together; a single text in its place is one paragraph. {@code <item:…>} tags
 * anywhere in the text are expanded ({@link ItemMentions}) — the file ships with the mod, so it is
 * trusted the way an operator is — and so are {@code <player:UUID>} and {@code <player:UUID:rank>}
 * ({@link HueriStatuePlayers}), from what the server sent just before the screen opened.
 */
@OnlyIn(Dist.CLIENT)
public final class HueriStatueText {

    /** The memorial's own page. */
    public static final String FOLDER_TEXT = "texts/hueri_statue/";
    /** The participants tab: the players who came to the first event, in the same format. */
    public static final String FOLDER_PARTICIPANTS = "texts/hueri_statue_participants/";
    private static final String FALLBACK = "en_us";

    private HueriStatueText() {}

    /** @param title the header, or {@code null} for the default one */
    public record Page(@Nullable Component title, List<Component> body) {}

    /**
     * A click on the statue at {@code pos} (its lower half): asks the server to open it, naming the
     * players this client's texts mention — both tabs' — and the statue's flower donors, so their
     * names can come back first.
     */
    public static void requestOpen(BlockPos pos) {
        StringBuilder all = new StringBuilder();
        for (String folder : List.of(FOLDER_TEXT, FOLDER_PARTICIPANTS)) {
            String raw = raw(folder);
            if (raw != null) all.append(raw).append('\n');
        }
        // The file's players first, so donors are the ones dropped should the total pass MAX.
        List<UUID> ids = new ArrayList<>(HueriStatuePlayers.ids(all.toString()));
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.level.getBlockEntity(pos) instanceof HueriStatueBlockEntity statue) {
            for (HueriStatueBlockEntity.Flower flower : statue.flowers()) {
                if (ids.size() >= HueriStatuePlayers.MAX) break;
                if (!ids.contains(flower.donor())) ids.add(flower.donor());
            }
        }
        PacketDistributor.sendToServer(new HueriStatuePackets.Open(pos, ids));
    }

    /** The players {@code folder}'s file names, in order; none when it is missing. */
    public static List<UUID> ids(String folder) {
        String raw = raw(folder);
        return raw == null ? List.of() : HueriStatuePlayers.ids(raw);
    }

    /** The page in {@code folder}, one of {@link #FOLDER_TEXT} and {@link #FOLDER_PARTICIPANTS}. */
    public static Page load(String folder) {
        String raw = raw(folder);
        if (raw == null) return missing("no " + folder + FALLBACK + ".json");

        Minecraft minecraft = Minecraft.getInstance();
        HolderLookup.Provider registries = minecraft.level != null
                ? minecraft.level.registryAccess() : net.minecraft.core.RegistryAccess.EMPTY;
        try {
            JsonElement json = JsonParser.parseString(raw);
            Component title = null;
            JsonElement body = json;
            if (json.isJsonObject() && json.getAsJsonObject().has("body")) {
                JsonElement header = json.getAsJsonObject().get("title");
                if (header != null) title = text(header, registries);
                body = json.getAsJsonObject().get("body");
            }
            List<Component> out = new ArrayList<>();
            if (body.isJsonArray()) {
                for (JsonElement paragraph : body.getAsJsonArray()) out.add(text(paragraph, registries));
            } else {
                out.add(text(body, registries));
            }
            return new Page(title, out);
        } catch (Exception e) {
            RollMod.LOGGER.error("[{}] Could not read the Hueri statue text", RollMod.MODID, e);
            return missing(e.getMessage());
        }
    }

    /** The file for the player's language, else {@link #FALLBACK}'s, as text; {@code null} when neither exists or reads. */
    @Nullable
    private static String raw(String folder) {
        Minecraft minecraft = Minecraft.getInstance();
        String language = minecraft.getLanguageManager().getSelected();
        Optional<Resource> resource = minecraft.getResourceManager().getResource(file(folder, language));
        if (resource.isEmpty()) resource = minecraft.getResourceManager().getResource(file(folder, FALLBACK));
        if (resource.isEmpty()) return null;
        try (Reader reader = resource.get().openAsReader()) {
            java.io.StringWriter out = new java.io.StringWriter();
            reader.transferTo(out);
            return out.toString();
        } catch (Exception e) {
            RollMod.LOGGER.error("[{}] Could not read the Hueri statue text", RollMod.MODID, e);
            return null;
        }
    }

    private static Component text(JsonElement json, HolderLookup.Provider registries) {
        Component text = ItemMentions.expand(Component.Serializer.fromJson(json, registries), true);
        return TextTags.replace(text, HueriStatuePlayers.TAG, m -> HueriStatuePlayers.display(
                ClientHueriStatue.PLAYERS.get(UUID.fromString(m.group(1))), m.group(2) != null));
    }

    private static ResourceLocation file(String folder, String language) {
        return RollMod.id(folder + language + ".json");
    }

    private static Page missing(String reason) {
        return new Page(null, List.of(Component.translatable("gui.roll_mod.hueri_statue.missing", String.valueOf(reason))
                .withStyle(ChatFormatting.RED)));
    }
}
