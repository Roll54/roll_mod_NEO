package com.roll_54.roll_mod.economy.service;

import aztech.modern_industrialization.api.energy.CableTier;
import aztech.modern_industrialization.api.energy.MIEnergyStorage;
import com.roll_54.roll_mod.economy.vendingblock.blockentity.OwnedBlockEntity;
import com.roll_54.roll_mod.economy.vendingblock.registry.BlockEntityRegistry;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * A service block's state: who owns it, which {@link ServiceType} it offers (none until the owner
 * picks one), the price per unit, and the stock it trades from.
 *
 * <p>Stock is one EU buffer for the energy services and one tank for the others, filled and emptied
 * by the owner's cables and pipes from any side. The EU buffer comes from the Modern Industrialization
 * machine hull the owner puts in it: the hull's tier sets its size and which cables connect, as on an
 * MI machine. No hull, no buffer. An <em>unlimited</em> block (set with the vendor key)
 * ignores its stock: it makes what it sells and voids what it buys.
 *
 * <p>Clients get the whole state through the block-entity update packet, which the screens read every
 * tick. Cables change the buffer every tick, so that packet is sent at most every
 * {@link #SYNC_INTERVAL} ticks rather than on every change.
 */
public class ServiceBlockEntity extends BlockEntity implements OwnedBlockEntity {

  private static final int SYNC_INTERVAL = 10;

  private UUID ownerID;
  private String ownerUser;
  @Nullable private ServiceType type;
  private long price;
  /** Size of one priced batch, in EU, XP points or mB; 0 until the owner sets one. */
  private long batch;
  private boolean unlimited;
  /** Charging only: each use moves exactly one batch for exactly the price, instead of filling the item. */
  private boolean fixedBatch;

  private long energy;
  private final EnergyPort energyPort = new EnergyPort();
  private final HullSlot hull = new HullSlot();
  private final ServiceTank tank = new ServiceTank();

  private boolean syncPending;

  public ServiceBlockEntity(BlockPos pos, BlockState state) {
    super(BlockEntityRegistry.SERVICE_BE.get(), pos, state);
  }

  /* ------------------------------------------- ownership ------------------------------------------- */

  public void setOwner(Player player) {
    this.ownerID = player.getUUID();
    this.ownerUser = player.getName().getString();
    changed();
  }

  @Override
  public UUID getOwnerID() {
    return ownerID;
  }

  @Override
  public String getOwnerUser() {
    return ownerUser;
  }

  @Override
  public boolean isOwner(Player player) {
    if (ownerID != null && ownerID.equals(player.getUUID())) return true;
    if (ownerUser != null && ownerUser.equals(player.getName().getString())) {
      ownerID = player.getUUID();
      changed();
      return true;
    }
    return false;
  }

  /* -------------------------------------------- service -------------------------------------------- */

  @Nullable
  public ServiceType getServiceType() {
    return type;
  }

  /** Sets the service once; a block that already offers one keeps it. */
  public boolean chooseService(ServiceType chosen) {
    if (type != null) return false;
    type = chosen;
    tank.setCapacity(chosen.resource() == ServiceType.Resource.XP
        ? ServiceType.xpCapacity()
        : ServiceType.fluidCapacity());
    if (level != null && !level.isClientSide()) level.invalidateCapabilities(worldPosition);
    syncNow();
    return true;
  }

  public long getPrice() {
    return price;
  }

  public void setPrice(long price) {
    this.price = Math.max(0L, price);
    syncNow();
  }

  /** The owner's batch size, or the server's default for this service while none is set. */
  public long getBatch() {
    return batch > 0 || type == null ? Math.max(1L, batch) : type.defaultBatch();
  }

  public void setBatch(long batch) {
    this.batch = Math.max(1L, batch);
    syncNow();
  }

  /** {@link #getBatch()} with its measure, e.g. "100K EU", "100 XP", "1K mB". */
  public net.minecraft.network.chat.Component batchText() {
    return type == null ? net.minecraft.network.chat.Component.empty() : type.amountText(getBatch());
  }

  /**
   * Sends this block's current state to {@code player} straight away. A screen is built from the
   * client's copy on the client and from this one on the server, element for element; the regular
   * update only leaves at the end of the tick, so a screen opened before it would be built from a
   * stale copy and not match the server's.
   */
  public void sendTo(net.minecraft.server.level.ServerPlayer player) {
    var packet = getUpdatePacket();
    if (packet != null) player.connection.send(packet);
  }

  public boolean isUnlimited() {
    return unlimited;
  }

  public void setUnlimited(boolean unlimited) {
    this.unlimited = unlimited;
    syncNow();
  }

  /** Whether the mode can be switched: charging only, the other services always move "as much as fits". */
  public boolean offersFixedBatch() {
    return type == ServiceType.CHARGE;
  }

  /**
   * True when each use moves exactly {@link #getBatch()} for exactly {@link #getPrice()}; false (the
   * default) when it fills the item and the price is per batch, rounded up.
   */
  public boolean isFixedBatch() {
    return fixedBatch && offersFixedBatch();
  }

  public void setFixedBatch(boolean fixedBatch) {
    this.fixedBatch = fixedBatch;
    syncNow();
  }

  /* --------------------------------------------- stock --------------------------------------------- */

  public long getEnergy() {
    return energy;
  }

  /** The hull's tier times {@code serviceEnergyPerTierEu}; 0 without a hull. */
  public long getEnergyCapacity() {
    CableTier tier = hullTier();
    if (tier == null) return 0L;
    long perEu = ServiceType.energyPerTierEu();
    // Saturating: a superconductor hull with a large multiplier must not wrap negative.
    return tier.eu > Long.MAX_VALUE / perEu ? Long.MAX_VALUE : tier.eu * perEu;
  }

  /** The tier of the hull in the hull slot, or null when it is empty. */
  @Nullable
  public CableTier hullTier() {
    return tierOf(hull.getStackInSlot(0));
  }

  /** The MI cable tier whose machine hull {@code stack} is, or null for anything else. */
  @Nullable
  public static CableTier tierOf(ItemStack stack) {
    if (stack.isEmpty()) return null;
    ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
    for (CableTier tier : CableTier.allTiers()) {
      if (id.equals(tier.itemKey)) return tier;
    }
    return null;
  }

  /** The hull slot the owner's screen shows. */
  public ItemStackHandler getHullSlot() {
    return hull;
  }

  /** Drops the hull, for when the block is broken. Its buffer goes with the block. */
  public void dropContents() {
    if (level == null) return;
    ItemStack stack = hull.getStackInSlot(0);
    if (!stack.isEmpty()) {
      Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack.copy());
      hull.setStackInSlot(0, ItemStack.EMPTY);
    }
  }

  /** Moves {@code delta} EU in (positive) or out (negative) of the buffer, clamped to it. */
  void addEnergy(long delta) {
    energy = Math.max(0L, Math.min(getEnergyCapacity(), energy + delta));
    changed();
  }

  public FluidTank getTank() {
    return tank;
  }

  /** The XP fluid, or {@link Fluids#EMPTY} when Sophisticated Core is not installed. */
  public static Fluid xpFluid() {
    return BuiltInRegistries.FLUID.get(ServiceType.XP_FLUID);
  }

  /** XP points the tank holds. */
  public long xpPoints() {
    return tank.getFluidAmount() / ServiceType.MB_PER_XP_POINT;
  }

  /** The energy face for cables, or null when this block does not trade energy. */
  @Nullable
  public MIEnergyStorage energyCapability() {
    return type != null && type.resource() == ServiceType.Resource.ENERGY ? energyPort : null;
  }

  /** The fluid face for pipes, or null when this block does not trade fluid or XP. */
  @Nullable
  public IFluidHandler fluidCapability() {
    return type != null && type.resource() != ServiceType.Resource.ENERGY ? tank : null;
  }

  /* ---------------------------------------------- sync --------------------------------------------- */

  /** Marks the state dirty; clients get it with the next throttled update. */
  private void changed() {
    setChanged();
    syncPending = true;
  }

  /** For changes a screen must show at once: the service, the price, unlimited. */
  private void syncNow() {
    setChanged();
    syncPending = false;
    if (level != null && !level.isClientSide()) {
      level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
  }

  public static void serverTick(Level level, BlockPos pos, BlockState state, ServiceBlockEntity be) {
    if (be.syncPending && level.getGameTime() % SYNC_INTERVAL == 0) be.syncNow();
  }

  @Override
  protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    super.saveAdditional(tag, registries);
    if (ownerID != null) tag.putUUID("ownerID", ownerID);
    if (ownerUser != null) tag.putString("ownerUser", ownerUser);
    if (type != null) tag.putString("service", type.id());
    tag.putLong("price", price);
    tag.putLong("batch", batch);
    tag.putBoolean("unlimited", unlimited);
    tag.putBoolean("fixedBatch", fixedBatch);
    tag.putLong("energy", energy);
    tag.put("hull", hull.serializeNBT(registries));
    tag.put("tank", tank.writeToNBT(registries, new CompoundTag()));
  }

  @Override
  protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    super.loadAdditional(tag, registries);
    ownerID = tag.hasUUID("ownerID") ? tag.getUUID("ownerID") : null;
    ownerUser = tag.contains("ownerUser") ? tag.getString("ownerUser") : null;
    type = tag.contains("service") ? ServiceType.byId(tag.getString("service")) : null;
    price = tag.getLong("price");
    batch = tag.getLong("batch");
    unlimited = tag.getBoolean("unlimited");
    fixedBatch = tag.getBoolean("fixedBatch");
    if (tag.contains("hull")) hull.deserializeNBT(registries, tag.getCompound("hull"));
    energy = tag.getLong("energy");
    if (type != null) {
      tank.setCapacity(type.resource() == ServiceType.Resource.XP
          ? ServiceType.xpCapacity()
          : ServiceType.fluidCapacity());
    }
    tank.readFromNBT(registries, tag.getCompound("tank"));
  }

  @Override
  public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
    return saveWithoutMetadata(registries);
  }

  @Nullable
  @Override
  public Packet<ClientGamePacketListener> getUpdatePacket() {
    return ClientboundBlockEntityDataPacket.create(this);
  }

  /* ------------------------------------------- handlers -------------------------------------------- */

  /**
   * The tank pipes and the service use. Locked to the XP fluid for the XP services; any one fluid
   * for filling and draining. Holds nothing before a service is picked.
   */
  private final class ServiceTank extends FluidTank {

    ServiceTank() {
      super(0);
    }

    @Override
    public boolean isFluidValid(FluidStack stack) {
      if (type == null || type.resource() == ServiceType.Resource.ENERGY) return false;
      if (type.resource() == ServiceType.Resource.XP) return stack.is(xpFluid()) && xpFluid() != Fluids.EMPTY;
      return true;
    }

    @Override
    protected void onContentsChanged() {
      changed();
    }
  }

  /**
   * The hull slot: one MI machine hull. It can only be taken out while the buffer is empty, so a hull
   * cannot be swapped for a smaller one to strand the energy it holds; to change tier, drain the block
   * first.
   */
  private final class HullSlot extends ItemStackHandler {

    HullSlot() {
      super(1);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
      return tierOf(stack) != null;
    }

    @Override
    public int getSlotLimit(int slot) {
      return 1;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
      return energy > 0 ? ItemStack.EMPTY : super.extractItem(slot, amount, simulate);
    }

    @Override
    protected void onContentsChanged(int slot) {
      // A new tier means other cables connect, so MI has to look the port up again.
      if (level != null && !level.isClientSide()) level.invalidateCapabilities(worldPosition);
      syncNow();
    }
  }

  /**
   * The EU buffer as MI sees it: cables of the hull's tier, in and out from every side, at most that
   * tier's transfer per call. A {@code long} buffer rather than {@code AnyTierEnergyStore}, which stops
   * at {@link Integer#MAX_VALUE}.
   */
  private final class EnergyPort implements MIEnergyStorage {

    private long maxTransfer() {
      CableTier tier = hullTier();
      return tier == null ? 0L : tier.getMaxTransfer();
    }

    @Override
    public long receive(long maxReceive, boolean simulate) {
      long accepted = Math.max(0L, Math.min(Math.min(maxReceive, maxTransfer()), getEnergyCapacity() - energy));
      if (!simulate && accepted > 0) addEnergy(accepted);
      return accepted;
    }

    @Override
    public long extract(long maxExtract, boolean simulate) {
      long given = Math.max(0L, Math.min(Math.min(maxExtract, maxTransfer()), energy));
      if (!simulate && given > 0) addEnergy(-given);
      return given;
    }

    @Override
    public long getAmount() {
      return energy;
    }

    @Override
    public long getCapacity() {
      return getEnergyCapacity();
    }

    @Override
    public boolean canConnect(CableTier cableTier) {
      return cableTier == hullTier();
    }

    @Override
    public boolean canReceive() {
      return true;
    }

    @Override
    public boolean canExtract() {
      return true;
    }
  }
}
