package com.roll_54.roll_mod.minestar.fzzyui;

import me.fzzyhmstrs.fzzy_config.annotations.Action;
import me.fzzyhmstrs.fzzy_config.api.SaveType;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.config.ConfigAction;
import me.fzzyhmstrs.fzzy_config.config.ConfigGroup;
import me.fzzyhmstrs.fzzy_config.config.ConfigSection;
import me.fzzyhmstrs.fzzy_config.entry.EntryCreator;
import me.fzzyhmstrs.fzzy_config.entry.EntryFlag;
import me.fzzyhmstrs.fzzy_config.entry.EntryParent;
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImplClient;
import me.fzzyhmstrs.fzzy_config.impl.ConfigSet;
import me.fzzyhmstrs.fzzy_config.impl.PermResult;
import me.fzzyhmstrs.fzzy_config.screen.internal.ConfigRootUpdateManager;
import me.fzzyhmstrs.fzzy_config.screen.internal.ConfigSingleUpdateManager;
import me.fzzyhmstrs.fzzy_config.updates.Updatable;
import me.fzzyhmstrs.fzzy_config.util.Translatable;
import me.fzzyhmstrs.fzzy_config.validation.ValidatedField;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * One namespace's configs as a tree the LdLib screen can draw: a {@link Page} per config and per
 * section, holding entries, groups, buttons and permission stubs in declaration order.
 *
 * <p>Built the way Fzzy's own screen builds its lists ({@code ConfigScreenManager.walkConfig}): each
 * config is walked with {@code @NonSync} entries included; plain fields are wrapped in a validated
 * field taken from the pristine base config, so their defaults are right; every editable field is
 * registered with one {@link ConfigSingleUpdateManager} per config, which is what records changes,
 * undoes them, writes plain fields back and sends synced changes to the server on apply. The tree only
 * holds those live fields — editors write straight into them.
 */
@OnlyIn(Dist.CLIENT)
final class ConfigModel {

    /** Anything a page lists. */
    sealed interface Node permits Page, Group, Entry, Button, Stub {
        Component name();

        @Nullable
        Component desc();
    }

    /** A config, or a section of one: something you navigate into. */
    static final class Page implements Node {
        final String scope;
        final Component name;
        @Nullable final Component desc;
        @Nullable final Page parent;
        final ConfigSingleUpdateManager manager;
        final List<Node> children = new ArrayList<>();

        Page(String scope, Translatable.Result texts, @Nullable Page parent, ConfigSingleUpdateManager manager) {
            this.scope = scope;
            this.name = texts.getName();
            this.desc = texts.getDesc();
            this.parent = parent;
            this.manager = manager;
        }

        @Override public Component name() { return name; }
        @Override @Nullable public Component desc() { return desc; }

        /** This page and its ancestors, root first. */
        List<Page> path() {
            LinkedList<Page> out = new LinkedList<>();
            for (Page p = this; p != null; p = p.parent) out.addFirst(p);
            return out;
        }
    }

    /** An inline, collapsible run of entries ({@link ConfigGroup}). */
    record Group(String id, Component name, @Nullable Component desc, boolean collapsedByDefault,
                 List<Node> children) implements Node {}

    /** An editable value. */
    record Entry(String scope, Component name, @Nullable Component desc, ValidatedField<?> field,
                 Set<Action> actions, Page page) implements Node {}

    /** A {@link ConfigAction}: a button that runs something. */
    record Button(String scope, Component name, @Nullable Component desc, ConfigAction action) implements Node {}

    /** An entry this player may not edit: no permission, or not editable outside a world. */
    record Stub(String scope, Component name, @Nullable Component desc, boolean outOfGame) implements Node {}

    final String namespace;
    final ConfigRootUpdateManager root = new ConfigRootUpdateManager();
    /** One root page per config, in title order. */
    final List<Page> configs = new ArrayList<>();
    /** Every page by scope. */
    final Map<String, Page> pages = new LinkedHashMap<>();
    final List<ConfigSingleUpdateManager> managers = new ArrayList<>();

    private ConfigModel(String namespace) {
        this.namespace = namespace;
    }

    /** Walks every GUI-visible config of {@code namespace}. Empty if there are none. */
    static ConfigModel build(String namespace) {
        ConfigModel model = new ConfigModel(namespace);
        int perm = FzzyBridge.playerPermissionLevel();
        for (String path : FzzyBridge.configPaths(namespace)) {
            String key = namespace + "." + path;
            Config active = FzzyBridge.activeConfig(key);
            Config base = FzzyBridge.baseConfig(key);
            if (active == null || base == null) continue;
            model.walkConfig(new ConfigSet(active, base, FzzyBridge.clientOnly(key), FzzyBridge.isRootConfig(active)), perm);
        }
        model.configs.sort(Comparator.comparing(p -> p.name.getString().toLowerCase(Locale.ROOT)));
        // Snapshot every value, so "changed" means "changed since the screen opened".
        for (ConfigSingleUpdateManager m : model.managers) {
            m.invalidatePush();
            m.pushUpdatableStates("");
        }
        return model;
    }

