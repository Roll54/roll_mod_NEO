package com.roll_54.roll_mod.economy.vendingblock.auction;

import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

/**
 * A single Auction House listing. Unlike a vendor {@code Position}, the {@link #item} keeps its
 * real count (the whole stack being sold). A listing is either a fixed-price buyout (see {@link
 * #fixedPrice}) or a bidding auction (see {@link #startPrice}/{@link #buyout}/{@link #currentBid}).
 * Bid state is mutable; everything else is set at creation.
 */
public class AuctionListing {

  public final UUID id;
  public final UUID sellerId;
  public final String sellerName;
  public final ItemStack item; // full stack being sold
  public final boolean bidding; // false = fixed-price, true = auction
  public final long fixedPrice; // fixed listings: buyout/sale price
  public final long startPrice; // bidding listings: minimum opening bid
  public final long buyout; // bidding listings: instant-buy price (0 = none)
  public final long createdAtMs;
  public final long expiresAtMs;

  public long currentBid; // 0 = no bids yet
  public UUID currentBidderId; // null = no bids yet
  public String currentBidderName; // null = no bids yet

  public AuctionListing(
      UUID id,
      UUID sellerId,
      String sellerName,
      ItemStack item,
      boolean bidding,
      long fixedPrice,
      long startPrice,
      long buyout,
      long createdAtMs,
      long expiresAtMs) {
    this.id = id;
    this.sellerId = sellerId;
    this.sellerName = sellerName == null ? "" : sellerName;
    this.item = item;
    this.bidding = bidding;
    this.fixedPrice = Math.max(0L, fixedPrice);
    this.startPrice = Math.max(0L, startPrice);
    this.buyout = Math.max(0L, buyout);
    this.createdAtMs = createdAtMs;
    this.expiresAtMs = expiresAtMs;
  }

  public boolean hasBids() {
    return currentBidderId != null;
  }

  /**
   * The price currently relevant to a buyer: current bid (or start price) for auctions, else the
   * fixed price.
   */
  public long displayPrice() {
    if (bidding) return hasBids() ? currentBid : startPrice;
    return fixedPrice;
  }

  /** Smallest acceptable next bid for a bidding listing. */
  public long minNextBid(long increment) {
    return hasBids() ? currentBid + Math.max(1L, increment) : startPrice;
  }

  public boolean isExpired(long nowMs) {
    return nowMs >= expiresAtMs;
  }

  public long remainingMs(long nowMs) {
    return Math.max(0L, expiresAtMs - nowMs);
  }

  /* ----------------------------------------------------- NBT ----------------------------------------------------- */

  public CompoundTag toTag(HolderLookup.Provider registries) {
    CompoundTag tag = new CompoundTag();
    tag.putUUID("id", id);
    tag.putUUID("sellerId", sellerId);
    tag.putString("sellerName", sellerName);
    // Store a count=1 prototype + the real count separately: vanilla ItemStack NBT encoding
    // hard-caps the count at [1;99], but a listing may hold a full/multi stack (e.g. 500).
    ItemStack proto = item.isEmpty() ? ItemStack.EMPTY : item.copyWithCount(1);
    tag.put("item", proto.saveOptional(registries));
    tag.putInt("count", item.getCount());
    tag.putBoolean("bidding", bidding);
    tag.putLong("fixedPrice", fixedPrice);
    tag.putLong("startPrice", startPrice);
    tag.putLong("buyout", buyout);
    tag.putLong("createdAt", createdAtMs);
    tag.putLong("expiresAt", expiresAtMs);
    tag.putLong("currentBid", currentBid);
    if (currentBidderId != null) {
      tag.putUUID("bidderId", currentBidderId);
      tag.putString("bidderName", currentBidderName == null ? "" : currentBidderName);
    }
    return tag;
  }

  public static AuctionListing fromTag(CompoundTag tag, HolderLookup.Provider registries) {
    ItemStack proto = ItemStack.parseOptional(registries, tag.getCompound("item"));
    // Pre-fix saves lack the top-level "count"; their "item" tag still carries the real count
    // (necessarily <=99, or it would have crashed on save), so fall back to the prototype's count.
    int count = tag.contains("count") ? tag.getInt("count") : proto.getCount();
    ItemStack item = proto.isEmpty() || count <= 0 ? ItemStack.EMPTY : proto.copyWithCount(count);
    AuctionListing l =
        new AuctionListing(
            tag.getUUID("id"),
            tag.getUUID("sellerId"),
            tag.getString("sellerName"),
            item,
            tag.getBoolean("bidding"),
            tag.getLong("fixedPrice"),
            tag.getLong("startPrice"),
            tag.getLong("buyout"),
            tag.getLong("createdAt"),
            tag.getLong("expiresAt"));
    l.currentBid = tag.getLong("currentBid");
    if (tag.hasUUID("bidderId")) {
      l.currentBidderId = tag.getUUID("bidderId");
      l.currentBidderName = tag.getString("bidderName");
    }
    return l;
  }

  /* --------------------------------------------------- Network --------------------------------------------------- */

  public void toNetwork(RegistryFriendlyByteBuf buf) {
    UUIDUtil.STREAM_CODEC.encode(buf, id);
    UUIDUtil.STREAM_CODEC.encode(buf, sellerId);
    buf.writeUtf(sellerName);
    ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, item);
    buf.writeBoolean(bidding);
    buf.writeVarLong(fixedPrice);
    buf.writeVarLong(startPrice);
    buf.writeVarLong(buyout);
    buf.writeLong(createdAtMs);
    buf.writeLong(expiresAtMs);
    buf.writeVarLong(currentBid);
    boolean hasBidder = currentBidderId != null;
    buf.writeBoolean(hasBidder);
    if (hasBidder) {
      UUIDUtil.STREAM_CODEC.encode(buf, currentBidderId);
      buf.writeUtf(currentBidderName == null ? "" : currentBidderName);
    }
  }

  public static AuctionListing fromNetwork(RegistryFriendlyByteBuf buf) {
    UUID id = UUIDUtil.STREAM_CODEC.decode(buf);
    UUID sellerId = UUIDUtil.STREAM_CODEC.decode(buf);
    String sellerName = buf.readUtf();
    ItemStack item = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
    boolean bidding = buf.readBoolean();
    long fixedPrice = buf.readVarLong();
    long startPrice = buf.readVarLong();
    long buyout = buf.readVarLong();
    long createdAt = buf.readLong();
    long expiresAt = buf.readLong();
    AuctionListing l =
        new AuctionListing(
            id,
            sellerId,
            sellerName,
            item,
            bidding,
            fixedPrice,
            startPrice,
            buyout,
            createdAt,
            expiresAt);
    l.currentBid = buf.readVarLong();
    if (buf.readBoolean()) {
      l.currentBidderId = UUIDUtil.STREAM_CODEC.decode(buf);
      l.currentBidderName = buf.readUtf();
    }
    return l;
  }
}
