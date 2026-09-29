package com.roll_54.roll_mod.items.modularsaber;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.items.electricItems.DrillDropCollector;
import com.roll_54.roll_mod.items.modulardrill.DrillModuleHelper;
import com.roll_54.roll_mod.items.modulardrill.ModuleType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.enchanting.GetEnchantmentLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * The saber modules that act on a kill or a damage event rather than on the swing itself:
 * Looting, XP Multiplier, XP Reactor, Beheading, Drop Collector and Vampirism. See
 * {@link ModularSaberItem} for the rest.
 *
 * <p>Every one of them requires the killer's main hand to hold a modular saber that is switched on:
 * a saber that is off, or empty, is a stick, and its modules are asleep.
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class SaberModuleEvents {

    private SaberModuleEvents() {}

    /** The live saber a melee source was dealt with, or {@code null}. */
    @Nullable
    private static ItemStack saberOf(DamageSource source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) return null;
        // Melee only: an arrow shot while holding the saber is not the saber's kill.
        if (source.getDirectEntity() != player) return null;
        return liveSaber(player);
    }

    @Nullable
    private static ItemStack liveSaber(Player player) {
        ItemStack stack = player.getMainHandItem();
        return stack.getItem() instanceof ModularSaberItem saber && saber.isLive(stack) ? stack : null;
    }

    /**
     * Looting (the Looting module, plus one level from the Meteorite module), applied the vanilla way: the saber reports the module's Looting level as if it were
     * enchanted, so every loot table's own looting rules apply unchanged. Fires for any enchantment
     * query on the stack, including ones not about a kill, which is harmless — only loot reads it.
     */
    @SubscribeEvent
    public static void onEnchantmentLevel(GetEnchantmentLevelEvent event) {
        ItemStack stack = event.getStack();
        if (!(stack.getItem() instanceof ModularSaberItem saber) || !saber.isLive(stack)) return;
        if (!event.isTargetting(Enchantments.LOOTING)) return;
        // The Fortune/Looting module's level (or a deprecated Looting module's — never both), plus
        // the Meteorite module's one on top.
        int level = (int) DrillModuleHelper.value(stack, ModuleType.FORTUNE)
                + (int) DrillModuleHelper.value(stack, ModuleType.LOOTING)
                + (DrillModuleHelper.get(stack).has(ModuleType.METEORITE) ? ModularSaberItem.METEORITE_LOOTING : 0);
        if (level <= 0) return;
        event.getHolder(Enchantments.LOOTING).ifPresent(holder -> event.getEnchantments().upgrade(holder, level));
    }

    /**
     * XP Multiplier, then XP Reactor: the orbs are multiplied first, so the two stack, and the
     * reactor then turns the whole drop into energy instead of orbs.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onExperienceDrop(LivingExperienceDropEvent event) {
        if (!(event.getAttackingPlayer() instanceof ServerPlayer player)) return;
        ItemStack stack = liveSaber(player);
        if (stack == null) return;

        int xp = event.getDroppedExperience();
        double multiplier = DrillModuleHelper.factor(stack, ModuleType.XP_MULTIPLIER);
        if (multiplier != 1.0) {
            xp = (int) Math.round(xp * multiplier);
            event.setDroppedExperience(xp);
        }

        if (xp > 0 && DrillModuleHelper.hasXpReactor(stack)) {
            DrillModuleHelper.absorbEnergy(stack, (ModularSaberItem) stack.getItem(), player,
                    xp * ModularSaberItem.EU_PER_XP);
            event.setDroppedExperience(0);
        }
    }

    /**
     * Beheading adds the victim's head to the drops; Drop Collector then hands every drop straight
     * to the killer. Low priority, so other mods' changes to the drop list are collected too.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDrops(LivingDropsEvent event) {
        ItemStack stack = saberOf(event.getSource());
        if (stack == null) return;
        ServerPlayer player = (ServerPlayer) event.getSource().getEntity();
        LivingEntity victim = event.getEntity();

        double chance = DrillModuleHelper.value(stack, ModuleType.BEHEADING);
        if (chance > 0 && victim.getRandom().nextDouble() < chance) {
            ItemStack head = headOf(victim);
            if (!head.isEmpty()) {
                event.getDrops().add(new ItemEntity(victim.level(), victim.getX(), victim.getY(), victim.getZ(), head));
            }
        }

        if (DrillModuleHelper.get(stack).has(ModuleType.DROP_COLLECTOR)) {
            List<ItemEntity> drops = new ArrayList<>(event.getDrops());
            event.getDrops().clear();
            for (ItemEntity drop : drops) {
                DrillDropCollector.giveToPlayer(player, drop.getItem());
            }
        }
    }

    /** The head a beheading drops: the vanilla heads, and a player's own head with their skin. */
    private static ItemStack headOf(LivingEntity victim) {
        EntityType<?> type = victim.getType();
        if (type == EntityType.ZOMBIE) return new ItemStack(Items.ZOMBIE_HEAD);
        if (type == EntityType.SKELETON) return new ItemStack(Items.SKELETON_SKULL);
        if (type == EntityType.WITHER_SKELETON) return new ItemStack(Items.WITHER_SKELETON_SKULL);
        if (type == EntityType.CREEPER) return new ItemStack(Items.CREEPER_HEAD);
        if (type == EntityType.PIGLIN) return new ItemStack(Items.PIGLIN_HEAD);
        if (type == EntityType.ENDER_DRAGON) return new ItemStack(Items.DRAGON_HEAD);
        if (victim instanceof Player player) {
            ItemStack head = new ItemStack(Items.PLAYER_HEAD);
            head.set(DataComponents.PROFILE, new ResolvableProfile(player.getGameProfile()));
            return head;
        }
        return ItemStack.EMPTY;
    }

    /** Vampirism: heals the attacker by a share of the damage that actually landed. */
    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        ItemStack stack = saberOf(event.getSource());
        if (stack == null) return;
        double share = DrillModuleHelper.value(stack, ModuleType.VAMPIRISM);
        float dealt = event.getNewDamage();
        if (share <= 0 || dealt <= 0) return;
        ((ServerPlayer) event.getSource().getEntity()).heal((float) (dealt * share));
    }
}
