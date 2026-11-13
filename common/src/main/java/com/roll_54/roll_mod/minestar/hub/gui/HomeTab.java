package com.roll_54.roll_mod.minestar.hub.gui;

import net.neoforged.fml.loading.FMLEnvironment;
import net.minecraft.network.chat.MutableComponent;
import com.roll_54.roll_mod.minestar.friends.ClientFriendsCache;
import com.roll_54.roll_mod.minestar.friends.FriendDetail;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
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
import com.roll_54.roll_mod.minestar.moderation.ClientModerationCache;
import com.roll_54.roll_mod.minestar.moderation.ClientPlayerStatusCache;
import com.roll_54.roll_mod.minestar.moderation.ModerationService;
import com.roll_54.roll_mod.util.Durations;
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
 * The hub's home tab: the player's own model on the left, their status beside it, and their kits
 * and the argument-less trips on the right.
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
    /** The teleports column on the left, where the model was. */
    private static final int TELEPORTS_W = 150;
    private static final int ROW_H = 12;
    private static final int ROW_GAP = 2;
    private static final int ICON = 12;

    /** Roughly head-to-toe for the preview box below; vanilla's inventory doll uses 30. */
    private static final int MODEL_SCALE = 22;

    private static final int COLOR_PANEL = 0x40000000;

    private HomeTab() {}

    public static UIElement build(Player player) {
        ServerPlayer server = player instanceof ServerPlayer sp ? sp : null;

        // A column now, so the section title can sit above what used to be the whole tab. The old
        // row lives on as `body`, unchanged apart from taking its height from the leftover space.
        UIElement root = new UIElement();
        // The shared tab inset, so the title row lines up with every other tab; PADDING stays the
        // gap between the columns.
        root.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).heightPercent(100)
                .paddingAll(HubSection.TAB_PADDING));

        HubSection.Info section = HubSection.info("home");
        // The money history, from a button beside the I. Its panel is added last, like every overlay.
        HubSection.Info history = HistoryPanel.build();

        UIElement body = new UIElement();
        body.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).flexGrow(1)
                .gapColumn(PADDING));
        // The doll is told when the information window is up, because it draws over everything —
        // see PlayerPreviewElement's class javadoc.
        // Appended, never inserted: info() registers six positional bindings and anything placed
        // before it would shift every one of them. The kits panel registers none of its own.
        // The quick teleports sit where the player model used to; neither has bindings, so info()'s stay put.
        body.addChildren(leftColumn(() -> section.overlay().isDisplayed()), info(player, server));

        // The panel last, so it covers the body when opened.
        root.addChildren(HubSection.header("home", section, history.button()), body, section.overlay(),
                history.overlay());
        return root;
    }

    /* ------------------------------------------- the model ------------------------------------------ */

    /** The model's box: about half the old full-height one. */
    private static final int MODEL_H = 90;

    /** The left column: the player model, half its old size, and the quick teleports under it. No bindings. */
    private static UIElement leftColumn(BooleanSupplier suppressed) {
        UIElement column = new UIElement();
        column.layout(l -> l.flexDirection(FlexDirection.COLUMN).width(TELEPORTS_W).heightPercent(100).flexShrink(0)
                .gapRow(PADDING));
        // The player model is gone from here (as asked); the teleports have the column. preview() is kept.
        UIElement teleports = HomeSidePanel.column(TELEPORTS_W);
        teleports.layout(l -> l.widthPercent(100).flexBasis(0).flexGrow(1));
        column.addChild(teleports);
        return column;
    }

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

    /** Minestar's level pink, as the friends and interactions tabs show it. */
    private static final int COLOR_LEVEL = 0xFF6EC7;
    private static final int COLOR_STARCOINS = 0xFFD54F;

    /* ------------------------------------------- the header ------------------------------------------ */

    private static final int HEAD = 32;
    private static final int COLOR_FRAME = 0xFFB45CFF;
    private static final int COLOR_TRACK = 0x40FFFFFF;

    private record ProfileHeader(UIElement root, Label badge, Label group, UIElement fill, Label xp, float[] shown) {}

    private static ProfileHeader profileHeader(Player player) {
        UIElement root = new UIElement();
        root.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).gapColumn(8)
                .alignItems(dev.vfyjxf.taffy.style.AlignItems.CENTER).marginBottom(4).flexShrink(0));

        // The head in a frame, the level on a badge across its bottom edge (like the site's "LVL 12").
        UIElement avatar = new UIElement();
        avatar.layout(l -> l.width(HEAD + 4).height(HEAD + 10).flexShrink(0));
        UIElement frame = new UIElement();
        frame.layout(l -> l.positionType(org.appliedenergistics.yoga.YogaPositionType.ABSOLUTE).left(0).top(0)
                .width(HEAD + 4).height(HEAD + 4).paddingAll(2));
        frame.style(s -> s.background(new com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup(
                new ColorRectTexture(0x90000000),
                new com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture(1, COLOR_FRAME))));
        PlayerHeadElement head = new PlayerHeadElement(player.getUUID(), player.getGameProfile().getName());
        head.layout(l -> l.width(HEAD).height(HEAD));
        frame.addChild(head);
        Label badge = new Label();
        badge.layout(l -> l.positionType(org.appliedenergistics.yoga.YogaPositionType.ABSOLUTE).left(2).top(HEAD - 2)
                .width(HEAD).height(10));
        badge.textStyle(t -> t.textAlignHorizontal(com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal.CENTER)
                .textAlignVertical(Vertical.CENTER).fontSize(7).textColor(0xFFFFFFFF));
        badge.style(s -> s.background(new com.lowdragmc.lowdraglib2.gui.texture.GuiTextureGroup(
                new ColorRectTexture(0xFF1B1530),
                new com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture(1, COLOR_FRAME))));
        badge.setDisplay(false);
        avatar.addChildren(frame, badge);

        UIElement names = new UIElement();
        names.layout(l -> l.flexDirection(FlexDirection.COLUMN).flexBasis(0).flexGrow(1).gapRow(3));
        UIElement nameLine = new UIElement();
        nameLine.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(14).gapColumn(5)
                .alignItems(dev.vfyjxf.taffy.style.AlignItems.CENTER));
        Label name = new Label();
        name.setText(Component.literal(player.getGameProfile().getName()));
        name.layout(l -> l.height(14));
        name.textStyle(t -> t.fontSize(13).textAlignVertical(Vertical.CENTER));
        Label group = new Label();
        group.layout(l -> l.height(12).paddingHorizontal(3));
        group.textStyle(t -> t.textAlignVertical(Vertical.CENTER));
        group.setDisplay(false);
        nameLine.addChildren(name, group);

        // The level's progress, filled in the level pink.
        UIElement track = new UIElement();
        track.layout(l -> l.widthPercent(100).height(4));
        track.style(s -> s.background(new ColorRectTexture(COLOR_TRACK)));
        UIElement fill = new UIElement();
        fill.layout(l -> l.widthPercent(0).heightPercent(100));
        fill.style(s -> s.background(new ColorRectTexture(0xFF000000 | COLOR_LEVEL)));
        track.addChild(fill);
        Label xp = new Label();
        xp.layout(l -> l.widthPercent(100).height(10));
        xp.textStyle(t -> t.textAlignVertical(Vertical.CENTER));
        names.addChildren(nameLine, track, xp);

        root.addChildren(avatar, names);
        return new ProfileHeader(root, badge, group, fill, xp, new float[] {-1f});
    }

    /** Fills the header from the viewer's Minestar profile; nothing level-related without one (a dev world). */
    private static void fillHeader(ProfileHeader header, @Nullable FriendDetail own) {
        boolean has = own != null;
        if (header.badge().isDisplayed() != has) header.badge().setDisplay(has);
        if (!has) {
            setIfChanged(header.xp(), Component.empty());
            return;
        }
        setIfChanged(header.badge(), Component.translatable("gui.roll_mod.hub.home.lvl_badge", own.level()));
        boolean grouped = !own.groupName().isEmpty();
        if (header.group().isDisplayed() != grouped) header.group().setDisplay(grouped);
        if (grouped) {
            MutableComponent text = Component.literal(own.groupName());
            if (own.groupColor() >= 0) text = text.withColor(own.groupColor());
            setIfChanged(header.group(), text);
        }
        float progress = own.levelXpNeeded() > 0 ? Math.min(1f, (float) own.levelXp() / own.levelXpNeeded()) : 0f;
        if (progress != header.shown()[0]) {
            header.shown()[0] = progress;
            header.fill().layout(l -> l.widthPercent(progress * 100f));
        }
        setIfChanged(header.xp(), Component.translatable("gui.roll_mod.hub.home.xp", own.levelXp(), own.levelXpNeeded(),
                own.level() + 1, own.xpToNext()).withStyle(ChatFormatting.GRAY));
    }

    /**
     * A label's text, only when it changed: setting it every tick re-measured every row of the tab, twenty times a
     * second, for nothing.
     */
    private static void setIfChanged(Label label, Component text) {
        if (!text.equals(label.getText())) label.setText(text);
        // An empty line takes no room (no Minestar profile, no unread letters), so the rest fits the column.
        boolean shown = !text.getString().isEmpty();
        if (label.isDisplayed() != shown) label.setDisplay(shown);
    }

    private static UIElement info(Player player, @Nullable ServerPlayer server) {
        // One binding per value. Ints and Components only — the client resolves the wording itself.
        SimpleBinding<Component> rank = DataBindingBuilder
                .componentS2C(supplier(server, p -> RankResolver.displayName(RankResolver.rankId(p)),
                        RankResolver.displayName(RankResolver.DEFAULT_RANK)))
                .initialValue(RankResolver.displayName(RankResolver.DEFAULT_RANK)).build();
        SimpleBinding<Integer> tier = intBinding(supplier(server, PlayerTier::of, 0), 0);
        // Bindings are polled every server tick while the screen is open, and counting walks the whole
        // quest book, so the four quest lines share one count, refreshed once a second.
        Supplier<QuestProgress.Counts> quests = throttled(server, QuestProgress::of, QuestProgress.Counts.NONE);
        SimpleBinding<Integer> questsDone = intBinding(() -> quests.get().requiredDone(), 0);
        SimpleBinding<Integer> questsTotal = intBinding(() -> quests.get().requiredTotal(), 0);
        SimpleBinding<Integer> optionalDone = intBinding(() -> quests.get().optionalDone(), 0);
        SimpleBinding<Integer> optionalTotal = intBinding(() -> quests.get().optionalTotal(), 0);

        UIElement column = new UIElement();
        column.layout(l -> l.flexDirection(FlexDirection.COLUMN).flexBasis(0).flexGrow(1).heightPercent(100)
                .gapRow(ROW_GAP));
        column.addSyncValue(rank.getSyncValue());
        column.addSyncValue(tier.getSyncValue());
        column.addSyncValue(questsDone.getSyncValue());
        column.addSyncValue(questsTotal.getSyncValue());
        column.addSyncValue(optionalDone.getSyncValue());
        column.addSyncValue(optionalTotal.getSyncValue());

        // The header, as on Eden's profile: the head in a frame with "LVL N" on a badge under it, the name large,
        // the group beside it, and the level's progress as a bar with the XP to the next. No sync values.
        ProfileHeader header = profileHeader(player);
        column.addChild(header.root());

        Label levelRow = row(column, null);
        levelRow.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> {
            FriendDetail own = ClientFriendsCache.DETAILS.get(player.getUUID());
            e.hoverTooltips = new HoverTooltips(own == null
                    ? List.of(Component.translatable("gui.roll_mod.hub.home.level.tip"))
                    : List.of(Component.translatable("gui.roll_mod.hub.home.level.tip"),
                            Component.translatable("gui.roll_mod.hub.friends.xp.next", own.xpToNext(), own.level() + 1),
                            Component.translatable("gui.roll_mod.hub.friends.xp.progress",
                                    own.levelXp(), own.levelXpNeeded()).withStyle(ChatFormatting.GRAY)),
                    null, null, ItemStack.EMPTY);
        });
        // Hours played, from the same Minestar profile as the level above it.
        Label playtimeRow = row(column, "gui.roll_mod.hub.home.playtime.tip");
        Label rankRow = row(column, "gui.roll_mod.hub.home.rank.tip");
        Label coinsRow = row(column, "gui.roll_mod.hub.home.starcoins.tip");
        UIElement coinsIcon = new UIElement();
        coinsIcon.layout(l -> l.width(ICON).height(ICON));
        coinsIcon.style(s -> s.background(SpriteTexture.of(RollMod.id("textures/item/lp.png"))));

        Label questsRow = row(column, null);
        Label tierRow = row(column, "gui.roll_mod.hub.home.tier.tip");

        // Standing, letters, and the staff summary. Read from the client caches, not bindings —
        // the same exception ClientCurrencyHolder gets — so the six sync ids above stay put.
        Label standingRow = wrappingRow(row(column, "gui.roll_mod.hub.home.standing.tip"));
        Label lettersRow = row(column, null);
        Label staffRow = wrappingRow(row(column, "gui.roll_mod.hub.home.staff.tip"));

        column.addEventListener(UIEvents.TICK, e -> {
            // Level and rank are the viewer's Minestar profile, fetched when the hub opened. Without
            // one (no Minestar API — a dev world) the rank falls back to the server's own.
            FriendDetail own = ClientFriendsCache.DETAILS.get(player.getUUID());
            fillHeader(header, own);
            setIfChanged(levelRow, own == null ? Component.empty()
                    : Component.translatable("gui.roll_mod.hub.home.level",
                            Component.literal(String.valueOf(own.level())).withColor(COLOR_LEVEL)));
            setIfChanged(playtimeRow, own == null ? Component.empty()
                    : Component.translatable("gui.roll_mod.hub.home.playtime",
                            Component.literal(String.valueOf(own.playtimeHours())).withStyle(ChatFormatting.AQUA)));
            if (own != null && !own.groupName().isEmpty()) {
                MutableComponent group = Component.literal(own.groupName());
                if (own.groupColor() >= 0) group = group.withColor(own.groupColor());
                setIfChanged(rankRow, Component.translatable("gui.roll_mod.hub.home.minestar_rank", group));
            } else {
                setIfChanged(rankRow, Component.translatable("gui.roll_mod.hub.home.rank",
                        value(rank, Component.empty())));
            }
            setIfChanged(coinsRow, Component.translatable("gui.roll_mod.hub.home.starcoins",
                    Component.literal(EnergyFormatUtils.formatEnergy(ClientCurrencyHolder.get(CurrencyType.MAIN)))
                            .withColor(COLOR_STARCOINS)));
            setIfChanged(questsRow, Component.translatable("gui.roll_mod.hub.home.quests",
                    value(questsDone, 0), value(questsTotal, 0)));
            setIfChanged(tierRow, Component.translatable("gui.roll_mod.hub.home.tier",
                    PlayerTier.name(value(tier, 0))));
            setIfChanged(standingRow, standing());
            int unread = ClientPlayerStatusCache.UNREAD_LETTERS;
            setIfChanged(lettersRow, unread > 0
                    ? Component.translatable("gui.roll_mod.hub.status.letters", unread)
                            .withStyle(ChatFormatting.GOLD)
                    : Component.empty());
            setIfChanged(staffRow, staff());
        });

        // The optional count is a footnote to the quest row rather than a row of its own.
        questsRow.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(
                List.of(Component.translatable("gui.roll_mod.hub.home.quests.tip"),
                        Component.translatable("gui.roll_mod.hub.home.quests.optional",
                                value(optionalDone, 0), value(optionalTotal, 0))
                                .withStyle(ChatFormatting.GRAY)),
                null, null, ItemStack.EMPTY));

        // As on Eden's profile: what the level gives, and the rewards earned on the site. No sync values.
        column.addChild(LevelRewardsCard.build(player));

        return column;
    }

    /**
     * Past this width a line takes no more pieces: the next one starts a line of its own. Small on
     * purpose — each fact reads on its own line rather than being strung out across the column.
     */
    private static final int MAX_LINE_W = 50;

    /** Warnings, and the mute when there is one. */
    private static Component standing() {
        int warns = ClientPlayerStatusCache.WARNS;
        List<Component> parts = new java.util.ArrayList<>();
        parts.add(Component.translatable("gui.roll_mod.hub.home.warns",
                Component.literal(String.valueOf(warns)).withStyle(warns == 0 ? ChatFormatting.GREEN
                        : warns >= ModerationService.WARNS_BEFORE_BAN - 1 ? ChatFormatting.RED
                        : ChatFormatting.GOLD),
                ModerationService.WARNS_BEFORE_BAN));
        if (ClientPlayerStatusCache.muted()) {
            long left = ClientPlayerStatusCache.muteRemaining();
            parts.add((left < 0L
                    ? Component.translatable("gui.roll_mod.hub.status.mutedForever")
                    : Component.translatable("gui.roll_mod.hub.status.muted", Durations.format(left)))
                    .withStyle(ChatFormatting.RED));
        }
        return lines(parts);
    }

    /** For staff only: who is on, who is banned, whether the server is closed. Empty for everyone else. */
    private static Component staff() {
        if (!ClientModerationCache.ALLOWED) return Component.empty();
        int online = 0;
        int banned = 0;
        long now = System.currentTimeMillis();
        for (ClientModerationCache.Row row : ClientModerationCache.ROWS) {
            if (row.row().online()) online++;
            if (row.banned(now)) banned++;
        }
        return lines(List.of(
                Component.translatable("gui.roll_mod.hub.home.staff.online", online),
                Component.translatable("gui.roll_mod.hub.home.staff.banned", banned),
                Component.translatable("gui.roll_mod.hub.home.staff.server",
                        Component.translatable(ClientModerationCache.WHITELIST
                                ? "gui.roll_mod.hub.moderation.whitelist.on"
                                : "gui.roll_mod.hub.moderation.whitelist.off")))
        ).copy().withStyle(ChatFormatting.AQUA);
    }

    /**
     * Joins the pieces with " · ", starting a new line instead whenever the current one is already
     * past {@link #MAX_LINE_W}. Measuring needs the client's font, so off the client — the dedicated
     * server's copy of this tree, which nobody sees — every piece simply gets its own line.
     */
    private static Component lines(List<Component> parts) {
        MutableComponent out = Component.empty();
        int lineWidth = 0;
        for (int i = 0; i < parts.size(); i++) {
            Component part = parts.get(i);
            if (i > 0) {
                if (lineWidth > MAX_LINE_W) {
                    out.append("\n");
                    lineWidth = 0;
                } else {
                    out.append(" · ");
                    lineWidth += ClientFont.width(Component.literal(" · "));
                }
            }
            out.append(part);
            lineWidth += ClientFont.width(part);
        }
        return out;
    }

    /** Isolated so the dedicated server never resolves the client's font. */
    private static final class ClientFont {
        static int width(Component text) {
            if (!FMLEnvironment.dist.isClient()) return Integer.MAX_VALUE / 4;
            return net.minecraft.client.Minecraft.getInstance().font.width(text);
        }
    }

    /**
     * A status line that may run to several lines: it wraps inside the column and grows to fit, so
     * the line breaks {@link #lines} puts in, and anything still too wide, push the rows
     * below it down instead of overlapping them.
     */
    private static Label wrappingRow(Label label) {
        label.layout(l -> l.heightAuto().minHeight(ROW_H));
        label.textStyle(t -> t.textWrap(TextWrap.WRAP).adaptiveHeight(true));
        return label;
    }

        /** A status line, with an optional hover explanation. */
    private static Label row(UIElement parent, @Nullable String tooltipKey) {
        Label label = new Label();
        label.layout(l -> l.widthPercent(100).height(ROW_H));
        label.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textShadow(com.roll_54.roll_mod.minestar.theme.RollUi.textShadow()));
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

    /** Ticks a {@link #throttled} value is reused for. */
    private static final int REFRESH_TICKS = 20;

    /** {@link #supplier}, but computed at most once per {@value #REFRESH_TICKS} ticks, by the server's tick count. */
    private static <T> Supplier<T> throttled(@Nullable ServerPlayer server,
                                             java.util.function.Function<ServerPlayer, T> mapper,
                                             T fallback) {
        if (server == null) return () -> fallback;
        Object[] cached = {null};
        int[] computedAt = {Integer.MIN_VALUE};
        return () -> {
            int now = server.server.getTickCount();
            if (cached[0] == null || now - computedAt[0] >= REFRESH_TICKS) {
                cached[0] = mapper.apply(server);
                computedAt[0] = now;
            }
            @SuppressWarnings("unchecked")
            T value = (T) cached[0];
            return value;
        };
    }

    private static <T> T value(SimpleBinding<T> binding, T fallback) {
        T v = binding.getSyncValue().getValue();
        return v == null ? fallback : v;
    }
}
