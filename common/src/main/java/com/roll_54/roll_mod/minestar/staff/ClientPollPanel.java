package com.roll_54.roll_mod.minestar.staff;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.network.packet.StaffPackets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A small poll window beside the player's inventory for the newest open poll they have not answered: a title bar
 * with an ✕ that puts it away (until the game restarts), the question, and a box per answer. A click on an answer
 * votes, and the window goes. Drawn and clicked here, over the inventory, rather than as screen widgets.
 */
@EventBusSubscriber(modid = RollMod.MODID, value = Dist.CLIENT)
public final class ClientPollPanel {

    private static final int WIDTH = 150;
    private static final int PADDING = 5;
    private static final int TITLE_H = 14;
    private static final int OPTION_H = 14;
    private static final int GAP = 2;
    private static final int CLOSE = 9;

    /** Polls the player closed with the ✕ this session. */
    private static final Set<UUID> DISMISSED = new HashSet<>();

    private ClientPollPanel() {}

    /** Where everything is this frame, or null when nothing shows. */
    private record Layout(UUID id, int x, int y, int h, List<FormattedCharSequence> question, List<String> options) {
        int closeX() { return x + WIDTH - CLOSE - 3; }
        int closeY() { return y + (TITLE_H - CLOSE) / 2; }
        int optionY(int i) { return y + TITLE_H + PADDING + question.size() * 10 + 3 + i * (OPTION_H + GAP); }
    }

    private static boolean isInventory(Screen screen) {
        return screen instanceof InventoryScreen || screen instanceof CreativeModeInventoryScreen;
    }

    private static CompoundTag pending() {
        ListTag polls = ClientStaffCache.get("polls").getList("polls", Tag.TAG_COMPOUND);
        for (int i = 0; i < polls.size(); i++) {
            CompoundTag poll = polls.getCompound(i);
            if (!poll.hasUUID("id") || poll.getBoolean("closed") || poll.getInt("mine") >= 0) continue;
            if (DISMISSED.contains(poll.getUUID("id"))) continue;
            return poll;
        }
        return null;
    }

    private static Layout layout(Screen screen) {
        if (!isInventory(screen) || !(screen instanceof AbstractContainerScreen<?> container)) return null;
        CompoundTag poll = pending();
        if (poll == null) return null;
        int room = container.getGuiLeft() - 8;
        if (room < WIDTH) return null;
        Font font = Minecraft.getInstance().font;
        List<FormattedCharSequence> question = font.split(Component.literal(poll.getString("question")), WIDTH - 2 * PADDING);
        ListTag opts = poll.getList("options", Tag.TAG_STRING);
        List<String> options = new java.util.ArrayList<>();
        for (int i = 0; i < opts.size(); i++) options.add(opts.getString(i));
        int h = TITLE_H + PADDING + question.size() * 10 + 3 + options.size() * (OPTION_H + GAP) + PADDING;
        int x = (room - WIDTH) / 2 + 4;
        int y = Math.max(4, container.getGuiTop());
        return new Layout(poll.getUUID("id"), x, y, h, question, options);
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Post event) {
        Layout l = layout(event.getScreen());
        if (l == null) return;
        GuiGraphics g = event.getGuiGraphics();
        Font font = Minecraft.getInstance().font;
        int mx = event.getMouseX(), my = event.getMouseY();
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        g.fill(l.x(), l.y(), l.x() + WIDTH, l.y() + l.h(), 0xF01E1E22);
        g.fill(l.x(), l.y(), l.x() + WIDTH, l.y() + TITLE_H, 0xFF2C2C34);
        g.renderOutline(l.x(), l.y(), WIDTH, l.h(), 0xFF5865F2);
        g.drawString(font, Component.translatable("gui.roll_mod.polls.title"), l.x() + PADDING, l.y() + 3, 0xFFFFFFFF, true);
        boolean overClose = inside(mx, my, l.closeX(), l.closeY(), CLOSE, CLOSE);
        g.drawString(font, "✕", l.closeX() + 1, l.closeY(), overClose ? 0xFFFF6060 : 0xFFCCCCCC, true);
        int y = l.y() + TITLE_H + PADDING;
        for (FormattedCharSequence line : l.question()) {
            g.drawString(font, line, l.x() + PADDING, y, 0xFFFFFFFF, true);
            y += 10;
        }
        for (int i = 0; i < l.options().size(); i++) {
            int oy = l.optionY(i);
            int ox = l.x() + PADDING, ow = WIDTH - 2 * PADDING;
            boolean hover = inside(mx, my, ox, oy, ow, OPTION_H);
            g.fill(ox, oy, ox + ow, oy + OPTION_H, hover ? 0x605865F2 : 0x40000000);
            g.renderOutline(ox, oy, ow, OPTION_H, hover ? 0xFF8890FF : 0x80FFFFFF);
            String text = font.plainSubstrByWidth(l.options().get(i), ow - 6);
            g.drawString(font, text, ox + 3, oy + 3, 0xFFFFFFFF, true);
        }
        g.pose().popPose();
    }

    @SubscribeEvent
    public static void onClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != 0) return;
        Layout l = layout(event.getScreen());
        if (l == null) return;
        double mx = event.getMouseX(), my = event.getMouseY();
        if (!inside(mx, my, l.x(), l.y(), WIDTH, l.h())) return;
        event.setCanceled(true);
        if (inside(mx, my, l.closeX(), l.closeY(), CLOSE, CLOSE)) {
            DISMISSED.add(l.id());
            return;
        }
        for (int i = 0; i < l.options().size(); i++) {
            if (inside(mx, my, l.x() + PADDING, l.optionY(i), WIDTH - 2 * PADDING, OPTION_H)) {
                CompoundTag args = new CompoundTag();
                args.putUUID("id", l.id());
                args.putInt("at", i);
                PacketDistributor.sendToServer(new StaffPackets.Act("polls/vote", args));
                // Gone at once; the server's answer then marks the vote as ours for good.
                DISMISSED.add(l.id());
                return;
            }
        }
    }
}
