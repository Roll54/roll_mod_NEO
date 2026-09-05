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
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Announces a finished daily task the way vanilla announces an advancement: the toast in the corner
 * plus the chat line.
 *
 * <p>No advancement is ever registered — the {@link AdvancementHolder} is synthesised on the spot
 * and handed straight to {@code AdvancementToast} on the client, so daily tasks never show up in
 * the advancement tree.
 *
 * <p>The chat line reads "<i>player</i> completed the daily task <i>[Task]</i>" rather than
 * vanilla's "made the advancement", so it never claims an advancement the player has not earned.
 * The bracketed, hover-tooltipped task name still comes from {@link Advancement#name}, and the
 * {@code announceAdvancements} gamerule still gates the line, as it did when this used
 * {@link AdvancementType#createAnnouncement}.
 *
 * <p>Nor is the line broadcast server-wide: a daily task is the group's business, so only the
 * online members of the group — the player's FTB party, or just the player when they are in none —
 * see it.
 */
public final class DailyTaskNotifier {

    /** {@code "<player> completed the daily task <[Task]>"}. */
    private static final String ANNOUNCEMENT_KEY = "message.roll_mod.daily_task.completed";

    private DailyTaskNotifier() {}

    /**
     * @param trigger the player whose action finished the task — they are the one named in chat
     * @param group   everyone who shares the task; each online member gets the toast and the chat
     *                line, and nobody outside the group is told
     */
    public static void announce(MinecraftServer server, DailyTaskGroups.TaskGroup group,
                                DailyTask task, int required, ServerPlayer trigger) {
        AdvancementHolder holder = holderFor(task, required);
        Component announcement =
                server.getGameRules().getBoolean(GameRules.RULE_ANNOUNCE_ADVANCEMENTS)
                        ? Component.translatable(ANNOUNCEMENT_KEY,
                                trigger.getDisplayName(), Advancement.name(holder))
                        : null;

        for (UUID memberId : group.members()) {
            ServerPlayer member = server.getPlayerList().getPlayer(memberId);
            if (member != null) {
                notify(member, holder, announcement);
            }
        }

        // The trigger is always in their own group, but a stale party roster must never cost them
        // the notification for a task they just finished.
        if (!group.members().contains(trigger.getUUID())) {
            notify(trigger, holder, announcement);
        }
    }

    /** The toast, plus the chat line when the gamerule allows one. */
    private static void notify(ServerPlayer player, AdvancementHolder holder,
                               @Nullable Component announcement) {
        PacketDistributor.sendToPlayer(player, new DailyTaskToastPacket(holder));
        if (announcement != null) {
            player.sendSystemMessage(announcement);
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
