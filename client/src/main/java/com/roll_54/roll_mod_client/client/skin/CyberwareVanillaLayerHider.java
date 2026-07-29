package com.roll_54.roll_mod_client.client.skin;

import com.perigrine3.createcybernetics.common.capabilities.PlayerCyberwareData;
import com.roll_54.roll_mod_client.RollModClient;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hides the vanilla "wear"/second-layer parts (sleeve, pants) of any body region that has a roll_mod
 * cyberware overlay installed, so the player's skin overlay does not bleed through the cyberlimb.
 *
 * <p>CC's {@code CyberwareLimbHider} keeps the base limb <em>visible</em> when a tagged item is
 * installed (CC's own cyberlimbs repaint the skin in place and need the geometry visible). roll_mod
 * instead draws an opaque overlay via {@link CyberwareSkinLayer}, but its textures leave the
 * second-layer UV region transparent — so the vanilla skin overlay drawn on {@code rightPants}/
 * {@code rightSleeve}/... shows through. We suppress only that second layer; the base stays visible so
 * both CC's repaint and roll_mod's base overlay keep working.
 *
 * <p>Runs at {@link EventPriority#LOWEST} on {@code Pre} so it applies after CC's hider (default
 * priority), and restores the original visibility on {@code Post}. Third-person only, same as
 * {@link CyberwareSkinLayer}.
 */
@EventBusSubscriber(modid = RollModClient.MODID, value = Dist.CLIENT)
public final class CyberwareVanillaLayerHider {
    private CyberwareVanillaLayerHider() {}

    /** Parts hidden this frame, per player id, so {@code Post} restores exactly what we changed. */
    private static final Map<Integer, List<ModelPart>> HIDDEN = new HashMap<>();

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        if (!(event.getEntity() instanceof AbstractClientPlayer player)) return;
        if (player.isInvisible()) return;
        if (!(event.getRenderer() instanceof PlayerRenderer renderer)) return;
        if (!(renderer.getModel() instanceof PlayerModel<?> model)) return;

        PlayerCyberwareData data = PlayerCyberwareData.getForVisual(player, player.registryAccess());
        if (data == null) return;

        List<ModelPart> hidden = null;
        for (CyberwareOverlay overlay : CyberwareOverlays.all()) {
            if (!data.hasSpecificItem(overlay.item().get(), overlay.slot())) {
                continue;
            }
            for (ModelPart part : overlay.region().secondLayerParts(model)) {
                if (part.visible) {
                    part.visible = false;
                    if (hidden == null) hidden = new ArrayList<>();
                    hidden.add(part);
                }
            }
        }

        if (hidden != null) {
            HIDDEN.put(player.getId(), hidden);
        }
    }

    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        if (!(event.getEntity() instanceof AbstractClientPlayer player)) return;

        List<ModelPart> hidden = HIDDEN.remove(player.getId());
        if (hidden == null) return;

        for (ModelPart part : hidden) {
            part.visible = true;
        }
    }
}
