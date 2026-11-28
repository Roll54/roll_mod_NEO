package com.roll_54.roll_mod_client.compat.xaero;

import com.roll_54.roll_mod.prospecting.Discovery;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Which prospected ore veins the maps show, opened from the ore button on Xaero's World Map and laid out like that
 * mod's own menus: a dark screen over the map, a title, a search field, "Select all" / "Deselect all", and a
 * scrolling list of the veins found in this dimension, each a check box followed by the vein's name. The two buttons
 * act on what the search shows (everything while it is empty). Changes apply at once.
 *
 * @deprecated replaced by {@link ProspectVeinPanel}, the small panel on the map itself; kept, no longer opened.
 */
@Deprecated
@OnlyIn(Dist.CLIENT)
public final class ProspectVeinFilterScreen extends Screen {

    private static final ResourceLocation BOX = ResourceLocation.withDefaultNamespace("widget/checkbox");
    private static final ResourceLocation BOX_HOVER = ResourceLocation.withDefaultNamespace("widget/checkbox_highlighted");
    private static final ResourceLocation TICKED = ResourceLocation.withDefaultNamespace("widget/checkbox_selected");
    private static final ResourceLocation TICKED_HOVER =
            ResourceLocation.withDefaultNamespace("widget/checkbox_selected_highlighted");

    /** Check box side, row height, and the gap (≥ 3 px) between box and name and from the row's edges. */
    private static final int BOX_SIZE = 17, ROW_HEIGHT = 21, PAD = 4;
    private static final int TOP = 8, CONTENT_W = 260;

    private final Screen parent;
    /** Every vein found here: key → its name, sorted by name. */
    private final Map<String, Component> veins = new LinkedHashMap<>();
    private EditBox search;
    private VeinList list;

    public ProspectVeinFilterScreen(Screen parent) {
        super(Component.translatable("gui.roll_mod.prospect_filter.title"));
        this.parent = parent;
        Map<String, List<Discovery>> found = new LinkedHashMap<>();
        ProspectChunk.all().forEachRemaining(c -> ProspectVeins.split(c.ores())
                .forEach(p -> found.putIfAbsent(p.key(), p.ores())));
        List<Map.Entry<String, Component>> named = new ArrayList<>();
        found.forEach((key, ores) -> named.add(Map.entry(key, ProspectVeins.name(key, ores))));
        named.sort((a, b) -> a.getValue().getString().compareToIgnoreCase(b.getValue().getString()));
        named.forEach(e -> veins.put(e.getKey(), e.getValue()));
    }

    @Override
    protected void init() {
        int left = (width - CONTENT_W) / 2;
        int y = TOP + font.lineHeight + 6;
        String query = search == null ? "" : search.getValue();
        search = new EditBox(font, left, y, CONTENT_W, 20, Component.translatable("gui.roll_mod.prospect_filter.search"));
        search.setHint(Component.translatable("gui.roll_mod.prospect_filter.search")
                .withStyle(s -> s.withColor(0xD8D8D8)));
        search.setValue(query);
        search.setResponder(q -> list.refill());
        addRenderableWidget(search);

        y += 24;
        int half = (CONTENT_W - 4) / 2;
        addRenderableWidget(Button.builder(Component.translatable("gui.roll_mod.prospect_filter.all"),
                b -> ProspectMapState.setShown(list.keys(), true)).bounds(left, y, half, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.roll_mod.prospect_filter.none"),
                b -> ProspectMapState.setShown(list.keys(), false)).bounds(left + half + 4, y, half, 20).build());

        y += 24;
        list = new VeinList(minecraft, width, height - 30 - y, y);
        addRenderableWidget(list);
        list.refill();

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds((width - 150) / 2, height - 26, 150, 20).build());
        setInitialFocus(search);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        super.render(graphics, mouseX, mouseY, partial);
        graphics.drawCenteredString(font, title, width / 2, TOP, 0xFFFFFF);
    }

    /** Dark over the map, as Xaero's menus are, instead of the vanilla blur. */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        graphics.fill(0, 0, width, height, 0xB0000000);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private final class VeinList extends ObjectSelectionList<VeinList.Row> {

        VeinList(Minecraft mc, int width, int height, int y) {
            super(mc, width, height, y, ROW_HEIGHT);
        }

        /** The rows matching the search again. */
        void refill() {
            clearEntries();
            String q = search.getValue().trim().toLowerCase(Locale.ROOT);
            veins.forEach((key, name) -> {
                if (q.isEmpty() || name.getString().toLowerCase(Locale.ROOT).contains(q)) addEntry(new Row(key, name));
            });
            setScrollAmount(getScrollAmount());
        }

        /** The keys of the rows shown: what "Select all" / "Deselect all" act on. */
        List<String> keys() {
            List<String> keys = new ArrayList<>();
            for (Row row : children()) keys.add(row.key);
            return keys;
        }

        @Override
        public int getRowWidth() {
            return CONTENT_W;
        }

        @Override
        protected boolean isSelectedItem(int index) {
            return false;
        }

        final class Row extends ObjectSelectionList.Entry<Row> {
            private final String key;
            private final Component name;

            Row(String key, Component name) {
                this.key = key;
                this.name = name;
            }

            @Override
            public void render(GuiGraphics g, int index, int top, int left, int rowWidth, int rowHeight, int mouseX,
                               int mouseY, boolean hovered, float partial) {
                boolean on = ProspectMapState.shown(key);
                int boxY = top + (rowHeight - BOX_SIZE) / 2;
                g.blitSprite(on ? (hovered ? TICKED_HOVER : TICKED) : (hovered ? BOX_HOVER : BOX),
                        left + PAD, boxY, BOX_SIZE, BOX_SIZE);
                int textX = left + PAD + BOX_SIZE + PAD;
                int textW = rowWidth - (textX - left) - PAD;
                // One line, centred on the box; cut with an ellipsis when it does not fit.
                var text = font.substrByWidth(name, textW);
                boolean cut = font.width(name) > textW;
                int textY = top + (rowHeight - font.lineHeight) / 2 + 1;
                g.drawString(font, net.minecraft.locale.Language.getInstance().getVisualOrder(text), textX, textY,
                        on ? 0xFFFFFF : 0xD8D8D8);
                if (cut && hovered) setTooltipForNextRenderPass(name);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button != 0) return false;
                ProspectMapState.setShown(List.of(key), !ProspectMapState.shown(key));
                minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1f));
                return true;
            }

            @Override
            public Component getNarration() {
                return name;
            }
        }
    }
}
