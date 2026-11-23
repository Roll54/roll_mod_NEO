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
            "ADVANCEMENT", "LEVEL_UP", "USE", "DAMAGE", "VISIT", "STAT"};
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
    /** In that card, what the item picker is open for: 0 none, 1 the icon, 2 a target to add, 3 a held item to add. */
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

        UIElement hookRow = StaffUi.row();
        hookRow.setOverflowVisible(true);
        Label hookLabel = StaffUi.text(Component.translatable("gui.roll_mod.tasks.admin.f.hook"));
        hookLabel.layout(l -> l.width(LABEL_W).height(StaffUi.ROW_H).flexShrink(0));
        hookLabel.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));
        Button hook = StaffUi.button(hookName(m.hook), 0, () -> {});
        hook.layout(l -> l.flexBasis(0).flexGrow(1));
        hook.setOnClick(e -> {
            int at = List.of(HOOKS).indexOf(m.hook);
            m.hook = HOOKS[(at + 1) % HOOKS.length];
            hook.setText(hookName(m.hook));
            dirty = true;
        });
        hookRow.addChildren(hookLabel, hook);
        body.addScrollViewChild(hookRow);

        UIElement targetRow = textRow("gui.roll_mod.tasks.admin.f.target", m.target, v -> m.target = v);
        targetRow.setOverflowVisible(true);
        targetRow.addChild(StaffUi.button("gui.roll_mod.tasks.admin.f.add_item", 60, () -> {
            picking = 2;
            rebuild = true;
        }));
        body.addScrollViewChild(targetRow);
        body.addScrollViewChild(StaffUi.dim("gui.roll_mod.tasks.admin.f.target_hint"));
        body.addScrollViewChild(switchRow("gui.roll_mod.tasks.admin.f.one_a_day", m.oneADay, v -> m.oneADay = v));
        body.addScrollViewChild(textRow("gui.roll_mod.tasks.admin.f.amount", edit.amount, v -> edit.amount = v));
        body.addScrollViewChild(textRow("gui.roll_mod.tasks.admin.f.weight", edit.weight, v -> edit.weight = v));
        body.addScrollViewChild(switchRow("gui.roll_mod.tasks.admin.f.scales", m.scales, v -> m.scales = v));
        body.addScrollViewChild(textRow("gui.roll_mod.tasks.admin.f.mult", edit.mult, v -> edit.mult = v));
        body.addScrollViewChild(StaffUi.dim("gui.roll_mod.tasks.admin.f.mult_hint"));
        body.addScrollViewChild(switchRow("gui.roll_mod.tasks.admin.f.pass_only", m.passOnly, v -> m.passOnly = v));

        body.addScrollViewChild(StaffUi.header("gui.roll_mod.tasks.admin.cond"));
        body.addScrollViewChild(StaffUi.dim("gui.roll_mod.tasks.admin.cond_hint"));
        body.addScrollViewChild(textRow("gui.roll_mod.tasks.admin.cond.dimension", m.dimension, v -> m.dimension = v));
        body.addScrollViewChild(textRow("gui.roll_mod.tasks.admin.cond.biome", m.biome, v -> m.biome = v));
        UIElement heldRow = textRow("gui.roll_mod.tasks.admin.cond.held", m.held, v -> m.held = v);
        heldRow.setOverflowVisible(true);
        heldRow.addChild(StaffUi.button("gui.roll_mod.tasks.admin.f.add_item", 60, () -> {
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
        body.addScrollViewChild(iconRow(m));

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
