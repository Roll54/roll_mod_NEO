package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.roll_54.roll_mod.cosmetics.ItemSkinDefinition;
import com.roll_54.roll_mod.cosmetics.ItemSkinRegistry;
import com.roll_54.roll_mod.minestar.battlepass.BattlePassReward;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Edits one battle-pass reward in place of the reward list: what kind it is (an item, money, a skin, or a choice of
 * several), and its value — an item picked from a searchable grid (roll_mod's own first, then Modern Industrialization,
 * then vanilla and the rest) or taken from the hand, an amount, a skin from the list or a random one. A choice lists its
 * options, each edited the same way.
 */
final class BattlePassRewardEditor {

    private static final int CELL = 18;
    private static final int GRID_MAX = 160;
    private static List<Item> sorted;

    private final UIElement into;
    private final Component heading;
    private final boolean allowChoice;
    private final Consumer<String> done;
    private final Runnable cancel;
    private String spec;
    /** While an option of a choice is being edited: its index and the options so far. */
    private int option = -1;
    private List<String> options = new ArrayList<>();

    BattlePassRewardEditor(UIElement into, Component heading, String spec, boolean allowChoice,
                           Consumer<String> done, Runnable cancel) {
        this.into = into;
        this.heading = heading;
        this.spec = spec == null ? "" : spec;
        this.allowChoice = allowChoice;
        this.done = done;
        this.cancel = cancel;
        if (BattlePassReward.kind(this.spec) == BattlePassReward.Kind.CHOICE) options = new ArrayList<>(BattlePassReward.options(this.spec));
    }

    /** A reward's 16px picture: the item, the starcoin for money, the skins tab's icon for a skin. */
    static com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture icon(String spec) {
        return switch (BattlePassReward.kind(spec)) {
            case MONEY -> com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture.of(com.roll_54.roll_mod.RollMod.id("textures/item/lp.png"));
            case SKIN -> com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture.of(com.roll_54.roll_mod.RollMod.id("textures/gui/hub/skins_tab.png"));
            default -> {
                ItemStack stack = BattlePassReward.stack(spec);
                yield stack.isEmpty() ? com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture.EMPTY : new ItemStackTexture(stack);
            }
        };
    }

    void show() {
        into.clearAllChildren();
        if (option >= 0) {
            // An option of the choice, with an editor of its own; back here when it is done.
            int index = option;
            new BattlePassRewardEditor(into, Component.translatable("gui.roll_mod.battlepass.admin.option", index + 1),
                    index < options.size() ? options.get(index) : "", false, s -> {
                if (index < options.size()) options.set(index, s);
                else options.add(s);
                spec = BattlePassReward.choice(options);
                option = -1;
                show();
            }, () -> {
                option = -1;
                show();
            }).show();
            return;
        }

        UIElement top = StaffUi.row();
        Label title = StaffUi.text(heading.copy().withStyle(ChatFormatting.YELLOW));
        title.layout(l -> l.flexBasis(0).flexGrow(1).height(StaffUi.ROW_H));
        title.textStyle(t -> t.textAlignVertical(Vertical.CENTER));
        top.addChildren(title,
                StaffUi.button("gui.roll_mod.battlepass.admin.cancel", 60, cancel),
                StaffUi.button("gui.roll_mod.battlepass.admin.done", 60, () -> done.accept(spec.strip())));
        top.setOverflowVisible(true);
        into.addChild(top);

        Label now = StaffUi.text(Component.translatable("gui.roll_mod.battlepass.admin.current")
                .append(" ").append(BattlePassReward.describe(spec)));
        now.layout(l -> l.marginTop(3));
        into.addChild(now);

        // The kind.
        BattlePassReward.Kind kind = BattlePassReward.kind(spec);
        UIElement kinds = StaffUi.row();
        kinds.setOverflowVisible(true);
        for (BattlePassReward.Kind k : BattlePassReward.Kind.values()) {
            if (k == BattlePassReward.Kind.CHOICE && !allowChoice) continue;
            kinds.addChild(StaffUi.button(Component.translatable("gui.roll_mod.battlepass.admin.kind." + k.name().toLowerCase(Locale.ROOT))
                    .withStyle(k == kind ? ChatFormatting.YELLOW : ChatFormatting.WHITE), 70, () -> {
                if (k == kind) return;
                spec = switch (k) {
                    case ITEM -> "minecraft:diamond*1";
                    case MONEY -> BattlePassReward.MONEY + "1000";
                    case SKIN -> BattlePassReward.SKIN + "random";
                    case CHOICE -> {
                        options = new ArrayList<>(spec.isBlank() ? List.of() : List.of(spec));
                        yield BattlePassReward.choice(options);
                    }
                };
                show();
            }));
        }
        into.addChild(kinds);

        switch (kind) {
            case ITEM -> itemBody();
            case MONEY -> moneyBody();
            case SKIN -> skinBody();
            case CHOICE -> choiceBody();
        }
    }

