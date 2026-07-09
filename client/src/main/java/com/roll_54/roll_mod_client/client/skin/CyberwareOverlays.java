package com.roll_54.roll_mod_client.client.skin;

import com.perigrine3.createcybernetics.api.CyberwareSlot;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.registry.ItemRegistry;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side registry of {@link CyberwareOverlay} entries. Add a cyberware's on-body texture by
 * registering one entry here; {@code CyberwareSkinLayer} iterates every entry each frame.
 */
public final class CyberwareOverlays {
    private CyberwareOverlays() {}

    private static final List<CyberwareOverlay> ENTRIES = new ArrayList<>();

    // roll_mod:textures/entity/cybernetic/limb/<name>.png
    private static ResourceLocation limb(String name) {
        return RollMod.id("textures/entity/cybernetic/limb/" + name + ".png");
    }

    private static final ResourceLocation METEORITE_RIGHTARM_WIDE = limb("meteorite_metal_rightarm_wide");
    private static final ResourceLocation METEORITE_RIGHTARM_SLIM = limb("meteorite_metal_rightarm_slim");
    private static final ResourceLocation METEORITE_LEFTARM_WIDE = limb("meteorite_metal_leftarm_wide");
    private static final ResourceLocation METEORITE_LEFTARM_SLIM = limb("meteorite_metal_leftarm_slim");

    static {
        register(new CyberwareOverlay(
                ItemRegistry.BASECYBERWARE_RIGHTARM_METEORITE_METAL,
                CyberwareSlot.RARM, BodyRegion.RIGHT_ARM,
                METEORITE_RIGHTARM_WIDE, METEORITE_RIGHTARM_SLIM));
        register(new CyberwareOverlay(
                ItemRegistry.BASECYBERWARE_LEFTARM_METEORITE_METAL,
                CyberwareSlot.LARM, BodyRegion.LEFT_ARM,
                METEORITE_LEFTARM_WIDE, METEORITE_LEFTARM_SLIM));
    }

    public static void register(CyberwareOverlay overlay) {
        ENTRIES.add(overlay);
    }

    public static List<CyberwareOverlay> all() {
        return ENTRIES;
    }
}
