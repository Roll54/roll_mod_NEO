package com.roll_54.roll_mod.compat.gtmogs.mixin;

import com.roll_54.roll_mod.RollMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * GTMOGS ships inside roll_mod and is part of it to players, so everything it names is named
 * {@code roll_mod:}: its registries ({@code roll_mod:ore_vein}, so veins live in
 * {@code data/<ns>/roll_mod/ore_vein/}), vein generator and indicator types, tags, network payloads,
 * EMI/JEI categories. All of them are built from one template id in {@code GTMOGS}, and ids written
 * without a namespace in GTMOGS's codecs get its namespace through {@code appendIdString}.
 *
 * <p>The mod itself is still {@code gtmogs} (its {@code @Mod} and mods.toml, which roll_mod cannot share).
 * Its blocks are moved by {@link GTRegistrationNamespaceMixin}.
 *
 * <p>Targets GTMOGS by name; the config is soft, so without GTMOGS this is skipped.
 */
@Mixin(targets = "com.quantumgarbage.gtmogs.GTMOGS")
public abstract class GTMOGSNamespaceMixin {

    /** {@code TEMPLATE_LOCATION}, the {@code gtmogs:} that {@code GTMOGS.id(path)} puts every path on. */
    @ModifyArg(method = "<clinit>",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/resources/ResourceLocation;fromNamespaceAndPath(Ljava/lang/String;Ljava/lang/String;)Lnet/minecraft/resources/ResourceLocation;"),
            index = 0)
    private static String roll_mod$templateNamespace(String namespace) {
        return RollMod.MODID;
    }

    /** {@code "classic"} and {@code ":classic"} read as {@code roll_mod:classic}. */
    @Inject(method = "appendIdString", at = @At("HEAD"), cancellable = true)
    private static void roll_mod$defaultNamespace(String id, CallbackInfoReturnable<String> cir) {
        int colon = id.indexOf(':');
        if (colon > 0) return;
        cir.setReturnValue(RollMod.MODID + ":" + (colon == 0 ? id.substring(1) : id));
    }
}
