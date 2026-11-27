package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
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
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.battlepass.BattlePassConfig;
import com.roll_54.roll_mod.minestar.battlepass.BattlePassReward;
import com.roll_54.roll_mod.minestar.battlepass.BattlePassService;
import com.roll_54.roll_mod.minestar.battlepass.ClientBattlePassCache;
import com.roll_54.roll_mod.minestar.dailytasks.DailyTaskRegistry;
import com.roll_54.roll_mod.minestar.hub.QuestGate;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTask;
import com.roll_54.roll_mod.network.packet.BattlePassPackets;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * The battle pass tab: the free track (128 levels) and the Prime track (64, each spanning two free columns) as rows
 * of reward boxes joined by a line, scrolled sideways; a button puts the Prime row above or below. Under the tracks,
 * the season's battle-pass tasks. Everything comes from {@link ClientBattlePassCache}; the tab asks for it when
 * shown and claims with a click on a ready box.
 */
public final class BattlePassTab {

    /** One free level's column; a Prime level spans two. */
    /** Room between boxes for the progress bar and its number. */
    private static final int COL = 88;
    private static final int BOX = 36;
    /** The season's last reward on each track is drawn bigger. */
    private static final int BIG = 52;
    private static final int ROW = BIG + 6;
    private static final int TRACK_H = ROW * 2 + 6;
    /**
     * The canvas, taller than the two rows so the block veils and gate lines reach down to the scroll bar instead of
     * stopping short of it; the rows sit {@link #TOP} lower inside it, centred.
     */
    private static final int CANVAS_H = TRACK_H + 14;
    private static final int TOP = 7;
    private static final int TASKS_W = 300;
    /** Where free level {@code level}'s box is centred. */
    private static int freeX(int level) {
        return (level - 1) * COL + COL / 2;
    }

    /**
     * Where Prime level {@code level}'s box is centred: always straight under a free box. Every Prime block's first
     * level sits under its free block's first level (Prime 1 under free 1, 9 under 17 … 57 under 113), so each big
     * Prime box is under a big free one; its other levels follow a free column pair apart; and the season's last
     * Prime level, big too, sits under the free track's last box (Prime 64 under free 128).
     */
    private static int primeX(int level) {
        if (level >= BattlePassConfig.PRIME_LEVELS) return freeX(BattlePassConfig.FREE_LEVELS);
        int per = BattlePassConfig.PRIME_PER_BLOCK, freePer = BattlePassConfig.FREE_PER_BLOCK;
        int block = (level - 1) / per, step = (level - 1) % per;
        return freeX(block * freePer + 1 + step * (freePer / per));
    }

    /** The Prime track's locks are purple. */
    private static final int PRIME_LOCK = 0xFFB45CFF;
    private static final int TASKS_H = 260;
    private static final String PRIME_TOP_KEY = "battlepass.primeTop";
    private static final ResourceLocation_ MONEY = new ResourceLocation_();

    private BattlePassTab() {}

    /** Tiny holder so the money icon is built once. */
    private static final class ResourceLocation_ {
        final IGuiTexture texture = SpriteTexture.of(RollMod.id("textures/item/lp.png"));
    }

