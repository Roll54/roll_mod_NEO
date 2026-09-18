package com.roll_54.roll_mod.economy.plot;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Server-global, persistent record of plots their owners have put up for resale. OPAC still owns
 * the actual claim; this only tracks "for sale, by whom, at what base price". Stored on the
 * overworld's {@code DimensionDataStorage} (file {@code world/data/roll_mod_currency_plots.dat}).
 * Keyed by plot id.
 */
public class PlotListings extends SavedData {

  // Compatibility name, not an oversight: this is the live save file on the server. Renaming it
  // would orphan every resale listing.
  private static final String NAME = "roll_mod_currency_plots";

  /** One resale listing: who is selling and the base price they receive when it sells. */
  public record Listing(UUID seller, long basePrice) {}

  private final Map<String, Listing> listings = new HashMap<>();

  public static PlotListings get(MinecraftServer server) {
    return server
        .overworld()
        .getDataStorage()
        .computeIfAbsent(new Factory<>(PlotListings::new, PlotListings::load, null), NAME);
  }

  public Listing get(String plotId) {
    return listings.get(plotId);
  }

  public void put(String plotId, UUID seller, long basePrice) {
    listings.put(plotId, new Listing(seller, basePrice));
    setDirty();
  }

  public void remove(String plotId) {
    if (listings.remove(plotId) != null) {
      setDirty();
    }
  }

  /* ----------------------------------------------------- NBT ----------------------------------------------------- */

  public static PlotListings load(CompoundTag tag, HolderLookup.Provider registries) {
    PlotListings data = new PlotListings();
    ListTag list = tag.getList("listings", Tag.TAG_COMPOUND);
    for (int i = 0; i < list.size(); i++) {
      CompoundTag entry = list.getCompound(i);
      String plotId = entry.getString("plot");
      UUID seller = entry.getUUID("seller");
      long basePrice = entry.getLong("price");
      if (!plotId.isEmpty()) {
        data.listings.put(plotId, new Listing(seller, basePrice));
      }
    }
    return data;
  }

  @Override
  public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
    ListTag list = new ListTag();
    for (Map.Entry<String, Listing> e : listings.entrySet()) {
      CompoundTag entry = new CompoundTag();
      entry.putString("plot", e.getKey());
      entry.putUUID("seller", e.getValue().seller());
      entry.putLong("price", e.getValue().basePrice());
      list.add(entry);
    }
    tag.put("listings", list);
    return tag;
  }
}
