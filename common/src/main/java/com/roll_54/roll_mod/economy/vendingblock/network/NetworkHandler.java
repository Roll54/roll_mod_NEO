package com.roll_54.roll_mod.economy.vendingblock.network;

import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * The vending-block, auction-house and plot payloads. Registered from {@link
 * com.roll_54.roll_mod.network.NetworkHandler} on the mod's single shared registrar.
 */
public class NetworkHandler {

  private NetworkHandler() {}

  public static void register(PayloadRegistrar registrar) {
    registrar.playToServer(
        OwnerChangePacket.TYPE, OwnerChangePacket.STREAM_CODEC, OwnerChangePacket::handle);
    registrar.playToServer(
        InfiniteInventoryPacket.TYPE,
        InfiniteInventoryPacket.STREAM_CODEC,
        InfiniteInventoryPacket::handle);
    registrar.playToServer(
        DiscardsPaymentPacket.TYPE,
        DiscardsPaymentPacket.STREAM_CODEC,
        DiscardsPaymentPacket::handle);
    registrar.playToServer(BuyModePacket.TYPE, BuyModePacket.STREAM_CODEC, BuyModePacket::handle);
    registrar.playToServer(
        FilterSlotUpdatePacket.TYPE,
        FilterSlotUpdatePacket.STREAM_CODEC,
        FilterSlotUpdatePacket::handle);
    registrar.playToServer(
        AddPositionPacket.TYPE, AddPositionPacket.STREAM_CODEC, AddPositionPacket::handle);
    registrar.playToServer(
        RemovePositionPacket.TYPE, RemovePositionPacket.STREAM_CODEC, RemovePositionPacket::handle);
    registrar.playToServer(
        StorageLockPacket.TYPE, StorageLockPacket.STREAM_CODEC, StorageLockPacket::handle);
    registrar.playToServer(
        SetFacadePacket.TYPE, SetFacadePacket.STREAM_CODEC, SetFacadePacket::handle);

    // Auction House
    registrar.playToClient(
        SyncAuctionsPacket.TYPE, SyncAuctionsPacket.STREAM_CODEC, SyncAuctionsPacket::handle);
    registrar.playToServer(
        CreateListingPacket.TYPE, CreateListingPacket.STREAM_CODEC, CreateListingPacket::handle);
    registrar.playToServer(
        AuctionActionPacket.TYPE, AuctionActionPacket.STREAM_CODEC, AuctionActionPacket::handle);

    // Plot purchasing (roll_mod:shop_dim)
    registrar.playToClient(
        com.roll_54.roll_mod.economy.plot.network.SyncPlotsPacket.TYPE,
        com.roll_54.roll_mod.economy.plot.network.SyncPlotsPacket.STREAM_CODEC,
        com.roll_54.roll_mod.economy.plot.network.SyncPlotsPacket::handle);
    registrar.playToServer(
        com.roll_54.roll_mod.economy.plot.network.BuyPlotPacket.TYPE,
        com.roll_54.roll_mod.economy.plot.network.BuyPlotPacket.STREAM_CODEC,
        com.roll_54.roll_mod.economy.plot.network.BuyPlotPacket::handle);
    registrar.playToServer(
        com.roll_54.roll_mod.economy.plot.network.PlotActionPacket.TYPE,
        com.roll_54.roll_mod.economy.plot.network.PlotActionPacket.STREAM_CODEC,
        com.roll_54.roll_mod.economy.plot.network.PlotActionPacket::handle);
  }
}
