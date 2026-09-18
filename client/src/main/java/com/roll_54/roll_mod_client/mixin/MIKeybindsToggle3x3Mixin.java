package com.roll_54.roll_mod_client.mixin;

import aztech.modern_industrialization.client.MIKeybinds;
import com.roll_54.roll_mod.items.NetheriteSteamDrillItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.Predicate;

/**
 * Lets MI's "toggle 3x3" keybind act on the netherite steam drill too.
 *
 * <p>The drill inherits the 3x3 area from MI's steam drill, but the only way to switch it off is
 * that keybind, and MI filters it down to its own item by identity, not by type. Without this the
 * netherite drill is stuck in 3x3 forever and can never reach its faster single-block speed.
 *
 * <p>The filter is a {@link Predicate} handed to a private factory in the class initializer, so the
 * widening happens where it is passed. Ordinal 1 is the second and last such call — the first
 * builds the jetpack flight toggle.
 */
@Mixin(MIKeybinds.class)
public class MIKeybindsToggle3x3Mixin {
    @ModifyArg(method = "<clinit>", at = @At(value = "INVOKE", ordinal = 1, target = "Laztech/modern_industrialization/client/MIKeybinds;toggleableItemAction(Lnet/minecraft/world/entity/EquipmentSlot;Ljava/util/function/Predicate;Ljava/util/function/BiConsumer;)Ljava/lang/Runnable;"), index = 1)
    private static Predicate<ItemStack> roll_mod$alsoToggleNetheriteDrill(Predicate<ItemStack> miSteamDrillOnly) {
        return miSteamDrillOnly.or(stack -> stack.getItem() instanceof NetheriteSteamDrillItem);
    }
}
