package com.roll_54.roll_mod.mixin;

import aztech.modern_industrialization.items.armor.MIKeyMap;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MIKeyMap.class)
public interface MIKeyMapInvoker {
    @Invoker("isHoldingUp")
    static boolean invokeIsHoldingUp(Player player) {
        throw new AssertionError();
    }
}