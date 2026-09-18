package com.roll_54.roll_mod.minestar.dailytasks.api;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.texture.TransformTexture;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib2.utils.ColorUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * An item drawn with a caption where the stack count normally goes — {@code "1-4"} for a payout
 * that is rolled rather than fixed.
 *
 * <p>{@link ItemStackTexture} can only show the count baked into its stack, and a range is not a
 * count: the reward strip has to say what the payout <em>can</em> be, before anything is rolled.
 * Vanilla's own decoration text would do it — {@code renderItemDecorations} takes an override
 * string — but it draws right-aligned at full size, and a four-character range is wider than the
 * 16px cell it has to sit in, so the caption is scaled down to fit instead.
 *
 * <p>Built on both dists, like every other {@link DailyTaskIcon} texture: only the drawing is
 * client-side.
 */
public final class ItemLabelTexture extends TransformTexture {

    /** Item rendering is defined in a 16×16 box; the draw below scales the cell into one. */
    private static final int CELL = 16;

    /** Below this the caption stops being readable, so a very long one overflows rather than blur. */
    private static final float MIN_SCALE = 0.5f;

    private final ItemStack stack;
    private final String label;
    private int color = -1;

    public ItemLabelTexture(ItemStack stack, String label) {
        this.stack = stack;
        this.label = label;
    }

    @Override
    public ItemLabelTexture setColor(int color) {
        this.color = color;
        return this;
    }

    @Override
    public IGuiTexture copy() {
        ItemLabelTexture copied = new ItemLabelTexture(stack, label);
        copied.color = color;
        copied.copyTransform(this);
        return copied;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    protected void drawInternal(GuiGraphics graphics, float mouseX, float mouseY,
                                float x, float y, float width, float height, float partialTicks) {
        draw(graphics, x, y, width, height, color);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    protected void drawInternal(GUIContext context, float x, float y, float width, float height) {
        int effective = context.elementColor == -1 ? color : ColorUtils.mulColor(color, context.elementColor);
        draw(context.graphics, x, y, width, height, effective);
    }

    /** The same scale-the-cell-into-a-16px-box dance {@link ItemStackTexture} does. */
    @OnlyIn(Dist.CLIENT)
    private void draw(GuiGraphics graphics, float x, float y, float width, float height, int drawColor) {
        if (stack.isEmpty()) return;
        graphics.flush();
        graphics.pose().pushPose();
        graphics.pose().scale(width / CELL, height / CELL, 1);
        graphics.pose().translate(x * CELL / width, y * CELL / height, -200);
        // No decoration text of its own: the count is meaningless here and the caption replaces it.
        DrawerHelper.drawItemStack(graphics, stack, 0, 0, drawColor, null);
        drawLabel(graphics);
        graphics.pose().popPose();
    }

    @OnlyIn(Dist.CLIENT)
    private void drawLabel(GuiGraphics graphics) {
        Font font = Minecraft.getInstance().font;
        int width = font.width(label);
        float scale = width <= CELL ? 1f : Math.max(MIN_SCALE, (float) CELL / width);

        graphics.pose().pushPose();
        // Over the item, which DrawerHelper draws at z 232.
        graphics.pose().translate(0f, 0f, 400f);
        graphics.pose().scale(scale, scale, 1f);
        // Bottom-right of the cell — where the stack count sits — worked back into the space the
        // scale above leaves this pose drawing in. The two extra pixels of drop are vanilla's: it
        // lets the count hang one pixel below the slot.
        graphics.drawString(font, label,
                Math.round(CELL / scale) - width,
                Math.round((CELL + 2) / scale) - font.lineHeight,
                0xFFFFFF, true);
        graphics.pose().popPose();
    }
}
