package com.roll_54.roll_mod.compat.createcybernetics.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.perigrine3.createcybernetics.effect.quickhacks.CyberpsychosisQuickhackController;
import com.roll_54.roll_mod.compat.createcybernetics.SyncWitness;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Stops the cyberpsychosis quickhack from re-serialising every player's cyberware twice a tick.
 *
 * <p>Create Cybernetics already has the right mechanism: {@code CyberwareAttachmentSyncEvents}
 * runs on player tick, asks {@code PlayerCyberwareData.isDirty()}, and only then calls
 * {@code syncData} and {@code clean()}. Every other part of the mod cooperates with it — {@code
 * setDirty} has callers all over the jar.
 *
 * <p>{@code CyberpsychosisQuickhackController.onPlayerTick} does not. It applies or clears the
 * {@code quickhack_cyberpsychosis} humanity penalty and then unconditionally calls
 * {@code setDirty()} followed by {@code ModAttachments.syncCyberware(player)} — every tick, for
 * every player, whether or not the penalty moved. That is two full serialisations per player per
 * tick: its own, plus the one it just provoked out of {@code CyberwareAttachmentSyncEvents} by
 * marking the attachment dirty. And the serialisation is not cheap — {@code
 * PlayerCyberwareData.serializeNBT} walks every {@code InstalledCyberware} and runs the
 * {@code ItemStack} codec over each one.
 *
 * <p>An hour of server profiling put the pair at 64.8s of tick time (39.2s here, 25.6s in the
 * sync events handler), ~1.3 ms/tick with eight players online — for a value that changes only
 * when the effect is applied or removed.
 *
 * <p>So: gate both calls on the penalty actually having changed. The witness is
 * {@code getHumanityPenaltySum()}, which is exactly what this handler mutates — it touches one key,
 * so a changed key is a changed sum. The comparison is pure; only the sync wrapper, which runs
 * second, records the new value.
 *
 * <p>This does not delay anything. When the penalty does change, both calls run exactly as before,
 * in the same tick. When another part of the mod changes the attachment it calls {@code setDirty()}
 * itself and {@code CyberwareAttachmentSyncEvents} picks it up, as it already does — this handler
 * was never what carried those changes.
 *
 * <p>The mod is a PLAYTEST PRERELEASE, so the config is {@code required = false} with
 * {@code defaultRequire = 0}: if either call site is renamed or restructured, the server loses the
 * optimisation and keeps running.
 */
@Mixin(value = CyberpsychosisQuickhackController.class, remap = false)
public class CyberpsychosisQuickhackControllerMixin {

    /**
     * Marks dirty only on a real change. Left in place when it changes, because {@code
     * CyberwareAttachmentSyncEvents} is the mod's own sync path and skipping it would push the
     * update to whenever something else happened to dirty the attachment.
     */
    @WrapOperation(
            method = "onPlayerTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/perigrine3/createcybernetics/common/capabilities/PlayerCyberwareData;setDirty()V"
            )
    )
    private static void roll_mod$dirtyOnlyWhenPenaltyChanged(
            PlayerCyberwareData data, Operation<Void> original) {
        if (SyncWitness.changed(data, data.getHumanityPenaltySum())) {
            original.call(data);
        }
    }

    /**
     * The expensive half. Runs immediately after the {@code setDirty()} above, so it sees the same
     * answer from the same pure comparison, and is the one that records it.
     */
    @WrapOperation(
            method = "onPlayerTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/perigrine3/createcybernetics/common/capabilities/ModAttachments;syncCyberware(Lnet/minecraft/server/level/ServerPlayer;)V"
            )
    )
    private static void roll_mod$syncOnlyWhenPenaltyChanged(
            ServerPlayer player, Operation<Void> original, @Local PlayerCyberwareData data) {
        int witness = data.getHumanityPenaltySum();
        if (SyncWitness.changed(data, witness)) {
            original.call(player);
            SyncWitness.remember(data, witness);
        }
    }
}
