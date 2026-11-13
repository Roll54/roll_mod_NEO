package com.roll_54.roll_mod.minestar.fzzyui;

import com.roll_54.roll_mod.RollMod;
import kotlin.jvm.JvmClassMappingKt;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.config.ConfigAction;
import me.fzzyhmstrs.fzzy_config.config.ConfigGroup;
import me.fzzyhmstrs.fzzy_config.entry.Entry;
import me.fzzyhmstrs.fzzy_config.entry.EntryFlag;
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImpl;
import me.fzzyhmstrs.fzzy_config.impl.ConfigApiImplClient;
import me.fzzyhmstrs.fzzy_config.registry.ClientConfigRegistry;
import me.fzzyhmstrs.fzzy_config.registry.SyncedConfigRegistry;
import me.fzzyhmstrs.fzzy_config.util.Translatable;
import me.fzzyhmstrs.fzzy_config.util.ValidationResult;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedNumber;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Every call the LdLib config screens make into Fzzy Config's internals, in one place.
 *
 * <p>Fzzy is written in Kotlin. Its {@code internal} members are public in the bytecode under a
 * {@code $fzzy_config} suffix, and some of what an editor needs (number bounds, collection element
 * handlers, a group's title) is private and read here by reflection. None of it is API: when Fzzy
 * changes, this is the file to fix. Written against Fzzy 0.7.7 (the pack runs 0.7.7+fix1).
 */
@OnlyIn(Dist.CLIENT)
final class FzzyBridge {

    /** Walk flag Fzzy's own screen uses: include {@code @NonSync} entries. */
    static final byte IGNORE_NON_SYNC = ConfigApiImpl.IGNORE_NON_SYNC;

    private FzzyBridge() {}

    /* ------------------------------------------------------ registry ------------------------------------------------------ */

    /** Namespaces with a config screen ({@code roll_mod}, {@code fzzy_config}...). */
    static Set<String> namespaces() {
        return ClientConfigRegistry.INSTANCE.getScreenScopes$fzzy_config();
    }

    /** The paths of a namespace's GUI-visible configs ({@code roll_mod}, {@code currency}...). */
    static Set<String> configPaths(String namespace) {
        return ClientConfigRegistry.INSTANCE.getSubScreenScopes$fzzy_config(namespace);
    }

    /** The live config registered under {@code key} ({@code namespace.path}), or null. */
    @Nullable
    static Config activeConfig(String key) {
        return ClientConfigRegistry.INSTANCE.getClientConfig$fzzy_config(key);
    }

    /** A pristine instance of the config, which Fzzy keeps for defaults: plain fields are wrapped from it. */
    @Nullable
    static Config baseConfig(String key) {
        try {
            Field f = field(ClientConfigRegistry.class, "clientConfigs");
            Object entry = ((Map<?, ?>) f.get(null)).get(key);
            if (entry == null) return null;
            Method getBase = entry.getClass().getMethod("getBase");
            getBase.setAccessible(true);
            return (Config) getBase.invoke(entry);
        } catch (ReflectiveOperationException | RuntimeException e) {
            RollMod.LOGGER.error("[Config UI] could not read Fzzy's base config for {}", key, e);
            return null;
        }
    }

    /** Whether the config lives only on this client (not synced from the server). */
    static boolean clientOnly(String key) {
        return !SyncedConfigRegistry.INSTANCE.hasConfig$fzzy_config(key);
    }

    static boolean isRootConfig(Config config) {
        return ConfigApiImpl.INSTANCE.isRootConfig$fzzy_config(JvmClassMappingKt.getKotlinClass(config.getClass()));
    }

    static int playerPermissionLevel() {
        return ConfigApiImplClient.INSTANCE.getPlayerPermissionLevel$fzzy_config();
    }

    /* -------------------------------------------------------- walk -------------------------------------------------------- */

    static void walk(Object walkable, String prefix, ConfigApiImpl.WalkAction action) {
        ConfigApiImpl.INSTANCE.walk$fzzy_config(walkable, prefix, IGNORE_NON_SYNC, action);
    }

    static void drill(Object walkable, String relativePath, ConfigApiImpl.WalkAction action) {
        ConfigApiImpl.INSTANCE.drill$fzzy_config(walkable, relativePath, '.', IGNORE_NON_SYNC, action);
    }

    static boolean isDeprecated(List<? extends Annotation> annotations) {
        return ConfigApiImpl.INSTANCE.isDeprecated$fzzy_config(annotations);
    }

    static ConfigApiImplClient.PrepareResult prepare(Object thing, int permLevel, Config config, String prefix,
                                                     String scope, List<? extends Annotation> annotations,
                                                     List<? extends Annotation> classAnnotations, boolean clientOnly,
                                                     List<EntryFlag.Flag> flags) {
        return ConfigApiImplClient.INSTANCE.prepare$fzzy_config(thing, permLevel, config, prefix, scope, annotations,
                classAnnotations, clientOnly, flags);
    }

    /** A config's own name and description, as Fzzy's screen titles it. */
    static Translatable.Result configTexts(Config config, String prefix) {
        return ConfigApiImplClient.INSTANCE.getText$fzzy_config(config, prefix, "",
                Arrays.asList(config.getClass().getAnnotations()), List.of(), config.getClass().getSimpleName());
    }

    /** Closes the group an entry's {@code @ConfigGroup.Pop} ends. */
    static void popGroup(List<? extends Annotation> annotations, LinkedList<String> groups) {
        ConfigGroup.Companion.pop$fzzy_config(annotations, groups);
    }

    /** The flags an entry carries, for {@link #prepare}. */
    static List<EntryFlag.Flag> flagsOf(Object thing) {
        List<EntryFlag.Flag> out = new ArrayList<>();
        if (thing instanceof EntryFlag flagged) {
            for (EntryFlag.Flag flag : EntryFlag.Flag.values()) {
                if (flagged.hasFlag(flag)) out.add(flag);
            }
        }
        return out;
    }

    /* ---------------------------------------------------- groups / actions ---------------------------------------------------- */

    static boolean groupCollapsedByDefault(ConfigGroup group) {
        Object v = read(group, "collapsedByDefault");
        return v instanceof Boolean b && b;
    }

    /** The button text a {@link ConfigAction} shows; null if it cannot be read. */
    @Nullable
    static Component actionTitle(ConfigAction action) {
        return read(action, "titleSupplier") instanceof Supplier<?> s && s.get() instanceof Component c ? c : null;
    }

    static boolean actionActive(ConfigAction action) {
        return !(read(action, "activeSupplier") instanceof Supplier<?> s) || !(s.get() instanceof Boolean b) || b;
    }

    static void runAction(ConfigAction action) {
        if (read(action, "pressAction") instanceof Runnable r) r.run();
    }

    /* ------------------------------------------------------- numbers ------------------------------------------------------- */

    /** How a number field asks to be edited. */
    enum NumberWidget { SLIDER, TEXTBOX, TEXTBOX_WITH_BUTTONS }

    /** {min, max} as set on the field, or null if they cannot be read. */
    @Nullable
    static Number[] numberRange(ValidatedNumber<?> number) {
        Object min = call(number, "getMinValue");
        Object max = call(number, "getMaxValue");
        return min instanceof Number a && max instanceof Number b ? new Number[]{a, b} : null;
    }

    /** {min, max} the number type itself allows (e.g. Integer.MIN_VALUE..MAX_VALUE). */
    @Nullable
    static Number[] numberTypeBounds(ValidatedNumber<?> number) {
        Object min = call(number, "minBound");
        Object max = call(number, "maxBound");
        return min instanceof Number a && max instanceof Number b ? new Number[]{a, b} : null;
    }

    static NumberWidget numberWidget(ValidatedNumber<?> number) {
        Object type = call(number, "getWidgetType");
        if (type instanceof Enum<?> e) {
            try {
                return NumberWidget.valueOf(e.name());
            } catch (IllegalArgumentException ignored) {
                // A widget type newer than this bridge: a text box always works.
            }
        }
        return NumberWidget.TEXTBOX;
    }

    /* ----------------------------------------------------- collections ----------------------------------------------------- */

    /** The element handler of a list or set: a template field that validates one element. */
    @Nullable
    static Entry<?, ?> elementHandler(Object collection) {
        return read(collection, "entryHandler") instanceof Entry<?, ?> e ? e : null;
    }

    @Nullable
    static Entry<?, ?> keyHandler(Object map) {
        return read(map, "keyHandler") instanceof Entry<?, ?> e ? e : null;
    }

    @Nullable
    static Entry<?, ?> valueHandler(Object map) {
        return read(map, "valueHandler") instanceof Entry<?, ?> e ? e : null;
    }

    /* ---------------------------------------------------- free-text form ---------------------------------------------------- */

    private static final String TOML_KEY = "entry = ";

    /** The value as TOML text, for types without a dedicated editor; null if it does not serialize. */
    @Nullable
    static String toText(Entry<?, ?> entry) {
        ValidationResult<String> r = ConfigApiImpl.INSTANCE.serializeEntry$fzzy_config(entry, IGNORE_NON_SYNC);
        if (r.isError()) return null;
        String s = r.get().trim();
        return s.startsWith(TOML_KEY) ? s.substring(TOML_KEY.length()) : s;
    }

    /** Parses TOML text into a fresh copy of {@code entry}'s value; null when it does not parse or validate. */
    @Nullable
    @SuppressWarnings({"unchecked", "rawtypes"})
    static Object fromText(Entry<?, ?> entry, String text, String scope) {
        Entry copy = (Entry) entry.instanceEntry();
        ValidationResult<?> r = ConfigApiImpl.INSTANCE.deserializeEntry$fzzy_config(copy, TOML_KEY + text, scope,
                IGNORE_NON_SYNC);
        return r.isError() ? null : r.get();
    }

    /* ------------------------------------------------------ reflection ------------------------------------------------------ */

    @Nullable
    private static Object read(Object owner, String name) {
        try {
            return field(owner.getClass(), name).get(owner);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    @Nullable
    private static Object call(Object owner, String name) {
        for (Class<?> c = owner.getClass(); c != null; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                // The real method, not the erased bridge javac/kotlinc add next to it.
                if (m.getName().equals(name) && m.getParameterCount() == 0 && !m.isBridge()) {
                    try {
                        m.setAccessible(true);
                        return m.invoke(owner);
                    } catch (ReflectiveOperationException | RuntimeException e) {
                        return null;
                    }
                }
            }
        }
        return null;
    }

    private static Field field(Class<?> type, String name) throws NoSuchFieldException {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
                // keep looking up the hierarchy
            }
        }
        throw new NoSuchFieldException(type.getName() + "." + name);
    }
}
