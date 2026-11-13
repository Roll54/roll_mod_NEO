package com.roll_54.roll_mod.minestar.fzzyui;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.Icons;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.data.Vertical;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.roll_54.roll_mod.minestar.theme.RollUi;
import com.roll_54.roll_mod.minestar.ui.ExpandableWindow;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import me.fzzyhmstrs.fzzy_config.annotations.Action;
import me.fzzyhmstrs.fzzy_config.api.ConfigApi;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Fzzy Config's config screen, rebuilt in LdLib: the configs of one namespace in a hub-style window
 * (moved by its title bar, resized from its edges, maximized by double-click or button, remembered in
 * {@link ConfigWindowState}), wearing the hub theme.
 *
 * <p>Left, the pages — each config and each of its sections. Right, a search box and the current page's
 * entries: name (description as tooltip), an editor ({@link ConfigEditors}), revert and reset buttons.
 * Below, the change count and Undo / Revert / Apply / Done. Done, the close button and Esc apply what
 * changed, as Fzzy's own screen does on close. Ctrl+S applies, Ctrl+Z undoes, Ctrl+F searches.
 *
 * <p>Opened in place of Fzzy's screen by {@code FzzyConfigRegistryMixin}; the data side — walking,
 * validation, undo, apply, server sync, permissions — is Fzzy's own, see {@link ConfigModel}.
 */
@OnlyIn(Dist.CLIENT)
public final class RollConfigScreen extends ModularUIScreen implements ConfigEditors.KeyCapture {

    private static final int PADDING = 6;
    private static final int BAR_H = 16;
    private static final int SIDEBAR_W = 112;
    private static final int ROW_GAP = 3;
    private static final int GREY = 0xFFA0A0A0;

    private static boolean listening;

    @Nullable private final Screen parent;
    private ConfigModel model;
    private ConfigModel.Page page;
    private final Set<String> collapsedGroups = new HashSet<>();
    /** Re-reads every visible row's state after a change. */
    private final List<Runnable> rowRefreshers = new ArrayList<>();
    @Nullable private Listener capturing;
    /** The config changed underneath us (sync from the server) while edits were pending. */
    private boolean stale;

    private final Label title = new Label();
    private final ScrollerView pageList = scroller();
    private final ScrollerView content = scroller();
    private final TextField search = new TextField();
    private final Label changes = new Label();
    private final Button undo = textButton("gui.roll_mod.config.undo");
    private final Button revert = textButton("gui.roll_mod.config.revert");
    private final Button apply = textButton("gui.roll_mod.config.apply");

    private RollConfigScreen(UIElement root, ExpandableWindow window, @Nullable Screen parent, ConfigModel model,
                             ConfigModel.Page page) {
        super(ModularUI.of(UI.of(root, RollUi.sheets(), size -> {
            window.fit((int) size.getWidth(), (int) size.getHeight());
            return size;
        })), Component.translatable("gui.roll_mod.config.title"));
        this.parent = parent;
        this.model = model;
        this.page = page;
        build(root, window);
    }

    /**
     * The screen for a Fzzy scope — {@code namespace}, {@code namespace.config} or a section's scope —
     * or null when there is nothing to show, so Fzzy's own screen is used instead.
     */
    @Nullable
    public static Screen open(String scope, @Nullable Screen parent) {
        listenOnce();
        String namespace = namespaceOf(scope);
        if (namespace == null) return null;
        ConfigModel model = ConfigModel.build(namespace);
        if (model.configs.isEmpty()) return null;
        ConfigModel.Page start = model.pages.getOrDefault(scope, model.configs.get(0));
        UIElement root = new UIElement();
        ExpandableWindow window = new ExpandableWindow(ConfigWindowState.get(), ConfigWindowState.DEFAULT_W,
                ConfigWindowState.DEFAULT_H, ConfigWindowState.MIN_W, ConfigWindowState.MIN_H);
        return new RollConfigScreen(root, window, parent, model, start);
    }

