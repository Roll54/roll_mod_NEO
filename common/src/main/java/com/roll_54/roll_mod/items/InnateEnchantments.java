package com.roll_54.roll_mod.items;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Enchantments a tool carries because of the material it is made of.
 * <p>
 * They live outside the {@code minecraft:enchantments} component, so they are always active, survive a
 * grindstone and never take up the anvil's enchantment slots. NeoForge routes every gameplay enchantment
 * lookup through {@code Item#getEnchantmentLevel} / {@code Item#getAllEnchantments}, which the tool
 * classes in {@link InnateEnchantedTools} answer with this table.
 */
public final class InnateEnchantments {

    private final Map<ResourceKey<Enchantment>, Integer> levels;

    private InnateEnchantments(Map<ResourceKey<Enchantment>, Integer> levels) {
        this.levels = levels;
    }

    public static InnateEnchantments of(ResourceKey<Enchantment> enchantment, int level) {
        Map<ResourceKey<Enchantment>, Integer> levels = new LinkedHashMap<>();
        levels.put(enchantment, level);
        return new InnateEnchantments(levels);
    }

    public static InnateEnchantments of(ResourceKey<Enchantment> first, int firstLevel,
                                        ResourceKey<Enchantment> second, int secondLevel) {
        Map<ResourceKey<Enchantment>, Integer> levels = new LinkedHashMap<>();
        levels.put(first, firstLevel);
        levels.put(second, secondLevel);
        return new InnateEnchantments(levels);
    }

    /**
     * The material's level, or the applied one when the player enchanted the tool higher.
     * <p>
     * Answered out of {@link #all} wherever a lookup is reachable, because NeoForge requires
     * {@code getEnchantmentLevel} and {@code getAllEnchantments} to never disagree.
     */
    public int level(ItemStack stack, Holder<Enchantment> enchantment) {
        HolderLookup.RegistryLookup<Enchantment> lookup = enchantment.unwrapLookup();
        if (lookup != null) {
            return all(stack, lookup).getLevel(enchantment);
        }
        // A direct holder carries no lookup, so match the table on the key instead.
        int applied = stack.getTagEnchantments().getLevel(enchantment);
        Integer innate = enchantment.unwrapKey().map(this.levels::get).orElse(null);
        return innate == null ? applied : Math.max(applied, innate);
    }

    /** The applied enchantments with the material's own merged in at the higher of the two levels. */
    public ItemEnchantments all(ItemStack stack, HolderLookup.RegistryLookup<Enchantment> lookup) {
        ItemEnchantments applied = stack.getTagEnchantments();
        ItemEnchantments.Mutable merged = new ItemEnchantments.Mutable(applied);
        this.levels.forEach((key, level) -> lookup.get(key).ifPresent(own -> {
            // A stack that already carries something the material's own enchantment excludes -- Silk Touch
            // against Fortune, say, which only a command with explicit components can still produce -- keeps
            // what it was given; the material's drops out rather than running alongside it.
            for (Holder<Enchantment> other : applied.keySet()) {
                if (!other.equals(own) && !Enchantment.areCompatible(own, other)) return;
            }
            merged.upgrade(own, level);
        }));
        return merged.toImmutable();
    }

    /**
     * Whether the tool may still be enchanted with {@code enchantment}.
     * <p>
     * The anvil and the enchanting table only test exclusivity against the enchantments component, which
     * the material's own deliberately stay out of, so without this nothing stops a player from putting
     * Silk Touch on a pickaxe that already mines with Fortune.
     */
    public boolean allowsAlongside(Holder<Enchantment> enchantment) {
        HolderLookup.RegistryLookup<Enchantment> lookup = enchantment.unwrapLookup();
        if (lookup == null) return true;

        for (ResourceKey<Enchantment> key : this.levels.keySet()) {
            Holder.Reference<Enchantment> own = lookup.get(key).orElse(null);
            // Levelling up the material's own enchantment is fine; areCompatible() rejects a holder
            // against itself, so it has to be let through here.
            if (own == null || own.equals(enchantment)) continue;
            if (!Enchantment.areCompatible(own, enchantment)) return false;
        }
        return true;
    }

    /**
     * Lists the material's enchantments in the tooltip. Vanilla only renders what sits in the
     * enchantments component, so without this the player would never see them.
     */
    public void appendTooltip(Item.TooltipContext context, ItemStack stack, List<Component> tooltip) {
        HolderLookup.Provider registries = context.registries();
        if (registries == null) return;

        registries.lookup(Registries.ENCHANTMENT).ifPresent(lookup -> this.levels.forEach((key, level) ->
                lookup.get(key).ifPresent(holder -> {
                    // Skip anything the player already has on the stack at this level or higher; vanilla
                    // is already drawing that line from the component.
                    if (stack.getTagEnchantments().getLevel(holder) < level) {
                        tooltip.add(Enchantment.getFullname(holder, level));
                    }
                })));
    }
}
