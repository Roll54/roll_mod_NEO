package com.roll_54.roll_mod.minestar.hub.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lowdragmc.lowdraglib2.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents;
import com.roll_54.roll_mod.minestar.data.MinestarFiles;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The order a player dragged a hub list into (homes, warps): ids per list, in {@code config/roll_mod/hub_order.json}.
 * Client preference. Rows not in the saved order keep their own order after the ones that are.
 *
 * <p>{@link #draggable} makes a row draggable onto another row of the same list: dropping it puts it in that row's
 * place. {@link #version} changes on every move, for the list's rebuild signature.
 */
public final class HubOrder {

    /** What is being dragged: a row of {@code list}. */
    private record Drag(String list, String id) {}

    private static final int DROP_MARK = 0xFFFFFFFF;

    private static Map<String, List<String>> orders;
    private static int version;

    private HubOrder() {}

    public static int version() {
        return version;
    }

    /** {@code items} in the player's order for {@code list}. */
    public static <T> List<T> sorted(String list, List<T> items, Function<T, String> id) {
        List<String> order = load().get(list);
        if (order == null || order.isEmpty()) return items;
        List<T> out = new ArrayList<>(items);
        out.sort((a, b) -> Integer.compare(rank(order, id.apply(a)), rank(order, id.apply(b))));
        return out;
    }

    private static int rank(List<String> order, String id) {
        int at = order.indexOf(id);
        return at < 0 ? Integer.MAX_VALUE : at;
    }

    /**
     * Lets {@code row} be dragged onto another row of {@code list}, and other rows dropped on it. {@code shown} is the
     * list's ids as they are on screen now, which a drop rewrites as the saved order.
     */
    public static void draggable(UIElement row, String list, String id, Supplier<List<String>> shown) {
        row.addEventListener(UIEvents.MOUSE_DOWN, e -> {
            if (e.button == 0) row.startDrag(new Drag(list, id), null);
        });
        row.addEventListener(UIEvents.DRAG_ENTER, e -> {
            if (e.dragHandler.draggingObject instanceof Drag d && d.list().equals(list) && !d.id().equals(id)) {
                row.style(s -> s.overlayTexture(new ColorBorderTexture(1, DROP_MARK)));
            }
        }, true);
        row.addEventListener(UIEvents.DRAG_LEAVE, e -> row.style(s -> s.overlayTexture(com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture.EMPTY)), true);
        row.addEventListener(UIEvents.DRAG_END, e -> row.style(s -> s.overlayTexture(com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture.EMPTY)), true);
        row.addEventListener(UIEvents.DRAG_PERFORM, e -> {
            row.style(s -> s.overlayTexture(com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture.EMPTY));
            if (!(e.dragHandler.draggingObject instanceof Drag d) || !d.list().equals(list) || d.id().equals(id)) return;
            List<String> order = new ArrayList<>(shown.get());
            if (!order.remove(d.id())) return;
            int at = order.indexOf(id);
            if (at < 0) return;
            // Dragged down: after the row it was dropped on; up: before it.
            int from = shown.get().indexOf(d.id());
            order.add(from < shown.get().indexOf(id) ? at + 1 : at, d.id());
            set(list, order);
            e.stopPropagation();
        });
    }

    private static void set(String list, List<String> order) {
        load().put(list, List.copyOf(order));
        version++;
        JsonObject json = new JsonObject();
        orders.forEach((name, ids) -> {
            JsonArray array = new JsonArray();
            ids.forEach(array::add);
            json.add(name, array);
        });
        MinestarFiles.write(file(), json);
    }

    private static Map<String, List<String>> load() {
        if (orders != null) return orders;
        orders = new HashMap<>();
        try {
            JsonObject json = MinestarFiles.readObject(file());
            for (String name : json.keySet()) {
                if (!json.get(name).isJsonArray()) continue;
                List<String> ids = new ArrayList<>();
                json.getAsJsonArray(name).forEach(el -> ids.add(el.getAsString()));
                orders.put(name, ids);
            }
        } catch (RuntimeException ignored) {
            // A hand-edited file that does not read: the default order.
        }
        return orders;
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("roll_mod").resolve("hub_order.json");
    }
}
