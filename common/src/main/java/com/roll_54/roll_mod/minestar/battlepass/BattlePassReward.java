package com.roll_54.roll_mod.minestar.battlepass;

import com.roll_54.roll_mod.cosmetics.ItemSkinDefinition;
import com.roll_54.roll_mod.cosmetics.ItemSkinRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * A battle-pass reward as written in {@code battlepass.json}, read the same on both sides:
 * <ul>
 *   <li>{@code namespace:item*count} — an item;</li>
 *   <li>{@code money:amount} — main currency;</li>
 *   <li>{@code skin:namespace:id} — unlocks a cosmetic skin;</li>
 *   <li>{@code choice:A|B|C} — the player picks one of the listed rewards (each one of the kinds above) when claiming.</li>
 * </ul>
 */
public final class BattlePassReward {

    public static final String MONEY = "money:";
    public static final String SKIN = "skin:";
    public static final String CHOICE = "choice:";
    /** Options in a choice; more is a menu, not a reward. */
    public static final int MAX_OPTIONS = 6;

    public enum Kind { ITEM, MONEY, SKIN, CHOICE }

    /** The starcoins' colour (the Home tab's). */
    public static final int STARCOIN_COLOR = 0xD7A1F9;

    private BattlePassReward() {}

    public static Kind kind(@Nullable String spec) {
        if (spec == null) return Kind.ITEM;
        if (spec.startsWith(MONEY)) return Kind.MONEY;
        if (spec.startsWith(SKIN)) return Kind.SKIN;
        if (spec.startsWith(CHOICE)) return Kind.CHOICE;
        return Kind.ITEM;
    }

    /** A choice's options; any other reward is its own single option. */
    public static List<String> options(@Nullable String spec) {
        if (spec == null || spec.isBlank()) return List.of();
        if (kind(spec) != Kind.CHOICE) return List.of(spec);
        List<String> out = new ArrayList<>();
        for (String part : spec.substring(CHOICE.length()).split("\\|")) {
            String s = part.strip();
            if (!s.isEmpty() && kind(s) != Kind.CHOICE) out.add(s);
            if (out.size() == MAX_OPTIONS) break;
        }
        return out;
    }

    public static String choice(List<String> options) {
        return CHOICE + String.join("|", options);
    }

    public static long money(@Nullable String spec) {
        if (spec == null || !spec.startsWith(MONEY)) return 0;
        try {
            return Long.parseLong(spec.substring(MONEY.length()).strip());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** {@code skin:random}: a random skin the player does not own, opened like a case. */
    public static boolean randomSkin(@Nullable String spec) {
        return (SKIN + "random").equals(spec);
    }

    @Nullable
    public static ItemSkinDefinition skin(@Nullable String spec) {
        if (spec == null || !spec.startsWith(SKIN)) return null;
        ResourceLocation id = ResourceLocation.tryParse(spec.substring(SKIN.length()).strip());
        return id == null ? null : ItemSkinRegistry.get(id);
    }

    /** The item an item reward names (empty for the other kinds or an unknown id). */
    public static ItemStack stack(@Nullable String spec) {
        return kind(spec) == Kind.ITEM ? BattlePassService.stack(spec) : ItemStack.EMPTY;
    }

    /** One line naming the reward, for tooltips and the editor. */
    public static Component describe(@Nullable String spec) {
        if (spec == null || spec.isBlank()) return Component.translatable("gui.roll_mod.battlepass.reward.none").withStyle(ChatFormatting.GRAY);
        return switch (kind(spec)) {
            // In the starcoins' own colour, as the hub shows them everywhere.
            case MONEY -> Component.translatable("gui.roll_mod.battlepass.money", money(spec)).withColor(STARCOIN_COLOR);
            case SKIN -> {
                if (randomSkin(spec)) yield Component.translatable("gui.roll_mod.battlepass.reward.random_skin")
                        .withStyle(ChatFormatting.LIGHT_PURPLE);
                ItemSkinDefinition skin = skin(spec);
                yield skin == null ? Component.literal(spec).withStyle(ChatFormatting.RED)
                        : Component.translatable("gui.roll_mod.battlepass.reward.skin", skin.displayName()).withStyle(ChatFormatting.LIGHT_PURPLE);
            }
            case CHOICE -> Component.translatable("gui.roll_mod.battlepass.reward.choice", options(spec).size())
                    .withStyle(ChatFormatting.AQUA);
            case ITEM -> {
                ItemStack stack = stack(spec);
                yield stack.isEmpty() ? Component.literal(spec).withStyle(ChatFormatting.RED)
                        : Component.literal(stack.getCount() + " × ").append(stack.getHoverName());
            }
        };
    }
}
