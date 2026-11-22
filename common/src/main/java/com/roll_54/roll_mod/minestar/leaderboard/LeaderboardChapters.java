package com.roll_54.roll_mod.minestar.leaderboard;

import com.roll_54.roll_mod.minestar.hub.QuestGate;
import com.roll_54.roll_mod.registry.ModConfigs;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The quest stage board's stages, stored in {@code hub.leaderboardChapters} as {@code ID=Name;ID=Name}, highest first.
 * ID is FTB's 16-hex-digit spelling of a quest (or, for stages saved before stages were quests, a chapter); Name is
 * what the board shows, blank for the quest's own title.
 */
public final class LeaderboardChapters {

    /** A board value per stage: the stage's place counted from the bottom, times this. */
    public static final long STAGE = 1000L;
    /** Stages the moderation tab lets one add; parse drops any past this. */
    public static final int MAX_STAGES = 16;
    /** Longest name kept for a stage. */
    private static final int MAX_LABEL = 24;

    /** One stage: the quest (or legacy chapter) id it is tied to and the name the board shows it by. */
    public record Stage(long chapter, String label) {}

    private LeaderboardChapters() {}

    /** The stages the server config holds now. */
    public static List<Stage> configured() {
        var config = ModConfigs.MAIN;
        return parse(config == null ? "" : config.hub.leaderboardChapters);
    }

    /** The stages {@code value} spells; malformed, zero and repeated ids are skipped. */
    public static List<Stage> parse(String value) {
        List<Stage> stages = new ArrayList<>();
        if (value == null || value.isBlank()) return stages;
        for (String entry : value.split(";")) {
            if (stages.size() >= MAX_STAGES) break;
            int eq = entry.indexOf('=');
            long id = QuestGate.parse(eq < 0 ? entry : entry.substring(0, eq));
            if (id == 0 || stages.stream().anyMatch(s -> s.chapter() == id)) continue;
            stages.add(new Stage(id, clean(eq < 0 ? "" : entry.substring(eq + 1))));
        }
        return stages;
    }

    /** The config string for {@code stages}. */
    public static String write(List<Stage> stages) {
        return stages.stream()
                .map(s -> QuestGate.code(s.chapter()) + "=" + clean(s.label()))
                .collect(Collectors.joining(";"));
    }

    /** A typed name made safe to store: trimmed, without the separators, at most {@value #MAX_LABEL} characters. */
    public static String clean(String label) {
        if (label == null) return "";
        String cleaned = label.replaceAll("[;=\\r\\n]", "").strip();
        return cleaned.length() > MAX_LABEL ? cleaned.substring(0, MAX_LABEL).strip() : cleaned;
    }
}
