package com.roll_54.roll_mod_server.mixin.crash;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.roll_54.roll_mod_server.crash.CrashReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Per-stack guard in {@code Inventory.tick}: one item whose {@code inventoryTick} throws loses only its own
 * tick, instead of failing the whole player tick. Unguarded, the exception becomes "Ticking player" and the
 * player is kicked with "Internal server error" on every tick, so they can never stay online to get rid of
 * the item.
 *
 * <p>Added for Sophisticated Backpacks 3.26.7 / Core 1.5.5: a backpack whose linked-storage group no longer
 * exists throws {@code IllegalStateException: Unknown linked storage group} from
 * {@code BackpackItem.inventoryTick} (via {@code LinkedStorageGroupManager.getGroup}).
 */
@Mixin(Inventory.class)
public class InventoryTickGuardMixin {

    @Shadow
    @Final
    public Player player;

    @WrapOperation(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;inventoryTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;IZ)V"))
    private void rollmodserver$guardInventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected,
                                                  Operation<Void> original) {
        try {
            original.call(stack, level, entity, slot, selected);
        } catch (Throwable t) {
            if (player.getServer() == null) throw t;
            CrashReporter.report(player.getServer(), t);
        }
    }
}
