package com.roll_54.roll_mod.items.modularsaber;

import aztech.modern_industrialization.MIComponents;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.data.OneStateToggleableItem;
import com.roll_54.roll_mod.items.modulardrill.DrillModuleHelper;
import com.roll_54.roll_mod.items.modulardrill.DrillModuleItem;
import com.roll_54.roll_mod.items.modulardrill.DrillModules;
import com.roll_54.roll_mod.items.modulardrill.ModularTool;
import com.roll_54.roll_mod.items.modulardrill.NewItemVisibility;
import com.roll_54.roll_mod.items.modulardrill.ModuleType;
import com.roll_54.roll_mod.items.modulardrill.ToolKind;
import com.roll_54.roll_mod.radiation.RadiationHandler;
import com.roll_54.roll_mod.registry.ModEffects;
import com.roll_54.roll_mod.util.EnergyFormatUtils;
import dev.technici4n.grandpower.api.ISimpleEnergyItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * The modular nano saber: a switchable energy blade whose damage, speed, energy use and on-hit
 * effects come from the modules installed at the Module Installation Table — the drill's module
 * system, for a sword.
 *
 * <p>Behaves like the fixed {@code EnergySwordItem} it replaces: Shift+right-click switches it on,
 * every hit while on costs energy, and running dry switches it off. On top of that a switched-on
 * saber drains its tier's idle rate every second, wherever it is in the inventory.
 *
 * <p>Effects that need the hit itself (sweep, irradiation) live here; those that hang off a kill or
 * a damage event (looting, XP, drops, beheading, vampirism) live in {@link SaberModuleEvents}.
 */
public class ModularSaberItem extends Item implements ISimpleEnergyItem, OneStateToggleableItem, ModularTool {

    /** EU gained per point of experience captured by the XP Reactor module — the drill's rate. */
    public static final long EU_PER_XP = 100L;

    /** What the saber hits for while switched off, like the fixed sabers. */
    private static final double DAMAGE_OFF = 0.5;
    private static final double BASE_ATTACK_SPEED = -2.4;

    /**
     * The Meteorite module: flat damage on top of everything else (added after the Damage module's
     * multiplier, so it is never multiplied), and one extra level of Looting. It costs no energy.
     */
    public static final double METEORITE_DAMAGE = 20.0;
    public static final int METEORITE_LOOTING = 1;

    /** Each mob a sweep reaches past the first costs this share of a hit. */
    private static final double SWEEP_TARGET_COST = 0.5;

    /** Radiation added to a player per Irradiation level, per hit (tier 1 starts at 50 000). */
    private static final int DOSE_PER_LEVEL = 20_000;
    private static final int POISON_TICKS = 200;

    private static final ResourceLocation DAMAGE_ID = RollMod.id("modular_saber_damage");
    private static final ResourceLocation SPEED_ID = RollMod.id("modular_saber_speed");

    private final SaberVoltage voltage;

    public ModularSaberItem(SaberVoltage voltage, Properties properties) {
        super(properties.stacksTo(1));
        this.voltage = voltage;
    }

    public SaberVoltage voltage() {
        return voltage;
    }

    @Override
    public int moduleSlots() {
        return voltage.slots;
    }

    @Override
    public ToolKind toolKind() {
        return ToolKind.SABER;
    }

    @Override
    public int maxComplexity() {
        return voltage.complexity;
    }

    /** A placeholder name while the client hides new items — see {@link NewItemVisibility}. */
    @Override
    public Component getName(ItemStack stack) {
        return NewItemVisibility.name(super.getName(stack));
    }

    /* -------------------------------------------- energy -------------------------------------------- */

    @Override
    public DataComponentType<Long> getEnergyComponent() {
        return MIComponents.ENERGY.get();
    }

    @Override
    public long getEnergyCapacity(ItemStack stack) {
        return (long) (voltage.baseCapacity * DrillModuleHelper.capacityFactor(stack));
    }

