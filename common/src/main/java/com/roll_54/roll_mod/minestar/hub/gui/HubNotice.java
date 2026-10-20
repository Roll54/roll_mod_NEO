package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.network.chat.Component;

/**
 * A message and an OK: what the server would have said in chat, shown where the player is looking.
 * The hub covers the chat, so a refusal sent there goes unread while the hub is open.
 *
 * <p>Built once with its tab, like {@link HubConfirm}: {@link #root()} goes on the tab's root, last.
 */
public final class HubNotice {

    private static final int W = 240;
    private static final int H = 110;
    private static final int ROW_H = 16;

    private HubSection.Floating floating;
    private final Label message = new Label();

    public HubNotice(Component title) {
        floating = HubSection.popup(title, SpriteTexture.of(HubBadge.TEXTURE), W, H, body -> {
            message.layout(l -> l.widthPercent(100).flexBasis(0).flexGrow(1));
            message.textStyle(t -> t.textWrap(TextWrap.WRAP));

            UIElement buttons = new UIElement();
            buttons.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(ROW_H).flexShrink(0));
            Button ok = new Button();
            ok.setText(Component.translatable("gui.roll_mod.hub.notice.ok"));
            ok.layout(l -> l.flexBasis(0).flexGrow(1).heightPercent(100));
            ok.setOnClick(e -> floating.close());
            buttons.addChild(ok);
            body.addChildren(message, buttons);
        }).windowed().noCloseButton();
    }

    /** Add this to the tab's root, last. */
    public UIElement root() {
        return floating.root();
    }

    public void show(Component what) {
        message.setText(what);
        floating.open();
    }
}
