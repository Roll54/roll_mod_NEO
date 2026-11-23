package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.roll_54.roll_mod.minestar.battlepass.ClientBattlePassCache;
import com.roll_54.roll_mod.minestar.dailytasks.DailyTaskRegistry;
import com.roll_54.roll_mod.minestar.dailytasks.TaskSettings;
import com.roll_54.roll_mod.minestar.dailytasks.api.DailyTask;
import com.roll_54.roll_mod.network.packet.BattlePassPackets;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The Moderation tab's task editor: how many main daily tasks a player is dealt, the cups each kind of task pays,
 * what a cup is worth in starcoins once the pass is full, how many seasonal tasks a season deals; then every task with
 * where it may appear (daily, weekly, seasonal) and the amount it asks for; and tasks made here from a hook and a
 * target. Save sends it all; the server checks the ban permission, writes {@code task_settings.json} and the battle
 * pass config, and sends the result to everyone.
 */
final class TasksAdminPanel {

    private static final String[] HOOKS = {"MINE", "KILL", "CRAFT", "SMELT", "FISH", "EAT", "EAT_SPEFIC", "COOK", "BREED",
            "SHEAR", "CLIP", "JUMP", "PLAYTIME", "DISTANCE", "RAID", "PLACE", "TAME", "TRADE", "ENCHANT", "BREW", "ANVIL",
            "ADVANCEMENT", "LEVEL_UP", "USE", "DAMAGE", "VISIT", "STAT", "INTERACT_BLOCK", "INTERACT_ENTITY",
            "RIGHT_CLICK_ITEM", "SLEEP", "HARVEST"};

    /** What a hook's target names, so the card offers the right list to pick it from. */
    private enum TargetKind { ITEM, ENTITY, BLOCK, DIMENSION, STAT, ADVANCEMENT, TEXT, NONE }

    private static TargetKind targetKind(String hook) {
        return switch (hook) {
            case "KILL", "BREED", "SHEAR", "TAME", "DAMAGE", "INTERACT_ENTITY" -> TargetKind.ENTITY;
            case "MINE", "PLACE", "INTERACT_BLOCK", "HARVEST" -> TargetKind.BLOCK;
            case "VISIT" -> TargetKind.DIMENSION;
            case "STAT" -> TargetKind.STAT;
            case "ADVANCEMENT" -> TargetKind.ADVANCEMENT;
            case "CLIP" -> TargetKind.TEXT;
            case "JUMP", "PLAYTIME", "DISTANCE", "RAID", "LEVEL_UP", "SLEEP" -> TargetKind.NONE;
            default -> TargetKind.ITEM;
        };
    }
    private static final String[] TIMES = {"", "day", "night"};
    private static final String[] WEATHERS = {"", "clear", "rain", "thunder"};

    /** One made task as the editor holds it: the settings' own object, with the number fields as typed. */
    private static final class Made {
        final TaskSettings.Made m;
        String amount;
        String weight;
        String mult;

        Made(TaskSettings.Made m) {
            this.m = m;
            this.amount = String.valueOf(m.amount);
            this.weight = String.valueOf(m.weight);
            this.mult = m.teamMultiplier == 0 ? "" : String.valueOf(m.teamMultiplier);
        }
    }

    private final boolean client;
    private final ScrollerView body = StaffUi.scroller();
    private final TextField search = new TextField();
    private int shownVersion = -1;
    private boolean rebuild;
    private boolean dirty;
    /** The made task whose card is open ({@code -1}: the list). */
    private int card = -1;
    /** In that card, what a picker is open for: 0 none, 1 the icon, 2 an item target, 3 a held item, 4 a target from a list, 5 a biome, 6 a dimension. */
    private int picking;

    private String dailyCount = "";
    private String starcoinsPerCup = "";
    private String seasonalCount = "";
    private String weeklyEasy = "", weeklyHard = "", weeklyPrime = "";
    private final Map<String, String> cups = new HashMap<>();
    private final Set<String> disabledDaily = new LinkedHashSet<>();
    private final Set<String> weeklyDailies = new LinkedHashSet<>();
    private final Set<String> weeklyChallenges = new LinkedHashSet<>();
    private final Set<String> seasonal = new LinkedHashSet<>();
    private final Map<String, String> amounts = new HashMap<>();
    private final Map<String, String> multipliers = new HashMap<>();
    private final List<Made> made = new ArrayList<>();

    TasksAdminPanel(UIElement pane, boolean client) {
        this.client = client;
        search.setText("", false);
        search.layout(l -> l.widthPercent(100).height(StaffUi.ROW_H).flexShrink(0));
        search.textFieldStyle(t -> t.placeholder(Component.translatable("gui.roll_mod.tasks.admin.search")));
        search.setTextResponder(t -> rebuild = true);
        pane.addChildren(search, body);
    }

    void enter() {
        if (!client) return;
        dirty = false;
        card = -1;
        picking = 0;
        PacketDistributor.sendToServer(new BattlePassPackets.Act("tasks/get", new CompoundTag()));
    }

