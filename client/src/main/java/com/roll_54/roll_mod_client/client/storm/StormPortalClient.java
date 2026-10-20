package com.roll_54.roll_mod_client.client.storm;

import com.roll_54.roll_mod.netherstorm.ClientStormCache;
import com.roll_54.roll_mod_client.RollModClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * Client half of the storm portal look: wraps the portal models at bake time, and rebuilds chunk
 * meshes whenever the synced storm flag flips — portal quads are baked into the chunk mesh, so the
 * new texture would otherwise only show up as chunks happen to re-mesh.
 */
@EventBusSubscriber(modid = RollModClient.MODID, value = Dist.CLIENT)
public final class StormPortalClient {
    private StormPortalClient() {}

    private static boolean lastSeen;

    /** True only while a nether portal is spawning its ambient particles (render thread only). */
    public static boolean spawningPortalParticles;

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        for (BlockState state : Blocks.NETHER_PORTAL.getStateDefinition().getPossibleStates()) {
            ModelResourceLocation location = BlockModelShaper.stateToModelLocation(state);
            event.getModels().computeIfPresent(location, (key, model) -> new StormPortalModel(model));
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        boolean active = ClientStormCache.isActive();
        if (active == lastSeen) return;
        lastSeen = active;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            mc.levelRenderer.allChanged();
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientStormCache.set(false);
    }
}
