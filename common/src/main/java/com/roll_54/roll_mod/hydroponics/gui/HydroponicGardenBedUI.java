package com.roll_54.roll_mod.hydroponics.gui;

import com.roll_54.roll_mod.minestar.theme.RollUi;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.FillDirection;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Slider;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Toggle;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SimpleBinding;
import com.roll_54.roll_mod.blocks.entity.HydroponicGardenBedBlockEntity;
import com.roll_54.roll_mod.economy.vendingblock.gui.VendorUIHelper;
import com.roll_54.roll_mod.hydroponics.HydroponicNode;
import com.roll_54.roll_mod.hydroponics.HydroponicNodeData;
import com.roll_54.roll_mod.hydroponics.HydroponicReagents;
import com.roll_54.roll_mod.hydroponics.HydroponicSettings;
import com.roll_54.roll_mod.registry.BlockRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.function.UnaryOperator;

/**
 * The Hydroponic Garden Bed's GUI, as an LdLib2 element tree.
 *
 * <pre>
 *   +------------------------------------------+
 *   |  [icon]  [====== pH slider ======]       |
 *   |  [icon]  [==== humidity slider ===]      |
 *   |  [icon]  [=== nutrients slider ===]      |
 *   |  Fully supplied            3 / 25 beds   |
 *   |          [A][W][F]           ( Working ) |
 *   |  [== energy ==]  [== water ==]           |
 *   |  [= fertilizer =] [<- acidity ->]        |
 *   |  [ player inventory + hotbar ]           |
 *   +------------------------------------------+
 * </pre>
 *
 * <p>Everything shown belongs to the whole merged node rather than to the bed that was clicked, and
 * the node lives in level {@link HydroponicNodeData SavedData} that only the server can see. So the
 * readouts are server-to-client sync values polled each tick, and the four settings are two-way
 * bindings whose server half runs {@link #apply}.
 *
 * <p><b>This is built on both sides.</b> {@code createUI} runs on the dedicated server too, and
 * LdLib2 matches sync values and slot indices <em>positionally</em>, so nothing here may branch on
 * dist, permissions or player state — the two trees have to come out identical. Every getter is
 * null-safe for the same reason: {@link HydroponicGardenBedBlockEntity#node()} always returns null
 * client-side.
 */
public final class HydroponicGardenBedUI {

    public static final int PANEL_WIDTH = 176;
    public static final int PANEL_HEIGHT = 233;

    private static final int ICON_X = 8;
    private static final int SLIDER_X = 26;
    private static final int SLIDER_WIDTH = 142;
    private static final int ROW_HEIGHT = 24;
    private static final int FIRST_ROW_Y = 18;
    /** Controls sit 12px tall on a 24px row, which centres them against the 12px glyphs. */
    private static final int CONTROL_HEIGHT = 12;
    private static final int CONTROL_INSET = 4;

    /** One line of text. The level name sits on its own line above its slider, not across it. */
    private static final int LABEL_HEIGHT = 9;

    /** The strip above the first row, which nothing else uses: the bed count lives here. */
    private static final int HEADER_Y = 1;
    private static final int LINE_HEIGHT = 10;

    /** Both status lines span the panel's full inner width; neither shares a row any more. */
    private static final int LINE_X = 8;
    private static final int LINE_WIDTH = 160;

    /** 3px below the nutrients slider, which ends at 82: at 82 the line sat right on the bar. */
    private static final int STATUS_Y = 85;
    private static final int SLOT_SIZE = 18;
    private static final int SLOT_Y = 97;
    private static final int SLOT_X_ACIDITY = 26;
    private static final int SLOT_X_WATER = 62;
    private static final int SLOT_X_FERTILIZER = 98;

    private static final int TOGGLE_X = 124;
    private static final int TOGGLE_Y = 99;
    private static final int TOGGLE_WIDTH = 44;
    private static final int TOGGLE_HEIGHT = 14;

