package com.roll_54.roll_mod.mixin.antidupe;

import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class TridentChargeMixin {

    @Shadow
    private PacketListener packetListener;

    @Inject(
            method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    protected void onChannelRead0(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        if (this.packetListener instanceof ServerGamePacketListenerImpl serverListener) {
            ServerPlayer player = serverListener.player;
            if (packet instanceof ServerboundContainerClickPacket) {
                if (player.getTicksUsingItem() != 0 || player.getUseItemRemainingTicks() != 0) {
                    ci.cancel();
                }
            }
        }
    }
}