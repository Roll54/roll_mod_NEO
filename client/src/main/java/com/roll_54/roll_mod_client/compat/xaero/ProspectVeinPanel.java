package com.roll_54.roll_mod_client.compat.xaero;

import com.roll_54.roll_mod.prospecting.ClientProspects;
import com.roll_54.roll_mod.prospecting.Discovery;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The vein filter as a small panel on the World Map itself, beside Xaero's left button column, opened and closed by
 * the ore button: a search field, "Select all" / "Deselect all" (acting on what the search shows), and a scrolling
 * list of the veins found in this dimension, each a check box, the vein's map icon, then its name. Changes apply at
 * once.
 *
 * <p>Not a widget of the map screen: Xaero's screen handles its own clicks and keys, so the panel is drawn after the
 * map ({@code ScreenEvent.Render.Post}) and takes the mouse and keyboard events before the map does
 * ({@code ScreenEvent.*.Pre}) while the mouse is over it or its search field is being typed in.
 */
@OnlyIn(Dist.CLIENT)
final class ProspectVeinPanel {

    private static final int WIDTH = 150, MARGIN = 8, GAP = 6, PAD = 4;
    private static final int FIELD_H = 16, ROW_H = 18, BOX = 10, ICON = 16, INDENT = 6;
    /** The list's inner padding: frame plus the 3 px text keeps from it. */
    private static final int VIEW_PAD = 3;
    private static final int BACKGROUND = 0xC0101010, FRAME = 0xFF505050, LIST_BG = 0x80000000;

    private static boolean open;
    private static String query = "";
    private static double scroll;

    /** The panel's box and parts, worked out per frame from the screen and the ore button. */
    private static int x, y, w, h, listTop, listBottom;
    private static EditBox search;
    private static Button all, none;
    private static Screen laidOutFor;

    private static int cachedVersion = -1;
    private static String cachedDim = "";
    private static List<Vein> veins = List.of();

    private record Vein(String key, Component name, List<Discovery> ores, ProspectVeins.Category category) {}

    /** One line of the list: a category's header ({@code vein} null) or a vein under it. */
    private record Row(ProspectVeins.Category category, Vein vein) {}

    private ProspectVeinPanel() {}

    static boolean isOpen() {
        return open;
    }

    static void toggle() {
        open = !open;
        if (search != null) search.setFocused(false);
    }

    /** Lays the panel out right of the column the ore button stands in. */
    static void layout(Screen screen, int columnRight) {
        laidOutFor = screen;
        x = columnRight + GAP;
        y = MARGIN;
        w = WIDTH;
        h = screen.height - 2 * MARGIN;
        Font font = Minecraft.getInstance().font;
        search = new EditBox(font, x + PAD, y + PAD, w - 2 * PAD, FIELD_H,
                Component.translatable("gui.roll_mod.prospect_filter.search"));
        search.setHint(Component.translatable("gui.roll_mod.prospect_filter.search").withStyle(s -> s.withColor(0xD8D8D8)));
        search.setValue(query);
        search.setResponder(q -> {
            query = q;
            scroll = 0;
        });
        int by = y + PAD + FIELD_H + PAD;
        int half = (w - 2 * PAD - PAD) / 2;
        all = Button.builder(Component.translatable("gui.roll_mod.prospect_filter.all"),
                b -> ProspectMapState.setShown(keys(filtered()), true)).bounds(x + PAD, by, half, FIELD_H).build();
        none = Button.builder(Component.translatable("gui.roll_mod.prospect_filter.none"),
                b -> ProspectMapState.setShown(keys(filtered()), false))
                .bounds(x + w - PAD - half, by, half, FIELD_H).build();
        listTop = by + FIELD_H + PAD;
        listBottom = y + h - PAD;
    }

    private static boolean active(Screen screen) {
        return open && laidOutFor == screen && search != null;
    }