    private static final int BAR_WIDTH = 76;
    /** 12, not 10: the bar carries its name now, and a line of text needs 8px inside the 1px rim. */
    private static final int BAR_HEIGHT = 12;
    private static final int BAR_LEFT_X = 8;
    private static final int BAR_RIGHT_X = 92;
    private static final int BAR_TOP_Y = 119;
    private static final int BAR_BOTTOM_Y = 133;

    private static final int INVENTORY_X = 8;
    private static final int INVENTORY_Y = 151;

    // Text, slots, sliders, the toggle and the bar frames take their look from the player's hub
    // theme (RollUi), so nothing here sets a text colour or a control sprite. The colours below
    // are the ones that carry meaning -- which reservoir a bar is, whether the node is supplied --
    // and stay the same in every theme.
    private static final int COLOUR_SUPPLIED = 0xFF4CAF50;
    private static final int COLOUR_STARVED = 0xFFD05050;
    private static final int COLOUR_ENERGY = 0xFFE0B020;
    private static final int COLOUR_WATER = 0xFF3A7BD5;
    private static final int COLOUR_FERTILIZER = 0xFF4CAF50;
    private static final int COLOUR_ACID = 0xFFD05050;
    private static final int COLOUR_BASE = 0xFF7B68D5;

    private HydroponicGardenBedUI() {
    }

    public static ModularUI build(HydroponicGardenBedBlockEntity bed, Player player) {
        BlockPos pos = bed.getBlockPos();

        // styledPanel, not panel: an inline background outranks the stylesheet, so a coloured rect
        // here would hide the gdp plate entirely. paddingAll(0) because `.panel_bg` carries
        // padding-all: 5 and every child below is placed at an absolute coordinate without it.
        UIElement root = VendorUIHelper.styledPanel(PANEL_WIDTH, PANEL_HEIGHT);
        root.layout(l -> l.paddingAll(0));

        settingRow(root, bed, player, pos, 0, AgriSoilGlyphs.Row.ACIDITY,
                HydroponicSettings.PH_MIN, HydroponicSettings.PH_MAX,
                HydroponicSettings::acidityKey, HydroponicSettings::ph, HydroponicSettings::withPh);
        settingRow(root, bed, player, pos, 1, AgriSoilGlyphs.Row.HUMIDITY,
                HydroponicSettings.WATER_MIN, HydroponicSettings.WATER_MAX,
                HydroponicSettings::humidityKey, HydroponicSettings::water,
                HydroponicSettings::withWater);
        settingRow(root, bed, player, pos, 2, AgriSoilGlyphs.Row.NUTRIENTS,
                HydroponicSettings.FERTILITY_MIN, HydroponicSettings.FERTILITY_MAX,
                HydroponicSettings::nutrientsKey, HydroponicSettings::fertility,
                HydroponicSettings::withFertility);

        statusLine(root, bed);
        nodeSizeLine(root, bed);
        enableToggle(root, bed, player, pos);
        inputSlots(root, bed);
        buffers(root, bed);

        root.addChild(VendorUIHelper.playerInventory(INVENTORY_X, INVENTORY_Y));

        // The player's theme (RollUi). Its MC choice is the old mc-then-gdp pair: mc for the
        // controls it styles (gdp defines none of them), gdp last so its panel rules win the tie.
        return ModularUI.of(
                RollUi.of(player, root), player);
    }

    // ------------------------------------------------------------------ settings

