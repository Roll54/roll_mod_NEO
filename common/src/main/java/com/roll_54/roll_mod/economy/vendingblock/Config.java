package com.roll_54.roll_mod.economy.vendingblock;

import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;

public class Config {

  public static class Client {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.EnumValue<AnimationMode> ANIMATION_MODE =
        BUILDER
            .comment(
                "Animation mode for vending block items.\n\n'SERVER_DEFAULT' uses the server-side setting, which is 'ROTATION' unless changed by admins.\n")
            .translation("config.roll_mod.animation.mode")
            .defineEnum("clientAnimationMode", AnimationMode.SERVER_DEFAULT);

    public enum AnimationMode {
      SERVER_DEFAULT,
      STILL,
      BOBBING,
      ROTATION,
      BOBBING_ROTATION,
      FACING_PLAYER;
    }

    public static final ModConfigSpec.EnumValue<ScrolltipPosition> SCROLLTIP_POSITION =
        BUILDER
            .comment(
                "Position of the 'scroll' tooltip in the vendor block GUI.\n\n'TOP' shows it at the top of the GUI,\n'ITEM_TOOLTIP' adds the scroll tooltip to the item tooltip.\n")
            .translation("config.roll_mod.scrolltip")
            .defineEnum("scrolltipPosition", ScrolltipPosition.TOP);

    public enum ScrolltipPosition {
      ITEM_TOOLTIP,
      TOP;
    }

    static {
      BUILDER.push("ownerNotifications");
    }

    public static final ModConfigSpec.BooleanValue PURCHASE_MESSAGES =
        BUILDER
            .comment(
                "Whether to receive messages when players purchase items from your vending blocks")
            .translation("config.roll_mod.messages.purchase")
            .define("purchaseMessages", true);

    public static final ModConfigSpec.BooleanValue GIVEAWAY_MESSAGES =
        BUILDER
            .comment(
                "Whether to receive messages when players take free items from your vending blocks")
            .translation("config.roll_mod.messages.giveaway")
            .define("giveawayMessages", true);

    public static final ModConfigSpec.BooleanValue DONATION_MESSAGES =
        BUILDER
            .comment("Whether to receive messages when players donate items to your vending blocks")
            .translation("config.roll_mod.messages.donation")
            .define("donationMessages", true);

    public static final ModConfigSpec.BooleanValue OUT_OF_STOCK_MESSAGES =
        BUILDER
            .comment("Whether to receive messages when your vending blocks run out of stock")
            .translation("config.roll_mod.messages.outOfStock")
            .define("outOfStockMessages", true);

    public static final ModConfigSpec.BooleanValue FULL_STORAGE_MESSAGES =
        BUILDER
            .comment("Whether to receive messages when your vending blocks' storage becomes full")
            .translation("config.roll_mod.messages.fullStorage")
            .define("fullStorageMessages", true);

    static {
      BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();
  }

  public static class Server {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.EnumValue<BreakLevel> BREAK_LEVEL =
        BUILDER
            .comment(
                "Defines who besides the block owner can break the vending block.\n\nSee minecraft.wiki for more information on permission levels.\n")
            .translation("config.roll_mod.server.breakLevel")
            .defineEnum("breakLevel", BreakLevel.GAMEMASTER);

    public enum BreakLevel {
      SERVER_OWNER,
      ADMIN,
      GAMEMASTER,
      MODERATOR,
      BLOCK_OWNER_ONLY;
    }

    public static final ModConfigSpec.EnumValue<AnimationMode> ANIMATION_MODE =
        BUILDER
            .comment(
                "Animation mode for vending block items\nApplies to all users who have selected 'SERVER_DEFAULT' (default) in their client settings.\n")
            .translation("config.roll_mod.animation.mode")
            .defineEnum("animationMode", AnimationMode.ROTATION);

    public enum AnimationMode {
      STILL,
      BOBBING,
      ROTATION,
      BOBBING_ROTATION,
      FACING_PLAYER;
    }

    @SuppressWarnings("deprecation")
    public static final ConfigValue<List<? extends String>> PRODUCT_BLACKLIST =
        BUILDER
            .comment(
                "List of blacklisted facades\n\nExamples:\n- minecraft:oak_wood\n- stone\n- *stained_glass\n- #minecraft:planks\n- #c:glass_blocks\n\n(Server restart required)")
            .translation("config.roll_mod.server.productBlacklist")
            .defineListAllowEmpty(
                "productBlacklist",
                java.util.Arrays.asList("roll_mod:vendor_key"),
                item -> item instanceof String);

    @SuppressWarnings("deprecation")
    public static final ConfigValue<List<? extends String>> FACADE_BLACKLIST =
        BUILDER
            .comment(
                "List of blacklisted facades\n\nExamples:\n- minecraft:oak_wood\n- stone\n- *stained_glass\n- #minecraft:planks\n- #c:glass_blocks\n\n(Server restart required)")
            .translation("config.roll_mod.server.facadeBlacklist")
            .defineListAllowEmpty(
                "facadeBlacklist",
                java.util.Arrays.asList(
                    "*copper_grate",
                    "#c:bookshelves",
                    "#c:glass_blocks",
                    "#minecraft:leaves",
                    "beacon",
                    "cobweb",
                    "glow_lichen",
                    "mangrove_roots",
                    "*piston",
                    "sculk_vein",
                    "vine"),
                item -> item instanceof String);

