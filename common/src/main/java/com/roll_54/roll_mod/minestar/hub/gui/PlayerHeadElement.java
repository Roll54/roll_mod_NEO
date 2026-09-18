package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.UUID;

/**
 * A player's face — skin plus hat layer — the way vanilla draws it in the tab list.
 *
 * <p>LdLib2 has no head or skin texture, and an {@code ItemStackTexture} of a player head will not
 * do inside a scroller: it draws outside the UI's batch and always on top, so a row scrolled half out
 * of the list would keep painting its head over the edge. This calls vanilla's
 * {@code PlayerFaceRenderer} from the element's own background pass instead, which is a plain blit
 * under the scroller's scissor and is clipped with its row.
 *
 * <p>Built on the dedicated server too, since the hub's tree is built identically on both sides, so
 * it holds only a UUID and a name; every client-only type lives in {@link ClientRender}, which the
 * dist check never reaches on a server — the same shape as {@link PlayerPreviewElement}.
 */
public class PlayerHeadElement extends UIElement {

    private final UUID player;
    private final String name;

    public PlayerHeadElement(UUID player, String name) {
        this.player = player;
        this.name = name;
    }

    @Override
    public void drawBackgroundAdditional(GUIContext context) {
        super.drawBackgroundAdditional(context);
        if (FMLEnvironment.dist != Dist.CLIENT) return;

        ClientRender.draw(context, player, name, getPositionX(), getPositionY(),
                Math.min(getSizeWidth(), getSizeHeight()));
    }

    /** Isolated holder for the client-only skin lookup and draw. */
    private static final class ClientRender {

        /**
         * Skin lookups for players who are not online, one per player for the session. The supplier
         * answers with the default skin at once and with the real one once the download lands, so the
         * face fills in by itself — and holding on to it means the lookup starts once, not every frame.
         */
        private static final java.util.Map<UUID,
                java.util.function.Supplier<net.minecraft.client.resources.PlayerSkin>> OFFLINE =
                new java.util.concurrent.ConcurrentHashMap<>();

        static void draw(GUIContext context, UUID player, String name, float x, float y, float size) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();

            // An online player's skin is already loaded and current; prefer it.
            net.minecraft.client.multiplayer.PlayerInfo info =
                    mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(player);
            net.minecraft.client.resources.PlayerSkin skin = info != null
                    ? info.getSkin()
                    : OFFLINE.computeIfAbsent(player, id -> mc.getSkinManager()
                            .lookupInsecure(new com.mojang.authlib.GameProfile(id, name))).get();

            net.minecraft.client.gui.components.PlayerFaceRenderer.draw(
                    context.graphics, skin, (int) x, (int) y, (int) size);
        }
    }
}
