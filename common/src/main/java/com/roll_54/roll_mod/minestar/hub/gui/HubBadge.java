package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.hub.warp.ClientWarpCache;
import com.roll_54.roll_mod.minestar.hub.warp.Warp;
import com.roll_54.roll_mod.minestar.hub.warp.WarpApproval;
import com.roll_54.roll_mod.economy.vendingblock.client.ClientAuctionCache;
import com.roll_54.roll_mod.minestar.hub.home.ClientHomeCache;
import com.roll_54.roll_mod.minestar.hub.home.HomeView;
import com.roll_54.roll_mod.minestar.moderation.ClientModerationCache;
import com.roll_54.roll_mod.minestar.moderation.ClientPlayerStatusCache;
import com.roll_54.roll_mod.minestar.tpa.ClientTpaCache;
import com.roll_54.roll_mod.minestar.tpa.TpaRequest;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * The "needs your attention" mark: one overlay drawn at the top-right of whatever icon it belongs
 * to — a hub bookmark, the inventory's hub button — rather than a second copy of every icon with the
 * mark baked in. One asset serves every badge, and changing its look is one file.
 *
 * <p>The art is {@code textures/gui/hub/exclamation_mark.png}. Until it exists the badge falls back
 * to the gold dot the hub button used to draw, so a missing file never shows the pink-and-black
 * checkerboard.
 */
public final class HubBadge {

    public static final ResourceLocation TEXTURE = RollMod.id("textures/gui/hub/exclamation_mark.png");

    /** Drawn size, in GUI pixels. */
    public static final int SIZE = 8;

    public static final int DOT_FILL = 0xFFE8A317;
    public static final int DOT_RIM = 0xFF3A2600;

    private static Boolean present;

    private HubBadge() {}

    /** Whether the PNG is there to draw. Asked once per session; off the client, never. */
    public static boolean hasTexture() {
        if (present == null) {
            present = FMLEnvironment.dist.isClient() && ClientCheck.exists(TEXTURE);
        }
        return present;
    }

    /** The PNG, or a rimmed gold dot in its place. */
    public static IGuiTexture texture() {
        return hasTexture()
                ? SpriteTexture.of(TEXTURE)
                : new GuiTextureGroup(new ColorRectTexture(DOT_FILL), new ColorBorderTexture(1, DOT_RIM));
    }

    /** The moderation tab wants attention while a warp waits for a verdict or has been reported. */
    public static boolean moderationPending() {
        if (!ClientModerationCache.ALLOWED) return false;
        for (Warp warp : ClientWarpCache.WARPS) {
            if (warp.approval() == WarpApproval.NOT_YET || !warp.reports().isEmpty()) return true;
        }
        return false;
    }

    /** The home tab shows the player's standing; a mute they have not looked at yet wants attention. */
    public static boolean homePending() {
        return ClientPlayerStatusCache.muteUnseen();
    }

    /** A daily task finished but not collected, or the all-complete bonus waiting. */
    public static boolean dailyPending() {
        return ClientPlayerStatusCache.DAILY_CLAIMABLE > 0;
    }

    /** Someone is waiting on an answer to a teleport request. */
    public static boolean tpaPending() {
        long now = System.currentTimeMillis();
        for (TpaRequest request : ClientTpaCache.INCOMING) {
            if (request.remainingMillis(now) > 0L) return true;
        }
        return false;
    }

    /** Auction items — expired, cancelled or won — waiting in the claim list. */
    public static boolean auctionPending() {
        return !ClientAuctionCache.CLAIMS.isEmpty();
    }

    /** Someone invited this player to a home and is waiting on the answer. */
    public static boolean homeInvitePending() {
        for (HomeView home : ClientHomeCache.HOMES) {
            if (home.relation() == HomeView.Relation.INVITED) return true;
        }
        return false;
    }

    /**
     * Whether the inventory's hub button should wear the badge: anything any tab would badge.
     *
     * <p>TPA, auction and homes come from caches the server fills once the hub has been opened this
     * session; before that they read empty and simply do not contribute, while the status-packet
     * sources (daily rewards, letters, mute) work from login.
     */
    public static boolean hubButton() {
        return ClientPlayerStatusCache.attention() || moderationPending() || tpaPending()
                || auctionPending() || homeInvitePending();
    }

    /** Isolated so the dedicated server never resolves the client's resource manager. */
    private static final class ClientCheck {
        static boolean exists(ResourceLocation location) {
            return net.minecraft.client.Minecraft.getInstance().getResourceManager()
                    .getResource(location).isPresent();
        }
    }
}