    public static final ModConfigSpec.BooleanValue VENDOR_KEY_IN_CREATIVE =
        BUILDER
            .comment(
                "Whether the vendor key should appear in the creative mode tab (Server restart required)")
            .translation("config.roll_mod.creative.keyInTab")
            .define("vendorKeyInCreative", true);

    public static final ModConfigSpec.BooleanValue AE2_BUS_SLEEP_ON_VENDOR =
        BUILDER
            .comment(
                "Whether AE2 import/export buses (addon buses included) facing a vending block sleep while it has nothing\nthey can take, or no room for anything they export, instead of polling it every few ticks.\nThe vending block wakes them whenever its storage, positions or mode change.")
            .translation("config.roll_mod.ae2.busSleep")
            .define("ae2BusSleepOnVendor", true);

    static {
      BUILDER.push("auctionHouse");
    }

    public static final ModConfigSpec.IntValue AH_MAX_DAYS =
        BUILDER
            .comment(
                "Maximum number of days a player may list an item on the Auction House (default for non-prime players)")
            .translation("config.roll_mod.ah.maxDays")
            .defineInRange("ahMaxDays", 10, 1, 365);

    public static final ModConfigSpec.IntValue AH_PRIME_DAYS =
        BUILDER
            .comment(
                "Maximum listing duration (days) granted to players with the LuckPerms 'rollmod.ah.prime' permission")
            .translation("config.roll_mod.ah.primeDays")
            .defineInRange("ahPrimeDays", 30, 1, 365);

    public static final ModConfigSpec.IntValue AH_MAX_LISTINGS =
        BUILDER
            .comment(
                "Default maximum number of active listings per player.\nOverridden per-player by the LuckPerms meta 'rollmod.ah.positions' when present.")
            .translation("config.roll_mod.ah.maxListings")
            .defineInRange("ahMaxListings", 5, 1, 1000);

    public static final ModConfigSpec.BooleanValue AH_SELLER_FEE_ENABLED =
        BUILDER
            .comment(
                "Whether the Auction House keeps a cut of every sale (fixed price, buyout and won auctions).\nVending blocks never charge it.")
            .translation("config.roll_mod.ah.sellerFeeEnabled")
            .define("ahSellerFeeEnabled", true);

    // Deliberately not range-checked: whatever is written here is used as is.
    public static final ModConfigSpec.ConfigValue<Double> AH_SELLER_FEE =
        BUILDER
            .comment(
                "Fraction of the sale price the Auction House keeps when ahSellerFeeEnabled is true.\n0.10 = 10%: an item sold for 1000 pays the seller 900. Not validated.")
            .translation("config.roll_mod.ah.sellerFee")
            .define("ahSellerFee", 0.10);

    public static final ModConfigSpec.LongValue AH_MIN_BID_INCREMENT =
        BUILDER
            .comment("Minimum amount a new bid must exceed the current highest bid by")
            .translation("config.roll_mod.ah.minBidIncrement")
            .defineInRange("ahMinBidIncrement", 1L, 1L, Long.MAX_VALUE);

    static {
      BUILDER.pop();
      BUILDER.push("serviceBlock");
    }

    public static final ModConfigSpec.LongValue SERVICE_ENERGY_PER_TIER_EU =
        BUILDER
            .comment(
                "Energy buffer of a charging or discharging service block, per EU/t of the machine hull placed in it.\nThe default 100000 gives LV (32 EU/t) 3.2M EU, MV 12.8M, HV 102.4M, EV 819.2M.")
            .translation("config.roll_mod.service.energyPerTierEu")
            .defineInRange("serviceEnergyPerTierEu", 100_000L, 1L, Long.MAX_VALUE);

    public static final ModConfigSpec.IntValue SERVICE_FLUID_CAPACITY =
        BUILDER
            .comment("Fluid (mB) a filling or draining service block can store")
            .translation("config.roll_mod.service.fluidCapacity")
            .defineInRange("serviceFluidCapacity", 256_000, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue SERVICE_XP_CAPACITY =
        BUILDER
            .comment("Liquid experience (mB, 20 mB = 1 XP point) an XP service block can store")
            .translation("config.roll_mod.service.xpCapacity")
            .defineInRange("serviceXpCapacity", 1_000_000, 20, Integer.MAX_VALUE);

    public static final ModConfigSpec.LongValue SERVICE_ENERGY_UNIT =
        BUILDER
            .comment("EU per priced unit of a charging or discharging service")
            .translation("config.roll_mod.service.energyUnit")
            .defineInRange("serviceEnergyUnit", 100_000L, 1L, Long.MAX_VALUE);

    public static final ModConfigSpec.IntValue SERVICE_XP_UNIT =
        BUILDER
            .comment("XP points per priced unit of an XP service")
            .translation("config.roll_mod.service.xpUnit")
            .defineInRange("serviceXpUnit", 100, 1, Integer.MAX_VALUE);

    public static final ModConfigSpec.IntValue SERVICE_FLUID_UNIT =
        BUILDER
            .comment("mB per priced unit of a filling or draining service")
            .translation("config.roll_mod.service.fluidUnit")
            .defineInRange("serviceFluidUnit", 1000, 1, Integer.MAX_VALUE);

    static {
      BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();
  }
}
