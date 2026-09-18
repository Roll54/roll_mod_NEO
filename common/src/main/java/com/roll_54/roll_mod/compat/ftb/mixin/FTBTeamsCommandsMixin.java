package com.roll_54.roll_mod.compat.ftb.mixin;

import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.TeamRank;
import dev.ftb.mods.ftbteams.data.FTBTeamsCommands;
import net.minecraft.commands.CommandSourceStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stops FTB Teams' command predicates throwing when its team manager is not loaded.
 *
 * <p>Both {@code hasNoParty} and {@code hasParty} call {@code FTBTeamsAPI.api().getManager()}
 * without first asking {@code isManagerLoaded()}, and {@code FTBTeamsAPIImpl.getManager} is a bare
 * {@code Objects.requireNonNull}. The manager only exists while a world is loaded — it is created on
 * server start and dropped on stop — so outside that window either predicate is a
 * {@link NullPointerException} waiting for something to evaluate it.
 *
 * <p>Something does. {@code Commands.sendCommands} walks the <em>whole</em> command tree calling
 * {@code CommandNode.canUse} on every node, so any command-list resend outside that window hits
 * these two. LuckPerms drives exactly such a resend from its own buffered, off-thread
 * {@code NeoForgeCommandListUpdater}, and reaches the server through
 * {@code BlockableEventLoop.executeBlocking} — a worker thread parked on the server thread until the
 * task finishes. A task that keeps throwing there is how this stops being a log line and starts
 * being a server that will not move.
 *
 * <p>Returning {@code false} is not a guess at what the method meant: both predicates already return
 * {@code false} for a source that is not a player, and "no team manager" is the same answer to the
 * same question — this player cannot use a party command right now. The nodes are simply left out of
 * that resend, and the next one (a join, an op change, the next permission update) puts them back
 * once the manager is up. Nothing is hidden that would otherwise have worked: with no manager the
 * commands could not have run either.
 *
 * <p>Guards both, deliberately. Fixing only the one in the crash log would move the same NPE to the
 * other predicate the next time the tree is walked.
 *
 * <p>{@code ftbteams} is a soft dependency — see {@code TeamsFacade} — so this lives in its own
 * mixin config with {@code required: false}. If the mod is absent the target class is not there,
 * this is skipped, and nothing else notices.
 */
@Mixin(value = FTBTeamsCommands.class, remap = false)
public class FTBTeamsCommandsMixin {

    @Inject(method = "hasNoParty", at = @At("HEAD"), cancellable = true)
    private void roll_mod$hasNoPartyWithoutManager(
            CommandSourceStack source, CallbackInfoReturnable<Boolean> cir) {
        if (managerMissing()) cir.setReturnValue(false);
    }

    @Inject(method = "hasParty", at = @At("HEAD"), cancellable = true)
    private void roll_mod$hasPartyWithoutManager(
            CommandSourceStack source, TeamRank rank, CallbackInfoReturnable<Boolean> cir) {
        if (managerMissing()) cir.setReturnValue(false);
    }

    /**
     * The check {@code getManager()} should have made. The null test on {@code api()} covers the
     * other end of the same lifetime: it is a plain static field, handed over during mod
     * construction, so it is null earlier still.
     */
    private static boolean managerMissing() {
        FTBTeamsAPI.API api = FTBTeamsAPI.api();
        return api == null || !api.isManagerLoaded();
    }
}