    private static boolean inside(double mx, double my) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** Whether the mouse is over the panel, so the map's own hover (tint, tooltip) keeps out of it. */
    static boolean covers(Screen screen, double mx, double my) {
        return active(screen) && inside(mx, my);
    }

    /** Whether the mouse is over the open panel now, read from the window (for code that has no screen mouse). */
    static boolean coversMouse() {
        var mc = Minecraft.getInstance();
        if (!active(mc.screen)) return false;
        var window = mc.getWindow();
        double mx = mc.mouseHandler.xpos() * window.getGuiScaledWidth() / window.getScreenWidth();
        double my = mc.mouseHandler.ypos() * window.getGuiScaledHeight() / window.getScreenHeight();
        return inside(mx, my);
    }

    /* ------------------------------------------------- data ------------------------------------------------- */

    private static List<Vein> veins() {
        String dim = ProspectMapState.dimension();
        if (cachedVersion != ClientProspects.version || !cachedDim.equals(dim)) {
            Map<String, List<Discovery>> found = new LinkedHashMap<>();
            ProspectChunk.all().forEachRemaining(c -> {
                for (ProspectVeins.Piece p : ProspectVeins.split(c.ores())) {
                    List<Discovery> list = found.computeIfAbsent(p.key(), k -> new ArrayList<>());
                    list.addAll(p.ores());
                }
            });
            List<Vein> out = new ArrayList<>();
            found.forEach((key, ores) -> out.add(new Vein(key, ProspectVeins.name(key, ores), merged(ores),
                    ProspectVeins.category(key))));
            out.sort((a, b) -> a.name().getString().compareToIgnoreCase(b.name().getString()));
            veins = List.copyOf(out);
            cachedVersion = ClientProspects.version;
            cachedDim = dim;
        }
        return veins;
    }

    /** The vein's ores summed over its chunks, so its icon is the one the map shows for it. */
    private static List<Discovery> merged(List<Discovery> ores) {
        Map<String, Discovery> sum = new LinkedHashMap<>();
        for (Discovery d : ores) sum.computeIfAbsent(d.oreKey, k -> new Discovery(k, d.rawItem, d.displayBlock)).absorb(d);
        return List.copyOf(sum.values());
    }

