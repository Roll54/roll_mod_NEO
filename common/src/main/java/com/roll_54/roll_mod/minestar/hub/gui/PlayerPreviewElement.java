package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.function.BooleanSupplier;

/**
 * A paper-doll of the viewing player, complete with worn armour and whatever is in hand — it draws
 * the live entity, so it needs no per-slot wiring and never goes stale.
 *
 * <p>LdLib2 has no entity or player widget of its own (its {@code Scene} renders a virtual world,
 * which would mean spawning a fake player into a dummy level), so this calls vanilla's inventory
 * paper-doll renderer directly.
 *
 * <p>It also draws <em>through</em> the UI. {@code renderEntityInInventoryFollowsMouse} flushes a
 * render pass of its own, so the model lands on top of whatever the element tree painted before it
 * regardless of {@code zIndex} — which is why anything that must appear over the doll has to tell it
 * to stand down instead, via the {@code suppressed} predicate. The information window is the one
 * thing that currently does.
 *
 * <p>The vanilla call is client-only, and this element is constructed on the dedicated server too,
 * because the hub's tree is built identically on both sides. Java resolves a method reference lazily,
 * so merely constructing this class on the server is safe; the client-only types are nonetheless
 * confined to the nested {@link ClientRender} holder, which the dist check below never reaches on a
 * server. That is the same shape used for the LuckPerms and FTB Quests soft dependencies.
 */
public class PlayerPreviewElement extends UIElement {

    /** How large the model draws, in the same units vanilla's inventory doll uses. */
    private final int scale;

    /**
     * Asked every frame whether to skip the draw.
     *
     * <p>A predicate rather than {@code setDisplay(false)} on the element: a hidden element stops
     * ticking in this library (see {@link HubUI}'s class javadoc), so hiding it would be a heavier
     * thing than "do not paint this frame", and coming back would be a frame late.
     */
    private final BooleanSupplier suppressed;

    public PlayerPreviewElement(int scale, BooleanSupplier suppressed) {
        this.scale = scale;
        this.suppressed = suppressed;
    }

    @Override
    public void drawBackgroundAdditional(GUIContext context) {
        super.drawBackgroundAdditional(context);
        if (FMLEnvironment.dist != Dist.CLIENT || suppressed.getAsBoolean()) return;

        ClientRender.draw(context, getPositionX(), getPositionY(),
                getSizeWidth(), getSizeHeight(), scale, context.mouseX, context.mouseY);
    }

    /** Isolated holder for the client-only rendering call. */
    private static final class ClientRender {

        static void draw(GUIContext context, float x, float y, float w, float h,
                         int scale, int mouseX, int mouseY) {
            net.minecraft.client.player.LocalPlayer player =
                    net.minecraft.client.Minecraft.getInstance().player;
            if (player == null) return;

            // Vanilla takes the box as edges, and puts the model's feet on the bottom edge.
            int x0 = (int) x;
            int y0 = (int) y;
            int x1 = (int) (x + w);
            int y1 = (int) (y + h);

            net.minecraft.client.gui.screens.inventory.InventoryScreen
                    .renderEntityInInventoryFollowsMouse(context.graphics, x0, y0, x1, y1, scale,
                            0.0625F, mouseX, mouseY, player);
        }
    }
}
