package com.roll_54.roll_mod.mixin.mi;

import aztech.modern_industrialization.machines.multiblocks.world.ChunkEventListeners;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.event.level.ChunkEvent;
import org.spongepowered.asm.mixin.Mixin;

/**
 * MI's multiblock chunk listeners insist on the server thread ("Thread is not server thread!") and crash the game
 * otherwise. With DimThreads every dimension ticks on a thread of its own, so a mob or a machine there that loads or
 * unloads a chunk lands here off that thread. Instead of crashing, the handling is passed to the server thread, which
 * runs it at the end of the tick — this also keeps MI's one shared listener map off several threads at once.
 */
@Mixin(value = ChunkEventListeners.class, remap = false)
public abstract class ChunkEventListenersMixin {

    @WrapMethod(method = "lambda$init$1")
    private static void roll_mod$loadOnServerThread(ChunkEvent.Load event, Operation<Void> original) {
        MinecraftServer server = event.getLevel().getServer();
        if (event.getLevel().isClientSide() || server == null || server.isSameThread()) {
            original.call(event);
        } else {
            server.execute(() -> original.call(event));
        }
    }

    @WrapMethod(method = "lambda$init$2")
    private static void roll_mod$unloadOnServerThread(ChunkEvent.Unload event, Operation<Void> original) {
        MinecraftServer server = event.getLevel().getServer();
        if (event.getLevel().isClientSide() || server == null || server.isSameThread()) {
            original.call(event);
        } else {
            server.execute(() -> original.call(event));
        }
    }
}
