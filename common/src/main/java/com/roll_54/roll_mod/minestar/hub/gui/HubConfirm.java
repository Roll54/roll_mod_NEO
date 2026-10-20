package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

/**
 * "Are you sure?" for the actions that cannot be taken back lightly — a ban, the whitelist switch —
 * as a window of its own with the hub's exclamation mark in its title.
 *
 * <p>Built once with its tab, like every hub popup: {@link #root()} goes on the tab's root, last.
 * {@link #ask} points it at one action and opens it; nothing is sent until Confirm is pressed, and
 * closing it any other way (Cancel, Close, another window opening, a tab change) drops the action.
 */
public final class HubConfirm {

    private static final int W = 270;
    private static final int H = 140;
    private static final int ROW_H = 16;

    private HubSection.Floating floating;
    private final Label message = new Label();
    private final Button confirm = new Button();
    @Nullable private Runnable pending;

    public HubConfirm() {
        floating = HubSection.popup(Component.translatable("gui.roll_mod.hub.confirm.title"),
                SpriteTexture.of(HubBadge.TEXTURE), W, H, body -> {
                    message.layout(l -> l.widthPercent(100).flexBasis(0).flexGrow(1));
                    message.textStyle(t -> t.textWrap(TextWrap.WRAP));

                    UIElement buttons = new UIElement();
                    buttons.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(ROW_H)
                            .flexShrink(0).gapColumn(4));
                    confirm.layout(l -> l.flexBasis(0).flexGrow(1).heightPercent(100));
                    confirm.setOnClick(e -> {
                        Runnable action = pending;
                        pending = null;
                        close();
                        if (action != null) action.run();
                    });
                    Button cancel = new Button();
                    cancel.setText(Component.translatable("gui.roll_mod.hub.confirm.cancel"));
                    cancel.layout(l -> l.flexBasis(0).flexGrow(1).heightPercent(100));
                    cancel.setOnClick(e -> {
                        pending = null;
                        close();
                    });
                    buttons.addChildren(confirm, cancel);
                    body.addChildren(message, buttons);
                }).windowed().noCloseButton();
    }

    private void close() {
        floating.close();
    }

    /** Add this to the tab's root, last. */
    public UIElement root() {
        return floating.root();
    }

    /** Asks about {@code what}; {@code action} runs only if Confirm is pressed. */
    public void ask(Component what, Component confirmText, Runnable action) {
        pending = action;
        message.setText(what);
        confirm.setText(confirmText.copy().withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        floating.open();
    }
}
