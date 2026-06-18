package com.roll_54.roll_mod.mixin.antidupe;

import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class ConnectionMixin {

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

            if (packet instanceof ServerboundEditBookPacket bookPacket) {
                ItemStack bookStack = player.getInventory().getItem(bookPacket.slot());

                if (bookPacket.title().isPresent() && bookPacket.title().get().length() > 32) {
                    ci.cancel();
                }

                if (bookStack.getItem() != Items.WRITABLE_BOOK) {
                    ci.cancel();
                }
            }
        }
    }
}