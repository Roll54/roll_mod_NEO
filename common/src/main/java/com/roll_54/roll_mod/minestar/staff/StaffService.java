package com.roll_54.roll_mod.minestar.staff;

import com.roll_54.roll_mod.minestar.letters.LetterService;
import com.roll_54.roll_mod.minestar.moderation.ModerationPermissions;
import com.roll_54.roll_mod.network.packet.StaffPackets;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Every staff-board action, checked here: the command blocker (those who may ban), polls (made by moderators, voted
 * on by everyone) and tickets (written by everyone, answered by moderators; the answer is sent as a letter).
 */
@net.neoforged.fml.common.EventBusSubscriber(modid = com.roll_54.roll_mod.RollMod.MODID)
public final class StaffService {

    /** The polls go out at login, so an open one the player has not answered shows in their inventory at once. */
    @net.neoforged.bus.api.SubscribeEvent
    public static void onLogin(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sendPolls(player);
    }

    public static final int MAX_OPTIONS = 8;
    public static final int MAX_QUESTION = 200;
    public static final int MAX_OPTION = 60;
    public static final int MAX_TITLE = 64;
    public static final int MAX_BODY = 1000;
    public static final int MAX_ANSWER = 1000;
    /** How many unanswered tickets one player may have waiting at once. */
    public static final int MAX_OPEN_TICKETS = 3;

    private StaffService() {}

