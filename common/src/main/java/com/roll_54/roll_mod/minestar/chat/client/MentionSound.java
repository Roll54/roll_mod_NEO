package com.roll_54.roll_mod.minestar.chat.client;

import com.roll_54.roll_mod.minestar.chat.ChatChannel;
import com.roll_54.roll_mod.minestar.chat.ChatChannelState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The ping when someone "@Name"s the player ({@code MentionPingPacket}), like a mention on Discord: a short
 * high chime played as a UI sound, so it is heard wherever the player stands, at the volume from hub
 * Settings → Chat (0 is off). A channel set to "All messages" also gets a softer, lower one for every new
 * line, and one set to "Nothing" no sound at all. Ported from Eden_LD's {@code MentionSound}.
 */
@OnlyIn(Dist.CLIENT)
public final class MentionSound {

    private MentionSound() {}

    /** The ping for a mention in {@code channel}, unless that channel is set to "Nothing". */
    public static void ping(ChatChannel channel) {
        if (ChatChannelState.get().notify(channel) == ChatChannelState.Notify.NOTHING) return;
        play();
    }

    public static void play() {
        play(ChatChannelState.get().mentionVolume(), 1.4f);
    }

    /** A new line in a channel set to "All messages": the ping, softer and lower. */
    public static void playSoft() {
        play(ChatChannelState.get().mentionVolume() / 2, 0.9f);
    }

    /** The ping at {@code percent}, whatever the setting says: the preview while the volume is changed. */
    public static void preview(int percent) {
        play(percent, 1.4f);
    }

    /** The ping whatever the setting says. */
    public static void playAnyway() {
        preview(100);
    }

    private static void play(int percent, float pitch) {
        if (percent <= 0) return;
        Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, pitch, percent / 100f));
    }
}
