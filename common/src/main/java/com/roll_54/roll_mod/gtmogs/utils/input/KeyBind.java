package com.roll_54.roll_mod.gtmogs.utils.input;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import net.neoforged.neoforge.common.NeoForge;

import com.mojang.blaze3d.platform.InputConstants;
import com.roll_54.roll_mod.gtmogs.GTMOGS;
import it.unimi.dsi.fastutil.booleans.BooleanBooleanMutablePair;

import java.util.*;
import java.util.function.Supplier;

@EventBusSubscriber(modid = GTMOGS.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public enum KeyBind {

    VANILLA_JUMP(() -> () -> Minecraft.getInstance().options.keyJump),
    VANILLA_SNEAK(() -> () -> Minecraft.getInstance().options.keyShift),
    VANILLA_FORWARD(() -> () -> Minecraft.getInstance().options.keyUp),
    VANILLA_BACKWARD(() -> () -> Minecraft.getInstance().options.keyDown),
    VANILLA_LEFT(() -> () -> Minecraft.getInstance().options.keyLeft),
    VANILLA_RIGHT(() -> () -> Minecraft.getInstance().options.keyRight),
    ;

    public static final KeyBind[] VALUES = values();

    private static double mouseDeltaX = 0.0;
    private static double mouseDeltaY = 0.0;

    public static void init() {
        GTMOGS.LOGGER.info("Registering KeyBinds");
        if (GTMOGS.isClientSide()) {
            NeoForge.EVENT_BUS.register(KeyBind.class);
        }
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onInputEvent(InputEvent.Key event) {
        List<KeyBind> updating = new ArrayList<>();
        for (KeyBind keybind : VALUES) {
            boolean previousPressed = keybind.isPressed;
            boolean previousKeyDown = keybind.isKeyDown;
            keybind.isPressed = keybind.isPressed();
            keybind.isKeyDown = keybind.isKeyDown();
            if (previousPressed != keybind.isPressed || previousKeyDown != keybind.isKeyDown) {
                updating.add(keybind);
            }
        }
    }

    public static void onRegisterKeyBinds(RegisterKeyMappingsEvent event) {
        Arrays.stream(VALUES).forEach(value -> {
            if (value.keybindingGetter == null) {
                event.register(value.keybinding);
            } else {
                value.keybinding = value.keybindingGetter.get().get();
            }
        });
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        mouseDeltaX = event.getScrollDeltaX();
        mouseDeltaY = event.getScrollDeltaY();
    }

    @OnlyIn(Dist.CLIENT)
    public static boolean scrollingUp() {
        return mouseDeltaY > 0;
    }

    @OnlyIn(Dist.CLIENT)
    public static boolean notScrolling() {
        return mouseDeltaY == 0;
    }

    @OnlyIn(Dist.CLIENT)
    public static boolean scrollingDown() {
        return mouseDeltaY < 0;
    }

    @OnlyIn(Dist.CLIENT)
    private Supplier<Supplier<KeyMapping>> keybindingGetter;
    @OnlyIn(Dist.CLIENT)
    private KeyMapping keybinding;
    @OnlyIn(Dist.CLIENT)
    private boolean isPressed, isKeyDown;

    private final WeakHashMap<ServerPlayer, BooleanBooleanMutablePair> mapping = new WeakHashMap<>();

    // For Vanilla/Other Mod keybinds
    // Double Supplier to keep client classes from loading
    KeyBind(Supplier<Supplier<KeyMapping>> keybindingGetter) {
        if (GTMOGS.isClientSide()) {
            this.keybindingGetter = keybindingGetter;
        }
    }

    KeyBind(String langKey, int button) {
        if (GTMOGS.isClientSide()) {
            this.keybinding = new KeyMapping(langKey, button, GTMOGS.NAME);
        }
    }

    KeyBind(String langKey, IKeyConflictContext ctx, int button) {
        if (GTMOGS.isClientSide()) {
            this.keybinding = new KeyMapping(langKey, ctx, InputConstants.Type.KEYSYM, button, GTMOGS.NAME);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public KeyMapping toMinecraft() {
        return this.keybinding;
    }

    @OnlyIn(Dist.CLIENT)
    public boolean isPressed() {
        return this.keybinding.isDown();
    }

    @OnlyIn(Dist.CLIENT)
    public boolean isKeyDown() {
        return this.keybinding.isDown();
    }

    public void update(boolean pressed, boolean keyDown, ServerPlayer player) {
        BooleanBooleanMutablePair pair = this.mapping.get(player);
        if (pair == null) {
            this.mapping.put(player, BooleanBooleanMutablePair.of(pressed, keyDown));
        } else {
            pair.left(pressed).right(keyDown);
        }
    }

    public boolean isPressed(Player player) {
        if (player.level().isClientSide) {
            return isPressed();
        } else {
            BooleanBooleanMutablePair pair = this.mapping.get((ServerPlayer) player);
            return pair != null && pair.leftBoolean();
        }
    }

    public boolean isKeyDown(Player player) {
        if (player.level().isClientSide) {
            return isKeyDown();
        } else {
            BooleanBooleanMutablePair pair = this.mapping.get((ServerPlayer) player);
            return pair != null && pair.rightBoolean();
        }
    }
}
