package com.roll_54.roll_mod.economy.plot.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.roll_54.roll_mod.minestar.hub.gui.HubInfo;
import com.roll_54.roll_mod.minestar.hub.gui.HubSection;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

/**
 * The shop dimension's current building theme, shown before a plot purchase goes through, with
 * "accept and buy" and Cancel. Nothing is bought until it is accepted.
 *
 * <p>The text is {@code assets/roll_mod/hub_info/<language>/plot_theme.json}, in the format of the
 * hub's section explanations ({@link HubInfo}): {@code text} / {@code space} / {@code rule} lines with
 * {@code &} colour codes, falling back to {@code en_us}, overridable by a resource pack. With
 * {@code "enabled": false} in it, or no file at all, the purchase goes through without the popup.
 *
 * <p>Built once with the plots tab, like {@code HubConfirm}: {@link #root()} goes on the tab's root,
 * last. The text is re-read on every open, as the info panels' is, so a language change or an
 * {@code F3+T} reload shows on the next purchase.
 */
final class PlotThemePopup {

  private static final String ID = "plot_theme";

  private static final int W = 320;
  private static final int H = 230;
  private static final int ROW_H = 16;
  private static final int GAP = 4;
  private static final int COLOR_PANEL = 0x40000000;

  private HubSection.Floating floating;
  private final ScrollerView text = new ScrollerView();
  private final Button accept = new Button();
  @Nullable private Runnable pending;

  PlotThemePopup() {
    floating =
        HubSection.popup(
                Component.translatable("menu.roll_mod.plot.theme.window"),
                W,
                H,
                body -> {
                  body.layout(l -> l.gapRow(GAP));
                  text.layout(l -> l.widthPercent(100).flexBasis(0).flexGrow(1));
                  text.scrollerStyle(
                      s ->
                          s.mode(ScrollerMode.VERTICAL)
                              .verticalScrollDisplay(ScrollDisplay.AUTO)
                              .horizontalScrollDisplay(ScrollDisplay.NEVER));
                  text.viewContainer(
                      c ->
                          c.layout(
                              l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).paddingAll(4)));
                  text.style(
                      s ->
                          s.background(
                              new ColorRectTexture(COLOR_PANEL)));

                  UIElement buttons = new UIElement();
                  buttons.layout(
                      l ->
                          l.flexDirection(FlexDirection.ROW)
                              .widthPercent(100)
                              .height(ROW_H)
                              .flexShrink(0)
                              .gapColumn(GAP));
                  accept.setText(
                      Component.translatable("menu.roll_mod.plot.theme.accept")
                          .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
                  accept.layout(l -> l.flexBasis(0).flexGrow(1).heightPercent(100));
                  accept.setOnClick(
                      e -> {
                        Runnable action = pending;
                        pending = null;
                        close();
                        if (action != null) action.run();
                      });
                  Button cancel = new Button();
                  cancel.setText(Component.translatable("gui.roll_mod.hub.confirm.cancel"));
                  cancel.layout(l -> l.flexBasis(0).flexGrow(1).heightPercent(100));
                  cancel.setOnClick(
                      e -> {
                        pending = null;
                        close();
                      });
                  buttons.addChildren(accept, cancel);
                  body.addChildren(text, buttons);
                })
            .windowed()
            .noCloseButton();
  }

  private void close() {
    floating.close();
  }

  /** Add this to the tab's root, last. */
  UIElement root() {
    return floating.root();
  }

  /** Shows the theme; {@code buy} runs once it is accepted, or at once when there is no theme to show. */
  void ask(Runnable buy) {
    if (!HubInfo.has(ID)) {
      buy.run();
      return;
    }
    pending = buy;
    text.clearAllScrollViewChildren();
    for (UIElement line : HubInfo.read(ID)) {
      text.addScrollViewChild(line);
    }
    floating.open();
  }
}
