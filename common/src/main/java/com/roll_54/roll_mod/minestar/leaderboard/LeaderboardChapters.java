package com.roll_54.roll_mod.minestar.leaderboard;

import com.roll_54.roll_mod.registry.ModConfigs;

import java.util.ArrayList;
import java.util.List;

/**
 * The quest stage board's stages ({@link LeaderboardCategory#CHAPTERS}): which FTB Quests quests mark a stage, highest
 * first, and the name each is shown by ("HV", "IV"). A team stands at the highest stage whose quest it completed.
 * ({@link Stage#chapter} holds the quest id; a chapter id saved before stages were quests still works.) Kept in {@code MyConfig.hub.leaderboardChapters} as
 * {@code CHAPTER_ID=Name;CHAPTER_ID=Name}, set from the moderation tab's Settings.
 */
public final class LeaderboardChapters {

    /** A board value is {@code stage * STAGE + percent}: the stage first, progress in it second. */
    public static final long STAGE = 1000;
    public static final int MAX_STAGES = 32;
    public static final int MAX_LABEL = 24;

    /** One stage: its quest's id (or, saved earlier, a chapter's), and its name on the board (blank: the quest title). */
    public record Stage(long chapter, String label) {}

    private LeaderboardChapters() {}

    /** The stages the server is set to, highest first. */
    public static List<Stage> configured() {
        return ModConfigs.MAIN == null ? List.of() : parse(ModConfigs.MAIN.hub.leaderboardChapters);
    }

    /** Reads {@code ID=Name;ID=Name}; malformed and repeated entries are skipped. */
    public static List<Stage> parse(String text) {
        List<Stage> out = new ArrayList<>();
        if (text == null || text.isBlank()) return out;
        for (String part : text.split(";")) {
            if (out.size() >= MAX_STAGES) break;
            int eq = part.indexOf('=');
            String id = (eq < 0 ? part : part.substring(0, eq)).strip();
            String label = eq < 0 ? "" : clean(part.substring(eq + 1));
            long chapter;
            try {
                chapter = Long.parseUnsignedLong(id, 16);
            } catch (NumberFormatException e) {
                continue;
            }
            if (chapter == 0 || out.stream().anyMatch(s -> s.chapter() == chapter)) continue;
            out.add(new Stage(chapter, label));
        }
        return out;
    }

    public static String write(List<Stage> stages) {
        StringBuilder out = new StringBuilder();
        for (Stage stage : stages) {
            if (!out.isEmpty()) out.append(';');
            out.append(String.format("%016X", stage.chapter())).append('=').append(clean(stage.label()));
        }
        return out.toString();
    }

    /** A name as stored: no separators, trimmed, at most {@link #MAX_LABEL} characters. */
    public static String clean(String label) {
        String text = label == null ? "" : label.replace(";", "").replace("=", "").strip();
        return text.length() > MAX_LABEL ? text.substring(0, MAX_LABEL) : text;
    }
}
