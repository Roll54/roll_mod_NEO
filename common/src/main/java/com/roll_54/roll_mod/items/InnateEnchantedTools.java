package com.roll_54.roll_mod.items;

import com.roll_54.roll_mod.util.TooltipManager;
import com.roll_54.roll_mod.util.TooltipOptions;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup.RegistryLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.List;

/**
 * Tool and armour variants that carry {@link InnateEnchantments} from their material on top of the coloured
 * name and lore the {@link TooltipManager} tools already provide.
 */
public final class InnateEnchantedTools {

    private InnateEnchantedTools() {
    }

    // ---------------- SWORD ----------------
    public static class InnateSwordItem extends TooltipManager.TooltipSwordItem {
        private final InnateEnchantments innate;

        public InnateSwordItem(Tier tier, float attackDamage, float attackSpeed,
                               Properties props, TooltipOptions opts, InnateEnchantments innate) {
            super(tier, attackDamage, attackSpeed, props, opts);
            this.innate = innate;
        }

        @Override public int getEnchantmentLevel(ItemStack stack, Holder<Enchantment> enchantment) {
            return innate.level(stack, enchantment);
        }

        @Override public ItemEnchantments getAllEnchantments(ItemStack stack, RegistryLookup<Enchantment> lookup) {
            return innate.all(stack, lookup);
        }

        @Override public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
            innate.appendTooltip(ctx, stack, tooltip);
            super.appendHoverText(stack, ctx, tooltip, flag);
        }

