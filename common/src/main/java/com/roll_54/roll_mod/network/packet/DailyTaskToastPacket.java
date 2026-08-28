package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.dailytasks.client.DailyTaskToastClient;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server → client: show the vanilla advancement toast for a finished daily task.
 *
 * <p>The whole {@link AdvancementHolder} is shipped over the wire (its criteria and requirements
 * are empty, so this is small) and handed straight to {@code AdvancementToast}, which is why the
 * toast is pixel-identical to a real advancement's.
 */
public record DailyTaskToastPacket(AdvancementHolder advancement) implements CustomPacketPayload {

    public static final Type<DailyTaskToastPacket> TYPE =
            new Type<>(RollMod.id("daily_task_toast"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DailyTaskToastPacket> STREAM_CODEC =
            AdvancementHolder.STREAM_CODEC.map(
                    DailyTaskToastPacket::new, DailyTaskToastPacket::advancement);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DailyTaskToastPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Guarded so DailyTaskToastClient — which touches net.minecraft.client — is never
            // class-loaded on a dedicated server, even if this handler were somehow reached there.
            if (FMLEnvironment.dist == Dist.CLIENT) {
                DailyTaskToastClient.show(payload.advancement());
            }
        });
    }
}
