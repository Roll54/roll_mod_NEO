package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.roll_54.roll_mod.network.packet.StaffPackets;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Small shared pieces for the command blocker, polls and tickets screens. */
final class StaffUi {

    static final int ROW_H = 16;
    static final int COLOR_ROW = 0x30FFFFFF;
    static final int COLOR_ROW_SELECTED = 0x60FFFFFF;
    static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private StaffUi() {}

    static Label text(Component text) {
        Label label = new Label();
        label.setText(text);
        label.layout(l -> l.widthPercent(100));
        label.textStyle(t -> t.textWrap(TextWrap.WRAP).adaptiveHeight(true));
        return label;
    }

    static Label dim(String key, Object... args) {
        return text(Component.translatable(key, args).withStyle(ChatFormatting.GRAY));
    }

    /** A label over a field: 3px clear of the box above and of the field under it. */
    static Label fieldLabel(String key) {
        Label label = dim(key);
        label.layout(l -> l.marginTop(3).marginBottom(3));
        return label;
    }

    static Button button(Component text, int width, Runnable action) {
        Button button = new Button();
        button.setText(text);
        button.layout(l -> {
            if (width > 0) l.width(width);
            l.height(ROW_H).flexShrink(0);
        });
        HubSection.centreCaption(button);
        button.setOnClick(e -> action.run());
        return button;
    }

    /**
     * A button whose caption may be longer than the button (a reward's name): read from the left, 3px clear of the
     * frame, and cut at the right edge instead of spilling over both sides.
     */
    static Button leftCaption(Button button) {
        button.layout(l -> l.paddingHorizontal(3));
        // The caption fills the button instead of sizing to its text: a text-sized caption is centred by the button
        // and runs out of it when too long. Filling, it starts 3px in from the left and is cut at the right.
        button.text.layout(l -> l.flexGrow(1).flexShrink(1).flexBasis(0).minWidth(0));
        button.text.textStyle(t -> t.adaptiveWidth(false)
                .textAlignHorizontal(com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal.LEFT)
                .textWrap(com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap.HIDE));
        button.setOverflowVisible(false);
        return button;
    }

    static Button button(String key, int width, Runnable action) {
        return button(Component.translatable(key), width, action);
    }

