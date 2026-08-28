package com.roll_54.roll_mod.minestar.dailytasks;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTask;
import com.roll_54.roll_mod.network.packet.DailyTaskToastPacket;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Announces a finished daily task the way vanilla announces an advancement: the toast in the corner
 * plus the chat line.
 *
 * <p>No advancement is ever registered — the {@link AdvancementHolder} is synthesised on the spot
 * and handed straight to {@code AdvancementToast} on the client, so daily tasks never show up in
 * the advancement tree. The chat line is produced by vanilla's own
 * {@link AdvancementType#createAnnouncement}, which keeps it correctly translated in every
 * language and honours the {@code announceAdvancements} gamerule just like the real thing.
 */
public final class DailyTaskNotifier {

    private DailyTaskNotifier() {}

    /**
     * @param trigger the player whose action finished the task — they are the one named in chat
     * @param group   everyone who shares the task; each online member gets the toast
     */
    public static void announce(MinecraftServer server, DailyTaskGroups.TaskGroup group,
                                DailyTask task, int required, ServerPlayer trigger) {
        AdvancementHolder holder = holderFor(task, required);

        for (UUID memberId : group.members()) {
            ServerPlayer member = server.getPlayerList().getPlayer(memberId);
            if (member != null) {
                PacketDistributor.sendToPlayer(member, new DailyTaskToastPacket(holder));
            }
        }

        if (server.getGameRules().getBoolean(GameRules.RULE_ANNOUNCE_ADVANCEMENTS)) {
            server.getPlayerList().broadcastSystemMessage(
                    AdvancementType.TASK.createAnnouncement(holder, trigger), false);
        }
    }

    /** A display-only advancement: no criteria, no requirements, no rewards, no parent. */
    public static AdvancementHolder holderFor(DailyTask task, int required) {
        DisplayInfo display = new DisplayInfo(
                task.toastIcon(),
                task.name(),
                task.tooltip(required),
                Optional.empty(),
                AdvancementType.TASK,
                true,   // showToast
                true,   // announceChat
                false); // hidden
        return new AdvancementHolder(
                RollMod.id("daily_task/" + task.id()),
                new Advancement(
                        Optional.empty(),
                        Optional.of(display),
                        AdvancementRewards.EMPTY,
                        Map.of(),
                        AdvancementRequirements.EMPTY,
                        false));
    }
}
