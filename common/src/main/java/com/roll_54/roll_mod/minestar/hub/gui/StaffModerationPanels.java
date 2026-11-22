package com.roll_54.roll_mod.minestar.hub.gui;

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextArea;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.roll_54.roll_mod.minestar.staff.ClientStaffCache;
import com.roll_54.roll_mod.minestar.staff.StaffService;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * The Moderation tab's three staff-board modes: <b>Commands</b> (the command blocker ported from Eden_LD: commands
 * only operators may use), <b>Polls</b> (make, close, delete; see the votes) and <b>Tickets</b> (players'
 * suggestions and bug reports: read, answer by letter, close).
 */
final class StaffModerationPanels {

    private static final int LIST_W = 150;

    private final boolean client;

    /* ------------------------------------------------- commands ------------------------------------------------ */

    final UIElement commandsPane;
    /** The list as edited here; null until the server's arrives (or after saving / reverting). */
    private List<String> opOnly;
    private boolean dirty;
    private final ScrollerView commandsBody = StaffUi.scroller();
    private int commandsVersion = -1;
    private boolean commandsRebuild;

    /* --------------------------------------------------- polls ------------------------------------------------- */

    final UIElement pollsPane;
    private final ScrollerView pollList = StaffUi.scroller();
    private int pollsVersion = -1;

    /* -------------------------------------------------- tickets ------------------------------------------------ */

    final UIElement ticketsPane;
    private final ScrollerView ticketList = StaffUi.scroller();
    private final ScrollerView ticketDetail = StaffUi.scroller();
    private UUID selectedTicket;
    private int ticketsVersion = -1;
    private String ticketSig = "";

