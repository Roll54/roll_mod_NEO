package com.roll_54.roll_mod.economy.vendingblock.network;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.economy.vendingblock.auction.AuctionListing;
import com.roll_54.roll_mod.economy.vendingblock.client.ClientAuctionCache;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server &rarr; client snapshot of the whole Auction House plus the receiving player's own
 * collection (claimable items). Sent on open and whenever listings/claims change for viewers.
 */
public record SyncAuctionsPacket(List<AuctionListing> listings, List<ItemStack> claims)
    implements CustomPacketPayload {

  public static final Type<SyncAuctionsPacket> TYPE =
      new Type<>(RollMod.id("sync_auctions"));

  public static final StreamCodec<RegistryFriendlyByteBuf, SyncAuctionsPacket> STREAM_CODEC =
      StreamCodec.of(SyncAuctionsPacket::encode, SyncAuctionsPacket::decode);

  private static void encode(RegistryFriendlyByteBuf buf, SyncAuctionsPacket packet) {
    buf.writeVarInt(packet.listings.size());
    for (AuctionListing l : packet.listings) l.toNetwork(buf);
    buf.writeVarInt(packet.claims.size());
    for (ItemStack s : packet.claims) ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, s);
  }

  private static SyncAuctionsPacket decode(RegistryFriendlyByteBuf buf) {
    int listingCount = buf.readVarInt();
    List<AuctionListing> listings = new ArrayList<>(listingCount);
    for (int i = 0; i < listingCount; i++) listings.add(AuctionListing.fromNetwork(buf));
    int claimCount = buf.readVarInt();
    List<ItemStack> claims = new ArrayList<>(claimCount);
    for (int i = 0; i < claimCount; i++) claims.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
    return new SyncAuctionsPacket(listings, claims);
  }

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  /**
   * Client-side handler: refresh the local cache so any open auction UI re-renders on its next
   * tick.
   */
  public static void handle(SyncAuctionsPacket packet, IPayloadContext context) {
    context.enqueueWork(() -> ClientAuctionCache.set(packet.listings(), packet.claims()));
  }
}
