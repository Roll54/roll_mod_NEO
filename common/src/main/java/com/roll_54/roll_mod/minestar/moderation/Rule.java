package com.roll_54.roll_mod.minestar.moderation;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * One server rule, as a moderator cites it when muting, warning or banning someone.
 *
 * <p>{@code id} is the number from the server's rulebook — {@code "1.2"} — and is what a punishment
 * stores. The wording lives in the lang files under {@code rule.roll_mod.<id>}, so it is shown in
 * the reader's own language and a rewording never orphans a punishment that cited it.
 */
public record Rule(String id, Severity severity) {

    /** The rulebook's punishment grade; {@link #NONE} for the rules that are principles, not offences. */
    public enum Severity {
        NONE(ChatFormatting.GRAY),
        LIGHT(ChatFormatting.YELLOW),
        MEDIUM(ChatFormatting.GOLD),
        HEAVY(ChatFormatting.RED);

        private final ChatFormatting color;

        Severity(ChatFormatting color) {
            this.color = color;
        }

        public Component label() {
            return Component.translatable("rule.roll_mod.severity." + name().toLowerCase())
                    .withStyle(color);
        }
    }

    public MutableComponent text() {
        return Component.translatable("rule.roll_mod." + id);
    }

    /** {@code 1.2 — Griefing claims is strictly forbidden.} */
    public MutableComponent label() {
        return Component.literal(id + " — ").append(text());
    }
}
