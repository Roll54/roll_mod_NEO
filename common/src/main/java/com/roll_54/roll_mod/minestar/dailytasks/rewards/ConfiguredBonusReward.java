package com.roll_54.roll_mod.minestar.dailytasks.rewards;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.battlepass.BattlePassReward;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyReward;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskIcon;
import com.roll_54.roll_mod.minestar.dailytasks.api.RewardRange;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A daily bonus defined in {@code minestar/daily_rewards.json} (the Moderation tab's Dailies editor) rather than in
 * code: a weight and a payout list. Not {@code @AutoDailyReward}: {@code DailyRewardRegistry} builds these from the
 * config, on the server and, from the synced copy, on every client.
 *
 * <p>A payout line is {@code namespace:item*min-max} (or {@code *count}, or no count for one) or
 * {@code money:amount} for starcoins. Lines that name nothing known are skipped.
 */
public final class ConfiguredBonusReward implements DailyReward {

    /** One payout line: items in a range, or starcoins. */
    private record Part(@Nullable RewardRange items, long money) {}

    private static final DailyTaskIcon STARCOIN_ICON =
            DailyTaskIcon.of(RollMod.id("textures/item/lp.png"), Items.GOLD_NUGGET);

    private final String id;
    private final int weight;
    private final List<Part> parts;

    public ConfiguredBonusReward(String id, int weight, List<String> payout) {
        this.id = id;
        this.weight = weight;
        List<Part> parsed = new ArrayList<>();
        for (String line : payout) {
            Part part = parse(line);
            if (part != null) parsed.add(part);
        }
        this.parts = List.copyOf(parsed);
    }

    /** {@code null} for a line that names nothing known. */
    @Nullable
    private static Part parse(@Nullable String line) {
        if (line == null || line.isBlank()) return null;
        line = line.strip();
        if (BattlePassReward.kind(line) == BattlePassReward.Kind.MONEY) {
            long amount = BattlePassReward.money(line);
            return amount > 0 ? new Part(null, amount) : null;
        }
        int star = line.lastIndexOf('*');
        String itemId = (star < 0 ? line : line.substring(0, star)).strip();
        int min = 1, max = 1;
        if (star >= 0) {
            String amount = line.substring(star + 1).strip();
            int dash = amount.indexOf('-');
            try {
                min = Integer.parseInt((dash < 0 ? amount : amount.substring(0, dash)).strip());
                max = dash < 0 ? min : Integer.parseInt(amount.substring(dash + 1).strip());
            } catch (NumberFormatException e) {
                min = max = 1;
            }
        }
        min = Math.max(1, min);
        max = Math.max(min, max);
        ResourceLocation rl = ResourceLocation.tryParse(itemId);
        if (rl == null) return null;
        Optional<Holder.Reference<Item>> item = BuiltInRegistries.ITEM.getHolder(rl);
        if (item.isEmpty() || item.get().value() == Items.AIR) return null;
        return new Part(RewardRange.of(item.get(), min, max), 0);
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public int weight() {
        return Math.max(1, weight);
    }

    /** A weight of 0 (or a payout with nothing in it) keeps the bonus configured but out of the draw. */
    @Override
    public boolean drawable() {
        return weight > 0 && !parts.isEmpty();
    }

    @Override
    public DailyTaskIcon icon() {
        if (parts.isEmpty()) return DailyTaskIcon.of(Items.CHEST);
        Part first = parts.getFirst();
        // Several things in one bonus: a chest stands for the lot, the strip underneath lists them.
        if (parts.size() > 1) return DailyTaskIcon.of(Items.CHEST);
        return first.items() != null ? first.items().itemIcon() : STARCOIN_ICON;
    }

    @Override
    public List<DailyTaskIcon> icons() {
        List<DailyTaskIcon> out = new ArrayList<>();
        for (Part part : parts) out.add(part.items() != null ? part.items().icon() : STARCOIN_ICON);
        return out.isEmpty() ? List.of(icon()) : out;
    }

    /** The built-in reward's title when the id is one of theirs (or has a lang key), otherwise a generic one. */
    @Override
    public Component name() {
        String key = titleKey();
        return Component.translatable(Language.getInstance().has(key) ? key : "dailyreward.roll_mod.custom.title");
    }

    /** Written from the payout itself, so the hover always matches what is paid. */
    @Override
    public Component tooltip() {
        MutableComponent out = Component.empty();
        boolean first = true;
        for (Part part : parts) {
            if (!first) out.append(", ");
            first = false;
            if (part.items() == null) {
                out.append(Component.translatable("gui.roll_mod.battlepass.money", part.money()));
            } else {
                RewardRange r = part.items();
                String amount = r.min() == r.max() ? String.valueOf(r.min()) : r.min() + "-" + r.max();
                out.append(Component.literal(amount + " × ")).append(r.item().value().getDescription());
            }
        }
        return out;
    }

    @Override
    public void grant(ServerPlayer player) {
        for (Part part : parts) {
            if (part.items() != null) part.items().grant(player);
            else StarcoinsReward.deposit(player, part.money(), id);
        }
    }
}