    /* ------------------------------------------------------- item ------------------------------------------------------ */

    private void itemBody() {
        ItemStack current = BattlePassReward.stack(spec);
        int[] count = {current.isEmpty() ? 1 : current.getCount()};
        String[] id = {current.isEmpty() ? "minecraft:diamond" : BuiltInRegistries.ITEM.getKey(current.getItem()).toString()};

        UIElement row = StaffUi.row();
        row.setOverflowVisible(true);
        Label countLabel = StaffUi.text(Component.translatable("gui.roll_mod.battlepass.admin.count"));
        countLabel.layout(l -> l.width(60).height(StaffUi.ROW_H).flexShrink(0));
        countLabel.textStyle(t -> t.textAlignVertical(Vertical.CENTER));
        TextField countField = new TextField();
        countField.setText(String.valueOf(count[0]));
        countField.layout(l -> l.width(60).height(StaffUi.ROW_H));
        countField.setTextResponder(t -> {
            try {
                count[0] = Math.max(1, Integer.parseInt(t.strip()));
                spec = id[0] + "*" + count[0];
            } catch (NumberFormatException ignored) {
            }
        });
        Button hand = StaffUi.button("gui.roll_mod.battlepass.admin.hand", 70, () -> {
            var player = net.minecraft.client.Minecraft.getInstance().player;
            if (player == null || player.getMainHandItem().isEmpty()) return;
            ItemStack held = player.getMainHandItem();
            spec = BuiltInRegistries.ITEM.getKey(held.getItem()) + "*" + held.getCount();
            show();
        });
        row.addChildren(countLabel, countField, hand);
        into.addChild(row);

        TextField search = new TextField();
        search.layout(l -> l.widthPercent(100).height(StaffUi.ROW_H).marginTop(3));
        into.addChild(StaffUi.fieldLabel("gui.roll_mod.battlepass.admin.search"));
        into.addChild(search);

        UIElement grid = new UIElement();
        grid.layout(l -> l.flexDirection(FlexDirection.ROW).flexWrap(FlexWrap.WRAP).widthPercent(100).marginTop(3));
        grid.setOverflowVisible(true);
        into.addChild(grid);
        Consumer<String> fill = query -> {
            grid.clearAllChildren();
            String q = query.strip().toLowerCase(Locale.ROOT);
            int shown = 0;
            for (Item item : items()) {
                if (shown >= GRID_MAX) break;
                ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
                if (!q.isEmpty() && !key.toString().contains(q)
                        && !item.getDescription().getString().toLowerCase(Locale.ROOT).contains(q)) continue;
                shown++;
                ItemStack stack = new ItemStack(item);
                UIElement cell = new UIElement();
                boolean picked = key.toString().equals(id[0]);
                cell.layout(l -> l.width(CELL).height(CELL).paddingAll(1));
                cell.style(s -> s.background(new ColorRectTexture(picked ? 0x80FFC040 : 0x30000000)));
                UIElement icon = new UIElement();
                icon.layout(l -> l.width(16).height(16));
                icon.style(s -> s.background(new ItemStackTexture(stack)));
                icon.setAllowHitTest(false);
                cell.addChild(icon);
                cell.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(List.of(
                        stack.getHoverName(), Component.literal(key.toString()).withStyle(ChatFormatting.DARK_GRAY)),
                        null, null, ItemStack.EMPTY));
                cell.addEventListener(UIEvents.CLICK, e -> {
                    spec = key + "*" + count[0];
                    show();
                });
                grid.addChild(cell);
            }
        };
        search.setTextResponder(fill::accept);
        fill.accept("");
    }

    /** Every item, roll_mod's first, then Modern Industrialization's, then vanilla, then the rest; built once. */
    private static List<Item> items() {
        if (sorted == null) {
            List<Item> all = new ArrayList<>();
            for (Item item : BuiltInRegistries.ITEM) if (item != Items.AIR) all.add(item);
            all.sort(Comparator.<Item>comparingInt(i -> rank(BuiltInRegistries.ITEM.getKey(i).getNamespace()))
                    .thenComparing(i -> BuiltInRegistries.ITEM.getKey(i).toString()));
            sorted = List.copyOf(all);
        }
        return sorted;
    }

    private static int rank(String namespace) {
        return switch (namespace) {
            case "roll_mod" -> 0;
            case "modern_industrialization" -> 1;
            case "minecraft" -> 2;
            default -> 3;
        };
    }

    /* ------------------------------------------------------ money ------------------------------------------------------ */

    private void moneyBody() {
        UIElement row = StaffUi.row();
        Label label = StaffUi.text(Component.translatable("gui.roll_mod.battlepass.admin.amount"));
        label.layout(l -> l.width(60).height(StaffUi.ROW_H).flexShrink(0));
        label.textStyle(t -> t.textAlignVertical(Vertical.CENTER));
        TextField field = new TextField();
        field.setText(String.valueOf(BattlePassReward.money(spec)));
        field.layout(l -> l.width(100).height(StaffUi.ROW_H));
        field.setTextResponder(t -> {
            try {
                spec = BattlePassReward.MONEY + Math.max(1, Long.parseLong(t.strip()));
            } catch (NumberFormatException ignored) {
            }
        });
        row.addChildren(label, field);
        into.addChild(row);
    }

    /* ------------------------------------------------------- skin ------------------------------------------------------ */

    private void skinBody() {
        into.addChild(pickRow(Component.translatable("gui.roll_mod.battlepass.reward.random_skin"),
                spec.equals(BattlePassReward.SKIN + "random"), () -> spec = BattlePassReward.SKIN + "random"));
        for (ItemSkinDefinition skin : ItemSkinRegistry.all()) {
            String value = BattlePassReward.SKIN + skin.skinId();
            into.addChild(pickRow(skin.displayName(), spec.equals(value), () -> spec = value));
        }
    }

    private UIElement pickRow(Component name, boolean picked, Runnable pick) {
        Button button = StaffUi.button(name.copy().withStyle(picked ? ChatFormatting.YELLOW : ChatFormatting.WHITE), 0, () -> {
            pick.run();
            show();
        });
        button.layout(l -> l.widthPercent(100).marginTop(3));
        return button;
    }

    /* ------------------------------------------------------ choice ----------------------------------------------------- */

    private void choiceBody() {
        into.addChild(StaffUi.dim("gui.roll_mod.battlepass.admin.choice_hint"));
        for (int i = 0; i < options.size(); i++) {
            int index = i;
            UIElement row = StaffUi.row();
            row.setOverflowVisible(true);
            Label label = StaffUi.text(Component.literal((i + 1) + ". ").append(BattlePassReward.describe(options.get(i))));
            label.layout(l -> l.flexBasis(0).flexGrow(1).height(StaffUi.ROW_H));
            label.textStyle(t -> t.textAlignVertical(Vertical.CENTER));
            row.addChildren(label,
                    StaffUi.button("gui.roll_mod.battlepass.admin.edit", 60, () -> {
                        option = index;
                        show();
                    }),
                    StaffUi.button(Component.literal("✕"), 16, () -> {
                        options.remove(index);
                        spec = BattlePassReward.choice(options);
                        show();
                    }));
            into.addChild(row);
        }
        if (options.size() < BattlePassReward.MAX_OPTIONS) {
            Button add = StaffUi.button("gui.roll_mod.battlepass.admin.add_option", 120, () -> {
                option = options.size();
                show();
            });
            add.layout(l -> l.marginTop(3));
            into.addChild(add);
        }
    }
}
