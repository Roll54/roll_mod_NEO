package com.roll_54.roll_mod.economy.vendingblock.blockentity;

import com.roll_54.roll_mod.economy.vendingblock.registry.BlockEntityRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

public class VendorBlockEntity extends BlockEntity {

  public static final int MAX_POSITIONS = 10;

  private UUID ownerID;
  private String ownerUser;
  public boolean infiniteInventory = false;
  public boolean discardsPayment = false;
  public boolean buyMode = false; // false = sell to players, true = buy from players
  public boolean hasError = false;
  public int errorCode = 0; // 0 = no error, 1 = no stock, 2 = storage full, 3 = not set

  private final List<Position> positions = new ArrayList<>();
  private ItemStack facade = ItemStack.EMPTY;

  public final VendorStorage storage = new VendorStorage(this::onStorageChanged);

  public VendorBlockEntity(BlockPos pos, BlockState state) {
    super(BlockEntityRegistry.VENDOR_BE.get(), pos, state);
  }

  private void onStorageChanged() {
    setChanged();
    if (level != null && !level.isClientSide()) {
      level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
      level.invalidateCapabilities(getBlockPos());
      checkErrorState();
    }
  }

  private void sync() {
    setChanged();
    if (level != null && !level.isClientSide()) {
      level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
      level.invalidateCapabilities(getBlockPos());
      checkErrorState();
    }
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
    return storage.countOf(position.item());
  }

  public long purchasesLeft(Position position) {
    if (infiniteInventory) return Long.MAX_VALUE;
    if (position.amount() <= 0) return 0;
    return (long) storage.countOf(position.item()) / position.amount();
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
    int remaining = amount;
    for (int slot = 0; slot < storage.getSlots() && remaining > 0; slot++) {
      ItemStack leftover = storage.insertItem(slot, item.copyWithCount(remaining), true);
      remaining = leftover.isEmpty() ? 0 : leftover.getCount();
    }
    return remaining <= 0;
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

  private final IItemHandler insertItemHandler =
      new IItemHandler() {
        @Override
        public int getSlots() {
          return storage.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
          return storage.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
          return storage.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
          return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
          return storage.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
          return storage.isItemValid(slot, stack);
        }
      };

  public IItemHandler getInsertItemHandler() {
    return insertItemHandler;
  }

  private final IItemHandler extractItemHandler =
      new IItemHandler() {
        @Override
        public int getSlots() {
          return storage.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
          return storage.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
          return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
          ItemStack inSlot = storage.getStackInSlot(slot);
          if (inSlot.isEmpty()) return ItemStack.EMPTY;
          // In sell mode, don't pipe out sale stock. In buy mode, position items ARE the
          // bought goods the owner collects, so allow extracting them.
          if (!buyMode && isPositionItem(inSlot)) return ItemStack.EMPTY;
          return storage.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
          return storage.getSlotLimit(slot);
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
    for (int i = 0; i < VendorStorage.SLOTS; i++) {
      ItemStack stack = storage.getStackInSlot(i);
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

    tag.put("storage", storage.serializeNBT(registries));
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

    storage.deserializeNBT(registries, tag.getCompound("storage"));
    this.facade = ItemStack.parseOptional(registries, tag.getCompound("facade"));

    if (tag.hasUUID("ownerID")) this.ownerID = tag.getUUID("ownerID");
    if (tag.contains("ownerUser")) this.ownerUser = tag.getString("ownerUser");
    if (tag.contains("hasError")) this.hasError = tag.getBoolean("hasError");
    if (tag.contains("errorCode")) this.errorCode = tag.getInt("errorCode");
    this.infiniteInventory = tag.getBoolean("infiniteInventory");
    this.discardsPayment = tag.getBoolean("discardsPayment");
    this.buyMode = tag.getBoolean("buyMode");

    if (level != null && !level.isClientSide()) checkErrorState();
  }

  @Override
  public Packet<ClientGamePacketListener> getUpdatePacket() {
    return ClientboundBlockEntityDataPacket.create(this);
  }

  @Override
  public CompoundTag getUpdateTag(Provider registries) {
    return saveWithoutMetadata(registries);
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