        @Override public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
            return super.supportsEnchantment(stack, enchantment) && innate.allowsAlongside(enchantment);
        }

        /** The material's enchantments never come off, so the glint is always on. */
        @Override public boolean isFoil(ItemStack stack) {
            return true;
        }
    }

    // ---------------- PICKAXE ----------------
    public static class InnatePickaxeItem extends TooltipManager.TooltipPickaxeItem {
        private final InnateEnchantments innate;

        public InnatePickaxeItem(Tier tier, float attackDamage, float attackSpeed,
                                 Properties props, TooltipOptions opts, InnateEnchantments innate) {
            super(tier, attackDamage, attackSpeed, props, opts);
            this.innate = innate;
        }

        @Override public int getEnchantmentLevel(ItemStack stack, Holder<Enchantment> enchantment) {
            return innate.level(stack, enchantment);
        }

        @Override public ItemEnchantments getAllEnchantments(ItemStack stack, RegistryLookup<Enchantment> lookup) {
            return innate.all(stack, lookup);
        }

        @Override public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
            innate.appendTooltip(ctx, stack, tooltip);
            super.appendHoverText(stack, ctx, tooltip, flag);
        }

        @Override public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
            return super.supportsEnchantment(stack, enchantment) && innate.allowsAlongside(enchantment);
        }

        /** The material's enchantments never come off, so the glint is always on. */
        @Override public boolean isFoil(ItemStack stack) {
            return true;
        }
    }

    // ---------------- AXE ----------------
    public static class InnateAxeItem extends TooltipManager.TooltipAxeItem {
        private final InnateEnchantments innate;

        public InnateAxeItem(Tier tier, float attackDamage, float attackSpeed,
                             Properties props, TooltipOptions opts, InnateEnchantments innate) {
            super(tier, attackDamage, attackSpeed, props, opts);
            this.innate = innate;
        }

        @Override public int getEnchantmentLevel(ItemStack stack, Holder<Enchantment> enchantment) {
            return innate.level(stack, enchantment);
        }

        @Override public ItemEnchantments getAllEnchantments(ItemStack stack, RegistryLookup<Enchantment> lookup) {
            return innate.all(stack, lookup);
        }

        @Override public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
            innate.appendTooltip(ctx, stack, tooltip);
            super.appendHoverText(stack, ctx, tooltip, flag);
        }

        @Override public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
            return super.supportsEnchantment(stack, enchantment) && innate.allowsAlongside(enchantment);
        }

        /** The material's enchantments never come off, so the glint is always on. */
        @Override public boolean isFoil(ItemStack stack) {
            return true;
        }
    }

    // ---------------- SHOVEL ----------------
    public static class InnateShovelItem extends TooltipManager.TooltipShovelItem {
        private final InnateEnchantments innate;

        public InnateShovelItem(Tier tier, float attackDamage, float attackSpeed,
                                Properties props, TooltipOptions opts, InnateEnchantments innate) {
            super(tier, attackDamage, attackSpeed, props, opts);
            this.innate = innate;
        }

        @Override public int getEnchantmentLevel(ItemStack stack, Holder<Enchantment> enchantment) {
            return innate.level(stack, enchantment);
        }

        @Override public ItemEnchantments getAllEnchantments(ItemStack stack, RegistryLookup<Enchantment> lookup) {
            return innate.all(stack, lookup);
        }

        @Override public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
            innate.appendTooltip(ctx, stack, tooltip);
            super.appendHoverText(stack, ctx, tooltip, flag);
        }

        @Override public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
            return super.supportsEnchantment(stack, enchantment) && innate.allowsAlongside(enchantment);
        }

        /** The material's enchantments never come off, so the glint is always on. */
        @Override public boolean isFoil(ItemStack stack) {
            return true;
        }
    }

    // ---------------- HOE ----------------
    public static class InnateHoeItem extends TooltipManager.TooltipHoeItem {
        private final InnateEnchantments innate;

        public InnateHoeItem(Tier tier, float attackDamage, float attackSpeed,
                             Properties props, TooltipOptions opts, InnateEnchantments innate) {
            super(tier, attackDamage, attackSpeed, props, opts);
            this.innate = innate;
        }

        @Override public int getEnchantmentLevel(ItemStack stack, Holder<Enchantment> enchantment) {
            return innate.level(stack, enchantment);
        }

        @Override public ItemEnchantments getAllEnchantments(ItemStack stack, RegistryLookup<Enchantment> lookup) {
            return innate.all(stack, lookup);
        }

        @Override public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
            innate.appendTooltip(ctx, stack, tooltip);
            super.appendHoverText(stack, ctx, tooltip, flag);
        }

        @Override public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
            return super.supportsEnchantment(stack, enchantment) && innate.allowsAlongside(enchantment);
        }

        /** The material's enchantments never come off, so the glint is always on. */
        @Override public boolean isFoil(ItemStack stack) {
            return true;
        }
    }

    // ---------------- BLOCKOGRIZ ----------------
    public static class InnateBlockogrizItem extends TooltipManager.TooltipBlockogrizItem {
        private final InnateEnchantments innate;

        public InnateBlockogrizItem(Tier tier, float attackDamage, float attackSpeed, int preEfficiency,
                                    Properties props, TooltipOptions opts, InnateEnchantments innate) {
            super(tier, attackDamage, attackSpeed, preEfficiency, props, opts);
            this.innate = innate;
        }

        @Override public int getEnchantmentLevel(ItemStack stack, Holder<Enchantment> enchantment) {
            return innate.level(stack, enchantment);
        }

        @Override public ItemEnchantments getAllEnchantments(ItemStack stack, RegistryLookup<Enchantment> lookup) {
            return innate.all(stack, lookup);
        }

        @Override public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
            innate.appendTooltip(ctx, stack, tooltip);
            super.appendHoverText(stack, ctx, tooltip, flag);
        }

        @Override public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
            return super.supportsEnchantment(stack, enchantment) && innate.allowsAlongside(enchantment);
        }

        /** The material's enchantments never come off, so the glint is always on. */
        @Override public boolean isFoil(ItemStack stack) {
            return true;
        }
    }

    // ---------------- ARMOUR ----------------
    /** An armour piece with its material's enchantments; each piece of a set may carry its own. */
    public static class InnateArmorItem extends TooltipArmorItem {
        private final InnateEnchantments innate;

        public InnateArmorItem(Holder<ArmorMaterial> material, ArmorItem.Type type, Properties props,
                               TooltipOptions opts, InnateEnchantments innate) {
            super(material, type, props, opts);
            this.innate = innate;
        }

        @Override public int getEnchantmentLevel(ItemStack stack, Holder<Enchantment> enchantment) {
            return innate.level(stack, enchantment);
        }

        @Override public ItemEnchantments getAllEnchantments(ItemStack stack, RegistryLookup<Enchantment> lookup) {
            return innate.all(stack, lookup);
        }

        @Override public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
            innate.appendTooltip(ctx, stack, tooltip);
            super.appendHoverText(stack, ctx, tooltip, flag);
        }

        @Override public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
            return super.supportsEnchantment(stack, enchantment) && innate.allowsAlongside(enchantment);
        }

        /** The material's enchantments never come off, so the glint is always on. */
        @Override public boolean isFoil(ItemStack stack) {
            return true;
        }
    }
}