    /**
     * One glyph + slider + label row, wired two ways.
     *
     * <p>The label is a sibling laid over the slider rather than a child of it: a child would be
     * placed by the slider's own flex box, and it carries {@code setAllowHitTest(false)} so it never
     * swallows a drag.
     */
    private static void settingRow(UIElement root, HydroponicGardenBedBlockEntity bed, Player player,
                                   BlockPos pos, int index, AgriSoilGlyphs.Row row, int min, int max,
                                   IntFunction<String> labelKey,
                                   ToIntFunction<HydroponicSettings> getter,
                                   SettingEdit edit) {
        int y = FIRST_ROW_Y + index * ROW_HEIGHT + CONTROL_INSET;
        int initial = getter.applyAsInt(settings(bed));

        UIElement glyph = new UIElement();
        root.addChild(VendorUIHelper.abs(glyph, ICON_X, y, AgriSoilGlyphs.HEIGHT, AgriSoilGlyphs.HEIGHT));

        Slider.Horizontal slider = new Slider.Horizontal();
        slider.setRange(min, max);
        // Only the scroll wheel reads this; drag and track-click are snapped by hand below.
        slider.sliderStyle(style -> style.sliderStep(1f));
        root.addChild(VendorUIHelper.abs(slider, SLIDER_X, y, SLIDER_WIDTH, CONTROL_HEIGHT));

        Label label = new Label();
        label.textStyle(text -> text.textAlignHorizontal(Horizontal.CENTER)
                .adaptiveWidth(false).textWrap(TextWrap.HIDE));
        label.setAllowHitTest(false);
        // Above the bar, not across it: the handle used to sit on top of its own level name. The
        // line fits in the gap the row already had above its control, so no row grows.
        root.addChild(VendorUIHelper.abs(label, SLIDER_X, y - LABEL_HEIGHT - 1, SLIDER_WIDTH, LABEL_HEIGHT));

        // Repaints the glyph and the level name for a step. Not a listener: it has to run on the
        // seed too, and `setValue(v, false)` deliberately does not notify.
        IntConsumer repaint = step -> {
            label.setText(Component.translatable(labelKey.apply(step)));
            glyph.style(s -> s.backgroundTexture(AgriSoilGlyphs.glyph(row, step - 1)));
        };

        slider.setOnValueChanged(value -> {
            int step = Math.round(value);
            // Slider.setValue only clamps; nothing in LdLib2 snaps a drag or a track click.
            if (value != step) {
                slider.setValue((float) step, false);
            }
            repaint.accept(step);
        });
        slider.setValue((float) initial, false);
        repaint.accept(initial);

        SimpleBinding<Float> binding = DataBindingBuilder
                .floatVal(() -> (float) getter.applyAsInt(settings(bed)),
                        value -> apply(player, pos, current -> edit.apply(current, Math.round(value))))
                .remoteGetter(slider::getValue)
                // Another player editing the same node must not yank the handle out from under a
                // drag in progress; the next poll sends our value anyway once the drag ends.
                .remoteSetter(value -> {
                    if (!slider.isDragging()) {
                        int step = Math.round(value);
                        slider.setValue((float) step, false);
                        repaint.accept(step);
                    }
                })
                .initialValue((float) initial)
                .name("hydroponic_setting_" + index)
                .build();
        slider.bind(binding);
    }

    private static void enableToggle(UIElement root, HydroponicGardenBedBlockEntity bed,
                                     Player player, BlockPos pos) {
        boolean initial = settings(bed).enabled();

        Toggle toggle = new Toggle();
        // No toggleStyle: the socket and the on-mark are the theme's, like every other toggle the
        // player sees in the hub.
        // 44px box minus the 12px square leaves ~28px of text, and "Stopped" alone is 40px, so
        // without a clamp the word ran off the panel edge. The mark colour carries the state; the
        // word is the confirmation.
        toggle.toggleLabel(label -> label.textStyle(
                text -> text.adaptiveWidth(false).textWrap(TextWrap.HIDE)));
        // setOn(value) would notify, firing the change listener as the GUI opens and bouncing a
        // spurious write straight back at the server.
        toggle.setOn(initial, false);
        toggle.setText(toggleLabel(initial));
        toggle.setOnToggleChanged(on -> toggle.setText(toggleLabel(on)));
        root.addChild(VendorUIHelper.abs(toggle, TOGGLE_X, TOGGLE_Y, TOGGLE_WIDTH, TOGGLE_HEIGHT));

        SimpleBinding<Boolean> binding = DataBindingBuilder
                .bool(() -> settings(bed).enabled(),
                        value -> apply(player, pos, current -> current.withEnabled(value)))
                .remoteGetter(toggle::isOn)
                .remoteSetter(value -> {
                    toggle.setOn(value, false);
                    toggle.setText(toggleLabel(value));
                })
                .initialValue(initial)
                .name("hydroponic_enabled")
                .build();
        toggle.bind(binding);
    }

