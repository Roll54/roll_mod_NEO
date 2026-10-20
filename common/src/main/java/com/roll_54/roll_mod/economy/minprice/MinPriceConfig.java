package com.roll_54.roll_mod.economy.minprice;

import static com.roll_54.roll_mod.RollMod.MODID;

import com.roll_54.roll_mod.data.datagen.ore.OreDefinition;
import com.roll_54.roll_mod.data.datagen.ore.OreDefinitions;
import com.roll_54.roll_mod.data.datagen.ore.OreTextureTemplates.BlockSubLayer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import kotlin.jvm.functions.Function0;
import me.fzzyhmstrs.fzzy_config.annotations.Comment;
import me.fzzyhmstrs.fzzy_config.api.ConfigApi;
import me.fzzyhmstrs.fzzy_config.api.FileType;
import me.fzzyhmstrs.fzzy_config.api.SaveType;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedList;
import me.fzzyhmstrs.fzzy_config.validation.collection.ValidatedStringMap;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedString;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedDouble;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedLong;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Minimal prices: the lowest price an item or fluid may be listed for on the auction or in a shop
 * block. Prices start from raw ores ({@link #tiers} / {@link #oreTiers}) and are carried up through
 * every recipe, see {@link PriceSolver}.
 */
public class MinPriceConfig extends Config {

  public static MinPriceConfig MAIN;

  public MinPriceConfig() {
    super(ResourceLocation.fromNamespaceAndPath(MODID, "min_price"), "", "");
  }

  public static void init() {
    MAIN =
        ConfigApi.registerAndLoadConfig((Function0<? extends MinPriceConfig>) MinPriceConfig::new);
  }

  @Override
  public @NotNull FileType fileType() {
    return FileType.JSON5;
  }

  @Override
  public @NotNull SaveType saveType() {
    return SaveType.SEPARATE;
  }

  @Override
  public int defaultPermLevel() {
    return 4;
  }

  /** An edit from the in-game config screen: the table is built again from the new values. */
  @Override
  public void onUpdateServer(@NotNull ServerPlayer player) {
    MinPriceService.rebuild(player.server);
  }

  @Comment("Master switch. Off: nothing is computed and no listing is checked.")
  public boolean enabled = false;

  @Comment("Refuse listings while the price table is still being built. Off: they pass unchecked.")
  public boolean blockSalesUntilReady = false;

  @Comment("Let shop positions with price 0 (giveaways, donations) through without a check.")
  public boolean allowFreePositions = false;

  @Comment("Price of one RAW ORE piece per tier. Referenced by name from oreTiers.")
  public ValidatedStringMap<Long> tiers =
      new ValidatedStringMap<>(
          ordered(
              "overworld", 100L,
              "nether", 300L,
              "end", 500L,
              "moon", 1_000L,
              "mars", 2_000L,
              "venus", 3_000L,
              "mercury", 4_000L,
              "glacio", 5_000L),
          new ValidatedString(),
          price());

  @Comment("Raw ore item id, or #tag, -> tier name from 'tiers'. Every roll_mod ore is listed at the cheapest dimension it generates in.")
  public ValidatedStringMap<String> oreTiers =
      new ValidatedStringMap<>(defaultOreTiers(), new ValidatedString(), new ValidatedString());

  @Comment("Recipe ids left out of the computation (packing/unpacking loops, test recipes). A trailing * matches a prefix, e.g. 'kubejs:*'.")
  public ValidatedList<String> recipeBlacklist = ValidatedList.ofString(List.of());

  @Comment("Cost multiplier per recipe type id. Placeholder values.")
  public ValidatedStringMap<Double> recipeTypeMultipliers =
      new ValidatedStringMap<>(
          ordered(
              "minecraft:crafting", 1.05,
              "minecraft:smelting", 1.10,
              "minecraft:blasting", 1.08,
              "minecraft:smoking", 1.10,
              "minecraft:campfire_cooking", 1.12,
              "minecraft:stonecutting", 1.02,
              "minecraft:smithing", 1.15,
              "modern_industrialization:macerator", 1.15,
              "modern_industrialization:compressor", 1.10,
              "modern_industrialization:forge_hammer", 1.05,
              "modern_industrialization:electrolyzer", 1.30),
          new ValidatedString(),
          multiplier(1.0));

  @Comment("Multiplier for recipe types not listed in recipeTypeMultipliers.")
  public ValidatedDouble defaultTypeMultiplier = multiplier(1.0);

  @Comment("Ignore recipes whose type is not listed in recipeTypeMultipliers instead of using the default multiplier.")
  public boolean skipUnknownTypes = false;

  @Comment("Item ids or #tags that never have a minimal price. Wins over the whitelist.")
  public ValidatedList<String> itemBlacklist = ValidatedList.ofString(List.of());

  @Comment("Item id -> fixed minimal price. Never computed from recipes.")
  public ValidatedStringMap<Long> itemWhitelist =
      new ValidatedStringMap<>(new LinkedHashMap<>(), new ValidatedString(), price());

  @Comment("Fluid id -> fixed minimal price per bucket (1000 mB).")
  public ValidatedStringMap<Long> fluidWhitelist =
      new ValidatedStringMap<>(
          ordered("minecraft:water", 0L, "minecraft:lava", 5L),
          new ValidatedString(),
          price());

  @Comment("Item id -> share of a recipe's cost per unit when the recipe makes several things. Unlisted items weigh 1; lower it for byproducts.")
  public ValidatedStringMap<Double> outputWeights =
      new ValidatedStringMap<>(new LinkedHashMap<>(), new ValidatedString(), multiplier(1.0));

  /**
   * Vanilla raw ores, then every ore from {@link OreDefinitions}. An ore that generates in several
   * dimensions takes the cheapest of their tiers: a minimal price must not be above the easiest
   * place to mine it.
   */
  private static Map<String, String> defaultOreTiers() {
    Map<String, String> map =
        ordered(
            "minecraft:raw_iron", "overworld",
            "minecraft:raw_copper", "overworld",
            "minecraft:raw_gold", "overworld",
            "minecraft:ancient_debris", "nether",
            "minecraft:quartz", "nether");
    for (OreDefinition ore : OreDefinitions.ORE_DEFINITIONS) {
      BlockSubLayer cheapest = null;
      for (BlockSubLayer base : ore.bases()) {
        if (cheapest == null || TIER_ORDER.indexOf(base) < TIER_ORDER.indexOf(cheapest)) cheapest = base;
      }
      if (cheapest != null) map.put(MODID + ":raw_" + ore.id(), tierOf(cheapest));
    }
    return map;
  }

  /** Ore layers from cheapest to most expensive tier, matching the default 'tiers' prices. */
  private static final List<BlockSubLayer> TIER_ORDER =
      List.of(
          BlockSubLayer.STONE,
          BlockSubLayer.DEEPSLATE,
          BlockSubLayer.NETHERRACK,
          BlockSubLayer.END,
          BlockSubLayer.MOON,
          BlockSubLayer.MARS,
          BlockSubLayer.VENUS,
          BlockSubLayer.MERCURY);

  private static String tierOf(BlockSubLayer base) {
    return switch (base) {
      case STONE, DEEPSLATE -> "overworld";
      case NETHERRACK -> "nether";
      case END -> "end";
      case MOON -> "moon";
      case MARS -> "mars";
      case VENUS -> "venus";
      case MERCURY -> "mercury";
    };
  }

  /** Key, value, key, value... in the order the config file should list them. */
  @SuppressWarnings("unchecked")
  private static <V> Map<String, V> ordered(Object... pairs) {
    Map<String, V> map = new LinkedHashMap<>();
    for (int i = 0; i < pairs.length; i += 2) map.put((String) pairs[i], (V) pairs[i + 1]);
    return map;
  }

  private static ValidatedLong price() {
    return new ValidatedLong(0L, Long.MAX_VALUE / 64, 0L, ValidatedNumber.WidgetType.TEXTBOX);
  }

  private static ValidatedDouble multiplier(double value) {
    return new ValidatedDouble(value, 1000.0, 0.0, ValidatedNumber.WidgetType.TEXTBOX);
  }
}
