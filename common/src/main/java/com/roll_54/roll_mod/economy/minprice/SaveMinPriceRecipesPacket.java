package com.roll_54.roll_mod.economy.minprice;

import com.roll_54.roll_mod.RollMod;
import me.fzzyhmstrs.fzzy_config.api.ConfigApi;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * What {@code /rollmod admin minprice handset} saves, keyed by item id or {@code fluid:<id>}:
 * <ul>
 *   <li>{@code recipes}: the recipe each one's minimal price is computed from
 *       ({@link MinPriceConfig#itemRecipes});</li>
 *   <li>{@code prices}: a price typed in by hand, independent of any recipe
 *       ({@link MinPriceConfig#itemWhitelist} / {@link MinPriceConfig#fluidWhitelist}, per bucket for a
 *       fluid); {@link #CLEAR} takes a typed price away again.</li>
 *   <li>{@code choices}: for an ingredient that may be any of several items (a tag or list), which one it is
 *       priced as — the options' ids joined with ',' → the chosen id ({@link MinPriceConfig#ingredientChoices}).</li>
 * </ul>
 * Entries for other keys stay. The config is saved and the table rebuilt at once, so every player's
 * prices change as soon as it is done.
 */
public record SaveMinPriceRecipesPacket(Map<String, String> recipes, Map<String, Long> prices,
                                        Map<String, String> choices)
    implements CustomPacketPayload {

  /** The same as the admin commands had: an operator. */
  public static final int PERMISSION_LEVEL = 2;
  /** In {@code prices}: drop the hand-set price, so the price comes from recipes again. */
  public static final long CLEAR = -1;
  private static final int MAX = 4096;

  public static final Type<SaveMinPriceRecipesPacket> TYPE = new Type<>(RollMod.id("save_min_price_recipes"));

  public static final StreamCodec<RegistryFriendlyByteBuf, SaveMinPriceRecipesPacket> STREAM_CODEC =
      StreamCodec.of(SaveMinPriceRecipesPacket::encode, SaveMinPriceRecipesPacket::decode);

  private static void encode(RegistryFriendlyByteBuf buf, SaveMinPriceRecipesPacket payload) {
    buf.writeVarInt(payload.recipes().size());
    payload.recipes().forEach((key, recipe) -> {
      buf.writeUtf(key);
      buf.writeUtf(recipe);
    });
    buf.writeVarInt(payload.prices().size());
    payload.prices().forEach((key, price) -> {
      buf.writeUtf(key);
      buf.writeVarLong(price + 1); // CLEAR (-1) fits a var-long as 0
    });
    buf.writeVarInt(payload.choices().size());
    payload.choices().forEach((set, chosen) -> {
      buf.writeUtf(set);
      buf.writeUtf(chosen);
    });
  }

  private static SaveMinPriceRecipesPacket decode(RegistryFriendlyByteBuf buf) {
    int count = size(buf);
    Map<String, String> recipes = new LinkedHashMap<>();
    for (int i = 0; i < count; i++) recipes.put(buf.readUtf(), buf.readUtf());
    count = size(buf);
    Map<String, Long> prices = new LinkedHashMap<>();
    for (int i = 0; i < count; i++) prices.put(buf.readUtf(), buf.readVarLong() - 1);
    count = size(buf);
    Map<String, String> choices = new LinkedHashMap<>();
    for (int i = 0; i < count; i++) choices.put(buf.readUtf(), buf.readUtf());
    return new SaveMinPriceRecipesPacket(recipes, prices, choices);
  }

  private static int size(RegistryFriendlyByteBuf buf) {
    int count = buf.readVarInt();
    if (count < 0 || count > MAX) throw new IllegalArgumentException("too many min price entries: " + count);
    return count;
  }

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public static void handle(SaveMinPriceRecipesPacket payload, IPayloadContext context) {
    context.enqueueWork(() -> {
      if (!(context.player() instanceof ServerPlayer player)) return;
      if (!player.hasPermissions(PERMISSION_LEVEL)) return;
      MinPriceConfig config = MinPriceConfig.MAIN;

      Map<String, String> recipes = new LinkedHashMap<>(config.itemRecipes.get());
      int savedRecipes = 0;
      for (Map.Entry<String, String> entry : payload.recipes().entrySet()) {
        ResourceLocation recipeId = ResourceLocation.tryParse(entry.getValue());
        // Only recipes the server has; anything else (EMI's own synthetic pages) cannot price an item.
        if (recipeId == null || player.server.getRecipeManager().byKey(recipeId).isEmpty()) continue;
        recipes.put(entry.getKey(), recipeId.toString());
        savedRecipes++;
      }

      Map<String, Long> items = new LinkedHashMap<>(config.itemWhitelist.get());
      Map<String, Long> fluids = new LinkedHashMap<>(config.fluidWhitelist.get());
      int savedPrices = 0;
      for (Map.Entry<String, Long> entry : payload.prices().entrySet()) {
        String key = entry.getKey();
        boolean fluid = key.startsWith(PriceSolver.FLUID_PREFIX);
        String id = fluid ? key.substring(PriceSolver.FLUID_PREFIX.length()) : key;
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null || !(fluid ? BuiltInRegistries.FLUID.containsKey(rl) : BuiltInRegistries.ITEM.containsKey(rl))) continue;
        Map<String, Long> target = fluid ? fluids : items;
        long price = entry.getValue();
        if (price == CLEAR) target.remove(rl.toString());
        else if (price >= 0) target.put(rl.toString(), price);
        else continue;
        savedPrices++;
      }

      Map<String, String> choices = new LinkedHashMap<>(config.ingredientChoices.get());
      int savedChoices = 0;
      for (Map.Entry<String, String> entry : payload.choices().entrySet()) {
        // Every option and the choice must be real items, and the choice one of the options.
        java.util.List<String> options = new java.util.ArrayList<>();
        boolean ok = true;
        for (String id : entry.getKey().split(",")) {
          ResourceLocation rl = ResourceLocation.tryParse(id.trim());
          if (rl == null || !BuiltInRegistries.ITEM.containsKey(rl)) { ok = false; break; }
          options.add(rl.toString());
        }
        ResourceLocation chosen = ResourceLocation.tryParse(entry.getValue().trim());
        if (!ok || options.size() < 2 || chosen == null || !options.contains(chosen.toString())) continue;
        java.util.Collections.sort(options);
        choices.put(String.join(",", options), chosen.toString());
        savedChoices++;
      }

      if (savedRecipes + savedPrices + savedChoices > 0) {
        if (savedChoices > 0) config.ingredientChoices.validateAndSet(choices);
        if (savedRecipes > 0) config.itemRecipes.validateAndSet(recipes);
        if (savedPrices > 0) {
          config.itemWhitelist.validateAndSet(items);
          config.fluidWhitelist.validateAndSet(fluids);
        }
        ConfigApi.save(config);
        MinPriceService.rebuild(player.server);
      }
      player.sendSystemMessage(
          Component.translatable("msg.roll_mod.minprice.saved", savedRecipes + savedChoices, savedPrices)
              .withStyle(savedRecipes + savedPrices + savedChoices > 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
    });
  }
}
