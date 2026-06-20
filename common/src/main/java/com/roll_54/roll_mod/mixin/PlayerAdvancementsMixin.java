package com.roll_54.roll_mod.mixin;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;
import java.util.Set;

@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementsMixin {

    @Shadow
    private ServerPlayer player;
    @Shadow private PlayerList playerList;
    @Shadow private Map<AdvancementHolder, AdvancementProgress> progress;
    @Shadow private Set<AdvancementHolder> progressChanged;

    @Shadow protected abstract AdvancementProgress getOrStartProgress(AdvancementHolder advancement);
    @Shadow protected abstract void unregisterListeners(AdvancementHolder advancement);
    @Shadow protected abstract void markForVisibilityUpdate(AdvancementHolder advancement);

    @Overwrite
    public boolean award(AdvancementHolder advancement, String criterionKey) {
        DisplayInfo displayinfo = advancement.value().display().orElse(null);
        if (this.player instanceof net.neoforged.neoforge.common.util.FakePlayer) return false;
        boolean flag = false;
        AdvancementProgress advancementprogress = this.getOrStartProgress(advancement);
        boolean flag1 = advancementprogress.isDone();
        if (advancementprogress.grantProgress(criterionKey)) {
            this.unregisterListeners(advancement);
            this.progressChanged.add(advancement);
            flag = true;
            net.neoforged.neoforge.event.EventHooks.onAdvancementProgressedEvent(this.player, advancement, advancementprogress, criterionKey, net.neoforged.neoforge.event.entity.player.AdvancementEvent.AdvancementProgressEvent.ProgressType.GRANT);
            if (!flag1 && advancementprogress.isDone()) {
                advancement.value().rewards().grant(this.player);
                advancement.value().display().ifPresent(p_352686_ -> {
                    if (p_352686_.shouldAnnounceChat() && this.player.level().getGameRules().getBoolean(GameRules.RULE_ANNOUNCE_ADVANCEMENTS)) {
                        if(displayinfo.getType() == AdvancementType.CHALLENGE){
                            this.playerList.broadcastSystemMessage(p_352686_.getType().createAnnouncement(advancement, this.player), false);
                        }
                    }
                    net.neoforged.neoforge.event.EventHooks.onAdvancementEarnedEvent(this.player, advancement);
                });
            }
        }

        if (!flag1 && advancementprogress.isDone()) {
            this.markForVisibilityUpdate(advancement);
        }

        return flag;
    }
}