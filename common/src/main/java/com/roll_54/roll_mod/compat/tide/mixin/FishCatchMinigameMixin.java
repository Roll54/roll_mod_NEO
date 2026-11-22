package com.roll_54.roll_mod.compat.tide.mixin;

import com.li64.tide.data.minigame.FishCatchMinigame;
import com.roll_54.roll_mod.compat.tide.RollFishingRodItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * {@link RollFishingRodItem.Perk#STEADY}: Tide's catch minigame half as hard with the rod in hand. The hit area's miss
 * part (what a fish's strength takes away) is halved and the fish moves at half speed. Changed on the message that
 * starts the client's minigame, the one place both numbers are final.
 */
@Mixin(value = FishCatchMinigame.class, remap = false)
public abstract class FishCatchMinigameMixin {

    @Shadow @Final private ServerPlayer player;

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE",
            target = "Lcom/li64/tide/network/messages/MinigameClientMsg;<init>(BBBFF)V"), index = 3)
    private float roll_mod$steadyArea(float area) {
        return steady() ? Math.min(1f, 1f - (1f - area) * RollFishingRodItem.STEADY_FACTOR) : area;
    }

    @ModifyArg(method = "<init>", at = @At(value = "INVOKE",
            target = "Lcom/li64/tide/network/messages/MinigameClientMsg;<init>(BBBFF)V"), index = 4)
    private float roll_mod$steadySpeed(float speed) {
        return steady() ? speed * RollFishingRodItem.STEADY_FACTOR : speed;
    }

    private boolean steady() {
        for (InteractionHand hand : InteractionHand.values()) {
            if (player.getItemInHand(hand).getItem() instanceof RollFishingRodItem rod
                    && rod.has(RollFishingRodItem.Perk.STEADY)) return true;
        }
        return false;
    }
}
