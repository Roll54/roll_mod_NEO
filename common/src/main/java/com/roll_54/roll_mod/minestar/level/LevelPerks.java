package com.roll_54.roll_mod.minestar.level;

import com.roll_54.roll_mod.config.MyConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * What a player's Minestar level gives, ported from Eden_LD: rules of the form "{@code amount} of {@code perk} at
 * level {@code from}, then every {@code every} levels (0 = once), up to {@code until} (0 = no end)", from the config
 * ({@code levelRewards.rules}, one {@code perk:from:every:amount:until} per line). Shared by the server, which applies
 * them, and the client, which shows them on the home tab.
 */
public final class LevelPerks {

    public enum Kind {
        /** Favourite emoji slots in the chat's emoji picker. */
        EMOJI_SLOTS,
        /** Saved sticker slots (Discord stickers). */
        STICKER_SLOTS,
        /** Open Parties and Claims bonus chunk claims, on the player's own limit. */
        CLAIMS,
        /** Free daily task rerolls per day. */
        DAILY_REROLLS;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        @Nullable
        public static Kind of(String id) {
            for (Kind kind : values()) if (kind.id().equals(id)) return kind;
            return null;
        }
    }

    public record Rule(String perk, int from, int every, int amount, int until) {

        /** {@code perk:from:every:amount:until} (the last ones may be left out), or null when it does not parse. */
        @Nullable
        public static Rule parse(String line) {
            String[] parts = line.trim().split(":");
            if (parts.length < 2 || Kind.of(parts[0].trim()) == null) return null;
            try {
                int from = Integer.parseInt(parts[1].trim());
                int every = parts.length > 2 ? Integer.parseInt(parts[2].trim()) : 0;
                int amount = parts.length > 3 ? Integer.parseInt(parts[3].trim()) : 1;
                int until = parts.length > 4 ? Integer.parseInt(parts[4].trim()) : 0;
                return amount == 0 ? null : new Rule(parts[0].trim(), from, every, amount, until);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        public String write() {
            return perk + ":" + from + ":" + every + ":" + amount + ":" + until;
        }
    }

    public record Gain(int level, Kind kind, int amount) {}

    private LevelPerks() {}

    /** The server's rules (config). */
    public static List<Rule> rules() {
        List<Rule> rules = new ArrayList<>();
        if (MyConfig.INSTANCE == null) return rules;
        for (String line : MyConfig.INSTANCE.levelRewards.rules) {
            Rule rule = line == null ? null : Rule.parse(line);
            if (rule != null) rules.add(rule);
        }
        return rules;
    }

    public static int total(Kind kind, int level) {
        return total(rules(), kind, level);
    }

    public static int total(List<Rule> rules, Kind kind, int level) {
        int sum = 0;
        for (Rule rule : rules) {
            if (kind.id().equals(rule.perk())) sum += rule.amount() * count(rule, level);
        }
        return Math.max(0, sum);
    }

    /** Every perk's total at this level (perks with nothing left out). */
    public static Map<Kind, Integer> totals(List<Rule> rules, int level) {
        Map<Kind, Integer> totals = new EnumMap<>(Kind.class);
        for (Kind kind : Kind.values()) {
            int total = total(rules, kind, level);
            if (total != 0) totals.put(kind, total);
        }
        return totals;
    }

    /** How often the rule fired up to and including this level. */
    private static int count(Rule rule, int level) {
        int from = Math.max(1, rule.from());
        int top = rule.until() > 0 ? Math.min(level, rule.until()) : level;
        if (top < from) return 0;
        return rule.every() > 0 ? (top - from) / rule.every() + 1 : 1;
    }

    /** What reaching exactly this level gives. */
    public static List<Gain> at(List<Rule> rules, int level) {
        List<Gain> gains = new ArrayList<>();
        for (Kind kind : Kind.values()) {
            int gained = total(rules, kind, level) - total(rules, kind, level - 1);
            if (gained != 0) gains.add(new Gain(level, kind, gained));
        }
        return gains;
    }

    /** The next level above {@code level} (searched up to 200 further) that gives anything, or -1. */
    public static int nextPerkLevel(List<Rule> rules, int level) {
        for (int next = level + 1; next <= level + 200; next++) {
            if (!at(rules, next).isEmpty()) return next;
        }
        return -1;
    }

    /** "+1 emoji slot" (translated on the client). */
    public static Component describe(Kind kind, int amount) {
        return Component.translatable("gui.roll_mod.level.perk." + kind.id(), (amount > 0 ? "+" : "") + amount);
    }

    /** Several gains as one line: "+1 emoji slot, +5 claims". */
    public static Component describe(Map<Kind, Integer> gains) {
        MutableComponent line = Component.empty();
        boolean first = true;
        for (Map.Entry<Kind, Integer> gain : gains.entrySet()) {
            if (!first) line.append(", ");
            line.append(describe(gain.getKey(), gain.getValue()));
            first = false;
        }
        return line;
    }

    /** What exactly this level gives, as one line. */
    public static Component describeAt(List<Rule> rules, int level) {
        Map<Kind, Integer> gains = new EnumMap<>(Kind.class);
        at(rules, level).forEach(gain -> gains.merge(gain.kind(), gain.amount(), Integer::sum));
        return describe(gains);
    }
}