    void tick() {
        if (!client) return;
        if (ClientBattlePassCache.TASKS_VERSION != shownVersion) {
            shownVersion = ClientBattlePassCache.TASKS_VERSION;
            if (!dirty) load(ClientBattlePassCache.TASKS);
            rebuild = true;
        }
        if (search.isDisplayed() != (card < 0)) search.setDisplay(card < 0);
        if (!rebuild) return;
        rebuild = false;
        build();
    }

    private void load(CompoundTag tag) {
        dailyCount = String.valueOf(tag.getInt("dailyCount"));
        starcoinsPerCup = String.valueOf(tag.getLong("starcoinsPerCup"));
        seasonalCount = String.valueOf(tag.getInt("seasonalCount"));
        weeklyEasy = String.valueOf(tag.getInt("weeklyEasyCount"));
        weeklyHard = String.valueOf(tag.getInt("weeklyHardCount"));
        weeklyPrime = String.valueOf(tag.getInt("weeklyPrimeCount"));
        cups.clear();
        CompoundTag cupTag = tag.getCompound("cups");
        for (String key : TaskSettings.CUP_KEYS) cups.put(key, String.valueOf(cupTag.getInt(key)));
        reset(disabledDaily, TaskSettings.strings(tag, "disabledDaily"));
        reset(weeklyDailies, TaskSettings.strings(tag, "weeklyDailies"));
        reset(weeklyChallenges, TaskSettings.strings(tag, "weeklyChallenges"));
        reset(seasonal, TaskSettings.strings(tag, "seasonalChallenges"));
        amounts.clear();
        TaskSettings.amounts(tag).forEach((id, amount) -> amounts.put(id, String.valueOf(amount)));
        multipliers.clear();
        TaskSettings.multipliers(tag).forEach((id, mult) -> multipliers.put(id, trim(mult)));
        made.clear();
        for (TaskSettings.Made m : TaskSettings.made(tag)) made.add(new Made(m));
        if (card >= made.size()) card = -1;
    }

    private static void reset(Set<String> set, List<String> values) {
        set.clear();
        set.addAll(values);
    }

    private void build() {
        body.clearAllScrollViewChildren();
        if (card >= 0 && picking != 0) {
            picker();
            return;
        }
        if (card >= 0) {
            card();
            return;
        }
        body.addScrollViewChild(StaffUi.header("gui.roll_mod.tasks.admin.title"));
        body.addScrollViewChild(StaffUi.dim("gui.roll_mod.tasks.admin.hint"));

        body.addScrollViewChild(StaffUi.header("gui.roll_mod.tasks.admin.general"));
        body.addScrollViewChild(numberRow("gui.roll_mod.tasks.admin.daily_count", dailyCount, v -> dailyCount = v));
        body.addScrollViewChild(numberRow("gui.roll_mod.tasks.admin.weekly_easy", weeklyEasy, v -> weeklyEasy = v));
        body.addScrollViewChild(numberRow("gui.roll_mod.tasks.admin.weekly_hard", weeklyHard, v -> weeklyHard = v));
        body.addScrollViewChild(numberRow("gui.roll_mod.tasks.admin.weekly_prime", weeklyPrime, v -> weeklyPrime = v));
        body.addScrollViewChild(numberRow("gui.roll_mod.tasks.admin.seasonal_count", seasonalCount, v -> seasonalCount = v));
        body.addScrollViewChild(numberRow("gui.roll_mod.tasks.admin.rate", starcoinsPerCup, v -> starcoinsPerCup = v));
        body.addScrollViewChild(StaffUi.header("gui.roll_mod.tasks.admin.cups"));
        for (String key : TaskSettings.CUP_KEYS) {
            body.addScrollViewChild(numberRow("gui.roll_mod.tasks.admin.cup." + key, cups.getOrDefault(key, "0"),
                    v -> cups.put(key, v)));
        }

        body.addScrollViewChild(StaffUi.header("gui.roll_mod.tasks.admin.list"));
        body.addScrollViewChild(StaffUi.dim("gui.roll_mod.tasks.admin.list_hint"));
        String query = search.getText().strip().toLowerCase(Locale.ROOT);
        for (DailyTask task : DailyTaskRegistry.scanned()) {
            String name = task.name().getString();
            if (!query.isEmpty() && !name.toLowerCase(Locale.ROOT).contains(query) && !task.id().contains(query)) continue;
            body.addScrollViewChild(taskRow(task.id(), task.name(), task.battlePassOnly(), task.baseAmount(), -1));
        }
        for (int i = 0; i < made.size(); i++) {
            TaskSettings.Made m = made.get(i).m;
            Component name = m.title.isBlank() ? Component.translatable("dailytask.roll_mod.custom.title")
                    : Component.literal(m.title);
            if (!query.isEmpty() && !name.getString().toLowerCase(Locale.ROOT).contains(query) && !m.id.contains(query)) continue;
            body.addScrollViewChild(taskRow(m.id, name.copy().withStyle(ChatFormatting.AQUA), m.passOnly, -1, i));
        }
        Button add = StaffUi.button("gui.roll_mod.tasks.admin.add", 140, () -> {
            TaskSettings.Made m = new TaskSettings.Made();
            m.id = "custom_" + Long.toString(System.currentTimeMillis() % 1_000_000_000L, 36);
            made.add(new Made(m));
            dirty = true;
            card = made.size() - 1;
            rebuild = true;
        });
        add.layout(l -> l.marginTop(3));
        body.addScrollViewChild(add);
        body.addScrollViewChild(saveRow());
    }

