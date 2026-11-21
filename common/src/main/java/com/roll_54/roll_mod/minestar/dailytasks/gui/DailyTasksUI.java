package com.roll_54.roll_mod.minestar.dailytasks.gui;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SimpleBinding;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.dailytasks.DailyRewardRegistry;
import com.roll_54.roll_mod.minestar.dailytasks.DailyTaskManager;
import com.roll_54.roll_mod.minestar.dailytasks.DailyTaskRegistry;
import com.roll_54.roll_mod.minestar.dailytasks.DailyTasksState;
import com.roll_54.roll_mod.minestar.dailytasks.TaskRewardPool;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyReward;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTask;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskIcon;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTaskQuota;
import com.roll_54.roll_mod.minestar.hub.gui.HubBadge;
import com.roll_54.roll_mod.minestar.hub.gui.HubSection;
import com.roll_54.roll_mod.minestar.hub.gui.HubSplit;
import com.roll_54.roll_mod.minestar.dailytasks.api.TaskReward;
import com.roll_54.roll_mod.network.packet.ClaimDailyBonusPacket;
import com.roll_54.roll_mod.network.packet.ClaimDailyTaskPacket;
import com.roll_54.roll_mod.network.packet.RerollDailyTaskPacket;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.appliedenergistics.yoga.YogaPositionType;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The daily-tasks screen, opened with the daily-tasks key (F4 by default).
 *
 * <p>Registered with LdLib2's {@code PlayerUIMenuType}, so it needs no block or item to hang off.
 *
 * <p>{@code createUI} runs on <em>both</em> sides — the server builds the same element tree and
 * uses it to feed the server→client bindings. That is also why the list always holds
 * {@link DailyTasksState#MAX_TASK_COUNT} rows rather than the player's actual quota: the tree has
 * to be identical on both sides for the positional bindings to line up, so a slot past the quota
 * syncs "no task" and the row collapses to nothing. Only six integers per row cross the wire: which
 * task occupies the slot (its index in {@link DailyTaskRegistry#all()}), progress, requirement,
 * claim state and the two rewards it pays (their indices in {@link TaskRewardPool#all()}); the
 * bonus panel down the right adds four more. Everything a player actually sees — name, tooltip,
 * icon — the client looks up in the registry itself, which is built identically on both sides. That
 * is why the registry sorts by id: an index has to mean the same task everywhere.
 */
public final class DailyTasksUI {

    public static final ResourceLocation UI_ID = RollMod.id("daily_tasks");


    /**
     * Tall enough for the row's three stacked lines — name, bar, then the reward strip and Claim
     * side by side — with a little air between them. See {@link #row}.
     */
    private static final int ROW_H = 50;
    private static final int ROW_GAP = 3;
    private static final int ICON = 32;
    private static final int CLAIM_W = 70;
    /**
     * The reroll button in the row's bottom-right corner: its icon, then the price. The caption is
     * sized for the longest translation of "free" — uk_ua's «Безкоштовно» — not for the price.
     */
    private static final int REROLL_ICON = 10;
    private static final int REROLL_TEXT_W = 64;
    private static final ResourceLocation REROLL_TEXTURE = RollMod.id("textures/gui/hub/daily/reroll.png");
    /** The pixel-art reroll mark for AE2 and Ore, drawn at its own 15x14. */
    private static final ResourceLocation REROLL_SMALL_TEXTURE = RollMod.id("textures/gui/hub/daily/small_reroll.png");
    /** The gap between the stacked pieces of a row's middle column. */
    private static final int ROW_INNER_GAP = 4;

    /** The all-complete bonus panel down the right, and the gap between it and the task column. */
    private static final int BONUS_W = 132;
    private static final int BONUS_GAP = 4;
    /** How far the bonus panel and the task rows can be squeezed by the divider between them. */
    private static final int BONUS_MIN_W = 100;
    private static final int TASKS_MIN_W = 220;

    /** The bonus panel: its own reward icon, then the bottom strip's button. */
    private static final int BONUS_ICON = 18;
    private static final int COLLECT_W = 70;

    /** One cell of a reward strip, in the bonus panel and in every task row. */
    private static final int REWARD_ICON = 16;

    /** The reset countdown in the shared title row — see {@link #header}. */
    private static final int RESETS_H = 12;
    private static final int RESETS_GAP = 4;

    private static final int COLOR_ROW = 0x40000000;
    /** The Prime slot's row: purple in every theme (not a black tint, so AE2's fill recolour leaves it alone). */
    private static final int COLOR_ROW_PRIME = 0x806A2C91;
    /** A slot earned from an FTB quest: bluish in every theme. */
    private static final int COLOR_ROW_BONUS = 0x802C6E91;
    private static final int COLOR_BUTTON = 0x80000000;

    /** Claim states, as synced in {@code state}. */
    private static final int LOCKED = 0;
    private static final int CLAIMABLE = 1;
    private static final int CLAIMED = 2;

    private DailyTasksUI() {}

    /** The element tree. Always a tab of the hub, which owns the one ModularUI. */
    public static UIElement buildRoot(Player player) {
        ServerPlayer server = player instanceof ServerPlayer sp ? sp : null;

        // No background: the hub paints the window frame and content panel behind every tab, so a
        // frame here drew a second window inset inside the first. The padding stays as inset spacing.
        UIElement root = new UIElement();
        // Fills the hub's tab box, whatever size the hub window has been given.
        root.layout(l -> l.flexDirection(FlexDirection.COLUMN)
                .widthPercent(100).heightPercent(100).paddingAll(HubSection.TAB_PADDING));

        // This section already had a title row of its own, so it keeps it and only gains the I
        // button on the end.
        HubSection.Info section = HubSection.info("daily_tasks");
        // Daily | Weekly | Seasonal: the last two are the battle pass's tasks, in a list of their own.
        int[] page = {0};
        com.lowdragmc.lowdraglib2.gui.ui.elements.Button[] pageButtons = new com.lowdragmc.lowdraglib2.gui.ui.elements.Button[3];
        String[] pageKeys = {"gui.roll_mod.daily_tasks.page.daily", "gui.roll_mod.daily_tasks.page.weekly",
                "gui.roll_mod.daily_tasks.page.seasonal"};
        for (int i = 0; i < 3; i++) {
            int index = i;
            com.lowdragmc.lowdraglib2.gui.ui.elements.Button b = new com.lowdragmc.lowdraglib2.gui.ui.elements.Button();
            b.setText(Component.translatable(pageKeys[i]));
            b.layout(l -> l.width(58).height(16).paddingAll(0).marginRight(3).marginTop(-2).marginBottom(-2).flexShrink(0));
            com.roll_54.roll_mod.minestar.hub.gui.HubSection.centreCaption(b);
            b.setOnClick(e -> page[0] = index);
            pageButtons[i] = b;
        }
        root.addChild(header(server, section, pageButtons));

        // The rows move into their own column so the bonus panel can sit beside them, and take
        // whatever height the window leaves under the header, so a taller hub shows more rows.
        // flexBasis(0), not flexGrow alone: LdLib defaults flex-shrink to 0 and flex-basis to auto,
        // so an auto basis would be all eight rows and the scroller would never overflow — the list
        // was clipped by the hub's content box with no scrollbar. Same fix as TpaTab's body.
        UIElement body = new UIElement();
        body.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).flexBasis(0).flexGrow(1));

        // ScrollerView clips its viewport and handles the wheel itself, so neither is set here.
        ScrollerView tasks = new ScrollerView();
        // Kept off the list's themed frame, so the first line does not run into it.
        tasks.viewPort(v -> v.layout(l -> l.paddingAll(4)));
        tasks.layout(l -> l.flexBasis(0).flexGrow(1).heightPercent(100));
        tasks.scrollerStyle(s -> s.mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.AUTO)
                .horizontalScrollDisplay(ScrollDisplay.NEVER));
        // The constructor pads the viewport, which would inset the rows from the window edge the
        // plain column never was.
        tasks.viewPort(view -> view.layout(l -> l.paddingAll(4)));
        tasks.viewContainer(container -> container.layout(l -> l.widthPercent(100)));
        for (int i = 0; i < DailyTasksState.MAX_TASK_COUNT; i++) {
            tasks.addScrollViewChild(row(server, i));
        }

        // A divider between the rows and the bonus panel, like the list/info divider of the other
        // tabs — here it is the panel on the right that keeps the width. It carries no sync value, so
        // the panel's bindings keep their places.
        UIElement bonus = bonusPanel(server);
        body.addChildren(tasks, HubSplit.handleRight("daily_tasks", bonus, BONUS_W,
                player.level().isClientSide(), BONUS_MIN_W, TASKS_MIN_W), bonus);
        root.addChild(body);

        // The battle pass's weekly or seasonal tasks. The daily page is squashed to no height rather than hidden
        // while they show: hidden, its rows would stop syncing.
        ScrollerView passTasks = new ScrollerView();
        passTasks.layout(l -> l.widthPercent(100).height(0));
        passTasks.scrollerStyle(s2 -> s2.mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.AUTO).horizontalScrollDisplay(ScrollDisplay.NEVER));
        passTasks.viewPort(v -> v.layout(l -> l.paddingAll(7)));
        passTasks.viewContainer(c -> c.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100)));
        passTasks.setOverflowVisible(false);
        root.addChild(passTasks);
        int[] shownPage = {-1, -1};
        root.addEventListener(UIEvents.TICK, e -> {
            if (!player.level().isClientSide()) return;
            int version = com.roll_54.roll_mod.minestar.battlepass.ClientBattlePassCache.VERSION;
            if (shownPage[0] == page[0] && (page[0] == 0 || shownPage[1] == version)) return;
            boolean changedPage = shownPage[0] != page[0];
            shownPage[0] = page[0];
            shownPage[1] = version;
            if (changedPage) {
                boolean daily = page[0] == 0;
                body.layout(l -> {
                    if (daily) l.flexBasis(0).flexGrow(1).heightAuto();
                    else l.flexGrow(0).flexBasis(0).height(0);
                });
                body.setOverflowVisible(!daily ? false : true);
                passTasks.layout(l -> {
                    if (daily) l.flexGrow(0).flexBasis(0).height(0);
                    else l.flexBasis(0).flexGrow(1).heightAuto();
                });
                for (int i = 0; i < 3; i++) {
                    int idx = i;
                    pageButtons[i].setText(Component.translatable(pageKeys[i]).withStyle(idx == page[0]
                            ? ChatFormatting.YELLOW : ChatFormatting.WHITE));
                }
                // Every page asks: the daily page shows what each daily adds to the battle pass too.
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                        new com.roll_54.roll_mod.network.packet.BattlePassPackets.Act("get", new net.minecraft.nbt.CompoundTag()));
            }
            if (page[0] != 0) {
                com.roll_54.roll_mod.minestar.hub.gui.BattlePassTab.fillTasksFor(passTasks,
                        com.roll_54.roll_mod.minestar.battlepass.ClientBattlePassCache.DATA, page[0] == 2);
            }
        });
        // Last, so it covers the rows when opened.
        root.addChild(section.overlay());

        return root;
    }

    /* ------------------------------------------------- header ------------------------------------------------- */

    /** The shared title row (see {@link HubSection#header}), with the reset countdown beside the I. */
    private static UIElement header(@Nullable ServerPlayer server, HubSection.Info section, UIElement... pages) {
        // Seconds come from the server: the roll follows the server machine's clock, which is not
        // the viewer's. Only the formatting happens client-side.
        SimpleBinding<Integer> secondsLeft = intBinding(
                () -> server == null ? 0 : (int) DailyTaskManager.secondsUntilNextRoll(), 0);

        Label resets = new Label();
        // Right after the title, taking the rest of the row: the title shrinks to its own text below.
        resets.layout(l -> l.flexGrow(1).height(RESETS_H).marginLeft(6).marginRight(RESETS_GAP));
        resets.textStyle(t -> t.textAlignHorizontal(Horizontal.LEFT)
                .textAlignVertical(Vertical.CENTER)
                .textColor(0xFFAAAAAA));
        resets.addSyncValue(secondsLeft.getSyncValue());
        resets.addEventListener(UIEvents.TICK, e -> resets.setText(Component.translatable(
                "gui.roll_mod.daily_tasks.resets_in", formatDuration(value(secondsLeft, 0)))));

        UIElement[] extras = new UIElement[pages.length + 1];
        extras[0] = resets;
        System.arraycopy(pages, 0, extras, 1, pages.length);
        UIElement header = HubSection.header(Component.translatable("gui.roll_mod.daily_tasks.title"), section, extras);
        // The countdown beside the title rather than across the row by the I: the title stops growing.
        if (header.getChildren().get(0) instanceof Label title) {
            title.layout(l -> l.flexGrow(0).flexShrink(0));
            title.textStyle(t -> t.adaptiveWidth(true));
        }
        return header;
    }

    /* ---------------------------------------------- bonus panel ----------------------------------------------- */

    /**
     * The all-complete bonus, down the right of the rows: how many of the day's tasks are finished,
     * the reward's icon, and the button that collects it.
     *
     * <p>Syncs four integers, in the same spirit as {@link #row}: which reward is on offer (its
     * index in {@link DailyRewardRegistry#all()}), how many tasks are done, how many there are to
     * do — the group's quota, which is no longer a constant — and the claim state. The client
     * resolves the reward's name, tooltip and icons from its own copy of the registry.
     */
    private static UIElement bonusPanel(@Nullable ServerPlayer server) {
        SimpleBinding<Integer> rewardIndex = intBinding(
                bonus(server, v -> DailyRewardRegistry.indexOf(v.reward().id()), -1), -1);
        SimpleBinding<Integer> completed = intBinding(
                bonus(server, DailyTaskManager.BonusView::completed, 0), 0);
        SimpleBinding<Integer> total = intBinding(
                bonus(server, DailyTaskManager.BonusView::total, DailyTaskQuota.DEFAULT),
                DailyTaskQuota.DEFAULT);
        SimpleBinding<Integer> claimState = intBinding(
                bonus(server, DailyTasksUI::bonusStateOf, LOCKED), LOCKED);

        UIElement panel = new UIElement();
        panel.layout(l -> l.flexDirection(FlexDirection.COLUMN).width(BONUS_W).heightPercent(100)
                .flexShrink(0).paddingAll(3).alignItems(AlignItems.CENTER));
        panel.style(s -> s.background(new ColorRectTexture(COLOR_ROW)));
        panel.addSyncValue(rewardIndex.getSyncValue());
        panel.addSyncValue(completed.getSyncValue());
        panel.addSyncValue(total.getSyncValue());
        panel.addSyncValue(claimState.getSyncValue());

        Label heading = new Label();
        heading.setText(Component.translatable("gui.roll_mod.daily_tasks.bonus")
                .withStyle(ChatFormatting.GRAY));
        heading.layout(l -> l.widthPercent(100).height(10));
        heading.textStyle(t -> t.textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER));

        Label counter = new Label();
        // As wide as its text, centred together with the badge right after it.
        counter.layout(l -> l.height(12));
        counter.textStyle(t -> t.textAlignHorizontal(Horizontal.CENTER).adaptiveWidth(true)
                .textAlignVertical(Vertical.CENTER)
                .textShadow(com.roll_54.roll_mod.minestar.theme.RollUi.textShadow()));

        // Auto margins on both sides centre the icon in whatever vertical space is left over,
        // which keeps the panel tidy without pinning its height to the task column's.
        UIElement icon = new UIElement();
        icon.layout(l -> l.width(BONUS_ICON).height(BONUS_ICON)
                .marginTopAuto().marginBottomAuto());

        // Collect, then what it pays: the label and the preview icons stack underneath the button.
        UIElement footer = new UIElement();
        footer.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100)
                .alignItems(AlignItems.CENTER));

        // A real button, so it takes the theme's button shape (AE2's bevel, Ore's art), not a flat dark plate.
        Button collect = new Button();
        // 2px lower (and with it the hint and the reward under it), clear of the bonus icon above.
        collect.layout(l -> l.width(COLLECT_W).height(14).paddingAll(0).marginTop(2));
        com.roll_54.roll_mod.minestar.hub.gui.HubSection.centreCaption(collect);
        // Sent unconditionally, like the per-task Claim: claimBonus re-validates on the server and
        // silently rejects a click on an unfinished or already-collected board.
        // Not clickable while greyed out (until the board is complete).
        collect.addEventListener(UIEvents.CLICK, e -> {
            if (collect.isActive()) PacketDistributor.sendToServer(new ClaimDailyBonusPacket());
        });

        // Says what the icons below it are. The panel is only 126px of content wide and the line is
        // far wider, so it has to wrap and take whatever height that needs.
        Label hint = new Label();
        hint.setText(Component.translatable("gui.roll_mod.daily_tasks.bonus.rewards_hint")
                .withStyle(ChatFormatting.GRAY));
        hint.layout(l -> l.widthPercent(100).marginTop(3));
        hint.textStyle(t -> t.textAlignHorizontal(Horizontal.CENTER)
                .textWrap(TextWrap.WRAP)
                .adaptiveHeight(true));

        // What the reward hands over, as the reward itself chose to advertise it.
        UIElement strip = new UIElement();
        strip.layout(l -> l.flexDirection(FlexDirection.ROW).height(REWARD_ICON)
                .marginTop(3).gapColumn(2));

        footer.addChildren(collect, hint, strip);

        int[] shownReward = {Integer.MIN_VALUE};
        panel.addEventListener(UIEvents.TICK, e -> {
            DailyReward reward = DailyRewardRegistry.byIndex(value(rewardIndex, -1));
            int done = value(completed, 0);
            int state = value(claimState, LOCKED);

            int goal = value(total, DailyTaskQuota.DEFAULT);

            // The reward only changes at the daily roll, so only rebuild the textures when it does.
            if (shownReward[0] != value(rewardIndex, -1)) {
                shownReward[0] = value(rewardIndex, -1);
                icon.style(s -> s.background(reward == null ? null : reward.icon().texture()));

                strip.clearAllChildren();
                if (reward != null) {
                    for (DailyTaskIcon preview : reward.icons()) {
                        UIElement slot = new UIElement();
                        slot.layout(l -> l.width(REWARD_ICON).height(REWARD_ICON));
                        slot.style(s -> s.background(preview.texture()));
                        strip.addChild(slot);
                    }
                }
            }

            counter.setText(Component.literal(done + " / " + goal)
                    .withStyle(done >= goal ? ChatFormatting.GREEN : ChatFormatting.WHITE));
            collect.setText(bonusCaption(state));
            // Greyed out until the board is complete: before that a click does nothing.
            if (collect.isActive() != (state == CLAIMABLE)) collect.setActive(state == CLAIMABLE);
        });

        panel.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> {
            DailyReward reward = DailyRewardRegistry.byIndex(value(rewardIndex, -1));
            if (reward == null) return;

            List<Component> lines = new ArrayList<>();
            lines.add(reward.name().copy());
            addLine(lines, reward.tooltip(), ChatFormatting.GRAY);
            addLine(lines, Component.literal(value(completed, 0) + " / "
                    + value(total, DailyTaskQuota.DEFAULT)), ChatFormatting.DARK_GRAY);
            e.hoverTooltips = new HoverTooltips(lines, null, null, ItemStack.EMPTY);
        });

        // The "ready to collect" badge right after the count, not off in the panel's corner.
        UIElement badge = badge();
        UIElement countRow = new UIElement();
        countRow.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(12).gapColumn(3)
                .justifyContent(org.appliedenergistics.yoga.YogaJustify.CENTER).alignItems(AlignItems.CENTER));
        countRow.setOverflowVisible(true);
        countRow.addChildren(counter, badge);
        panel.addChildren(heading, countRow, icon, footer);
        // Toggled by this panel's own TICK below: the badge has no listener or sync value of its own.
        panel.addEventListener(UIEvents.TICK, e -> {
            boolean flag = value(claimState, LOCKED) == CLAIMABLE;
            if (badge.isDisplayed() != flag) badge.setDisplay(flag);
        });
        return panel;
    }

    /**
     * The hub's attention mark (see {@link HubBadge}) at the top-right of a row or the bonus panel,
     * shown while there is something to collect there. Absolute, so it takes no room in the layout,
     * and carrying no sync value, so it moves no binding.
     */
    private static UIElement badge() {
        UIElement badge = new UIElement();
        badge.layout(l -> l.width(HubBadge.SIZE).height(HubBadge.SIZE).flexShrink(0));
        badge.style(s -> s.background(com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture.dynamic(HubBadge::coloured)).zIndex(1));
        badge.setDisplay(false);
        return badge;
    }

    /* -------------------------------------------------- row --------------------------------------------------- */

    private static UIElement row(@Nullable ServerPlayer server, int index) {
        SimpleBinding<Integer> taskIndex = intBinding(
                view(server, index, v -> DailyTaskRegistry.indexOf(v.task().id()), -1), -1);
        SimpleBinding<Integer> progress = intBinding(view(server, index, DailyTaskManager.TaskView::progress, 0), 0);
        SimpleBinding<Integer> required = intBinding(view(server, index, DailyTaskManager.TaskView::required, 0), 0);
        SimpleBinding<Integer> claimState = intBinding(view(server, index, DailyTasksUI::stateOf, LOCKED), LOCKED);
        // 0 a plain row, 1 the Prime slot (purple), 2 a slot earned from a quest (blue).
        SimpleBinding<Integer> prime = intBinding(view(server, index, v -> v.prime() ? 1 : v.bonus() ? 2 : 0, 0), 0);
        // The row's two drawn rewards, as indices into TaskRewardPool.all(). The starcoin payout
        // every task adds is a constant, so the client appends it rather than the server syncing it.
        SimpleBinding<Integer> rewardA = intBinding(view(server, index, v -> rewardIndex(v, 0), -1), -1);
        SimpleBinding<Integer> rewardB = intBinding(view(server, index, v -> rewardIndex(v, 1), -1), -1);
        // What rerolling this slot would cost the viewer: -1 hides the button (done, or out of
        // rerolls), 0 is free. Per viewer, not per group — the price and the free allowance are
        // theirs — which the per-player server tree gives for free.
        SimpleBinding<Integer> rerollCost = intBinding(
                () -> server == null ? -1 : (int) DailyTaskManager.rerollCost(server, index), -1);
        SimpleBinding<Integer> rerollsLeft = intBinding(
                () -> server == null ? 0 : DailyTaskManager.rerollsLeft(server, index), 0);

        UIElement row = new UIElement();
        row.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(ROW_H)
                .marginTop(index > 0 ? ROW_GAP : 0).paddingAll(3).paddingHorizontal(com.roll_54.roll_mod.minestar.hub.gui.HubSection.ROW_INSET));
        ColorRectTexture plain = new ColorRectTexture(COLOR_ROW);
        ColorRectTexture purple = new ColorRectTexture(COLOR_ROW_PRIME);
        ColorRectTexture blue = new ColorRectTexture(COLOR_ROW_BONUS);
        row.style(s -> s.background(com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture.dynamic(() -> switch (value(prime, 0)) {
            case 1 -> purple;
            case 2 -> blue;
            default -> plain;
        })));
        // Clipped only once it collapses (the tick below): a collapsed row keeps its fixed-height children.
        row.setOverflowVisible(true);
        row.addSyncValue(taskIndex.getSyncValue());
        row.addSyncValue(progress.getSyncValue());
        row.addSyncValue(required.getSyncValue());
        row.addSyncValue(claimState.getSyncValue());
        row.addSyncValue(prime.getSyncValue());
        row.addSyncValue(rewardA.getSyncValue());
        row.addSyncValue(rewardB.getSyncValue());
        row.addSyncValue(rerollCost.getSyncValue());
        row.addSyncValue(rerollsLeft.getSyncValue());

        // One element for both icon kinds: DailyTaskIcon.texture() hands back an ItemStackTexture
        // for an item icon and a SpriteTexture for a PNG, so the row does not care which it is.
        UIElement icon = new UIElement();
        icon.layout(l -> l.width(ICON).height(ICON).marginRight(4));

        UIElement middle = new UIElement();
        middle.layout(l -> l.flexDirection(FlexDirection.COLUMN).flexGrow(1).marginRight(4));

        Label name = new Label();
        // As wide as its text, so the badge can sit right after it.
        name.layout(l -> l.height(10).flexShrink(1));
        name.textStyle(t -> t.textAlignVertical(Vertical.CENTER).adaptiveWidth(true).textWrap(TextWrap.HIDE));

        ProgressBar bar = new ProgressBar();
        bar.layout(l -> l.widthPercent(100).height(12));
        bar.setRange(0f, 1f);
        // ProgressBar's own constructor pads barContainer by 4px on every side and sizes the
        // element to 14px. At a row-sized height that padding eats the whole content box and the
        // fill collapses to zero height — invisible, while the label still draws because the
        // constructor positions it ABSOLUTE. 2px keeps the fill inside the themes' 2px frame (at 1px a small fill sat on
        // the frame's left edge) without starving it.
        bar.barContainer(container -> container.layout(l -> l.paddingAll(2)));
        // The Prime and earned slots tint their bar too (purple, blue), not just the row: a tint over the track, under
        // the fill.
        UIElement barTint = new UIElement();
        barTint.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).left(0).top(0).widthPercent(100).heightPercent(100));
        barTint.setAllowHitTest(false);
        ColorRectTexture tintPrime = new ColorRectTexture(0x806A2C91);
        ColorRectTexture tintBonus = new ColorRectTexture(0x802C6E91);
        barTint.style(s -> s.zIndex(-1).background(com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture.dynamic(() ->
                switch (value(prime, 0)) {
                    case 1 -> tintPrime;
                    case 2 -> tintBonus;
                    default -> com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture.EMPTY;
                })));
        bar.barContainer(container -> container.addChild(barTint));

        // The bottom line under the bar: the button that claims the task, then what it pays, both
        // packed against the column's left edge.
        UIElement bottom = new UIElement();
        bottom.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100)
                .height(REWARD_ICON).marginTop(ROW_INNER_GAP).alignItems(AlignItems.CENTER));

        // A real button, so it takes the theme's button shape, not a flat dark plate.
        Button claim = new Button();
        claim.layout(l -> l.width(CLAIM_W).height(14).marginRight(ROW_INNER_GAP).paddingAll(0));
        tint(claim, prime);
        claim.text.layout(l -> l.top(1)); // the caption 1px lower, level with the reroll price
        com.roll_54.roll_mod.minestar.hub.gui.HubSection.centreCaption(claim);
        // Sent unconditionally: DailyTaskManager.claim re-validates on the server and silently
        // rejects a click on a task that is not finished or already taken.
        // Not clickable while greyed out (until the task is done).
        claim.addEventListener(UIEvents.CLICK, e -> {
            if (claim.isActive()) PacketDistributor.sendToServer(new ClaimDailyTaskPacket(index));
        });

        // Icons sit flush against each other, so the strip reads as one run.
        UIElement rewards = new UIElement();
        rewards.layout(l -> l.flexDirection(FlexDirection.ROW).height(REWARD_ICON));

        Label rerollPrice = new Label();
        UIElement reroll = rerollButton(index, rerollCost, rerollsLeft, rerollPrice);
        tint(reroll, prime);
        // What a claimed daily adds to the battle pass, after its rewards.
        Label passPoints = new Label();
        passPoints.layout(l -> l.height(REWARD_ICON).marginLeft(4).paddingTop(1));
        passPoints.textStyle(t -> t.textAlignVertical(Vertical.CENTER).fontSize(7).adaptiveWidth(true));
        UIElement passIcon = com.roll_54.roll_mod.minestar.hub.gui.BattlePassTab.pointsIcon();
        passIcon.layout(l -> l.marginLeft(4));
        passPoints.layout(l -> l.marginLeft(2));
        bottom.addChildren(claim, rewards, passIcon, passPoints);
        // The "ready to claim" badge right after the task's name, not off in the row's corner.
        UIElement badge = badge();
        UIElement title = new UIElement();
        title.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(10).gapColumn(3)
                .alignItems(AlignItems.CENTER));
        title.setOverflowVisible(true);
        title.addChildren(name, badge);
        middle.addChildren(title, bar, bottom);
        row.addChildren(icon, middle, reroll);

        int[] shownTask = {Integer.MIN_VALUE};
        int[] shownRewards = {Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
        boolean[] shownPresent = {true};
        row.addEventListener(UIEvents.TICK, e -> {
            DailyTask task = DailyTaskRegistry.byIndex(value(taskIndex, -1));
            int done = value(progress, 0);
            int goal = value(required, 0);
            int state = value(claimState, LOCKED);

            boolean flag = state == CLAIMABLE;
            if (badge.isDisplayed() != flag) badge.setDisplay(flag);

            // A slot past the group's quota syncs no task at all: collapse it to nothing so the
            // list is as long as the set actually drawn.
            //
            // Height rather than setDisplay, deliberately — screenTick and serverTick both recurse
            // only into children that are displayed, so a row that hid itself here would never
            // tick again to notice a task arriving, and every row starts out empty.
            boolean present = value(taskIndex, -1) >= 0;
            if (shownPresent[0] != present) {
                shownPresent[0] = present;
                // Only an empty row clips (its fixed-height children would show otherwise); a shown one lets its
                // buttons' bevelled bottom edge (AE2, Ore) draw past the row's padding instead of being cut off.
                row.setOverflowVisible(present);
                // The padding too: kept on an empty row it left 6px each, and four of them made the list scroll.
                row.layout(l -> l.height(present ? ROW_H : 0)
                        // The gap goes above a row, not under it: under the last one it doubled up with the list's own
                        // edge into one thick bar.
                        .marginTop(present && index > 0 ? ROW_GAP : 0).paddingTop(present ? 3 : 0).paddingBottom(present ? 3 : 0));
            }

            // The icon only changes at the daily roll, so only rebuild the texture when it does.
            if (shownTask[0] != value(taskIndex, -1)) {
                shownTask[0] = value(taskIndex, -1);
                icon.style(s -> s.background(task == null ? null : task.icon().texture()));
            }

            // Same again for the reward strip, which also only moves at the roll.
            int drawnA = value(rewardA, -1);
            int drawnB = value(rewardB, -1);
            int slotKind = value(prime, 0);
            if (shownRewards[0] != drawnA || shownRewards[1] != drawnB || shownRewards[2] != slotKind) {
                shownRewards[0] = drawnA;
                shownRewards[1] = drawnB;
                shownRewards[2] = slotKind;

                rewards.clearAllChildren();
                if (task != null) {
                    for (TaskReward reward : drawn(drawnA, drawnB)) {
                        rewards.addChild(rewardIcon(reward.icon()));
                    }
                    // Only the base board pays starcoins; the Prime and earned slots pay their draw alone.
                    if (slotKind == 0) {
                    rewards.addChild(rewardIcon(TaskRewardPool.STARCOINS.icon()));
                    // How many starcoins, written after their icon as the battle-pass points are.
                    Label coins = new Label();
                    coins.setText(Component.literal("+" + TaskRewardPool.STARCOIN_AMOUNT).withColor(0xD7A1F9));
                    coins.layout(l -> l.height(REWARD_ICON).marginLeft(2).paddingTop(1));
                    coins.textStyle(t -> t.textAlignVertical(Vertical.CENTER).fontSize(7).adaptiveWidth(true));
                    rewards.addChild(coins);
                    }
                }
            }

            int bp = com.roll_54.roll_mod.minestar.battlepass.ClientBattlePassCache.DATA.getInt("pDaily");
            boolean showBp = task != null && bp > 0;
            if (passIcon.isDisplayed() != showBp) passIcon.setDisplay(showBp);
            passPoints.setText(!showBp ? Component.empty()
                    : Component.translatable("gui.roll_mod.daily_tasks.bp_points", bp).withStyle(ChatFormatting.GOLD));

            name.setText(task == null
                    ? Component.translatable("gui.roll_mod.daily_tasks.unavailable")
                    : state == LOCKED ? task.name().copy()
                            : task.name().copy().withStyle(ChatFormatting.GREEN));
            bar.setProgress(goal <= 0 ? 0f : Math.min(1f, (float) done / goal));
            bar.label.setText(task == null ? Component.empty()
                    : Component.literal(done + " / " + goal));
            claim.setText(claimCaption(state));
            // Greyed out until the task is done (and after it is taken): before that a click does nothing.
            if (claim.isActive() != (state == CLAIMABLE)) claim.setActive(state == CLAIMABLE);

            int cost = value(rerollCost, -1);
            boolean canReroll = task != null && cost >= 0;
            if (reroll.isDisplayed() != canReroll) reroll.setDisplay(canReroll);
            if (canReroll) rerollPrice.setText(rerollCaption(cost));
        });

        row.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> {
            DailyTask task = DailyTaskRegistry.byIndex(value(taskIndex, -1));
            if (task == null) return;

            List<Component> lines = new ArrayList<>();
            lines.add(task.name().copy());
            addLine(lines, task.tooltip(value(required, 0)), ChatFormatting.GRAY);
            addLine(lines, Component.literal(value(progress, 0) + " / " + value(required, 0)),
                    ChatFormatting.DARK_GRAY);
            addLine(lines, Component.translatable("gui.roll_mod.daily_tasks.rewards",
                            rewardNames(value(rewardA, -1), value(rewardB, -1), value(prime, 0) == 0)),
                    ChatFormatting.AQUA);
            e.hoverTooltips = new HoverTooltips(lines, null, null, ItemStack.EMPTY);
        });

        return row;
    }

    /**
     * The row's reroll button, pinned to its bottom-right corner. Absolute, like {@link #badge()},
     * so it takes no room from the claim button and reward strip on the left. The row's TICK shows
     * it only while the slot can still be rerolled — never on a finished task.
     */
    /**
     * The Prime (purple) and earned (blue) slots tint their buttons too: a translucent wash over the button's face,
     * under its caption and icon.
     */
    private static void tint(UIElement button, SimpleBinding<Integer> kind) {
        UIElement wash = new UIElement();
        wash.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).left(1).top(1).right(1).bottom(1));
        wash.setAllowHitTest(false);
        ColorRectTexture prime = new ColorRectTexture(0x606A2C91);
        ColorRectTexture bonus = new ColorRectTexture(0x602C6E91);
        wash.style(s -> s.zIndex(-1).background(com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture.dynamic(() ->
                switch (value(kind, 0)) {
                    case 1 -> prime;
                    case 2 -> bonus;
                    default -> com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture.EMPTY;
                })));
        button.addChild(wash);
    }

    private static UIElement rerollButton(int index, SimpleBinding<Integer> cost,
                                          SimpleBinding<Integer> left, Label price) {
        // A real button with the icon and price as its content, so it is the theme's button shape.
        Button reroll = new Button();
        reroll.noText();
        boolean bevel = net.neoforged.fml.loading.FMLEnvironment.dist.isClient()
                && (com.roll_54.roll_mod.minestar.theme.UiThemeState.get() == com.roll_54.roll_mod.minestar.theme.UiTheme.AE2
                || com.roll_54.roll_mod.minestar.theme.UiThemeState.get().oreArt());
        // AE2 and Ore use the pixel-art small_reroll.png, squeezed into the usual icon box: the button keeps its size.
        int grow = 0;
        int side = 0;
        // Anchored on the right, so the 3px left padding widens it leftwards and the price stays put.
        // At the row's right end, as it was, and level with Locked: that one is centred in the 16px bottom line over
        // the row's padding, and measured in game its bottom edge sits 6px up. Wider than before,
        // same height.
        reroll.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).right(8 - side).bottom(6 - grow)
                .flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER)
                .height(14 + 2 * grow).paddingAll(0).paddingLeft(6 + side).paddingRight(3 + side));
        reroll.style(s -> s.zIndex(1));
        reroll.setDisplay(false);

        UIElement icon = new UIElement();
        // Neither the icon nor the price may shrink: squeezed, the button came out narrower than its content and the
        // icon hung out past its left edge.
        // Placed exactly, out of the flow: the theme's button padding (which beats an inline paddingAll(0)) pushed it
        // down past the button's bottom edge on Ore and AE2.
        int iconLeft = 6 + side;
        icon.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).left(iconLeft).top((14 + 2 * grow - REROLL_ICON) / 2f)
                .width(REROLL_ICON).height(REROLL_ICON));
        icon.setAllowHitTest(false);
        icon.style(s -> s.background(SpriteTexture.of(bevel ? REROLL_SMALL_TEXTURE : REROLL_TEXTURE)));

        // Right after the icon and as wide as the price: a fixed 64px box with the price centred in it left a wide
        // gap between them. The button shrinks to fit.
        // Starts after the icon, which is out of the flow now; 1px lower than centred.
        price.layout(l -> l.height(14).marginLeft(REROLL_ICON + 3).marginRight(4).flexShrink(0).top(1));
        price.textStyle(t -> t.adaptiveWidth(true).textAlignHorizontal(Horizontal.LEFT)
                .textAlignVertical(Vertical.CENTER)
                .textShadow(com.roll_54.roll_mod.minestar.theme.RollUi.textShadow()));
        reroll.addChildren(icon, price);
        // Sent unconditionally: DailyTaskManager.rerollSlot prices and re-validates on the server.
        reroll.addEventListener(UIEvents.CLICK, e -> {
            PacketDistributor.sendToServer(new RerollDailyTaskPacket(index));
            e.stopPropagation();
        });
        reroll.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> {
            int now = value(cost, -1);
            Component what = now == 0
                    ? Component.translatable("gui.roll_mod.daily_tasks.reroll.free")
                    : Component.translatable("gui.roll_mod.daily_tasks.reroll.price", now);
            e.hoverTooltips = new HoverTooltips(List.of(
                    // The button's orchid, not gold.
                    Component.translatable("gui.roll_mod.daily_tasks.reroll.tip", what).withColor(0xD7A1F9),
                    Component.translatable("gui.roll_mod.daily_tasks.reroll.left", value(left, 0))
                            .withStyle(ChatFormatting.GRAY)),
                    null, null, ItemStack.EMPTY);
            // Otherwise the row's own tooltip, handled further up, replaces this one.
            e.stopPropagation();
        });
        return reroll;
    }

    private static Component rerollCaption(int cost) {
        return cost == 0
                ? Component.translatable("gui.roll_mod.daily_tasks.reroll.free")
                        .withStyle(ChatFormatting.GREEN)
                // A light orchid, from the reroll icon's own pink-to-violet, rather than gold, which clashed with it.
                : Component.literal(String.valueOf(cost)).withColor(0xD7A1F9);
    }

    /* ------------------------------------------------- rewards ------------------------------------------------ */

    /**
     * The pool index of the {@code n}th reward this row pays, or {@code -1} when it has none — a
     * slot past the quota, or a pool that has shrunk since the draw.
     */
    private static int rewardIndex(DailyTaskManager.TaskView view, int n) {
        List<TaskReward> rewards = view.rewards();
        return n < rewards.size() ? TaskRewardPool.indexOf(rewards.get(n).id()) : -1;
    }

    /** The synced pair, resolved against the client's own copy of the pool; unknown ones dropped. */
    private static List<TaskReward> drawn(int first, int second) {
        List<TaskReward> rewards = new ArrayList<>(TaskRewardPool.NUM_PER_TASK);
        for (int index : new int[]{first, second}) {
            TaskReward reward = TaskRewardPool.byIndex(index);
            if (reward != null) rewards.add(reward);
        }
        return rewards;
    }

    /** One 16px cell of a reward strip. */
    private static UIElement rewardIcon(DailyTaskIcon icon) {
        UIElement slot = new UIElement();
        slot.layout(l -> l.width(REWARD_ICON).height(REWARD_ICON));
        slot.style(s -> s.background(icon.texture()));
        return slot;
    }

    /** {@code "8 × Diamond, 2 × Netherite Ingot, 2500 Starcoins"}, for the row tooltip. */
    private static Component rewardNames(int first, int second, boolean coins) {
        MutableComponent out = Component.empty();
        boolean any = false;
        for (TaskReward reward : drawn(first, second)) {
            if (any) out.append(", ");
            out.append(reward.name());
            any = true;
        }
        if (coins) out.append(any ? ", " : "").append(TaskRewardPool.STARCOINS.name());
        return out;
    }

    /* ------------------------------------------------- content ------------------------------------------------ */

    private static Component claimCaption(int state) {
        return switch (state) {
            case CLAIMED -> Component.translatable("gui.roll_mod.daily_tasks.claimed")
                    .withStyle(ChatFormatting.DARK_GRAY);
            case CLAIMABLE -> Component.translatable("gui.roll_mod.daily_tasks.claim")
                    .withStyle(ChatFormatting.GREEN);
            default -> Component.translatable("gui.roll_mod.daily_tasks.locked")
                    .withStyle(ChatFormatting.GRAY);
        };
    }

    private static Component bonusCaption(int state) {
        return switch (state) {
            case CLAIMED -> Component.translatable("gui.roll_mod.daily_tasks.bonus.collected")
                    .withStyle(ChatFormatting.DARK_GRAY);
            case CLAIMABLE -> Component.translatable("gui.roll_mod.daily_tasks.bonus.collect")
                    .withStyle(ChatFormatting.GREEN);
            default -> Component.translatable("gui.roll_mod.daily_tasks.bonus.locked")
                    .withStyle(ChatFormatting.GRAY);
        };
    }

    private static int stateOf(DailyTaskManager.TaskView v) {
        if (v.claimed()) return CLAIMED;
        return v.completed() ? CLAIMABLE : LOCKED;
    }

    private static int bonusStateOf(DailyTaskManager.BonusView v) {
        if (v.claimed()) return CLAIMED;
        return v.completed() >= v.total() ? CLAIMABLE : LOCKED;
    }

    private static void addLine(List<Component> lines, @Nullable Component line, ChatFormatting style) {
        if (line == null || line.getString().isEmpty()) return;
        lines.add(line.copy().withStyle(style));
    }

    private static String formatDuration(long seconds) {
        seconds = Math.max(0, seconds);
        return String.format("%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
    }

    /* ------------------------------------------------- plumbing ----------------------------------------------- */

    /**
     * A server→client int. Attached with {@code addSyncValue} rather than {@code bind} because it
     * feeds logic, not an element: with no remote data source the received value simply rests in
     * the sync value, and {@code SyncValue.update()} leaves it alone client-side, an S2C binding
     * not being {@code toSync} there.
     */
    private static SimpleBinding<Integer> intBinding(Supplier<Integer> supplier, int initial) {
        return DataBindingBuilder.intValS2C(supplier).initialValue(initial).build();
    }

    private static int value(SimpleBinding<Integer> binding, int fallback) {
        Integer v = binding.getSyncValue().getValue();
        return v == null ? fallback : v;
    }

    /**
     * Wraps a per-row lookup as a supplier for an S2C binding. On the client {@code server} is null
     * and the supplier is never consulted; before the first roll the view is null and the fallback
     * stands in.
     */
    private static <T> Supplier<T> view(@Nullable ServerPlayer server, int index,
                                        Function<DailyTaskManager.TaskView, T> mapper, T fallback) {
        return () -> {
            if (server == null) return fallback;
            DailyTaskManager.TaskView v = DailyTaskManager.view(server, index);
            return v == null ? fallback : mapper.apply(v);
        };
    }

    /** {@link #view} for the bonus panel: null before anything is rolled, or with no reward registered. */
    private static <T> Supplier<T> bonus(@Nullable ServerPlayer server,
                                         Function<DailyTaskManager.BonusView, T> mapper, T fallback) {
        return () -> {
            if (server == null) return fallback;
            DailyTaskManager.BonusView v = DailyTaskManager.bonusView(server);
            return v == null ? fallback : mapper.apply(v);
        };
    }
}
