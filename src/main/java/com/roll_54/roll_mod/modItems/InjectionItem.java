package com.roll_54.roll_mod.modItems;

import com.roll_54.roll_mod.init.ItemRegistry;
import net.minecraft.core.Holder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;


public class InjectionItem extends Item {

    public final Holder<MobEffect> effect1;
    public final Holder<MobEffect> effect2;
    public final Holder<MobEffect> effect3;
    public final int effectTime;
    public final int effectLvl;


    public InjectionItem(Properties properties, Holder<MobEffect> effect1, Holder<MobEffect> effect2, Holder<MobEffect> effect3, int effectTime, int effectLvl) {
        super(properties);
        this.effect1 = effect1;
        this.effect2 = effect2;
        this.effect3 = effect3;
        this.effectTime = effectTime;
        this.effectLvl = effectLvl;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {

            // Apply effects safely
            if (effect1 != null)
                player.addEffect(new MobEffectInstance(effect1, effectTime, effectLvl));

            if (effect2 != null)
                player.addEffect(new MobEffectInstance(effect2, effectTime, effectLvl));

            if (effect3 != null)
                player.addEffect(new MobEffectInstance(effect3, effectTime, effectLvl));

            // Consume item & give syringe
            if (!player.isCreative()) {
                stack.shrink(1);

                ItemStack syringe = new ItemStack(ItemRegistry.SYRINGE.get());
                player.addItem(syringe);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }



}