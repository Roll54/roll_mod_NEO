package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextArea;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import com.roll_54.roll_mod.minestar.staff.ClientStaffCache;
import com.roll_54.roll_mod.minestar.staff.StaffService;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * The Letters tab's two popups for players: <b>Write to moderation</b> — a suggestion or a bug report, and the
 * player's own tickets with their answers — and <b>Polls</b>, where the staff's polls are voted on.
 */
final class PlayerStaffPopups {

    // 1.75x the first size (300x330); the in-hub panel still fits itself to the tab.
    private static final int WRITE_W = 525;
    private static final int WRITE_H = 578;
    private static final int POLLS_W = 280;
    private static final int POLLS_H = 300;

    final HubSection.Floating write;
    final HubSection.Floating polls;

    PlayerStaffPopups() {
        ScrollerView[] mine = {null};
        write = HubSection.popup(Component.translatable("gui.roll_mod.tickets.write.title"), WRITE_W, WRITE_H, body -> {
            body.layout(l -> l.flexDirection(FlexDirection.COLUMN).gapRow(1));
            Selector<String> kind = new Selector<>();
            kind.setCandidateUIProvider(UIElementProvider.text(k -> k == null ? Component.empty() : StaffUi.kind(k)));
            kind.setCandidates(List.of("SUGGESTION", "BUG"));
            kind.setSelected("SUGGESTION", false);
            kind.layout(l -> l.widthPercent(100).height(StaffUi.ROW_H));
            TextField title = new TextField();
            title.setText("");
            title.setTextValidator(t -> t.length() <= StaffService.MAX_TITLE);
            title.layout(l -> l.widthPercent(100).height(StaffUi.ROW_H));
            TextArea text = new TextArea();
            text.setValue(new String[]{""}, false);
            text.layout(l -> l.widthPercent(100).height(70));
            Button send = StaffUi.button("gui.roll_mod.tickets.send", 0, () -> {
                CompoundTag args = new CompoundTag();
                args.putString("kind", kind.getValue() == null ? "SUGGESTION" : kind.getValue());
                args.putString("title", title.getText());
                args.putString("body", String.join("\n", text.getValue()));
                StaffUi.send("tickets/send", args);
                title.setText("");
                text.setValue(new String[]{""}, false);
            });
            send.layout(l -> l.widthPercent(100));
            mine[0] = StaffUi.scroller();
            body.addChildren(StaffUi.fieldLabel("gui.roll_mod.tickets.write.kind"), kind,
                    StaffUi.fieldLabel("gui.roll_mod.tickets.write.subject"), title,
                    StaffUi.fieldLabel("gui.roll_mod.tickets.write.text"), text, send,
                    StaffUi.header("gui.roll_mod.tickets.mine"), mine[0]);
        }).windowed();

        ScrollerView[] pollList = {null};
        polls = HubSection.popup(Component.translatable("gui.roll_mod.polls.title"), POLLS_W, POLLS_H, body -> {
            body.layout(l -> l.flexDirection(FlexDirection.COLUMN));
            pollList[0] = StaffUi.scroller();
            body.addChild(pollList[0]);
        }).windowed();

        this.mine = mine;
        this.pollList = pollList;
    }

    private final ScrollerView[] mine;
    private final ScrollerView[] pollList;
    private final int[] mineVersion = {-1};
    private final int[] pollsVersion = {-1};
    private final boolean[] asked = {false, false};

    /** Called from the Letters tab's tick, which runs wherever the popups are shown. */
    void tick() {
        tickWrite();
        tickPolls();
    }

    private void tickWrite() {
        {
            if (!write.isOpen()) {
                asked[0] = false;
                return;
            }
            if (!asked[0]) {
                asked[0] = true;
                StaffUi.send("tickets/mine");
            }
            int v = ClientStaffCache.version("my_tickets");
            if (v == mineVersion[0]) return;
            mineVersion[0] = v;
            mine[0].clearAllScrollViewChildren();
            List<CompoundTag> tickets = StaffUi.compounds(ClientStaffCache.get("my_tickets"), "tickets");
            if (tickets.isEmpty()) mine[0].addScrollViewChild(StaffUi.dim("gui.roll_mod.tickets.none"));
            for (CompoundTag t : tickets) {
                UIElement card = new UIElement();
                card.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).paddingAll(4).marginBottom(4));
                card.style(s -> s.background(new com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture(StaffUi.COLOR_ROW)));
                StaffUi.ticketDetail(card, t);
                mine[0].addScrollViewChild(card);
            }
        }
    }

    private void tickPolls() {
        {
            if (!polls.isOpen()) {
                asked[1] = false;
                return;
            }
            if (!asked[1]) {
                asked[1] = true;
                StaffUi.send("polls/get");
            }
            int v = ClientStaffCache.version("polls");
            if (v == pollsVersion[0]) return;
            pollsVersion[0] = v;
            pollList[0].clearAllScrollViewChildren();
            List<CompoundTag> all = StaffUi.compounds(ClientStaffCache.get("polls"), "polls");
            if (all.isEmpty()) pollList[0].addScrollViewChild(StaffUi.dim("gui.roll_mod.polls.none"));
            for (CompoundTag poll : all) pollList[0].addScrollViewChild(StaffUi.pollCard(poll, false));
        }
    }

    /** The two buttons for the Letters tab's title row. */
    UIElement[] buttons() {
        Button writeButton = StaffUi.button("gui.roll_mod.tickets.write", 110, write::open);
        Button pollsButton = StaffUi.button("gui.roll_mod.polls.button", 70, polls::open);
        // 2px higher than the row would centre them, the caption 1px lower inside.
        for (Button b : new Button[]{writeButton, pollsButton}) {
            b.layout(l -> l.marginRight(2).top(-2));
            b.text.layout(l -> l.marginTop(1));
        }
        return new UIElement[]{writeButton, pollsButton};
    }
}
