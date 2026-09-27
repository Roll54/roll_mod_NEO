package com.roll_54.roll_mod.minestar.moderation;

import com.roll_54.roll_mod.minestar.moderation.Rule.Severity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The server rules a moderator picks from when muting, warning or banning.
 *
 * <p>Hardcoded from the Minestar rulebook: the ids and grades are here, the wording is in the lang
 * files ({@code rule.roll_mod.<id>}, Ukrainian original and English translation). Changing a rule is
 * a code change and a release, which is deliberate — the rulebook changes rarely, and a rule that
 * exists in the picker but not on the website is worse than one that takes a build to add.
 */
public final class RulesStore {

    private static final Map<String, Rule> RULES = new LinkedHashMap<>();

    static {
        // 1 — general
        add("1.1", Severity.HEAVY);
        add("1.1.1", Severity.HEAVY);
        add("1.2", Severity.HEAVY);
        add("1.3", Severity.HEAVY);
        add("1.4", Severity.HEAVY);
        add("1.5", Severity.HEAVY);
        add("1.6", Severity.MEDIUM);
        add("1.6.1", Severity.MEDIUM);
        add("1.6.2", Severity.MEDIUM);
        add("1.7", Severity.LIGHT);
        add("1.8", Severity.HEAVY);
        add("1.9", Severity.HEAVY);
        add("1.10", Severity.HEAVY);
        add("1.11", Severity.HEAVY);
        add("1.12", Severity.MEDIUM);
        add("1.13", Severity.MEDIUM);
        add("1.14", Severity.NONE);
        add("1.15", Severity.MEDIUM);
        add("1.16", Severity.HEAVY);
        add("1.17", Severity.HEAVY);
        add("1.18", Severity.HEAVY);
        add("1.19", Severity.HEAVY);
        // 2 — chat
        add("2.1", Severity.MEDIUM);
        add("2.2", Severity.MEDIUM);
        add("2.2.1", Severity.LIGHT);
        add("2.2.2", Severity.MEDIUM);
        add("2.3", Severity.LIGHT);
        add("2.4", Severity.LIGHT);
        add("2.5", Severity.HEAVY);
        add("2.6", Severity.HEAVY);
        add("2.7", Severity.LIGHT);
        add("2.8", Severity.LIGHT);
        // 3 — building and claims
        add("3.1", Severity.NONE);
        add("3.2", Severity.MEDIUM);
        add("3.3", Severity.MEDIUM);
        add("3.4", Severity.MEDIUM);
        add("3.5", Severity.LIGHT);
        add("3.6", Severity.MEDIUM);
        add("3.7", Severity.MEDIUM);
        // 4 — names and appearance
        add("4.1", Severity.HEAVY);
        add("4.2", Severity.MEDIUM);
        add("4.3", Severity.MEDIUM);
        add("4.4", Severity.HEAVY);
        add("4.4.1", Severity.HEAVY);
        add("4.5", Severity.HEAVY);
        // 5 — PvP
        add("5.1", Severity.MEDIUM);
        add("5.2", Severity.MEDIUM);
        add("5.3", Severity.LIGHT);
        add("5.4", Severity.MEDIUM);
        add("5.5", Severity.MEDIUM);
        add("5.6", Severity.MEDIUM);
        // 6 — trade
        add("6.1", Severity.HEAVY);
        add("6.2", Severity.LIGHT);
        add("6.3", Severity.MEDIUM);
        add("6.4", Severity.HEAVY);
        add("6.5", Severity.MEDIUM);
        add("6.6", Severity.HEAVY);
    }

    private RulesStore() {}

    private static void add(String id, Severity severity) {
        RULES.put(id, new Rule(id, severity));
    }

    /** Every rule, in rulebook order. */
    public static List<Rule> all() {
        return List.copyOf(RULES.values());
    }

    public static List<String> ids() {
        return List.copyOf(RULES.keySet());
    }

    /** The rule with this id, or {@code null} — including for an id stored before a rule was removed. */
    public static @Nullable Rule byId(String id) {
        return id == null ? null : RULES.get(id);
    }

    /**
     * The rule ids in a stored rule field. One punishment can cite several rules — the moderation
     * tab lets a moderator pick more than one — and they are kept comma-joined ({@code "1.2,2.5"}) in
     * the same single field a lone id always used, so the stores' files did not change shape.
     */
    public static List<String> ids(String joined) {
        if (joined == null || joined.isBlank()) return List.of();
        List<String> out = new ArrayList<>();
        for (String id : joined.split(",")) {
            if (!id.isBlank()) out.add(id.strip());
        }
        return out;
    }

    /** Joins rule ids the way {@link #ids} splits them. */
    public static String join(Collection<String> ids) {
        return String.join(",", ids);
    }

    /**
     * What to show for a stored rule field: empty when none was cited, each rule's label joined with
     * {@code ", "} otherwise, and the bare id for one that names no rule any more, so an old
     * punishment still reads as something rather than as a blank.
     */
    public static Component label(String joined) {
        MutableComponent out = Component.empty();
        List<String> ids = ids(joined);
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) out.append(", ");
            Rule rule = byId(ids.get(i));
            out.append(rule == null ? Component.literal(ids.get(i)) : rule.label());
        }
        return out;
    }
}
