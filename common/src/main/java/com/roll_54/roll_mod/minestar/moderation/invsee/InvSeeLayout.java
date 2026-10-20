package com.roll_54.roll_mod.minestar.moderation.invsee;

import com.roll_54.roll_mod.compat.curios.CuriosCompat.CurioRow;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The shape of one inventory window: everything both sides need to build the same slots in the
 * same order. LdLib2 matches slots by position, and {@code PlayerUIMenuType.openUI} carries no
 * data, so the server sends this just before the open and the client builds from it (see
 * {@link ClientInvSeeCache}).
 *
 * @param offline   the target was offline. Only the vanilla inventory exists then, read from and
 *                  written to their saved playerdata file.
 * @param editable  the slots take and accept items; otherwise the window is read-only.
 * @param curios    one row per curio slot type, cosmetic rows included; empty when offline.
 * @param cyberware the implants page has slots; false without Create Cybernetics.
 */
public record InvSeeLayout(UUID target, String name, boolean offline, boolean editable, Page page,
                           List<CurioRow> curios, boolean cyberware) {

    /** What the window shows. Append-only: the ordinal goes over the wire. */
    public enum Page { INVENTORY, IMPLANTS }

    /** Vanilla's 36 main slots, 4 armour and the offhand, in {@code Inventory}'s own numbering. */
    public static final int INVENTORY_SLOTS = 41;

    private static final int MAX_CURIO_ROWS = 64;
    private static final int MAX_CURIO_SIZE = 64;

    /** What a client builds when the layout never arrived: no slots but the moderator's own. */
    public static final InvSeeLayout EMPTY = new InvSeeLayout(new UUID(0L, 0L), "", true, false,
            Page.INVENTORY, List.of(), false);

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(target);
        buf.writeUtf(clamp(name, 64), 64);
        buf.writeBoolean(offline);
        buf.writeBoolean(editable);
        buf.writeVarInt(page.ordinal());
        int rows = Math.min(MAX_CURIO_ROWS, curios.size());
        buf.writeVarInt(rows);
        for (int i = 0; i < rows; i++) {
            CurioRow row = curios.get(i);
            buf.writeUtf(clamp(row.id(), 128), 128);
            buf.writeVarInt(Math.min(MAX_CURIO_SIZE, row.size()));
            buf.writeBoolean(row.cosmetic());
        }
        buf.writeBoolean(cyberware);
    }

    public static InvSeeLayout decode(RegistryFriendlyByteBuf buf) {
        UUID target = buf.readUUID();
        String name = buf.readUtf(64);
        boolean offline = buf.readBoolean();
        boolean editable = buf.readBoolean();
        Page page = Page.values()[Math.floorMod(buf.readVarInt(), Page.values().length)];
        int rows = Math.min(MAX_CURIO_ROWS, buf.readVarInt());
        List<CurioRow> curios = new ArrayList<>(rows);
        for (int i = 0; i < rows; i++) {
            curios.add(new CurioRow(buf.readUtf(128), Math.min(MAX_CURIO_SIZE, buf.readVarInt()),
                    buf.readBoolean()));
        }
        boolean cyberware = buf.readBoolean();
        return new InvSeeLayout(target, name, offline, editable, page, List.copyOf(curios), cyberware);
    }

    /**
     * The layout as the server will build it, with the limits {@link #encode} applies, so both
     * sides agree on the slot count even for a player with an absurd curio setup.
     */
    public InvSeeLayout clamped() {
        List<CurioRow> rows = new ArrayList<>();
        for (int i = 0; i < Math.min(MAX_CURIO_ROWS, curios.size()); i++) {
            CurioRow row = curios.get(i);
            rows.add(new CurioRow(clamp(row.id(), 128), Math.min(MAX_CURIO_SIZE, row.size()), row.cosmetic()));
        }
        return new InvSeeLayout(target, clamp(name, 64), offline, editable, page, List.copyOf(rows), cyberware);
    }

    private static String clamp(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max);
    }
}
