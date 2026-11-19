package com.roll_54.roll_mod.minestar.hud;

import com.google.common.collect.Ordering;
import com.roll_54.roll_mod.RollMod;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.Collection;
import java.util.List;

/**
 * Hovering an effect icon in the top-right of the HUD — possible while {@link FreeCursor} has the
 * cursor free — shows the effect's name, level and time left. The icons are laid out exactly as
 * vanilla's {@code Gui.renderEffects} places them: beneficial effects on the top row, harmful ones on
 * the row below, right to left.
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = RollMod.MODID, value = Dist.CLIENT)
public final class EffectTooltips {

    private static final int ICON = 24;

    private EffectTooltips() {}

    @SubscribeEvent
    public static void onRegisterLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.EFFECTS, RollMod.id("effect_tooltips"), EffectTooltips::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (!FreeCursor.active() || mc.player == null || mc.level == null || mc.options.hideGui) return;
        if (mc.screen instanceof EffectRenderingInventoryScreen<?> s && s.canSeeEffects()) return;
        Collection<MobEffectInstance> effects = mc.player.getActiveEffects();
        if (effects.isEmpty()) return;

        double scaleX = graphics.guiWidth() / (double) mc.getWindow().getScreenWidth();
        double scaleY = graphics.guiHeight() / (double) mc.getWindow().getScreenHeight();
        int mouseX = (int) (mc.mouseHandler.xpos() * scaleX);
        int mouseY = (int) (mc.mouseHandler.ypos() * scaleY);

        int beneficial = 0;
        int harmful = 0;
        for (MobEffectInstance effect : Ordering.natural().reverse().sortedCopy(effects)) {
            if (!IClientMobEffectExtensions.of(effect).isVisibleInGui(effect) || !effect.showIcon()) continue;
            // Vanilla: count the row up first, then step 25px left per icon.
            int y = 1 + (mc.isDemo() ? 15 : 0);
            int x;
            if (effect.getEffect().value().isBeneficial()) {
                x = graphics.guiWidth() - 25 * ++beneficial;
            } else {
                x = graphics.guiWidth() - 25 * ++harmful;
                y += 26;
            }
            if (mouseX >= x && mouseX < x + ICON && mouseY >= y && mouseY < y + ICON) {
                graphics.renderComponentTooltip(mc.font, lines(effect, mc.level.tickRateManager().tickrate()), mouseX, mouseY);
                return;
            }
        }
    }

    private static List<Component> lines(MobEffectInstance effect, float tickRate) {
        MutableComponent name = effect.getEffect().value().getDisplayName().copy();
        if (effect.getAmplifier() > 0) {
            name.append(" ").append(Component.translatable("enchantment.level." + (effect.getAmplifier() + 1)));
        }
        Component time = MobEffectUtil.formatDuration(effect, 1.0f, tickRate);
        return List.of(name.withStyle(effect.getEffect().value().isBeneficial() ? ChatFormatting.WHITE : ChatFormatting.RED),
                Component.translatable("gui.roll_mod.effect.time_left", time).withStyle(ChatFormatting.GRAY));
    }
}
