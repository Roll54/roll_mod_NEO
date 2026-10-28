package com.roll_54.roll_mod.economy.service;

import aztech.modern_industrialization.blocks.storage.tank.TankItem;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.economy.util.EnergyFormatUtils;
import com.roll_54.roll_mod.economy.vendingblock.Config;
import com.roll_54.roll_mod.registry.ItemRegistry;
import dev.technici4n.grandpower.api.ILongEnergyStorage;
import java.util.Locale;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * What a service block offers. Picked once by the owner from the list on first open, then fixed: the
 * block has to be broken and placed again to offer something else.
 *
 * <p>Stored by {@link #id()}, so entries may be added but never renamed.
 */
public enum ServiceType {
  /** Sells energy: charges the customer's item from the block's buffer. */
  CHARGE(Resource.ENERGY, true),
  /** Buys energy: empties the customer's item into the block's buffer. */
  DISCHARGE(Resource.ENERGY, false),
  /** Sells experience out of the block's liquid XP. */
  SELL_XP(Resource.XP, true),
  /** Buys the customer's experience into the block's liquid XP. */
  BUY_XP(Resource.XP, false),
  /** Sells fluid: fills the customer's tank from the block's tank. */
  FILL(Resource.FLUID, true),
  /** Buys fluid: empties the customer's tank into the block's tank. */
  DRAIN(Resource.FLUID, false);

  /** What the block stores and trades in. */
  public enum Resource {
    ENERGY,
    XP,
    FLUID
  }

  /** {@code sophisticatedcore:xp_still}; 20 mB is one XP point, Sophisticated Core's own ratio. */
  public static final ResourceLocation XP_FLUID = ResourceLocation.parse("sophisticatedcore:xp_still");
  public static final int MB_PER_XP_POINT = 20;

  private final Resource resource;
  private final boolean sells;

  ServiceType(Resource resource, boolean sells) {
    this.resource = resource;
    this.sells = sells;
  }

  public Resource resource() {
    return resource;
  }

  /** True when the customer pays; false for buy-back, where the owner pays the customer. */
  public boolean sells() {
    return sells;
  }

  /** Whether the customer puts an item in the work slot; XP comes straight from the player. */
  public boolean usesSlot() {
    return resource != Resource.XP;
  }

  /**
   * Whether the customer may put {@code stack} in the work slot. Energy services take anything with an
   * energy storage (GrandPower or NeoForge); fluid services take only Modern Industrialization tanks and
   * Ad Astra's oxygen containers (space suits, gas tanks).
   */
  public boolean accepts(ItemStack stack) {
    if (stack.isEmpty()) return false;
    return switch (resource) {
      case ENERGY -> stack.getCapability(ILongEnergyStorage.ITEM) != null
          || stack.getCapability(Capabilities.EnergyStorage.ITEM) != null;
      case FLUID -> (stack.getItem() instanceof TankItem || isAdAstraOxygen(stack))
          && stack.copyWithCount(1).getCapability(Capabilities.FluidHandler.ITEM) != null;
      case XP -> false;
    };
  }

  /**
   * Ad Astra's space suits (and the suits extending them) and gas tanks, matched by class name so this
   * loads without Ad Astra installed. Suit pieces with no tank have no fluid storage and fail the
   * capability check in {@link #accepts}.
   */
  private static boolean isAdAstraOxygen(ItemStack stack) {
    for (Class<?> c = stack.getItem().getClass(); c != null; c = c.getSuperclass()) {
      String name = c.getName();
      if (name.equals("earth.terrarium.adastra.common.items.armor.SpaceSuitItem")
          || name.equals("earth.terrarium.adastra.common.items.GasTankItem")) {
        return true;
      }
    }
    return false;
  }

  public String id() {
    return name().toLowerCase(Locale.ROOT);
  }

  @Nullable
  public static ServiceType byId(String id) {
    for (ServiceType type : values()) {
      if (type.id().equals(id)) return type;
    }
    return null;
  }

  public Component title() {
    return Component.translatable("gui.roll_mod.service.type." + id());
  }

  public Component description() {
    return Component.translatable("gui.roll_mod.service.type." + id() + ".desc");
  }

  /** Caption of the customer's action button. */
  public Component action() {
    return Component.translatable("gui.roll_mod.service.action." + id());
  }

  /**
   * The service's own icon over the middle of the screen, or null where it uses {@link #icon()}'s item
   * instead: blue lightning for energy, a blue drop for fluid.
   */
  @Nullable
  public ResourceLocation iconTexture() {
    return switch (resource) {
      case ENERGY -> RollMod.id("textures/gui/container/blue_lightning.png");
      case FLUID -> RollMod.id("textures/gui/container/blue_drop.png");
      case XP -> null;
    };
  }

  /** The icon on the block's front: {@link #iconTexture()}, or the experience bottle's texture for XP. */
  public ResourceLocation blockIconTexture() {
    ResourceLocation texture = iconTexture();
    return texture != null ? texture : ResourceLocation.withDefaultNamespace("textures/item/experience_bottle.png");
  }

  /** The item drawn over the middle of the screen when {@link #iconTexture()} is null. */
  public ItemStack icon() {
    return switch (resource) {
      case ENERGY -> new ItemStack(ItemRegistry.REDSTONE_BATTERY.get());
      case XP -> new ItemStack(Items.EXPERIENCE_BOTTLE);
      case FLUID -> item("modern_industrialization:aluminum_tank", Items.BUCKET);
    };
  }

  private static ItemStack item(String id, net.minecraft.world.item.Item fallback) {
    var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
    return new ItemStack(item == Items.AIR ? fallback : item);
  }

  /** The server's default batch size: EU, XP points or mB, until the owner sets their own. */
  public long defaultBatch() {
    return switch (resource) {
      case ENERGY -> read(Config.Server.SERVICE_ENERGY_UNIT::get, 100_000L);
      case XP -> read(() -> (long) Config.Server.SERVICE_XP_UNIT.get(), 100L);
      case FLUID -> read(() -> (long) Config.Server.SERVICE_FLUID_UNIT.get(), 1000L);
    };
  }

  /** {@code amount} with this service's measure, e.g. "100K EU", "100 XP", "1K mB". */
  public Component amountText(long amount) {
    return Component.translatable(
        "gui.roll_mod.service.unit." + resource.name().toLowerCase(Locale.ROOT),
        EnergyFormatUtils.formatEnergy(amount));
  }

  /** Buffer EU per EU/t of the hull's tier. */
  static long energyPerTierEu() {
    return read(Config.Server.SERVICE_ENERGY_PER_TIER_EU::get, 100_000L);
  }

  static int fluidCapacity() {
    return (int) read(() -> (long) Config.Server.SERVICE_FLUID_CAPACITY.get(), 256_000L);
  }

  static int xpCapacity() {
    return (int) read(() -> (long) Config.Server.SERVICE_XP_CAPACITY.get(), 1_000_000L);
  }

  /**
   * A server config value, or {@code fallback} while the config is not loaded — a client that has
   * not joined a world yet, where the block entity and its screen are still built.
   */
  private static long read(Supplier<Long> value, long fallback) {
    try {
      return value.get();
    } catch (IllegalStateException e) {
      return fallback;
    }
  }
}
