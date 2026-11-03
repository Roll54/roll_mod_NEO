package com.roll_54.roll_mod.gtmogs.core.mixins;

import net.minecraft.server.network.ServerGamePacketListenerImpl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerGamePacketListenerImpl.class)
public interface ServerGamePacketListenerImplAccessor {

    @Accessor
    void setAboveGroundTickCount(int aboveGroundTickCount);
}
