package com.roll_54.roll_mod.minestar.hud;

import com.roll_54.roll_mod.RollMod;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The status rows above the hotbar as flat bars instead of icon rows: health one red bar (absorption as gold diagonal strips over it from the left, tinted while poisoned / withered / freezing), hunger one {@code #C4984C} bar, and armor,
 * air and mount health as matching bars — each in the slot its icons had, so other mods' rows still
 * stack above them ({@link Gui#leftHeight} / {@link Gui#rightHeight}).
 *
 * <p>The vanilla rows are cancelled by name before anything draws them, along with AppleSkin's
 * overlays (which would land on icons that are no longer there) and, through the same cancelled
 * events, Overflowing Bars' replacement hearts and armor. The crosshair also goes while
 * {@link FreeCursor} has the cursor free, since it no longer marks what you would use.
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = RollMod.MODID, value = Dist.CLIENT)
public final class StatusBars {

    private static final Set<ResourceLocation> REPLACED = Set.of(VanillaGuiLayers.PLAYER_HEALTH,
            VanillaGuiLayers.ARMOR_LEVEL, VanillaGuiLayers.FOOD_LEVEL, VanillaGuiLayers.AIR_LEVEL,
            VanillaGuiLayers.VEHICLE_HEALTH);

    private static final int WIDTH = 81;
    /** Tall enough for the numbers written on the bar (17/20); rows stay vanilla's 10px apart. */
    private static final int HEIGHT = 9;
    private static final int ROW = 10;

    private static final int FRAME = 0xFF000000;
    private static final int TRACK = 0xFF2A2A2A;
    private static final int HEALTH = 0xFFD83A3A;
    private static final int POISON = 0xFF6A9A2A;
    private static final int WITHER = 0xFF3A3A3A;
    private static final int FROZEN = 0xFF7AD0F0;
    private static final int ABSORPTION = 0xFFE8C840;
    private static final int ARMOR = 0xFF9AA4AE;
    private static final int FOOD = 0xFFC4984C;
    private static final int FOOD_HUNGER_EFFECT = 0xFF7A8A3A;
    private static final int MOUNT = 0xFFC05050;
    private static final int AIR = 0xFF5AA7E6;

    private StatusBars() {}

    /** Whether the bars replace the vanilla rows (client config hud.statusBars); off, vanilla's own show. */
    static boolean enabled() {
        var config = com.roll_54.roll_mod.config.RollClientConfig.INSTANCE;
        return config == null || config.hud == null || config.hud.statusBars;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLayer(RenderGuiLayerEvent.Pre event) {
        ResourceLocation id = event.getName();
        if (!enabled()) {
            // Vanilla rows stay; only the free cursor still hides the crosshair. Health past 20 is the one exception:
            // one row of tiered hearts (TieredHearts) in place of vanilla's growing stack.
            if (id.equals(VanillaGuiLayers.CROSSHAIR) && FreeCursor.active()) event.setCanceled(true);
            LocalPlayer player = Minecraft.getInstance().player;
            if (id.equals(VanillaGuiLayers.PLAYER_HEALTH) && player != null && TieredHearts.applies(player)) {
                event.setCanceled(true);
            }
            return;
        }
        if (REPLACED.contains(id) || "appleskin".equals(id.getNamespace())
                || (id.equals(VanillaGuiLayers.CROSSHAIR) && FreeCursor.active())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRegisterLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.PLAYER_HEALTH, RollMod.id("status_bars"), StatusBars::render);
        event.registerAbove(VanillaGuiLayers.PLAYER_HEALTH, RollMod.id("tiered_hearts"), (graphics, delta) -> {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (enabled() || player == null || mc.options.hideGui || mc.gameMode == null
                    || !mc.gameMode.canHurtPlayer() || !TieredHearts.applies(player)) return;
            TieredHearts.render(graphics, player, mc.gui);
        });
    }

    private static void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (!enabled() || player == null || mc.options.hideGui || mc.gameMode == null || !mc.gameMode.canHurtPlayer()) return;
        Gui gui = mc.gui;
        List<Hover> hovers = new ArrayList<>();
        int left = graphics.guiWidth() / 2 - 91;
        int right = graphics.guiWidth() / 2 + 10;

        // Left column: health, then armor above it.
        float max = Math.max(player.getMaxHealth(), 1f);
        float health = Math.max(0f, player.getHealth());
        float absorption = player.getAbsorptionAmount();
        float scale = Math.max(max, health);
        int healthColor = player.hasEffect(MobEffects.WITHER) ? WITHER
                : player.hasEffect(MobEffects.POISON) ? POISON
                : player.isFullyFrozen() ? FROZEN : HEALTH;
        int y = graphics.guiHeight() - gui.leftHeight;
        int inner = WIDTH - 2;
        int healthW = Math.round(inner * health / scale);
        // Absorption lies over the health from the left, as diagonal gold strips the red shows between;
        // more absorption than the bar holds fills it.
        int absorbW = Math.min(inner, Math.round(inner * absorption / scale));
        frame(graphics, left, y);
        if (scale > TieredHearts.PER_ROW && healthColor == HEALTH) {
            // Past 20 health the bar works as the hearts do (TieredHearts): it holds one row of 20, the row filling now
            // in its colour over the full row below.
            int row = TieredHearts.row(health);
            float rest = health - TieredHearts.PER_ROW * row;
            if (row > 0) graphics.fill(left + 1, y + 1, left + 1 + inner, y + HEIGHT - 1,
                    row == 1 ? HEALTH : TieredHearts.color(row - 1));
            int restW = Math.round(inner * Math.min(1f, rest / TieredHearts.PER_ROW));
            graphics.fill(left + 1, y + 1, left + 1 + restW, y + HEIGHT - 1, row == 0 ? HEALTH : TieredHearts.color(row));
            // No "2x" here: the bar's colours already show the row; the number is for the hearts only.
        } else {
            graphics.fill(left + 1, y + 1, left + 1 + healthW, y + HEIGHT - 1, healthColor);
        }
        for (int c = 0; c < absorbW; c++) {
            for (int r = 0; r < HEIGHT - 2; r++) {
                // Two pixels of gold, two of gap, rising to the right.
                if (Math.floorMod(c + r, 4) < 2) {
                    int px = left + 1 + c, py = y + HEIGHT - 2 - r;
                    graphics.fill(px, py, px + 1, py + 1, ABSORPTION);
                }
            }
        }
        label(graphics, mc, left, y, absorption > 0 ? number(health) + "+" + number(absorption) + "/" + number(max)
                : number(health) + "/" + number(max));
        hovers.add(new Hover(left, y, absorption > 0
                ? List.of(Component.translatable("gui.roll_mod.bar.health", number(health), number(max)),
                        Component.translatable("gui.roll_mod.bar.absorption", number(absorption)).withStyle(ChatFormatting.GOLD))
                : List.of(Component.translatable("gui.roll_mod.bar.health", number(health), number(max)))));
        gui.leftHeight += ROW;

        int armor = player.getArmorValue();
        if (armor > 0) {
            int ay = graphics.guiHeight() - gui.leftHeight;
            bar(graphics, left, ay, Math.min(armor, 20) / 20f, ARMOR);
            label(graphics, mc, left, ay, armor + "/20");
            double toughness = player.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
            hovers.add(new Hover(left, ay, toughness > 0
                    ? List.of(Component.translatable("gui.roll_mod.bar.armor", armor),
                            Component.translatable("gui.roll_mod.bar.toughness", number((float) toughness)).withStyle(ChatFormatting.GRAY))
                    : List.of(Component.translatable("gui.roll_mod.bar.armor", armor))));
            gui.leftHeight += ROW;
        }

        // Right column: hunger, or the mount's health while riding one; air above.
        int ry = graphics.guiHeight() - gui.rightHeight;
        if (player.getVehicle() instanceof LivingEntity mount && mount.showVehicleHealth()) {
            bar(graphics, right, ry, mount.getHealth() / Math.max(mount.getMaxHealth(), 1f), MOUNT);
            label(graphics, mc, right, ry, number(mount.getHealth()) + "/" + number(mount.getMaxHealth()));
            hovers.add(new Hover(right, ry, List.of(Component.translatable("gui.roll_mod.bar.mount",
                    mount.getDisplayName(), number(mount.getHealth()), number(mount.getMaxHealth())))));
        } else {
            int food = player.getFoodData().getFoodLevel();
            bar(graphics, right, ry, food / 20f, player.hasEffect(MobEffects.HUNGER) ? FOOD_HUNGER_EFFECT : FOOD);
            label(graphics, mc, right, ry, food + "/20");
            hovers.add(new Hover(right, ry, List.of(Component.translatable("gui.roll_mod.bar.hunger", food, 20),
                    Component.translatable("gui.roll_mod.bar.saturation",
                            number(player.getFoodData().getSaturationLevel())).withStyle(ChatFormatting.GRAY))));
        }
        gui.rightHeight += ROW;

        int maxAir = player.getMaxAirSupply();
        int air = Math.min(player.getAirSupply(), maxAir);
        if (player.isEyeInFluid(FluidTags.WATER) || air < maxAir) {
            int airY = graphics.guiHeight() - gui.rightHeight;
            bar(graphics, right, airY, Math.max(air, 0) / (float) maxAir, AIR);
            label(graphics, mc, right, airY, number(Math.max(air, 0) / 20f) + "/" + number(maxAir / 20f));
            hovers.add(new Hover(right, airY, List.of(Component.translatable("gui.roll_mod.bar.air",
                    number(Math.max(air, 0) / 20f), number(maxAir / 20f)))));
            gui.rightHeight += ROW;
        }

        // While the cursor is free, the bar under it says what it shows.
        if (FreeCursor.active()) {
            int mx = FreeCursor.guiMouseX(mc), my = FreeCursor.guiMouseY(mc);
            for (Hover h : hovers) {
                if (mx >= h.x && mx < h.x + WIDTH && my >= h.y - 1 && my < h.y - 1 + ROW) {
                    graphics.renderComponentTooltip(mc.font, h.lines, mx, my);
                    break;
                }
            }
        }
    }

    /** A bar's place (its top-left; the whole 10px row counts) and what its tooltip says. */
    private record Hover(int x, int y, List<Component> lines) {}

    /** 17, 17.5 — no trailing ".0". */
    private static String number(float v) {
        float r = Math.round(v * 10f) / 10f;
        return r == (int) r ? String.valueOf((int) r) : String.valueOf(r);
    }

    /** The bar's numbers, centred on it, white with a shadow so they read over any fill. */
    private static void label(GuiGraphics graphics, Minecraft mc, int x, int y, String text) {
        int w = mc.font.width(text);
        graphics.drawString(mc.font, text, x + (WIDTH - w) / 2, y, 0xFFFFFFFF, true); // 1px higher than before, so the digits sit centred in the bar
    }

    private static void bar(GuiGraphics graphics, int x, int y, float fraction, int color) {
        frame(graphics, x, y);
        int fill = Math.round((WIDTH - 2) * Math.max(0f, Math.min(1f, fraction)));
        if (fill > 0) graphics.fill(x + 1, y + 1, x + 1 + fill, y + HEIGHT - 1, color);
    }

    private static void frame(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + WIDTH, y + HEIGHT, FRAME);
        graphics.fill(x + 1, y + 1, x + WIDTH - 1, y + HEIGHT - 1, TRACK);
    }
}
