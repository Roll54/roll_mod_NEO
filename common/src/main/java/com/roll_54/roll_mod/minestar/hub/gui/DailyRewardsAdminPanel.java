package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.roll_54.roll_mod.minestar.battlepass.BattlePassReward;
import com.roll_54.roll_mod.minestar.battlepass.ClientBattlePassCache;
import com.roll_54.roll_mod.minestar.dailytasks.DailyRewardConfig;
import com.roll_54.roll_mod.network.packet.BattlePassPackets;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * The Moderation tab's Dailies editor: the weighted pool the daily tasks draw their two rewards from, and the ingot
 * that stands in for iron at each quest stage (the battle pass's ingots by default). Items are picked with the same
 * editor as the battle pass's rewards. Save sends it all; the server checks the ban permission, writes
 * {@code daily_rewards.json} and sends the new pool to everyone.
 */
final class DailyRewardsAdminPanel {

    private static final String[] STAGES = {"bronze", "steel", "LV", "MV", "HV", "EV", "IV"};

    private final boolean client;
    private final ScrollerView body = StaffUi.scroller();
    /** {@code id, item*count, weight}. */
    private final List<String[]> rows = new ArrayList<>();
    private int shownVersion = -1;
    private boolean rebuild;
    private boolean dirty;
    /** The pool row whose card is open ({@code -1}: the list). */
    private int entry = -1;
    /** In that card, what the item picker is open on: {@code -1} the base item, {@code 0..6} a stage; null for none. */
    private Integer editing;

    /** One all-tasks-done bonus as the editor holds it. */
    private static final class Bonus {
        String id;
        String weight;
        final List<String> payout;
        /** Per quest stage (bronze … IV), what it pays from then on; empty: as the stage before. */
        final List<List<String>> stages = new ArrayList<>();

        Bonus(String id, String weight, List<String> payout) {
            this(id, weight, payout, List.of());
        }

        Bonus(String id, String weight, List<String> payout, List<List<String>> stages) {
            this.id = id;
            this.weight = weight;
            this.payout = new ArrayList<>(payout);
            for (int st = 0; st < com.roll_54.roll_mod.minestar.dailytasks.TaskRewardPool.STAGES; st++) {
                this.stages.add(st < stages.size() ? new ArrayList<>(stages.get(st)) : new ArrayList<>());
            }
        }

        /** The lines being edited: the base payout ({@code -1}) or a stage's. */
        List<String> lines(int stage) {
            return stage < 0 ? payout : stages.get(stage);
        }
    }

    private final List<Bonus> bonuses = new ArrayList<>();
    /** The bonus whose card is open ({@code -1}: none). */
    private int bonusEntry = -1;
    /** In that card, the payout line the picker is open on ({@code -1}: a new line); null for none. */
    private Integer bonusEditing;
    /** Which of that bonus's payouts the card edits: {@code -1} the base one, {@code 0..6} a stage's. */
    private int bonusStage = -1;

    DailyRewardsAdminPanel(UIElement pane, boolean client) {
        this.client = client;
        pane.addChild(body);
    }

    void enter() {
        if (!client) return;
        dirty = false;
        editing = null;
        entry = -1;
        bonusEntry = -1;
        bonusEditing = null;
        bonusStage = -1;
        PacketDistributor.sendToServer(new BattlePassPackets.Act("daily/get", new CompoundTag()));
    }

    void tick() {
        if (!client) return;
        if (ClientBattlePassCache.DAILY_VERSION != shownVersion) {
            shownVersion = ClientBattlePassCache.DAILY_VERSION;
            if (!dirty) load(ClientBattlePassCache.DAILY);
            rebuild = true;
        }
        if (!rebuild) return;
        rebuild = false;
        build();
    }

    private void load(CompoundTag tag) {
        rows.clear();
        for (String[] row : DailyRewardConfig.rows(tag)) rows.add(row.clone());
        if (entry >= rows.size()) entry = -1;
        bonuses.clear();
        for (DailyRewardConfig.BonusEntry b : DailyRewardConfig.bonusRows(tag)) {
            bonuses.add(new Bonus(b.id, String.valueOf(b.weight), b.payout, b.stages == null ? List.of() : b.stages));
        }
        if (bonusEntry >= bonuses.size()) bonusEntry = -1;
    }

    private void build() {
        body.clearAllScrollViewChildren();
        if (bonusEntry >= 0 && bonusEditing != null) {
            bonusPicker();
            return;
        }
        if (bonusEntry >= 0) {
            bonusCard();
            return;
        }
        if (entry >= 0 && editing != null) {
            itemPicker();
            return;
        }
        if (entry >= 0) {
            card();
            return;
        }

        body.addScrollViewChild(StaffUi.header("gui.roll_mod.dailies.admin.title"));
        body.addScrollViewChild(StaffUi.dim("gui.roll_mod.dailies.admin.hint"));
        body.addScrollViewChild(StaffUi.header("gui.roll_mod.dailies.admin.pool"));
        for (int i = 0; i < rows.size(); i++) {
            int index = i;
            String[] row = rows.get(i);
            UIElement line = StaffUi.row();
            line.setOverflowVisible(true);
            line.addChild(preview(previewSpec(row[1])));
            // The reward, and how many stages change it; a click opens its card.
            int changes = 0;
            for (String st : stages(row)) if (!st.isBlank()) changes++;
            Button item = StaffUi.button(describeLine(row[1]), 0, () -> {
                entry = index;
                rebuild = true;
            });
            item.layout(l -> l.flexBasis(0).flexGrow(1));
            // A long name reads from the left and is cut at the button's edge, not spilled over the icon and the weight.
            StaffUi.leftCaption(item);
            // How many stages change it: a short tag of its own, the full sentence on hover.
            Label stagesTag = StaffUi.text(changes > 0
                    ? Component.translatable("gui.roll_mod.dailies.admin.changes_short", changes).withStyle(ChatFormatting.AQUA)
                    : Component.empty());
            stagesTag.layout(l -> l.width(28).height(StaffUi.ROW_H).flexShrink(0));
            stagesTag.textStyle(t -> t.textAlignVertical(Vertical.CENTER));
            int shownChanges = changes;
            if (changes > 0) stagesTag.addEventListener(com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents.HOVER_TOOLTIPS,
                    e -> e.hoverTooltips = new com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips(java.util.List.of(
                            Component.translatable("gui.roll_mod.dailies.admin.changes", shownChanges)), null, null, ItemStack.EMPTY));
            Label weightLabel = StaffUi.text(Component.translatable("gui.roll_mod.dailies.admin.weight"));
            weightLabel.layout(l -> l.width(44).height(StaffUi.ROW_H).flexShrink(0));
            weightLabel.textStyle(t -> t.textAlignVertical(Vertical.CENTER));
            TextField weight = new TextField();
            weight.setText(row[2]);
            weight.layout(l -> l.width(36).height(StaffUi.ROW_H).flexShrink(0));
            weight.setTextResponder(t -> {
                row[2] = t.strip();
                dirty = true;
            });
            Button remove = StaffUi.button(Component.literal("✕"), 16, () -> {
                rows.remove(index);
                dirty = true;
                rebuild = true;
            });
            line.addChildren(item, stagesTag, weightLabel, weight, remove);
            body.addScrollViewChild(line);
        }
        Button add = StaffUi.button("gui.roll_mod.dailies.admin.add", 120, () -> {
            rows.add(new String[]{"custom_" + System.currentTimeMillis() % 100000, "minecraft:diamond*1", "1", ""});
            dirty = true;
            entry = rows.size() - 1;
            editing = -1;
            rebuild = true;
        });
        add.layout(l -> l.marginTop(3));
        body.addScrollViewChild(add);
        bonusList();
        body.addScrollViewChild(saveRow());
    }

    /* ---------------------------------------------- the daily bonus ---------------------------------------------- */

    /** The all-tasks-done bonus draw: one row per bonus, its payout as icons, its weight. */
    private void bonusList() {
        UIElement header = StaffUi.header("gui.roll_mod.dailies.admin.bonus");
        header.layout(l -> l.marginTop(6));
        body.addScrollViewChild(header);
        body.addScrollViewChild(StaffUi.dim("gui.roll_mod.dailies.admin.bonus_hint"));
        for (int i = 0; i < bonuses.size(); i++) {
            int index = i;
            Bonus bonus = bonuses.get(i);
            UIElement line = StaffUi.row();
            line.setOverflowVisible(true);
            line.addChild(preview(bonus.payout.isEmpty() ? "" : previewSpec(bonus.payout.getFirst())));
            Button open = StaffUi.button(summary(bonus.payout), 0, () -> {
                bonusEntry = index;
                bonusStage = -1;
                rebuild = true;
            });
            open.layout(l -> l.flexBasis(0).flexGrow(1));
            StaffUi.leftCaption(open);
            Label weightLabel = StaffUi.text(Component.translatable("gui.roll_mod.dailies.admin.weight"));
            weightLabel.layout(l -> l.width(44).height(StaffUi.ROW_H).flexShrink(0));
            weightLabel.textStyle(t -> t.textAlignVertical(Vertical.CENTER));
            TextField weight = new TextField();
            weight.setText(bonus.weight);
            weight.layout(l -> l.width(36).height(StaffUi.ROW_H).flexShrink(0));
            weight.setTextResponder(t -> {
                bonus.weight = t.strip();
                dirty = true;
            });
            Button remove = StaffUi.button(Component.literal("✕"), 16, () -> {
                bonuses.remove(index);
                dirty = true;
                rebuild = true;
            });
            line.addChildren(open, weightLabel, weight, remove);
            body.addScrollViewChild(line);
        }
        Button add = StaffUi.button("gui.roll_mod.dailies.admin.bonus_add", 120, () -> {
            bonuses.add(new Bonus("bonus_" + System.currentTimeMillis() % 100000, "1", List.of()));
            dirty = true;
            bonusEntry = bonuses.size() - 1;
            bonusStage = -1;
            bonusEditing = -1;
            rebuild = true;
        });
        add.layout(l -> l.marginTop(3));
        body.addScrollViewChild(add);
    }

    /** "8-16 × Iron Ingot, 2500 starcoins…": the bonus's payout in one line. */
    private static Component summary(List<String> payout) {
        if (payout.isEmpty()) {
            return Component.translatable("gui.roll_mod.dailies.admin.bonus_empty").withStyle(ChatFormatting.GRAY);
        }
        net.minecraft.network.chat.MutableComponent out = Component.empty();
        for (int i = 0; i < payout.size(); i++) {
            if (i > 0) out.append(", ");
            out.append(describeLine(payout.get(i)));
        }
        return out;
    }

    /**
     * One bonus's card: each payout line with its amount range, and a way to add one; on the base payout, under it,
     * what it turns into at each quest stage (a click opens that stage's lines, edited the same way).
     */
    private void bonusCard() {
        Bonus bonus = bonuses.get(bonusEntry);
        List<String> payout = bonus.lines(bonusStage);
        UIElement top = StaffUi.row();
        top.setOverflowVisible(true);
        Component heading = bonusStage < 0 ? Component.translatable("gui.roll_mod.dailies.admin.bonus_card")
                : Component.translatable("gui.roll_mod.dailies.admin.from_stage", STAGES[bonusStage]);
        Label title = StaffUi.text(heading.copy().withStyle(ChatFormatting.YELLOW));
        title.layout(l -> l.flexBasis(0).flexGrow(1).height(StaffUi.ROW_H));
        title.textStyle(t -> t.textAlignVertical(Vertical.CENTER));
        top.addChildren(title, StaffUi.button("gui.roll_mod.dailies.admin.back", 60, () -> {
            // From a stage, back to the bonus; from the bonus, back to the list.
            if (bonusStage >= 0) bonusStage = -1;
            else bonusEntry = -1;
            rebuild = true;
        }));
        body.addScrollViewChild(top);
        body.addScrollViewChild(StaffUi.dim(bonusStage < 0 ? "gui.roll_mod.dailies.admin.bonus_card_hint"
                : "gui.roll_mod.dailies.admin.bonus_stage_hint"));

        for (int i = 0; i < payout.size(); i++) {
            int index = i;
            String spec = payout.get(i);
            UIElement line = StaffUi.row();
            line.setOverflowVisible(true);
            line.addChild(preview(previewSpec(spec)));
            Button pick = StaffUi.button(describeLine(spec), 0, () -> {
                bonusEditing = index;
                rebuild = true;
            });
            pick.layout(l -> l.flexBasis(0).flexGrow(1));
            StaffUi.leftCaption(pick);
            line.addChild(pick);
            if (BattlePassReward.kind(spec) == BattlePassReward.Kind.ITEM) {
                int[] range = range(spec);
                String item = itemId(spec);
                TextField min = new TextField();
                min.setText(String.valueOf(range[0]));
                min.layout(l -> l.width(28).height(StaffUi.ROW_H).flexShrink(0));
                Label dash = StaffUi.text(Component.literal("–"));
                dash.layout(l -> l.width(8).height(StaffUi.ROW_H).flexShrink(0));
                dash.textStyle(t -> t.textAlignVertical(Vertical.CENTER));
                TextField max = new TextField();
                max.setText(String.valueOf(range[1]));
                max.layout(l -> l.width(28).height(StaffUi.ROW_H).flexShrink(0));
                int[] current = range.clone();
                min.setTextResponder(t -> {
                    current[0] = parse(t, current[0]);
                    payout.set(index, line(item, current[0], current[1]));
                    dirty = true;
                });
                max.setTextResponder(t -> {
                    current[1] = parse(t, current[1]);
                    payout.set(index, line(item, current[0], current[1]));
                    dirty = true;
                });
                line.addChildren(min, dash, max);
            }
            line.addChild(StaffUi.button(Component.literal("✕"), 16, () -> {
                payout.remove(index);
                dirty = true;
                rebuild = true;
            }));
            body.addScrollViewChild(line);
        }
        Button add = StaffUi.button("gui.roll_mod.dailies.admin.bonus_add_line", 120, () -> {
            bonusEditing = -1;
            rebuild = true;
        });
        add.layout(l -> l.marginTop(3));
        body.addScrollViewChild(add);

        if (bonusStage < 0) {
            // What it turns into at each quest stage, as the pool's rewards do.
            body.addScrollViewChild(StaffUi.header("gui.roll_mod.dailies.admin.stages"));
            body.addScrollViewChild(StaffUi.dim("gui.roll_mod.dailies.admin.bonus_stages_hint"));
            List<String> shown = bonus.payout;
            for (int st = 0; st < bonus.stages.size(); st++) {
                int index = st;
                List<String> own = bonus.stages.get(st);
                if (!own.isEmpty()) shown = own;
                UIElement line = StaffUi.row();
                line.setOverflowVisible(true);
                line.addChild(stageLabel(STAGES[st]));
                line.addChild(preview(shown.isEmpty() ? "" : previewSpec(shown.getFirst())));
                Component text = own.isEmpty()
                        ? Component.translatable("gui.roll_mod.dailies.admin.same", summary(shown)).withStyle(ChatFormatting.DARK_GRAY)
                        : summary(own);
                Button open = StaffUi.button(text, 0, () -> {
                    bonusStage = index;
                    // A stage of its own starts from what it carried over, to change from there.
                    if (bonus.stages.get(index).isEmpty()) {
                        List<String> carried = bonus.payout;
                        for (int k = 0; k < index; k++) if (!bonus.stages.get(k).isEmpty()) carried = bonus.stages.get(k);
                        bonus.stages.get(index).addAll(carried);
                        dirty = true;
                    }
                    rebuild = true;
                });
                open.layout(l -> l.flexBasis(0).flexGrow(1));
                StaffUi.leftCaption(open);
                line.addChild(open);
                if (!own.isEmpty()) {
                    line.addChild(StaffUi.button(Component.literal("✕"), 16, () -> {
                        bonus.stages.get(index).clear();
                        dirty = true;
                        rebuild = true;
                    }));
                }
                body.addScrollViewChild(line);
            }
        }
        body.addScrollViewChild(saveRow());
    }

    /** The picker for one payout line: an item (its range kept) or starcoins. */
    private void bonusPicker() {
        Bonus bonus = bonuses.get(bonusEntry);
        List<String> payout = bonus.lines(bonusStage);
        int target = bonusEditing;
        String current = target >= 0 && target < payout.size() ? payout.get(target) : "minecraft:diamond*1";
        UIElement holder = new UIElement();
        holder.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100));
        holder.setOverflowVisible(true);
        body.addScrollViewChild(holder);
        new BattlePassRewardEditor(holder, Component.translatable("gui.roll_mod.dailies.admin.bonus_card"),
                previewSpec(current), false, picked -> {
            String line = null;
            if (BattlePassReward.kind(picked) == BattlePassReward.Kind.MONEY && BattlePassReward.money(picked) > 0) {
                line = picked.strip();
            } else if (BattlePassReward.kind(picked) == BattlePassReward.Kind.ITEM && !BattlePassReward.stack(picked).isEmpty()) {
                // A new item keeps the line's range; a fresh line takes the picked count.
                int[] range = target >= 0 && BattlePassReward.kind(current) == BattlePassReward.Kind.ITEM
                        ? range(current) : new int[]{BattlePassReward.stack(picked).getCount(), BattlePassReward.stack(picked).getCount()};
                line = line(itemId(picked), range[0], range[1]);
            }
            if (line != null) {
                if (target >= 0 && target < payout.size()) payout.set(target, line);
                else payout.add(line);
                dirty = true;
            }
            bonusEditing = null;
            rebuild = true;
        }, () -> {
            bonusEditing = null;
            rebuild = true;
        }).show();
    }

    /** {@code namespace:item} of an item line. */
    private static String itemId(String spec) {
        int star = spec.lastIndexOf('*');
        return (star < 0 ? spec : spec.substring(0, star)).strip();
    }

    /** {@code {min, max}} of an item line; {@code *8} is {@code 8-8}, no count is one. */
    private static int[] range(String spec) {
        int star = spec.lastIndexOf('*');
        if (star < 0) return new int[]{1, 1};
        String amount = spec.substring(star + 1).strip();
        int dash = amount.indexOf('-');
        int min = parse(dash < 0 ? amount : amount.substring(0, dash), 1);
        int max = dash < 0 ? min : parse(amount.substring(dash + 1), min);
        return new int[]{min, Math.max(min, max)};
    }

    private static int parse(String text, int fallback) {
        try {
            return Math.max(1, Integer.parseInt(text.strip()));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String line(String item, int min, int max) {
        int lo = Math.max(1, Math.min(min, max));
        int hi = Math.max(lo, max);
        return lo == hi ? item + "*" + lo : item + "*" + lo + "-" + hi;
    }

    /** A spec the battle pass's helpers read: a range collapses to its lower end. */
    private static String previewSpec(String spec) {
        if (BattlePassReward.kind(spec) != BattlePassReward.Kind.ITEM) return spec;
        return itemId(spec) + "*" + range(spec)[0];
    }

    private static Component describeLine(String spec) {
        if (BattlePassReward.kind(spec) != BattlePassReward.Kind.ITEM) return BattlePassReward.describe(spec);
        ItemStack stack = BattlePassReward.stack(previewSpec(spec));
        if (stack.isEmpty()) return Component.literal(spec).withStyle(ChatFormatting.RED);
        int[] range = range(spec);
        String amount = range[0] == range[1] ? String.valueOf(range[0]) : range[0] + "-" + range[1];
        return Component.literal(amount + " × ").append(stack.getHoverName());
    }

    /** One reward's card: its base item, then what it becomes from each quest stage on. */
    private void card() {
        String[] row = rows.get(entry);
        UIElement top = StaffUi.row();
        top.setOverflowVisible(true);
        Label title = StaffUi.text(Component.translatable("gui.roll_mod.dailies.admin.card").withStyle(ChatFormatting.YELLOW));
        title.layout(l -> l.flexBasis(0).flexGrow(1).height(StaffUi.ROW_H));
        title.textStyle(t -> t.textAlignVertical(Vertical.CENTER));
        top.addChildren(title, StaffUi.button("gui.roll_mod.dailies.admin.back", 60, () -> {
            entry = -1;
            rebuild = true;
        }));
        body.addScrollViewChild(top);

        // As the bonus card: each line an item button and its amount from–to (rolled anew each time).
        body.addScrollViewChild(StaffUi.dim("gui.roll_mod.dailies.admin.range_hint"));
        body.addScrollViewChild(rangeLine(Component.translatable("gui.roll_mod.dailies.admin.base"), row[1],
                -1, spec -> row[1] = spec, null));

        body.addScrollViewChild(StaffUi.header("gui.roll_mod.dailies.admin.stages"));
        body.addScrollViewChild(StaffUi.dim("gui.roll_mod.dailies.admin.stages_hint"));
        List<String> stages = stages(row);
        String shown = row[1];
        for (int st = 0; st < com.roll_54.roll_mod.minestar.dailytasks.TaskRewardPool.STAGES; st++) {
            int index = st;
            String own = st < stages.size() ? stages.get(st) : "";
            if (!own.isBlank()) shown = own;
            if (own.isBlank()) {
                // What it carries over from the stage before (dimmed); a click gives this stage its own.
                UIElement line = StaffUi.row();
                line.setOverflowVisible(true);
                line.addChild(stageLabel(STAGES[st]));
                line.addChild(preview(previewSpec(shown)));
                Button pick = StaffUi.button(Component.translatable("gui.roll_mod.dailies.admin.same", describeLine(shown))
                        .withStyle(ChatFormatting.DARK_GRAY), 0, () -> {
                    editing = index;
                    rebuild = true;
                });
                pick.layout(l -> l.flexBasis(0).flexGrow(1));
                StaffUi.leftCaption(pick);
                line.addChild(pick);
                body.addScrollViewChild(line);
            } else {
                body.addScrollViewChild(rangeLine(Component.literal(STAGES[st]), own, index,
                        spec -> setStage(row, index, spec), () -> {
                            setStage(row, index, "");
                            rebuild = true;
                        }));
            }
        }
        body.addScrollViewChild(saveRow());
    }

    private static Label stageLabel(String text) {
        Label label = StaffUi.text(Component.literal(text).withStyle(ChatFormatting.GRAY));
        label.layout(l -> l.width(60).height(StaffUi.ROW_H).flexShrink(0));
        label.textStyle(t -> t.textAlignVertical(Vertical.CENTER));
        return label;
    }

    /**
     * One card line as on the bonus card: its label, the item (a click opens the picker on {@code target}), the
     * amount from–to, and ✕ when {@code remove} is given.
     */
    private UIElement rangeLine(Component labelText, String spec, int target,
                                java.util.function.Consumer<String> set, @javax.annotation.Nullable Runnable remove) {
        UIElement line = StaffUi.row();
        line.setOverflowVisible(true);
        Label label = StaffUi.text(labelText.copy().withStyle(ChatFormatting.GRAY));
        label.layout(l -> l.width(60).height(StaffUi.ROW_H).flexShrink(0));
        label.textStyle(t -> t.textAlignVertical(Vertical.CENTER));
        line.addChildren(label, preview(previewSpec(spec)));
        Button pick = StaffUi.button(describeLine(spec), 0, () -> {
            editing = target;
            rebuild = true;
        });
        pick.layout(l -> l.flexBasis(0).flexGrow(1));
        StaffUi.leftCaption(pick);
        line.addChild(pick);
        int[] current = range(spec).clone();
        String item = itemId(spec);
        TextField min = new TextField();
        min.setText(String.valueOf(current[0]));
        min.layout(l -> l.width(28).height(StaffUi.ROW_H).flexShrink(0));
        Label dash = StaffUi.text(Component.literal("–"));
        dash.layout(l -> l.width(8).height(StaffUi.ROW_H).flexShrink(0));
        dash.textStyle(t -> t.textAlignVertical(Vertical.CENTER));
        TextField max = new TextField();
        max.setText(String.valueOf(current[1]));
        max.layout(l -> l.width(28).height(StaffUi.ROW_H).flexShrink(0));
        min.setTextResponder(t -> {
            current[0] = parse(t, current[0]);
            set.accept(line(item, current[0], current[1]));
            dirty = true;
        });
        max.setTextResponder(t -> {
            current[1] = parse(t, current[1]);
            set.accept(line(item, current[0], current[1]));
            dirty = true;
        });
        line.addChildren(min, dash, max);
        if (remove != null) line.addChild(StaffUi.button(Component.literal("✕"), 16, remove));
        return line;
    }

    /** The item picker, for the card's base item or one of its stages. */
    private void itemPicker() {
        String[] row = rows.get(entry);
        int target = editing;
        List<String> stages = stages(row);
        String spec = target < 0 ? row[1] : target < stages.size() && !stages.get(target).isBlank() ? stages.get(target) : row[1];
        UIElement holder = new UIElement();
        holder.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100));
        holder.setOverflowVisible(true);
        body.addScrollViewChild(holder);
        Component heading = target < 0 ? Component.translatable("gui.roll_mod.dailies.admin.base")
                : Component.translatable("gui.roll_mod.dailies.admin.from_stage", STAGES[target]);
        new BattlePassRewardEditor(holder, heading, previewSpec(spec), false, picked -> {
            // The daily pool pays items only. A new item keeps the line's from–to.
            if (BattlePassReward.kind(picked) == BattlePassReward.Kind.ITEM && !BattlePassReward.stack(picked).isEmpty()) {
                int[] range = range(spec);
                String line = line(itemId(picked), range[0], range[1]);
                if (target < 0) row[1] = line;
                else setStage(row, target, line);
                dirty = true;
            }
            editing = null;
            rebuild = true;
        }, () -> {
            editing = null;
            rebuild = true;
        }).show();
    }

    private static List<String> stages(String[] row) {
        return row.length > 3 && !row[3].isBlank() ? new ArrayList<>(List.of(row[3].split(",", -1))) : new ArrayList<>();
    }

    private void setStage(String[] row, int stage, String spec) {
        List<String> stages = stages(row);
        while (stages.size() <= stage) stages.add("");
        stages.set(stage, spec);
        while (!stages.isEmpty() && stages.get(stages.size() - 1).isBlank()) stages.remove(stages.size() - 1);
        row[3] = String.join(",", stages);
        dirty = true;
    }

    private UIElement saveRow() {
        UIElement saveRow = StaffUi.row();
        saveRow.layout(l -> l.marginTop(6).marginBottom(2));
        saveRow.setOverflowVisible(true);
        saveRow.addChildren(
                StaffUi.button("gui.roll_mod.staff.commands.revert", 70, () -> {
                    dirty = false;
                    entry = -1;
                    bonusEntry = -1;
                    load(ClientBattlePassCache.DAILY);
                    rebuild = true;
                }),
                StaffUi.button("gui.roll_mod.hub.moderation.save", 70, () -> {
                    CompoundTag args = new CompoundTag();
                    ListTag list = new ListTag();
                    for (String[] row : rows) list.add(StringTag.valueOf(String.join("|", row)));
                    args.put("rows", list);
                    ListTag bonusList = new ListTag();
                    for (Bonus b : bonuses) {
                        int w;
                        try {
                            w = Math.max(0, Integer.parseInt(b.weight.strip()));
                        } catch (NumberFormatException e) {
                            w = 1;
                        }
                        bonusList.add(StringTag.valueOf(DailyRewardConfig.bonusRow(b.id, w, b.payout, b.stages)));
                    }
                    args.put("bonus", bonusList);
                    dirty = false;
                    PacketDistributor.sendToServer(new BattlePassPackets.Act("daily/set", args));
                }));
        return saveRow;
    }

    private static UIElement preview(String spec) {
        UIElement preview = new UIElement();
        preview.layout(l -> l.width(16).height(16).flexShrink(0));
        ItemStack stack = BattlePassReward.stack(spec);
        // Money has no item: the starcoin, as on the battle pass.
        IGuiTexture icon = !stack.isEmpty() ? new ItemStackTexture(stack)
                : BattlePassReward.kind(spec) == BattlePassReward.Kind.MONEY
                ? com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture.of(com.roll_54.roll_mod.RollMod.id("textures/item/lp.png"))
                : IGuiTexture.EMPTY;
        preview.style(s -> s.background(icon));
        return preview;
    }
}
