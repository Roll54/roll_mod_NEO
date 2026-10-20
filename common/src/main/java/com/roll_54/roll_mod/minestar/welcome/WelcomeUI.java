package com.roll_54.roll_mod.minestar.welcome;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Toggle;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.theme.RollUi;
import com.roll_54.roll_mod.network.packet.WelcomeResponsePacket;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * The starting screen: three things every new player has to know, each on its own tile with a
 * checkmark, and an "I read all of them and agree" checkmark under them. Accept only unlocks once all
 * four are ticked; Decline, Esc or any other close kicks — see {@link WelcomeService}.
 *
 * <p>Covers the whole screen, the way the hub's host does: the size provider hands back the screen
 * itself. Drawn in the player's theme ({@link RollUi}) — the root is the theme's panel surface and
 * each rule's tile is its {@code preview_bg} well, so both follow whatever is picked in the hub.
 *
 * <p>The key names in the text are {@link Component#keybind} components, so each player sees the key
 * they actually have bound to FTB Quests and to JEI's recipe / usage lookups, not a default.
 *
 * <p>Built on both sides like every LdLib2 UI here, but nothing syncs: the ticks are client state,
 * and the answer travels as a {@link WelcomeResponsePacket}.
 */
public final class WelcomeUI {

    public static final ResourceLocation UI_ID = RollMod.id("welcome");
    private static final String ROOT_ID = "roll_mod_welcome";

    /** The content column; the backdrop around it fills whatever is left of the screen. */
    private static final int COLUMN_MAX_W = 520;
    private static final int SCREEN_PADDING = 16;
    private static final int TILE_PADDING = 8;
    private static final int TILE_GAP = 6;
    private static final int CHECK = 14;
    private static final int BUTTON_W = 100;
    private static final int BUTTON_H = 18;

    /** The heading and every rule's title: #B7043B. */
    private static final int TEXT_TITLE = 0xFFB7043B;

    private WelcomeUI() {}

    public static ModularUI createUI(Player player) {
        UIElement root = new UIElement();
        root.setId(ROOT_ID);
        root.addClass("panel_bg");
        root.layout(l -> l.flexDirection(FlexDirection.COLUMN).alignItems(AlignItems.CENTER)
                .paddingAll(SCREEN_PADDING));

        UIElement column = new UIElement();
        column.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).maxWidth(COLUMN_MAX_W)
                .heightPercent(100).gapRow(8));
        root.addChild(column);

        // The colour rides on the text itself as well as the label: a theme's `label { text-color }` rule
        // can outrank the label's own colour, but never a colour carried by the component.
        Label title = label(Component.translatable("gui.roll_mod.welcome.title")
                .withStyle(ChatFormatting.BOLD).withColor(TEXT_TITLE & 0xFFFFFF));
        title.textStyle(t -> t.textColor(TEXT_TITLE).textAlignHorizontal(Horizontal.CENTER).fontSize(14));
        Label subtitle = label(Component.translatable("gui.roll_mod.welcome.subtitle")
                .withStyle(ChatFormatting.GRAY));
        subtitle.textStyle(t -> t.textAlignHorizontal(Horizontal.CENTER));
        column.addChildren(title, subtitle);

        // The rules take whatever height is left, and scroll on a screen too short for all three.
        // flexBasis(0) for the reason DailyTasksUI gives: an auto basis would size the scroller to
        // its content and it would never overflow.
        ScrollerView rules = new ScrollerView();
        rules.layout(l -> l.widthPercent(100).flexBasis(0).flexGrow(1));
        rules.scrollerStyle(s -> s.mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.AUTO)
                .horizontalScrollDisplay(ScrollDisplay.NEVER));
        rules.viewPort(v -> v.layout(l -> l.paddingAll(0)));
        rules.viewContainer(c -> c.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100)
                .gapRow(TILE_GAP)));
        column.addChild(rules);

        List<Toggle> checks = new ArrayList<>();
        Component questsKey = key("key.ftbquests.quests");
        Component recipeKey = key("key.jei.showRecipe");
        Component usesKey = key("key.jei.showUses");
        checks.add(point(rules, "quests", questsKey));
        checks.add(point(rules, "recipes", recipeKey, usesKey));
        checks.add(point(rules, "ores"));

        Toggle agree = new Toggle();
        agree.setText(Component.translatable("gui.roll_mod.welcome.agree"));
        agree.layout(l -> l.widthPercent(100).height(CHECK).flexShrink(0));
        column.addChild(agree);
        checks.add(agree);

        UIElement buttons = new UIElement();
        buttons.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(BUTTON_H)
                .flexShrink(0).gapColumn(10).alignItems(AlignItems.CENTER));
        UIElement leftPad = new UIElement();
        leftPad.layout(l -> l.flexGrow(1));
        UIElement rightPad = new UIElement();
        rightPad.layout(l -> l.flexGrow(1));

        Button accept = new Button();
        accept.setText(Component.translatable("gui.roll_mod.welcome.accept"));
        accept.layout(l -> l.width(BUTTON_W).height(BUTTON_H));
        accept.setActive(false);
        accept.setOnClick(e -> {
            // Checked again here as well as through setActive, so a disabled button can never send.
            if (allOn(checks)) PacketDistributor.sendToServer(new WelcomeResponsePacket(true));
        });

        Button decline = new Button();
        decline.setText(Component.translatable("gui.roll_mod.welcome.decline").withStyle(ChatFormatting.RED));
        decline.layout(l -> l.width(BUTTON_W).height(BUTTON_H));
        decline.setOnClick(e -> PacketDistributor.sendToServer(new WelcomeResponsePacket(false)));

        buttons.addChildren(leftPad, accept, decline, rightPad);
        column.addChild(buttons);

        for (Toggle check : checks) {
            check.setOnToggleChanged(on -> accept.setActive(allOn(checks)));
        }

        // The screen itself as the root's size, so the backdrop covers all of it. Re-run on every
        // resize and GUI-scale change; the column re-centres and the rules re-wrap on their own.
        return ModularUI.of(RollUi.of(player, root, screen -> screen), player);
    }

    /** Whether {@code ui} is the starting screen; identified by the id stamped on its root. */
    public static boolean isWelcome(@Nullable ModularUI ui) {
        return ui != null && ROOT_ID.equals(ui.ui.getRootElement().getId());
    }

    /** One rule, on its own tile: a short title and its text on the left, its checkmark on the right. */
    private static Toggle point(ScrollerView rules, String name, Object... args) {
        UIElement tile = new UIElement();
        // Purely visual: the theme's well surface, so the tile follows the theme.
        tile.addClass("preview_bg");
        tile.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).alignItems(AlignItems.CENTER)
                .paddingAll(TILE_PADDING).gapColumn(8));

        UIElement text = new UIElement();
        text.layout(l -> l.flexDirection(FlexDirection.COLUMN).flexGrow(1).flexBasis(0).gapRow(3));
        Label title = label(Component.translatable("gui.roll_mod.welcome." + name + ".title")
                .withStyle(ChatFormatting.BOLD).withColor(TEXT_TITLE & 0xFFFFFF));
        title.textStyle(t -> t.textColor(TEXT_TITLE));
        text.addChildren(title, label(Component.translatable("gui.roll_mod.welcome." + name + ".body", args)));

        Toggle check = new Toggle();
        check.noText();
        check.layout(l -> l.width(CHECK).height(CHECK).flexShrink(0));

        tile.addChildren(text, check);
        rules.addScrollViewChild(tile);
        return check;
    }

    private static Component key(String mapping) {
        return Component.literal("[").append(Component.keybind(mapping)).append("]")
                .withStyle(ChatFormatting.YELLOW);
    }

    /** A wrapping label in the theme's own text colour. */
    private static Label label(Component text) {
        Label label = new Label();
        label.setText(text);
        label.layout(l -> l.widthPercent(100));
        label.textStyle(t -> t.textWrap(TextWrap.WRAP).adaptiveHeight(true));
        return label;
    }

    private static boolean allOn(List<Toggle> checks) {
        for (Toggle check : checks) {
            if (!check.isOn()) return false;
        }
        return true;
    }
}
