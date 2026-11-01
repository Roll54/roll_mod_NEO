package com.roll_54.roll_mod_client.mixin.mi;

import aztech.modern_industrialization.client.textures.TextureManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.roll_54.roll_mod.RollMod;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModFileInfo;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Makes MI tint our grayscale part templates instead of its own: any
 * {@code modern_industrialization:textures/materialsets/<set>/<part>.png} that has a file of the same
 * name in {@code assets/roll_mod/textures/override/modern_industrialization/textures/materialsets/common/}
 * is read from there, for every material set. MI looks in the set's own folder before {@code common},
 * so without the set paths its {@code shiny/{ring,gear,blade}_overlay.png} would still win over ours
 * for shiny materials.
 *
 * <p>MI only reads these templates while generating textures, which at runtime means its runtime
 * datagen — forced on by {@link MIClientForceRuntimeDatagenMixin} and {@link MIForceRuntimeResourcesMixin}.
 * Its resource provider then holds just vanilla, MI's jar and {@code extra_datagen_resources}
 * ({@code TexturesProvider.run}, MI 2.4.1), not our jar, so the file is read straight from roll_mod's
 * mod file. Only templates we ship a {@code common} file for are rerouted; every other set-specific,
 * gem, ore and block template is untouched.
 */
@Mixin(TextureManager.class)
public class MITextureManagerOverrideMixin {
    @Unique
    private static final String roll_mod$MATERIALSETS = "modern_industrialization:textures/materialsets/";

    @Inject(method = "getAssetAsTexture", at = @At("HEAD"), cancellable = true)
    private void roll_mod$readOverride(String path, CallbackInfoReturnable<NativeImage> cir) throws IOException {
        Path override = roll_mod$override(path);
        if (override == null) return;
        try (InputStream in = Files.newInputStream(override)) {
            cir.setReturnValue(NativeImage.read(in));
        }
        RollMod.LOGGER.debug("[{}] MI template {} rerouted to {}", RollMod.MODID, path, override);
    }

    @Inject(method = "hasAsset", at = @At("HEAD"), cancellable = true)
    private void roll_mod$hasOverride(String path, CallbackInfoReturnable<Boolean> cir) {
        if (roll_mod$override(path) != null) cir.setReturnValue(true);
    }

    @Shadow @Final private BiConsumer<NativeImage, String> textureWriter;
    @Shadow @Final private Set<String> generatedTextures;

    /**
     * A texture MI generates (per-material items, blocks, ores...) loses to MI's generated pack, which
     * sits on top of every mod's resources. So when roll_mod ships a file at the very path MI is about
     * to generate ({@code assets/<ns>/textures/...} in our jar, e.g. from the "MI in New" pack), ours is
     * written into the generated pack instead of MI's — MI's own {@code datagen_texture_overrides}
     * lookup, but reading roll_mod's jar, which MI's resource provider does not include.
     */
    @Inject(method = "addTexture(Ljava/lang/String;Lcom/mojang/blaze3d/platform/NativeImage;Z)V",
            at = @At("HEAD"), cancellable = true)
    private void roll_mod$shippedTextureWins(String path, NativeImage image, boolean closeImage, CallbackInfo ci)
            throws IOException {
        Path shipped = roll_mod$shipped(path);
        if (shipped == null) return;
        try (InputStream in = Files.newInputStream(shipped)) {
            textureWriter.accept(NativeImage.read(in), path);
        }
        if (closeImage) image.close();
        generatedTextures.add(path.replace(":textures/", ":"));
        RollMod.LOGGER.debug("[{}] MI generated texture {} replaced by {}", RollMod.MODID, path, shipped);
        ci.cancel();
    }

    /** {@code <ns>:textures/<rest>} as shipped in roll_mod's jar at {@code assets/<ns>/textures/<rest>}, or null. */
    @Unique
    @Nullable
    private static Path roll_mod$shipped(String path) {
        int colon = path.indexOf(':');
        if (colon < 0 || !path.startsWith("textures/", colon + 1)) return null;
        IModFileInfo rollMod = ModList.get().getModFileById(RollMod.MODID);
        if (rollMod == null) return null;
        String[] parts = ("assets/" + path.substring(0, colon) + "/" + path.substring(colon + 1)).split("/");
        Path file = rollMod.getFile().findResource(parts);
        return Files.isRegularFile(file) ? file : null;
    }

    @Unique
    @Nullable
    private static Path roll_mod$override(String path) {
        if (!path.startsWith(roll_mod$MATERIALSETS)) return null;
        // <set>/<file>: the set is dropped, every set reads the same common override.
        String rest = path.substring(roll_mod$MATERIALSETS.length());
        int slash = rest.indexOf('/');
        if (slash < 0) return null;
        String fileName = rest.substring(slash + 1);
        IModFileInfo rollMod = ModList.get().getModFileById(RollMod.MODID);
        if (rollMod == null) return null;
        Path file = rollMod.getFile().findResource("assets", RollMod.MODID, "textures", "override",
                "modern_industrialization", "textures", "materialsets", "common", fileName);
        return Files.isRegularFile(file) ? file : null;
    }
}