    StaffModerationPanels(boolean client, UIElement commandsPane, UIElement pollsPane, UIElement ticketsPane) {
        this.client = client;
        this.commandsPane = commandsPane;
        this.pollsPane = pollsPane;
        this.ticketsPane = ticketsPane;

        commandsPane.addChild(commandsBody);

        // Polls: the form on top, every poll under it.
        TextField question = new TextField();
        question.setText("");
        question.setTextValidator(t -> t.length() <= StaffService.MAX_QUESTION);
        question.layout(l -> l.widthPercent(100).height(StaffUi.ROW_H));
        // One field per answer, growing as you go: two to start, and a fresh empty one appears below as soon as you
        // start typing into the last (up to eight). Empty ones are left out when the poll is made.
        UIElement options = new UIElement();
        options.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).gapRow(3));
        List<TextField> fields = new ArrayList<>();
        Runnable[] grow = {null};
        java.util.function.Supplier<TextField> addField = () -> {
            TextField field = new TextField();
            field.setText("");
            field.setTextValidator(t -> t.length() <= StaffService.MAX_OPTION);
            field.layout(l -> l.widthPercent(100).height(StaffUi.ROW_H));
            field.setTextResponder(t -> grow[0].run());
            fields.add(field);
            options.addChild(field);
            return field;
        };
        grow[0] = () -> {
            TextField last = fields.get(fields.size() - 1);
            if (!last.getText().isBlank() && fields.size() < StaffService.MAX_OPTIONS) addField.get();
        };
        addField.get();
        addField.get();
        Button create = StaffUi.button("gui.roll_mod.polls.create", 0, () -> {
            CompoundTag args = new CompoundTag();
            args.putString("question", question.getText());
            ListTag list = new ListTag();
            fields.stream().map(f -> f.getText().strip()).filter(t -> !t.isEmpty())
                    .limit(StaffService.MAX_OPTIONS).forEach(t -> list.add(StringTag.valueOf(t)));
            args.put("options", list);
            StaffUi.send("polls/create", args);
            question.setText("");
            // Back to two empty answers.
            options.clearAllChildren();
            fields.clear();
            addField.get();
            addField.get();
        });
        // 3px clear of the answers above and of the poll list below (CLAUDE.md, UI rule).
        create.layout(l -> l.widthPercent(100).marginTop(3).marginBottom(3));
        UIElement form = new UIElement();
        form.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).flexShrink(0).gapRow(0).marginTop(2));
        form.addChildren(StaffUi.fieldLabel("gui.roll_mod.polls.form.question"), question,
                StaffUi.fieldLabel("gui.roll_mod.polls.form.options"), options, create);
        pollsPane.addChildren(form, pollList);

        // Tickets: the list on the left, the picked one and its answer on the right.
        UIElement row = new UIElement();
        row.layout(l -> l.flexDirection(FlexDirection.ROW).widthPercent(100).flexBasis(0).flexGrow(1).gapColumn(4));
        ticketList.layout(l -> l.width(LIST_W).flexGrow(0).flexShrink(0).heightPercent(100));
        UIElement right = new UIElement();
        right.layout(l -> l.flexDirection(FlexDirection.COLUMN).flexBasis(0).flexGrow(1).heightPercent(100));
        right.style(s -> s.background(new ColorRectTexture(0x40000000)));
        right.addChild(ticketDetail);
        row.addChildren(ticketList, right);
        ticketsPane.addChild(row);
    }

    /** A mode was switched to: ask the server for its data. */
    void enter(boolean commands, boolean polls, boolean tickets) {
        if (!client) return;
        if (commands) {
            opOnly = null;
            dirty = false;
            StaffUi.send("commands/get");
        }
        if (polls) StaffUi.send("polls/get");
        if (tickets) StaffUi.send("tickets/all");
    }

    void tick(boolean commands, boolean polls, boolean tickets) {
        if (!client) return;
        if (commands) tickCommands();
        if (polls) tickPolls();
        if (tickets) tickTickets();
    }

    /* ------------------------------------------------- commands ------------------------------------------------ */

    private void tickCommands() {
        int v = ClientStaffCache.version("commands");
        if (v != commandsVersion) {
            commandsVersion = v;
            if (!dirty) opOnly = null;
            commandsRebuild = true;
        }
        if (!commandsRebuild) return;
        commandsRebuild = false;
        CompoundTag tag = ClientStaffCache.get("commands");
        if (opOnly == null) opOnly = new ArrayList<>(StaffUi.strings(tag, "op_only"));
        List<String> roots = StaffUi.strings(tag, "roots");

        commandsBody.clearAllScrollViewChildren();
        commandsBody.addScrollViewChild(StaffUi.header("gui.roll_mod.staff.commands.title"));
        commandsBody.addScrollViewChild(StaffUi.dim("gui.roll_mod.staff.commands.hint"));
        if (opOnly.isEmpty()) commandsBody.addScrollViewChild(StaffUi.dim("gui.roll_mod.staff.commands.none"));
        UIElement chips = new UIElement();
        chips.layout(l -> l.flexDirection(FlexDirection.ROW).flexWrap(dev.vfyjxf.taffy.style.FlexWrap.WRAP)
                .widthPercent(100).gapColumn(3).gapRow(3).marginTop(3));
        for (String name : opOnly) {
            Component caption = Component.literal("/" + name + " ✕")
                    .withStyle(roots.contains(name) ? ChatFormatting.WHITE : ChatFormatting.GRAY);
            int width = (name.length() + 3) * 6 + 12;
            chips.addChild(StaffUi.button(caption, width, () -> {
                opOnly.remove(name);
                dirty = true;
                commandsRebuild = true;
            }));
        }
        commandsBody.addScrollViewChild(chips);

        TextField add = new TextField();
        add.setText("");
        add.layout(l -> l.flexBasis(0).flexGrow(1).height(StaffUi.ROW_H));
        com.lowdragmc.lowdraglib2.gui.ui.elements.Label known = StaffUi.dim("gui.roll_mod.staff.commands.add.tip");
        known.layout(l -> l.marginTop(3));
        Runnable addIt = () -> {
            String name = add.getText().strip();
            if (name.startsWith("/")) name = name.substring(1).strip();
            if (name.contains(" ")) name = name.substring(0, name.indexOf(' '));
            if (name.isEmpty() || opOnly.contains(name)) return;
            opOnly.add(name);
            dirty = true;
            commandsRebuild = true;
        };
        UIElement addRow = StaffUi.row();
        addRow.addChildren(add, StaffUi.button("gui.roll_mod.staff.commands.add", 60, addIt));
        commandsBody.addScrollViewChild(addRow);
        commandsBody.addScrollViewChild(known);

        UIElement saveRow = StaffUi.row();
        Button revert = StaffUi.button("gui.roll_mod.staff.commands.revert", 70, () -> {
            opOnly = null;
            dirty = false;
            commandsRebuild = true;
        });
        revert.setActive(dirty);
        Button save = StaffUi.button("gui.roll_mod.hub.moderation.save", 70, () -> {
            CompoundTag args = new CompoundTag();
            ListTag names = new ListTag();
            opOnly.forEach(n -> names.add(StringTag.valueOf(n)));
            args.put("names", names);
            dirty = false;
            StaffUi.send("commands/set", args);
        });
        save.setActive(dirty);
        saveRow.addChildren(revert, save);
        commandsBody.addScrollViewChild(saveRow);
    }

    /* --------------------------------------------------- polls ------------------------------------------------- */

    private void tickPolls() {
        int v = ClientStaffCache.version("polls");
        if (v == pollsVersion) return;
        pollsVersion = v;
        pollList.clearAllScrollViewChildren();
        List<CompoundTag> all = StaffUi.compounds(ClientStaffCache.get("polls"), "polls");
        if (all.isEmpty()) pollList.addScrollViewChild(StaffUi.dim("gui.roll_mod.polls.none"));
        for (CompoundTag poll : all) pollList.addScrollViewChild(StaffUi.pollCard(poll, true));
    }

    /* -------------------------------------------------- tickets ------------------------------------------------ */

    private void tickTickets() {
        List<CompoundTag> all = StaffUi.compounds(ClientStaffCache.get("tickets"), "tickets");
        int v = ClientStaffCache.version("tickets");
        if (v != ticketsVersion) {
            ticketsVersion = v;
            ticketList.clearAllScrollViewChildren();
            if (all.isEmpty()) ticketList.addScrollViewChild(StaffUi.dim("gui.roll_mod.tickets.none"));
            for (CompoundTag t : all) {
                UUID id = t.getUUID("id");
                UIElement row = new UIElement();
                row.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).paddingAll(3).marginBottom(2));
                row.style(s -> s.background(com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture.dynamic(() ->
                        new ColorRectTexture(id.equals(selectedTicket) ? StaffUi.COLOR_ROW_SELECTED : StaffUi.COLOR_ROW))));
                row.addChild(StaffUi.text(Component.empty().append(StaffUi.kind(t.getString("kind"))).append(" ")
                        .append(Component.literal(t.getString("title")).withStyle(ChatFormatting.WHITE))));
                row.addChild(StaffUi.text(Component.literal(t.getString("author") + " · ").withStyle(ChatFormatting.GRAY)
                        .append(StaffUi.status(t.getString("status")))));
                row.addEventListener(UIEvents.MOUSE_DOWN, e -> selectedTicket = id);
                ticketList.addScrollViewChild(row);
            }
        }
        CompoundTag current = null;
        for (CompoundTag t : all) if (t.getUUID("id").equals(selectedTicket)) current = t;
        String sig = v + ":" + selectedTicket;
        if (sig.equals(ticketSig)) return;
        ticketSig = sig;
        ticketDetail.clearAllScrollViewChildren();
        if (current == null) {
            ticketDetail.addScrollViewChild(StaffUi.dim("gui.roll_mod.tickets.pick"));
            return;
        }
        UIElement detail = new UIElement();
        detail.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100));
        StaffUi.ticketDetail(detail, current);
        TextArea answer = new TextArea();
        answer.setValue(new String[]{""}, false);
        answer.layout(l -> l.widthPercent(100).height(60).marginTop(6));
        CompoundTag ticket = current;
        Button send = StaffUi.button("gui.roll_mod.tickets.answer", 0, () -> {
            CompoundTag args = StaffUi.id(ticket);
            args.putString("text", String.join("\n", answer.getValue()));
            StaffUi.send("tickets/answer", args);
        });
        send.layout(l -> l.widthPercent(100).marginTop(3));
        boolean closed = ticket.getString("status").equals("CLOSED");
        UIElement actions = StaffUi.row();
        actions.addChildren(
                StaffUi.button(closed ? "gui.roll_mod.tickets.reopen" : "gui.roll_mod.tickets.close", 70,
                        () -> StaffUi.send(closed ? "tickets/reopen" : "tickets/close", StaffUi.id(ticket))),
                StaffUi.button("gui.roll_mod.polls.delete", 70, () -> {
                    StaffUi.send("tickets/delete", StaffUi.id(ticket));
                    selectedTicket = null;
                }));
        detail.addChildren(StaffUi.dim("gui.roll_mod.tickets.answer.hint"), answer, send, actions);
        ticketDetail.addScrollViewChild(detail);
    }
}
