package com.roll_54.roll_mod.minestar.op;

import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.UUID;

/**
 * One row of {@code minestar/operators.json}: a player, and whether they are to be an operator.
 *
 * <p>The name is what a person hand-editing the file types; the id is what the server matches on
 * once it has seen that player, and is filled in the first time it does.
 */
public record OperatorEntry(String name, UUID id, boolean op, int level) {

    /** What {@code /op} grants by default, and what a row with no level of its own gets. */
    public static final int DEFAULT_LEVEL = 4;

    public OperatorEntry withId(UUID id) {
        return new OperatorEntry(name, id, op, level);
    }

    public OperatorEntry withOp(boolean op) {
        return new OperatorEntry(name, id, op, level);
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        // Clamped rather than trusted: the name comes from a hand-edited file, and writeUtf throws
        // on anything longer than its limit — which would take the connection with it.
        buf.writeUtf(name.length() > 16 ? name.substring(0, 16) : name, 16);
        buf.writeBoolean(id != null);
        if (id != null) buf.writeUUID(id);
        buf.writeBoolean(op);
        buf.writeVarInt(level);
    }

    public static OperatorEntry decode(RegistryFriendlyByteBuf buf) {
        String name = buf.readUtf(16);
        UUID id = buf.readBoolean() ? buf.readUUID() : null;
        return new OperatorEntry(name, id, buf.readBoolean(), buf.readVarInt());
    }
}
