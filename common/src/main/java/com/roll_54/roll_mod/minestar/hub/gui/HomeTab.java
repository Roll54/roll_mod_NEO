package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SimpleBinding;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.economy.currency.model.CurrencyType;
import com.roll_54.roll_mod.economy.network.client.ClientCurrencyHolder;
import com.roll_54.roll_mod.economy.util.EnergyFormatUtils;
import com.roll_54.roll_mod.minestar.hub.PlayerTier;
import com.roll_54.roll_mod.minestar.hub.QuestProgress;
import com.roll_54.roll_mod.minestar.hub.RankResolver;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * The hub's home tab: the player's own model on the left, their status on the right.
 *
 * <p>Everything the server knows and the client does not — rank, tier, quest counts — crosses as an
 * LdLib2 server→client binding, the same mechanism the daily-tasks screen uses, so no new packets
 * are needed. The starcoin balance is the exception: {@code ClientCurrencyHolder} is already a live
 * client-side cache kept fresh by the currency payloads, so it is read directly.
 *
 * <p>Like every other tab, this is built on both sides and the bindings are positional, so the tree
 * must not vary with dist or player state.
 */
public final class HomeTab {

    private static final int PADDING = 8;
    private static final int PREVIEW_W = 110;
    private static final int ROW_H = 14;
    private static final int ROW_GAP = 6;
    private static final int ICON = 12;

    /** Roughly head-to-toe for the preview box below; vanilla's inventory doll uses 30. */
    private static final int MODEL_SCALE = 45;

    private static final int COLOR_PANEL = 0x40000000;

    private HomeTab() {}

