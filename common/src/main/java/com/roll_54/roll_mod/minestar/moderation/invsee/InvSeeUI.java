package com.roll_54.roll_mod.minestar.moderation.invsee;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.SimpleBinding;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ItemSlot;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.event.HoverTooltips;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.compat.createcybernetics.CyberwareSlotHandler;
import com.roll_54.roll_mod.compat.curios.CuriosCompat.CurioRow;
import com.roll_54.roll_mod.economy.vendingblock.gui.VendorUIHelper;
import com.roll_54.roll_mod.minestar.hub.gui.PlayerHeadElement;
import com.roll_54.roll_mod.minestar.theme.RollUi;
import com.roll_54.roll_mod.network.packet.InvSeePackets;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

/**
 * The moderator's window into another player: their inventory, armour, offhand and curios, or
 * their implants. The moderator's own inventory sits underneath, so items move between the two by
 * dragging and shift-clicking. It is opened by {@link InvSeeSessions}, never by the hub.
 *
 * <p>The hub is not a place for slots bound to someone else's items. Its tree is shared by every
 * tab, and a bound slot there would hand out sync ids the other side never allocates (see
 * {@code LetterRewardChips}). So this is a menu of its own, and its slots are real ones.
 *
 * <p><b>Built on both sides from the same {@link InvSeeLayout}.</b> The server binds the slots to
 * the session's handlers. The client binds the same number of slots, in the same order, to empty
 * placeholders that the menu's sync then fills. Every decision here reads the layout only, never
 * the dist, the permission or the player, so both trees match.
 *
 * <p>The header always says which of the two this is: "! LIVE" for an online player's items,
 * "! OFFLINE FILE" for a saved playerdata file. When the window is editable it also says that
 * changes apply at once.
 */
public final class InvSeeUI {

    public static final ResourceLocation UI_ID = RollMod.id("moderation_invsee");

    private static final int PAD = 8;
    private static final int SLOT = 18;
    private static final int LINE = 10;
    private static final int BUTTON_H = 14;
    private static final int BUTTON_W = 62;
    private static final int CONTENT_Y = 50;
    private static final int PLAYER_INV_W = 162;
    private static final int PLAYER_INV_H = 76;

    private static final int INVENTORY_W = 236;
    private static final int GROUP_GAP = 4;

    private static final int IMPLANTS_W = 340;
    private static final int PART_LABEL_W = 50;
    private static final int PART_ROW_H = 19;
    private static final int PARTS_PER_COLUMN = 6;

    private static final int[] ARMOUR = {39, 38, 37, 36};
    private static final int OFFHAND = 40;

    private static final int TEXT = 0xFFE0E0E0;
    private static final int TEXT_DIM = 0xFFAAAAAA;
    private static final int WARN = 0xFFFF5555;

    private InvSeeUI() {}

    /**
     * @param session the server's session; {@code null} on the client, whose slots are placeholders.
     */
    public static ModularUI createUI(Player player, InvSeeLayout layout, @Nullable InvSeeSession session) {
        boolean implants = layout.page() == InvSeeLayout.Page.IMPLANTS;
        int width = implants ? IMPLANTS_W : INVENTORY_W;
        int contentBottom = implants ? implantsHeight() : inventoryHeight(layout);
        // A moderator's own inventory page would show the same slots twice, so the grid underneath
        // is left out. The target's id is the same on both sides, so both trees still match. The
        // implants page keeps it: that is where implants to install come from.
        boolean ownGrid = implants || !layout.target().equals(player.getUUID());
        int inventoryY = contentBottom + 6;
        int height = ownGrid ? inventoryY + PLAYER_INV_H + 6 : contentBottom + 6;

        UIElement root = VendorUIHelper.styledPanel(width, height);
        root.layout(l -> l.paddingAll(0));

        header(root, layout, width);

        if (implants) {
            implants(root, layout, session);
        } else {
            inventory(root, layout, session);
        }

        if (ownGrid) root.addChild(VendorUIHelper.playerInventory((width - PLAYER_INV_W) / 2, inventoryY));
        return ModularUI.of(RollUi.of(player, root), player);
    }

    /* ---------------------------------------------- header -------------------------------------------- */

