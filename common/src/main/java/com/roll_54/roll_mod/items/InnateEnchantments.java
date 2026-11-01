package com.roll_54.roll_mod.items;

import net.minecraft.ChatFormatting;
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

    public static InnateEnchantments of(ResourceKey<Enchantment> first, int firstLevel,
                                        ResourceKey<Enchantment> second, int secondLevel,
                                        ResourceKey<Enchantment> third, int thirdLevel) {
        Map<ResourceKey<Enchantment>, Integer> levels = new LinkedHashMap<>();
        levels.put(first, firstLevel);
        levels.put(second, secondLevel);
        levels.put(third, thirdLevel);
        return new InnateEnchantments(levels);
    }

    /** An item whose material carries innate enchantments; lets the tooltip handler find them. */
    public interface Carrier {
        InnateEnchantments innateEnchantments();
    }

    /**
     * The material's level <em>plus</em> the applied one: an unenchanted tool mines with Fortune I, and
     * the same tool enchanted with Fortune I mines with Fortune II.
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
        return innate == null ? applied : Math.min(MAX_LEVEL, applied + innate);
    }

    /** "I", "II", ... the way vanilla writes enchantment levels; a plain number past its table. */
    private static Component levelName(int level) {
        return level >= 1 && level <= 10
                ? Component.translatable("enchantment.level." + level)
                : Component.literal(Integer.toString(level));
    }

    /** ItemEnchantments refuses anything above this. */
    private static final int MAX_LEVEL = 255;

    /** The material's level for {@code enchantment}, or 0 when the material does not carry it. */
    public int innateLevel(Holder<Enchantment> enchantment) {
        return enchantment.unwrapKey().map(key -> this.levels.getOrDefault(key, 0)).orElse(0);
    }

    /** The applied enchantments with the material's own added on top of them (levels sum). */
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
            merged.set(own, Math.min(MAX_LEVEL, applied.getLevel(own) + level));
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
            // Applying the material's own enchantment is fine -- it stacks on top of the innate level;
            // areCompatible() rejects a holder against itself, so it has to be let through here.
            if (own == null || own.equals(enchantment)) continue;
            if (!Enchantment.areCompatible(own, enchantment)) return false;
        }
        return true;
    }

    /**
     * Lists the material's enchantments in the tooltip. Vanilla only renders what sits in the
     * enchantments component, so without this the player would never see them.
     * <p>
     * Only the ones the player has <em>not</em> also applied are added here: for those, vanilla draws its
     * own line from the component (after this runs), and {@link #fixAppliedTooltipLines} swaps that line
     * for the summed level once the tooltip is complete.
     */
    public void appendTooltip(Item.TooltipContext context, ItemStack stack, List<Component> tooltip) {
        HolderLookup.Provider registries = context.registries();
        if (registries == null) return;

        registries.lookup(Registries.ENCHANTMENT).ifPresent(lookup -> this.levels.forEach((key, level) ->
                lookup.get(key).ifPresent(holder -> {
                    if (stack.getTagEnchantments().getLevel(holder) <= 0) {
                        tooltip.add(Enchantment.getFullname(holder, level).copy()
                                .append(Component.translatable("tooltip.roll_mod.innate_enchantment.material")
                                        .withStyle(ChatFormatting.DARK_GRAY)));
                    }
                })));
    }

    /**
     * Rewrites vanilla's tooltip line for each applied enchantment the material also carries, from the
     * applied level ("Fortune I") to the level the tool actually works at ("Fortune II"). Called on the
     * finished tooltip (ItemTooltipEvent), since vanilla adds the component's lines after
     * {@code appendHoverText}.
     */
    public void fixAppliedTooltipLines(ItemStack stack, List<Component> tooltip) {
        ItemEnchantments applied = stack.getTagEnchantments();
        for (Holder<Enchantment> holder : applied.keySet()) {
            int innate = innateLevel(holder);
            if (innate <= 0) continue;
            int appliedLevel = applied.getLevel(holder);
            String vanillaLine = Enchantment.getFullname(holder, appliedLevel).getString();
            for (int i = 0; i < tooltip.size(); i++) {
                if (tooltip.get(i).getString().equals(vanillaLine)) {
                    tooltip.set(i, Enchantment.getFullname(holder, Math.min(MAX_LEVEL, appliedLevel + innate)).copy()
                            .append(Component.translatable("tooltip.roll_mod.innate_enchantment.bonus", levelName(innate))
                                    .withStyle(ChatFormatting.DARK_GRAY)));
                    break;
                }
            }
        }
    }
}