    /** The namespace a scope belongs to: its longest leading part Fzzy knows as one. */
    @Nullable
    private static String namespaceOf(String scope) {
        Set<String> known = FzzyBridge.namespaces();
        String s = scope;
        while (true) {
            if (known.contains(s)) return s;
            int dot = s.lastIndexOf('.');
            if (dot < 0) return null;
            s = s.substring(0, dot);
        }
    }

    /** Fzzy tells us when a config changes under the screen: a sync from the server, or another apply. */
    private static void listenOnce() {
        if (listening) return;
        listening = true;
        ConfigApi.event().onSyncClient((id, config) -> refreshOpen(id.getNamespace()));
        ConfigApi.event().onUpdateClient((id, config) -> refreshOpen(id.getNamespace()));
    }

    private static void refreshOpen(String namespace) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.screen instanceof RollConfigScreen screen && screen.model.namespace.equals(namespace)) {
                screen.onExternalChange();
            }
        });
    }

    /* ------------------------------------------------------- layout ------------------------------------------------------- */

    private void build(UIElement root, ExpandableWindow window) {
        root.layout(l -> l.widthPercent(100).heightPercent(100));
        window.layout(l -> l.flexDirection(FlexDirection.COLUMN).paddingAll(PADDING).gapRow(4));
        window.addClass("panel_bg");
        root.addChild(window);

        // Title bar: the page's path, then maximize and close. Dragging it moves the window.
        UIElement titleBar = new UIElement();
        titleBar.layout(l -> l.flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER).height(BAR_H)
                .flexShrink(0).gapColumn(2));
        title.layout(l -> l.flexGrow(1).heightPercent(100));
        title.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE).textShadow(true));
        titleBar.addChildren(title,
                windowButton(() -> window.maximized() ? Icons.WINDOW_RESTORE : Icons.WINDOW_MAXIMIZE,
                        "gui.roll_mod.config.maximize.tip", window::toggleMaximize),
                windowButton(() -> Icons.WINDOW_CLOSE, "gui.roll_mod.config.close.tip", this::onClose));
        window.dragBy(titleBar);
        window.dragBy(title);
        window.addChild(titleBar);

        // Body: pages left; search and entries right.
        UIElement body = new UIElement();
        body.layout(l -> l.flexDirection(FlexDirection.ROW).flexGrow(1).flexBasis(0).gapColumn(4));
        pageList.layout(l -> l.width(SIDEBAR_W).flexShrink(0).heightPercent(100));
        UIElement main = new UIElement();
        main.layout(l -> l.flexDirection(FlexDirection.COLUMN).flexGrow(1).heightPercent(100).gapRow(3));
        search.setText("", false);
        search.layout(l -> l.height(ConfigEditors.CONTROL_H).widthPercent(100).flexShrink(0));
        search.setTextResponder(s -> showEntries());
        search.style(s -> s.tooltips(Component.translatable("gui.roll_mod.config.search.tip")));
        content.layout(l -> l.flexGrow(1).flexBasis(0).widthPercent(100));
        main.addChildren(search, content);
        body.addChildren(pageList, main);
        window.addChild(body);

        // Footer: what changed, and what to do with it.
        UIElement footer = new UIElement();
        footer.layout(l -> l.flexDirection(FlexDirection.ROW).alignItems(AlignItems.CENTER).height(BAR_H)
                .flexShrink(0).gapColumn(3));
        changes.layout(l -> l.flexGrow(1).heightPercent(100));
        changes.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));
        undo.setOnClick(e -> undoLast());
        revert.setOnClick(e -> revertAll());
        apply.setOnClick(e -> applyNow());
        Button done = textButton("gui.roll_mod.config.done");
        done.setOnClick(e -> onClose());
        footer.addChildren(changes, undo, revert, apply, done);
        window.addChild(footer);

        showPage(page);
    }

    private static ScrollerView scroller() {
        ScrollerView view = new ScrollerView();
        view.scrollerStyle(s -> s.mode(ScrollerMode.VERTICAL)
                .verticalScrollDisplay(ScrollDisplay.AUTO)
                .horizontalScrollDisplay(ScrollDisplay.NEVER));
        view.viewContainer(c -> c.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100).gapRow(ROW_GAP)));
        return view;
    }

    private static Button textButton(String key) {
        Button button = new Button();
        button.setText(Component.translatable(key));
        button.layout(l -> l.height(BAR_H - 2).flexShrink(0));
        return button;
    }

    private static UIElement windowButton(java.util.function.Supplier<IGuiTexture> icon, String tip, Runnable action) {
        UIElement button = new UIElement();
        button.layout(l -> l.width(BAR_H - 2).height(BAR_H - 2).flexShrink(0));
        // Through a supplier every frame: LdLib swaps its Icons fields at resource load.
        button.style(s -> s.background(IGuiTexture.dynamic(icon)).tooltips(Component.translatable(tip)));
        button.addEventListener(com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents.CLICK, e -> action.run());
        return button;
    }

    /* ------------------------------------------------------ showing ------------------------------------------------------ */

    private void showPage(ConfigModel.Page target) {
        page = target;
        search.setText("", false);
        rebuildPageList();
        showEntries();
    }

    /** The configs and their sections, indented, the current one marked. */
    private void rebuildPageList() {
        pageList.clearAllScrollViewChildren();
        for (ConfigModel.Page config : model.configs) addPageButton(config, 0);
        MutableComponent path = Component.empty();
        for (ConfigModel.Page p : page.path()) {
            if (!path.getSiblings().isEmpty()) path.append(Component.literal(" › ").withStyle(ChatFormatting.GRAY));
            path.append(p.name.copy());
        }
        title.setText(path);
    }

    private void addPageButton(ConfigModel.Page target, int depth) {
        Button button = new Button();
        MutableComponent name = target.name.copy();
        button.setText(target == page ? Component.literal("› ").append(name).withStyle(ChatFormatting.YELLOW) : name);
        button.layout(l -> l.height(ConfigEditors.CONTROL_H).widthPercent(100).marginLeft(depth * 8f));
        button.textStyle(t -> t.textWrap(TextWrap.HIDE));
        if (target.desc != null) button.style(s -> s.tooltips(target.desc));
        button.setOnClick(e -> showPage(target));
        pageList.addScrollViewChild(button);
        for (ConfigModel.Node node : target.children) {
            if (node instanceof ConfigModel.Page section) addPageButton(section, depth + 1);
        }
    }

    /** The current page's rows, or every matching entry while the search box has text. */
    private void showEntries() {
        content.clearAllScrollViewChildren();
        rowRefreshers.clear();
        String query = search.getValue() == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            addNodes(page.children, 0);
        } else {
            int found = 0;
            for (ConfigModel.Entry entry : model.allEntries()) {
                if (matches(entry, query)) {
                    addEntry(entry, 0, true);
                    found++;
                }
            }
            if (found == 0) content.addScrollViewChild(note(Component.translatable("gui.roll_mod.config.search.none")));
        }
        refresh();
    }

    private static boolean matches(ConfigModel.Entry entry, String query) {
        return entry.name().getString().toLowerCase(Locale.ROOT).contains(query)
                || (entry.desc() != null && entry.desc().getString().toLowerCase(Locale.ROOT).contains(query))
                || entry.scope().toLowerCase(Locale.ROOT).contains(query);
    }

    private void addNodes(List<ConfigModel.Node> nodes, int depth) {
        for (ConfigModel.Node node : nodes) {
            switch (node) {
                case ConfigModel.Page section -> content.addScrollViewChild(sectionRow(section, depth));
                case ConfigModel.Group group -> addGroup(group, depth);
                case ConfigModel.Entry entry -> addEntry(entry, depth, false);
                case ConfigModel.Button button -> content.addScrollViewChild(actionRow(button, depth));
                case ConfigModel.Stub stub -> content.addScrollViewChild(stubRow(stub, depth));
            }
        }
    }

    private UIElement sectionRow(ConfigModel.Page section, int depth) {
        Button button = new Button();
        button.setText(section.name.copy().append(" ›"));
        button.layout(l -> l.height(ConfigEditors.CONTROL_H + 2).widthPercent(100).marginLeft(depth * 8f));
        if (section.desc != null) button.style(s -> s.tooltips(section.desc));
        button.setOnClick(e -> showPage(section));
        return button;
    }

    private void addGroup(ConfigModel.Group group, int depth) {
        boolean collapsed = collapsedGroups.contains(group.id())
                || (group.collapsedByDefault() && !collapsedGroups.contains("!" + group.id()));
        Button header = new Button();
        header.setText(Component.literal(collapsed ? "▸ " : "▾ ").append(group.name().copy()));
        header.layout(l -> l.height(ConfigEditors.CONTROL_H).widthPercent(100).marginLeft(depth * 8f));
        if (group.desc() != null) header.style(s -> s.tooltips(group.desc()));
        header.setOnClick(e -> {
            if (collapsed) {
                collapsedGroups.remove(group.id());
                collapsedGroups.add("!" + group.id());
            } else {
                collapsedGroups.add(group.id());
                collapsedGroups.remove("!" + group.id());
            }
            showEntries();
        });
        content.addScrollViewChild(header);
        if (!collapsed) addNodes(group.children(), depth + 1);
    }

    /** Name, editor, revert and reset — and for lists and maps, their panel underneath. */
    private void addEntry(ConfigModel.Entry entry, int depth, boolean showPage) {
        ConfigEditors.Binding binding = ConfigEditors.fieldBinding(entry.field(), this::refresh);
        ConfigEditors.Editor editor = ConfigEditors.create(entry.field(), binding, entry.scope(), this);

        Label name = new Label();
        MutableComponent text = entry.name().copy();
        if (showPage) text = Component.literal(entry.page().name.getString() + " › ").withStyle(ChatFormatting.GRAY).append(text);
        name.setText(text);
        name.layout(l -> l.widthPercent(42).flexShrink(0).height(ConfigEditors.CONTROL_H));
        name.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));
        List<Component> tips = new ArrayList<>();
        if (entry.desc() != null && !entry.desc().getString().isEmpty()) tips.add(entry.desc());
        for (Action action : entry.actions()) tips.add(action.getSettingTooltip().copy().withStyle(ChatFormatting.GOLD));
        if (!tips.isEmpty()) name.style(s -> s.tooltips(tips.toArray(Component[]::new)));

        UIElement revertOne = ConfigEditors.iconButton(Icons.HISTORY, false, () -> {
            entry.field().revert();
            showEntries();
        });
        revertOne.style(s -> s.tooltips(Component.translatable("gui.roll_mod.config.revert_one.tip")));
        UIElement reset = ConfigEditors.iconButton(Icons.REPLAY, false, () -> {
            entry.field().restore();
            showEntries();
        });
        reset.style(s -> s.tooltips(Component.translatable("gui.roll_mod.config.reset.tip")));
        rowRefreshers.add(() -> {
            revertOne.setActive(entry.field().peekState());
            reset.setActive(!entry.field().isDefault());
        });

        UIElement inline = editor.inline();
        inline.layout(l -> l.flexGrow(1));
        UIElement row = ConfigEditors.row(name, inline, revertOne, reset);
        row.layout(l -> l.widthPercent(100).marginLeft(depth * 8f).flexGrow(0));
        if (editor.panel() == null) {
            content.addScrollViewChild(row);
            return;
        }
        UIElement column = new UIElement();
        column.layout(l -> l.flexDirection(FlexDirection.COLUMN).widthPercent(100));
        column.addChildren(row, editor.panel());
        content.addScrollViewChild(column);
    }

    private UIElement actionRow(ConfigModel.Button node, int depth) {
        Button button = new Button();
        Component label = FzzyBridge.actionTitle(node.action());
        button.setText(label != null ? label : node.name());
        button.layout(l -> l.height(ConfigEditors.CONTROL_H + 2).widthPercent(100).marginLeft(depth * 8f));
        if (node.desc() != null) button.style(s -> s.tooltips(node.desc()));
        button.setActive(FzzyBridge.actionActive(node.action()));
        button.setOnClick(e -> FzzyBridge.runAction(node.action()));
        return button;
    }

    private UIElement stubRow(ConfigModel.Stub stub, int depth) {
        Label name = new Label();
        name.setText(stub.name().copy().withStyle(ChatFormatting.DARK_GRAY));
        name.layout(l -> l.widthPercent(42).flexShrink(0).height(ConfigEditors.CONTROL_H));
        name.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));
        Label why = new Label();
        why.setText(Component.translatable(stub.outOfGame() ? "gui.roll_mod.config.out_of_game" : "gui.roll_mod.config.no_perms")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        why.layout(l -> l.flexGrow(1).height(ConfigEditors.CONTROL_H));
        why.textStyle(t -> t.textAlignVertical(Vertical.CENTER).textWrap(TextWrap.HIDE));
        UIElement row = ConfigEditors.row(name, why);
        row.layout(l -> l.widthPercent(100).marginLeft(depth * 8f));
        return row;
    }

    private static UIElement note(Component text) {
        Label label = new Label();
        label.setText(text.copy().withStyle(ChatFormatting.GRAY));
        label.layout(l -> l.widthPercent(100).height(ConfigEditors.CONTROL_H));
        return label;
    }

    /** After any change: row buttons and the footer. */
    private void refresh() {
        rowRefreshers.forEach(Runnable::run);
        int count = model.changeCount();
        MutableComponent text = count == 0
                ? Component.translatable("gui.roll_mod.config.no_changes").withStyle(s -> s.withColor(GREY))
                : Component.translatable("gui.roll_mod.config.changes", count).withStyle(ChatFormatting.YELLOW);
        if (stale) text.append(Component.literal("  ")).append(Component.translatable("gui.roll_mod.config.stale").withStyle(ChatFormatting.RED));
        changes.setText(text);
        undo.setActive(count > 0);
        revert.setActive(count > 0);
        apply.setActive(count > 0);
    }

    /* ------------------------------------------------------ actions ------------------------------------------------------ */

    private void undoLast() {
        model.root.revertLast();
        showEntries();
    }

    private void revertAll() {
        model.root.revert();
        showEntries();
    }

    private void applyNow() {
        model.apply(false);
        refresh();
    }

    /** The config changed underneath: rebuild, unless that would throw away the player's edits. */
    private void onExternalChange() {
        if (model.hasChanges()) {
            stale = true;
            refresh();
            return;
        }
        String scope = page.scope;
        ConfigModel fresh = ConfigModel.build(model.namespace);
        if (fresh.configs.isEmpty()) return;
        model = fresh;
        stale = false;
        showPage(model.pages.getOrDefault(scope, model.configs.get(0)));
    }

    /** Done, the close button and Esc: apply what changed, then back to where the screen came from. */
    @Override
    public void onClose() {
        model.apply(true);
        ConfigWindowState.get().save();
        Minecraft.getInstance().setScreen(parent);
    }

    /* ------------------------------------------------------- keys ------------------------------------------------------- */

    @Override
    public void capture(Listener listener) {
        capturing = listener;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (capturing != null) {
            if (isModifier(keyCode)) return true;
            Listener listener = capturing;
            capturing = null;
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                listener.pressed(-1, false, false, false);
            } else {
                listener.pressed(keyCode, hasControlDown(), hasShiftDown(), hasAltDown());
            }
            return true;
        }
        if (hasControlDown() && !hasShiftDown() && !hasAltDown()) {
            switch (keyCode) {
                case GLFW.GLFW_KEY_S -> {
                    applyNow();
                    return true;
                }
                case GLFW.GLFW_KEY_Z -> {
                    undoLast();
                    return true;
                }
                case GLFW.GLFW_KEY_F -> {
                    search.focus();
                    return true;
                }
                default -> { }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private static boolean isModifier(int key) {
        return key == GLFW.GLFW_KEY_LEFT_CONTROL || key == GLFW.GLFW_KEY_RIGHT_CONTROL
                || key == GLFW.GLFW_KEY_LEFT_SHIFT || key == GLFW.GLFW_KEY_RIGHT_SHIFT
                || key == GLFW.GLFW_KEY_LEFT_ALT || key == GLFW.GLFW_KEY_RIGHT_ALT;
    }

    /** Vanilla's menu background: the panorama with no world, blurred, darkened. */
    @Override
    public void renderBackground(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (Minecraft.getInstance().level == null) renderPanorama(graphics, partialTick);
        renderBlurredBackground(partialTick);
        renderMenuBackground(graphics);
    }
}
