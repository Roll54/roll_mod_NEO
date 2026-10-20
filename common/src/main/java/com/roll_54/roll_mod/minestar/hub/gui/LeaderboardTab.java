package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.leaderboard.ClientLeaderboardCache;
import com.roll_54.roll_mod.minestar.leaderboard.LeaderboardCategory;
import com.roll_54.roll_mod.minestar.leaderboard.LeaderboardRow;
import com.roll_54.roll_mod.network.packet.LeaderboardPackets;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The hub's leaderboards: a drop-down of boards — richest, quests completed, playtime, top earners of
 * the week, daily tasks done — over the top 50 of the one picked, with the viewer's own line pinned
 * underneath.
 *
 * <p>Every board is seasonal and built on the server ({@code LeaderboardService}); this tab only asks
 * for one and draws what comes back through {@link ClientLeaderboardCache}. It asks once when the hub
 * opens, again when the drop-down changes, and on ⟳ — the server caches each board for two minutes,
 * so asking costs it little.
 */
public final class LeaderboardTab {

    private static final int ROW_H = 20;
    private static final int HEAD = 16;
    private static final int RANK_W = 16;
    private static final int REFRESH_W = 16;

    private static final int COLOR_ROW = 0x30FFFFFF;
    private static final int COLOR_ROW_HOVER = 0x50FFFFFF;
    private static final int COLOR_OWN = 0x50FFD75E;
    private static final int COLOR_GOLD = 0xFFFFD700;
    private static final int COLOR_SILVER = 0xFFC0C0C0;
    private static final int COLOR_BRONZE = 0xFFCD7F32;

    private static final int PLACE = 16;
    private static final int PLACE_GAP = 2;
    private static final int ROW_GAP = 6;
    private static final IGuiTexture PLACE_1 = placeIcon("1st");
    private static final IGuiTexture PLACE_2 = placeIcon("2nd");
    private static final IGuiTexture PLACE_3 = placeIcon("3rd");
    private static final IGuiTexture PLACE_N = placeIcon("n");

    private LeaderboardTab() {}

