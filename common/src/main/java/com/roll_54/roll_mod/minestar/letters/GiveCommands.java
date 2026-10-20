package com.roll_54.roll_mod.minestar.letters;

import com.mojang.brigadier.StringReader;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reads what a command reward hands out, when it is a {@code give}, so the letter can show the item
 * instead of the generic command icon.
 *
 * <p>Only {@code give <target> <item>[components] [count]} (or {@code minecraft:give}) is understood.
 * The item is parsed by vanilla's own {@link ItemParser}, so anything the command itself accepts —
 * enchantments, custom names, any component — shows exactly as it will arrive. Every other command
 * is ignored, and a {@code give} that does not parse is treated as if it were not there.
 */
public final class GiveCommands {

    private GiveCommands() {}

    /**
     * Every item the reward's commands give, in order, each count multiplied by how many times the
     * reward runs. Empty when none of its commands is a readable {@code give}.
     */
    public static List<ItemStack> parse(LetterReward reward, @Nullable HolderLookup.Provider registries) {
        if (registries == null || reward.type() != LetterReward.Type.COMMAND) return List.of();
        List<ItemStack> given = new ArrayList<>();
        for (String command : reward.commands()) {
            ItemStack stack = parseOne(command, registries);
            if (stack == null) continue;
            long total = (long) stack.getCount() * Math.max(1, reward.repeat());
            given.add(stack.copyWithCount((int) Math.min(Integer.MAX_VALUE, total)));
        }
        return given;
    }

    /** The stack one {@code give} command hands out, or {@code null} if it is not one. */
    public static @Nullable ItemStack parseOne(String command, HolderLookup.Provider registries) {
        try {
            StringReader reader = new StringReader(command.strip());
            if (reader.canRead() && reader.peek() == '/') reader.skip();
            String name = reader.readUnquotedString().toLowerCase(Locale.ROOT);
            // readUnquotedString stops at ':', so a namespaced command arrives in two pieces.
            if (name.equals("minecraft") && reader.canRead() && reader.peek() == ':') {
                reader.skip();
                name = reader.readUnquotedString().toLowerCase(Locale.ROOT);
            }
            if (!name.equals("give")) return null;

            reader.skipWhitespace();
            skipTarget(reader);
            reader.skipWhitespace();
            if (!reader.canRead()) return null;

            ItemParser.ItemResult item = new ItemParser(registries).parse(reader);
            int count = 1;
            reader.skipWhitespace();
            if (reader.canRead() && StringReader.isAllowedNumber(reader.peek())) count = reader.readInt();
            ItemStack stack = new ItemStack(item.item(), 1, item.components());
            if (stack.isEmpty()) return null;
            return stack.copyWithCount(Math.clamp(count, 1, LetterReward.MAX_STACK));
        } catch (Exception e) {
            // CommandSyntaxException for a malformed give, or anything odd in a hand-written one: the
            // reward just keeps its ordinary icon.
            return null;
        }
    }

    /**
     * Steps over the target — a name, {@code %player%}, or a selector whose {@code [...]} may hold
     * spaces and quoted strings.
     */
    private static void skipTarget(StringReader reader) {
        int depth = 0;
        char quote = 0;
        while (reader.canRead()) {
            char c = reader.peek();
            if (quote != 0) {
                if (c == '\\') reader.skip();
                else if (c == quote) quote = 0;
            } else if (c == '"' || c == '\'') {
                quote = c;
            } else if (c == '[') {
                depth++;
            } else if (c == ']') {
                depth--;
            } else if (Character.isWhitespace(c) && depth <= 0) {
                return;
            }
            reader.skip();
        }
    }
}
