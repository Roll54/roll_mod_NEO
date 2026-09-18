package com.roll_54.roll_mod.minestar.hub;

import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

/**
 * How far the player's team is through the FTB Quests book, for the hub's home screen.
 *
 * <p>Counts are split by whether a quest actually gates progression: {@code isOptionalForProgression}
 * rather than the bare {@code isOptional()} flag, so repeatable quests and quests excluded by taking
 * the other branch of a questline do not drag the "required" total up with work nobody has to do.
 *
 * <p>FTB Quests is a soft dependency, so every type it owns is confined to the nested {@link Ftb}
 * holder, which is only loaded when the mod is present. Without it every count is zero and the home
 * screen simply shows 0/0.
 */
public final class QuestProgress {

    /**
     * @param requiredDone quests finished that count toward progression
     * @param requiredTotal quests that count toward progression at all
     * @param optionalDone finished quests nobody was required to do
     * @param optionalTotal optional quests in the book
     */
    public record Counts(int requiredDone, int requiredTotal, int optionalDone, int optionalTotal) {
        public static final Counts NONE = new Counts(0, 0, 0, 0);
    }

    private static final boolean LOADED = ModList.get().isLoaded("ftbquests");

    private QuestProgress() {}

    public static Counts of(Player player) {
        if (!LOADED) return Counts.NONE;
        try {
            return Ftb.count(player);
        } catch (Throwable ignored) {
            // No quest file loaded yet, or the player has no team — nothing to report.
            return Counts.NONE;
        }
    }

    /** Isolated holder for the FTB Quests API; only referenced when {@link #LOADED}. */
    private static final class Ftb {

        static Counts count(Player player) {
            dev.ftb.mods.ftbquests.quest.BaseQuestFile file =
                    dev.ftb.mods.ftbquests.api.FTBQuestsAPI.api()
                            .getQuestFile(player.level().isClientSide());
            if (file == null) return Counts.NONE;

            dev.ftb.mods.ftbquests.quest.TeamData data = file.getTeamData(player).orElse(null);
            if (data == null) return Counts.NONE;

            int requiredDone = 0, requiredTotal = 0, optionalDone = 0, optionalTotal = 0;
            for (dev.ftb.mods.ftbquests.quest.Quest quest
                    : file.collect(dev.ftb.mods.ftbquests.quest.Quest.class)) {
                boolean done = data.isCompleted(quest);
                if (quest.isOptionalForProgression(data)) {
                    optionalTotal++;
                    if (done) optionalDone++;
                } else {
                    requiredTotal++;
                    if (done) requiredDone++;
                }
            }
            return new Counts(requiredDone, requiredTotal, optionalDone, optionalTotal);
        }
    }
}
