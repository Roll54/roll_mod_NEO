package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.ui.elements.TextArea;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.roll_54.roll_mod.util.LegacyText;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * A {@link TextArea} that colours {@code &} codes as they are typed.
 *
 * <p>The stock one draws each line through {@code Component.literal}, so {@code &c} is just two
 * characters and the only way to see colour would be typing section signs — which the chat-character
 * filter refuses, and which {@link LegacyText} throws away anyway. This draws each line through
 * {@link LegacyText#editorPreview} instead; everything else — editing, the cursor, selection,
 * scrolling — is the stock behaviour, and the text stored is exactly what was typed.
 *
 * <p>The loop mirrors {@code TextArea.drawLines} one for one; only the string handed to the font
 * differs.
 */
class FormattedTextArea extends TextArea {

    @Override
    protected void drawLines(GUIContext context, Font font, ResourceLocation fontId, float scale,
                             float x, float y, int firstLine, int lastLine) {
        var style = getTextAreaStyle();
        int color = isError() ? style.errorColor() : style.textColor();
        for (int i = firstLine; i <= lastLine && i < lines.size(); i++) {
            float lineY = y + i * lineHeight() - getScrollY();
            float lineX = x - getScrollX();
            Component text = Component.literal(LegacyText.editorPreview(lines.get(i)))
                    .withStyle(s -> s.withFont(fontId));
            context.pose.pushPose();
            context.pose.translate(lineX, lineY, 0);
            context.pose.scale(scale, scale, 1);
            context.graphics.drawString(font, text, 0, 0, color, style.textShadow());
            context.pose.popPose();
        }
        // As the stock one does: an empty editor shows its placeholder.
        if (lines.size() == 1 && lines.getFirst().isEmpty()) {
            drawPlaceHolder(context, font, scale, x, y);
        }
    }
}
