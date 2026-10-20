package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.roll_54.roll_mod.RollMod;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.AlignContent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.appliedenergistics.yoga.YogaPositionType;


/**
 * The hub's settings, opened from the gear button among the window controls: the theme, the
 * experimental "theme everywhere" switch, and the chat buttons. The contents are built by
 * {@link SettingsTab}; this is the popup around them.
 *
 * <p>A modal over the whole window: a dimmed backdrop that closes it when clicked, with the panel in
 * its middle. Built on both sides like everything in the hub's tree, and hidden with
 * {@code setDisplay}, which is safe here — nothing in it syncs or ticks. The preference is only read
 * on the client copy; the server's copy shows the default and is never drawn.
 */
final class HubSettings {

    private static final int PANEL_W = 340;
    private static final int PANEL_H = 260;
    /** Keeps the title, the settings and the close button off the panel's edges. */
    private static final int PADDING = 10;
    private static final int ROW_H = 16;
    private static final int LABEL_W = 70;
    private static final int CLOSE_W = 70;
    private static final int CLOSE_H = 16;

    /** Over the window controls too, so the backdrop really is modal. */
    private static final int Z = 20;
    private static final int BACKDROP = 0x80000000;

    private final UIElement root;

    HubSettings(Player player) {
        root = new UIElement();
        root.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).left(0).top(0).right(0).bottom(0)
                .justifyContent(AlignContent.CENTER).alignItems(AlignItems.CENTER));
        root.style(s -> s.background(new ColorRectTexture(BACKDROP))
                .zIndex(Z));
        root.setDisplay(false);
        // A click on the backdrop itself closes; one on the panel stops at the panel below.
        root.addEventListener(UIEvents.CLICK, e -> {
            if (e.target == root) hide();
        });

        UIElement panel = new UIElement();
        panel.addClass("panel_bg");
        // Big enough for the chat-button editor, never bigger than the hub window it sits in.
        panel.layout(l -> l.width(PANEL_W).maxWidthPercent(95).height(PANEL_H).maxHeightPercent(92)
                .flexDirection(FlexDirection.COLUMN).paddingAll(PADDING).gapRow(6));
        panel.addEventListener(UIEvents.CLICK, e -> e.stopPropagation());

        Label title = new Label();
        title.setText(Component.translatable("gui." + RollMod.MODID + ".hub.settings.title"));
        title.layout(l -> l.widthPercent(100).height(12).paddingLeft(2).flexShrink(0));
        title.textStyle(t -> t.textShadow(true).textWrap(TextWrap.HIDE).textAlignVertical(Vertical.CENTER));

        Button close = new Button();
        close.setText(Component.translatable("gui." + RollMod.MODID + ".hub.settings.close"));
        close.layout(l -> l.width(CLOSE_W).height(CLOSE_H).marginLeftAuto().flexShrink(0));
        close.setOnClick(e -> hide());

        // Theme, the force switch and the chat buttons — see SettingsTab. Its drop-downs open above
        // this modal's backdrop.
        panel.addChildren(title, SettingsTab.content(player, Z + 1), close);
        root.addChild(panel);
    }

    UIElement root() {
        return root;
    }

    void toggle() {
        root.setDisplay(!root.isDisplayed());
    }

    void hide() {
        root.setDisplay(false);
    }
}