    public static void act(ServerPlayer player, String action, CompoundTag args) {
        MinecraftServer server = player.server;
        StaffBoard board = StaffBoard.get(server);
        boolean staff = ModerationPermissions.canModerate(player);
        switch (action) {
            case "commands/get" -> {
                if (ModerationPermissions.canBan(player)) sendCommands(player);
            }
            case "commands/set" -> {
                if (!ModerationPermissions.canBan(player)) return;
                List<String> names = new ArrayList<>();
                ListTag list = args.getList("names", Tag.TAG_STRING);
                for (int i = 0; i < list.size(); i++) names.add(list.getString(i));
                OpOnlyCommands.set(server, names);
                sendCommands(player);
                player.sendSystemMessage(Component.translatable("msg.roll_mod.staff.commands.saved",
                        OpOnlyCommands.names().size()).withStyle(ChatFormatting.GREEN));
            }
            case "polls/get" -> sendPolls(player);
            case "polls/create" -> {
                if (!staff) return;
                String question = clamp(args.getString("question"), MAX_QUESTION);
                List<String> options = new ArrayList<>();
                ListTag list = args.getList("options", Tag.TAG_STRING);
                for (int i = 0; i < list.size() && options.size() < MAX_OPTIONS; i++) {
                    String o = clamp(list.getString(i), MAX_OPTION);
                    if (!o.isEmpty()) options.add(o);
                }
                if (question.isEmpty() || options.size() < 2) {
                    player.sendSystemMessage(Component.translatable("msg.roll_mod.staff.polls.invalid")
                            .withStyle(ChatFormatting.RED));
                    return;
                }
                StaffBoard.Poll poll = new StaffBoard.Poll();
                poll.question = question;
                poll.options.addAll(options);
                poll.author = player.getGameProfile().getName();
                poll.created = System.currentTimeMillis();
                board.polls.put(poll.id, poll);
                board.setDirty();
                broadcastPolls(server);
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    p.sendSystemMessage(Component.translatable("msg.roll_mod.staff.polls.new", question)
                            .withStyle(ChatFormatting.AQUA));
                }
            }
            case "polls/close", "polls/reopen", "polls/delete" -> {
                if (!staff) return;
                StaffBoard.Poll poll = board.polls.get(uuid(args));
                if (poll == null) return;
                if (action.equals("polls/delete")) board.polls.remove(poll.id);
                else poll.closed = action.equals("polls/close");
                board.setDirty();
                broadcastPolls(server);
            }
            case "polls/vote" -> {
                StaffBoard.Poll poll = board.polls.get(uuid(args));
                int at = args.getInt("at");
                if (poll == null || poll.closed || at < 0 || at >= poll.options.size()) return;
                poll.votes.put(player.getUUID(), at);
                board.setDirty();
                broadcastPolls(server);
            }
            case "tickets/mine" -> sendMine(player);
            case "tickets/all" -> {
                if (staff) sendAll(player);
            }
            case "tickets/send" -> {
                String title = clamp(args.getString("title"), MAX_TITLE);
                String body = clamp(args.getString("body"), MAX_BODY);
                if (title.isEmpty() || body.isEmpty()) {
                    player.sendSystemMessage(Component.translatable("msg.roll_mod.staff.tickets.empty")
                            .withStyle(ChatFormatting.RED));
                    return;
                }
                long open = board.tickets.values().stream()
                        .filter(t -> t.author.equals(player.getUUID()) && t.status == StaffBoard.TicketStatus.OPEN).count();
                if (open >= MAX_OPEN_TICKETS) {
                    player.sendSystemMessage(Component.translatable("msg.roll_mod.staff.tickets.too_many", MAX_OPEN_TICKETS)
                            .withStyle(ChatFormatting.RED));
                    return;
                }
                StaffBoard.Ticket ticket = new StaffBoard.Ticket();
                ticket.author = player.getUUID();
                ticket.authorName = player.getGameProfile().getName();
                ticket.kind = args.getString("kind").equals("BUG") ? StaffBoard.TicketKind.BUG : StaffBoard.TicketKind.SUGGESTION;
                ticket.title = title;
                ticket.body = body;
                ticket.created = System.currentTimeMillis();
                board.tickets.put(ticket.id, ticket);
                board.setDirty();
                player.sendSystemMessage(Component.translatable("msg.roll_mod.staff.tickets.sent")
                        .withStyle(ChatFormatting.GREEN));
                sendMine(player);
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    if (ModerationPermissions.canModerate(p)) {
                        p.sendSystemMessage(Component.translatable("msg.roll_mod.staff.tickets.new_for_staff",
                                ticket.authorName, Component.translatable(kindKey(ticket.kind)), title)
                                .withStyle(ChatFormatting.GOLD));
                        sendAll(p);
                    }
                }
            }
            case "tickets/answer" -> {
                if (!staff) return;
                StaffBoard.Ticket ticket = board.tickets.get(uuid(args));
                String text = clamp(args.getString("text"), MAX_ANSWER);
                if (ticket == null || text.isEmpty()) return;
                ticket.answer = text;
                ticket.answeredBy = player.getGameProfile().getName();
                ticket.status = StaffBoard.TicketStatus.ANSWERED;
                board.setDirty();
                // The answer reaches the player as a letter, so they find it even if they were offline.
                LetterService.sendSystem(server, ticket.author, player.getGameProfile().getName(),
                        Component.translatable("letter.roll_mod.ticket.answer.title", ticket.title).getString(),
                        text, "", 0L);
                refreshTickets(server, ticket.author);
            }
            case "tickets/close", "tickets/reopen", "tickets/delete" -> {
                if (!staff) return;
                StaffBoard.Ticket ticket = board.tickets.get(uuid(args));
                if (ticket == null) return;
                if (action.equals("tickets/delete")) board.tickets.remove(ticket.id);
                else ticket.status = action.equals("tickets/close") ? StaffBoard.TicketStatus.CLOSED
                        : StaffBoard.TicketStatus.OPEN;
                board.setDirty();
                refreshTickets(server, ticket.author);
            }
            default -> { }
        }
    }

    public static String kindKey(StaffBoard.TicketKind kind) {
        return "gui.roll_mod.tickets.kind." + kind.name().toLowerCase(java.util.Locale.ROOT);
    }

    private static void sendCommands(ServerPlayer player) {
        CompoundTag tag = new CompoundTag();
        tag.put("op_only", strings(OpOnlyCommands.names()));
        tag.put("roots", strings(OpOnlyCommands.roots(player.server)));
        PacketDistributor.sendToPlayer(player, new StaffPackets.Data("commands", tag));
    }

    private static void broadcastPolls(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) sendPolls(p);
    }

    /** Every poll, newest first, with this player's own vote ({@code mine}, -1 for none). */
    private static void sendPolls(ServerPlayer player) {
        StaffBoard board = StaffBoard.get(player.server);
        List<StaffBoard.Poll> all = new ArrayList<>(board.polls.values());
        java.util.Collections.reverse(all);
        ListTag list = new ListTag();
        for (StaffBoard.Poll poll : all) {
            CompoundTag t = new CompoundTag();
            t.putUUID("id", poll.id);
            t.putString("question", poll.question);
            t.put("options", strings(poll.options));
            t.putIntArray("counts", poll.counts());
            t.putInt("mine", poll.votes.getOrDefault(player.getUUID(), -1));
            t.putBoolean("closed", poll.closed);
            t.putString("author", poll.author);
            t.putLong("created", poll.created);
            list.add(t);
        }
        CompoundTag tag = new CompoundTag();
        tag.put("polls", list);
        tag.putBoolean("staff", ModerationPermissions.canModerate(player));
        PacketDistributor.sendToPlayer(player, new StaffPackets.Data("polls", tag));
    }

    private static void sendMine(ServerPlayer player) {
        sendTickets(player, "my_tickets", t -> t.author.equals(player.getUUID()));
    }

    private static void sendAll(ServerPlayer player) {
        sendTickets(player, "tickets", t -> true);
    }

    private static void sendTickets(ServerPlayer player, String kind, java.util.function.Predicate<StaffBoard.Ticket> which) {
        List<StaffBoard.Ticket> all = new ArrayList<>(StaffBoard.get(player.server).tickets.values());
        java.util.Collections.reverse(all);
        ListTag list = new ListTag();
        for (StaffBoard.Ticket ticket : all) {
            if (!which.test(ticket)) continue;
            CompoundTag t = new CompoundTag();
            t.putUUID("id", ticket.id);
            t.putString("author", ticket.authorName);
            t.putString("kind", ticket.kind.name());
            t.putString("title", ticket.title);
            t.putString("body", ticket.body);
            t.putLong("created", ticket.created);
            t.putString("status", ticket.status.name());
            t.putString("answer", ticket.answer);
            t.putString("answeredBy", ticket.answeredBy);
            list.add(t);
        }
        CompoundTag tag = new CompoundTag();
        tag.put("tickets", list);
        PacketDistributor.sendToPlayer(player, new StaffPackets.Data(kind, tag));
    }

    /** After a ticket changed: its author's own list, and every online moderator's full one. */
    private static void refreshTickets(MinecraftServer server, UUID author) {
        ServerPlayer owner = server.getPlayerList().getPlayer(author);
        if (owner != null) sendMine(owner);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (ModerationPermissions.canModerate(p)) sendAll(p);
        }
    }

    private static ListTag strings(List<String> values) {
        ListTag list = new ListTag();
        values.forEach(v -> list.add(StringTag.valueOf(v)));
        return list;
    }

    private static UUID uuid(CompoundTag args) {
        return args.hasUUID("id") ? args.getUUID("id") : new UUID(0, 0);
    }

    private static String clamp(String text, int max) {
        String s = text == null ? "" : text.strip();
        return s.length() <= max ? s : s.substring(0, max);
    }
}
