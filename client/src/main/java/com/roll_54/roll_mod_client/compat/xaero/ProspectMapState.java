package com.roll_54.roll_mod_client.compat.xaero;

import com.roll_54.roll_mod.compat.gtmogs.OreMaterials;
import com.roll_54.roll_mod.config.RollClientConfig;
import com.roll_54.roll_mod.data.datagen.ore.OreDefinition;
import com.roll_54.roll_mod.prospecting.ClientProspects;
import com.roll_54.roll_mod.prospecting.Discovery;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * What the map overlays show, kept apart from Xaero's classes so the keybind can reach it without either map
 * installed: whether ores are shown at all (the keybind and the World Map button flip it), the ore layer's height,
 * the settings, and the colours and tooltips both maps draw.
 */
@OnlyIn(Dist.CLIENT)
public final class ProspectMapState {

    private static boolean visible = true;
    /** The ore layer's height; null until first used, then the player's own. */
    private static Integer layerY;

    private ProspectMapState() {}

    public static boolean visible() {
        return visible;
    }

    /* ----------------------------------------------- vein filter ----------------------------------------------- */

    /**
     * The veins the player chose not to see (keys as {@link ProspectVeins} makes them). Hidden rather than shown, so a
     * vein found later is shown until the player says otherwise. Kept in {@code config/roll_mod_prospect_veins.txt},
     * one key a line.
     */
    private static java.util.Set<String> hidden;
    private static int filterVersion;

    private static java.nio.file.Path filterFile() {
        return net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get().resolve("roll_mod_prospect_veins.txt");
    }

    private static java.util.Set<String> hidden() {
        if (hidden == null) {
            hidden = new java.util.TreeSet<>();
            try {
                var file = filterFile();
                if (java.nio.file.Files.exists(file)) {
                    for (String line : java.nio.file.Files.readAllLines(file)) {
                        if (!line.isBlank()) hidden.add(line.trim());
                    }
                }
            } catch (java.io.IOException e) {
                com.roll_54.roll_mod.RollMod.LOGGER.warn("[Prospecting] could not read the vein filter", e);
            }
        }
        return hidden;
    }

    /** Whether the vein {@code key} is shown on the maps. */
    public static boolean shown(String key) {
        return !hidden().contains(key);
    }

    /** Bumped whenever the filter changes, so the maps group their veins again. */
    public static int filterVersion() {
        return filterVersion;
    }

    /** Shows or hides each of {@code keys}, then saves the filter. */
    public static void setShown(java.util.Collection<String> keys, boolean show) {
        boolean changed = show ? hidden().removeAll(keys) : hidden().addAll(keys);
        if (!changed) return;
        filterVersion++;
        try {
            java.nio.file.Files.write(filterFile(), hidden());
        } catch (java.io.IOException e) {
            com.roll_54.roll_mod.RollMod.LOGGER.warn("[Prospecting] could not save the vein filter", e);
        }
    }

    public static void toggle() {
        visible = !visible;
        var player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(Component.translatable(visible
                    ? "message.roll_mod.prospect_map.shown" : "message.roll_mod.prospect_map.hidden"), true);
        }
    }

    public static RollClientConfig.ProspectingMap config() {
        RollClientConfig config = RollClientConfig.INSTANCE;
        return config != null ? config.prospectingMap : new RollClientConfig.ProspectingMap();
    }

    public static boolean oreLayer() {
        return config().mapMode == RollClientConfig.ProspectMapMode.ORE_LAYER;
    }

    public static int layerY() {
        if (layerY == null) {
            var player = Minecraft.getInstance().player;
            layerY = player == null ? 0 : player.getBlockY();
        }
        return layerY;
    }

    /** Back to the height the player stands at. */
    public static void resetLayer() {
        layerY = null;
        layerY();
    }

    public static void moveLayer(int dy) {
        layerY = layerY() + dy;
    }

    /** The dimension the player is in, as the finds are keyed. */
    public static String dimension() {
        var level = Minecraft.getInstance().level;
        return level == null ? "" : level.dimension().location().toString();
    }

    /** The ore scanner's colour for the ore, opaque; the ore block's own map colour for an ore it does not know. */
    public static int color(Discovery d) {
        String base = d.oreKey.substring(d.oreKey.indexOf(':') + 1);
        OreDefinition def = OreMaterials.byId(base);
        if (def != null) return 0xFF000000 | Integer.parseInt(def.hexColor().substring(1), 16);
        var block = BuiltInRegistries.BLOCK.get(d.displayBlock);
        return 0xFF000000 | block.defaultMapColor().col;
    }

    /**
     * The hover text of a prospected chunk: the vein, then each of its ores. Once {@code exactCountAfter} blocks of
     * the chunk's ores were mined, every line carries its count of blocks in the chunk, the vein line their total.
     */
    public static List<Component> chunkTooltip(List<Discovery> ores) {
        return chunkTooltip(ProspectVeins.name(ores), ores);
    }

    /** As {@link #chunkTooltip(List)}, for a vein whose name is already known ({@code null}: unknown vein). */
    public static List<Component> chunkTooltip(Component veinName, List<Discovery> ores) {
        List<Component> lines = new ArrayList<>();
        int mined = 0, total = 0;
        boolean revealed = false;
        for (Discovery d : ores) {
            mined += d.mined;
            revealed |= d.revealed;
            total += d.veinCount;
        }
        boolean counts = revealed || mined >= config().exactCountAfter;
        Component vein = veinName;
        if (vein == null) vein = Component.translatable("gui.roll_mod.prospect_map.vein_unknown");
        lines.add((counts
                ? Component.translatable("gui.roll_mod.prospect_map.vein_count", vein, total)
                : Component.translatable("gui.roll_mod.prospect_map.vein_name", vein)).withStyle(ChatFormatting.GOLD));
        for (Discovery d : ores) {
            Component name = ClientProspects.name(d);
            lines.add((counts
                    ? Component.translatable("gui.roll_mod.prospect_map.ore_count", name, d.veinCount)
                    : name.copy()).withStyle(ChatFormatting.WHITE));
        }
        return lines;
    }

    /** The hover text of one ore in a chunk: its name and heights, and the vein's size once enough of it was mined. */
    public static List<Component> tooltip(Discovery d) {
        List<Component> lines = new ArrayList<>();
        lines.add(ClientProspects.name(d).copy().withStyle(ChatFormatting.WHITE));
        lines.add(ClientProspects.ranges(d).withStyle(ChatFormatting.AQUA));
        if (d.revealed || d.mined >= config().exactCountAfter) {
            lines.add(Component.translatable("gui.roll_mod.prospect_map.vein", d.veinCount, d.mined)
                    .withStyle(ChatFormatting.GOLD));
        } else {
            lines.add(Component.translatable("gui.roll_mod.prospect_map.mine_more",
                    config().exactCountAfter - d.mined).withStyle(s -> s.withColor(0xD8D8D8)));
        }
        return lines;
    }
}
