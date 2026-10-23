package com.roll_54.roll_mod_client.mixin.mi;

import aztech.modern_industrialization.client.textures.TextureManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.roll_54.roll_mod.RollMod;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModFileInfo;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

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
