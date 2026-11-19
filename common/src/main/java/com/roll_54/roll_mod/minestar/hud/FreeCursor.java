package com.roll_54.roll_mod.minestar.hud;

import com.mojang.blaze3d.platform.InputConstants;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.mixin.client.MinecraftUseInvoker;
import com.roll_54.roll_mod.mixin.client.MouseHandlerAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.util.Lazy;
import org.lwjgl.glfw.GLFW;

/**
 * Hold Left Alt (rebindable) to free the mouse from the camera: the cursor appears, the view stops
 * turning, and right-clicking uses whatever is under the cursor — a chest to the side, a button, a
 * villager — exactly as if you were looking at it. Left and middle clicks do nothing while it is held.
 * Movement keys keep working; letting go of Alt captures the mouse again.
 *
 * <p>The cursor's target is put where vanilla keeps the crosshair's ({@link CursorPick}), so the block
 * outline, Jade and the use itself all follow the cursor; the click goes through vanilla's own
 * {@code startUseItem}. Nothing new crosses the network: the server already checks reach by distance.
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = RollMod.MODID, value = Dist.CLIENT)
public final class FreeCursor {

    public static final Lazy<KeyMapping> KEY = Lazy.of(() -> new KeyMapping(
            "key.roll_mod.free_cursor", GLFW.GLFW_KEY_LEFT_ALT, "key.categories.roll_mod"));

    private static boolean active;
    /** Whether we let go of the mouse, so only we take it back. */
    private static boolean releasedByUs;
    /**
     * Where the cursor was while free. Vanilla re-centres it on every release, so anything that grabs
     * the mouse for a moment while Alt is held — a screen closing, a grab the server's reply triggers,
     * the window losing focus — would otherwise throw it back to the middle when it is released again.
     */
    private static double cursorX = -1;
    private static double cursorY = -1;

    private FreeCursor() {}

    /** Whether the cursor is free right now. */
    public static boolean active() {
        return active;
    }

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(KEY.get());
    }

    /** Checked every frame rather than every tick, so the cursor follows Alt without a delay. */
    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        boolean held = mc.player != null && mc.screen == null && mc.isWindowActive() && keyHeld(mc);
        if (held) {
            boolean again = active;
            active = true;
            if (mc.mouseHandler.isMouseGrabbed()) {
                mc.mouseHandler.releaseMouse();
                releasedByUs = true;
                // Grabbed back mid-hold: put the cursor where it was, not where vanilla left it.
                if (again && cursorX >= 0) moveCursor(mc, cursorX, cursorY);
            } else {
                cursorX = mc.mouseHandler.xpos();
                cursorY = mc.mouseHandler.ypos();
            }
        } else if (active) {
            active = false;
            cursorX = cursorY = -1;
            if (releasedByUs && mc.screen == null && !mc.mouseHandler.isMouseGrabbed()) mc.mouseHandler.grabMouse();
            releasedByUs = false;
        }
    }

    private static void moveCursor(Minecraft mc, double x, double y) {
        GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), x, y);
        ((MouseHandlerAccessor) mc.mouseHandler).roll_mod$setXpos(x);
        ((MouseHandlerAccessor) mc.mouseHandler).roll_mod$setYpos(y);
    }

    /** The cursor in GUI-scaled coordinates, for HUD hover checks. */
    public static int guiMouseX(Minecraft mc) {
        return (int) (mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth());
    }

    public static int guiMouseY(Minecraft mc) {
        return (int) (mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight());
    }

    /**
     * The raw key state: KeyMapping's own "down" flag is cleared whenever the mouse is released or a
     * screen opens, and this has to keep working through both.
     */
    private static boolean keyHeld(Minecraft mc) {
        InputConstants.Key key = KEY.get().getKey();
        if (key.getType() != InputConstants.Type.KEYSYM || key.getValue() == InputConstants.UNKNOWN.getValue()) {
            return KEY.get().isDown();
        }
        return InputConstants.isKeyDown(mc.getWindow().getWindow(), key.getValue());
    }

    /**
     * A mouse button while the cursor is free. Right press: use what is under the cursor. Everything
     * else is swallowed — no attack, and no click grabbing the mouse back. Returns whether it was handled.
     */
    public static boolean onMouseButton(int button, int action) {
        Minecraft mc = Minecraft.getInstance();
        if (!active || mc.screen != null) return false;
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && action == GLFW.GLFW_PRESS && mc.gameMode != null) {
            ((MinecraftUseInvoker) mc).roll_mod$startUseItem();
        }
        return true;
    }
}