    /** Zero with an XP Reactor installed — kills become the only way in, as on the drill. */
    @Override
    public long getEnergyMaxInput(ItemStack stack) {
        return DrillModuleHelper.hasXpReactor(stack) ? 0L : getEnergyCapacity(stack);
    }

    @Override
    public long getEnergyMaxOutput(ItemStack stack) {
        return 0;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x30e996;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        double frac = (double) getStoredEnergy(stack) / getEnergyCapacity(stack);
        return (int) (13 * frac);
    }

    /** What one hit costs: base × every installed module's cost factor. */
    public long costPerHit(ItemStack stack) {
        double cost = voltage.baseCostPerHit;
        for (DrillModuleItem module : DrillModuleHelper.get(stack).moduleItems()) {
            cost *= module.costFactor(ToolKind.SABER);
        }
        return (long) Math.ceil(cost);
    }

    /** Whether it is switched on and can pay for a hit. */
    public boolean isLive(ItemStack stack) {
        return isActivated(stack) && getStoredEnergy(stack) > 0;
    }

    /* -------------------------------------------- toggle -------------------------------------------- */

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }

        if (!level.isClientSide) {
            boolean on = !isActivated(stack);
            setActivated(player, stack, on);
            player.getInventory().setChanged();
            Component state = Component.translatable(on ? "tooltip.roll_mod.on" : "tooltip.roll_mod.off")
                    .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED);
            player.displayClientMessage(Component.translatable("tooltip.roll_mod.battery_toggled", state), true);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS, 0.6f, 1.4f);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /**
     * The idle drain: once a second while switched on, wherever the saber is. Once a second rather
     * than every tick, so the energy component — and with it the client's copy of the stack —
     * changes twenty times less often.
     */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof Player player) || !isActivated(stack)) return;
        if (level.getGameTime() % 20 != 0) return;
        if (player.getAbilities().instabuild) return;

        long drain = voltage.idleDrainPerSecond;
        if (getStoredEnergy(stack) < drain) {
            setStoredEnergy(stack, 0);
            switchOff(player, stack);
            return;
        }
        tryUseEnergy(stack, drain);
    }

    private void switchOff(Player player, ItemStack stack) {
        setActivated(player, stack, false);
        player.displayClientMessage(Component.translatable("tooltip.roll_mod.no_energy")
                .withStyle(ChatFormatting.RED), true);
    }

    /* --------------------------------------------- hit ---------------------------------------------- */

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!(attacker instanceof Player player) || !isActivated(stack)) {
            return super.hurtEnemy(stack, target, attacker);
        }

        long cost = costPerHit(stack);
        if (getStoredEnergy(stack) < cost) {
            switchOff(player, stack);
            return false;
        }
        tryUseEnergy(stack, cost);

        if (player instanceof ServerPlayer server) {
            irradiate(stack, target);
            sweep(stack, server, target, cost);
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    /** Irradiation: a dose for a player, which their armour softens; poisoning for anything else. */
    private static void irradiate(ItemStack stack, LivingEntity target) {
        int level = (int) DrillModuleHelper.value(stack, ModuleType.IRRADIATION);
        if (level <= 0) return;
        if (target instanceof ServerPlayer player) {
            RadiationHandler.addDose(player, level * DOSE_PER_LEVEL);
        } else {
            target.addEffect(new MobEffectInstance(ModEffects.RADIATION_POISONING, POISON_TICKS, level - 1));
        }
    }

    /**
     * Sweep: the hit's damage again, to every hostile mob within the module's radius of the
     * target. Never players, pets or villagers — only {@link Enemy}. Each extra mob costs part of a
     * hit, and the sweep stops where the energy does.
     */
    private void sweep(ItemStack stack, ServerPlayer player, LivingEntity target, long hitCost) {
        double radius = DrillModuleHelper.value(stack, ModuleType.SWEEP);
        if (radius <= 0) return;

        float damage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        long extraCost = (long) Math.ceil(hitCost * SWEEP_TARGET_COST);
        List<LivingEntity> around = target.level().getEntitiesOfClass(LivingEntity.class,
                target.getBoundingBox().inflate(radius),
                e -> e != target && e != player && e.isAlive() && e instanceof Enemy);
        for (LivingEntity mob : around) {
            if (getStoredEnergy(stack) < extraCost) break;
            tryUseEnergy(stack, extraCost);
            mob.hurt(player.damageSources().playerAttack(player), damage);
        }
        if (!around.isEmpty()) player.sweepAttack();
    }

    /* ------------------------------------------ attributes ------------------------------------------ */

    /**
     * Damage and speed, built from the modules on every read. Returned here rather than written to
     * the ATTRIBUTE_MODIFIERS component, which would replace this method's answer outright and so
     * lose the on/off switch.
     */
    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        boolean live = isLive(stack);
        double damage = live
                ? voltage.baseDamage * DrillModuleHelper.factor(stack, ModuleType.DAMAGE)
                        + (DrillModuleHelper.get(stack).has(ModuleType.METEORITE) ? METEORITE_DAMAGE : 0)
                : DAMAGE_OFF;
        // The merged Speed module, or a deprecated Attack Speed one still installed; never both.
        double speed = BASE_ATTACK_SPEED + (live
                ? DrillModuleHelper.saberValue(stack, ModuleType.SPEED)
                        + DrillModuleHelper.value(stack, ModuleType.ATTACK_SPEED)
                : 0);
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(DAMAGE_ID, damage, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(SPEED_ID, speed, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    /** Energy and the switch change components constantly; only a different item is a new swing. */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    @Override
    public boolean shouldCauseBlockBreakReset(ItemStack oldStack, ItemStack newStack) {
        return oldStack.getItem() != newStack.getItem();
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    /* -------------------------------------------- tooltip ------------------------------------------- */

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.roll_mod.energy",
                        EnergyFormatUtils.formatEnergy(getStoredEnergy(stack)),
                        EnergyFormatUtils.formatEnergy(getEnergyCapacity(stack)))
                .withStyle(ChatFormatting.AQUA));
        boolean on = isActivated(stack);
        tooltip.add(Component.translatable("tooltip.roll_mod.mode",
                        Component.translatable(on ? "tooltip.roll_mod.on" : "tooltip.roll_mod.off"))
                .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED));
        tooltip.add(Component.translatable("tooltip.roll_mod.toggle_hint").withStyle(ChatFormatting.GRAY));

        if (!Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("tooltip.roll_mod.general_press_shift")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }

        tooltip.add(Component.translatable("tooltip.roll_mod.modular_saber.cost",
                        EnergyFormatUtils.formatEnergyWithUnit(costPerHit(stack)))
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.roll_mod.modular_saber.idle",
                        EnergyFormatUtils.formatEnergyWithUnit(voltage.idleDrainPerSecond))
                .withStyle(ChatFormatting.AQUA));

        DrillModules modules = DrillModuleHelper.get(stack);
        tooltip.add(Component.translatable("tooltip.roll_mod.modular_drill.slots",
                        modules.modules().size(), voltage.slots)
                .withStyle(ChatFormatting.WHITE));
        int complexity = DrillModuleHelper.complexity(stack);
        tooltip.add(Component.translatable("tooltip.roll_mod.modular_tool.complexity",
                        complexity, maxComplexity())
                .withStyle(complexity >= maxComplexity() ? ChatFormatting.RED : ChatFormatting.WHITE));
        for (DrillModuleItem module : modules.moduleItems()) {
            tooltip.add(Component.literal("  • ").append(module.getDescription())
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (DrillModuleHelper.hasXpReactor(stack)) {
            tooltip.add(Component.translatable("tooltip.roll_mod.modular_saber.xp_locked")
                    .withStyle(ChatFormatting.GOLD));
        }
        tooltip.add(Component.translatable("tooltip.roll_mod.modular_saber.install_hint")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
