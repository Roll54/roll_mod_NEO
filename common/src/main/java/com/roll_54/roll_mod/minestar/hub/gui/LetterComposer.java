package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextArea;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.roll_54.roll_mod.economy.currency.model.CurrencyType;
import com.roll_54.roll_mod.economy.vendingblock.gui.VendorUIHelper;
import com.roll_54.roll_mod.minestar.letters.ClientLetterCache;
import com.roll_54.roll_mod.minestar.letters.Letter;
import com.roll_54.roll_mod.minestar.letters.LetterIcon;
import com.roll_54.roll_mod.minestar.letters.LetterReward;
import com.roll_54.roll_mod.minestar.letters.LetterView;
import com.roll_54.roll_mod.network.packet.LetterComposePacket;
import com.roll_54.roll_mod.util.Durations;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.appliedenergistics.yoga.YogaPositionType;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * The moderation tab's letters mode: write a letter, attach rewards, send it; or look over what has
 * been sent and withdraw it.
 *
 * <p>Rewards are added through a small menu that opens <em>at the cursor</em> from the {@code +}
 * button — money, an item stack, or a command — the shape of FTB Quests' reward menu. The draft is
 * client state until Send; the server re-validates all of it in {@code LetterService}.
 *
 * <p>No sync values anywhere in here. Reward previews are {@link VendorUIHelper#phantomSlot()}s that
 * are never bound — see {@link LetterRewardChips}.
 */
final class LetterComposer {

    private static final int ROW_H = 16;
    private static final int LINE_H = 12;
    private static final int GAP = 3;
    private static final int PLUS = 16;
    private static final int FIELD_W = 44;
    private static final String DEFAULT_EXPIRY = "7d";

    private static final int POPUP_W = 190;
    private static final int POPUP_H = 124;

    private static final int COLOR_PANEL = 0x40000000;

    private enum Kind { MONEY, ITEM, COMMAND }

    /** What {@link ModerationTab} places: the pane in its stack, the popup over the whole tab. */
    final UIElement pane;
    final HubSection.Floating popup;

    private final Consumer<Boolean> inventory;
    private final List<LetterReward> draft = new ArrayList<>();

    private final TextField title;
    private final TextField delay;
    private final TextField expiry;
    /** The letter's {@link LetterIcon}: an item id, a {@code .png} path, or blank for the envelope. */
    private final TextField icon;
    /** Shows {@link #icon}, and takes an item dropped from JEI/EMI or clicked in off the cursor. */
    private final ItemSlot iconSlot;
    private final UIElement iconPreview;
    private String shownIcon;
    private ItemStack shownIconItem = ItemStack.EMPTY;
    private final TextArea body;
    private final UIElement rewardStrip;
    private final Label status;
    private final UIElement buttons;

    /** The letter being edited, or {@code null} while writing a new one. */
    @Nullable private UUID editingId;

    // Reward menu state.
    private Kind kind = Kind.MONEY;
    private UIElement form;
    private ItemSlot pick;
    private boolean picking;
    private Label pickHint;

    private int draftVersion;
    private int shownDraft = -1;
    private String shownButtons;
    private String shownStatus;
    private boolean inventoryShown;

    LetterComposer(Consumer<Boolean> inventory) {
        this.inventory = inventory;

        pane = new UIElement();
        pane.layout(l -> l.positionType(YogaPositionType.ABSOLUTE).left(0).top(0)
                .widthPercent(100).heightPercent(100).flexDirection(FlexDirection.COLUMN).gapRow(GAP));
        pane.setOverflowVisible(false);

        UIElement top = row();
        title = new TextField();
        title.setText("");
        title.layout(l -> l.flexGrow(1).height(ROW_H));
        hint(title, "gui.roll_mod.hub.moderation.letters.title.tip");
        delay = new TextField();
        delay.setText("");
        delay.layout(l -> l.width(FIELD_W).height(ROW_H));
        hint(delay, "gui.roll_mod.hub.moderation.letters.delay.tip");
        expiry = new TextField();
        expiry.setText(DEFAULT_EXPIRY);
        expiry.layout(l -> l.width(FIELD_W).height(ROW_H));
        hint(expiry, "gui.roll_mod.hub.moderation.letters.expiry.tip");
        top.addChildren(title, delay, expiry);

        UIElement iconRow = row();
        iconRow.layout(l -> l.alignItems(AlignItems.CENTER));
        iconPreview = new UIElement();
        iconPreview.layout(l -> l.width(LetterIcons.SIZE).height(LetterIcons.SIZE).flexShrink(0));
        iconSlot = LetterIcons.slot(ItemStack.EMPTY, true);
        iconSlot.addEventListener(UIEvents.CLICK, e -> {
            AbstractContainerMenu menu = iconSlot.getModularUI() == null ? null : iconSlot.getModularUI().getMenu();
            if (menu == null || menu.getCarried().isEmpty()) return;
            iconSlot.setItem(menu.getCarried().copyWithCount(1));
            e.stopPropagation();
        });
        hint(iconSlot, "gui.roll_mod.hub.moderation.letters.icon.slot.tip");
        iconPreview.addChild(iconSlot);
        icon = new TextField();
        icon.setText("");
        icon.layout(l -> l.flexGrow(1).height(ROW_H));
        hint(icon, "gui.roll_mod.hub.moderation.letters.icon.tip");
        iconRow.addChildren(iconPreview, icon);

        body = new FormattedTextArea();
        // Padded so typed text does not sit on the edge of its box; TextArea lays out inside it.
        body.layout(l -> l.widthPercent(100).flexBasis(0).flexGrow(1).paddingAll(4));
        body.style(s -> s.background(new ColorRectTexture(COLOR_PANEL)));
        hint(body, "gui.roll_mod.hub.moderation.letters.body.tip");

        UIElement rewards = new UIElement();
        rewards.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).gapColumn(GAP)
                .alignItems(AlignItems.CENTER));
        Button plus = new Button();
        plus.setText("+");
        plus.layout(l -> l.width(PLUS).height(PLUS));
        plus.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(
                List.of(Component.translatable("gui.roll_mod.hub.moderation.letters.add.tip")),
                null, null, ItemStack.EMPTY));
        rewardStrip = new UIElement();
        rewardStrip.layout(l -> l.flexGrow(1));
        rewards.addChildren(plus, rewardStrip);

        status = new Label();
        status.layout(l -> l.widthPercent(100));
        status.textStyle(t -> t.textWrap(TextWrap.WRAP).adaptiveHeight(true));

        buttons = row();

        pane.addChildren(top, iconRow, body, rewards, status, buttons);

        /* ------------------------------------------ reward menu --------------------------------------- */

        popup = HubSection.popup(Component.translatable("gui.roll_mod.hub.moderation.letters.add"),
                POPUP_W, POPUP_H, content -> {
                    UIElement kinds = row();
                    for (Kind k : Kind.values()) {
                        Button b = new Button();
                        b.setText(Component.translatable("gui.roll_mod.hub.moderation.letters.kind."
                                + k.name().toLowerCase()));
                        b.layout(l -> l.flexGrow(1).height(ROW_H));
                        b.setOnClick(e -> choose(k));
                        kinds.addChild(b);
                    }
                    form = new UIElement();
                    form.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100)
                            .flexGrow(1).gapRow(GAP));
                    content.addChildren(kinds, form);
                });

        // At the cursor, the FTB Quests way. Opened fresh on the money form each time: the last
        // kind used is not a good guess for the next.
        plus.addEventListener(UIEvents.CLICK, e -> {
            choose(Kind.MONEY);
            popup.openAt(e.x, e.y);
        });
    }

    /** The letter being edited, for the tab's list to highlight; {@code null} for a new one. */
    @Nullable UUID editingId() {
        return editingId;
    }

    /**
     * Opens a letter for editing. Called only when the moderator picks it, never on a cache refresh,
     * so an edit in progress is not overwritten by the server's copy.
     */
    void load(LetterView letter) {
        long now = System.currentTimeMillis();
        editingId = letter.id();
        title.setText(letter.title());
        icon.setText(letter.icon());
        body.setLines(List.of(letter.body().split("\n", -1)));
        draft.clear();
        draft.addAll(letter.rewards());
        draftVersion++;

        boolean due = letter.due(now);
        delay.setText(due ? Durations.formatInput(letter.sendAt() - now) : "");
        // Only a letter still waiting can be moved; one already out keeps its send time.
        delay.setActive(due);
        expiry.setText(letter.expiresAt() == 0L ? "0"
                : due ? Durations.formatInput(letter.expiresAt() - letter.sendAt())
                : letter.expired(now) ? DEFAULT_EXPIRY
                : Durations.formatInput(letter.expiresAt() - now));
    }

    /** Back to a blank new letter. */
    void clear() {
        editingId = null;
        title.setText("");
        icon.setText("");
        body.setLines(List.of(""));
        delay.setText("");
        delay.setActive(true);
        expiry.setText(DEFAULT_EXPIRY);
        draft.clear();
        draftVersion++;
    }

    /** Called every frame by the tab; {@code active} is whether letters mode is on screen. */
    void tick(boolean active) {
        boolean wantInventory = active && popup.isOpen() && kind == Kind.ITEM;
        if (wantInventory != inventoryShown) {
            inventoryShown = wantInventory;
            inventory.accept(wantInventory);
        }
        if (!popup.isOpen()) picking = false;

        if (shownDraft != draftVersion) {
            shownDraft = draftVersion;
            rebuildStrip();
        }

        long now = System.currentTimeMillis();
        LetterView editing = editingId == null ? null : find(editingId);
        // Withdrawn from elsewhere, or pruned: nothing left to edit.
        if (editingId != null && editing == null && ClientLetterCache.VERSION > 0) clear();

        String buttonState = editingId == null ? "new:" + (parsedDelay() > 0L) : "edit";
        if (!buttonState.equals(shownButtons)) {
            shownButtons = buttonState;
            rebuildButtons(parsedDelay() > 0L);
        }

        // Once a second: the status line carries countdowns.
        String statusSig = editingId + ":" + ClientLetterCache.VERSION + ":" + now / 1000L;
        if (!statusSig.equals(shownStatus)) {
            shownStatus = statusSig;
            status.setText(statusLine(editing, now));
        }

        if (kind == Kind.ITEM && pick != null) tickPicker();
        tickIcon();
    }

    /**
     * Keeps the icon field and its preview in step, whichever one changed: typing an id or a path
     * redraws the preview, and an item landing in the preview slot writes its id into the field.
     */
    private void tickIcon() {
        ItemStack dropped = iconSlot.getValue();
        if (dropped != null && !dropped.isEmpty() && !ItemStack.isSameItemSameComponents(dropped, shownIconItem)) {
            icon.setText(BuiltInRegistries.ITEM.getKey(dropped.getItem()).toString());
        }
        String text = icon.getText() == null ? "" : icon.getText().trim();
        if (text.equals(shownIcon)) return;
        shownIcon = text;

        ResourceLocation id = text.isEmpty() ? null : ResourceLocation.tryParse(text);
        ItemStack stack = id == null ? ItemStack.EMPTY : LetterIcon.item(id);
        shownIconItem = stack;
        iconSlot.setItem(stack.copy());
        // Behind the slot: a PNG, the envelope for blank, or the envelope greyed for a typo.
        IGuiTexture back = !stack.isEmpty() ? IGuiTexture.EMPTY
                : id != null && LetterIcon.isTexture(id) && LetterIcons.exists(id) ? SpriteTexture.of(id)
                : text.isEmpty() ? LetterIcons.envelope(false)
                : LetterIcons.envelope(false).copy().setColor(0x60FFFFFF);
        iconPreview.style(s -> s.background(back));
    }

    /* ------------------------------------------- compose -------------------------------------------- */

    private void rebuildButtons(boolean scheduling) {
        buttons.clearAllChildren();
        if (editingId == null) {
            buttons.addChild(button(scheduling ? "gui.roll_mod.hub.moderation.letters.schedule"
                    : "gui.roll_mod.hub.moderation.letters.send", () -> submit(false)));
            return;
        }
        UUID id = editingId;
        buttons.addChildren(
                button("gui.roll_mod.hub.moderation.letters.save", () -> submit(false)),
                button("gui.roll_mod.hub.moderation.letters.withdraw", () -> {
                    PacketDistributor.sendToServer(LetterComposePacket.delete(id));
                    clear();
                }),
                button("gui.roll_mod.hub.moderation.letters.sendAsNew", () -> submit(true)));
    }

    /**
     * Sends the form: a new letter, or a save of the one being edited. {@code asNew} sends the edited
     * letter as a fresh copy instead — new id, nobody has accepted it yet.
     */
    private void submit(boolean asNew) {
        long wait = parsedDelay();
        long lifetime = Durations.parse(expiry.getText());
        // Refused, not guessed: a typo must not become a letter that never expires or goes out now.
        if (wait < 0L) {
            delay.setText("");
            return;
        }
        if (lifetime < 0L) {
            expiry.setText("");
            return;
        }
        if (title.getText().isBlank()) return;
        String text = String.join("\n", Arrays.asList(body.getValue()));
        List<LetterReward> rewards = List.copyOf(draft);

        if (editingId != null && !asNew) {
            PacketDistributor.sendToServer(LetterComposePacket.update(editingId, title.getText(), text,
                    wait, lifetime, rewards, icon.getText()));
        } else {
            PacketDistributor.sendToServer(LetterComposePacket.create(title.getText(), text, wait,
                    lifetime, rewards, icon.getText()));
            clear();
        }
    }

    /** The Send in field: blank is "now" ({@code 0}), a typo is {@code -1}. */
    private long parsedDelay() {
        String text = delay.getText();
        return text == null || text.isBlank() ? 0L : Durations.parse(text);
    }

    private Component statusLine(@Nullable LetterView letter, long now) {
        if (letter == null) {
            return Component.translatable("gui.roll_mod.hub.moderation.letters.status.new")
                    .withStyle(ChatFormatting.DARK_GRAY);
        }
        if (letter.due(now)) {
            return Component.translatable("gui.roll_mod.hub.moderation.letters.status.due",
                    Durations.format(letter.sendAt() - now)).withStyle(ChatFormatting.AQUA);
        }
        String sent = new SimpleDateFormat("dd.MM HH:mm").format(new Date(
                letter.sendAt() > 0L ? letter.sendAt() : letter.createdAt()));
        return Component.translatable(letter.expired(now)
                        ? "gui.roll_mod.hub.moderation.letters.status.expired"
                        : "gui.roll_mod.hub.moderation.letters.status.active",
                sent, letter.acceptedCount())
                .withStyle(letter.expired(now) ? ChatFormatting.GRAY : ChatFormatting.GREEN);
    }

    @Nullable
    private static LetterView find(UUID id) {
        for (LetterView letter : ClientLetterCache.LETTERS) {
            if (letter.id().equals(id)) return letter;
        }
        return null;
    }

    private static Button button(String key, Runnable onClick) {
        Button button = new Button();
        button.setText(Component.translatable(key));
        button.layout(l -> l.flexGrow(1).height(ROW_H));
        button.setOnClick(e -> onClick.run());
        return button;
    }

    private void rebuildStrip() {
        rewardStrip.clearAllChildren();
        if (draft.isEmpty()) {
            Label none = new Label();
            none.setText(Component.translatable("gui.roll_mod.hub.moderation.letters.noRewards")
                    .withStyle(ChatFormatting.DARK_GRAY));
            none.layout(l -> l.height(PLUS));
            none.textStyle(t -> t.textAlignVertical(Vertical.CENTER));
            rewardStrip.addChild(none);
            return;
        }
        // Clicking a chip takes it back off the letter.
        rewardStrip.addChild(LetterRewardChips.strip(draft, index -> {
            if (index >= 0 && index < draft.size()) {
                draft.remove((int) index);
                draftVersion++;
            }
        }, Component.translatable("gui.roll_mod.hub.moderation.letters.remove.tip")
                .withStyle(ChatFormatting.GRAY)));
    }

    /* ------------------------------------------ reward menu ----------------------------------------- */

    private void choose(Kind next) {
        kind = next;
        picking = false;
        pick = null;
        pickHint = null;
        form.clearAllChildren();
        switch (next) {
            case MONEY -> moneyForm();
            case ITEM -> {
                itemForm();
                // Up out of the way: the inventory it asks for covers the bottom of the hub.
                popup.place(popup.x(), 0);
            }
            case COMMAND -> commandForm();
        }
    }

    private void moneyForm() {
        CurrencyType[] currency = {CurrencyType.MAIN};
        UIElement r = row();
        Button which = new Button();
        which.setText(currencyName(currency[0]));
        which.layout(l -> l.width(70).height(ROW_H));
        which.setOnClick(e -> {
            CurrencyType[] all = CurrencyType.values();
            currency[0] = all[(currency[0].ordinal() + 1) % all.length];
            which.setText(currencyName(currency[0]));
        });
        TextField amount = new TextField();
        amount.setTextRegexValidator("\\d*");
        amount.setText("100");
        amount.layout(l -> l.flexGrow(1).height(ROW_H));
        r.addChildren(which, amount);

        form.addChildren(r, addButton(() -> {
            long value = VendorUIHelper.parseLong(amount.getText(), 0L);
            return value > 0L ? LetterReward.money(currency[0], value) : null;
        }));
    }

    private void itemForm() {
        UIElement r = row();
        r.layout(l -> l.height(LetterRewardChips.SLOT));
        pick = VendorUIHelper.phantomSlot();
        pick.layout(l -> l.width(LetterRewardChips.SLOT).height(LetterRewardChips.SLOT));
        // Clicking the slot with something already on the cursor works too, as in the auction.
        pick.addEventListener(UIEvents.CLICK, e -> {
            AbstractContainerMenu menu = pick.getModularUI() == null ? null : pick.getModularUI().getMenu();
            if (menu == null) return;
            ItemStack cursor = menu.getCarried();
            pick.setItem(cursor.isEmpty() ? ItemStack.EMPTY : cursor.copy());
            e.stopPropagation();
        });
        Button select = new Button();
        select.setText(Component.translatable("menu.roll_mod.ah.pick.button"));
        select.layout(l -> l.flexGrow(1).height(ROW_H));
        select.setOnClick(e -> picking = true);
        TextField count = new TextField();
        count.setTextRegexValidator("\\d*");
        count.setText("");
        count.layout(l -> l.width(34).height(ROW_H));
        hint(count, "gui.roll_mod.hub.moderation.letters.count.tip");
        r.addChildren(pick, select, count);

        pickHint = new Label();
        pickHint.layout(l -> l.widthPercent(100));
        pickHint.textStyle(t -> t.textWrap(TextWrap.WRAP).adaptiveHeight(true));

        ItemSlot slot = pick;
        form.addChildren(r, pickHint, addButton(() -> {
            ItemStack chosen = slot.getValue();
            if (chosen == null || chosen.isEmpty()) return null;
            // Blank keeps the stack as lifted; a number overrides it.
            long wanted = VendorUIHelper.parseLong(count.getText(), chosen.getCount());
            int size = (int) Math.max(1L, Math.min(LetterReward.MAX_STACK, wanted));
            return LetterReward.item(chosen.copyWithCount(size));
        }));
    }

    /**
     * Arms-and-watches, as {@code AuctionUI} does: the inventory slots are real menu slots that
     * LdLib gives no hook on, so rather than intercept the click this watches what lands on the
     * cursor and copies it. The player still puts their own stack straight back.
     */
    private void tickPicker() {
        AbstractContainerMenu menu = pick.getModularUI() == null ? null : pick.getModularUI().getMenu();
        if (menu == null) return;
        if (picking && !menu.getCarried().isEmpty()) {
            pick.setItem(menu.getCarried().copy());
            picking = false;
        }
        if (pickHint == null) return;
        ItemStack chosen = pick.getValue();
        pickHint.setText(picking
                ? Component.translatable("gui.roll_mod.hub.moderation.letters.pick.active").withStyle(ChatFormatting.YELLOW)
                : chosen == null || chosen.isEmpty()
                        ? Component.translatable("menu.roll_mod.ah.pick.idle").withStyle(ChatFormatting.GRAY)
                        : Component.translatable("menu.roll_mod.ah.pick.chosen", chosen.getHoverName())
                                .withStyle(ChatFormatting.GREEN));
    }

    private void commandForm() {
        TextField command = new TextField();
        command.setText("");
        command.layout(l -> l.widthPercent(100).height(ROW_H));
        hint(command, "gui.roll_mod.hub.moderation.letters.command.tip");

        Label note = new Label();
        note.setText(Component.translatable("gui.roll_mod.hub.moderation.letters.command.note")
                .withStyle(ChatFormatting.GRAY));
        note.layout(l -> l.widthPercent(100));
        note.textStyle(t -> t.textWrap(TextWrap.WRAP).adaptiveHeight(true));

        form.addChildren(command, note, addButton(() -> {
            LetterReward reward = LetterReward.command(command.getText());
            return reward.valid() ? reward : null;
        }));
    }

    /** "Add" — puts the form's reward on the letter and closes the menu, or does nothing if invalid. */
    private Button addButton(java.util.function.Supplier<LetterReward> make) {
        Button add = new Button();
        add.setText(Component.translatable("gui.roll_mod.hub.moderation.letters.addButton"));
        add.layout(l -> l.widthPercent(100).height(ROW_H).marginTopAuto());
        add.setOnClick(e -> {
            if (draft.size() >= Letter.MAX_REWARDS) return;
            LetterReward reward = make.get();
            if (reward == null) return;
            draft.add(reward);
            draftVersion++;
            popup.close();
        });
        return add;
    }

    private static Component currencyName(CurrencyType type) {
        return Component.translatable("gui.roll_mod.letters.currency." + type.id());
    }

    /* ------------------------------------------- plumbing ------------------------------------------- */

    private static UIElement row() {
        UIElement r = new UIElement();
        r.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(ROW_H).gapColumn(GAP));
        return r;
    }

    private static void hint(UIElement element, String key) {
        element.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(
                List.of(Component.translatable(key)), null, null, ItemStack.EMPTY));
    }
}
