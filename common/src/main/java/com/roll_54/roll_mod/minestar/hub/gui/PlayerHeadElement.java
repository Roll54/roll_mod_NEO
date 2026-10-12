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
    /** The {@code textures} property the server looked up, empty when it has none. */
    private final String skin;
    private final String skinSignature;

    public PlayerHeadElement(UUID player, String name) {
        this(player, name, "", "");
    }

    /**
     * With the skin the server fetched for a player the client cannot see — see {@code SkinLookup}.
     * Without it the head asks the server itself ({@code ClientSkinCache}), so an offline player
     * still gets their face from the server's authlib rather than the default one.
     */
    public PlayerHeadElement(UUID player, String name, String skin, String skinSignature) {
        this.player = player;
        this.name = name;
        this.skin = skin == null ? "" : skin;
        this.skinSignature = skinSignature == null ? "" : skinSignature;
    }

    @Override
    public void drawBackgroundAdditional(GUIContext context) {
        super.drawBackgroundAdditional(context);
        if (FMLEnvironment.dist != Dist.CLIENT) return;

        ClientRender.draw(context, player, name, skin, skinSignature, getPositionX(), getPositionY(),
                Math.min(getSizeWidth(), getSizeHeight()));
    }

    /** Isolated holder for the client-only skin lookup and draw. */
    private static final class ClientRender {

        /**
         * Skin lookups for players who are not online, one per player and skin for the session. The
         * supplier answers with the default skin at once and with the real one once the download
         * lands, so the face fills in by itself — and holding on to it means the lookup starts once,
         * not every frame. Keyed by the skin too: a face first drawn before the server's lookup had
         * arrived must not stay the default one once it does.
         */
        private static final java.util.Map<String,
                java.util.function.Supplier<net.minecraft.client.resources.PlayerSkin>> OFFLINE =
                new java.util.concurrent.ConcurrentHashMap<>();

        static void draw(GUIContext context, UUID player, String name, String skin, String signature,
                         float x, float y, float size) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();

            // An online player's skin is already loaded and current; prefer it.
            net.minecraft.client.multiplayer.PlayerInfo info =
                    mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(player);
            net.minecraft.client.resources.PlayerSkin face;
            if (info != null) {
                face = info.getSkin();
            } else {
                // Not handed a skin: the one the server's authlib returned, asked for on first draw.
                if (skin.isEmpty()) {
                    com.roll_54.roll_mod.minestar.friends.ClientSkinCache.Entry known =
                            com.roll_54.roll_mod.minestar.friends.ClientSkinCache.get(player);
                    if (known != null) {
                        skin = known.value();
                        signature = known.signature();
                    }
                }
                String textures = skin, textureSignature = signature;
                face = OFFLINE.computeIfAbsent(player + "|" + textures, key -> mc.getSkinManager()
                        .lookupInsecure(profile(player, name, textures, textureSignature))).get();
            }

            // Drawn the way vanilla's tab list draws it, whatever state LdLib's own pass left behind:
            // flushed first, so nothing of LdLib's batch is still pending over or under it; an
            // untinted shader colour, or the face came out greyed by the last element's colour; and
            // nearest filtering on the skin, or an 8px face scaled up to 32 came out blurred.
            context.graphics.flush();
            mc.getTextureManager().getTexture(face.texture()).setFilter(false, false);
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
            // Whole pixels: a fractional origin leaves the face's 4px texels straddling screen pixels.
            net.minecraft.client.gui.components.PlayerFaceRenderer.draw(
                    context.graphics, face, Math.round(x), Math.round(y), Math.round(size));
            context.graphics.flush();
        }

        /**
         * The profile to look the skin up from. Carries the server-fetched textures property when
         * there is one, which is what lets the skin manager download a skin at all; the lookup is
         * the insecure one, so an unsigned property from a third-party auth server is accepted.
         */
        private static com.mojang.authlib.GameProfile profile(UUID id, String name, String skin, String signature) {
            com.mojang.authlib.GameProfile profile = new com.mojang.authlib.GameProfile(id, name);
            if (!skin.isEmpty()) {
                profile.getProperties().put("textures", new com.mojang.authlib.properties.Property(
                        "textures", skin, signature.isEmpty() ? null : signature));
            }
            return profile;
        }
    }
}