    private static void header(UIElement root, InvSeeLayout layout, int width) {
        PlayerHeadElement head = new PlayerHeadElement(layout.target(), layout.name());
        root.addChild(VendorUIHelper.abs(head, PAD, 6, 12, 12));

        Label name = label(Component.literal(layout.name()), TEXT, true);
        root.addChild(VendorUIHelper.abs(name, PAD + 16, 8, width - PAD * 2 - 16 - 90, LINE));

        // The mode chip: what an edit here actually touches. The "!" is the point, and it is on both.
        Label chip = label(Component.translatable(layout.offline()
                ? "gui.roll_mod.invsee.chip.offline" : "gui.roll_mod.invsee.chip.live")
                .withStyle(ChatFormatting.BOLD), layout.offline() ? 0xFFFFAA00 : WARN, true);
        chip.textStyle(t -> t.textAlignHorizontal(Horizontal.RIGHT));
        tooltip(chip, layout.offline()
                ? "gui.roll_mod.invsee.chip.offline.tip" : "gui.roll_mod.invsee.chip.live.tip");
        root.addChild(VendorUIHelper.abs(chip, width - PAD - 90, 8, 90, LINE));

        // Page buttons for a live player; the offline file has only the one page.
        int x = PAD;
        if (!layout.offline()) {
            x = pageButton(root, layout, InvSeeLayout.Page.INVENTORY, "gui.roll_mod.invsee.page.inventory", x);
            if (layout.cyberware()) {
                x = pageButton(root, layout, InvSeeLayout.Page.IMPLANTS, "gui.roll_mod.invsee.page.implants", x);
            }
        }
        Button back = new Button();
        back.setText(Component.translatable("gui.roll_mod.invsee.back"));
        back.setOnClick(e -> PacketDistributor.sendToServer(new InvSeePackets.Back()));
        root.addChild(VendorUIHelper.abs(back, width - PAD - BUTTON_W, 20, BUTTON_W, BUTTON_H));

        Label hint = label(Component.translatable(!layout.editable() ? "gui.roll_mod.invsee.read_only"
                        : layout.offline() ? "gui.roll_mod.invsee.editing.offline" : "gui.roll_mod.invsee.editing.live"),
                layout.editable() ? WARN : TEXT_DIM, false);
        root.addChild(VendorUIHelper.abs(hint, PAD, 37, width - PAD * 2 - 80, LINE));
    }

    private static int pageButton(UIElement root, InvSeeLayout layout, InvSeeLayout.Page page, String key, int x) {
        Button button = new Button();
        button.setText(Component.translatable(key));
        boolean current = layout.page() == page;
        button.setActive(!current);
        button.setOnClick(e -> {
            if (!current) PacketDistributor.sendToServer(InvSeePackets.Open.online(layout.target(), page));
        });
        root.addChild(VendorUIHelper.abs(button, x, 20, BUTTON_W, BUTTON_H));
        return x + BUTTON_W + 3;
    }

    /* -------------------------------------------- inventory ------------------------------------------- */

    private static void inventory(UIElement root, InvSeeLayout layout, @Nullable InvSeeSession session) {
        IItemHandler handler = session != null ? session.inventory
                : new ItemStackHandler(InvSeeLayout.INVENTORY_SLOTS);
        boolean editable = layout.editable();

        // The player's own screen's arrangement: armour down the left, the three main rows, the
        // hotbar under a small gap, and the offhand after it.
        int gridX = PAD + SLOT + 4;
        for (int row = 0; row < 4; row++) {
            int y = CONTENT_Y + row * SLOT + (row == 3 ? 4 : 0);
            root.addChild(slot(handler, ARMOUR[row], editable, 1, PAD, y));
            int first = row < 3 ? 9 + row * 9 : 0;
            for (int i = 0; i < 9; i++) {
                root.addChild(slot(handler, first + i, editable, 64, gridX + i * SLOT, y));
            }
        }
        root.addChild(slot(handler, OFFHAND, editable, 64, gridX + 9 * SLOT + 4, CONTENT_Y + 3 * SLOT + 4));

        int y = CONTENT_Y + 4 * SLOT + 8;
        if (layout.offline()) {
            Label note = label(Component.translatable("gui.roll_mod.invsee.offline_note"), TEXT_DIM, false);
            root.addChild(VendorUIHelper.abs(note, PAD, y, INVENTORY_W - PAD * 2, LINE));
            return;
        }

        // Curio rows wrap like words: each is its name over its slots, as wide as it needs.
        int x = PAD;
        for (int r = 0; r < layout.curios().size(); r++) {
            CurioRow row = layout.curios().get(r);
            int w = groupWidth(row);
            if (x > PAD && x + w > INVENTORY_W - PAD) {
                x = PAD;
                y += LINE + SLOT + GROUP_GAP;
            }
            Component title = Component.translatable("curios.identifier." + row.id());
            if (row.cosmetic()) title = Component.translatable("gui.roll_mod.invsee.cosmetic", title);
            Label name = label(title, TEXT_DIM, false);
            root.addChild(VendorUIHelper.abs(name, x, y, w, LINE - 1));

            IItemHandler rowHandler = session != null ? session.curios.get(r) : new ItemStackHandler(row.size());
            for (int i = 0; i < row.size(); i++) {
                root.addChild(slot(rowHandler, i, editable, 64, x + i * SLOT, y + LINE));
            }
            x += w + GROUP_GAP;
        }
    }

    private static int groupWidth(CurioRow row) {
        return Math.max(row.size(), 3) * SLOT;
    }

    /** Where the inventory page's content ends; the same walk as {@link #inventory}, without slots. */
    private static int inventoryHeight(InvSeeLayout layout) {
        int y = CONTENT_Y + 4 * SLOT + 8;
        if (layout.offline()) return y + LINE;
        if (layout.curios().isEmpty()) return y;
        int x = PAD;
        for (CurioRow row : layout.curios()) {
            int w = groupWidth(row);
            if (x > PAD && x + w > INVENTORY_W - PAD) {
                x = PAD;
                y += LINE + SLOT + GROUP_GAP;
            }
            x += w + GROUP_GAP;
        }
        return y + LINE + SLOT;
    }

