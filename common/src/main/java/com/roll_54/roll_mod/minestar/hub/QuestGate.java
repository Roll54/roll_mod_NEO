package com.roll_54.roll_mod.minestar.hub;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * FTB Quests lookups for content gated behind a quest — so far, official warps that only open once
 * the visitor's team has finished a chosen quest.
 *
 * <p>Quests are kept by FTB's numeric id ({@code 0} meaning "no requirement"), which is what its
 * quest file and its own commands use, and which survives a quest being renamed or moved.
 *
 * <p>FTB Quests is a soft dependency, so its types stay inside {@link Ftb}, as in
 * {@link QuestProgress}. Without the mod, or with a quest that no longer exists, nothing is locked:
 * a requirement nobody can ever meet would strand a warp for good.
 */
public final class QuestGate {

    /** One quest as the picker lists it. */
    public record Entry(long id, Component title, Component chapter, String search) {}

    private static final boolean LOADED = ModList.get().isLoaded("ftbquests");

    private QuestGate() {}

    public static boolean available() {
        return LOADED;
    }

    /** Whether {@code player}'s team has finished {@code questId}. Works on either side. */
    public static boolean isCompleted(Player player, long questId) {
        if (questId == 0 || !LOADED) return true;
        try {
            return Ftb.isCompleted(player, questId);
        } catch (Throwable ignored) {
            // No quest file yet (early login): treat as not done rather than letting anyone through.
            return false;
        }
    }

    /** The quest's title, or its hex id if this side has no such quest. */
    public static Component title(boolean client, long questId) {
        if (LOADED) {
            try {
                Component title = Ftb.title(client, questId);
                if (title != null) return title;
            } catch (Throwable ignored) {
                // Fall through to the id.
            }
        }
        return Component.literal(code(questId));
    }

    /** Every quest in the book, by chapter then title. Empty without FTB Quests. */
    public static List<Entry> all(boolean client) {
        if (!LOADED) return List.of();
        try {
            return Ftb.all(client);
        } catch (Throwable ignored) {
            return List.of();
        }
    }

    /** FTB's own spelling of a quest id: 16 hex digits. */
    public static String code(long questId) {
        return String.format("%016X", questId);
    }

    /** The id {@link #code} spelled, or {@code 0} for blank or garbage. */
    public static long parse(String code) {
        if (code == null || code.isBlank()) return 0;
        try {
            return Long.parseUnsignedLong(code.strip(), 16);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Isolated holder for the FTB Quests API; only referenced when {@link #LOADED}. */
    private static final class Ftb {

        static boolean isCompleted(Player player, long questId) {
            dev.ftb.mods.ftbquests.quest.BaseQuestFile file = file(player.level().isClientSide());
            if (file == null) return false;
            dev.ftb.mods.ftbquests.quest.Quest quest = file.getQuest(questId);
            if (quest == null) return true; // Deleted from the book: see the class javadoc.
            dev.ftb.mods.ftbquests.quest.TeamData data = file.getTeamData(player).orElse(null);
            return data != null && data.isCompleted(quest);
        }

        static Component title(boolean client, long questId) {
            dev.ftb.mods.ftbquests.quest.BaseQuestFile file = file(client);
            if (file == null) return null;
            dev.ftb.mods.ftbquests.quest.Quest quest = file.getQuest(questId);
            return quest == null ? null : quest.getTitle();
        }

        static List<Entry> all(boolean client) {
            dev.ftb.mods.ftbquests.quest.BaseQuestFile file = file(client);
            if (file == null) return List.of();
            List<Entry> out = new ArrayList<>();
            // Every quest, hidden ones included: this is the admin's picker, not the player's book.
            for (dev.ftb.mods.ftbquests.quest.Quest quest : file.collect(dev.ftb.mods.ftbquests.quest.Quest.class)) {
                Component title = quest.getTitle();
                Component chapter = quest.getChapter() == null ? Component.empty() : quest.getChapter().getTitle();
                String search = (title.getString() + " " + chapter.getString() + " " + quest.getCodeString())
                        .toLowerCase();
                out.add(new Entry(quest.id, title, chapter, search));
            }
            out.sort(Comparator.comparing((Entry e) -> e.chapter().getString())
                    .thenComparing(e -> e.title().getString()));
            return out;
        }

        private static dev.ftb.mods.ftbquests.quest.BaseQuestFile file(boolean client) {
            return dev.ftb.mods.ftbquests.api.FTBQuestsAPI.api().getQuestFile(client);
        }
    }
}