    static UIElement row() {
        UIElement row = new UIElement();
        row.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).gapColumn(4).marginTop(3)
                .alignItems(dev.vfyjxf.taffy.style.AlignItems.CENTER));
        return row;
    }

    static ScrollerView scroller() {
        ScrollerView scroller = new ScrollerView();
        // 7px: the themed frames are up to 4px thick, and text keeps 3px clear of them.
        scroller.viewPort(v -> v.layout(l -> l.paddingAll(7)));
        scroller.layout(l -> l.widthPercent(100).flexBasis(0).flexGrow(1));
        scroller.scrollerStyle(s -> s.mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.AUTO)
                .horizontalScrollDisplay(ScrollDisplay.NEVER));
        scroller.viewContainer(c -> c.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100)));
        return scroller;
    }

    static void send(String action, CompoundTag args) {
        PacketDistributor.sendToServer(new StaffPackets.Act(action, args));
    }

    static void send(String action) {
        send(action, new CompoundTag());
    }

    static CompoundTag id(CompoundTag of) {
        CompoundTag args = new CompoundTag();
        if (of.hasUUID("id")) args.putUUID("id", of.getUUID("id"));
        return args;
    }

    static List<String> strings(CompoundTag tag, String key) {
        List<String> out = new ArrayList<>();
        ListTag list = tag.getList(key, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) out.add(list.getString(i));
        return out;
    }

    static List<CompoundTag> compounds(CompoundTag tag, String key) {
        List<CompoundTag> out = new ArrayList<>();
        ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) out.add(list.getCompound(i));
        return out;
    }

    static String date(long millis) {
        return DATE.format(Instant.ofEpochMilli(millis));
    }

    /**
     * One poll as a card: the question, then each answer as a button with its votes and share — the player's own
     * pick marked — and, for staff, Close/Reopen and Delete under it.
     */
    static UIElement pollCard(CompoundTag poll, boolean staff) {
        UIElement card = new UIElement();
        card.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).paddingAll(4).marginBottom(4));
        card.style(s -> s.background(new ColorRectTexture(COLOR_ROW)));
        boolean closed = poll.getBoolean("closed");
        // The question, and for staff Close/Reopen and Delete beside it at the top, short enough to fit there.
        UIElement top = new UIElement();
        top.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).gapColumn(3)
                .alignItems(dev.vfyjxf.taffy.style.AlignItems.CENTER));
        Label question = text(Component.literal(poll.getString("question")).withStyle(ChatFormatting.WHITE));
        // At least a button's height and centred in it, so the question sits level with Close and Delete.
        question.layout(l -> l.flexBasis(0).flexGrow(1).minHeight(ROW_H));
        question.textStyle(t -> t.textAlignVertical(Vertical.CENTER));
        top.addChild(question);
        if (staff) {
            top.addChildren(
                    button(closed ? "gui.roll_mod.polls.reopen" : "gui.roll_mod.polls.close", 44,
                            () -> send(closed ? "polls/reopen" : "polls/close", id(poll))),
                    button("gui.roll_mod.polls.delete", 44, () -> send("polls/delete", id(poll))));
        }
        card.addChild(top);
        card.addChild(dim("gui.roll_mod.polls.meta", poll.getString("author"), date(poll.getLong("created")),
                Component.translatable(closed ? "gui.roll_mod.polls.closed" : "gui.roll_mod.polls.open")));
        List<String> options = strings(poll, "options");
        int[] counts = poll.getIntArray("counts");
        int total = 0;
        for (int c : counts) total += c;
        int mine = poll.getInt("mine");
        // Discord-style answers: a box per answer, filled as far as its share of the votes, the share on the right,
        // the player's own pick outlined in gold. A click votes (or moves the vote) while the poll is open.
        for (int i = 0; i < options.size(); i++) {
            int at = i;
            int votes = i < counts.length ? counts[i] : 0;
            float share = total == 0 ? 0 : votes / (float) total;
            boolean picked = i == mine;
            UIElement answer = new UIElement();
            answer.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).height(ROW_H).marginTop(3)
                    .paddingHorizontal(4).alignItems(dev.vfyjxf.taffy.style.AlignItems.CENTER));
            answer.style(s -> s.background(com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture.dynamic(() ->
                    shareBox(share, picked, !closed && answer.isHover()))));
            Label name = new Label();
            name.setText(Component.literal(options.get(i)).withStyle(picked ? ChatFormatting.YELLOW : ChatFormatting.WHITE));
            name.layout(l -> l.flexBasis(0).flexGrow(1).height(ROW_H));
            name.textStyle(t -> t.textAlignVertical(Vertical.CENTER));
            Label count = new Label();
            count.setText(Component.literal(votes + " · " + Math.round(share * 100) + "%").withStyle(ChatFormatting.GRAY));
            count.layout(l -> l.height(ROW_H).flexShrink(0));
            count.textStyle(t -> t.textAlignVertical(Vertical.CENTER).adaptiveWidth(true));
            answer.addChildren(name, count);
            if (!closed) {
                answer.addEventListener(com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents.CLICK, e -> {
                    CompoundTag args = id(poll);
                    args.putInt("at", at);
                    send("polls/vote", args);
                });
            }
            card.addChild(answer);
        }
        return card;
    }

    /** An answer's box: a dark field, the share filled in, a 1px rim (gold for the player's own pick). */
    private static com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture shareBox(float share, boolean picked, boolean hover) {
        return new com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture() {
            @Override
            public void draw(net.minecraft.client.gui.GuiGraphics g, float mx, float my, float x, float y, float w, float h, float pt) {
                int x0 = (int) x, y0 = (int) y, x1 = (int) (x + w), y1 = (int) (y + h);
                g.fill(x0, y0, x1, y1, hover ? 0x60000000 : 0x40000000);
                int fill = x0 + 1 + Math.round((x1 - x0 - 2) * share);
                if (fill > x0 + 1) g.fill(x0 + 1, y0 + 1, fill, y1 - 1, picked ? 0x80B08A20 : 0x705865F2);
                int rim = picked ? 0xFFE0B040 : 0x80FFFFFF;
                g.fill(x0, y0, x1, y0 + 1, rim);
                g.fill(x0, y1 - 1, x1, y1, rim);
                g.fill(x0, y0 + 1, x0 + 1, y1 - 1, rim);
                g.fill(x1 - 1, y0 + 1, x1, y1 - 1, rim);
            }
        };
    }

    /** A ticket's status, coloured: open (yellow), answered (green), closed (grey). */
    static Component status(String status) {
        ChatFormatting colour = switch (status) {
            case "ANSWERED" -> ChatFormatting.GREEN;
            case "CLOSED" -> ChatFormatting.GRAY;
            default -> ChatFormatting.YELLOW;
        };
        return Component.translatable("gui.roll_mod.tickets.status." + status.toLowerCase(java.util.Locale.ROOT))
                .withStyle(colour);
    }

    static Component kind(String kind) {
        return Component.translatable("gui.roll_mod.tickets.kind." + kind.toLowerCase(java.util.Locale.ROOT))
                .withStyle(kind.equals("BUG") ? ChatFormatting.RED : ChatFormatting.AQUA);
    }

    /** A ticket in full: kind, title, author and date, status, the text and, once answered, the answer. */
    static void ticketDetail(UIElement into, CompoundTag t) {
        into.addChild(text(Component.empty().append(kind(t.getString("kind"))).append(" · ")
                .append(Component.literal(t.getString("title")).withStyle(ChatFormatting.WHITE))));
        into.addChild(text(Component.translatable("gui.roll_mod.tickets.meta", t.getString("author"),
                date(t.getLong("created"))).withStyle(ChatFormatting.GRAY).append(" · ").append(status(t.getString("status")))));
        Label body = text(Component.literal(t.getString("body")));
        body.layout(l -> l.marginTop(4));
        into.addChild(body);
        if (!t.getString("answer").isEmpty()) {
            Label head = text(Component.translatable("gui.roll_mod.tickets.answer_by", t.getString("answeredBy"))
                    .withStyle(ChatFormatting.GREEN));
            head.layout(l -> l.marginTop(6));
            into.addChildren(head, text(Component.literal(t.getString("answer"))));
        }
    }

    static Label header(String key) {
        Label label = text(Component.translatable(key).withStyle(ChatFormatting.WHITE));
        label.layout(l -> l.marginTop(4).height(12));
        label.textStyle(t -> t.textAlignVertical(Vertical.CENTER));
        return label;
    }
}
