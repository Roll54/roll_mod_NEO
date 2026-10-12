package com.roll_54.roll_mod.network;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.economy.network.CurrencyNetworking;
import com.roll_54.roll_mod.network.packet.ClaimDailyBonusPacket;
import com.roll_54.roll_mod.network.packet.ClaimDailyTaskPacket;
import com.roll_54.roll_mod.network.packet.RerollDailyTaskPacket;
import com.roll_54.roll_mod.network.packet.DailyTaskToastPacket;
import com.roll_54.roll_mod.network.packet.OpenHubPacket;
import com.roll_54.roll_mod.network.packet.WelcomeResponsePacket;
import com.roll_54.roll_mod.network.packet.FriendActionPacket;
import com.roll_54.roll_mod.network.packet.ChatChannelPacket;
import com.roll_54.roll_mod.network.packet.HistoryPackets;
import com.roll_54.roll_mod.network.packet.LeaderboardPackets;
import com.roll_54.roll_mod.network.packet.SkinPackets;
import com.roll_54.roll_mod.network.packet.SyncFriendDetailPacket;
import com.roll_54.roll_mod.network.packet.SyncFriendsPacket;
import com.roll_54.roll_mod.network.packet.drill.DrillThrottlePacket;
import com.roll_54.roll_mod.network.packet.drill.DrillVolumePacket;
import com.roll_54.roll_mod.network.packet.drill.DrillTrashFilterPacket;
import com.roll_54.roll_mod.network.packet.drill.OpenDrillConfigPacket;
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
import com.roll_54.roll_mod.network.packet.MoneyActionPacket;
import com.roll_54.roll_mod.network.packet.InteractionSettingPacket;
import com.roll_54.roll_mod.network.packet.KitNamePackets;
import com.roll_54.roll_mod.network.packet.SyncInteractionSettingsPacket;
import com.roll_54.roll_mod.network.packet.SyncMoneyPacket;
import com.roll_54.roll_mod.network.packet.TeleportActionPacket;
import com.roll_54.roll_mod.network.packet.TpaActionPacket;
import com.roll_54.roll_mod.network.packet.TpaModePacket;
import com.roll_54.roll_mod.network.packet.SyncHomesPacket;
import com.roll_54.roll_mod.network.packet.SyncWarpsPacket;
import com.roll_54.roll_mod.network.packet.TeleportCountdownPacket;
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
                WelcomeResponsePacket.TYPE,
                WelcomeResponsePacket.STREAM_CODEC,
                WelcomeResponsePacket::handle
        );
        registrar.playToClient(
                ChatChannelPacket.TYPE,
                ChatChannelPacket.STREAM_CODEC,
                ChatChannelPacket::handle
        );
        registrar.playToServer(
                LeaderboardPackets.Request.TYPE,
                LeaderboardPackets.Request.STREAM_CODEC,
                LeaderboardPackets.Request::handle
        );
        registrar.playToClient(
                LeaderboardPackets.Sync.TYPE,
                LeaderboardPackets.Sync.STREAM_CODEC,
                LeaderboardPackets.Sync::handle
        );
        registrar.playToServer(
                SkinPackets.Request.TYPE,
                SkinPackets.Request.STREAM_CODEC,
                SkinPackets.Request::handle
        );
        registrar.playToClient(
                SkinPackets.Sync.TYPE,
                SkinPackets.Sync.STREAM_CODEC,
                SkinPackets.Sync::handle
        );
        registrar.playToServer(
                HistoryPackets.Request.TYPE,
                HistoryPackets.Request.STREAM_CODEC,
                HistoryPackets.Request::handle
        );
        registrar.playToClient(
                HistoryPackets.Sync.TYPE,
                HistoryPackets.Sync.STREAM_CODEC,
                HistoryPackets.Sync::handle
        );
        registrar.playToServer(
                FriendActionPacket.TYPE,
                FriendActionPacket.STREAM_CODEC,
                FriendActionPacket::handle
        );
        registrar.playToClient(
                SyncFriendsPacket.TYPE,
                SyncFriendsPacket.STREAM_CODEC,
                SyncFriendsPacket::handle
        );
        registrar.playToClient(
                SyncFriendDetailPacket.TYPE,
                SyncFriendDetailPacket.STREAM_CODEC,
                SyncFriendDetailPacket::handle
        );
        registrar.playToServer(
                DrillThrottlePacket.TYPE,
                DrillThrottlePacket.STREAM_CODEC,
                DrillThrottlePacket::handle
        );
        registrar.playToServer(
                OpenDrillConfigPacket.TYPE,
                OpenDrillConfigPacket.STREAM_CODEC,
                OpenDrillConfigPacket::handle
        );
        registrar.playToServer(
                DrillVolumePacket.TYPE,
                DrillVolumePacket.STREAM_CODEC,
                DrillVolumePacket::handle
        );
        registrar.playToServer(
                DrillTrashFilterPacket.TYPE,
                DrillTrashFilterPacket.STREAM_CODEC,
                DrillTrashFilterPacket::handle
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
        registrar.playToClient(
                TeleportCountdownPacket.TYPE,
                TeleportCountdownPacket.STREAM_CODEC,
                TeleportCountdownPacket::handle
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
                com.roll_54.roll_mod.netherstorm.SyncStormPacket.TYPE,
                com.roll_54.roll_mod.netherstorm.SyncStormPacket.STREAM_CODEC,
                com.roll_54.roll_mod.netherstorm.SyncStormPacket::handle
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
        registrar.playToClient(
                KitNamePackets.Sync.TYPE,
                KitNamePackets.Sync.STREAM_CODEC,
                KitNamePackets.Sync::handle
        );
        registrar.playToServer(
                KitNamePackets.Rename.TYPE,
                KitNamePackets.Rename.STREAM_CODEC,
                KitNamePackets.Rename::handle
        );
        registrar.playToServer(
                InteractionSettingPacket.TYPE,
                InteractionSettingPacket.STREAM_CODEC,
                InteractionSettingPacket::handle
        );
        registrar.playToClient(
                SyncInteractionSettingsPacket.TYPE,
                SyncInteractionSettingsPacket.STREAM_CODEC,
                SyncInteractionSettingsPacket::handle
        );
        registrar.playToServer(
                MoneyActionPacket.TYPE,
                MoneyActionPacket.STREAM_CODEC,
                MoneyActionPacket::handle
        );
        registrar.playToClient(
                SyncMoneyPacket.TYPE,
                SyncMoneyPacket.STREAM_CODEC,
                SyncMoneyPacket::handle
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