    private void walkConfig(ConfigSet set, int perm) {
        Config config = set.getActive();
        Config base = set.getBase();
        String prefix = config.getId().toLanguageKey();
        ConfigSingleUpdateManager manager = new ConfigSingleUpdateManager(set, new ArrayList<>(), perm);
        managers.add(manager);
        root.addChild(manager);

        Page rootPage = new Page(prefix, FzzyBridge.configTexts(config, prefix), null, manager);
        configs.add(rootPage);
        pages.put(prefix, rootPage);

        LinkedList<String> groups = new LinkedList<>();
        Map<String, Group> groupById = new HashMap<>();

        FzzyBridge.walk(config, prefix, (walkable, parentScope, scope, thing, prop, annotations, classAnnotations, callback) -> {
            if (FzzyBridge.isDeprecated(annotations)) {
                callback.cont();
                return;
            }
            Page page = pages.getOrDefault(parentScope, rootPage);
            // Where this lands is decided before prepare(): a group's own prepare() opens it.
            List<Node> container = groups.isEmpty() || !groupById.containsKey(groups.peekLast())
                    ? page.children : groupById.get(groups.peekLast()).children();

            List<EntryFlag.Flag> flags = FzzyBridge.flagsOf(thing);
            EntryCreator creator;
            if (thing instanceof EntryCreator c) {
                creator = c;
                c.prepare(scope, groups, annotations, classAnnotations);
            } else if (thing != null) {
                ValidatedField<?> wrapped = wrapPlainField(manager, base, prefix, scope, thing);
                if (wrapped == null) return;
                wrapped.prepare(scope, groups, annotations, classAnnotations);
                creator = wrapped;
            } else {
                return;
            }
            if (config.saveType() == SaveType.SEPARATE && creator instanceof EntryParent && creator instanceof EntryFlag f) {
                f.setFlag(EntryFlag.Flag.SEPARATE);
            }

            ConfigApiImplClient.PrepareResult result = FzzyBridge.prepare(creator, perm, config, prefix, scope,
                    annotations, classAnnotations, set.getClientOnly(), flags);
            if (result.getFail()) return;
            if (result.getCont()) callback.cont();
            if (creator instanceof Updatable u) {
                u.setUpdateManager(manager);
                manager.setUpdatableEntry(u);
            }

            Translatable.Result texts = result.getTexts();
            PermResult perms = result.getPerms();
            if (perms == PermResult.FAILURE || perms == PermResult.OUT_OF_GAME) {
                container.add(new Stub(scope, texts.getName(), texts.getDesc(), perms == PermResult.OUT_OF_GAME));
                // Its children are not reachable either.
                if (creator instanceof ConfigSection) callback.cont();
            } else if (creator instanceof ConfigSection) {
                Page section = new Page(scope, texts, page, manager);
                pages.put(scope, section);
                container.add(section);
            } else if (creator instanceof ConfigGroup group) {
                String id = groups.isEmpty() ? scope : groups.peekLast();
                Group node = new Group(id, texts.getName(), texts.getDesc(),
                        FzzyBridge.groupCollapsedByDefault(group), new ArrayList<>());
                groupById.put(id, node);
                container.add(node);
            } else if (creator instanceof ConfigAction action) {
                container.add(new Button(scope, texts.getName(), texts.getDesc(), action));
            } else if (creator instanceof ValidatedField<?> field) {
                container.add(new Entry(scope, texts.getName(), texts.getDesc(), field, result.getActions(), page));
            }
            FzzyBridge.popGroup(annotations, groups);
        });
    }

    /**
     * A plain field ({@code public int x = 5;}) as the validated field Fzzy would show for it: the
     * wrapper is built from the base config, so it carries the right default and any
     * {@code @Restrict} bounds, then set to the live value.
     */
    @Nullable
    private static ValidatedField<?> wrapPlainField(ConfigSingleUpdateManager manager, Config base, String prefix,
                                                    String scope, Object value) {
        ValidatedField<?>[] holder = new ValidatedField<?>[1];
        String relative = scope.startsWith(prefix + ".") ? scope.substring(prefix.length() + 1) : scope;
        FzzyBridge.drill(base, relative, (w, o, n, baseValue, prop, annotations, classAnnotations, cb) -> {
            ValidatedField<?> template = manager.basicValidationStrategy(baseValue, prop.getReturnType(), scope, annotations);
            if (template != null && template.instanceEntry() instanceof ValidatedField<?> copy) holder[0] = copy;
        });
        ValidatedField<?> field = holder[0];
        if (field == null) return null;
        field.trySet(value);
        field.setEntryKey(scope);
        return field;
    }

    /** Every entry of every page, for search. */
    List<Entry> allEntries() {
        List<Entry> out = new ArrayList<>();
        for (Page page : configs) collect(page.children, out);
        return out;
    }

    private static void collect(List<Node> nodes, List<Entry> out) {
        for (Node node : nodes) {
            if (node instanceof Entry e) out.add(e);
            else if (node instanceof Page p) collect(p.children, out);
            else if (node instanceof Group g) collect(g.children(), out);
        }
    }

    boolean hasChanges() {
        return root.hasChanges();
    }

    int changeCount() {
        return root.changeCount();
    }

    /**
     * Applies what changed and re-snapshots. {@code finalApply}: the screen is closing — Fzzy then
     * skips the re-snapshot.
     */
    void apply(boolean finalApply) {
        for (ConfigSingleUpdateManager m : managers) {
            if (m.hasChanges()) m.apply(finalApply);
            if (finalApply) m.invalidatePush();
        }
    }

    /** Whether applying the pending changes asks the player to restart. */
    boolean pendingRestart() {
        for (Entry e : allEntries()) {
            if (!e.field().peekState()) continue;
            for (Action a : e.actions()) if (a.getRestartPrompt()) return true;
        }
        return false;
    }
}
