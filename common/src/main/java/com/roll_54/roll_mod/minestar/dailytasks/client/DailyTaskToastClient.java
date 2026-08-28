package com.roll_54.roll_mod.minestar.dailytasks.client;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Client-only tail of {@link com.roll_54.roll_mod.network.packet.DailyTaskToastPacket}. Kept in its
 * own class so the packet class itself stays loadable on a dedicated server.
 */
@OnlyIn(Dist.CLIENT)
public final class DailyTaskToastClient {

    private DailyTaskToastClient() {}

    public static void show(AdvancementHolder advancement) {
        Minecraft.getInstance().getToasts().addToast(new AdvancementToast(advancement));
    }
}
