package com.roll_54.roll_mod.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.roll_54.roll_mod.RollMod;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemStack;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Client-side helper that reads {@code data/roll_mod/data_maps/item/starcoin_prices.json}
 * and appends a localized price line to tooltips when present.
 */
public final class StarcoinPriceTooltip {
    private static final ResourceLocation PRICE_MAP = ResourceLocation.fromNamespaceAndPath(
            RollMod.MODID, "data_maps/item/starcoin_prices.json");

    private static final Map<ResourceLocation, Integer> PRICES = new HashMap<>();
    private static boolean loaded;

    private StarcoinPriceTooltip() {}

    /**
     * Adds a starcoin price line to the tooltip if a price is defined for the given stack.
     */
    public static void maybeAddPrice(ItemStack stack, java.util.List<Component> tooltip) {
        if (stack.isEmpty()) return;
        loadIfNeeded();
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        Integer price = PRICES.get(id);
        if (price != null) {
            tooltip.add(Component.translatable("tooltip.roll_mod.sc_price", price)
                    .withStyle(ChatFormatting.GOLD));
        }
    }

    private static void loadIfNeeded() {
        if (loaded) return;
        loaded = true;
        PRICES.clear();
        ResourceManager rm = Minecraft.getInstance().getResourceManager();
        Optional<Resource> res = rm.getResource(PRICE_MAP);
        if (res.isEmpty()) return;

        try (InputStream in = res.get().open();
             InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject values = root.getAsJsonObject("values");
            for (Map.Entry<String, JsonElement> entry : values.entrySet()) {
                ResourceLocation itemId = ResourceLocation.parse(entry.getKey());
                JsonObject obj = entry.getValue().getAsJsonObject();
                if (obj.has("price")) {
                    PRICES.put(itemId, obj.get("price").getAsInt());
                }
            }
        } catch (Exception e) {
            RollMod.LOGGER.warn("Failed to load starcoin prices", e);
        }
    }

    /** Resets the cache, allowing reloading (e.g., after resource reload). */
    public static void reset() {
        loaded = false;
        PRICES.clear();
    }
}
