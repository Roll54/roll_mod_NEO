package com.roll_54.roll_mod.minestar.kits;

import com.roll_54.roll_mod.compat.curios.CuriosCompat.CurioRow;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * The shape of one kit window: everything both sides need to build the same slots in the same order.
 * LdLib2 matches slots by position and {@code PlayerUIMenuType.openUI} carries no data, so the server
 * sends this just before the open and the client builds from it ({@link ClientKitWindowCache}) — the
 * same arrangement as the moderation inventory window's {@code InvSeeLayout}.
 *
 * @param kit      the kit's id
 * @param title    what the kit is called, {@code &} colours and all
 * @param editable a moderator's editor; otherwise a player's read-only preview
 * @param curios   one row per curio slot type, never cosmetic ones
 */
public record KitWindowLayout(String kit, String title, boolean editable, List<CurioRow> curios) {

    private static final int MAX_CURIO_ROWS = 64;
    private static final int MAX_CURIO_SIZE = 64;

    /** What a client builds when the layout never arrived: no kit slots, nothing to edit. */
    public static final KitWindowLayout EMPTY = new KitWindowLayout("", "", false, List.of());

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(clamp(kit, 32), 32);
        buf.writeUtf(clamp(title, Kit.MAX_DISPLAY), Kit.MAX_DISPLAY * 2);
        buf.writeBoolean(editable);
        int rows = Math.min(MAX_CURIO_ROWS, curios.size());
        buf.writeVarInt(rows);
        for (int i = 0; i < rows; i++) {
            buf.writeUtf(clamp(curios.get(i).id(), 128), 128);
            buf.writeVarInt(Math.min(MAX_CURIO_SIZE, curios.get(i).size()));
        }
    }

    public static KitWindowLayout decode(RegistryFriendlyByteBuf buf) {
        String kit = buf.readUtf(32);
        String title = buf.readUtf(Kit.MAX_DISPLAY * 2);
        boolean editable = buf.readBoolean();
        int rows = Math.min(MAX_CURIO_ROWS, buf.readVarInt());
        List<CurioRow> curios = new ArrayList<>(rows);
        for (int i = 0; i < rows; i++) {
            curios.add(new CurioRow(buf.readUtf(128), Math.min(MAX_CURIO_SIZE, buf.readVarInt()), false));
        }
        return new KitWindowLayout(kit, title, editable, List.copyOf(curios));
    }

    /** The layout as the server will build it, with the limits {@link #encode} applies, so both sides agree. */
    public KitWindowLayout clamped() {
        List<CurioRow> rows = new ArrayList<>();
        for (int i = 0; i < Math.min(MAX_CURIO_ROWS, curios.size()); i++) {
            CurioRow row = curios.get(i);
            rows.add(new CurioRow(clamp(row.id(), 128), Math.min(MAX_CURIO_SIZE, row.size()), false));
        }
        return new KitWindowLayout(clamp(kit, 32), clamp(title, Kit.MAX_DISPLAY), editable, List.copyOf(rows));
    }

    private static String clamp(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max);
    }
}
