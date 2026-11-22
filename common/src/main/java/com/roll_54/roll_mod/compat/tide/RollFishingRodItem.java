package com.roll_54.roll_mod.compat.tide;

import com.li64.tide.registries.items.TideFishingRodItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A Tide rod of ours with perks of its own ({@link Perk}), listed in its tooltip. The catch perks are applied by
 * {@link TideRodPerks} on Tide's catch event; the bamboo rod's mending in rain or water happens here, while it is held.
 */
public class RollFishingRodItem extends TideFishingRodItem {

    public enum Perk {
        /** Bamboo: mends itself in rain or water. */
        REGROWS,
        /** Bamboo: a catch sometimes brings a piece of Tide bait along. */
        NATURAL_BAIT,
        /** Carbon fibre: a fish is sometimes caught twice. */
        DOUBLE_CATCH,
        /** Carbon fibre: often takes no wear from a catch. */
        SPARES_WEAR,
        /** Meteorite metal: fishes in lava (the tide:lava_fishing_rods tag does it). */
        LAVA,
        /** Meteorite metal: twice the experience from every catch. */
        STAR_XP,
        /** Meteorite metal: at night under the open sky, now and then a meteorite metal nugget. */
        STARFALL,
        /** Electric: shows what bit. */
        SONAR,
        /** Electric: fish bite 50% sooner. */
        FAST,
        /** Electric: runs on energy, never wears out. */
        ELECTRIC,
        /** Electric: the catch minigame is half as hard ({@code FishCatchMinigameMixin}). */
        STEADY
    }

    /** What {@link Perk#STEADY} leaves of the minigame's difficulty: -50%. */
    public static final float STEADY_FACTOR = 0.5f;

    /** Ticks between two points of mending for the bamboo rod. */
    private static final int REGROW_TICKS = 200;

    private final List<Perk> perks;
    /** Name colour and lore, as the mod's other items of the same metal have them; null for none. */
    private com.roll_54.roll_mod.util.TooltipOptions looks;

    /** Gives the rod a coloured name and lore lines ({@code loreitem.roll_mod.<id>.tooltip_lineN}). */
    public RollFishingRodItem looks(com.roll_54.roll_mod.util.TooltipOptions options) {
        this.looks = options;
        return this;
    }

    @Override
    public Component getName(ItemStack stack) {
        return com.roll_54.roll_mod.util.TooltipManager.colorName(super.getName(stack), looks);
    }

    public RollFishingRodItem(int baitSlots, double durability, Properties properties, Perk... perks) {
        super(baitSlots, durability, properties);
        this.perks = List.of(perks);
    }

    public boolean has(Perk perk) {
        return perks.contains(perk);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
        super.inventoryTick(stack, level, holder, slot, selected);
        if (level.isClientSide || !has(Perk.REGROWS) || !stack.isDamaged()) return;
        if (!(holder instanceof Player player) || level.getGameTime() % REGROW_TICKS != 0) return;
        if (player.isInWaterOrRain()) stack.setDamageValue(stack.getDamageValue() - 1);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        com.roll_54.roll_mod.util.TooltipManager.addLore(stack, looks, lines, flag);
        for (Perk perk : perks) {
            lines.add(Component.translatable("item.roll_mod.rod_perk." + perk.name().toLowerCase(java.util.Locale.ROOT))
                    // The stabiliser reads as Tide's diamond line does ("-25% catch difficulty"), in its blue.
                    .withStyle(perk == Perk.STEADY ? ChatFormatting.BLUE : ChatFormatting.GOLD));
        }
    }
}
