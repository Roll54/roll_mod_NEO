package com.roll_54.roll_mod_client.compat.fusion;

import aztech.modern_industrialization.materials.Material;
import aztech.modern_industrialization.materials.MaterialRegistry;
import aztech.modern_industrialization.materials.part.MaterialItemPart;
import aztech.modern_industrialization.materials.part.TextureGenParams;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

import java.util.HashSet;
import java.util.Set;

/**
 * Fusion connected textures for MI casings, in the pattern of the "MI in New" resource pack. Every casing
 * block {@code <ns>:<id>} gets:
 * <ul>
 *     <li>{@code <ns>:block/<id>_ct}: connects to the same block and to any MI machine
 *     ({@code mi_ct:is_mi_machine}); worn by the placed block through the Fusion model modifier
 *     {@code fusion/model_modifiers/blocks/<id>.json} pointing at {@code <ns>:block/<id>_connected}.</li>
 *     <li>{@code <ns>:block/<id>_mct}: connects to the same block only; worn by machines that use the
 *     casing, through {@code <casing ns>:machine_casing/<casing>}.</li>
 * </ul>
 * MI material casings get all of it from MI's runtime datagen, the {@code _ct} textures tinted from the
 * {@code <part>_ct} templates (see {@code MICasingConnectedTexturesMixin} and
 * {@code MIMachineCasingsProviderMixin}). {@link #EXTRA_CASINGS} ship their textures, mcmetas, connected
 * models and modifiers as static resources and only need the machine casing model generated, since MI's
 * runtime pack sits on top and would otherwise cover it with a plain {@code use_block_model} one.
 */
public final class MiCasingConnectedTextures {

    /** Casings from other mods whose connected textures are shipped as static resources. */
    public static final Set<ResourceLocation> EXTRA_CASINGS = Set.of(
            ResourceLocation.fromNamespaceAndPath("extended_industrialization", "steel_plated_bricks"),
            ResourceLocation.fromNamespaceAndPath("yet_another_industrialization", "battery_casing"),
            ResourceLocation.fromNamespaceAndPath("yet_another_industrialization", "steel_plated_end_stone_bricks")
    );

    private static final ResourceLocation MI_MACHINE_PREDICATE = ResourceLocation.fromNamespaceAndPath("mi_ct", "is_mi_machine");

    private static Set<ResourceLocation> casings;

    /** Fusion models fail to load without Fusion, so nothing is generated then. */
    public static boolean enabled(){
        return ModList.get().isLoaded("fusion");
    }

    /** Every block that gets connected textures: MI material casings (KubeJS ones included) plus the extras. */
    public static synchronized Set<ResourceLocation> casings(){
        if(casings == null){
            Set<ResourceLocation> set = new HashSet<>(EXTRA_CASINGS);
            for(Material material : MaterialRegistry.getMaterials().values()){
                for(MaterialItemPart part : material.getParts().values()){
                    if(part.getTextureGenParams() instanceof TextureGenParams.CasingBlock)
                        set.add(ResourceLocation.parse(part.getItemId()));
                }
            }
            casings = Set.copyOf(set);
        }
        return casings;
    }

    /** {@code fusion} block of {@code <id>_ct.png.mcmeta} or {@code <id>_mct.png.mcmeta}. */
    public static JsonObject textureMeta(ResourceLocation block, boolean machineFace){
        JsonObject fusion = new JsonObject();
        fusion.addProperty("type", "connecting");
        fusion.addProperty("layout", "simple");
        fusion.add("connections", connections(block, machineFace));
        JsonObject meta = new JsonObject();
        meta.add("fusion", fusion);
        return meta;
    }

    /** {@code <ns>:block/<id>_connected}, the placed block's model. */
    public static JsonObject connectedBlockModel(ResourceLocation block){
        JsonObject json = new JsonObject();
        writeConnectingModel(json, block, false);
        return json;
    }

    /** {@code fusion/model_modifiers/blocks/<id>.json}, swapping the block's default model for the connected one. */
    public static JsonObject modelModifier(ResourceLocation block){
        JsonObject json = new JsonObject();
        JsonArray targets = new JsonArray();
        targets.add(block.toString());
        json.add("targets", targets);
        json.addProperty("ignore_missing_targets", true);
        JsonArray overrides = new JsonArray();
        overrides.add(block.withPath(path -> "block/" + path + "_connected").toString());
        json.add("default_model_overrides", overrides);
        return json;
    }

    /**
     * Fills a model root with the Fusion connecting model fields (loader included). With {@code machineFace}
     * it is {@code <casing ns>:machine_casing/<casing>}, the casing faces of machines using the block.
     */
    public static void writeConnectingModel(JsonObject json, ResourceLocation block, boolean machineFace){
        json.addProperty("loader", "fusion:model");
        json.addProperty("type", "connecting");
        json.addProperty("parent", "minecraft:block/cube_all");
        JsonObject textures = new JsonObject();
        textures.addProperty("all", block.withPath(path -> "block/" + path + (machineFace ? "_mct" : "_ct")).toString());
        textures.addProperty("particle", block.withPath(path -> "block/" + path).toString());
        json.add("textures", textures);
        json.add("connections", connections(block, machineFace));
    }

    private static JsonElement connections(ResourceLocation block, boolean machineFace){
        JsonObject sameBlock = new JsonObject();
        sameBlock.addProperty("type", "match_block");
        sameBlock.addProperty("ignore_missing", true);
        sameBlock.addProperty("block", block.toString());
        if(machineFace)
            return sameBlock;
        JsonObject machine = new JsonObject();
        machine.addProperty("type", MI_MACHINE_PREDICATE.toString());
        JsonArray array = new JsonArray();
        array.add(sameBlock);
        array.add(machine);
        return array;
    }

    private MiCasingConnectedTextures(){
    }
}
