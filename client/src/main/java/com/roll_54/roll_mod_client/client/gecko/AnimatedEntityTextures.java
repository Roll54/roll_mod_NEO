package com.roll_54.roll_mod_client.client.gecko;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod_client.RollModClient;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import software.bernie.geckolib.cache.texture.AnimatableTexture;

import java.util.List;

/**
 * Animates entity textures that carry a {@code .mcmeta} animation outside of a GeckoLib renderer.
 *
 * <p>GeckoLib's {@code TextureManagerMixin} already loads every animated texture as an
 * {@link AnimatableTexture} showing its first frame, but only its own renderers ever advance one.
 * Vanilla armor layers and the cyberware limb overlays are drawn by plain {@code RenderType}s, so
 * this steps each listed texture to the current frame once per rendered frame instead. Adding a
 * texture here is all it takes — the strip and its {@code .mcmeta} do the rest.
 */
@EventBusSubscriber(modid = RollModClient.MODID, value = Dist.CLIENT)
public final class AnimatedEntityTextures {
    private AnimatedEntityTextures() {}

    private static final List<ResourceLocation> TEXTURES = List.of(
            RollMod.id("textures/models/armor/meteorite_layer_1.png"),
            RollMod.id("textures/models/armor/meteorite_layer_2.png"),
            RollMod.id("textures/entity/cybernetic/limb/meteorite_metal_leftarm_slim.png"),
            RollMod.id("textures/entity/cybernetic/limb/meteorite_metal_rightarm_slim.png"),
            RollMod.id("textures/entity/cybernetic/limb/meteorite_metal_leftleg.png"),
            RollMod.id("textures/entity/cybernetic/limb/meteorite_metal_rightleg.png"));

    @SubscribeEvent
    public static void onRenderFrame(RenderFrameEvent.Pre event) {
        if (Minecraft.getInstance().level == null) return;
        for (ResourceLocation texture : TEXTURES) {
            AnimatableTexture.setAndUpdate(texture);
        }
    }
}