    /** The veins the search keeps: by their name or their category's ("незер" finds every Nether vein). */
    private static List<Vein> filtered() {
        String q = query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) return veins();
        List<Vein> out = new ArrayList<>();
        for (Vein v : veins()) {
            if (v.name().getString().toLowerCase(Locale.ROOT).contains(q)
                    || v.category().title().getString().toLowerCase(Locale.ROOT).contains(q)) out.add(v);
        }
        return out;
    }

    /** The list's lines: each category with veins kept, in the categories' order, its header then its veins. */
    private static List<Row> rows() {
        List<Row> rows = new ArrayList<>();
        List<Vein> kept = filtered();
        for (ProspectVeins.Category c : ProspectVeins.Category.values()) {
            boolean any = false;
            for (Vein v : kept) {
                if (v.category() != c) continue;
                if (!any) rows.add(new Row(c, null));
                any = true;
                rows.add(new Row(c, v));
            }
        }
        return rows;
    }

    private static List<Vein> inCategory(ProspectVeins.Category c) {
        List<Vein> out = new ArrayList<>();
        for (Vein v : filtered()) if (v.category() == c) out.add(v);
        return out;
    }

    private static List<String> keys(List<Vein> list) {
        List<String> keys = new ArrayList<>(list.size());
        for (Vein v : list) keys.add(v.key());
        return keys;
    }

    private static double maxScroll(int count) {
        return Math.max(0, count * ROW_H - (listBottom - listTop - 2 * VIEW_PAD));
    }

    /* ------------------------------------------------- drawing ------------------------------------------------- */

    static void render(Screen screen, GuiGraphics g, int mouseX, int mouseY, float partial) {
        if (!active(screen)) return;
        Font font = Minecraft.getInstance().font;
        var pose = g.pose();
        pose.pushPose();
        pose.translate(0, 0, 400); // over the map's elements
        g.fill(x, y, x + w, y + h, BACKGROUND);
        g.renderOutline(x, y, w, h, FRAME);
        search.render(g, mouseX, mouseY, partial);
        all.render(g, mouseX, mouseY, partial);
        none.render(g, mouseX, mouseY, partial);

        g.fill(x + PAD, listTop, x + w - PAD, listBottom, LIST_BG);
        g.renderOutline(x + PAD, listTop, w - 2 * PAD, listBottom - listTop, FRAME);
        List<Row> rows = rows();
        scroll = Mth.clamp(scroll, 0, maxScroll(rows.size()));
        int viewLeft = x + PAD + VIEW_PAD, viewRight = x + w - PAD - VIEW_PAD;
        int viewTop = listTop + VIEW_PAD, viewBottom = listBottom - VIEW_PAD;
        if (rows.isEmpty()) {
            g.drawString(font, Component.translatable("gui.roll_mod.prospect_filter.empty"), viewLeft + 2,
                    viewTop + (ROW_H - font.lineHeight) / 2 + 1, 0xD8D8D8, false);
        }
        Component tip = null;
        g.enableScissor(viewLeft, viewTop, viewRight, viewBottom);
        for (int i = 0; i < rows.size(); i++) {
            int top = viewTop + i * ROW_H - (int) scroll;
            if (top + ROW_H < viewTop || top > viewBottom) continue;
            Row row = rows.get(i);
            boolean hover = mouseX >= viewLeft && mouseX < viewRight && mouseY >= Math.max(top, viewTop)
                    && mouseY < Math.min(top + ROW_H, viewBottom);
            if (hover) g.fill(viewLeft, top, viewRight, top + ROW_H, 0x30FFFFFF);
            int bx = viewLeft + 2, by = top + (ROW_H - BOX) / 2;
            int textY = top + (ROW_H - font.lineHeight) / 2 + 1;
            if (row.vein() == null) {
                // Category header: its box ticked when all its veins are shown, a dash when some are.
                List<Vein> members = inCategory(row.category());
                int on = 0;
                for (Vein v : members) if (ProspectMapState.shown(v.key())) on++;
                box(g, bx, by, hover);
                if (on == members.size()) tick(g, bx, by);
                else if (on > 0) g.fill(bx + 2, by + 4, bx + BOX - 2, by + 6, 0xFF6BE36B);
                g.drawString(font, row.category().title(), bx + BOX + 3, textY, 0xFFE0B050, false);
                g.fill(bx + BOX + 3, top + ROW_H - 2, viewRight - 2, top + ROW_H - 1, 0x60E0B050);
                continue;
            }
            Vein v = row.vein();
            boolean on = ProspectMapState.shown(v.key());
            // Veins stand indented under their header.
            bx += INDENT;
            box(g, bx, by, hover);
            if (on) tick(g, bx, by);
            // The vein's map icon at its own 16 px, whole pixels.
            int ix = bx + BOX + 3, iy = top + (ROW_H - ICON) / 2;
            pose.pushPose();
            pose.translate(ix + ICON / 2f, iy + ICON / 2f, 0);
            ProspectIcons.draw(g, v.ores(), ICON);
            pose.popPose();
            // Name: one line, centred on the row, cut when too long (full name in the tooltip then).
            int tx = ix + ICON + 3, tw = viewRight - 2 - tx;
            boolean cut = font.width(v.name()) > tw;
            var line = font.substrByWidth(v.name(), tw);
            g.drawString(font, net.minecraft.locale.Language.getInstance().getVisualOrder(line), tx, textY,
                    on ? 0xFFFFFF : 0xD8D8D8, false);
            if (hover && cut) tip = v.name();
        }
        g.disableScissor();
        pose.popPose();
        if (tip != null) g.renderTooltip(font, tip, mouseX, mouseY);
    }

    /** An empty 10 px check box. */
    private static void box(GuiGraphics g, int bx, int by, boolean hover) {
        g.fill(bx, by, bx + BOX, by + BOX, hover ? 0xFFC0C0C0 : 0xFF8B8B8B);
        g.fill(bx + 1, by + 1, bx + BOX - 1, by + BOX - 1, 0xFF1E1E1E);
    }

    /** A green tick in the 10 px box. */
    private static void tick(GuiGraphics g, int bx, int by) {
        int c = 0xFF6BE36B;
        // Two pixels thick: down from the left, up to the right.
        int[][] px = { {2, 4}, {2, 5}, {3, 5}, {3, 6}, {4, 6}, {4, 7}, {5, 5}, {5, 6}, {6, 4}, {6, 5}, {7, 2}, {7, 3},
                {7, 4}, {8, 2}, {8, 3} };
        for (int[] p : px) g.fill(bx + p[0], by + p[1], bx + p[0] + 1, by + p[1] + 1, c);
    }

    /* ------------------------------------------------- input ------------------------------------------------- */

    static void onMouseClicked(ScreenEvent.MouseButtonPressed.Pre event) {
        Screen screen = event.getScreen();
        if (!active(screen)) return;
        double mx = event.getMouseX(), my = event.getMouseY();
        if (!inside(mx, my)) {
            search.setFocused(false);
            return;
        }
        event.setCanceled(true);
        int button = event.getButton();
        search.setFocused(search.isMouseOver(mx, my));
        if (search.isFocused()) {
            search.mouseClicked(mx, my, button);
            return;
        }
        if (all.mouseClicked(mx, my, button) || none.mouseClicked(mx, my, button)) return;
        if (button != 0) return;
        int viewTop = listTop + VIEW_PAD, viewBottom = listBottom - VIEW_PAD;
        if (mx < x + PAD + VIEW_PAD || mx >= x + w - PAD - VIEW_PAD || my < viewTop || my >= viewBottom) return;
        int index = (int) ((my - viewTop + scroll) / ROW_H);
        List<Row> rows = rows();
        if (index < 0 || index >= rows.size()) return;
        Row row = rows.get(index);
        if (row.vein() == null) {
            // A header: all of its veins on, unless they all are already; then all off.
            List<String> members = keys(inCategory(row.category()));
            boolean allOn = members.stream().allMatch(ProspectMapState::shown);
            ProspectMapState.setShown(members, !allOn);
        } else {
            String key = row.vein().key();
            ProspectMapState.setShown(List.of(key), !ProspectMapState.shown(key));
        }
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
    }

    static void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if (active(event.getScreen()) && inside(event.getMouseX(), event.getMouseY())) event.setCanceled(true);
    }

    static void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        if (active(event.getScreen()) && inside(event.getMouseX(), event.getMouseY())) event.setCanceled(true);
    }

    static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        if (!active(event.getScreen()) || !inside(event.getMouseX(), event.getMouseY())) return;
        event.setCanceled(true);
        scroll = Mth.clamp(scroll - event.getScrollDeltaY() * ROW_H * 2, 0, maxScroll(filtered().size()));
    }

    static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (!active(event.getScreen()) || !search.isFocused()) return;
        if (event.getKeyCode() == GLFW.GLFW_KEY_ESCAPE) {
            search.setFocused(false);
            event.setCanceled(true);
            return;
        }
        search.keyPressed(event.getKeyCode(), event.getScanCode(), event.getModifiers());
        event.setCanceled(true); // the map's own keys stay quiet while typing
    }

    static void onCharTyped(ScreenEvent.CharacterTyped.Pre event) {
        if (!active(event.getScreen()) || !search.isFocused()) return;
        search.charTyped(event.getCodePoint(), event.getModifiers());
        event.setCanceled(true);
    }
}
