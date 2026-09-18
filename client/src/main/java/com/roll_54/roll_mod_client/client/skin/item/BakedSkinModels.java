package com.roll_54.roll_mod_client.client.skin.item;

import com.roll_54.roll_mod.cosmetics.ItemSkinDefinition;
import com.roll_54.roll_mod.cosmetics.ItemSkinRegistry;
import com.roll_54.roll_mod.cosmetics.SkinKind;
import com.roll_54.roll_mod_client.RollModClient;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Bakes every {@link SkinKind#ITEM_MODEL} skin and hands the baked result to the renderer.
 *
 * <p>Side-loading is not optional here. A skin model such as {@code models/tool/murasama_on.json}
 * is currently only reachable as an {@code overrides} target of the nano saber, which means it is
 * baked inside that model's {@code ItemOverrides} and never enters
 * {@link ModelManager#getModel(ModelResourceLocation)}. Registering it as a standalone model is what
 * makes it addressable on its own.
 *
 * <p>Note the ids carry no {@code item/} prefix: {@code ModelBakery} resolves side-loaded models
 * directly under {@code models/}, unlike the item models it derives from the item registry.
 */
@EventBusSubscriber(modid = RollModClient.MODID, value = Dist.CLIENT)
public final class BakedSkinModels {
    private BakedSkinModels() {}

    private static final Logger LOGGER = LoggerFactory.getLogger("roll_mod/skins");

    /** Replaced wholesale on every bake, so the render thread never sees a half-filled map. */
    private static volatile Map<ResourceLocation, BakedModel> baked = Map.of();

    @SubscribeEvent
    public static void registerAdditional(ModelEvent.RegisterAdditional event) {
        for (ItemSkinDefinition skin : ItemSkinRegistry.all()) {
            if (skin.kind() == SkinKind.ITEM_MODEL) {
                event.register(ModelResourceLocation.standalone(skin.model()));
            }
        }
    }

    @SubscribeEvent
    public static void onBakingCompleted(ModelEvent.BakingCompleted event) {
        ModelManager manager = event.getModelManager();
        BakedModel missing = manager.getMissingModel();

        Map<ResourceLocation, BakedModel> next = new HashMap<>();
        int expected = 0;
        for (ItemSkinDefinition skin : ItemSkinRegistry.all()) {
            if (skin.kind() != SkinKind.ITEM_MODEL) {
                continue;
            }
            expected++;
            BakedModel model = manager.getModel(ModelResourceLocation.standalone(skin.model()));
            if (model == null || model == missing) {
                // Dropped rather than stored: a skin that resolves to the missing model would render
                // as a purple cube on the player's weapon, which is worse than not applying at all.
                LOGGER.warn("Item skin {} has no baked model at {} — it will not render",
                        skin.skinId(), skin.model());
                continue;
            }
            next.put(skin.skinId(), model);
        }
        baked = Map.copyOf(next);
        // Side-loading is the fragile half of this feature — a wrong id produces no error, just a
        // skin that silently never appears — so say plainly how many made it.
        LOGGER.debug("Baked {} of {} item-model skins", next.size(), expected);
    }

    @Nullable
    public static BakedModel get(ResourceLocation skinId) {
        return baked.get(skinId);
    }
}
