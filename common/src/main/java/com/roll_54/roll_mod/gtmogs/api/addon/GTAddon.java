package com.roll_54.roll_mod.gtmogs.api.addon;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This defines an Addon to GTMOGS.
 * <p>
 * Any class found with this annotation applied will be loaded
 * as an addon entrypoint for the mod with the given {@linkplain #value() ID}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface GTAddon {

    /**
     * The unique mod identifier for this addon.
     * <p>
     * This is required to be the same as the one in your {@link net.neoforged.fml.common.Mod @Mod} annotation.
     */
    String value();
}
