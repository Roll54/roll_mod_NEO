package com.roll_54.roll_mod.gtmogs.data.datagen.lang;


import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.neoforge.common.data.LanguageProvider;

import com.tterrag.registrate.providers.RegistrateLangProvider;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LangHandler {

    public static void init(RegistrateLangProvider provider) {
        IntegrationLang.init(provider);
        ConfigurationLang.init(provider);


        provider.add("command.gtmogs.dump_data.success", "Dumped %s resources from registry %s to %s");
        provider.add("command.gtmogs.place_vein.failure", "Failed to place vein %s at position %s");
        provider.add("command.gtmogs.place_vein.success", "Placed vein %s at position %s");
        provider.add("command.gtmogs.share_prospection_data.notification", "%s is sharing prospecting data with you!");

        provider.add("behavior.prospector.added_waypoint", "Created waypoint named %s!");
        // gui
        provider.add("gtmogs.button.ore_veins", "Show GT Ore Veins");
        provider.add("gtmogs.button.bedrock_fluids", "Show Bedrock Fluid Veins");
        provider.add("gtmogs.button.hide_depleted", "Hide Depleted Veins");
        provider.add("gtmogs.button.show_depleted", "Show Depleted Veins");
        provider.add("gtmogs.recipe_type.show_recipes", "Show Recipes");;

        provider.add("gtmogs.direction.tooltip.up", "Up");
        provider.add("gtmogs.direction.tooltip.down", "Down");
        provider.add("gtmogs.direction.tooltip.left", "Left");
        provider.add("gtmogs.direction.tooltip.right", "Right");
        provider.add("gtmogs.direction.tooltip.back", "Back");
        provider.add("gtmogs.direction.tooltip.front", "Front");

        provider.add("gtmogs.tooltip.status.trinary.false", "False");
        provider.add("gtmogs.tooltip.status.trinary.true", "True");
        provider.add("gtmogs.tooltip.status.trinary.unknown", "Unknown");
    }

    /**
     * Returns the sub-key consisting of the given key plus the given index.<br>
     * E.g.,<br>
     *
     * <pre>
     * <code>getSubKey("terminal.fluid_prospector.tier", 0)</code>
     * </pre>
     *
     * returns the <code>String</code>:
     *
     * <pre>
     * <code>
     * "terminal.fluid_prospector.tier.0"</code>
     * </pre>
     *
     * @param key   Base key of the sub-key.
     * @param index Index of the sub-key.
     * @return Sub-key consisting of key and index.
     */
    protected static String getSubKey(String key, int index) {
        return key + "." + index;
    }

    /**
     * Registers multiple values under the same key with a given provider.<br>
     * <br>
     * For example, a cumbersome way to add translations would be the following:<br>
     *
     * <pre>
     * <code>provider.add("terminal.fluid_prospector.tier.0", "radius size 1");
     * provider.add("terminal.fluid_prospector.tier.1", "radius size 2");
     * provider.add("terminal.fluid_prospector.tier.2", "radius size 3");</code>
     * </pre>
     *
     * Instead, <code>multiLang</code> can be used for the same result:
     *
     * <pre>
     * <code>multiLang(provider, "terminal.fluid_prospector.tier", "radius size 1", "radius size 2", "radius size 3");</code>
     * </pre>
     *
     * In situations requiring a large number of generated translations, the
     * following could be used instead, which
     * generates translations for 100 tiers:
     *
     * <pre>
     * <code>multiLang(provider, "terminal.fluid_prospector.tier", IntStream.of(100)
     *                 .map(i -> i + 1)
     *                 .mapToObj(Integer::toString)
     *                 .map(i -> "radius size " + i)
     *                 .toArray(String[]::new));</code>
     * </pre>
     *
     * @param provider The provider to add to.
     * @param key      Base key of the key-value-pairs. The real key for each
     *                 translation will be appended by ".0" for
     *                 the first, ".1" for the second, etc. This ensures that the
     *                 keys are unique.
     * @param values   All translation values.
     */
    protected static void multiLang(RegistrateLangProvider provider, String key, String... values) {
        for (var i = 0; i < values.length; i++) {
            provider.add(getSubKey(key, i), values[i]);
        }
    }

    /**
     * Gets all translation components from a multi lang's sub-keys.<br>
     * E.g., given a multi lang:
     *
     * <pre>
     * <code>multiLang(provider, "terminal.fluid_prospector.tier", "radius size 1", "radius size 2", "radius size 3");</code>
     * </pre>
     *
     * The following code can be used to print out the translations:
     *
     * <pre>
     * <code>for (var component : getMultiLang("terminal.fluid_prospector.tier")) {
     *     System.out.println(component.getString());
     * }</code>
     * </pre>
     *
     * Result:
     *
     * <pre>
     * <code>radius size 1
     * radius size 2
     * radius size 3</code>
     * </pre>
     *
     * @param key Base key of the multi lang. E.g. "terminal.fluid_prospector.tier".
     * @return Returns all translation components from a multi lang's sub-keys
     */
    public static List<MutableComponent> getMultiLang(String key) {
        var outputKeys = new ArrayList<String>();
        var i = 0;
        var next = getSubKey(key, i);
        while (Language.getInstance().has(next)) {
            outputKeys.add(next);
            next = getSubKey(key, ++i);
        }
        return outputKeys.stream().map(Component::translatable).collect(Collectors.toList());
    }

    /**
     * Gets all translation components from a multi lang's sub-keys. Supports
     * additional arguments for the translation
     * components.<br>
     * E.g., given a multi lang:
     *
     * <pre>
     * <code>multiLang(provider, "terminal.fluid_prospector.tier", "radius size 1", "radius size 2", "radius size 3");</code>
     * </pre>
     *
     * The following code can be used to print out the translations:
     *
     * <pre>
     * <code>for (var component : getMultiLang("terminal.fluid_prospector.tier")) {
     *     System.out.println(component.getString());
     * }</code>
     * </pre>
     *
     * Result:
     *
     * <pre>
     * <code>radius size 1
     * radius size 2
     * radius size 3</code>
     * </pre>
     *
     * @param key Base key of the multi lang. E.g. "terminal.fluid_prospector.tier".
     * @return Returns all translation components from a multi lang's sub-keys.
     */
    public static List<MutableComponent> getMultiLang(String key, Object... args) {
        var outputKeys = new ArrayList<String>();
        var i = 0;
        var next = getSubKey(key, i);
        while (Language.getInstance().has(next)) {
            outputKeys.add(next);
            next = getSubKey(key, ++i);
        }
        return outputKeys.stream().map(k -> Component.translatable(k, args)).collect(Collectors.toList());
    }

    /**
     * See {@link #getMultiLang(String)}. If no multiline key is available, get
     * single instead.
     *
     * @param key Base key of the multi lang. E.g. "terminal.fluid_prospector.tier".
     * @return Returns all translation components from a multi lang's sub-keys.
     */
    public static List<MutableComponent> getSingleOrMultiLang(String key) {
        List<MutableComponent> multiLang = getMultiLang(key);

        if (!multiLang.isEmpty()) {
            return multiLang;
        }

        return List.of(Component.translatable(key));
    }

    /**
     * Gets a single translation from a multi lang.
     *
     * @param key   Base key of the multi lang. E.g. "gtmogs.gui.overclock.enabled".
     * @param index Index of the single translation. E.g. 3 would return
     *              "gtmogs.gui.overclock.enabled.3".
     * @return Returns a single translation from a multi lang.
     */
    public static MutableComponent getFromMultiLang(String key, int index) {
        return Component.translatable(getSubKey(key, index));
    }

    /**
     * Gets a single translation from a multi lang. Supports additional arguments
     * for the translation component.
     *
     * @param key   Base key of the multi lang. E.g. "gtmogs.gui.overclock.enabled".
     * @param index Index of the single translation. E.g. 3 would return
     *              "gtmogs.gui.overclock.enabled.3".
     * @return Returns a single translation from a multi lang.
     */
    public static MutableComponent getFromMultiLang(String key, int index, Object... args) {
        return Component.translatable(getSubKey(key, index), args);
    }

    /**
     * Adds one key-value-pair to the given lang provider per line in the given
     * multiline (a multiline is a String
     * containing newline characters).<br>
     * Example:
     *
     * <pre>
     * <code>multilineLang(provider, "gtmogs.gui.overclock.enabled", "Overclocking Enabled.\nClick to Disable");</code>
     * </pre>
     *
     * This results in the following translations:<br>
     *
     * <pre>
     * <code>"gtmogs.gui.overclock.enabled.0": "Overclocking Enabled.",
     * "gtmogs.gui.overclock.enabled.1": "Click to Disable",</code>
     * </pre>
     *
     * @param provider  The provider to add to.
     * @param key       Base key of the key-value-pair. The real key for each line
     *                  will be appended by ".0" for the
     *                  first line, ".1" for the second, etc. This ensures that the
     *                  keys are unique.
     * @param multiline The multiline string. It is a multiline because it contains
     *                  at least one newline character '\n'.
     */
    protected static void multilineLang(RegistrateLangProvider provider, String key, String multiline) {
        var lines = multiline.split("\n");
        multiLang(provider, key, lines);
    }

    /**
     * Replace a value in a language provider's mappings
     *
     * @param provider the provider whose mappings should be modified
     * @param key      the key for the value
     * @param value    the value to use in place of the old one
     */
    public static void replace(@NotNull RegistrateLangProvider provider, @NotNull String key,
                               @NotNull String value) {
        try {
            // the regular lang mappings
            Field field = LanguageProvider.class.getDeclaredField("data");
            field.setAccessible(true);
            // noinspection unchecked
            Map<String, String> map = (Map<String, String>) field.get(provider);
            map.put(key, value);

            // upside-down lang mappings
            Field upsideDownField = RegistrateLangProvider.class.getDeclaredField("upsideDown");
            upsideDownField.setAccessible(true);
            // noinspection unchecked
            map = (Map<String, String>) field.get(upsideDownField.get(provider));

            Method toUpsideDown = RegistrateLangProvider.class.getDeclaredMethod("toUpsideDown",
                    String.class);
            toUpsideDown.setAccessible(true);

            map.put(key, (String) toUpsideDown.invoke(provider, value));
        } catch (NoSuchFieldException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Error replacing entry in datagen.", e);
        }
    }
}