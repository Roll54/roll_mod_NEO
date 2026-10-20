package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.roll_54.roll_mod.minestar.hub.gui.HubWindowState.PinKind;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * Personal pins: homes, warps and players a viewer floats to the top of their lists. Kept in {@link
 * HubWindowState}, so they are this machine's and never the server's — the server copy of the hub
 * tree sees nothing pinned and never writes a client's file.
 */
public final class HubPins {

    /** The marker on a pinned row. */
    public static final String MARK = "⚑";

    private HubPins() {}

    public static boolean pinned(Player viewer, PinKind kind, @Nullable UUID id) {
        return id != null && viewer.level().isClientSide()
                && HubWindowState.get().isPinned(viewer.getUUID(), kind, id);
    }

    public static void set(Player viewer, PinKind kind, UUID id, boolean pinned) {
        if (!viewer.level().isClientSide()) return;
        HubWindowState.get().setPinned(viewer.getUUID(), kind, id, pinned);
    }

    /** Changes whenever any pin does; part of the list signatures so a toggle rebuilds the list. */
    public static int version(Player viewer) {
        return viewer.level().isClientSide() ? HubWindowState.get().pinVersion() : 0;
    }

    /**
     * Splits {@code entries} into the pinned ones (moved out, in list order) and leaves the rest in
     * place — so a pinned entry shows once, at the top, rather than twice.
     */
    public static <T> List<T> takePinned(Player viewer, PinKind kind, List<T> entries, Function<T, UUID> id) {
        List<T> pinned = new ArrayList<>();
        entries.removeIf(entry -> {
            if (!pinned(viewer, kind, id.apply(entry))) return false;
            pinned.add(entry);
            return true;
        });
        return pinned;
    }

    /** "Pin" / "Unpin" for the chosen entry, flipping itself when clicked. */
    public static Button toggle(Player viewer, PinKind kind, UUID id) {
        Button button = new Button();
        Runnable label = () -> button.setText(pinned(viewer, kind, id)
                ? Component.translatable("gui.roll_mod.hub.unpin")
                : Component.literal(MARK + " ").append(Component.translatable("gui.roll_mod.hub.pin"))
                        .withStyle(ChatFormatting.WHITE));
        label.run();
        button.setOnClick(e -> {
            set(viewer, kind, id, !pinned(viewer, kind, id));
            label.run();
        });
        return button;
    }
}
