package com.roll_54.roll_mod.minestar.dailytasks;

import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTask;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskHook;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskIcon;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * A task per Tide fish: a daily for every common, uncommon and rare fish ({@code tide_fish_<fish>}), and a weekly battle
 * pass challenge for every very rare ("epic") one ({@code bp_epic_<fish>}). Added to the task registry by hand rather
 * than as 95 annotated classes; only when Tide is loaded. Each is named after its fish, in Tide's own words.
 */
public final class TideFishTasks {

    private TideFishTasks() {}

    /** Every task, or none without Tide. */
    public static List<DailyTask> all() {
        if (!net.neoforged.fml.ModList.get().isLoaded("tide")) return List.of();
        List<DailyTask> out = new ArrayList<>();
        for (String fish : TideFish.fish(TideFish.Rarity.COMMON)) out.add(new OneFish("tide_fish_" + fish, fish, 2, TideFish.Rarity.COMMON, false));
        for (String fish : TideFish.fish(TideFish.Rarity.UNCOMMON)) out.add(new OneFish("tide_fish_" + fish, fish, 1, TideFish.Rarity.UNCOMMON, false));
        for (String fish : TideFish.fish(TideFish.Rarity.RARE)) out.add(new OneFish("tide_fish_" + fish, fish, 1, TideFish.Rarity.RARE, false));
        for (String fish : TideFish.fish(TideFish.Rarity.VERY_RARE)) out.add(new OneFish("bp_epic_" + fish, fish, 1, TideFish.Rarity.VERY_RARE, true));
        return out;
    }

    /** The weekly challenges' ids, for the battle pass's pool. */
    public static List<String> epicIds() {
        return TideFish.fish(TideFish.Rarity.VERY_RARE).stream().map(f -> "bp_epic_" + f).toList();
    }

    /** Catch this one fish. */
    private static final class OneFish implements DailyTask {
        private final String id;
        private final String fish;
        private final int amount;
        private final TideFish.Rarity rarity;
        private final boolean passOnly;
        private final ResourceLocation item;
        private DailyTaskIcon icon;

        OneFish(String id, String fish, int amount, TideFish.Rarity rarity, boolean passOnly) {
            this.id = id;
            this.fish = fish;
            this.amount = amount;
            this.rarity = rarity;
            this.passOnly = passOnly;
            this.item = ResourceLocation.fromNamespaceAndPath("tide", fish);
        }

        @Override public String id() { return id; }
        @Override public DailyTaskHook hook() { return DailyTaskHook.FISH; }
        @Override public int baseAmount() { return amount; }
        @Override public boolean battlePassOnly() { return passOnly; }
        @Override public String requiredMod() { return "tide"; }
        /** One fish among dozens: weighted so all of them together come up about as often as one ordinary task. */
        @Override public int weight() { return 1; }

        @Override
        public boolean matches(Object subject) {
            return subject instanceof ItemStack stack && !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(item);
        }

        @Override
        public Component name() {
            return Component.translatable("dailytask.roll_mod.tide_fish.title", Component.translatable("item.tide." + fish));
        }

        @Override
        public Component tooltip(int required) {
            return Component.translatable("dailytask.roll_mod.tide_fish.desc", required, Component.translatable("item.tide." + fish),
                    Component.translatable("dailytask.roll_mod.tide_fish.rarity." + rarity.name().toLowerCase(java.util.Locale.ROOT)));
        }

        @Override
        public DailyTaskIcon icon() {
            if (icon == null) {
                var resolved = BuiltInRegistries.ITEM.get(item);
                icon = DailyTaskIcon.of(resolved == Items.AIR ? Items.COD : resolved);
            }
            return icon;
        }
    }
}
