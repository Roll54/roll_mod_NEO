package com.roll_54.roll_mod.economy.network;

import com.roll_54.roll_mod.economy.network.client.ClientPayloadHandler;
import com.roll_54.roll_mod.economy.network.payload.PlaytimeRewardPayload;
import com.roll_54.roll_mod.economy.network.payload.SyncBalancePayload;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * The currency payloads. Registered from {@link com.roll_54.roll_mod.network.NetworkHandler} rather
 * than from an event of its own: a mod gets one registrar per channel version, and asking for a
 * second {@code registrar("1")} under the same mod id is a duplicate registration.
 */
public final class CurrencyNetworking {

  private CurrencyNetworking() {}

  public static void register(final PayloadRegistrar registrar) {
    registrar.playToClient(
        SyncBalancePayload.TYPE, SyncBalancePayload.STREAM_CODEC, ClientPayloadHandler::handle);

    registrar.playToClient(
        PlaytimeRewardPayload.TYPE,
        PlaytimeRewardPayload.STREAM_CODEC,
        ClientPayloadHandler::handle);
  }
}
