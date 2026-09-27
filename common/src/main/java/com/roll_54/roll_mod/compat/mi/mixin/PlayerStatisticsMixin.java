package com.roll_54.roll_mod.compat.mi.mixin;

import aztech.modern_industrialization.compat.ftbquests.FTBQuestsFacade;
import aztech.modern_industrialization.stats.PlayerStatistics;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.roll_54.roll_mod.compat.mi.QuestSubmitBuffer;
import java.util.UUID;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Routes MI's per-output quest submit through {@link QuestSubmitBuffer} instead of straight into
 * FTB Quests. See that class for why, and for what the batching does and does not change.
 *
 * <p>Only the FTB call is redirected. Everything else {@code addProducedItems} does — the
 * {@code producedItems} counter, {@code awardStat}, the team and guild fan-out — runs untouched, in
 * the same tick, in the same order.
 */
@Mixin(value = PlayerStatistics.class, remap = false)
public class PlayerStatisticsMixin {

    @WrapOperation(
            method = "addProducedItems",
            at = @At(
                    value = "INVOKE",
                    target = "Laztech/modern_industrialization/compat/ftbquests/FTBQuestsFacade;addCompleted(Ljava/util/UUID;Lnet/minecraft/world/item/Item;J)V"
            )
    )
    private void roll_mod$batchQuestSubmit(
            FTBQuestsFacade facade, UUID uuid, Item item, long amount, Operation<Void> original) {
        if (!QuestSubmitBuffer.record(uuid, item, amount)) {
            original.call(facade, uuid, item, amount);
        }
    }
}