    /** A label and a number field on one row. */
    private UIElement numberRow(String key, String value, java.util.function.Consumer<String> set) {
        UIElement row = StaffUi.row();
        Label label = StaffUi.text(Component.translatable(key));
        label.layout(l -> l.flexBasis(0).flexGrow(1).height(StaffUi.ROW_H));
        label.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));
        TextField field = new TextField();
        field.setText(value);
        field.layout(l -> l.width(70).height(StaffUi.ROW_H).flexShrink(0));
        field.setTextResponder(t -> {
            set.accept(t.strip());
            dirty = true;
        });
        row.addChildren(label, field);
        return row;
    }

    /**
     * One task: its name (a made one opens its card), where it may appear — Д daily, Т weekly, С seasonal — and the
     * amount it asks for (blank: its own, shown greyed in the field).
     */
    private UIElement taskRow(String id, Component name, boolean passOnly, int ownAmount, int madeIndex) {
        UIElement row = StaffUi.row();
        row.setOverflowVisible(true);
        Button nameButton = StaffUi.button(name, 0, () -> {
            if (madeIndex >= 0) {
                card = madeIndex;
                rebuild = true;
            }
        });
        nameButton.layout(l -> l.flexBasis(0).flexGrow(1));
        StaffUi.leftCaption(nameButton);
        nameButton.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(
                List.of(Component.literal(id).withStyle(ChatFormatting.GRAY)), null, null, ItemStack.EMPTY));
        row.addChild(nameButton);

        // Daily: the battle pass's own challenges never land on the board.
        if (passOnly) {
            UIElement gap = new UIElement();
            gap.layout(l -> l.width(18).height(StaffUi.ROW_H).flexShrink(0));
            row.addChild(gap);
        } else {
            row.addChild(toggle("gui.roll_mod.tasks.admin.daily_short", "gui.roll_mod.tasks.admin.daily_tip",
                    !disabledDaily.contains(id), on -> {
                        if (on) disabledDaily.remove(id);
                        else disabledDaily.add(id);
                    }));
        }
        Set<String> weekly = passOnly ? weeklyChallenges : weeklyDailies;
        row.addChild(toggle("gui.roll_mod.tasks.admin.weekly_short", "gui.roll_mod.tasks.admin.weekly_tip",
                weekly.contains(id), on -> {
                    if (on) weekly.add(id);
                    else weekly.remove(id);
                }));
        row.addChild(toggle("gui.roll_mod.tasks.admin.seasonal_short", "gui.roll_mod.tasks.admin.seasonal_tip",
                seasonal.contains(id), on -> {
                    if (on) seasonal.add(id);
                    else seasonal.remove(id);
                }));

        TextField amount = new TextField();
        if (madeIndex >= 0) {
            Made m = made.get(madeIndex);
            amount.setText(m.amount);
            amount.setTextResponder(t -> {
                m.amount = t.strip();
                dirty = true;
            });
        } else {
            amount.setText(amounts.getOrDefault(id, ""));
            amount.textFieldStyle(t -> t.placeholder(Component.literal(String.valueOf(ownAmount)).withStyle(ChatFormatting.DARK_GRAY)));
            amount.setTextResponder(t -> {
                if (t.isBlank()) amounts.remove(id);
                else amounts.put(id, t.strip());
                dirty = true;
            });
        }
        amount.layout(l -> l.width(44).height(StaffUi.ROW_H).flexShrink(0));
        amount.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(
                List.of(Component.translatable("gui.roll_mod.tasks.admin.amount_tip")), null, null, ItemStack.EMPTY));
        row.addChild(amount);

        // The team multiplier: a made task's own, or another one for a code task (blank: the task's own).
        TextField mult = new TextField();
        if (madeIndex >= 0) {
            Made m = made.get(madeIndex);
            mult.setText(m.mult);
            mult.textFieldStyle(t -> t.placeholder(Component.literal("0").withStyle(ChatFormatting.DARK_GRAY)));
            mult.setTextResponder(t -> {
                m.mult = t.strip();
                dirty = true;
            });
        } else {
            mult.setText(multipliers.getOrDefault(id, ""));
            String own = ownMult(id);
            mult.textFieldStyle(t -> t.placeholder(Component.literal(own).withStyle(ChatFormatting.DARK_GRAY)));
            mult.setTextResponder(t -> {
                if (t.isBlank()) multipliers.remove(id);
                else multipliers.put(id, t.strip());
                dirty = true;
            });
        }
        mult.layout(l -> l.width(34).height(StaffUi.ROW_H).flexShrink(0));
        mult.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(
                List.of(Component.translatable("gui.roll_mod.tasks.admin.mult_tip")), null, null, ItemStack.EMPTY));
        row.addChild(mult);
        return row;
    }

    /** A code task's own team multiplier as the list shows it: "1" for one that does not grow, "0" for the usual. */
    private static String ownMult(String id) {
        for (DailyTask task : DailyTaskRegistry.scanned()) {
            if (!task.id().equals(id)) continue;
            return task.scalesWithTeam() ? trim(task.teamMultiplier()) : "1";
        }
        return "0";
    }

    /** A small on/off switch: a letter, green when on, grey when off. */
    private Button toggle(String key, String tipKey, boolean on, java.util.function.Consumer<Boolean> set) {
        boolean[] state = {on};
        Button button = StaffUi.button(caption(key, on), 18, () -> {});
        button.setOnClick(e -> {
            state[0] = !state[0];
            set.accept(state[0]);
            button.setText(caption(key, state[0]));
            dirty = true;
        });
        button.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(
                List.of(Component.translatable(tipKey)), null, null, ItemStack.EMPTY));
        return button;
    }

    private static Component caption(String key, boolean on) {
        return Component.translatable(key).withStyle(on ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY);
    }

    /* ------------------------------------------------------ a made task ------------------------------------------------------ */

    private void card() {
        Made edit = made.get(card);
        TaskSettings.Made m = edit.m;
        body.addScrollViewChild(StaffUi.header("gui.roll_mod.tasks.admin.card"));
        body.addScrollViewChild(StaffUi.dim("gui.roll_mod.tasks.admin.card_hint"));
        body.addScrollViewChild(textRow("gui.roll_mod.tasks.admin.f.title", m.title, v -> m.title = v));
        body.addScrollViewChild(textRow("gui.roll_mod.tasks.admin.f.desc", m.desc, v -> m.desc = v));
        body.addScrollViewChild(StaffUi.dim("gui.roll_mod.tasks.admin.f.desc_hint"));
        body.addScrollViewChild(iconRow(m));

        // What it counts: a drop-down of every hook.
        UIElement hookRow = StaffUi.row();
        hookRow.setOverflowVisible(true);
        Label hookLabel = StaffUi.text(Component.translatable("gui.roll_mod.tasks.admin.f.hook"));
        hookLabel.layout(l -> l.width(LABEL_W).height(StaffUi.ROW_H).flexShrink(0));
        hookLabel.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));
        com.lowdragmc.lowdraglib2.gui.ui.elements.Selector<String> hook = new com.lowdragmc.lowdraglib2.gui.ui.elements.Selector<>();
        hook.setCandidateUIProvider(com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider.text(
                h -> h == null ? Component.empty() : hookName(h)));
        hook.setCandidates(List.of(HOOKS));
        hook.setSelected(m.hook, false);
        hook.layout(l -> l.flexBasis(0).flexGrow(1).height(StaffUi.ROW_H));
        hook.selectorStyle(st -> st.maxItemCount(12));
        hook.setOnValueChanged(h -> {
            if (h == null || h.equals(m.hook)) return;
            m.hook = h;
            dirty = true;
            rebuild = true; // the target row changes with it
        });
        hookRow.addChildren(hookLabel, hook);
        body.addScrollViewChild(hookRow);

        TargetKind kind = targetKind(m.hook);
        if (kind != TargetKind.NONE) {
            UIElement targetRow = textRow("gui.roll_mod.tasks.admin.f.target", m.target, v -> m.target = v);
            targetRow.setOverflowVisible(true);
            if (kind != TargetKind.TEXT) {
                targetRow.addChild(StaffUi.button("gui.roll_mod.tasks.admin.f.pick", 60, () -> {
                    picking = kind == TargetKind.ITEM ? 2 : 4;
                    rebuild = true;
                }));
            }
            body.addScrollViewChild(targetRow);
            body.addScrollViewChild(StaffUi.dim("gui.roll_mod.tasks.admin.f.target_hint"));
            body.addScrollViewChild(switchRow("gui.roll_mod.tasks.admin.f.one_a_day", m.oneADay, v -> m.oneADay = v));
        }
        body.addScrollViewChild(textRow("gui.roll_mod.tasks.admin.f.amount", edit.amount, v -> edit.amount = v));
        body.addScrollViewChild(textRow("gui.roll_mod.tasks.admin.f.weight", edit.weight, v -> edit.weight = v));
        body.addScrollViewChild(switchRow("gui.roll_mod.tasks.admin.f.scales", m.scales, v -> m.scales = v));
        body.addScrollViewChild(textRow("gui.roll_mod.tasks.admin.f.mult", edit.mult, v -> edit.mult = v));
        body.addScrollViewChild(StaffUi.dim("gui.roll_mod.tasks.admin.f.mult_hint"));
        body.addScrollViewChild(switchRow("gui.roll_mod.tasks.admin.f.pass_only", m.passOnly, v -> m.passOnly = v));

        body.addScrollViewChild(StaffUi.header("gui.roll_mod.tasks.admin.cond"));
        body.addScrollViewChild(StaffUi.dim("gui.roll_mod.tasks.admin.cond_hint"));
        UIElement dimRow = textRow("gui.roll_mod.tasks.admin.cond.dimension", m.dimension, v -> m.dimension = v);
        dimRow.setOverflowVisible(true);
        dimRow.addChild(StaffUi.button("gui.roll_mod.tasks.admin.f.pick", 60, () -> {
            picking = 6;
            rebuild = true;
        }));
        body.addScrollViewChild(dimRow);
        UIElement biomeRow = textRow("gui.roll_mod.tasks.admin.cond.biome", m.biome, v -> m.biome = v);
        biomeRow.setOverflowVisible(true);
        biomeRow.addChild(StaffUi.button("gui.roll_mod.tasks.admin.f.pick", 60, () -> {
            picking = 5;
            rebuild = true;
        }));
        body.addScrollViewChild(biomeRow);
        UIElement heldRow = textRow("gui.roll_mod.tasks.admin.cond.held", m.held, v -> m.held = v);
        heldRow.setOverflowVisible(true);
        heldRow.addChild(StaffUi.button("gui.roll_mod.tasks.admin.f.pick", 60, () -> {
            picking = 3;
            rebuild = true;
        }));
        body.addScrollViewChild(heldRow);
        body.addScrollViewChild(cycleRow("gui.roll_mod.tasks.admin.cond.time", TIMES, m.time, v -> m.time = v));
        body.addScrollViewChild(cycleRow("gui.roll_mod.tasks.admin.cond.weather", WEATHERS, m.weather, v -> m.weather = v));
        body.addScrollViewChild(textRow("gui.roll_mod.tasks.admin.cond.min_y", m.minY, v -> m.minY = v));
        body.addScrollViewChild(textRow("gui.roll_mod.tasks.admin.cond.max_y", m.maxY, v -> m.maxY = v));
        body.addScrollViewChild(switchRow("gui.roll_mod.tasks.admin.cond.sneaking", m.sneaking, v -> m.sneaking = v));
        body.addScrollViewChild(StaffUi.header("gui.roll_mod.tasks.admin.look"));
        body.addScrollViewChild(modRow(m));

        UIElement buttons = StaffUi.row();
        buttons.layout(l -> l.marginTop(6));
        buttons.setOverflowVisible(true);
        buttons.addChildren(
                StaffUi.button("gui.roll_mod.tasks.admin.back", 70, () -> {
                    card = -1;
                    rebuild = true;
                }),
                StaffUi.button("gui.roll_mod.tasks.admin.delete", 70, () -> {
                    disabledDaily.remove(m.id);
                    weeklyDailies.remove(m.id);
                    weeklyChallenges.remove(m.id);
                    seasonal.remove(m.id);
                    made.remove(card);
                    card = -1;
                    dirty = true;
                    rebuild = true;
                }));
        body.addScrollViewChild(buttons);
    }

    private static final int LABEL_W = 110;

    /** The mod the task needs, picked from the mods loaded here; "none" first. */
    private UIElement modRow(TaskSettings.Made m) {
        UIElement row = StaffUi.row();
        row.setOverflowVisible(true);
        Label label = StaffUi.text(Component.translatable("gui.roll_mod.tasks.admin.f.mod"));
        label.layout(l -> l.width(LABEL_W).height(StaffUi.ROW_H).flexShrink(0));
        label.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));
        List<String> mods = new ArrayList<>();
        mods.add("");
        net.neoforged.fml.ModList.get().getMods().stream()
                .map(info -> info.getModId()).sorted().forEach(mods::add);
        if (!m.requiredMod.isBlank() && !mods.contains(m.requiredMod)) mods.add(m.requiredMod);
        com.lowdragmc.lowdraglib2.gui.ui.elements.Selector<String> mod = new com.lowdragmc.lowdraglib2.gui.ui.elements.Selector<>();
        mod.setCandidateUIProvider(com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider.text(id -> id == null ? Component.empty()
                : id.isEmpty() ? Component.translatable("gui.roll_mod.tasks.admin.f.mod_none").withStyle(ChatFormatting.GRAY)
                : Component.literal(modName(id))));
        mod.setCandidates(mods);
        mod.setSelected(m.requiredMod, false);
        mod.layout(l -> l.flexBasis(0).flexGrow(1).height(StaffUi.ROW_H));
        mod.selectorStyle(st -> st.maxItemCount(10));
        mod.setOnValueChanged(id -> {
            if (id == null) return;
            m.requiredMod = id;
            dirty = true;
        });
        row.addChildren(label, mod);
        return row;
    }

    private static String modName(String id) {
        return net.neoforged.fml.ModList.get().getModContainerById(id)
                .map(c -> c.getModInfo().getDisplayName() + " (" + id + ")").orElse(id);
    }

    /** The icon: the item (or picture) it shows, a button that opens the item picker, and one that clears it. */
    private UIElement iconRow(TaskSettings.Made m) {
        UIElement row = StaffUi.row();
        row.setOverflowVisible(true);
        Label label = StaffUi.text(Component.translatable("gui.roll_mod.tasks.admin.f.icon"));
        label.layout(l -> l.width(LABEL_W).height(StaffUi.ROW_H).flexShrink(0));
        label.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));
        UIElement preview = new UIElement();
        preview.layout(l -> l.width(16).height(16).flexShrink(0));
        ItemStack stack = iconStack(m.icon);
        preview.style(st -> st.background(stack.isEmpty() ? com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture.EMPTY
                : new com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture(stack)));
        Button pick = StaffUi.button(stack.isEmpty()
                ? Component.translatable("gui.roll_mod.tasks.admin.f.icon_pick").withStyle(ChatFormatting.GRAY)
                : stack.getHoverName(), 0, () -> {
            picking = 1;
            rebuild = true;
        });
        pick.layout(l -> l.flexBasis(0).flexGrow(1));
        StaffUi.leftCaption(pick);
        Button clear = StaffUi.button(Component.literal("✕"), 16, () -> {
            m.icon = "";
            dirty = true;
            rebuild = true;
        });
        row.addChildren(label, preview, pick, clear);
        return row;
    }

    private static ItemStack iconStack(String id) {
        if (id == null || id.isBlank() || id.endsWith(".png")) return ItemStack.EMPTY;
        net.minecraft.resources.ResourceLocation rl = net.minecraft.resources.ResourceLocation.tryParse(id.strip());
        if (rl == null) return ItemStack.EMPTY;
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(rl)
                .map(ItemStack::new).orElse(ItemStack.EMPTY);
    }

    /** The battle pass's item picker, for the icon or a target to add. */
    private void picker() {
        TaskSettings.Made m = made.get(card).m;
        int what = picking;
        if (what >= 4) {
            listPicker(m, what);
            return;
        }
        UIElement holder = new UIElement();
        holder.layout(l -> l.flexDirection(dev.vfyjxf.taffy.style.FlexDirection.COLUMN).widthPercent(100));
        holder.setOverflowVisible(true);
        body.addScrollViewChild(holder);
        String current = what == 1 && !m.icon.isBlank() && !m.icon.endsWith(".png") ? m.icon + "*1" : "minecraft:stone*1";
        new BattlePassRewardEditor(holder, Component.translatable(what == 1 ? "gui.roll_mod.tasks.admin.f.icon"
                : "gui.roll_mod.tasks.admin.f.add_item"), current, false, picked -> {
            ItemStack stack = com.roll_54.roll_mod.minestar.battlepass.BattlePassReward.stack(picked);
            if (!stack.isEmpty()) {
                String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                if (what == 1) {
                    m.icon = id;
                } else if (what == 3) {
                    m.held = m.held.isBlank() ? id : m.held.strip() + ", " + id;
                } else {
                    m.target = m.target.isBlank() ? id : m.target.strip() + ", " + id;
                }
                dirty = true;
            }
            picking = 0;
            rebuild = true;
        }, () -> {
            picking = 0;
            rebuild = true;
        }).show();
    }


    /** One choice in the searchable list: what goes into the field, how it reads, and an item to show by it. */
    private record Choice(String id, Component name, ItemStack icon) {}

    /**
     * A searchable list to pick from, in place of the card: creatures, blocks, dimensions, statistics or advancements
     * for the target (4), biomes (5) or dimensions (6) for the conditions. Tags come first with a #. Clicking one adds it
     * to the field (comma-separated) and goes back to the card.
     */
    private void listPicker(TaskSettings.Made m, int what) {
        TargetKind kind = what == 5 ? null : what == 6 ? TargetKind.DIMENSION : targetKind(m.hook);
        List<Choice> all = choices(kind);
        body.addScrollViewChild(StaffUi.header(what == 5 ? "gui.roll_mod.tasks.admin.cond.biome"
                : what == 6 ? "gui.roll_mod.tasks.admin.cond.dimension" : "gui.roll_mod.tasks.admin.f.target"));
        TextField query = new TextField();
        query.setText("", false);
        query.layout(l -> l.widthPercent(100).height(StaffUi.ROW_H).flexShrink(0));
        query.textFieldStyle(t -> t.placeholder(Component.translatable("gui.roll_mod.tasks.admin.search")));
        UIElement results = new UIElement();
        results.layout(l -> l.flexDirection(dev.vfyjxf.taffy.style.FlexDirection.COLUMN).widthPercent(100));
        results.setOverflowVisible(true);
        java.util.function.Consumer<String> fill = q -> {
            results.clearAllChildren();
            String needle = q.strip().toLowerCase(Locale.ROOT);
            int shown = 0;
            for (Choice c : all) {
                if (!needle.isEmpty() && !c.id().toLowerCase(Locale.ROOT).contains(needle)
                        && !c.name().getString().toLowerCase(Locale.ROOT).contains(needle)) continue;
                if (++shown > 150) break;
                UIElement row = StaffUi.row();
                row.setOverflowVisible(true);
                UIElement icon = new UIElement();
                icon.layout(l -> l.width(16).height(16).flexShrink(0).marginRight(3));
                if (!c.icon().isEmpty()) {
                    icon.style(st -> st.background(new com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture(c.icon())));
                }
                Button pick = StaffUi.button(c.name().copy().append(Component.literal("  " + c.id()).withStyle(ChatFormatting.GRAY)), 0, () -> {
                    switch (what) {
                        case 5 -> m.biome = add(m.biome, c.id());
                        case 6 -> m.dimension = c.id(); // one dimension: the player is in only one
                        default -> m.target = add(m.target, c.id());
                    }
                    dirty = true;
                    picking = 0;
                    rebuild = true;
                });
                pick.layout(l -> l.flexBasis(0).flexGrow(1));
                StaffUi.leftCaption(pick);
                row.addChildren(icon, pick);
                results.addChild(row);
            }
            if (shown == 0) results.addChild(StaffUi.dim("gui.roll_mod.tasks.admin.pick_none"));
        };
        query.setTextResponder(fill::accept);
        fill.accept("");
        Button back = StaffUi.button("gui.roll_mod.tasks.admin.back", 70, () -> {
            picking = 0;
            rebuild = true;
        });
        body.addScrollViewChild(query);
        body.addScrollViewChild(back);
        body.addScrollViewChild(results);
    }

    private static String add(String field, String id) {
        return field == null || field.isBlank() ? id : field.strip() + ", " + id;
    }

    /** What may be picked for {@code kind} (null: biomes), tags first. */
    private static List<Choice> choices(TargetKind kind) {
        List<Choice> out = new ArrayList<>();
        var mc = net.minecraft.client.Minecraft.getInstance();
        var connection = mc.getConnection();
        if (kind == null) {
            if (connection == null) return out;
            var biomes = connection.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME);
            biomes.getTagNames().map(t -> t.location()).sorted().forEach(rl ->
                    out.add(new Choice("#" + rl, Component.literal("#" + rl.getPath()).withStyle(ChatFormatting.AQUA), ItemStack.EMPTY)));
            biomes.keySet().stream().sorted().forEach(rl -> out.add(new Choice(rl.toString(),
                    Component.translatable("biome." + rl.getNamespace() + "." + rl.getPath()), ItemStack.EMPTY)));
            return out;
        }
        switch (kind) {
            case ENTITY -> {
                var reg = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE;
                reg.getTagNames().map(t -> t.location()).sorted().forEach(rl ->
                        out.add(new Choice("#" + rl, Component.literal("#" + rl.getPath()).withStyle(ChatFormatting.AQUA), ItemStack.EMPTY)));
                for (var type : reg) {
                    net.minecraft.world.item.SpawnEggItem egg = net.minecraft.world.item.SpawnEggItem.byId(type);
                    if (egg == null && type.getCategory() == net.minecraft.world.entity.MobCategory.MISC) continue;
                    out.add(new Choice(reg.getKey(type).toString(), type.getDescription(),
                            egg == null ? ItemStack.EMPTY : new ItemStack(egg)));
                }
            }
            case BLOCK -> {
                var reg = net.minecraft.core.registries.BuiltInRegistries.BLOCK;
                reg.getTagNames().map(t -> t.location()).sorted().forEach(rl ->
                        out.add(new Choice("#" + rl, Component.literal("#" + rl.getPath()).withStyle(ChatFormatting.AQUA), ItemStack.EMPTY)));
                for (var block : reg) {
                    if (block == net.minecraft.world.level.block.Blocks.AIR) continue;
                    out.add(new Choice(reg.getKey(block).toString(), block.getName(), new ItemStack(block.asItem())));
                }
            }
            case DIMENSION -> {
                if (connection != null) {
                    connection.levels().stream().map(k -> k.location()).sorted().forEach(rl -> out.add(new Choice(rl.toString(),
                            Component.literal(rl.getPath()), ItemStack.EMPTY)));
                }
            }
            case STAT -> net.minecraft.core.registries.BuiltInRegistries.CUSTOM_STAT.keySet().stream().sorted().forEach(rl ->
                    out.add(new Choice(rl.toString(), Component.translatable("stat." + rl.toString().replace(':', '.')), ItemStack.EMPTY)));
            case ADVANCEMENT -> {
                if (connection != null) {
                    for (var node : connection.getAdvancements().getTree().nodes()) {
                        var holder = node.holder();
                        if (holder.id().getPath().startsWith("recipes/")) continue;
                        var display = holder.value().display().orElse(null);
                        out.add(new Choice(holder.id().toString(),
                                display == null ? Component.literal(holder.id().getPath()) : display.getTitle(),
                                display == null ? ItemStack.EMPTY : display.getIcon()));
                    }
                    out.sort(java.util.Comparator.comparing(Choice::id));
                }
            }
            default -> {
            }
        }
        return out;
    }

    /** A label and an on/off button ("Так" / "Ні"). */
    private UIElement switchRow(String key, boolean value, java.util.function.Consumer<Boolean> set) {
        UIElement row = StaffUi.row();
        row.setOverflowVisible(true);
        Label label = StaffUi.text(Component.translatable(key));
        label.layout(l -> l.flexBasis(0).flexGrow(1).height(StaffUi.ROW_H));
        label.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));
        boolean[] state = {value};
        Button button = StaffUi.button(yesNo(value), 50, () -> {});
        button.setOnClick(e -> {
            state[0] = !state[0];
            set.accept(state[0]);
            button.setText(yesNo(state[0]));
            dirty = true;
        });
        row.addChildren(label, button);
        return row;
    }

    private static Component yesNo(boolean on) {
        return Component.translatable(on ? "gui.roll_mod.tasks.admin.yes" : "gui.roll_mod.tasks.admin.no")
                .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.GRAY);
    }

    /** A label and a button that steps through {@code values} ("" first: any). */
    private UIElement cycleRow(String key, String[] values, String value, java.util.function.Consumer<String> set) {
        UIElement row = StaffUi.row();
        row.setOverflowVisible(true);
        Label label = StaffUi.text(Component.translatable(key));
        label.layout(l -> l.width(LABEL_W).height(StaffUi.ROW_H).flexShrink(0));
        label.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));
        String[] state = {value == null ? "" : value};
        Button button = StaffUi.button(cycleName(key, state[0]), 0, () -> {});
        button.layout(l -> l.flexBasis(0).flexGrow(1));
        button.setOnClick(e -> {
            int at = List.of(values).indexOf(state[0]);
            state[0] = values[(at + 1) % values.length];
            set.accept(state[0]);
            button.setText(cycleName(key, state[0]));
            dirty = true;
        });
        row.addChildren(label, button);
        return row;
    }

    private static Component cycleName(String key, String value) {
        return value.isEmpty() ? Component.translatable("gui.roll_mod.tasks.admin.cond.any").withStyle(ChatFormatting.GRAY)
                : Component.translatable(key + "." + value);
    }

    private static Component hookName(String hook) {
        return Component.translatable("gui.roll_mod.tasks.admin.hook." + hook.toLowerCase(Locale.ROOT));
    }

    private UIElement textRow(String key, String value, java.util.function.Consumer<String> set) {
        UIElement row = StaffUi.row();
        Label label = StaffUi.text(Component.translatable(key));
        label.layout(l -> l.width(LABEL_W).height(StaffUi.ROW_H).flexShrink(0));
        label.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));
        TextField field = new TextField();
        field.setText(value);
        // LdLib's own placeholder reads "Empty"; an empty field here just stays empty.
        field.textFieldStyle(t -> t.placeholder(Component.empty()));
        field.layout(l -> l.flexBasis(0).flexGrow(1).height(StaffUi.ROW_H));
        field.setTextResponder(t -> {
            set.accept(t);
            dirty = true;
        });
        row.addChildren(label, field);
        return row;
    }

    /* --------------------------------------------------------- save --------------------------------------------------------- */

    private UIElement saveRow() {
        UIElement saveRow = StaffUi.row();
        saveRow.layout(l -> l.marginTop(6).marginBottom(2));
        saveRow.setOverflowVisible(true);
        saveRow.addChildren(
                StaffUi.button("gui.roll_mod.staff.commands.revert", 70, () -> {
                    dirty = false;
                    card = -1;
                    load(ClientBattlePassCache.TASKS);
                    rebuild = true;
                }),
                StaffUi.button("gui.roll_mod.hub.moderation.save", 70, () -> {
                    dirty = false;
                    PacketDistributor.sendToServer(new BattlePassPackets.Act("tasks/set", args()));
                }));
        return saveRow;
    }

    private CompoundTag args() {
        CompoundTag args = new CompoundTag();
        args.putInt("dailyCount", number(dailyCount, 4));
        args.putLong("starcoinsPerCup", number(starcoinsPerCup, 10));
        args.putInt("seasonalCount", number(seasonalCount, 5));
        args.putInt("weeklyEasyCount", number(weeklyEasy, 3));
        args.putInt("weeklyHardCount", number(weeklyHard, 1));
        args.putInt("weeklyPrimeCount", number(weeklyPrime, 1));
        CompoundTag cupTag = new CompoundTag();
        cups.forEach((key, value) -> cupTag.putInt(key, number(value, 0)));
        args.put("cups", cupTag);
        args.put("disabledDaily", list(disabledDaily));
        args.put("weeklyDailies", list(weeklyDailies));
        args.put("weeklyChallenges", list(weeklyChallenges));
        args.put("seasonalChallenges", list(seasonal));
        CompoundTag amountTag = new CompoundTag();
        amounts.forEach((id, value) -> {
            int n = number(value, 0);
            if (n > 0) amountTag.putInt(id, n);
        });
        args.put("amounts", amountTag);
        CompoundTag multTag = new CompoundTag();
        multipliers.forEach((id, value) -> {
            double v = decimal(value, -1);
            if (v >= 0) multTag.putDouble(id, v);
        });
        args.put("multipliers", multTag);
        ListTag madeTag = new ListTag();
        for (Made edit : made) {
            TaskSettings.Made m = edit.m;
            m.amount = number(edit.amount, 1);
            m.weight = number(edit.weight, 1);
            try {
                m.teamMultiplier = edit.mult.isBlank() ? 0 : Double.parseDouble(edit.mult.strip().replace(',', '.'));
            } catch (NumberFormatException e) {
                m.teamMultiplier = 0;
            }
            madeTag.add(m.toTag());
        }
        args.put("made", madeTag);
        return args;
    }

    private static String trim(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v);
    }

    private static double decimal(String text, double fallback) {
        try {
            return Double.parseDouble(text.strip().replace(',', '.'));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static ListTag list(Set<String> values) {
        ListTag list = new ListTag();
        for (String v : values) list.add(StringTag.valueOf(v));
        return list;
    }

    private static int number(String text, int fallback) {
        try {
            return Integer.parseInt(text.strip());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