    private static Component toggleLabel(boolean enabled) {
        return Component.translatable(enabled
                ? "gui.roll_mod.hydroponic.working"
                : "gui.roll_mod.hydroponic.stopped");
    }

    /** What a setting edit does to the record; the record's own constructor clamps the result. */
    @FunctionalInterface
    private interface SettingEdit {
        HydroponicSettings apply(HydroponicSettings current, int value);
    }

    /**
     * Applies a settings edit on the server, replacing the gate the old settings packet carried.
     *
     * <p>That packet checked that the player had this very bed's menu open, and leaned on the menu's
     * {@code stillValid} for distance. The binding channel already refuses to write into a UI the
     * player does not have open and puts no {@link BlockPos} on the wire at all, so what is left to
     * check here is that the bed is still a bed, still in reach, and still part of a node — the same
     * three things that could have changed since the GUI opened.
     */
    private static void apply(Player player, BlockPos pos, UnaryOperator<HydroponicSettings> edit) {
        if (!(player instanceof ServerPlayer serverPlayer)
                || !(serverPlayer.level() instanceof ServerLevel level)) {
            return;
        }
        if (!serverPlayer.canInteractWithBlock(pos, 4.0D)
                || !level.getBlockState(pos).is(BlockRegistry.HYDROPONIC_GARDEN_BED.get())) {
            return;
        }
        HydroponicNodeData data = HydroponicNodeData.get(level);
        HydroponicNode node = data.nodeAt(pos);
        if (node == null) {
            return;
        }
        HydroponicSettings current = node.settings();
        HydroponicSettings updated = edit.apply(current);
        if (updated.equals(current)) {
            return;
        }
        node.setSettings(updated);
        data.setDirty();
        // setSettings deliberately does not touch the other members; pushing the shared state onto
        // them is the caller's job, and every other bed in the node is showing it too.
        HydroponicNodeData.syncMembers(level, node);
    }

    // ------------------------------------------------------------------ readouts

    /**
     * Supplied / starved. The only readout that needs no sync value: the bed already ships this
     * flag to clients in its update tag, because AgriCraft asks for the soil on both sides.
     */
    private static void statusLine(UIElement root, HydroponicGardenBedBlockEntity bed) {
        Label status = new Label();
        // Full width, and clamped: this line used to stop at x 112 where the bed count began, and
        // every one of its strings is wider than that -- English by ~50px, Ukrainian by more -- so
        // it ran straight through its neighbour. The bed count has moved to the header strip.
        status.textStyle(text -> text.textShadow(false)
                .adaptiveWidth(false).textWrap(TextWrap.HIDE));
        status.addEventListener(UIEvents.TICK, event -> {
            boolean supplied = bed.isSupplied();
            status.setText(Component.translatable(supplied
                    ? "gui.roll_mod.hydroponic.supplied"
                    : "gui.roll_mod.hydroponic.starved"));
            status.textStyle(text -> text.textColor(supplied ? COLOUR_SUPPLIED : COLOUR_STARVED));
        });
        root.addChild(VendorUIHelper.abs(status, LINE_X, STATUS_Y, LINE_WIDTH, LINE_HEIGHT));

        // Why it is starved. The buffers are server-only, so the reasons come over as a bitmask
        // from the same HydroponicNode#shortfall that gates the real upkeep.
        SimpleBinding<Integer> shortfall = DataBindingBuilder
                .intValS2C(() -> {
                    HydroponicNode node = bed.node();
                    return node == null ? 0 : node.shortfall();
                })
                .initialValue(0)
                .name("hydroponic_shortfall")
                .build();
        root.addSyncValue(shortfall.getSyncValue());
        status.addEventListener(UIEvents.HOVER_TOOLTIPS, event -> {
            if (bed.isSupplied()) {
                return;
            }
            event.hoverTooltips = new HoverTooltips(
                    shortfallLines(value(shortfall, 0), settings(bed)), null, null, null);
            event.stopPropagation();
        });
    }

