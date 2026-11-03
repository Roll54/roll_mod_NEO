package com.roll_54.roll_mod.worldgen;

import com.roll_54.roll_mod.RollMod;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;

/**
 * The mining world's sky: the overworld's, without clouds. Its dimension type names these effects
 * ({@code "effects": "roll_mod:mining_overworld"}); a NaN cloud height is how vanilla skips drawing clouds.
 */
@EventBusSubscriber(modid = RollMod.MODID, value = Dist.CLIENT)
public final class MiningWorldClient {

    private MiningWorldClient() {}

    @SubscribeEvent
    public static void registerEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(RollMod.id("mining_overworld"), new NoCloudsEffects());
    }

    private static final class NoCloudsEffects extends DimensionSpecialEffects {

        NoCloudsEffects() {
            super(Float.NaN, true, SkyType.NORMAL, false, false);
        }

        /** As the overworld's. */
        @Override
        public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
            return fogColor.multiply(brightness * 0.94F + 0.06F, brightness * 0.94F + 0.06F, brightness * 0.91F + 0.09F);
        }

        @Override
        public boolean isFoggyAt(int x, int y) {
            return false;
        }
    }
}