    public static UIElement build(Player player) {
        ServerPlayer server = player instanceof ServerPlayer sp ? sp : null;

        // A column now, so the section title can sit above what used to be the whole tab. The old
        // row lives on as `body`, unchanged apart from taking its height from the leftover space.
        UIElement root = new UIElement();
        root.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).heightPercent(100)
                .paddingAll(PADDING));

        HubSection.Info section = HubSection.info("home");

        UIElement body = new UIElement();
        body.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).flexGrow(1)
                .gapColumn(PADDING));
        // The doll is told when the information window is up, because it draws over everything —
        // see PlayerPreviewElement's class javadoc.
        body.addChildren(preview(() -> section.overlay().isDisplayed()), info(player, server));

        // The panel last, so it covers the body when opened.
        root.addChildren(HubSection.header("home", section), body, section.overlay());
        return root;
    }

    /* ------------------------------------------- the model ------------------------------------------ */

    private static UIElement preview(BooleanSupplier suppressed) {
        UIElement frame = new UIElement();
        frame.layout(l -> l.width(PREVIEW_W).heightPercent(100));
        frame.style(s -> s.background(new ColorRectTexture(COLOR_PANEL)));

        PlayerPreviewElement doll = new PlayerPreviewElement(MODEL_SCALE, suppressed);
        doll.layout(l -> l.widthPercent(100).heightPercent(100));
        frame.addChild(doll);

        return frame;
    }

    /* -------------------------------------------- the info ------------------------------------------ */

    private static UIElement info(Player player, @Nullable ServerPlayer server) {
        // One binding per value. Ints and Components only — the client resolves the wording itself.
        SimpleBinding<Component> rank = DataBindingBuilder
                .componentS2C(supplier(server, p -> RankResolver.displayName(RankResolver.rankId(p)),
                        RankResolver.displayName(RankResolver.DEFAULT_RANK)))
                .initialValue(RankResolver.displayName(RankResolver.DEFAULT_RANK)).build();
        SimpleBinding<Integer> tier = intBinding(supplier(server, PlayerTier::of, 0), 0);
        SimpleBinding<Integer> questsDone =
                intBinding(supplier(server, p -> QuestProgress.of(p).requiredDone(), 0), 0);
        SimpleBinding<Integer> questsTotal =
                intBinding(supplier(server, p -> QuestProgress.of(p).requiredTotal(), 0), 0);
        SimpleBinding<Integer> optionalDone =
                intBinding(supplier(server, p -> QuestProgress.of(p).optionalDone(), 0), 0);
        SimpleBinding<Integer> optionalTotal =
                intBinding(supplier(server, p -> QuestProgress.of(p).optionalTotal(), 0), 0);

        UIElement column = new UIElement();
        column.layout(l -> l.flexDirection(FlexDirection.COLUMN).flexGrow(1).heightPercent(100)
                .gapRow(ROW_GAP));
        column.addSyncValue(rank.getSyncValue());
        column.addSyncValue(tier.getSyncValue());
        column.addSyncValue(questsDone.getSyncValue());
        column.addSyncValue(questsTotal.getSyncValue());
        column.addSyncValue(optionalDone.getSyncValue());
        column.addSyncValue(optionalTotal.getSyncValue());

        Label nickname = row(column, null);
        nickname.setText(Component.literal(player.getGameProfile().getName())
                .withStyle(ChatFormatting.WHITE));

        Label rankRow = row(column, "gui.roll_mod.hub.home.rank.tip");
        Label coinsRow = row(column, "gui.roll_mod.hub.home.starcoins.tip");
        UIElement coinsIcon = new UIElement();
        coinsIcon.layout(l -> l.width(ICON).height(ICON));
        coinsIcon.style(s -> s.background(SpriteTexture.of(RollMod.id("textures/item/lp.png"))));

        Label questsRow = row(column, null);
        Label tierRow = row(column, "gui.roll_mod.hub.home.tier.tip");

        column.addEventListener(UIEvents.TICK, e -> {
            rankRow.setText(Component.translatable("gui.roll_mod.hub.home.rank",
                    value(rank, Component.empty())));
            coinsRow.setText(Component.translatable("gui.roll_mod.hub.home.starcoins",
                    EnergyFormatUtils.formatEnergy(ClientCurrencyHolder.get(CurrencyType.MAIN))));
            questsRow.setText(Component.translatable("gui.roll_mod.hub.home.quests",
                    value(questsDone, 0), value(questsTotal, 0)));
            tierRow.setText(Component.translatable("gui.roll_mod.hub.home.tier",
                    PlayerTier.name(value(tier, 0))));
        });

        // The optional count is a footnote to the quest row rather than a row of its own.
        questsRow.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(
                List.of(Component.translatable("gui.roll_mod.hub.home.quests.tip"),
                        Component.translatable("gui.roll_mod.hub.home.quests.optional",
                                value(optionalDone, 0), value(optionalTotal, 0))
                                .withStyle(ChatFormatting.GRAY)),
                null, null, ItemStack.EMPTY));

        return column;
    }

    /** A status line, with an optional hover explanation. */
    private static Label row(UIElement parent, @Nullable String tooltipKey) {
        Label label = new Label();
        label.layout(l -> l.widthPercent(100).height(ROW_H));
        label.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textShadow(true));
        if (tooltipKey != null) {
            label.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(
                    List.of(Component.translatable(tooltipKey)), null, null, ItemStack.EMPTY));
        }
        parent.addChild(label);
        return label;
    }

    /* ------------------------------------------- plumbing ------------------------------------------- */

    private static SimpleBinding<Integer> intBinding(Supplier<Integer> supplier, int initial) {
        return DataBindingBuilder.intValS2C(supplier).initialValue(initial).build();
    }

    /**
     * Wraps a server-side lookup as a binding supplier. On the client {@code server} is null and the
     * supplier is never meaningfully consulted, so the fallback stands in.
     */
    private static <T> Supplier<T> supplier(@Nullable ServerPlayer server,
                                            java.util.function.Function<ServerPlayer, T> mapper,
                                            T fallback) {
        return () -> server == null ? fallback : mapper.apply(server);
    }

    private static <T> T value(SimpleBinding<T> binding, T fallback) {
        T v = binding.getSyncValue().getValue();
        return v == null ? fallback : v;
    }
}