    private static List<Component> shortfallLines(int mask, HydroponicSettings settings) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.roll_mod.hydroponic.shortfall"));
        String prefix = "gui.roll_mod.hydroponic.shortfall.";
        if ((mask & HydroponicNode.SHORT_DISABLED) != 0) {
            lines.add(Component.translatable(prefix + "disabled").withStyle(ChatFormatting.RED));
        }
        if ((mask & HydroponicNode.SHORT_ENERGY) != 0) {
            lines.add(Component.translatable(prefix + "energy", HydroponicSettings.ENERGY_COST)
                    .withStyle(ChatFormatting.RED));
        }
        if ((mask & HydroponicNode.SHORT_WATER) != 0) {
            lines.add(Component.translatable(prefix + "water", settings.waterCost())
                    .withStyle(ChatFormatting.RED));
        }
        if ((mask & HydroponicNode.SHORT_FERTILIZER) != 0) {
            lines.add(Component.translatable(prefix + "fertilizer", settings.fertilizerCost())
                    .withStyle(ChatFormatting.RED));
        }
        if ((mask & HydroponicNode.SHORT_ACID) != 0) {
            lines.add(Component.translatable(prefix + "acid", Math.abs(settings.acidityDelta()))
                    .withStyle(ChatFormatting.RED));
        }
        if ((mask & HydroponicNode.SHORT_BASE) != 0) {
            lines.add(Component.translatable(prefix + "base", Math.abs(settings.acidityDelta()))
                    .withStyle(ChatFormatting.RED));
        }
        // Nothing is short right now, yet this bed went without: the node had enough for some of
        // its beds but ran dry before reaching this one.
        if (mask == 0) {
            lines.add(Component.translatable(prefix + "partial").withStyle(ChatFormatting.GOLD));
        }
        return lines;
    }

    private static void nodeSizeLine(UIElement root, HydroponicGardenBedBlockEntity bed) {
        SimpleBinding<Integer> size = DataBindingBuilder
                .intValS2C(() -> {
                    HydroponicNode node = bed.node();
                    return node == null ? 1 : node.size();
                })
                .initialValue(1)
                .name("hydroponic_node_size")
                .build();
        root.addSyncValue(size.getSyncValue());

        Label label = new Label();
        label.textStyle(text -> text.textAlignHorizontal(Horizontal.RIGHT)
                .adaptiveWidth(false).textWrap(TextWrap.HIDE));
        label.addEventListener(UIEvents.TICK, event -> label.setText(Component.translatable(
                "gui.roll_mod.hydroponic.node_size",
                value(size, 1), HydroponicNode.MAX_MEMBERS)));
        // The header strip, which nothing else occupies. Right-aligned across the full width, so it
        // reads as a corner caption and leaves the status line below it a whole row to itself.
        root.addChild(VendorUIHelper.abs(label, LINE_X, HEADER_Y, LINE_WIDTH, LINE_HEIGHT));
    }

    private static void inputSlots(UIElement root, HydroponicGardenBedBlockEntity bed) {
        // Fixed order, because the slots land in the ModularUI's real container menu and both sides
        // have to agree on their indices.
        slot(root, bed, HydroponicReagents.Kind.ACIDITY, SLOT_X_ACIDITY);
        slot(root, bed, HydroponicReagents.Kind.WATER, SLOT_X_WATER);
        slot(root, bed, HydroponicReagents.Kind.FERTILIZER, SLOT_X_FERTILIZER);
    }

    /**
     * {@link HydroponicInputSlot} is kept rather than swapped for LdLib2's {@code ItemHandlerSlot}:
     * only it carries the per-kind reagent filter and the one-container-per-slot cap, and only it
     * reads the node's {@code getStackLimit} rather than the plain 64 of {@code getSlotLimit}.
     */
    private static void slot(UIElement root, HydroponicGardenBedBlockEntity bed,
                             HydroponicReagents.Kind kind, int x) {
        ItemSlot slot = new ItemSlot().bind(new HydroponicInputSlot(bed.inputs(), kind, 0, 0));
        root.addChild(VendorUIHelper.abs(slot, x, SLOT_Y, SLOT_SIZE, SLOT_SIZE));
    }

    private static void buffers(UIElement root, HydroponicGardenBedBlockEntity bed) {
        bufferBar(root, bed, BAR_LEFT_X, BAR_TOP_Y, COLOUR_ENERGY, "energy", "EU",
                node -> node.energy(), node -> node.energyCapacity());
        bufferBar(root, bed, BAR_RIGHT_X, BAR_TOP_Y, COLOUR_WATER, "water", "mB",
                node -> node.water(), node -> node.waterCapacity());
        bufferBar(root, bed, BAR_LEFT_X, BAR_BOTTOM_Y, COLOUR_FERTILIZER, "fertilizer", "mB",
                node -> node.fertilizer(), node -> node.fertilizerCapacity());
        acidityBar(root, bed, BAR_RIGHT_X, BAR_BOTTOM_Y);
    }

    private static void bufferBar(UIElement root, HydroponicGardenBedBlockEntity bed, int x, int y,
                                  int colour, String key, String unit,
                                  NodeLong amount, NodeLong capacity) {
        SimpleBinding<Long> value = longBinding(bed, amount, key + "_amount");
        SimpleBinding<Long> cap = longBinding(bed, capacity, key + "_capacity");
        root.addSyncValue(value.getSyncValue());
        root.addSyncValue(cap.getSyncValue());

        ProgressBar bar = bar(colour);
        bar.label(label -> label.setText(Component.translatable("gui.roll_mod.hydroponic." + key)));
        bar.addEventListener(UIEvents.TICK, event -> {
            long held = value(value, 0L);
            long total = value(cap, 0L);
            bar.setProgress(total > 0 ? (float) ((double) held / total) : 0f);
        });
        tooltip(bar, () -> List.of(
                Component.translatable("gui.roll_mod.hydroponic." + key),
                Component.literal(value(value, 0L) + " / " + value(cap, 0L) + " " + unit)));
        root.addChild(VendorUIHelper.abs(bar, x, y, BAR_WIDTH, BAR_HEIGHT));
    }

    /**
     * The acidity reservoir is signed — stored acid above zero, stored base below — so it reads as
     * two bars meeting in the middle, filling outward: acid to the right, base to the left.
     */
    private static void acidityBar(UIElement root, HydroponicGardenBedBlockEntity bed, int x, int y) {
        SimpleBinding<Integer> acidity = DataBindingBuilder
                .intValS2C(() -> {
                    HydroponicNode node = bed.node();
                    return node == null ? 0 : node.acidity();
                })
                .initialValue(0)
                .name("hydroponic_acidity")
                .build();
        root.addSyncValue(acidity.getSyncValue());

        // No frame of its own: each half draws the theme's bar frame.
        UIElement container = new UIElement();

        int half = BAR_WIDTH / 2;
        ProgressBar base = bar(COLOUR_BASE);
        base.progressBarStyle(style -> style.fillDirection(FillDirection.RIGHT_TO_LEFT));
        container.addChild(VendorUIHelper.abs(base, 0, 0, half, BAR_HEIGHT));

        ProgressBar acid = bar(COLOUR_ACID);
        container.addChild(VendorUIHelper.abs(acid, half, 0, BAR_WIDTH - half, BAR_HEIGHT));

        // One name across both halves, laid over them; each half's own label stays blank.
        Label name = barLabel();
        name.setText(Component.translatable("gui.roll_mod.hydroponic.acidity.short"));
        name.setAllowHitTest(false);
        container.addChild(VendorUIHelper.abs(name, 0, 0, BAR_WIDTH, BAR_HEIGHT));

        container.addEventListener(UIEvents.TICK, event -> {
            int stored = value(acidity, 0);
            acid.setProgress(Math.max(0, stored) / (float) HydroponicNode.ACIDITY_RANGE);
            base.setProgress(Math.max(0, -stored) / (float) HydroponicNode.ACIDITY_RANGE);
        });
        tooltip(container, () -> {
            int stored = value(acidity, 0);
            return List.of(
                    Component.translatable("gui.roll_mod.hydroponic.acidity"),
                    Component.translatable(stored >= 0
                                    ? "gui.roll_mod.hydroponic.acidity.acid"
                                    : "gui.roll_mod.hydroponic.acidity.base",
                            Math.abs(stored), HydroponicNode.ACIDITY_RANGE));
        });
        root.addChild(VendorUIHelper.abs(container, x, y, BAR_WIDTH, BAR_HEIGHT));
    }

    /**
     * A bar in a colour of its own. {@code paddingAll(1)} is not decoration: {@link ProgressBar}'s
     * constructor pads its container by 4 on every side, which leaves a 10px-tall bar with nothing
     * left to fill.
     *
     * <p>Its built-in label defaults to the literal "Label", so it is blanked here and the caller
     * names the bar if it wants one.
     */
    private static ProgressBar bar(int colour) {
        ProgressBar bar = new ProgressBar();
        bar.setRange(0f, 1f);
        // The frame is the theme's; only the fill colour is ours.
        bar.barContainer(container -> container.layout(l -> l.paddingAll(1)));
        bar.bar(fill -> fill.style(s -> s.backgroundTexture(new ColorRectTexture(colour))));
        bar.label(label -> {
            label.setText(Component.empty());
            styleBarText(label);
        });
        return bar;
    }

    /** A free-standing label in the same style as a bar's own, for laying over a composite bar. */
    private static Label barLabel() {
        Label label = new Label();
        styleBarText(label);
        return label;
    }

    private static void styleBarText(Label label) {
        label.textStyle(text -> text.textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.CENTER)
                .adaptiveWidth(false).textWrap(TextWrap.HIDE));
    }

    /** LdLib2 has no tooltip setter; a hover is an event you answer. */
    private static void tooltip(UIElement element, Supplier<List<Component>> lines) {
        element.addEventListener(UIEvents.HOVER_TOOLTIPS, event -> {
            event.hoverTooltips = new HoverTooltips(lines.get(), null, null, null);
            event.stopPropagation();
        });
    }

    // ------------------------------------------------------------------ plumbing

    /** One buffer figure off the node. */
    @FunctionalInterface
    private interface NodeLong {
        long read(HydroponicNode node);
    }

    private static SimpleBinding<Long> longBinding(HydroponicGardenBedBlockEntity bed,
                                                   NodeLong reader, String name) {
        return DataBindingBuilder
                .longValS2C(() -> {
                    HydroponicNode node = bed.node();
                    return node == null ? 0L : reader.read(node);
                })
                .initialValue(0L)
                .name("hydroponic_" + name)
                .build();
    }

    /**
     * The node's settings if this side can see them, else the snapshot the bed keeps for AgriCraft —
     * which is the only one a client has.
     */
    private static HydroponicSettings settings(HydroponicGardenBedBlockEntity bed) {
        HydroponicNode node = bed.node();
        return node == null ? bed.settings() : node.settings();
    }

    /** A sync value is empty until its first push; every read needs a fallback. */
    private static <T> T value(SimpleBinding<T> binding, T fallback) {
        T current = binding.getSyncValue().getValue();
        return current == null ? fallback : current;
    }
}
