package com.roll_54.roll_mod.economy.vendingblock.blockentity;

import appeng.api.config.Actionable;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.util.AECableType;
import appeng.helpers.InterfaceLogic;
import appeng.helpers.InterfaceLogicHost;
import com.roll_54.roll_mod.compat.ae2.Ae2BusWaker;
import com.roll_54.roll_mod.economy.vendingblock.registry.BlockEntityRegistry;
import com.roll_54.roll_mod.economy.vendingblock.registry.BlockRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * The vending block. Its inventory is an ME interface: AE2's own {@link InterfaceLogic}, with the
 * interface's 9 config slots, 9 storage slots, upgrade slot (crafting or fuzzy card), priority and
 * fuzzy mode. Cabled to a network it stocks the configured items, crafts them with a crafting card,
 * and pushes everything not configured into the network — goods bought from players included —
 * exactly like an interface. Purchases are paid out of that storage.
 */
public class VendorBlockEntity extends BlockEntity
    implements OwnedBlockEntity, InterfaceLogicHost, IInWorldGridNodeHost, IActionHost {

  public static final int MAX_POSITIONS = 10;

  /** ME link state for the owner's screen and Jade; the client has no grid node to ask. */
  public static final int ME_OFFLINE = 0;
  public static final int ME_NO_CHANNEL = 1;
  public static final int ME_ONLINE = 2;

  private static final String TAG_INTERFACE = "interface";
  private static final String TAG_ME_STATUS = "meStatus";

  private UUID ownerID;
  private String ownerUser;
  public boolean infiniteInventory = false;
  public boolean discardsPayment = false;
  public boolean buyMode = false; // false = sell to players, true = buy from players
  public boolean hasError = false;
  public int errorCode = 0; // 0 = no error, 1 = no stock, 2 = storage full, 3 = not set

  private final List<Position> positions = new ArrayList<>();
  private ItemStack facade = ItemStack.EMPTY;

  private static final IGridNodeListener<VendorBlockEntity> NODE_LISTENER =
      new IGridNodeListener<>() {
        @Override
        public void onSaveChanges(VendorBlockEntity be, IGridNode node) {
          be.setChanged();
        }

        @Override
        public void onGridChanged(VendorBlockEntity be, IGridNode node) {
          be.logic.gridChanged();
        }

        @Override
        public void onStateChanged(VendorBlockEntity be, IGridNode node, State state) {
          if (be.mainNode.hasGridBooted()) be.logic.notifyNeighbors();
          be.updateMeStatus();
        }
      };

  private final IManagedGridNode mainNode =
      GridHelper.createManagedNode(this, NODE_LISTENER)
          .setInWorldNode(true)
          .setTagName("proxy")
          .setVisualRepresentation(BlockRegistry.VENDOR.get());

  // After the node: the logic adds its ticker and crafting-requester services to it.
  private final InterfaceLogic logic =
      new InterfaceLogic(mainNode, this, BlockRegistry.VENDOR.get().asItem());

  private int meStatus = ME_OFFLINE;

  public VendorBlockEntity(BlockPos pos, BlockState state) {
    super(BlockEntityRegistry.VENDOR_BE.get(), pos, state);
  }

  private void sync() {
    setChanged();
    if (level != null && !level.isClientSide()) {
      level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
      level.invalidateCapabilities(getBlockPos());
      checkErrorState();
      wakeAe2Buses();
    }
  }

  /** AE2 buses facing this block sleep while it can give or take nothing; any change may undo that. */
  private void wakeAe2Buses() {
    Ae2BusWaker.wake(level, getBlockPos());
  }

  // --- ME interface ----------------------------------------------------

  @Override
  public InterfaceLogic getInterfaceLogic() {
    return logic;
  }

  @Override
  public BlockEntity getBlockEntity() {
    return this;
  }

  /** The interface's config, storage, cards, priority or fuzzy mode changed. */
  @Override
  public void saveChanges() {
    sync();
  }

  @Override
  public ItemStack getMainMenuIcon() {
    return new ItemStack(BlockRegistry.VENDOR.get());
  }

  public IManagedGridNode getMainNode() {
    return mainNode;
  }

  @Override
  public @Nullable IGridNode getGridNode(Direction dir) {
    return mainNode.getNode();
  }

  @Override
  public @Nullable IGridNode getActionableNode() {
    return mainNode.getNode();
  }

  @Override
  public AECableType getCableConnectionType(Direction dir) {
    return logic.getCableConnectionType(dir);
  }

  /** {@link #ME_OFFLINE}, {@link #ME_NO_CHANNEL} or {@link #ME_ONLINE}; synced to the client. */
  public int getMeStatus() {
    return meStatus;
  }

  private void updateMeStatus() {
    int status;
    if (mainNode.isActive()) status = ME_ONLINE;
    else if (mainNode.isPowered()) status = ME_NO_CHANNEL;
    else status = ME_OFFLINE;
    if (status != meStatus) {
      meStatus = status;
      if (level != null && !level.isClientSide()) {
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
      }
    }
  }

  @Override
  public void clearRemoved() {
    super.clearRemoved();
    // Client side this does nothing; server side the node joins on the first tick.
    GridHelper.onFirstTick(this, be -> be.mainNode.create(be.getLevel(), be.getBlockPos()));
  }

  @Override
  public void onChunkUnloaded() {
    super.onChunkUnloaded();
    mainNode.destroy();
  }

  @Override
  public void setRemoved() {
    super.setRemoved();
    mainNode.destroy();
    // A bus asleep on this block would otherwise stay asleep against whatever replaces it.
    if (level != null && !level.isClientSide()) wakeAe2Buses();
  }

  // --- stock -------------------------------------------------------------

  /** Units of {@code item} (same item and components) in the interface storage. */
  public int stockOf(ItemStack item) {
    AEItemKey key = AEItemKey.of(item);
    if (key == null) return 0;
    long total = 0;
    var storage = logic.getStorage();
    for (int slot = 0; slot < storage.size(); slot++) {
      if (key.equals(storage.getKey(slot))) total += storage.getAmount(slot);
    }
    return (int) Math.min(Integer.MAX_VALUE, total);
  }

  /** Removes up to {@code amount} of {@code item}; returns how many were removed. */
  public int takeStock(ItemStack item, int amount) {
    AEItemKey key = AEItemKey.of(item);
    if (key == null || amount <= 0) return 0;
    int removed = 0;
    var storage = logic.getStorage();
    for (int slot = 0; slot < storage.size() && removed < amount; slot++) {
      removed += (int) storage.extract(slot, key, amount - removed, Actionable.MODULATE);
    }
    return removed;
  }

  /** Puts up to {@code amount} of {@code item} into the interface storage; returns how many fit. */
  public int storeGoods(ItemStack item, int amount, boolean simulate) {
    AEItemKey key = AEItemKey.of(item);
    if (key == null || amount <= 0) return 0;
    Actionable mode = simulate ? Actionable.SIMULATE : Actionable.MODULATE;
    var storage = logic.getStorage();
    long stored = 0;
    for (int slot = 0; slot < storage.size() && stored < amount; slot++) {
      stored += storage.insert(slot, key, amount - stored, mode);
    }
    return (int) stored;
  }

  // --- positions -------------------------------------------------------

  public List<Position> getPositions() {
    return positions;
  }

  public Position getPosition(int index) {
    return (index >= 0 && index < positions.size()) ? positions.get(index) : null;
  }

  public boolean addPosition(Position position) {
    if (positions.size() >= MAX_POSITIONS || position.item().isEmpty()) return false;
    positions.add(
        new Position(position.item().copyWithCount(1), position.amount(), position.price()));
    sync();
    return true;
  }

  public void removePosition(int index) {
    if (index >= 0 && index < positions.size()) {
      positions.remove(index);
      sync();
    }
  }

  public int stockFor(Position position) {
    return stockOf(position.item());
  }

  public long purchasesLeft(Position position) {
    if (infiniteInventory) return Long.MAX_VALUE;
    if (position.amount() <= 0) return 0;
    return (long) stockOf(position.item()) / position.amount();
  }

  public boolean isPositionItem(ItemStack stack) {
    for (Position p : positions) {
      if (!p.item().isEmpty() && ItemStack.isSameItemSameComponents(stack, p.item())) return true;
    }
    return false;
  }

  /**
   * Whether {@code amount} units of {@code item} can be inserted into storage (used by buy mode).
   */
  public boolean canStore(ItemStack item, int amount) {
    if (item.isEmpty() || amount <= 0) return false;
    return storeGoods(item, amount, true) >= amount;
  }

  // --- facade ----------------------------------------------------------

  public ItemStack getFacade() {
    return facade;
  }

  public void setFacade(ItemStack stack) {
    this.facade = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
    sync();
  }

  // --- capabilities ----------------------------------------------------

  /**
   * The interface storage as items. Hoppers and pipes insert through {@link #getInsertItemHandler()}
   * and take out of the bottom face through {@link #getExtractItemHandler()}.
   */
  private ItemStack stackInSlot(int slot) {
    GenericStack stack = logic.getStorage().getStack(slot);
    return stack != null && stack.what() instanceof AEItemKey key
        ? key.toStack((int) Math.min(Integer.MAX_VALUE, stack.amount()))
        : ItemStack.EMPTY;
  }

  private int slotCount() {
    return logic.getStorage().size();
  }

  private final IItemHandler insertItemHandler =
      new IItemHandler() {
        @Override
        public int getSlots() {
          return slotCount();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
          return stackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
          var storage = logic.getStorage();
          AEItemKey key = AEItemKey.of(stack);
          if (slot >= storage.size() || key == null) return stack;
          long inserted =
              storage.insert(
                  slot,
                  key,
                  stack.getCount(),
                  simulate ? Actionable.SIMULATE : Actionable.MODULATE);
          return inserted <= 0 ? stack : stack.copyWithCount(stack.getCount() - (int) inserted);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
          return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
          return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
          AEItemKey key = AEItemKey.of(stack);
          var storage = logic.getStorage();
          return key != null && slot < storage.size() && storage.isAllowedIn(slot, key);
        }
      };

  public IItemHandler getInsertItemHandler() {
    return insertItemHandler;
  }

  private final IItemHandler extractItemHandler =
      new IItemHandler() {
        @Override
        public int getSlots() {
          return slotCount();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
          return stackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
          return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
          ItemStack inSlot = stackInSlot(slot);
          if (inSlot.isEmpty() || amount <= 0) return ItemStack.EMPTY;
          // In sell mode, don't pipe out sale stock. In buy mode, position items ARE the
          // bought goods the owner collects, so allow extracting them.
          if (!buyMode && isPositionItem(inSlot)) return ItemStack.EMPTY;
          var storage = logic.getStorage();
          AEItemKey key = AEItemKey.of(inSlot);
          int toTake = Math.min(amount, inSlot.getMaxStackSize());
          long taken =
              storage.extract(
                  slot, key, toTake, simulate ? Actionable.SIMULATE : Actionable.MODULATE);
          return taken <= 0 ? ItemStack.EMPTY : inSlot.copyWithCount((int) taken);
        }

        @Override
        public int getSlotLimit(int slot) {
          return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
          return false;
        }
      };

  public IItemHandler getExtractItemHandler() {
    return extractItemHandler;
  }

  private final IItemHandler emptyItemHandler =
      new IItemHandler() {
        @Override
        public int getSlots() {
          return 0;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
          return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
          return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
          return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
          return 0;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
          return false;
        }
      };

  public IItemHandler getPublicItemHandler() {
    return emptyItemHandler;
  }

  public void drops() {
    if (level == null) return;
    // The interface's storage and its card.
    List<ItemStack> stacks = new ArrayList<>();
    logic.addDrops(stacks);
    for (ItemStack stack : stacks) {
      if (stack.isEmpty()) continue;
      int remaining = stack.getCount();
      int max = Math.max(1, stack.getMaxStackSize());
      while (remaining > 0) {
        int chunk = Math.min(remaining, max);
        Containers.dropItemStack(
            level,
            worldPosition.getX(),
            worldPosition.getY(),
            worldPosition.getZ(),
            stack.copyWithCount(chunk));
        remaining -= chunk;
      }
    }
  }

  // --- ownership -------------------------------------------------------

  public void setOwner(Player player) {
    this.ownerID = player.getUUID();
    this.ownerUser = player.getName().getString();
    mainNode.setOwningPlayer(player);
    setChanged();
    if (level != null && !level.isClientSide()) checkErrorState();
  }

  public void setOwnerByUsername(String username) {
    Player player = null;
    if (level != null && level.getServer() != null) {
      player = level.getServer().getPlayerList().getPlayerByName(username);
    }

    if (player != null) {
      this.ownerID = player.getUUID();
      this.ownerUser = player.getName().getString();
      mainNode.setOwningPlayer(player);
    } else if (this.ownerUser == null || !this.ownerUser.equals(username)) {
      this.ownerID = null;
      this.ownerUser = username;
    }
    setChanged();

    if (level != null && !level.isClientSide()) {
      level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
      checkErrorState();
    }
  }

  public void updateOwnershipInfo(Player player) {
    if (this.ownerUser != null
        && this.ownerID == null
        && this.ownerUser.equals(player.getName().getString())) {
      this.ownerID = player.getUUID();
      setChanged();
      if (level != null && !level.isClientSide()) {
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        checkErrorState();
      }
    }
  }

  public UUID getOwnerID() {
    return this.ownerID;
  }

  public String getOwnerUser() {
    return this.ownerUser;
  }

  public boolean hasOwner() {
    return this.ownerID != null || this.ownerUser != null;
  }

  public boolean isOwner(Player player) {
    if (this.ownerID != null && this.ownerID.equals(player.getUUID())) return true;
    if (this.ownerUser != null && this.ownerUser.equals(player.getName().getString())) {
      this.ownerID = player.getUUID();
      setChanged();
      return true;
    }
    return false;
  }

  // --- settings --------------------------------------------------------

  public void setInfinite(boolean infiniteInventory) {
    this.infiniteInventory = infiniteInventory;
    sync();
  }

  public void setDiscarding(boolean discardsPayment) {
    this.discardsPayment = discardsPayment;
    sync();
  }

  public void setBuyMode(boolean buyMode) {
    this.buyMode = buyMode;
    sync();
  }

  public boolean isInfinite() {
    return infiniteInventory;
  }

  public boolean isDiscarding() {
    return discardsPayment;
  }

  public boolean isBuyMode() {
    return buyMode;
  }

  // --- persistence -----------------------------------------------------

  @Override
  protected void saveAdditional(CompoundTag tag, Provider registries) {
    super.saveAdditional(tag, registries);

    ListTag posList = new ListTag();
    for (Position position : positions) posList.add(position.toTag(registries));
    tag.put("positions", posList);

    CompoundTag interfaceTag = new CompoundTag();
    logic.writeToNBT(interfaceTag, registries);
    tag.put(TAG_INTERFACE, interfaceTag);
    mainNode.saveToNBT(tag);
    tag.put("facade", facade.saveOptional(registries));

    if (this.ownerID != null) tag.putUUID("ownerID", this.ownerID);
    if (this.ownerUser != null) tag.putString("ownerUser", this.ownerUser);
    tag.putBoolean("hasError", this.hasError);
    tag.putInt("errorCode", this.errorCode);
    tag.putBoolean("infiniteInventory", this.infiniteInventory);
    tag.putBoolean("discardsPayment", this.discardsPayment);
    tag.putBoolean("buyMode", this.buyMode);
  }

  @Override
  protected void loadAdditional(CompoundTag tag, Provider registries) {
    super.loadAdditional(tag, registries);

    positions.clear();
    ListTag posList = tag.getList("positions", 10); // 10 = CompoundTag
    for (int i = 0; i < posList.size() && positions.size() < MAX_POSITIONS; i++) {
      Position position = Position.fromTag(posList.getCompound(i), registries);
      if (!position.item().isEmpty()) positions.add(position);
    }

    logic.readFromNBT(tag.getCompound(TAG_INTERFACE), registries);
    mainNode.loadFromNBT(tag);
    this.facade = ItemStack.parseOptional(registries, tag.getCompound("facade"));

    if (tag.hasUUID("ownerID")) this.ownerID = tag.getUUID("ownerID");
    if (tag.contains("ownerUser")) this.ownerUser = tag.getString("ownerUser");
    if (tag.contains("hasError")) this.hasError = tag.getBoolean("hasError");
    if (tag.contains("errorCode")) this.errorCode = tag.getInt("errorCode");
    this.infiniteInventory = tag.getBoolean("infiniteInventory");
    this.discardsPayment = tag.getBoolean("discardsPayment");
    this.buyMode = tag.getBoolean("buyMode");
    if (tag.contains(TAG_ME_STATUS, Tag.TAG_INT)) this.meStatus = tag.getInt(TAG_ME_STATUS);

    if (level != null && !level.isClientSide()) checkErrorState();
  }

  @Override
  public Packet<ClientGamePacketListener> getUpdatePacket() {
    return ClientboundBlockEntityDataPacket.create(this);
  }

  @Override
  public CompoundTag getUpdateTag(Provider registries) {
    CompoundTag tag = saveWithoutMetadata(registries);
    tag.putInt(TAG_ME_STATUS, meStatus);
    return tag;
  }

  public void checkErrorState() {
    boolean wasError = this.hasError;
    int wasCode = this.errorCode;

    this.hasError = false;
    this.errorCode = 0;

    if (positions.isEmpty()) {
      this.hasError = true;
      this.errorCode = 3; // not set up
    } else if (buyMode) {
      // Buy mode: error when bought goods can no longer be stored (unless discarding them).
      if (!discardsPayment) {
        boolean anyRoom = false;
        for (Position position : positions) {
          if (canStore(position.item(), position.amount())) {
            anyRoom = true;
            break;
          }
        }
        if (!anyRoom) {
          this.hasError = true;
          this.errorCode = 2; // storage full
        }
      }
    } else if (!infiniteInventory) {
      boolean anyInStock = false;
      for (Position position : positions) {
        if (purchasesLeft(position) >= 1) {
          anyInStock = true;
          break;
        }
      }
      if (!anyInStock) {
        this.hasError = true;
        this.errorCode = 1; // out of stock
      }
    }

    if (wasError != this.hasError || wasCode != this.errorCode) {
      setChanged();
      if (level != null && !level.isClientSide()) {
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
      }
    }
  }
}
