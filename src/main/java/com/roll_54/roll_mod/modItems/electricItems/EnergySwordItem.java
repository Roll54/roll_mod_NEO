package com.roll_54.roll_mod.modItems.electricItems;

import aztech.modern_industrialization.MIComponents;
import com.roll_54.roll_mod.data.ToggleableItem;
import com.roll_54.roll_mod.util.EnergyFormatUtils;
import dev.technici4n.grandpower.api.ISimpleEnergyItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class EnergySwordItem extends Item implements ToggleableItem, ISimpleEnergyItem {

    private final long energyCapacity;
    private final long energyPerHit;

    private final ItemAttributeModifiers offAttributes;
    private final ItemAttributeModifiers onAttributes;

    public EnergySwordItem(
            Properties properties,
            long energyCapacity,
            long energyPerHit,
            double damageOff,
            double damageOn
    ) {
        super(properties.stacksTo(1));

        this.energyCapacity = energyCapacity;
        this.energyPerHit = energyPerHit;

        this.offAttributes = createDamageModifiers(damageOff, -2.4);
        this.onAttributes  = createDamageModifiers(damageOn,  -2.4);
    }



    @Override
    public DataComponentType<Long> getEnergyComponent() {
        return MIComponents.ENERGY.get();
    }

    @Override
    public long getEnergyCapacity(ItemStack stack) {
        return energyCapacity;
    }

    @Override
    public long getEnergyMaxInput(ItemStack stack) {
        return energyCapacity;
    }

    @Override
    public long getEnergyMaxOutput(ItemStack stack) {
        return 0;
    }

    @Override
    public long getStoredEnergy(ItemStack stack) {
        return ISimpleEnergyItem.super.getStoredEnergy(stack);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }

        boolean active = isActivated(stack);

        if (!level.isClientSide) {
            boolean newState = !active;
            setActivated(player, stack, newState);

            // оновлення інвентаря
            player.getInventory().setChanged();

            Component stateText = Component.translatable(
                    newState ? "tooltip.roll_mod.on" : "tooltip.roll_mod.off"
            ).withStyle(newState ? ChatFormatting.GREEN : ChatFormatting.RED);

            player.displayClientMessage(
                    Component.translatable("tooltip.roll_mod.battery_toggled", stateText),
                    true
            );
        }

        level.playSound(
                null,
                player.blockPosition(),
                SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS,
                0.6f,
                1.4f
        );

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }



    @Override
    public boolean hurtEnemy(ItemStack stack,
                             LivingEntity target,
                             LivingEntity attacker) {

        if (!(attacker instanceof Player player)) {
            return super.hurtEnemy(stack, target, attacker);
        }

        if (!isActivated(stack)) {
            return super.hurtEnemy(stack, target, attacker);
        }

        long hitCost = energyPerHit; // енергія за удар

        if (getStoredEnergy(stack) < hitCost) {
            setActivated(player, stack, false);

            player.displayClientMessage(
                    Component.translatable("tooltip.roll_mod.no_energy")
                            .withStyle(ChatFormatting.RED),
                    true
            );

            return false;
        }

        // ⚡️ удар + витрати
        tryUseEnergy(stack, hitCost);

        return super.hurtEnemy(stack, target, attacker);
    }

    private static ItemAttributeModifiers createDamageModifiers(double damage, double speed) {
        return ItemAttributeModifiers.builder()
                .add(
                        Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(
                                ResourceLocation.fromNamespaceAndPath("roll_mod", "energy_sword_damage"),
                                damage,
                                AttributeModifier.Operation.ADD_VALUE
                        ),
                        EquipmentSlotGroup.MAINHAND
                )
                .add(
                        Attributes.ATTACK_SPEED,
                        new AttributeModifier(
                                ResourceLocation.fromNamespaceAndPath("roll_mod", "energy_sword_speed"),
                                speed,
                                AttributeModifier.Operation.ADD_VALUE
                        ),
                        EquipmentSlotGroup.MAINHAND
                )
                .build();
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        return isActivated(stack) && getStoredEnergy(stack) > 0
                ? onAttributes
                : offAttributes;
    }


    @Override
    public void appendHoverText(ItemStack stack,
                                Item.TooltipContext ctx,
                                List<Component> tooltip,
                                TooltipFlag flag) {
        long stored = getStoredEnergy(stack);
        long cap = getEnergyCapacity(stack);
        boolean active = isActivated(stack);

        String formattedStored = EnergyFormatUtils.formatEnergy(stored);
        String formattedCap = EnergyFormatUtils.formatEnergy(cap);

        tooltip.add(Component.translatable("tooltip.roll_mod.energy", formattedStored, formattedCap)
                .withStyle(ChatFormatting.AQUA));

        tooltip.add(Component.translatable("tooltip.roll_mod.mode",
                        Component.translatable(active ? "tooltip.roll_mod.on" : "tooltip.roll_mod.off"))
                .withStyle(active ? ChatFormatting.GREEN : ChatFormatting.RED));

        tooltip.add(Component.translatable("tooltip.roll_mod.toggle_hint")
                .withStyle(ChatFormatting.GRAY));

    }
}
