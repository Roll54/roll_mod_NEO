package com.roll_54.roll_mod.cosmetics.client;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.roll_54.roll_mod.cosmetics.ItemSkinDefinition;
import com.roll_54.roll_mod.cosmetics.ItemSkinRegistry;
import com.roll_54.roll_mod.minestar.hub.gui.SkinDollElement;
import com.roll_54.roll_mod.minestar.theme.RollUi;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * A random skin opened like a case: a strip of skins runs past a marker, slows down and stops on the one the server
 * already unlocked ({@code SkinRoll}). Esc or Close leaves at any point; the skin is the player's either way.
 */
@OnlyIn(Dist.CLIENT)
public final class SkinRollScreen extends ModularUIScreen {

    private static final int CARD = 64;
    private static final int GAP = 4;
    private static final int STEP = CARD + GAP;
    private static final int STRIP_W = 5 * STEP + 2 * CARD;
    private static final int STRIP_H = CARD + 12;
    private static final long SPIN_MS = 6000;
    /** Case-style rarity colours, given to each skin by its id so the same skin always wears the same. */
    private static final int[] RARITY = {0xFF4B69FF, 0xFF8847FF, 0xFFD32CE6, 0xFFEB4B4B, 0xFFE4AE39};

    private SkinRollScreen(UIElement root) {
        super(ModularUI.of(UI.of(root, RollUi.sheets(), size -> size)), Component.translatable("gui.roll_mod.skin_roll.title"));
    }

    /** Called from the battle-pass data packet when it carries a roll. */
    public static void open(CompoundTag tag) {
        List<ItemSkinDefinition> strip = new ArrayList<>();
        ListTag list = tag.getList("strip", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
            ItemSkinDefinition skin = id == null ? null : ItemSkinRegistry.get(id);
            strip.add(skin);
        }
        int winner = tag.getInt("winner");
        if (winner < 0 || winner >= strip.size() || strip.get(winner) == null) return;
        float jitter = tag.getFloat("jitter");

        UIElement root = new UIElement();
        root.layout(l -> l.widthPercent(100).heightPercent(100).alignItems(AlignItems.CENTER)
                .justifyContent(org.appliedenergistics.yoga.YogaJustify.CENTER));

        UIElement panel = new UIElement();
        panel.addClass("panel_bg");
        panel.layout(l -> l.flexDirection(FlexDirection.COLUMN).alignItems(AlignItems.CENTER)
                .width(STRIP_W + 20).paddingAll(10).gapRow(6));

        Label title = new Label();
        title.setText(Component.translatable("gui.roll_mod.skin_roll.title").withStyle(ChatFormatting.GOLD));
        title.layout(l -> l.widthPercent(100).height(12));
        title.textStyle(t -> t.textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.CENTER));

        Label result = new Label();
        result.setText(Component.literal(" "));
        result.layout(l -> l.widthPercent(100).height(12));
        result.textStyle(t -> t.textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.CENTER));

        Button close = new Button();
        close.setText(Component.translatable("gui.roll_mod.skin_roll.close"));
        close.layout(l -> l.width(80).height(16).paddingAll(0));

        Strip spinner = new Strip(strip, winner, jitter, () -> {
            ItemSkinDefinition won = strip.get(winner);
            result.setText(Component.translatable("gui.roll_mod.skin_roll.won", won.displayName())
                    .withStyle(s -> s.withColor(rarity(won) & 0xFFFFFF)));
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 1.2f, 0.6f));
        });
        spinner.layout(l -> l.width(STRIP_W).height(STRIP_H).flexShrink(0));

        panel.addChildren(title, spinner, result, close);
        root.addChild(panel);
        SkinRollScreen screen = new SkinRollScreen(root);
        close.addEventListener(UIEvents.CLICK, e -> screen.onClose());
        Minecraft.getInstance().setScreen(screen);
    }

    private static int rarity(ItemSkinDefinition skin) {
        // The skin's own rarity now, not one picked from its id.
        return 0xFF000000 | com.roll_54.roll_mod.cosmetics.ItemSkinRegistry.rarity(skin).color;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** The strip itself: drawn by hand each frame, so it moves smoothly rather than at the 20 Hz tick. */
    private static final class Strip extends UIElement {
        private final List<ItemSkinDefinition> cards;
        private final float target;
        private final Runnable onStop;
        private long start = -1;
        private int lastCard = -1;
        private boolean stopped;

        Strip(List<ItemSkinDefinition> cards, int winner, float jitter, Runnable onStop) {
            this.cards = cards;
            this.target = (winner + jitter) * STEP;
            this.onStop = onStop;
        }

        @Override
        public void drawBackgroundAdditional(GUIContext context) {
            super.drawBackgroundAdditional(context);
            long now = Util.getMillis();
            if (start < 0) start = now;
            float t = Math.min(1f, (now - start) / (float) SPIN_MS);
            float eased = 1f - (float) Math.pow(1f - t, 4); // fast at first, a long slow crawl at the end
            float offset = target * eased;

            GuiGraphics g = context.graphics;
            int x0 = Math.round(getPositionX());
            int y0 = Math.round(getPositionY());
            int w = Math.round(getSizeWidth());
            int h = Math.round(getSizeHeight());
            int centre = x0 + w / 2;
            g.fill(x0, y0, x0 + w, y0 + h, 0xFF16161C);
            g.enableScissor(x0, y0, x0 + w, y0 + h);
            int shift = Math.round(offset);
            for (int i = 0; i < cards.size(); i++) {
                int left = centre - CARD / 2 + i * STEP - shift;
                if (left + CARD < x0 || left > x0 + w) continue;
                ItemSkinDefinition skin = cards.get(i);
                int top = y0 + 6;
                g.fill(left, top, left + CARD, top + CARD, 0xFF2A2A34);
                int colour = skin == null ? 0xFF808080 : rarity(skin);
                g.fill(left, top + CARD - 4, left + CARD, top + CARD, colour);
                if (skin != null) SkinDollElement.drawSkin(context, left + 8, top + 4, 48, 48, skin);
            }
            g.disableScissor();
            // The marker: a gold line down the middle.
            g.fill(centre - 1, y0, centre + 1, y0 + h, 0xFFFFC040);

            // A tick for every card that passes the marker, like a case opening.
            int card = (int) Math.floor((offset + CARD / 2f) / STEP);
            if (card != lastCard && !stopped) {
                if (lastCard >= 0) Minecraft.getInstance().getSoundManager()
                        .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.6f, 0.25f));
                lastCard = card;
            }
            if (t >= 1f && !stopped) {
                stopped = true;
                onStop.run();
            }
        }
    }
}
