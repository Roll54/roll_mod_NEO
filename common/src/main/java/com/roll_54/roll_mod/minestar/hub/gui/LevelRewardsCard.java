package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.roll_54.roll_mod.minestar.friends.ClientFriendsCache;
import com.roll_54.roll_mod.minestar.friends.FriendDetail;
import com.roll_54.roll_mod.minestar.level.ClientLevelRewards;
import com.roll_54.roll_mod.minestar.level.LevelPerks;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The home tab's level rewards, as Eden's profile shows them: what the player's Minestar level gives now (emoji and
 * sticker slots, Open Parties and Claims bonus chunks, free daily rerolls), what the next perk level brings, a track of
 * the perk levels around theirs, and the rewards earned on the Minestar site — a framed star per reward in its
 * rarity's colour, with title, description and rarity on hover. Rebuilt only when the data changes.
 */
final class LevelRewardsCard {

    /** Common, rare, epic, legendary. */
    private static final int[] RARITY = {0xFFB0B0B8, 0xFF4DA3FF, 0xFFB45CFF, 0xFFFFB32E};
    private static final String[] RARITY_KEYS = {"common", "rare", "epic", "legendary"};
    private static final int REWARD = 18;
    private static final int COLOR_CARD = 0x30FFFFFF;
    private static final int COLOR_REACHED = 0xFF6BD66B;

    private LevelRewardsCard() {}

    static UIElement build(Player player) {
        UIElement card = new UIElement();
        card.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).paddingAll(3).gapRow(2).marginTop(2)
                .flexShrink(0));
        card.style(s -> s.background(new ColorRectTexture(COLOR_CARD)));
        String[] shown = {null};
        card.addEventListener(UIEvents.TICK, e -> {
            FriendDetail own = ClientFriendsCache.DETAILS.get(player.getUUID());
            String sig = ClientLevelRewards.VERSION + "|" + (own == null ? -1 : own.level()) + "|"
                    + (own == null ? 0 : own.rewards().size());
            if (sig.equals(shown[0])) return;
            shown[0] = sig;
            fill(card, own);
        });
        return card;
    }

    private static void fill(UIElement card, FriendDetail own) {
        card.clearAllChildren();
        List<LevelPerks.Rule> rules = ClientLevelRewards.RULES;
        int level = own != null ? own.level() : ClientLevelRewards.LEVEL;

        card.addChild(caption(Component.translatable("gui.roll_mod.hub.home.perks")));
        Map<LevelPerks.Kind, Integer> totals = LevelPerks.totals(rules, level);
        card.addChild(text(totals.isEmpty() ? Component.translatable("gui.roll_mod.hub.home.perks.none")
                : Component.translatable("gui.roll_mod.hub.home.perks.current", LevelPerks.describe(totals)), 0xFFFFFFFF));
        int next = LevelPerks.nextPerkLevel(rules, level);
        // The next perk is the highlighted chip below; a line of its own only once there is none.
        if (next <= 0) card.addChild(text(Component.translatable("gui.roll_mod.hub.home.perks.max"), 0xFFAAAAAA));

        // The last two perk levels reached, then the next four.
        List<Integer> track = new ArrayList<>();
        for (int at = level; at >= 1 && track.size() < 2; at--) {
            if (!LevelPerks.at(rules, at).isEmpty()) track.add(0, at);
        }
        int reached = track.size();
        for (int at = next; at > 0 && track.size() < reached + 4; at = LevelPerks.nextPerkLevel(rules, at)) track.add(at);
        if (!track.isEmpty()) {
            UIElement chips = new UIElement();
            chips.layout(l -> l.flexDirection(FlexDirection.ROW).flexWrap(FlexWrap.WRAP).widthPercent(100).gapColumn(3).gapRow(3));
            for (int at : track) chips.addChild(chip(rules, at, at <= level, at == next));
            card.addChild(chips);
        }

        if (own != null) {
            card.addChild(caption(Component.translatable("gui.roll_mod.hub.home.rewards", own.rewards().size())));
            if (own.rewards().isEmpty()) {
                card.addChild(text(Component.translatable("gui.roll_mod.hub.home.rewards.none"), 0xFFAAAAAA));
            } else {
                UIElement grid = new UIElement();
                grid.layout(l -> l.flexDirection(FlexDirection.ROW).flexWrap(FlexWrap.WRAP).widthPercent(100).gapColumn(3).gapRow(3));
                for (FriendDetail.Reward reward : own.rewards()) grid.addChild(reward(reward));
                card.addChild(grid);
            }
        }
    }

    private static UIElement chip(List<LevelPerks.Rule> rules, int level, boolean reached, boolean next) {
        int border = reached ? COLOR_REACHED : next ? 0xFFB45CFF : 0x50FFFFFF;
        Label chip = new Label();
        chip.setText((reached ? Component.literal("✔ ") : Component.empty()).copy()
                .append(Component.translatable("gui.roll_mod.hub.home.perks.level", level)).append(" · ")
                .append(LevelPerks.describeAt(rules, level)));
        chip.layout(l -> l.height(12).paddingHorizontal(4).flexShrink(0));
        chip.textStyle(t -> t.textAlignVertical(Vertical.CENTER).adaptiveWidth(true)
                .textColor(reached ? COLOR_REACHED : next ? 0xFFFFFFFF : 0xFFAAAAAA));
        chip.style(s -> s.background(new GuiTextureGroup(new ColorRectTexture(reached ? 0x3040C050 : 0x30000000),
                new ColorBorderTexture(1, border))));
        return chip;
    }

    /** One site reward: a star in its rarity's colour, framed; title, description and rarity on hover. */
    private static UIElement reward(FriendDetail.Reward reward) {
        int rarity = Math.floorMod(reward.rarity(), RARITY.length);
        int color = RARITY[rarity];
        Label icon = new Label();
        icon.setText(Component.literal("★"));
        icon.layout(l -> l.width(REWARD).height(REWARD));
        icon.textStyle(t -> t.textAlignHorizontal(Horizontal.CENTER).textAlignVertical(Vertical.CENTER).textColor(color));
        icon.style(s -> s.background(new GuiTextureGroup(new ColorRectTexture(0x70000000), new ColorBorderTexture(1, color))));
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(reward.title()).withColor(color & 0xFFFFFF));
        if (!reward.description().isBlank()) lines.add(Component.literal(reward.description()).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("gui.roll_mod.hub.home.rarity." + RARITY_KEYS[rarity]).withStyle(ChatFormatting.DARK_GRAY));
        icon.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(lines, null, null, ItemStack.EMPTY));
        return icon;
    }

    private static Label caption(Component text) {
        Label label = new Label();
        label.setText(text.copy().withStyle(ChatFormatting.BOLD));
        label.layout(l -> l.widthPercent(100).height(10));
        label.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textColor(0xFFAAAAAA));
        return label;
    }

    private static Label text(Component text, int color) {
        Label label = new Label();
        label.setText(text);
        label.layout(l -> l.widthPercent(100));
        label.textStyle(t -> t.textWrap(TextWrap.WRAP).adaptiveHeight(true).textColor(color));
        return label;
    }
}
