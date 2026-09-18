package com.roll_54.roll_mod.economy.vendingblock.auction;

import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Server-global, persistent Auction House storage. Stored once on the overworld's {@code
 * DimensionDataStorage} (file {@code world/data/roll_mod_currency_auctions.dat}). Holds every
 * active listing plus a per-player "collection" of items waiting to be claimed (sold/expired/won).
 */
public class AuctionData extends SavedData {

  // Compatibility name, not an oversight: this is the live save file on the server. Renaming it
  // would orphan every running auction.
  private static final String NAME = "roll_mod_currency_auctions";

  private final List<AuctionListing> listings = new ArrayList<>();
  private final Map<UUID, List<ItemStack>> claims = new HashMap<>();

  public static AuctionData get(MinecraftServer server) {
    return server
        .overworld()
        .getDataStorage()
        .computeIfAbsent(new Factory<>(AuctionData::new, AuctionData::load, null), NAME);
  }

  public List<AuctionListing> getListings() {
    return listings;
  }

  public AuctionListing findById(UUID id) {
    if (id == null) return null;
    for (AuctionListing l : listings) {
      if (l.id.equals(id)) return l;
    }
    return null;
  }

  public int activeCountFor(UUID sellerId) {
    int n = 0;
    for (AuctionListing l : listings) {
      if (l.sellerId.equals(sellerId)) n++;
    }
    return n;
  }

  public void addListing(AuctionListing listing) {
    listings.add(listing);
    setDirty();
  }

  public void removeListing(AuctionListing listing) {
    listings.remove(listing);
    setDirty();
  }

  public List<ItemStack> getClaims(UUID playerId) {
    return claims.getOrDefault(playerId, List.of());
  }

  public void addClaim(UUID playerId, ItemStack stack) {
    if (stack == null || stack.isEmpty()) return;
    claims.computeIfAbsent(playerId, k -> new ArrayList<>()).add(stack.copy());
    setDirty();
  }

  public void setClaims(UUID playerId, List<ItemStack> stacks) {
    if (stacks == null || stacks.isEmpty()) {
      claims.remove(playerId);
    } else {
      claims.put(playerId, stacks);
    }
    setDirty();
  }

  /* ----------------------------------------------------- NBT ----------------------------------------------------- */

  public static AuctionData load(CompoundTag tag, HolderLookup.Provider registries) {
    AuctionData data = new AuctionData();
    ListTag list = tag.getList("listings", Tag.TAG_COMPOUND);
    for (int i = 0; i < list.size(); i++) {
      AuctionListing l = AuctionListing.fromTag(list.getCompound(i), registries);
      if (!l.item.isEmpty()) data.listings.add(l);
    }
    ListTag claimEntries = tag.getList("claims", Tag.TAG_COMPOUND);
    for (int i = 0; i < claimEntries.size(); i++) {
      CompoundTag entry = claimEntries.getCompound(i);
      UUID owner = entry.getUUID("owner");
      ListTag items = entry.getList("items", Tag.TAG_COMPOUND);
      List<ItemStack> stacks = new ArrayList<>();
      for (int j = 0; j < items.size(); j++) {
        CompoundTag c = items.getCompound(j);
        ItemStack s;
        // New format wraps the item as {item: <count=1 proto>, count: <int>}; legacy entries are a
        // raw ItemStack tag (which uses "id", never a nested "item"), so distinguish on that key.
        if (c.contains("item")) {
          ItemStack proto = ItemStack.parseOptional(registries, c.getCompound("item"));
          int count = c.getInt("count");
          s = proto.isEmpty() || count <= 0 ? ItemStack.EMPTY : proto.copyWithCount(count);
        } else {
          s = ItemStack.parseOptional(registries, c);
        }
        if (!s.isEmpty()) stacks.add(s);
      }
      if (!stacks.isEmpty()) data.claims.put(owner, stacks);
    }
    return data;
  }

  @Override
  public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
    ListTag list = new ListTag();
    for (AuctionListing l : listings) list.add(l.toTag(registries));
    tag.put("listings", list);

    ListTag claimEntries = new ListTag();
    for (Map.Entry<UUID, List<ItemStack>> e : claims.entrySet()) {
      if (e.getValue().isEmpty()) continue;
      CompoundTag entry = new CompoundTag();
      entry.putUUID("owner", e.getKey());
      ListTag items = new ListTag();
      for (ItemStack s : e.getValue()) {
        // Mirror AuctionListing: count=1 prototype + real count, so claims with >99 items
        // survive NBT save (vanilla caps the encoded ItemStack count at [1;99]).
        CompoundTag itemTag = new CompoundTag();
        itemTag.put("item", s.copyWithCount(1).saveOptional(registries));
        itemTag.putInt("count", s.getCount());
        items.add(itemTag);
      }
      entry.put("items", items);
      claimEntries.add(entry);
    }
    tag.put("claims", claimEntries);
    return tag;
  }
}
