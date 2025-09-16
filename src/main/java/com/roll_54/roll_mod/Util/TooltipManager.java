package com.roll_54.roll_mod.Util;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;

import java.util.List;
public class TooltipManager {
    //Sword
    public static class TooltipSwordItem extends SwordItem {
        private final int tooltipLines;
        private final Integer nameColorHex;
        private final Integer loreColorHex;

        public TooltipSwordItem(Tier tier, float attackDamage, float attackSpeed,
                                Properties props, int tooltipLines,
                                Integer nameColorHex, Integer loreColorHex) {
            super(tier, props.attributes(SwordItem.createAttributes(tier, attackDamage, attackSpeed)));
            if (tooltipLines < 1) throw new IllegalArgumentException("tooltipLines must be >= 1");
            this.tooltipLines = tooltipLines;
            this.nameColorHex = nameColorHex;
            this.loreColorHex = loreColorHex;
        }

        @Override
        public Component getName(ItemStack stack) {
            return TooltipStyler.colorName(super.getName(stack), nameColorHex);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx,
                                    List<Component> tooltip, TooltipFlag flag) {
            TooltipStyler.addLore(stack, tooltipLines, loreColorHex, tooltip, flag);
        }
    }
    //Pickaxe
    public static class TooltipPickaxeItem extends PickaxeItem {
        private final int tooltipLines;
        private final Integer nameColorHex;
        private final Integer loreColorHex;

        public TooltipPickaxeItem(Tier tier, float attackDamage, float attackSpeed,
                                  Properties props, int tooltipLines,
                                  Integer nameColorHex, Integer loreColorHex) {
            super(tier, props.attributes(PickaxeItem.createAttributes(tier, attackDamage, attackSpeed)));
            if (tooltipLines < 1) throw new IllegalArgumentException("tooltipLines must be >= 1");
            this.tooltipLines = tooltipLines;
            this.nameColorHex = nameColorHex;
            this.loreColorHex = loreColorHex;
        }

        @Override
        public Component getName(ItemStack stack) {
            return TooltipStyler.colorName(super.getName(stack), nameColorHex);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx,
                                    List<Component> tooltip, TooltipFlag flag) {
            TooltipStyler.addLore(stack, tooltipLines, loreColorHex, tooltip, flag);
        }
    }
    //Axe

    public static class TooltipAxeItem extends AxeItem {
        private final int tooltipLines;
        private final Integer nameColorHex;
        private final Integer loreColorHex;

        public TooltipAxeItem(Tier tier, float attackDamage, float attackSpeed,
                              Properties props, int tooltipLines,
                              Integer nameColorHex, Integer loreColorHex) {
            super(tier, props.attributes(AxeItem.createAttributes(tier, attackDamage, attackSpeed)));
            if (tooltipLines < 1) throw new IllegalArgumentException("tooltipLines must be >= 1");
            this.tooltipLines = tooltipLines;
            this.nameColorHex = nameColorHex;
            this.loreColorHex = loreColorHex;
        }

        @Override
        public Component getName(ItemStack stack) {
            return TooltipStyler.colorName(super.getName(stack), nameColorHex);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx,
                                    List<Component> tooltip, TooltipFlag flag) {
            TooltipStyler.addLore(stack, tooltipLines, loreColorHex, tooltip, flag);
        }
    }
    //Shovel
    public static class TooltipShovelItem extends ShovelItem {
        private final int tooltipLines;
        private final Integer nameColorHex;
        private final Integer loreColorHex;

        public TooltipShovelItem(Tier tier, float attackDamage, float attackSpeed,
                                 Properties props, int tooltipLines,
                                 Integer nameColorHex, Integer loreColorHex) {
            super(tier, props.attributes(ShovelItem.createAttributes(tier, attackDamage, attackSpeed)));
            if (tooltipLines < 1) throw new IllegalArgumentException("tooltipLines must be >= 1");
            this.tooltipLines = tooltipLines;
            this.nameColorHex = nameColorHex;
            this.loreColorHex = loreColorHex;
        }

        @Override
        public Component getName(ItemStack stack) {
            return TooltipStyler.colorName(super.getName(stack), nameColorHex);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx,
                                    List<Component> tooltip, TooltipFlag flag) {
            TooltipStyler.addLore(stack, tooltipLines, loreColorHex, tooltip, flag);
        }
    }
    //hoe KEKW
    public static class TooltipHoeItem extends HoeItem {
        private final int tooltipLines;
        private final Integer nameColorHex;
        private final Integer loreColorHex;

        public TooltipHoeItem(Tier tier, float attackDamage, float attackSpeed,
                              Properties props, int tooltipLines,
                              Integer nameColorHex, Integer loreColorHex) {
            super(tier, props.attributes(HoeItem.createAttributes(tier, attackDamage, attackSpeed)));
            if (tooltipLines < 1) throw new IllegalArgumentException("tooltipLines must be >= 1");
            this.tooltipLines = tooltipLines;
            this.nameColorHex = nameColorHex;
            this.loreColorHex = loreColorHex;
        }

        @Override
        public Component getName(ItemStack stack) {
            return TooltipStyler.colorName(super.getName(stack), nameColorHex);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx,
                                    List<Component> tooltip, TooltipFlag flag) {
            TooltipStyler.addLore(stack, tooltipLines, loreColorHex, tooltip, flag);
        }
    }

}