    /* --------------------------------------------- implants ------------------------------------------- */

    private static void implants(UIElement root, InvSeeLayout layout, @Nullable InvSeeSession session) {
        if (!layout.cyberware()) {
            Label none = label(Component.translatable("gui.roll_mod.invsee.no_cyberware"), TEXT_DIM, false);
            root.addChild(VendorUIHelper.abs(none, PAD, CONTENT_Y, IMPLANTS_W - PAD * 2, LINE));
            return;
        }

        // The window's one sync value, so a moderator far from the player still sees their humanity.
        Humanity humanity = new Humanity(session);
        SimpleBinding<Integer> binding = DataBindingBuilder.intValS2C((Supplier<Integer>) humanity::get)
                .initialValue(0).build();
        root.addSyncValue(binding.getSyncValue());
        Label readout = label(Component.empty(), TEXT, false);
        readout.textStyle(t -> t.textAlignHorizontal(Horizontal.RIGHT));
        readout.addEventListener(UIEvents.TICK, e -> {
            Integer value = binding.getSyncValue().getValue();
            readout.setText(colored(Component.translatable("gui.roll_mod.invsee.humanity",
                    Component.literal(String.valueOf(value == null ? 0 : value)).withStyle(ChatFormatting.AQUA)), TEXT));
        });
        root.addChild(VendorUIHelper.abs(readout, IMPLANTS_W - PAD - 80, 37, 80, LINE));

        IItemHandler handler = session != null && session.cyberware != null ? session.cyberware
                : new ItemStackHandler(CyberwareSlotHandler.slotCount());
        boolean editable = layout.editable();
        int columnW = (IMPLANTS_W - PAD * 3) / 2;

        List<CyberwareSlotHandler.Section> sections = CyberwareSlotHandler.sections();
        int body = 0;
        int sub = 0;
        for (CyberwareSlotHandler.Section section : sections) {
            boolean isBody = section.key().startsWith("body.");
            int index = isBody ? body++ : sub++;
            int column = index / PARTS_PER_COLUMN;
            int line = index % PARTS_PER_COLUMN;
            int top = isBody ? CONTENT_Y : subTop();
            if (!isBody) {
                column = index % 2;
                line = index / 2;
            }
            int x = PAD + column * (columnW + PAD);
            int y = top + line * PART_ROW_H;

            Label name = label(Component.translatable("gui.roll_mod.invsee." + section.key()), TEXT_DIM, false);
            root.addChild(VendorUIHelper.abs(name, x, y + 5, PART_LABEL_W - 2, LINE));
            for (int i = 0; i < section.size(); i++) {
                int slot = section.start() + i;
                root.addChild(slot(handler, slot, editable, CyberwareSlotHandler.limit(slot),
                        x + PART_LABEL_W + i * SLOT, y));
            }
        }

        Label subTitle = label(Component.translatable("gui.roll_mod.invsee.sub.title"), TEXT, false);
        root.addChild(VendorUIHelper.abs(subTitle, PAD, subTop() - LINE - 1, IMPLANTS_W - PAD * 2, LINE));
    }

    private static int subTop() {
        return CONTENT_Y + PARTS_PER_COLUMN * PART_ROW_H + LINE + 3;
    }

    private static int implantsHeight() {
        int subs = CyberwareSlotHandler.Sub.values().length;
        return subTop() + ((subs + 1) / 2) * PART_ROW_H;
    }

    /** Humanity on the server, nothing on the client, whose binding is fed by the sync instead. */
    private record Humanity(@Nullable InvSeeSession session) {
        int get() {
            return session == null ? 0 : session.humanity.getAsInt();
        }
    }

    /* --------------------------------------------- plumbing ------------------------------------------- */

    private static ItemSlot slot(IItemHandler handler, int index, boolean editable, int max, int x, int y) {
        ItemSlot slot = new ItemSlot().bind(new InvSeeSlot(handler, index, editable, max));
        return VendorUIHelper.abs(slot, x, y, SLOT, SLOT);
    }

    private static Label label(Component text, int color, boolean shadow) {
        Label label = new Label();
        label.setText(colored(text, color));
        label.textStyle(t -> t.textColor(color).textShadow(shadow).adaptiveWidth(false).textWrap(TextWrap.HIDE));
        return label;
    }

    /**
     * The colour carried by the text as well as the label: a theme's {@code label} rule can outrank
     * the label's own colour, never one on the component (the trick {@code WelcomeUI} uses).
     */
    private static Component colored(Component text, int color) {
        return Component.empty().withColor(color & 0xFFFFFF).append(text);
    }

    private static void tooltip(UIElement element, String key) {
        element.addEventListener(UIEvents.HOVER_TOOLTIPS, e -> e.hoverTooltips = new HoverTooltips(
                List.of(Component.translatable(key)), null, null, ItemStack.EMPTY));
    }
}