    public static UIElement build(Player player) {
        boolean client = player.level().isClientSide();
        UIElement root = new UIElement();
        root.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).heightPercent(100)
                .paddingAll(HubSection.TAB_PADDING).gapRow(3));
        HubSection.Info section = HubSection.info("battlepass");

        boolean[] primeTop = {client && HubWindowState.get().split(PRIME_TOP_KEY, 0) == 1};
        int[] layoutVersion = {0};
        Button swap = new Button();
        swap.setText(Component.translatable("gui.roll_mod.battlepass.swap"));
        // Sized like the other title-row buttons (the auction's): 16 tall, overhanging the 12px row evenly.
        swap.layout(l -> l.width(64).height(16).paddingAll(0).marginRight(4).marginTop(-2).marginBottom(-2));
        HubSection.centreCaption(swap);
        swap.setOnClick(e -> {
            primeTop[0] = !primeTop[0];
            layoutVersion[0]++;
            if (!client) return;
            HubWindowState state = HubWindowState.get();
            state.setSplit(PRIME_TOP_KEY, primeTop[0] ? 1 : 0);
            state.save();
        });

        Label summary = new Label();
        summary.layout(l -> l.widthPercent(100).marginTop(3));
        summary.textStyle(t -> t.textWrap(TextWrap.WRAP).adaptiveHeight(true));

        ScrollerView track = new ScrollerView();
        track.layout(l -> l.widthPercent(100).height(TRACK_H + 22).flexShrink(0));
        track.scrollerStyle(s -> s.mode(ScrollerMode.HORIZONTAL)
                .horizontalScrollDisplay(ScrollDisplay.AUTO).verticalScrollDisplay(ScrollDisplay.NEVER));
        // No padding top and bottom: the canvas fills the height itself, so veils and gates run edge to edge.
        track.viewPort(v -> v.layout(l -> l.paddingHorizontal(4).paddingVertical(0)));
        track.style(s -> s.background(new ColorRectTexture(0x30000000)));

        // The tasks are a page of their own in the tab, laid out like the daily tasks; the button flips between
        // the pass and them.
        ScrollerView tasks = new ScrollerView();
        tasks.layout(l -> l.widthPercent(100).flexBasis(0).flexGrow(1));
        tasks.scrollerStyle(s2 -> s2.mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.AUTO).horizontalScrollDisplay(ScrollDisplay.NEVER));
        tasks.viewPort(v -> v.layout(l -> l.paddingAll(7)));
        tasks.viewContainer(c -> c.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100)));
        tasks.setDisplay(false);
        UIElement pass = new UIElement();
        pass.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).flexBasis(0).flexGrow(1).gapRow(3));
        pass.addChildren(summary, track);
        // The tasks moved to the Daily Tasks tab (Weekly / Seasonal); the flip button is kept but no longer shown.
        boolean[] showTasks = {false};
        Button tasksButton = new Button();
        tasksButton.setText(Component.translatable("gui.roll_mod.battlepass.tasks.button"));
        tasksButton.layout(l -> l.width(64).height(16).paddingAll(0).marginRight(4).marginTop(-2).marginBottom(-2));
        HubSection.centreCaption(tasksButton);
        tasksButton.setOnClick(e -> {
            showTasks[0] = !showTasks[0];
            tasks.setDisplay(showTasks[0]);
            pass.setDisplay(!showTasks[0]);
            swap.setDisplay(!showTasks[0]);
            tasksButton.setText(Component.translatable(showTasks[0] ? "gui.roll_mod.battlepass.pass.button"
                    : "gui.roll_mod.battlepass.tasks.button"));
        });

        // A choice reward: a small window listing its options; picking one claims the level with it.
        ScrollerView choiceList = StaffUi.scroller();
        HubSection.Floating choice = HubSection.popup(Component.translatable("gui.roll_mod.battlepass.choose"), 260, 190,
                body -> {
                    body.layout(l -> l.flexDirection(FlexDirection.COLUMN));
                    body.addChild(choiceList);
                });
        ChoicePicker picker = (prime, level, options) -> {
            choiceList.clearAllScrollViewChildren();
            for (int i = 0; i < options.size(); i++) {
                int pick = i;
                UIElement row = StaffUi.row();
                row.setOverflowVisible(true);
                String option = options.get(i);
                UIElement preview;
                if (BattlePassReward.kind(option) == BattlePassReward.Kind.SKIN) {
                    // The skin itself, or every skin in turn for a random one.
                    var def = BattlePassReward.skin(option);
                    preview = def != null ? SkinDollElement.Icon.skin(def, false, () -> false)
                            : new SkinDollElement.Cycle(new ArrayList<>(com.roll_54.roll_mod.cosmetics.ItemSkinRegistry.all()));
                } else {
                    preview = new UIElement();
                    ItemStack stack = BattlePassReward.stack(option);
                    IGuiTexture tex = stack.isEmpty() ? MONEY.texture : new ItemStackTexture(stack);
                    preview.style(st -> st.background(tex));
                }
                preview.layout(l -> l.width(16).height(16).flexShrink(0));
                Button take = StaffUi.button(BattlePassReward.describe(options.get(i)), 0, () -> {
                    CompoundTag args = new CompoundTag();
                    args.putBoolean("prime", prime);
                    args.putInt("level", level);
                    args.putInt("pick", pick);
                    PacketDistributor.sendToServer(new BattlePassPackets.Act("claim", args));
                    choice.close();
                });
                take.layout(l -> l.flexBasis(0).flexGrow(1));
                row.addChildren(preview, take);
                choiceList.addScrollViewChild(row);
            }
            choice.open();
        };

        root.addChildren(HubSection.header("battlepass", section, swap), pass, tasks,
                section.overlay(), choice.root());

        int[] shown = {-1, -1};
        boolean[] wasVisible = {false};
        root.addEventListener(UIEvents.TICK, e -> {
            if (!client) return;
            boolean visible = root.getSizeHeight() > 1;
            if (visible && !wasVisible[0]) PacketDistributor.sendToServer(
                    new BattlePassPackets.Act("get", new CompoundTag()));
            wasVisible[0] = visible;
            if (shown[0] == ClientBattlePassCache.VERSION && shown[1] == layoutVersion[0]) return;
            shown[0] = ClientBattlePassCache.VERSION;
            shown[1] = layoutVersion[0];
            CompoundTag data = ClientBattlePassCache.DATA;
            fillSummary(summary, data);
            fillTrack(track, data, primeTop[0], picker);
            fillTasks(tasks, data);
        });
        return root;
    }

    /* ------------------------------------------------------ summary ------------------------------------------------ */

    private static void fillSummary(Label summary, CompoundTag data) {
        long points = data.getLong("points");
        int ppl = Math.max(1, data.getInt("ppl"));
        int pplPrime = Math.max(1, data.getInt("pplPrime"));
        int free = (int) Math.min(BattlePassConfig.FREE_LEVELS, points / ppl);
        int prime = (int) Math.min(BattlePassConfig.PRIME_LEVELS, points / pplPrime);
        long next = free >= BattlePassConfig.FREE_LEVELS ? 0 : (long) (free + 1) * ppl - points;
        summary.setText(Component.translatable("gui.roll_mod.battlepass.summary", data.getInt("season"), points,
                free, BattlePassConfig.FREE_LEVELS, next, prime, BattlePassConfig.PRIME_LEVELS)
                .withStyle(ChatFormatting.GRAY));
    }

    /* ------------------------------------------------------- track ------------------------------------------------- */

    /** Opens the picker for a choice reward: which track and level, and its options. */
    interface ChoicePicker {
        void open(boolean prime, int level, List<String> options);
    }

    private static void fillTrack(ScrollerView track, CompoundTag data, boolean primeTop, ChoicePicker picker) {
        track.clearAllScrollViewChildren();
        UIElement canvas = new UIElement();
        int width = BattlePassConfig.FREE_LEVELS * COL;
        canvas.layout(l -> l.width(width).height(CANVAS_H));
        // Each row's middle; the boxes are centred on it.
        int freeMid = TOP + (primeTop ? ROW + 6 : 0) + ROW / 2;
        int primeMid = TOP + (primeTop ? 0 : ROW + 6) + ROW / 2;

        long points = data.getLong("points");
        int ppl = Math.max(1, data.getInt("ppl"));
        int pplPrime = Math.max(1, data.getInt("pplPrime"));
        long[] claimedFree = data.getLongArray("claimedFree");
        long claimedPrime = data.getLong("claimedPrime");
        boolean isPrime = data.getBoolean("prime");
        int open = data.getInt("open");
        List<String> free = strings(data, "free");
        List<String> prime = strings(data, "primeRewards");

        // Between each pair of boxes a bar filled as far as the points go towards the next level, with what is still
        // needed written on it.
        for (int k = 1; k < BattlePassConfig.FREE_LEVELS; k++) {
            int left = (k - 1) * COL + COL / 2 + size(k, false) / 2;
            int right = k * COL + COL / 2 - size(k + 1, false) / 2;
            canvas.addChild(segment(left, right, freeMid, points - (long) k * ppl, ppl, false));
        }
        for (int k = 1; k < BattlePassConfig.PRIME_LEVELS; k++) {
            int left = primeX(k) + size(k, true) / 2;
            int right = primeX(k + 1) - size(k + 1, true) / 2;
            canvas.addChild(segment(left, right, primeMid, points - (long) k * pplPrime, pplPrime, true));
        }

        for (int level = 1; level <= BattlePassConfig.FREE_LEVELS && level <= free.size(); level++) {
            int i = level - 1;
            boolean claimed = claimedFree.length > (i >> 6) && (claimedFree[i >> 6] & (1L << (i & 63))) != 0;
            boolean reached = points >= (long) level * ppl;
            boolean gated = (open & (1 << (i / BattlePassConfig.FREE_PER_BLOCK))) == 0;
            canvas.addChild(node(level, false, free.get(i), i * COL + COL / 2, freeMid, claimed, reached, gated, true,
                    size(level, false), picker));
        }
        // The Prime track spread over the free one's length: its first box under the first free one, its last under the last.
        for (int level = 1; level <= BattlePassConfig.PRIME_LEVELS && level <= prime.size(); level++) {
            int i = level - 1;
            boolean claimed = (claimedPrime & (1L << i)) != 0;
            boolean reached = points >= (long) level * pplPrime;
            boolean gated = (open & (1 << (i / BattlePassConfig.PRIME_PER_BLOCK))) == 0;
            canvas.addChild(node(level, true, prime.get(i), primeX(level), primeMid, claimed, reached, gated, isPrime,
                    size(level, true), picker));
        }
        // A thin line between the two rows, in the 6px gap between them.
        UIElement divider = new UIElement();
        divider.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(0).top(TOP + ROW + 2)
                .width(BattlePassConfig.FREE_LEVELS * COL).height(2));
        divider.style(s -> s.background(IGuiTexture.dynamic(() -> new ColorRectTexture(light() ? 0x40000000 : 0x50FFFFFF))));
        divider.setAllowHitTest(false);
        canvas.addChild(divider);
        // Without Prime the whole row sits under a purple veil, a lock on each reward.
        if (!isPrime) {
            UIElement veil = new UIElement();
            int top = primeMid - ROW / 2;
            veil.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(0).top(top)
                    .width(BattlePassConfig.FREE_LEVELS * COL).height(ROW));
            veil.style(s -> s.background(new ColorRectTexture(0xB0502A80)).zIndex(2));
            veil.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(List.of(
                    Component.translatable("gui.roll_mod.battlepass.prime_only").withStyle(ChatFormatting.LIGHT_PURPLE)),
                    null, null, ItemStack.EMPTY));
            // A lock on every Prime reward, over its box.
            for (int level = 1; level <= BattlePassConfig.PRIME_LEVELS && level <= prime.size(); level++) {
                veil.addChild(lockMark(primeX(level), ROW / 2, PRIME_LOCK));
            }
            canvas.addChild(veil);
        }
        // A block whose quest is not done yet: a dark veil over its columns on both tracks (no lock on it — the only
        // locks on the track are Prime's), and at its left edge a thin gate line with a small lock that names the quest.
        ListTag gates = data.getList("gates", Tag.TAG_STRING);
        for (int b = 0; b < BattlePassConfig.BLOCKS; b++) {
            if ((open & (1 << b)) != 0) continue;
            int bx = b * BattlePassConfig.FREE_PER_BLOCK * COL;
            int bw = BattlePassConfig.FREE_PER_BLOCK * COL;
            long quest = b < gates.size() ? parseQuest(gates.getString(b)) : 0;
            List<Component> tip = quest == 0
                    ? List.of(Component.translatable("gui.roll_mod.battlepass.gated").withStyle(ChatFormatting.GRAY))
                    : List.of(Component.translatable("gui.roll_mod.battlepass.gate_quest",
                            QuestGate.title(true, quest)).withStyle(ChatFormatting.GRAY));
            UIElement veil = new UIElement();
            veil.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(bx).top(0).width(bw).height(CANVAS_H));
            veil.style(s2 -> s2.background(new ColorRectTexture(0xA0000000)).zIndex(2));
            veil.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(tip, null, null, ItemStack.EMPTY));
            canvas.addChild(veil);
            canvas.addChild(gate(bx, tip));
        }
        track.addScrollViewChild(canvas);
    }

    /** A veil over one block of a track: {@code fill} with a lock in its middle, and why on hover. */
    /** A box's side: the first reward of each chapter (block) and the track's last one are big, the rest regular. */
    private static int size(int level, boolean prime) {
        int perBlock = prime ? BattlePassConfig.PRIME_PER_BLOCK : BattlePassConfig.FREE_PER_BLOCK;
        int last = prime ? BattlePassConfig.PRIME_LEVELS : BattlePassConfig.FREE_LEVELS;
        return (level - 1) % perBlock == 0 || level == last ? BIG : BOX;
    }

    private static UIElement veil(int x, int y, int width, int fill, int lockColor, String tipKey) {
        return veil(x, y, width, ROW, fill, lockColor, tipKey);
    }

    private static UIElement veil(int x, int y, int width, int height, int fill, int lockColor, String tipKey) {
        UIElement veil = new UIElement();
        veil.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(x).top(y).width(width).height(height));
        veil.style(s -> s.background(new ColorRectTexture(fill)).zIndex(2));
        veil.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(List.of(
                Component.translatable(tipKey).withStyle(ChatFormatting.GRAY)), null, null, ItemStack.EMPTY));
        if (lockColor != 0) veil.addChild(lockMark(width / 2, height / 2, lockColor));
        return veil;
    }

    /** A 24px lock centred on ({@code cx}, {@code cy}) of its parent, in {@code color}. */
    /** A block's gate: a thin line down both tracks at {@code x} and a small lock between the rows, naming the quest. */
    private static UIElement gate(int x, List<Component> tip) {
        int hit = 16;
        UIElement gate = new UIElement();
        gate.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(x - hit / 2).top(0).width(hit).height(CANVAS_H));
        gate.style(s -> s.zIndex(3));
        UIElement line = new UIElement();
        line.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(hit / 2 - 1).top(0).width(2).height(CANVAS_H));
        line.style(s -> s.background(HubIcons.snapped(new ColorRectTexture(0xFFBFBFC8))));
        line.setAllowHitTest(false);
        // The lock at its own 12px, on a dark plate, in the gap between the rows.
        UIElement plate = new UIElement();
        plate.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(0).top(TOP + ROW + 3 - hit / 2).width(hit).height(hit));
        plate.style(s -> s.background(HubIcons.snapped(new GuiTextureGroup(new ColorRectTexture(0xFF1E1E24),
                new ColorBorderTexture(1, 0xFFBFBFC8)))));
        plate.setAllowHitTest(false);
        UIElement lock = new UIElement();
        lock.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(2).top(2).width(12).height(12));
        lock.style(s -> s.background(HubIcons.snapped(SpriteTexture.of(RollMod.id("textures/gui/hub/icons/lock.png")))));
        lock.setAllowHitTest(false);
        plate.addChild(lock);
        gate.addChildren(line, plate);
        gate.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(tip, null, null, ItemStack.EMPTY));
        return gate;
    }

    private static UIElement lockMark(int cx, int cy, int color) {
        UIElement mark = new UIElement();
        mark.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(cx - 12).top(cy - 12).width(24).height(24));
        mark.style(s -> s.background(SpriteTexture.of(RollMod.id("textures/gui/hub/icons/lock.png")).setColor(color)));
        mark.setAllowHitTest(false);
        return mark;
    }

    /**
     * A progress bar from {@code left} to {@code right} on row middle {@code mid}: {@code into} points of the
     * {@code cost} the next level takes are filled in; the number is what is still needed (none once it is full).
     */
    private static UIElement segment(int left, int right, int mid, long into, int cost, boolean prime) {
        int width = Math.max(4, right - left);
        float fraction = Math.max(0, Math.min(1, into / (float) cost));
        UIElement bar = new UIElement();
        bar.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(left).top(mid - 5).width(width).height(10));
        int fill = prime ? 0xFFB45CFF : 0xFFE0B040;
        bar.style(s -> s.background(new IGuiTexture() {
            @Override
            public void draw(net.minecraft.client.gui.GuiGraphics g, float mx, float my, float x, float y, float w, float h,
                             float pt) {
                int x0 = (int) x, y0 = (int) y, x1 = (int) (x + w), y1 = (int) (y + h);
                g.fill(x0, y0, x1, y1, surface(0xFF1E1E24, 0.86f));
                int f = x0 + Math.round((x1 - x0) * fraction);
                if (f > x0) g.fill(x0, y0, f, y1, fill);
            }
        }));
        bar.setAllowHitTest(false);
        if (fraction < 1) {
            // How far into this level the points are, out of what it costs: "0/100", "40/100".
            long done = Math.max(0, Math.min(cost, into));
            Label number = new Label();
            number.setText(Component.literal(done + "/" + cost));
            number.layout(l -> l.widthPercent(100).heightPercent(100));
            number.textStyle(t -> t.textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.CENTER).fontSize(7));
            bar.addChild(number);
        }
        return bar;
    }

    private static UIElement line(int x, int y, int width) {
        UIElement line = new UIElement();
        line.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(x).top(y - 1).width(width).height(2));
        line.style(s -> s.background(new ColorRectTexture(0x80FFFFFF)));
        line.setAllowHitTest(false);
        return line;
    }

    /** How long a choice reward shows each of its options before the next. */
    private static final long CHOICE_CYCLE_MS = 1500;

    /** The option a choice of {@code count} options shows right now; all boxes turn together. */
    private static int choiceIndex(int count) {
        return (int) ((System.currentTimeMillis() / CHOICE_CYCLE_MS) % count);
    }

    /** One reward's icon: its item, or the starcoin for money; faded once claimed. */
    private static IGuiTexture optionTexture(String spec, boolean claimed) {
        ItemStack stack = BattlePassReward.stack(spec);
        IGuiTexture texture = stack.isEmpty() ? MONEY.texture : new ItemStackTexture(stack.copyWithCount(1));
        return claimed ? texture.copy().setColor(0x80FFFFFF) : texture;
    }

    /** The number under a reward: its item count above one, or its money in short form. */
    private static String countText(String spec) {
        ItemStack stack = BattlePassReward.stack(spec);
        return stack.isEmpty() ? compact(money(spec)) : stack.getCount() > 1 ? String.valueOf(stack.getCount()) : "";
    }

    /** One level's box, centred on ({@code cx}, {@code cy}): opaque, framed by state, clicked to claim when ready. */
    private static UIElement node(int level, boolean prime, String spec, int cx, int cy, boolean claimed,
                                  boolean reached, boolean gated, boolean allowed, int size, ChoicePicker picker) {
        List<String> options = BattlePassReward.options(spec);
        boolean choice = BattlePassReward.kind(spec) == BattlePassReward.Kind.CHOICE;
        // A choice shows its first option, marked; a skin shows the skins tab's icon.
        String shown = choice && !options.isEmpty() ? options.get(0) : spec;
        boolean ready = allowed && reached && !claimed && !gated;
        int rim = claimed ? 0xFF3F8F3F : ready ? 0xFFFFC040 : reached && gated ? 0xFFB04040
                : prime ? 0xFF8A5AC0 : 0xFF9A9AA6;
        int fill = claimed ? 0xFF243824 : reached && allowed ? 0xFF2A2A32 : 0xFF3A3A44;
        // On a light theme the text turns dark (LightText), so the box goes light under it.
        int lightFill = claimed ? 0xFFCFE6CF : 0;
        UIElement box = new UIElement();
        box.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(cx - size / 2).top(cy - size / 2)
                .width(size).height(size));
        box.style(s -> s.background(IGuiTexture.dynamic(() -> new GuiTextureGroup(
                new ColorRectTexture(ready && box.isHover() ? (light() ? 0xFFF0E2A8 : 0xFF4A4630)
                        : light() ? (lightFill != 0 ? lightFill : surface(fill, 1f)) : fill),
                new ColorBorderTexture(1, rim)))).zIndex(1));
        // A whole multiple of the 16px item art (32 in a regular box, 48 in a big one), never a fractional scale.
        int iconSize = (size - 4) / 16 * 16;
        ItemStack stack = BattlePassReward.stack(shown);
        boolean skin = BattlePassReward.kind(shown) == BattlePassReward.Kind.SKIN;
        UIElement icon = new UIElement();
        icon.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left((size - iconSize) / 2).top((size - iconSize) / 2)
                .width(iconSize).height(iconSize));
        if (skin) {
            // The skin itself; a random one (a case) cycles through every skin it could be.
            var def = BattlePassReward.skin(shown);
            UIElement picture = def != null ? SkinDollElement.Icon.skin(def, false, () -> false)
                    : new SkinDollElement.Cycle(new ArrayList<>(com.roll_54.roll_mod.cosmetics.ItemSkinRegistry.all()));
            picture.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left((size - iconSize) / 2).top((size - iconSize) / 2)
                    .width(iconSize).height(iconSize));
            picture.setAllowHitTest(false);
            box.addChild(picture);
        } else if (choice && options.size() > 1) {
            // A choice cycles through its options, one at a time ({@link #CHOICE_CYCLE_MS} each).
            List<IGuiTexture> textures = new ArrayList<>();
            for (String option : options) textures.add(optionTexture(option, claimed));
            icon.style(s -> s.background(IGuiTexture.dynamic(() -> textures.get(choiceIndex(options.size())))));
            icon.setAllowHitTest(false);
            box.addChild(icon);
        } else {
            IGuiTexture texture = optionTexture(shown, claimed);
            icon.style(s -> s.background(texture));
            icon.setAllowHitTest(false);
            box.addChild(icon);
        }
        if (choice && !options.isEmpty()) {
            // A choice: "2/3" in the corner — which of its options is showing; one is picked when claiming.
            Label mark = new Label();
            mark.setText(Component.literal("1/" + options.size()).withStyle(ChatFormatting.AQUA));
            mark.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(3).top(3).width(size - 6).height(8));
            mark.textStyle(t -> t.textAlignHorizontal(Horizontal.RIGHT));
            mark.setAllowHitTest(false);
            box.addChild(mark);
            Label count = new Label();
            count.setText(Component.literal(countText(options.get(0))));
            count.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(0).top(size - 10).width(size - 3).height(8));
            count.textStyle(t -> t.textAlignHorizontal(Horizontal.RIGHT));
            count.setAllowHitTest(false);
            box.addChild(count);
            box.addEventListener(UIEvents.TICK, e -> {
                int i = choiceIndex(options.size());
                Component markText = Component.literal((i + 1) + "/" + options.size()).withStyle(ChatFormatting.AQUA);
                if (!markText.equals(mark.getText())) mark.setText(markText);
                Component countValue = Component.literal(countText(options.get(i)));
                if (!countValue.equals(count.getText())) count.setText(countValue);
            });
        } else {
            String countText = skin ? "" : countText(shown);
            if (!countText.isEmpty()) {
                Label count = new Label();
                count.setText(Component.literal(countText));
                count.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(0).top(size - 10).width(size - 3).height(8));
                count.textStyle(t -> t.textAlignHorizontal(Horizontal.RIGHT));
                count.setAllowHitTest(false);
                box.addChild(count);
            }
        }
        if (claimed) {
            Label mark = new Label();
            mark.setText(Component.literal("✔").withStyle(ChatFormatting.GREEN));
            mark.layout(l -> l.positionType(TaffyPosition.ABSOLUTE).left(3).top(3).width(10).height(9));
            mark.setAllowHitTest(false);
            box.addChild(mark);
        }
        List<Component> tip = new ArrayList<>();
        tip.add(Component.translatable(prime ? "gui.roll_mod.battlepass.level.prime" : "gui.roll_mod.battlepass.level",
                level).withStyle(prime ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.WHITE));
        if (choice) {
            tip.add(BattlePassReward.describe(spec));
            for (String option : options) tip.add(Component.literal(" • ").append(BattlePassReward.describe(option)));
        } else {
            tip.add(BattlePassReward.describe(spec));
        }
        if (!allowed) tip.add(Component.translatable("gui.roll_mod.battlepass.prime_only").withStyle(ChatFormatting.LIGHT_PURPLE));
        else if (claimed) tip.add(Component.translatable("gui.roll_mod.battlepass.claimed").withStyle(ChatFormatting.GREEN));
        else if (gated) tip.add(Component.translatable("gui.roll_mod.battlepass.gated").withStyle(ChatFormatting.RED));
        else if (ready) tip.add(Component.translatable("gui.roll_mod.battlepass.ready").withStyle(ChatFormatting.GOLD));
        box.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(tip, null, null, ItemStack.EMPTY));
        if (ready) {
            box.addEventListener(UIEvents.CLICK, e -> {
                if (choice) {
                    picker.open(prime, level, options);
                    return;
                }
                CompoundTag args = new CompoundTag();
                args.putBoolean("prime", prime);
                args.putInt("level", level);
                PacketDistributor.sendToServer(new BattlePassPackets.Act("claim", args));
            });
        }
        return box;
    }

    /* ------------------------------------------------------- tasks ------------------------------------------------- */

    private static void fillTasks(ScrollerView list, CompoundTag data) {
        fillTasks(list, data, false);
    }

    /**
     * The battle pass tasks as rows like the daily tasks': the weekly ones ({@code seasonal} false) or the season's.
     * Shared with the Daily Tasks tab's Weekly and Seasonal pages.
     */
    /** For the Daily Tasks tab's Weekly and Seasonal pages. */
    public static void fillTasksFor(ScrollerView list, CompoundTag data, boolean seasonal) {
        fillTasks(list, data, seasonal);
    }

    static void fillTasks(ScrollerView list, CompoundTag data, boolean seasonal) {
        list.clearAllScrollViewChildren();
        // No note above the rows any more (gui.roll_mod.battlepass.daily_note / season_note, kept in the lang files):
        // each row's tooltip says what to do, and its right side what it pays.
        ListTag tasks = data.getList(seasonal ? "seasonal" : "tasks", Tag.TAG_COMPOUND);
        if (tasks.isEmpty()) {
            Label none = new Label();
            none.setText(Component.translatable("gui.roll_mod.battlepass.no_tasks").withStyle(ChatFormatting.GRAY));
            none.layout(l -> l.widthPercent(100));
            list.addScrollViewChild(none);
            return;
        }
        // Easy ones first, then the hard ones, the Prime ones at the very bottom (each batch's own order within a kind).
        List<CompoundTag> sorted = new java.util.ArrayList<>();
        for (int i = 0; i < tasks.size(); i++) sorted.add(tasks.getCompound(i));
        sorted.sort(java.util.Comparator.comparingInt(c -> switch (c.getString("kind")) {
            case "HARD" -> 1;
            case "PRIME" -> 2;
            default -> 0;
        }));
        for (CompoundTag t : sorted) {
            int taskIndex = t.getInt("task");
            DailyTask task = taskIndex >= 0 ? DailyTaskRegistry.byIndex(taskIndex) : null;
            long quest = parseQuest(t.getString("quest"));
            String kind = t.getString("kind");
            boolean done = t.getBoolean("done");
            int progress = t.getInt("progress");
            int required = Math.max(1, t.getInt("required"));
            int points = switch (kind) {
                case "HARD" -> data.getInt("pHard");
                case "PRIME" -> data.getInt("pPrime");
                case "SEASON" -> data.getInt("pSeason");
                default -> data.getInt("pEasy");
            };
            // A daily task's row: its icon, the name over a progress bar, and what it pays on the right.
            UIElement row = new UIElement();
            row.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(44).marginBottom(3)
                    .paddingAll(4).paddingHorizontal(6).gapColumn(8).alignItems(AlignItems.CENTER));
            int fill = switch (kind) {
                case "HARD" -> 0x50904020;
                case "PRIME" -> 0x506A2C91;
                case "SEASON" -> 0x50206060;
                default -> 0x40000000;
            };
            row.style(s -> s.background(new ColorRectTexture(fill)));
            // What exactly to do, on hover: the task's own description with its amount, or the quest to finish.
            List<Component> what = new ArrayList<>();
            if (task != null) {
                what.add(task.name().copy().withStyle(ChatFormatting.YELLOW));
                what.add(task.tooltip(required).copy().withStyle(ChatFormatting.GRAY));
            } else if (quest != 0) {
                what.add(QuestGate.title(true, quest).copy().withStyle(ChatFormatting.YELLOW));
                what.add(Component.translatable("gui.roll_mod.battlepass.quest_tip").withStyle(ChatFormatting.GRAY));
            }
            what.add(Component.translatable("gui.roll_mod.battlepass.progress_tip", Math.min(progress, required), required)
                    .withStyle(ChatFormatting.DARK_GRAY));
            row.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(what, null, null, ItemStack.EMPTY));
            UIElement icon = new UIElement();
            icon.layout(l -> l.width(32).height(32).flexShrink(0));
            // A quest task wears a book.
            if (task != null) icon.style(s -> s.background(task.icon().texture()));
            else if (quest != 0) icon.style(s -> s.background(new ItemStackTexture(net.minecraft.world.item.Items.WRITABLE_BOOK)));
            UIElement middle = new UIElement();
            middle.layout(l -> l.flexDirection(FlexDirection.COLUMN).flexBasis(0).flexGrow(1).gapRow(3));
            Label name = new Label();
            Component title = task != null ? task.name().copy()
                    : quest != 0 ? Component.translatable("gui.roll_mod.battlepass.quest_task", QuestGate.title(true, quest))
                    : Component.literal("?");
            name.setText(title.copy()
                    .withStyle(done ? ChatFormatting.GREEN : ChatFormatting.WHITE));
            name.layout(l -> l.widthPercent(100).height(10));
            name.textStyle(t2 -> t2.textWrap(TextWrap.HIDE));
            com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar bar = new com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar();
            bar.layout(l -> l.widthPercent(100).height(12));
            bar.setRange(0f, 1f);
            // 2px: inside the themes' 2px frame, as the daily rows' bar (at 1px a small fill sat on the frame's edge).
            bar.barContainer(c -> c.layout(l -> l.paddingAll(2)));
            bar.setProgress(done ? 1f : Math.min(1f, progress / (float) required));
            bar.label.setText(Component.literal((done ? required : progress) + " / " + required));
            middle.addChildren(name, bar);
            UIElement right = new UIElement();
            right.layout(l -> l.flexDirection(FlexDirection.COLUMN).width(70).flexShrink(0).alignItems(AlignItems.FLEX_END)
                    .gapRow(3));
            Label tag = new Label();
            tag.setText(Component.translatable("gui.roll_mod.battlepass.kind." + kind.toLowerCase(java.util.Locale.ROOT))
                    .withStyle(kind.equals("HARD") ? ChatFormatting.GOLD : kind.equals("PRIME") ? ChatFormatting.LIGHT_PURPLE
                            : kind.equals("SEASON") ? ChatFormatting.DARK_AQUA : ChatFormatting.AQUA));
            tag.layout(l -> l.height(10));
            tag.textStyle(t2 -> t2.adaptiveWidth(true));
            Label reward = new Label();
            reward.setText(done ? Component.literal("✔").withStyle(ChatFormatting.GREEN)
                    : Component.translatable("gui.roll_mod.battlepass.task_points", points).withStyle(ChatFormatting.YELLOW));
            reward.layout(l -> l.height(POINTS_ICON).paddingTop(1));
            reward.textStyle(t2 -> t2.adaptiveWidth(true).textAlignVertical(Vertical.CENTER));
            // The points with the pass's own icon in front of them.
            UIElement pays = new UIElement();
            pays.layout(l -> l.flexDirection(FlexDirection.ROW).height(POINTS_ICON).gapColumn(2).alignItems(AlignItems.CENTER));
            if (!done) pays.addChild(pointsIcon());
            pays.addChild(reward);
            right.addChildren(tag, pays);
            row.addChildren(icon, middle, right);
            list.addScrollViewChild(row);
        }
    }

    /* ------------------------------------------------------ helpers ------------------------------------------------ */

    /** Whether a light theme is on (client); its text is dark, so this tab's dark fills go light under it. */
    private static boolean light() {
        return net.neoforged.fml.loading.FMLEnvironment.dist.isClient()
                && com.roll_54.roll_mod.minestar.theme.LightText.active();
    }

    /** {@code dark} on a dark theme; on a light one the theme's raised panel colour, shaded by {@code shade}. */
    private static int surface(int dark, float shade) {
        if (!light()) return dark;
        var palette = com.roll_54.roll_mod.minestar.theme.UiThemeState.get().palette();
        if (palette == null) return dark;
        int c = palette.raised();
        int r = (int) (((c >> 16) & 0xFF) * shade), g = (int) (((c >> 8) & 0xFF) * shade), b = (int) ((c & 0xFF) * shade);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    /** The battle-pass points icon: the tab's trophy, at its own 16px. */
    public static final int POINTS_ICON = 16;

    public static UIElement pointsIcon() {
        UIElement icon = new UIElement();
        icon.layout(l -> l.width(POINTS_ICON).height(POINTS_ICON).flexShrink(0));
        icon.style(s -> s.background(SpriteTexture.of(RollMod.id("textures/gui/hub/battlepass.png"))));
        icon.setAllowHitTest(false);
        return icon;
    }

    private static long parseQuest(String hex) {
        if (hex == null || hex.isBlank()) return 0;
        try {
            return Long.parseUnsignedLong(hex, 16);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static List<String> strings(CompoundTag tag, String key) {
        List<String> out = new ArrayList<>();
        ListTag list = tag.getList(key, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) out.add(list.getString(i));
        return out;
    }

    private static long money(String spec) {
        try {
            return spec.startsWith("money:") ? Long.parseLong(spec.substring(6).strip()) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String compact(long amount) {
        return amount >= 1000 ? (amount % 1000 == 0 ? amount / 1000 + "k" : String.format("%.1fk", amount / 1000.0)) : String.valueOf(amount);
    }
}
