package com.roll_54.roll_mod.compat.tide;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;

/**
 * Our rods' catch perks, on the catch event Tide posts. Tide does not read the event back (it hands out its own copy of
 * the catch), so nothing here changes the catch itself: extras are spawned at the player, and wear is given back a
 * tick later, after Tide has worn the rod.
 *
 * <p>Registered on the game bus only when Tide is loaded ({@link TideRods#register}): it names our Tide rod class.
 */
public final class TideRodPerks {

    private static final float NATURAL_BAIT = 0.20f;
    private static final float DOUBLE_CATCH = 0.25f;
    private static final float SPARES_WEAR = 0.50f;
    private static final float STARFALL = 0.05f;
    /**
     * The meteorite hook's chance per catch of a legendary Tide fish on top of the catch (one of the seven, at random),
     * on any rod. Before, it was a meteorite metal nugget; that was replaced.
     */
    static final float METEORITE_HOOK = 0.05f;

    private TideRodPerks() {}

    @SubscribeEvent
    public static void onFished(ItemFishedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        // The hook works on any Tide rod, ours or not, so it is checked before the rod.
        var hook = com.li64.tide.registries.entities.misc.fishing.HookAccessor.getHook(player);
        if (hook != null && TideRods.METEORITE_HOOK != null && hook.getHook().is(TideRods.METEORITE_HOOK.get())
                && player.getRandom().nextFloat() < METEORITE_HOOK) {
            java.util.List<String> legends = com.roll_54.roll_mod.minestar.dailytasks.TideFish.fish(
                    com.roll_54.roll_mod.minestar.dailytasks.TideFish.Rarity.LEGENDARY);
            give(player, item("tide", legends.get(player.getRandom().nextInt(legends.size()))), 1);
        }
        ItemStack rod = player.getMainHandItem().getItem() instanceof RollFishingRodItem
                ? player.getMainHandItem() : player.getOffhandItem();
        if (!(rod.getItem() instanceof RollFishingRodItem item)) return;
        ServerLevel level = player.serverLevel();
        if (item instanceof ElectricFishingRodItem electric) electric.payForCatch(rod);
        var random = player.getRandom();

        if (item.has(RollFishingRodItem.Perk.NATURAL_BAIT) && random.nextFloat() < NATURAL_BAIT) {
            give(player, item("tide", "bait"), 1);
        }
        if (item.has(RollFishingRodItem.Perk.DOUBLE_CATCH) && random.nextFloat() < DOUBLE_CATCH) {
            for (ItemStack caught : event.getDrops()) {
                if (caught.is(ItemTags.FISHES) || isTideFish(caught)) give(player, caught.copy());
            }
        }
        if (item.has(RollFishingRodItem.Perk.STAR_XP)) {
            level.addFreshEntity(new ExperienceOrb(level, player.getX(), player.getY() + 0.5, player.getZ(),
                    1 + random.nextInt(4)));
        }
        if (item.has(RollFishingRodItem.Perk.STARFALL) && random.nextFloat() < STARFALL && starryNight(level, player.blockPosition())) {
            give(player, item("roll_mod", "meteorite_metal_nugget"), 1);
        }
        if (item.has(RollFishingRodItem.Perk.SPARES_WEAR) && random.nextFloat() < SPARES_WEAR) {
            ItemStack worn = rod;
            int before = worn.getDamageValue();
            // Tide wears the rod after posting this event: give it back once it has.
            player.server.execute(() -> {
                if (worn.getDamageValue() > before) worn.setDamageValue(before);
            });
        }
    }

    private static boolean isTideFish(ItemStack stack) {
        return "tide".equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace())
                && java.util.Arrays.stream(com.roll_54.roll_mod.minestar.dailytasks.TideFish.Rarity.values())
                        .anyMatch(r -> com.roll_54.roll_mod.minestar.dailytasks.TideFish.is(stack, r));
    }

    /** Night, and the sky open above — or the End, where it is always both. */
    private static boolean starryNight(Level level, BlockPos pos) {
        if (level.dimension() == Level.END) return true;
        return level.dimensionType().hasSkyLight() && level.isNight() && level.canSeeSky(pos.above());
    }

    private static Item item(String ns, String path) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(ns, path));
    }

    private static void give(ServerPlayer player, Item item, int count) {
        if (item == Items.AIR) return;
        give(player, new ItemStack(item, count));
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack) && !stack.isEmpty()) {
            player.level().addFreshEntity(new ItemEntity(player.level(), player.getX(), player.getY() + 0.5, player.getZ(), stack));
        }
    }
}
