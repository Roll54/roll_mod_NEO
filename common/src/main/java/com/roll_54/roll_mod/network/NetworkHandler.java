package com.roll_54.roll_mod.network;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.economy.network.CurrencyNetworking;
import com.roll_54.roll_mod.network.packet.ClaimDailyBonusPacket;
import com.roll_54.roll_mod.network.packet.ClaimDailyTaskPacket;
import com.roll_54.roll_mod.network.packet.RerollDailyTaskPacket;
import com.roll_54.roll_mod.network.packet.DailyTaskToastPacket;
import com.roll_54.roll_mod.network.packet.OpenHubPacket;
import com.roll_54.roll_mod.network.packet.HomeActionPacket;
import com.roll_54.roll_mod.network.packet.KitActionPacket;
import com.roll_54.roll_mod.network.packet.SyncKitsPacket;
import com.roll_54.roll_mod.network.packet.OperatorTogglePacket;
import com.roll_54.roll_mod.network.packet.SyncOperatorsPacket;
import com.roll_54.roll_mod.network.packet.SyncPlayerStatusPacket;
import com.roll_54.roll_mod.network.packet.SyncModerationPacket;
import com.roll_54.roll_mod.network.packet.ModerationActionPacket;
import com.roll_54.roll_mod.network.packet.SyncLettersPacket;
import com.roll_54.roll_mod.network.packet.LetterComposePacket;
import com.roll_54.roll_mod.network.packet.LetterAcceptPacket;
import com.roll_54.roll_mod.network.packet.SyncTpaPacket;
import com.roll_54.roll_mod.network.packet.TeleportActionPacket;
import com.roll_54.roll_mod.network.packet.TpaActionPacket;
import com.roll_54.roll_mod.network.packet.TpaModePacket;
import com.roll_54.roll_mod.network.packet.SyncHomesPacket;
import com.roll_54.roll_mod.network.packet.SyncWarpsPacket;
import com.roll_54.roll_mod.network.packet.WarpActionPacket;
import com.roll_54.roll_mod.network.packet.PacketLaunchRocket;
import com.roll_54.roll_mod.network.packet.armor.MultiProtectingGraviChestItemPacket;
import com.roll_54.roll_mod.network.packet.skin.SkinActionPacket;
import com.roll_54.roll_mod.network.packet.skin.SyncActiveSkinsPacket;
import com.roll_54.roll_mod.network.packet.skin.SyncUnlockedSkinsPacket;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = RollMod.MODID)
public class NetworkHandler {
    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                PacketLaunchRocket.TYPE,
                PacketLaunchRocket.STREAM_CODEC,
                PacketLaunchRocket::handle
        );
        registrar.playToServer(
                MultiProtectingGraviChestItemPacket.TYPE,
                MultiProtectingGraviChestItemPacket.STREAM_CODEC,
                MultiProtectingGraviChestItemPacket::handle
        );
        registrar.playToServer(
                ClaimDailyTaskPacket.TYPE,
                ClaimDailyTaskPacket.STREAM_CODEC,
                ClaimDailyTaskPacket::handle
        );
        registrar.playToServer(
                RerollDailyTaskPacket.TYPE,
                RerollDailyTaskPacket.STREAM_CODEC,
                RerollDailyTaskPacket::handle
        );
        registrar.playToServer(
                ClaimDailyBonusPacket.TYPE,
                ClaimDailyBonusPacket.STREAM_CODEC,
                ClaimDailyBonusPacket::handle
        );
        registrar.playToServer(
                OpenHubPacket.TYPE,
                OpenHubPacket.STREAM_CODEC,
                OpenHubPacket::handle
        );
        registrar.playToServer(
                WarpActionPacket.TYPE,
                WarpActionPacket.STREAM_CODEC,
                WarpActionPacket::handle
        );
        registrar.playToClient(
                SyncWarpsPacket.TYPE,
                SyncWarpsPacket.STREAM_CODEC,
                SyncWarpsPacket::handle
        );
        registrar.playToServer(
                HomeActionPacket.TYPE,
                HomeActionPacket.STREAM_CODEC,
                HomeActionPacket::handle
        );
        registrar.playToClient(
                SyncHomesPacket.TYPE,
                SyncHomesPacket.STREAM_CODEC,
                SyncHomesPacket::handle
        );
        registrar.playToClient(
                DailyTaskToastPacket.TYPE,
                DailyTaskToastPacket.STREAM_CODEC,
                DailyTaskToastPacket::handle
        );

        registrar.playToServer(
                SkinActionPacket.TYPE,
                SkinActionPacket.STREAM_CODEC,
                SkinActionPacket::handle
        );
        registrar.playToClient(
                SyncActiveSkinsPacket.TYPE,
                SyncActiveSkinsPacket.STREAM_CODEC,
                SyncActiveSkinsPacket::handle
        );
        registrar.playToClient(
                SyncUnlockedSkinsPacket.TYPE,
                SyncUnlockedSkinsPacket.STREAM_CODEC,
                SyncUnlockedSkinsPacket::handle
        );

        registrar.playToServer(
                KitActionPacket.TYPE,
                KitActionPacket.STREAM_CODEC,
                KitActionPacket::handle
        );
        registrar.playToClient(
                SyncKitsPacket.TYPE,
                SyncKitsPacket.STREAM_CODEC,
                SyncKitsPacket::handle
        );
        registrar.playToServer(
                TeleportActionPacket.TYPE,
                TeleportActionPacket.STREAM_CODEC,
                TeleportActionPacket::handle
        );
        registrar.playToServer(
                TpaActionPacket.TYPE,
                TpaActionPacket.STREAM_CODEC,
                TpaActionPacket::handle
        );
        registrar.playToServer(
                TpaModePacket.TYPE,
                TpaModePacket.STREAM_CODEC,
                TpaModePacket::handle
        );
        registrar.playToClient(
                SyncTpaPacket.TYPE,
                SyncTpaPacket.STREAM_CODEC,
                SyncTpaPacket::handle
        );
        registrar.playToServer(
                OperatorTogglePacket.TYPE,
                OperatorTogglePacket.STREAM_CODEC,
                OperatorTogglePacket::handle
        );
        registrar.playToClient(
                SyncOperatorsPacket.TYPE,
                SyncOperatorsPacket.STREAM_CODEC,
                SyncOperatorsPacket::handle
        );
        registrar.playToClient(
                SyncPlayerStatusPacket.TYPE,
                SyncPlayerStatusPacket.STREAM_CODEC,
                SyncPlayerStatusPacket::handle
        );
        registrar.playToClient(
                SyncModerationPacket.TYPE,
                SyncModerationPacket.STREAM_CODEC,
                SyncModerationPacket::handle
        );
        registrar.playToServer(
                ModerationActionPacket.TYPE,
                ModerationActionPacket.STREAM_CODEC,
                ModerationActionPacket::handle
        );
        registrar.playToClient(
                SyncLettersPacket.TYPE,
                SyncLettersPacket.STREAM_CODEC,
                SyncLettersPacket::handle
        );
        registrar.playToServer(
                LetterComposePacket.TYPE,
                LetterComposePacket.STREAM_CODEC,
                LetterComposePacket::handle
        );
        registrar.playToServer(
                LetterAcceptPacket.TYPE,
                LetterAcceptPacket.STREAM_CODEC,
                LetterAcceptPacket::handle
        );

        // The economy's payloads keep their own lists, but share this registrar: NeoForge allows a
        // mod one registrar per channel version, so each feature asking for its own registrar("1")
        // would be a duplicate registration under the same mod id.
        CurrencyNetworking.register(registrar);
        com.roll_54.roll_mod.economy.vendingblock.network.NetworkHandler.register(registrar);
    }
}