    public static UIElement build(Player player) {
        UIElement root = new UIElement();
        root.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).heightPercent(100)
                .paddingAll(HubSection.TAB_PADDING));

        HubSection.Info section = HubSection.info("leaderboards");
        LeaderboardCategory[] category = {LeaderboardCategory.RICHEST};

        UIElement controls = new UIElement();
        controls.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(16).flexShrink(0)
                .gapColumn(2).marginBottom(3));

        Selector<LeaderboardCategory> selector = new Selector<>();
        // Null-safe: setCandidateUIProvider re-renders the current value, and there is none yet.
        selector.setCandidateUIProvider(UIElementProvider.text(c -> c == null ? Component.empty() : c.title()));
        selector.setCandidates(List.of(LeaderboardCategory.values()));
        selector.setSelected(LeaderboardCategory.RICHEST, false);
        selector.layout(l -> l.flexGrow(1).height(16));
        selector.setOnValueChanged(c -> {
            if (c == null) return;
            category[0] = c;
            PacketDistributor.sendToServer(new LeaderboardPackets.Request(c));
        });

        Button refresh = new Button();
        refresh.setText(Component.literal("⟳"));
        refresh.layout(l -> l.width(REFRESH_W).height(16).paddingAll(0));
        refresh.setOnClick(e -> PacketDistributor.sendToServer(new LeaderboardPackets.Request(category[0])));
        controls.addChildren(selector, refresh);

        ScrollerView list = new ScrollerView();
        list.layout(l -> l.widthPercent(100).flexBasis(0).flexGrow(1));
        list.scrollerStyle(s -> s.mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.AUTO)
                .horizontalScrollDisplay(ScrollDisplay.NEVER));
        list.viewContainer(c -> c.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100)));
        list.viewPort(v -> v.layout(l -> l.paddingAll(0)));

        // The viewer's own line, pinned under the list so it is visible however far down they are.
        UIElement own = new UIElement();
        own.layout(l -> l.widthPercent(100).height(ROW_H).flexShrink(0).marginTop(3));

        root.addChildren(HubSection.header("leaderboards", section), controls, list, own, section.overlay());

        int[] shown = {-1};
        boolean[] asked = {false};
        root.addEventListener(UIEvents.TICK, e -> {
            if (!player.level().isClientSide()) return;
            // Asked on the first client tick rather than while the tree is built: the screen is up
            // by then, and the server copy of this tree never asks at all.
            if (!asked[0]) {
                asked[0] = true;
                PacketDistributor.sendToServer(new LeaderboardPackets.Request(category[0]));
            }
            int version = ClientLeaderboardCache.VERSION;
            if (version == shown[0]) return;
            shown[0] = version;

            LeaderboardCategory board = ClientLeaderboardCache.CATEGORY;
            list.clearAllScrollViewChildren();
            list.clearLayoutCache();
            own.clearAllChildren();
            if (!ClientLeaderboardCache.LOADED) {
                list.addScrollViewChild(note("gui.roll_mod.hub.leaderboards.loading"));
                return;
            }
            if (ClientLeaderboardCache.FAILED) {
                list.addScrollViewChild(note("gui.roll_mod.hub.leaderboards.failed"));
                return;
            }
            List<LeaderboardRow> rows = ClientLeaderboardCache.ROWS;
            if (rows.isEmpty()) list.addScrollViewChild(note("gui.roll_mod.hub.leaderboards.empty"));
            LeaderboardRow mine = ClientLeaderboardCache.OWN;
            for (LeaderboardRow row : rows) {
                boolean isMine = mine != null && mine.id().equals(row.id()) && mine.rank() == row.rank();
                list.addScrollViewChild(row(board, row, isMine));
            }
            own.addChild(ownLine(board, mine));
        });

        return root;
    }

    private static UIElement row(LeaderboardCategory board, LeaderboardRow row, boolean highlight) {
        UIElement element = new UIElement();
        element.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(ROW_H)
                .marginBottom(2).paddingAll(2).paddingHorizontal(HubSection.ROW_INSET).gapColumn(ROW_GAP));
        element.style(s -> s.background(new ColorRectTexture(highlight ? COLOR_OWN : COLOR_ROW)));

        Label rank = new Label();
        rank.setText(Component.literal(row.rank() > 0 ? "#" + row.rank() : "—"));
        int rankColor = switch (row.rank()) {
            case 1 -> COLOR_GOLD;
            case 2 -> COLOR_SILVER;
            case 3 -> COLOR_BRONZE;
            default -> 0xFFE0E0E0;
        };
        // At least RANK_W (fits "#1".."#9"), and as wide as its text beyond that.
        rank.layout(l -> l.minWidth(RANK_W).heightPercent(100).flexShrink(0));
        // Right-aligned so the number sits against its place icon, whatever its width — left-aligned in
        // a box sized for "#50", "#1" left a gap before the medal.
        rank.textStyle(t -> t.adaptiveWidth(true).textColor(rankColor).textAlignHorizontal(Horizontal.RIGHT)
                .textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));
        element.addChild(rank);

        // The place icon, left of the head. Team rows have no head but still get it. An unranked
        // row ("—") gets an empty box of the same size, so its name lines up with the others.
        UIElement place = new UIElement();
        // Pulled in against the number: the row's gap is sized for separating columns, and the
        // number and its icon read as one.
        place.layout(l -> l.width(PLACE).height(PLACE).flexShrink(0).marginLeft(PLACE_GAP - ROW_GAP));
        IGuiTexture placeTexture = placeTexture(row.rank());
        if (placeTexture != null) place.style(s -> s.background(placeTexture));
        element.addChild(place);

        // Teams have no face; the quests board rows are teams.
        if (!board.teams()) {
            PlayerHeadElement head = new PlayerHeadElement(row.id(), row.name());
            head.layout(l -> l.width(HEAD).height(HEAD).flexShrink(0));
            element.addChild(head);
        }

        Label name = new Label();
        name.setText(Component.literal(row.name()));
        name.layout(l -> l.flexBasis(0).flexGrow(1).heightPercent(100));
        name.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));

        Label value = new Label();
        value.setText(board.format(row.value()).copy().withStyle(ChatFormatting.YELLOW));
        // As wide as its own text, measured in the theme's font: "3 400", "12 h 5 m" or "7 завдань",
        // whatever the language, and the name takes everything left. A fixed column had to be sized
        // for the longest of them and left most rows a wide empty gap.
        value.layout(l -> l.heightPercent(100).flexShrink(0));
        value.textStyle(t -> t.adaptiveWidth(true).textAlignHorizontal(Horizontal.RIGHT)
                .textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));

        element.addChildren(name, value);

        // A player row opens that player in the interactions tab. Not team rows (no player behind
        // them) and not the viewer's own, which has nothing to do with themself there.
        if (!board.teams() && !highlight) {
            element.addEventListener(UIEvents.MOUSE_ENTER, e ->
                    element.style(s -> s.background(new ColorRectTexture(COLOR_ROW_HOVER))));
            element.addEventListener(UIEvents.MOUSE_LEAVE, e ->
                    element.style(s -> s.background(new ColorRectTexture(COLOR_ROW))));
            element.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(List.of(
                    Component.literal(row.name()),
                    Component.translatable("gui.roll_mod.hub.leaderboards.view_profile")
                            .withStyle(ChatFormatting.GRAY)), null, null, ItemStack.EMPTY));
            element.addEventListener(UIEvents.CLICK, e -> {
                PlayerInteractionsTab.focus(row.id(), row.name());
                HubUI.switchTo("interactions");
            });
        }
        return element;
    }

    /** "You: #12 · 3 400", or that the viewer is not on this board. */
    private static UIElement ownLine(LeaderboardCategory board, @Nullable LeaderboardRow mine) {
        if (mine == null || mine.value() <= 0) {
            return note(board.teams() ? "gui.roll_mod.hub.leaderboards.own.noteam"
                    : "gui.roll_mod.hub.leaderboards.own.none");
        }
        LeaderboardRow labelled = new LeaderboardRow(mine.rank(), mine.id(),
                Component.translatable("gui.roll_mod.hub.leaderboards.own").getString()
                        + (board.teams() ? " (" + mine.name() + ")" : ""), mine.value());
        return row(board, labelled, true);
    }

    @Nullable
    private static IGuiTexture placeTexture(int rank) {
        return switch (rank) {
            case 1 -> PLACE_1;
            case 2 -> PLACE_2;
            case 3 -> PLACE_3;
            default -> rank > 0 ? PLACE_N : null;
        };
    }

    private static IGuiTexture placeIcon(String place) {
        return SpriteTexture.of(RollMod.id("textures/gui/hub/leaderboard/" + place + "_place_in_leaderboard.png"));
    }

    private static Label note(String key) {
        Label label = new Label();
        label.setText(Component.translatable(key).withStyle(ChatFormatting.GRAY));
        label.layout(l -> l.widthPercent(100).height(ROW_H));
        label.textStyle(t -> t.textAlignVertical(Vertical.CENTER));
        return label;
    }
}
