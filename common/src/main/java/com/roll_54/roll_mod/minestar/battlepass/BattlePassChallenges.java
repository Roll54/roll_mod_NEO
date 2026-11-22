package com.roll_54.roll_mod.minestar.battlepass;

import com.roll_54.roll_mod.minestar.dailytasks.api.AutoDailyTask;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTask;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskHook;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskIcon;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The battle pass's own challenges: tasks that are not bigger daily tasks but goals of their own, dealt only by the
 * pass ({@link DailyTask#battlePassOnly()}), never on the daily board. They are ordinary {@link DailyTask}s fed by the
 * same hooks, so progress, the rows and the toasts all work as for a daily.
 *
 * <p>The weekly ones ({@code bp_…}) are listed in {@code battlepass.json → weeklyChallenges}; the long seasonal ones
 * ({@code bp_season_…}) in {@code seasonalChallenges}. Their amount is the full requirement: a hard weekly slot asks for
 * twice it ({@code BattlePassService.deal}).
 */
public final class BattlePassChallenges {

    private BattlePassChallenges() {}

    /** One challenge: everything a {@link DailyTask} needs, given to the constructor. */
    public abstract static class Challenge implements DailyTask {
        private final String id;
        private final DailyTaskHook hook;
        private final int amount;
        private final String iconItem;
        private final Predicate<Object> test;
        @Nullable private final String mod;
        private DailyTaskIcon icon;

        protected Challenge(String id, DailyTaskHook hook, int amount, String iconItem, @Nullable String mod,
                            Predicate<Object> test) {
            this.id = id;
            this.hook = hook;
            this.amount = amount;
            this.iconItem = iconItem;
            this.mod = mod;
            this.test = test;
        }

        @Override public String id() { return id; }
        @Override public DailyTaskHook hook() { return hook; }
        @Override public int baseAmount() { return amount; }
        @Override public boolean scalesWithTeam() { return false; }
        @Override public boolean battlePassOnly() { return true; }
        @Override @Nullable public String requiredMod() { return mod; }
        @Override public boolean matches(Object subject) { return test.test(subject); }

        @Override
        public DailyTaskIcon icon() {
            // Resolved on first use: a modded icon (Tide's) may not be registered when the class is made.
            if (icon == null) {
                ResourceLocation rl = ResourceLocation.tryParse(iconItem);
                Item item = rl == null ? Items.AIR : BuiltInRegistries.ITEM.get(rl);
                icon = DailyTaskIcon.of(item == Items.AIR ? Items.PAPER : item);
            }
            return icon;
        }
    }

    /* ------------------------------------------------- predicates -------------------------------------------------- */

    private static Predicate<Object> block(TagKey<net.minecraft.world.level.block.Block> tag) {
        return s -> s instanceof BlockState state && state.is(tag);
    }

    private static Predicate<Object> block(net.minecraft.world.level.block.Block block) {
        return s -> s instanceof BlockState state && state.is(block);
    }

    private static Predicate<Object> mob(EntityType<?>... types) {
        Set<EntityType<?>> set = Set.of(types);
        return s -> s instanceof LivingEntity e && set.contains(e.getType());
    }

    private static Predicate<Object> item(Item... items) {
        Set<Item> set = Set.of(items);
        return s -> s instanceof ItemStack stack && set.contains(stack.getItem());
    }

    private static Predicate<Object> itemTag(TagKey<Item> tag) {
        return s -> s instanceof ItemStack stack && stack.is(tag);
    }

    private static Predicate<Object> itemTag(String tag) {
        return itemTag(TagKey.create(Registries.ITEM, ResourceLocation.parse(tag)));
    }

    private static Predicate<Object> namespace(String ns) {
        return s -> s instanceof ItemStack stack && !stack.isEmpty()
                && ns.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace());
    }

    private static Predicate<Object> anyItem() {
        return s -> s instanceof ItemStack stack && !stack.isEmpty();
    }

    private static Predicate<Object> player() {
        return s -> s instanceof ServerPlayer;
    }

    private static Predicate<Object> anyMob() {
        return s -> s instanceof LivingEntity;
    }

    /* --------------------------------------------------- weekly ---------------------------------------------------- */

    @AutoDailyTask public static final class MineDiamonds extends Challenge {
        public MineDiamonds() { super("bp_mine_diamonds", DailyTaskHook.MINE, 32, "minecraft:diamond_ore", null, block(Tags.Blocks.ORES_DIAMOND)); }
    }
    @AutoDailyTask public static final class MineDeepOres extends Challenge {
        public MineDeepOres() { super("bp_mine_deep_ores", DailyTaskHook.MINE, 256, "minecraft:deepslate_iron_ore", null,
                s -> s instanceof BlockState st && st.is(Tags.Blocks.ORES) && st.is(Tags.Blocks.ORES_IN_GROUND_DEEPSLATE)); }
    }
    @AutoDailyTask public static final class MineDebris extends Challenge {
        public MineDebris() { super("bp_mine_ancient_debris", DailyTaskHook.MINE, 16, "minecraft:ancient_debris", null, block(Blocks.ANCIENT_DEBRIS)); }
    }
    @AutoDailyTask public static final class MineObsidian extends Challenge {
        public MineObsidian() { super("bp_mine_obsidian", DailyTaskHook.MINE, 32, "minecraft:obsidian", null, block(Blocks.OBSIDIAN)); }
    }
    @AutoDailyTask public static final class KillCreepers extends Challenge {
        public KillCreepers() { super("bp_kill_creepers", DailyTaskHook.KILL, 64, "minecraft:gunpowder", null, mob(EntityType.CREEPER)); }
    }
    @AutoDailyTask public static final class KillEndermen extends Challenge {
        public KillEndermen() { super("bp_kill_endermen", DailyTaskHook.KILL, 48, "minecraft:ender_pearl", null, mob(EntityType.ENDERMAN)); }
    }
    /* Trial Chambers (1.21): their breezes and bogged, and the mace from an ominous vault's heavy core. */
    @AutoDailyTask public static final class KillBreezes extends Challenge {
        public KillBreezes() { super("bp_kill_breezes", DailyTaskHook.KILL, 24, "minecraft:breeze_rod", null, mob(EntityType.BREEZE)); }
    }
    @AutoDailyTask public static final class KillBogged extends Challenge {
        public KillBogged() { super("bp_kill_bogged", DailyTaskHook.KILL, 32, "minecraft:trial_key", null, mob(EntityType.BOGGED)); }
    }
    @AutoDailyTask public static final class BreezeSeason extends Challenge {
        public BreezeSeason() { super("bp_season_breezes", DailyTaskHook.KILL, 250, "minecraft:wind_charge", null, mob(EntityType.BREEZE)); }
    }
    @AutoDailyTask public static final class MaceSeason extends Challenge {
        public MaceSeason() { super("bp_season_mace", DailyTaskHook.CRAFT, 1, "minecraft:mace", null, item(Items.MACE)); }
    }
    @AutoDailyTask public static final class KillBlazes extends Challenge {
        public KillBlazes() { super("bp_kill_blazes", DailyTaskHook.KILL, 64, "minecraft:blaze_rod", null, mob(EntityType.BLAZE)); }
    }
    @AutoDailyTask public static final class KillWitherSkeletons extends Challenge {
        public KillWitherSkeletons() { super("bp_kill_wither_skeletons", DailyTaskHook.KILL, 32, "minecraft:wither_skeleton_skull", null, mob(EntityType.WITHER_SKELETON)); }
    }
    @AutoDailyTask public static final class KillPhantoms extends Challenge {
        public KillPhantoms() { super("bp_kill_phantoms", DailyTaskHook.KILL, 6, "minecraft:phantom_membrane", null, mob(EntityType.PHANTOM)); }
    }
    @AutoDailyTask public static final class KillGuardians extends Challenge {
        public KillGuardians() { super("bp_kill_guardians", DailyTaskHook.KILL, 48, "minecraft:prismarine_shard", null, mob(EntityType.GUARDIAN)); }
    }
    @AutoDailyTask public static final class KillSlimes extends Challenge {
        public KillSlimes() { super("bp_kill_slimes", DailyTaskHook.KILL, 96, "minecraft:slime_ball", null, mob(EntityType.SLIME, EntityType.MAGMA_CUBE)); }
    }
    @AutoDailyTask public static final class KillHoglins extends Challenge {
        public KillHoglins() { super("bp_kill_hoglins", DailyTaskHook.KILL, 32, "minecraft:cooked_porkchop", null, mob(EntityType.HOGLIN, EntityType.ZOGLIN)); }
    }
    @AutoDailyTask public static final class KillIllagers extends Challenge {
        public KillIllagers() { super("bp_kill_illagers", DailyTaskHook.KILL, 48, "minecraft:crossbow", null, s -> s instanceof Raider); }
    }
    @AutoDailyTask public static final class KillDrowned extends Challenge {
        public KillDrowned() { super("bp_kill_drowned", DailyTaskHook.KILL, 48, "minecraft:trident", null, mob(EntityType.DROWNED)); }
    }
    @AutoDailyTask public static final class CatchFish extends Challenge {
        public CatchFish() { super("bp_catch_fish", DailyTaskHook.FISH, 40, "minecraft:cod", null, itemTag(ItemTags.FISHES)); }
    }
    @AutoDailyTask public static final class CatchTreasure extends Challenge {
        public CatchTreasure() { super("bp_catch_treasure", DailyTaskHook.FISH, 8, "minecraft:name_tag", null,
                item(Items.ENCHANTED_BOOK, Items.NAME_TAG, Items.SADDLE, Items.NAUTILUS_SHELL)); }
    }
    @AutoDailyTask public static final class BreedHorses extends Challenge {
        public BreedHorses() { super("bp_breed_horses", DailyTaskHook.BREED, 4, "minecraft:golden_carrot", null,
                mob(EntityType.HORSE, EntityType.DONKEY, EntityType.MULE, EntityType.LLAMA)); }
    }
    @AutoDailyTask public static final class BreedBees extends Challenge {
        public BreedBees() { super("bp_breed_bees", DailyTaskHook.BREED, 6, "minecraft:honeycomb", null, mob(EntityType.BEE)); }
    }
    @AutoDailyTask public static final class CraftCakes extends Challenge {
        public CraftCakes() { super("bp_craft_cakes", DailyTaskHook.CRAFT, 6, "minecraft:cake", null, item(Items.CAKE)); }
    }
    @AutoDailyTask public static final class CraftGoldenCarrots extends Challenge {
        public CraftGoldenCarrots() { super("bp_craft_golden_carrots", DailyTaskHook.CRAFT, 32, "minecraft:golden_carrot", null, item(Items.GOLDEN_CARROT)); }
    }
    @AutoDailyTask public static final class CraftRails extends Challenge {
        public CraftRails() { super("bp_craft_rails", DailyTaskHook.CRAFT, 96, "minecraft:rail", null, itemTag(ItemTags.RAILS)); }
    }
    @AutoDailyTask public static final class SmeltGlass extends Challenge {
        public SmeltGlass() { super("bp_smelt_glass", DailyTaskHook.SMELT, 128, "minecraft:glass", null, item(Items.GLASS)); }
    }
    @AutoDailyTask public static final class SmeltIron extends Challenge {
        public SmeltIron() { super("bp_smelt_iron", DailyTaskHook.SMELT, 96, "minecraft:iron_ingot", null, item(Items.IRON_INGOT)); }
    }
    @AutoDailyTask public static final class SmeltBricks extends Challenge {
        public SmeltBricks() { super("bp_smelt_bricks", DailyTaskHook.SMELT, 128, "minecraft:brick", null, item(Items.BRICK)); }
    }
    @AutoDailyTask public static final class EatGoldenApples extends Challenge {
        public EatGoldenApples() { super("bp_eat_golden_apples", DailyTaskHook.EAT, 3, "minecraft:golden_apple", null,
                item(Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE)); }
    }
    @AutoDailyTask public static final class DrinkHoney extends Challenge {
        public DrinkHoney() { super("bp_drink_honey", DailyTaskHook.EAT, 8, "minecraft:honey_bottle", null, item(Items.HONEY_BOTTLE)); }
    }
    @AutoDailyTask public static final class Marathon extends Challenge {
        public Marathon() { super("bp_walk_marathon", DailyTaskHook.DISTANCE, 15000, "minecraft:leather_boots", null, player()); }
    }
    @AutoDailyTask public static final class CookFeasts extends Challenge {
        public CookFeasts() { super("bp_cook_dishes", DailyTaskHook.COOK, 48, "farmersdelight:cooking_pot", "farmersdelight", anyItem()); }
    }
    @AutoDailyTask public static final class DefendVillage extends Challenge {
        public DefendVillage() { super("bp_win_raid", DailyTaskHook.RAID, 2, "minecraft:white_banner", null, player()); }
    }
    @AutoDailyTask public static final class CatchEpicFish extends Challenge {
        public CatchEpicFish() { super("bp_catch_epic_fish", DailyTaskHook.FISH, 2, "tide:great_white_shark", "tide",
                s -> com.roll_54.roll_mod.minestar.dailytasks.TideFish.is(s, com.roll_54.roll_mod.minestar.dailytasks.TideFish.Rarity.VERY_RARE)); }
    }
    @AutoDailyTask public static final class CatchTideFish extends Challenge {
        public CatchTideFish() { super("bp_catch_tide_fish", DailyTaskHook.FISH, 80, "tide:bluegill", "tide", namespace("tide")); }
    }

    /* -------------------------------------------------- seasonal --------------------------------------------------- */

    /**
     * One seasonal per legendary fish of Tide: catch that very fish. Named after the fish (Tide's own name for it), so
     * the season shows which legend it asks for.
     */
    public abstract static class LegendaryOne extends Challenge {
        private final String fish;

        protected LegendaryOne(String fish) {
            super("bp_season_legend_" + fish, DailyTaskHook.FISH, 1, "tide:" + fish, "tide",
                    s -> s instanceof ItemStack stack && !stack.isEmpty()
                            && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(ResourceLocation.fromNamespaceAndPath("tide", fish)));
            this.fish = fish;
        }

        private net.minecraft.network.chat.Component fishName() {
            return net.minecraft.network.chat.Component.translatable("item.tide." + fish);
        }

        @Override
        public net.minecraft.network.chat.Component name() {
            return net.minecraft.network.chat.Component.translatable("dailytask.roll_mod.bp_season_legend.title", fishName());
        }

        @Override
        public net.minecraft.network.chat.Component tooltip(int required) {
            return net.minecraft.network.chat.Component.translatable("dailytask.roll_mod.bp_season_legend.desc", fishName());
        }
    }
    @AutoDailyTask public static final class LegendCoelacanth extends LegendaryOne {
        public LegendCoelacanth() { super("coelacanth"); }
    }
    @AutoDailyTask public static final class LegendShootingStarfish extends LegendaryOne {
        public LegendShootingStarfish() { super("shooting_starfish"); }
    }
    @AutoDailyTask public static final class LegendDevilsHolePupfish extends LegendaryOne {
        public LegendDevilsHolePupfish() { super("devils_hole_pupfish"); }
    }
    @AutoDailyTask public static final class LegendMidasFish extends LegendaryOne {
        public LegendMidasFish() { super("midas_fish"); }
    }
    @AutoDailyTask public static final class LegendAlphaFish extends LegendaryOne {
        public LegendAlphaFish() { super("alpha_fish"); }
    }
    @AutoDailyTask public static final class LegendDragonFish extends LegendaryOne {
        public LegendDragonFish() { super("dragon_fish"); }
    }
    @AutoDailyTask public static final class LegendVoidseeker extends LegendaryOne {
        public LegendVoidseeker() { super("voidseeker"); }
    }
    @AutoDailyTask public static final class LegendaryFish extends Challenge {
        public LegendaryFish() { super("bp_season_legendary_fish", DailyTaskHook.FISH, 5, "tide:shooting_starfish", "tide",
                s -> itemTag("tide:legendary_fish").test(s)
                        || com.roll_54.roll_mod.minestar.dailytasks.TideFish.is(s, com.roll_54.roll_mod.minestar.dailytasks.TideFish.Rarity.LEGENDARY)); }
    }
    @AutoDailyTask public static final class EpicFishSeason extends Challenge {
        public EpicFishSeason() { super("bp_season_epic_fish", DailyTaskHook.FISH, 20, "tide:great_white_shark", "tide",
                s -> com.roll_54.roll_mod.minestar.dailytasks.TideFish.is(s, com.roll_54.roll_mod.minestar.dailytasks.TideFish.Rarity.VERY_RARE)); }
    }
    @AutoDailyTask public static final class TideCollector extends Challenge {
        public TideCollector() { super("bp_season_tide_fish", DailyTaskHook.FISH, 750, "tide:angelfish", "tide", namespace("tide")); }
    }
    @AutoDailyTask public static final class Wither extends Challenge {
        public Wither() { super("bp_season_wither", DailyTaskHook.KILL, 5, "minecraft:nether_star", null, mob(EntityType.WITHER)); }
    }
    @AutoDailyTask public static final class Dragon extends Challenge {
        public Dragon() { super("bp_season_dragon", DailyTaskHook.KILL, 3, "minecraft:dragon_head", null, mob(EntityType.ENDER_DRAGON)); }
    }
    @AutoDailyTask public static final class ElderGuardian extends Challenge {
        public ElderGuardian() { super("bp_season_elder_guardian", DailyTaskHook.KILL, 6, "minecraft:sponge", null, mob(EntityType.ELDER_GUARDIAN)); }
    }
    @AutoDailyTask public static final class Warden extends Challenge {
        public Warden() { super("bp_season_warden", DailyTaskHook.KILL, 10, "minecraft:echo_shard", null, mob(EntityType.WARDEN)); }
    }
    @AutoDailyTask public static final class Netherite extends Challenge {
        public Netherite() { super("bp_season_ancient_debris", DailyTaskHook.MINE, 400, "minecraft:netherite_scrap", null, block(Blocks.ANCIENT_DEBRIS)); }
    }
    @AutoDailyTask public static final class Diamonds extends Challenge {
        public Diamonds() { super("bp_season_diamonds", DailyTaskHook.MINE, 300, "minecraft:diamond", null, block(Tags.Blocks.ORES_DIAMOND)); }
    }
    @AutoDailyTask public static final class Angler extends Challenge {
        public Angler() { super("bp_season_angler", DailyTaskHook.FISH, 1000, "minecraft:fishing_rod", null, anyItem()); }
    }
    @AutoDailyTask public static final class MonsterHunter extends Challenge {
        public MonsterHunter() { super("bp_season_monsters", DailyTaskHook.KILL, 5000, "minecraft:netherite_sword", null, s -> s instanceof Enemy); }
    }
    @AutoDailyTask public static final class Explorer extends Challenge {
        public Explorer() { super("bp_season_explorer", DailyTaskHook.DISTANCE, 200_000, "minecraft:compass", null, player()); }
    }
    @AutoDailyTask public static final class Rancher extends Challenge {
        public Rancher() { super("bp_season_rancher", DailyTaskHook.BREED, 500, "minecraft:wheat", null, anyMob()); }
    }
    @AutoDailyTask public static final class Chef extends Challenge {
        public Chef() { super("bp_season_chef", DailyTaskHook.COOK, 500, "farmersdelight:cooking_pot", "farmersdelight", anyItem()); }
    }
    @AutoDailyTask public static final class Shulkers extends Challenge {
        public Shulkers() { super("bp_season_shulkers", DailyTaskHook.KILL, 100, "minecraft:shulker_shell", null, mob(EntityType.SHULKER)); }
    }
    @AutoDailyTask public static final class Brutes extends Challenge {
        public Brutes() { super("bp_season_brutes", DailyTaskHook.KILL, 40, "minecraft:golden_axe", null, mob(EntityType.PIGLIN_BRUTE)); }
    }
    @AutoDailyTask public static final class GoldenGlutton extends Challenge {
        public GoldenGlutton() { super("bp_season_golden_apples", DailyTaskHook.EAT, 48, "minecraft:enchanted_golden_apple", null,
                item(Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE)); }
    }
    @AutoDailyTask public static final class RaidHero extends Challenge {
        public RaidHero() { super("bp_season_raids", DailyTaskHook.RAID, 10, "minecraft:totem_of_undying", null, player()); }
    }
}
