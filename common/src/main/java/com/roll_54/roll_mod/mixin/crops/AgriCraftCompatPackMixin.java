package com.roll_54.roll_mod.mixin.crops;

import com.agricraft.agricraft.neoforge.AgriCraftNeoForge;
import net.minecraft.server.packs.PackType;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

/**
 * Keeps AgriCraft (4.0.24+) from adding its built-in compat packs for mods whose crops roll_mod defines itself.
 *
 * <p>AgriCraft ships {@code datapacks/<modid>} and {@code resourcepacks/<modid>} for several mods and adds them as
 * required built-in packs at the top, above mod data, whenever that mod is loaded. Its Farmer's Delight pack
 * registers {@code farmersdelight:cabbage}, {@code tomato} and {@code rice} on the same seed items as
 * {@code roll_mod:cabbage}, {@code tomato} and {@code rice}; seed lookup takes the first plant, so the FD plants
 * win, ours become unplantable and every mutation bred from {@code roll_mod:cabbage} (iceberg mint and its
 * descendants) breaks. The pack also adds its own soils on the Farmer's Delight rich soil blocks, which
 * {@code agricraft:rich_soil} already covers.
 *
 * <p>TODO(crops): adopt the Farmer's Delight plants instead — drop roll_mod:cabbage/tomato/rice, move their
 * mutations (iceberg_mint = cabbage + sugar_cane, ...) onto the farmersdelight:* ids, settle the rich soil values,
 * then remove "farmersdelight" from {@code roll_mod$BLOCKED}. Tracked in docs/crops-wiki.md.
 */
@Mixin(AgriCraftNeoForge.class)
public class AgriCraftCompatPackMixin {

    @Unique
    private static final Set<String> roll_mod$BLOCKED = Set.of("farmersdelight");

    @Inject(method = "addPack", at = @At("HEAD"), cancellable = true)
    private static void roll_mod$skipBlockedCompatPacks(String type, String modid, PackType packType,
                                                        AddPackFindersEvent event, CallbackInfo ci) {
        if (roll_mod$BLOCKED.contains(modid)) {
            ci.cancel();
        }
    }
}
