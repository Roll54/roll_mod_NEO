package com.roll_54.roll_mod.minestar.hub.warp;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.roll_54.roll_mod.RollMod;
import net.minecraft.network.chat.Component;

/**
 * Where a warp stands with moderation.
 *
 * <p>The point of the review is safety: a warp can drop a visitor anywhere, so a moderator vouching
 * for one is what makes it safe to use. Enforcement of the rules themselves is a server matter — all
 * this does is gate the teleport and say who to talk to.
 */
public enum WarpApproval {

    /** Freshly made and not looked at yet. Usable, but unvouched for. */
    NOT_YET("not_yet", 0xFFFFD24A),

    /** A moderator has checked it. */
    APPROVED("approved", 0xFF55DD55),

    /** Blocked: only the owner may travel there, and they are told who to appeal to. */
    DISAPPROVED("disapproved", 0xFFFF5555),

    /** An official warp — events, shops and the like. Pinned to the top, owner not shown. */
    ADMIN("admin", 0xFF55DDDD);

    private final String id;
    private final int color;

    WarpApproval(String id, int color) {
        this.id = id;
        this.color = color;
    }

    public String id() {
        return id;
    }

    /**
     * The colour this state's <em>text</em> is drawn in — the note under a warp's title. Amber for
     * unreviewed rather than red: an unreviewed warp is normal and usable, and colouring it like a
     * blocked one would say the opposite.
     *
     * <p>No longer tints {@link #icon()}: the badges are now artwork drawn in their own colours, and
     * repainting a green tick amber would contradict the shape.
     */
    public int color() {
        return color;
    }

    /**
     * The sprite this state wears in the warp list's left-hand slot, from
     * {@code assets/roll_mod/textures/gui/hub/warp/<id>.png}.
     *
     * <p>Shape rather than colour is what carries the meaning here, so the four states stay apart for
     * a player who cannot tell the four {@link #color()}s from each other; the tint is a second,
     * redundant channel rather than the only one. That is also why every row shows a badge — the slot
     * used to be filled for {@link #ADMIN} alone, which said nothing about the other three.
     *
     * <p>The artwork is drawn in its own colours and is <em>not</em> tinted with {@link #color()}: a
     * green tick recoloured amber would say the opposite of what it draws. {@link #color()} still
     * paints the text beside it, which is where the state's colour now lives.
     *
     * <p>The four files are named after {@link #id()}, so this stays one line as states are added —
     * but a missing file draws the pink-and-black checkerboard with nothing in the log to say why, so
     * a new state needs its PNG in the same commit. {@code admin.png} is 32x32 and the rest 16x16;
     * {@link SpriteTexture} scales to the element box either way.
     *
     * <p>Deliberately not an {@code ItemStackTexture}, which would let real items stand in: that
     * draws outside the UI batch and keeps painting after its row has scrolled out of the viewport.
     * See {@code HubUI.HubTab.texture()}.
     */
    public IGuiTexture icon() {
        return SpriteTexture.of(RollMod.id("textures/gui/hub/warp/" + id + ".png"));
    }

    /** {@code gui.roll_mod.hub.warp.approval.<id>} — the line shown on the warp's detail panel. */
    public Component line() {
        return Component.translatable("gui." + RollMod.MODID + ".hub.warp.approval." + id);
    }

    /**
     * The sentence under {@link #line()} saying what the state means for someone about to travel.
     * The states are not equally obvious — "Awaiting review" in particular reads as a warning when
     * it is not one — so each says plainly whether to go and what to watch for.
     */
    public Component note() {
        return Component.translatable("gui." + RollMod.MODID + ".hub.warp.approval." + id + ".note");
    }

    /** Unknown or absent values read as {@link #NOT_YET}, so old data and typos both stay usable. */
    public static WarpApproval byId(String id) {
        for (WarpApproval approval : values()) {
            if (approval.id.equals(id)) return approval;
        }
        return NOT_YET;
    }

    /** What {@code /rollmod admin warps approve} accepts: {@code true|false|admin}. */
    public static WarpApproval byCommandArgument(String argument) {
        return switch (argument.toLowerCase()) {
            case "true" -> APPROVED;
            case "false" -> DISAPPROVED;
            case "admin" -> ADMIN;
            default -> NOT_YET;
        };
    }
}
