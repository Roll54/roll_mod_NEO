package com.roll_54.roll_mod.minestar.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Hearts past 20 health the way Scaling Health draws them, while the flat status bars are off: always one row of ten.
 * Every 20 health is a row with a colour of its own (Scaling Health's 14, looping); the row filling now is painted over
 * the full row below it, so that row's colour shows in the hearts not yet reached. Left of the hearts, small, the row
 * count ("3x"), white with a black outline. The coloured hearts are Scaling Health's white heart (MIT) tinted, inside
 * vanilla's own container sprite.
 */
@OnlyIn(Dist.CLIENT)
public final class TieredHearts {

    /** Health per row: ten hearts. */
    public static final int PER_ROW = 20;

    /** Scaling Health's default row colours, in order; past the last they loop. Row 0 is vanilla's red. */
    // Full saturation (one channel at 0, the strongest at FF): the white heart only shades them, so a softer tint
    // read washed out next to vanilla's red. The pale lilac became magenta; grey and white stay last, as before.
    private static final int[] COLORS = {0xBF0000, 0xFF5200, 0xFF9500, 0xFFD500, 0x9DFF00, 0x2EE600, 0x00E68A,
            0x00D8F0, 0x0080FF, 0x1A1AFF, 0x9000FF, 0xF000C8, 0x8C8C8C, 0xE6E6E6};

    private TieredHearts() {}

    /** The colour of row {@code row} (0 for the first 20 health), opaque ARGB. */
    public static int color(int row) {
        return 0xFF000000 | COLORS[Math.floorMod(row, COLORS.length)];
    }

    /** Which row the current health is filling (0 up to 20, 1 up to 40...). */
    public static int row(float health) {
        int h = Mth.ceil(Math.max(0f, health));
        return h <= PER_ROW ? 0 : (h - 1) / PER_ROW;
    }

    /** Whether this player's hearts are drawn here rather than by vanilla: more than one row of them. */
    static boolean applies(LocalPlayer player) {
        return Math.max(player.getMaxHealth(), player.getHealth()) > PER_ROW;
    }

    static void render(GuiGraphics graphics, LocalPlayer player, Gui gui) {
        int left = graphics.guiWidth() / 2 - 91;
        int y = graphics.guiHeight() - gui.leftHeight;
        int health = Mth.ceil(Math.max(0f, player.getHealth()));
        int row = row(health);
        boolean effect = player.hasEffect(MobEffects.WITHER) || player.hasEffect(MobEffects.POISON) || player.isFullyFrozen();
        String effectSprite = player.hasEffect(MobEffects.WITHER) ? "withered_"
                : player.hasEffect(MobEffects.POISON) ? "poisoned_" : "frozen_";
        int rest = health - PER_ROW * row;

        for (int i = 0; i < 10; i++) {
            int x = left + i * 8;
            graphics.blitSprite(sprite("container"), x, y, 9, 9);
            int filled = rest - i * 2;
            if (effect) {
                // A status effect paints every heart in vanilla's own sprite, as vanilla does.
                if (row > 0 || filled > 0) graphics.blitSprite(sprite(effectSprite + (row > 0 || filled >= 2 ? "full" : "half")), x, y, 9, 9);
                continue;
            }
            if (row > 0) heart(graphics, x, y, row - 1, false);
            if (filled >= 2) heart(graphics, x, y, row, false);
            else if (filled == 1) heart(graphics, x, y, row, true);
        }
        rowLabel(graphics, left, y, health, player.getMaxHealth());
        gui.leftHeight += 10;

        // Absorption, on a row of its own above, in the same colours by its own rows.
        int absorption = Mth.ceil(player.getAbsorptionAmount());
        if (absorption > 0) {
            int ay = graphics.guiHeight() - gui.leftHeight;
            int arow = row(absorption);
            int arest = absorption - PER_ROW * arow;
            for (int i = 0; i < 10; i++) {
                int x = left + i * 8;
                int filled = arest - i * 2;
                if (arow == 0 && filled <= 0) continue;
                graphics.blitSprite(sprite("container"), x, ay, 9, 9);
                if (arow > 0) graphics.blitSprite(sprite("absorbing_full"), x, ay, 9, 9);
                if (filled >= 2) graphics.blitSprite(sprite("absorbing_full"), x, ay, 9, 9);
                else if (filled == 1) graphics.blitSprite(sprite("absorbing_half"), x, ay, 9, 9);
            }
            gui.leftHeight += 10;
        }
    }

    /** "3x", small, white with a black outline, right-aligned 3px left of {@code left}. */
    public static void rowLabel(GuiGraphics graphics, int left, int y, float health, float max) {
        Minecraft mc = Minecraft.getInstance();
        String text = (row(health) + 1) + "x";
        // Full size: the digits' 7px plus the 1px outline above and below are the heart's 9px, on whole pixels.
        float scale = 1f;
        graphics.pose().pushPose();
        graphics.pose().translate(left - 3 - mc.font.width(text), y + 1, 0);
        graphics.pose().scale(scale, scale, 1f);
        // The outline: the text in black one pixel off in every direction, then white on top.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx != 0 || dy != 0) graphics.drawString(mc.font, text, dx, dy, 0xFF000000, false);
            }
        }
        graphics.drawString(mc.font, text, 0, 0, 0xFFFFFFFF, false);
        graphics.pose().popPose();
    }

    /**
     * Scaling Health's white heart (its {@code hud.png}, MIT licence, cropped to {@code tinted_hearts.png}: full at 0,0,
     * half at 9,0, the glint at 17,0, each 9x9), tinted in the row's colour with the white glint over it, as it draws them.
     */
    private static final ResourceLocation TINTED =
            ResourceLocation.fromNamespaceAndPath("roll_mod", "textures/gui/hud/tinted_hearts.png");

    /** One heart of row {@code row}'s colour; {@code half} fills only the left half, as vanilla's half heart. */
    private static void heart(GuiGraphics graphics, int x, int y, int row, boolean half) {
        if (row == 0) {
            // The first row is vanilla's own red heart.
            graphics.blitSprite(sprite(half ? "half" : "full"), x, y, 9, 9);
            return;
        }
        int c = color(row);
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        graphics.setColor(((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f, 1f);
        graphics.blit(TINTED, x, y, half ? 9 : 0, 0, 9, 9, 32, 16);
        graphics.setColor(1f, 1f, 1f, 0.8f);
        graphics.blit(TINTED, x, y, 17, 0, 9, 9, 32, 16);
        graphics.setColor(1f, 1f, 1f, 1f);
    }

    private static ResourceLocation sprite(String name) {
        return ResourceLocation.withDefaultNamespace("hud/heart/" + name);
    }
}
