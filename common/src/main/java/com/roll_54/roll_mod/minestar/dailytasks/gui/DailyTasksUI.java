package com.roll_54.roll_mod.minestar.dailytasks.gui;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SimpleBinding;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.MCSprites;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.dailytasks.DailyTaskManager;
import com.roll_54.roll_mod.minestar.dailytasks.DailyTaskRegistry;
import com.roll_54.roll_mod.minestar.dailytasks.DailyTasksState;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTask;
import com.roll_54.roll_mod.network.packet.ClaimDailyTaskPacket;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

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
 * uses it to feed the server→client bindings. Only four integers per row cross the wire: which task
 * occupies the slot (its index in {@link DailyTaskRegistry#all()}), progress, requirement and claim
 * state. Everything a player actually sees — name, tooltip, icon — the client looks up in the
 * registry itself, which is built identically on both sides. That is why the registry sorts by id:
 * an index has to mean the same task everywhere.
 */
public final class DailyTasksUI {

    public static final ResourceLocation UI_ID = RollMod.id("daily_tasks");


    //do not change this number are good!
    private static final int ROOT_W = 350;
    private static final int PADDING = 12;
    private static final int ROW_H = 28;
    private static final int ICON = 18;
    private static final int CLAIM_W = 70;

    private static final int COLOR_ROW = 0x40000000;
    private static final int COLOR_BUTTON = 0x80000000;

    /** Claim states, as synced in {@code state}. */
    private static final int LOCKED = 0;
    private static final int CLAIMABLE = 1;
    private static final int CLAIMED = 2;

    private DailyTasksUI() {}

    public static ModularUI createUI(Player player) {
        ServerPlayer server = player instanceof ServerPlayer sp ? sp : null;

        UIElement root = new UIElement();
        root.layout(l -> l.flexDirection(FlexDirection.COLUMN).width(ROOT_W).paddingAll(PADDING));
        root.style(s -> s.background(MCSprites.BORDER));

        root.addChild(header(server));
        for (int i = 0; i < DailyTasksState.TASK_COUNT; i++) {
            root.addChild(row(server, i));
        }

        return ModularUI.of(UI.of(root), player);
    }

    /* ------------------------------------------------- header ------------------------------------------------- */

    private static UIElement header(@Nullable ServerPlayer server) {
        UIElement header = new UIElement();
        header.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(12)
                .marginBottom(4));

        Label title = new Label();
        title.setText(Component.translatable("gui.roll_mod.daily_tasks.title"));
        title.layout(l -> l.flexGrow(1));
        title.textStyle(t -> t.textAlignVertical(Vertical.CENTER));

        // Seconds come from the server: the roll follows the server machine's clock, which is not
        // the viewer's. Only the formatting happens client-side.
        SimpleBinding<Integer> secondsLeft = intBinding(
                () -> server == null ? 0 : (int) DailyTaskManager.secondsUntilNextRoll(), 0);

        Label resets = new Label();
        resets.layout(l -> l.width(96));
        resets.textStyle(t -> t.textAlignHorizontal(Horizontal.RIGHT)
                .textAlignVertical(Vertical.CENTER)
                .textColor(0xFFAAAAAA));
        resets.addSyncValue(secondsLeft.getSyncValue());
        resets.addEventListener(UIEvents.TICK, e -> resets.setText(Component.translatable(
                "gui.roll_mod.daily_tasks.resets_in", formatDuration(value(secondsLeft, 0)))));

        header.addChildren(title, resets);
        return header;
    }

    /* -------------------------------------------------- row --------------------------------------------------- */

    private static UIElement row(@Nullable ServerPlayer server, int index) {
        SimpleBinding<Integer> taskIndex = intBinding(
                view(server, index, v -> DailyTaskRegistry.indexOf(v.task().id()), -1), -1);
        SimpleBinding<Integer> progress = intBinding(view(server, index, DailyTaskManager.TaskView::progress, 0), 0);
        SimpleBinding<Integer> required = intBinding(view(server, index, DailyTaskManager.TaskView::required, 0), 0);
        SimpleBinding<Integer> claimState = intBinding(view(server, index, DailyTasksUI::stateOf, LOCKED), LOCKED);

        UIElement row = new UIElement();
        row.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(ROW_H)
                .marginBottom(3).paddingAll(3));
        row.style(s -> s.background(new ColorRectTexture(COLOR_ROW)));
        row.addSyncValue(taskIndex.getSyncValue());
        row.addSyncValue(progress.getSyncValue());
        row.addSyncValue(required.getSyncValue());
        row.addSyncValue(claimState.getSyncValue());

        // One element for both icon kinds: DailyTaskIcon.texture() hands back an ItemStackTexture
        // for an item icon and a SpriteTexture for a PNG, so the row does not care which it is.
        UIElement icon = new UIElement();
        icon.layout(l -> l.width(ICON).height(ICON).marginRight(4));

        UIElement middle = new UIElement();
        middle.layout(l -> l.flexDirection(FlexDirection.COLUMN).flexGrow(1).marginRight(4));

        Label name = new Label();
        name.layout(l -> l.widthPercent(100).height(10));
        name.textStyle(t -> t.textAlignVertical(Vertical.CENTER));

        ProgressBar bar = new ProgressBar();
        bar.layout(l -> l.widthPercent(100).height(12));
        bar.setRange(0f, 1f);
        // ProgressBar's own constructor pads barContainer by 4px on every side and sizes the
        // element to 14px. At a row-sized height that padding eats the whole content box and the
        // fill collapses to zero height — invisible, while the label still draws because the
        // constructor positions it ABSOLUTE. 1px keeps the frame without starving the fill.
        bar.barContainer(container -> container.layout(l -> l.paddingAll(1)));

        Label claim = new Label();
        claim.layout(l -> l.width(CLAIM_W).heightPercent(100));
        claim.textStyle(t -> t.textAlignHorizontal(Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER)
                .textShadow(true));
        claim.style(s -> s.background(new ColorRectTexture(COLOR_BUTTON)));
        // Sent unconditionally: DailyTaskManager.claim re-validates on the server and silently
        // rejects a click on a task that is not finished or already taken.
        claim.addEventListener(UIEvents.CLICK,
                e -> PacketDistributor.sendToServer(new ClaimDailyTaskPacket(index)));

        middle.addChildren(name, bar);
        row.addChildren(icon, middle, claim);

        int[] shownTask = {Integer.MIN_VALUE};
        row.addEventListener(UIEvents.TICK, e -> {
            DailyTask task = DailyTaskRegistry.byIndex(value(taskIndex, -1));
            int done = value(progress, 0);
            int goal = value(required, 0);
            int state = value(claimState, LOCKED);

            // The icon only changes at the daily roll, so only rebuild the texture when it does.
            if (shownTask[0] != value(taskIndex, -1)) {
                shownTask[0] = value(taskIndex, -1);
                icon.style(s -> s.background(task == null ? null : task.icon().texture()));
            }

            name.setText(task == null
                    ? Component.translatable("gui.roll_mod.daily_tasks.unavailable")
                    : state == LOCKED ? task.name().copy()
                            : task.name().copy().withStyle(ChatFormatting.GREEN));
            bar.setProgress(goal <= 0 ? 0f : Math.min(1f, (float) done / goal));
            bar.label.setText(task == null ? Component.empty()
                    : Component.literal(done + " / " + goal));
            claim.setText(claimCaption(state));
        });

        row.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> {
            DailyTask task = DailyTaskRegistry.byIndex(value(taskIndex, -1));
            if (task == null) return;

            List<Component> lines = new ArrayList<>();
            lines.add(task.name().copy());
            addLine(lines, task.tooltip(value(required, 0)), ChatFormatting.GRAY);
            addLine(lines, Component.literal(value(progress, 0) + " / " + value(required, 0)),
                    ChatFormatting.DARK_GRAY);
            e.hoverTooltips = new HoverTooltips(lines, null, null, ItemStack.EMPTY);
        });

        return row;
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

    private static int stateOf(DailyTaskManager.TaskView v) {
        if (v.claimed()) return CLAIMED;
        return v.completed() ? CLAIMABLE : LOCKED;
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
}
