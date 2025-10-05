package com.roll_54.roll_mod.Util;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;

import java.util.List;
public class TooltipManager {

    private static Component colorName(Component base, TooltipOptions opts) {
        if (opts != null && opts.hasNameColor()) {
            return base.copy().withStyle(s -> s.withColor(opts.nameColorHex()));
        }
        return base;
    }

    private static void addLore(ItemStack stack, TooltipOptions opts, List<Component> tooltip, TooltipFlag flag) {
        if (opts == null || !opts.hasLore()) return;
        TooltipStyler.addLore(stack, opts.loreLines(), opts.loreColorHex(), tooltip, flag);
    }

    // ---------------- SWORD ----------------
    public static class TooltipSwordItem extends SwordItem {
        private final TooltipOptions opts;

        public TooltipSwordItem(Tier tier, float attackDamage, float attackSpeed,
                                Properties props, TooltipOptions opts) {
            super(tier, props.attributes(SwordItem.createAttributes(tier, attackDamage, attackSpeed)));
            this.opts = (opts == null) ? TooltipOptions.NONE : opts;
        }

        @Override public Component getName(ItemStack stack) { return colorName(super.getName(stack), opts); }
        @Override public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
            addLore(stack, opts, tooltip, flag);
        }
    }

    // ---------------- PICKAXE ----------------
    public static class TooltipPickaxeItem extends PickaxeItem {
        private final TooltipOptions opts;

        public TooltipPickaxeItem(Tier tier, float attackDamage, float attackSpeed,
                                  Properties props, TooltipOptions opts) {
            super(tier, props.attributes(PickaxeItem.createAttributes(tier, attackDamage, attackSpeed)));
            this.opts = (opts == null) ? TooltipOptions.NONE : opts;
        }

        @Override public Component getName(ItemStack stack) { return colorName(super.getName(stack), opts); }
        @Override public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
            addLore(stack, opts, tooltip, flag);
        }
    }

    // ---------------- AXE ----------------
    public static class TooltipAxeItem extends AxeItem {
        private final TooltipOptions opts;

        public TooltipAxeItem(Tier tier, float attackDamage, float attackSpeed,
                              Properties props, TooltipOptions opts) {
            super(tier, props.attributes(AxeItem.createAttributes(tier, attackDamage, attackSpeed)));
            this.opts = (opts == null) ? TooltipOptions.NONE : opts;
        }

        @Override public Component getName(ItemStack stack) { return colorName(super.getName(stack), opts); }
        @Override public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
            addLore(stack, opts, tooltip, flag);
        }
    }

    // ---------------- SHOVEL ----------------
    public static class TooltipShovelItem extends ShovelItem {
        private final TooltipOptions opts;

        public TooltipShovelItem(Tier tier, float attackDamage, float attackSpeed,
                                 Properties props, TooltipOptions opts) {
            super(tier, props.attributes(ShovelItem.createAttributes(tier, attackDamage, attackSpeed)));
            this.opts = (opts == null) ? TooltipOptions.NONE : opts;
        }

        @Override public Component getName(ItemStack stack) { return colorName(super.getName(stack), opts); }
        @Override public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
            addLore(stack, opts, tooltip, flag);
        }
    }

    // ---------------- HOE ----------------
    public static class TooltipHoeItem extends HoeItem {
        private final TooltipOptions opts;

        public TooltipHoeItem(Tier tier, float attackDamage, float attackSpeed,
                              Properties props, TooltipOptions opts) {
            super(tier, props.attributes(HoeItem.createAttributes(tier, attackDamage, attackSpeed)));
            this.opts = (opts == null) ? TooltipOptions.NONE : opts;
        }

        @Override public Component getName(ItemStack stack) { return colorName(super.getName(stack), opts); }
        @Override public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
            addLore(stack, opts, tooltip, flag);
        }
    }
}