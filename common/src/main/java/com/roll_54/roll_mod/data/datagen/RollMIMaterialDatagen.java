package com.roll_54.roll_mod.data.datagen;

import aztech.modern_industrialization.MI;
import aztech.modern_industrialization.MIItem;
import aztech.modern_industrialization.client.textures.MITextures;
import aztech.modern_industrialization.client.textures.TextureManager;
import aztech.modern_industrialization.client.textures.coloramp.TabulatedColoramp;
import aztech.modern_industrialization.materials.Material;
import aztech.modern_industrialization.materials.part.MaterialItemPart;
import aztech.modern_industrialization.materials.part.TextureGenParams;
import aztech.modern_industrialization.materials.property.MaterialProperty;
import com.google.common.hash.Hashing;
import com.mojang.blaze3d.platform.NativeImage;
import com.roll_54.roll_mod.compat.mi.RollMIMaterials;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.VanillaPackResourcesBuilder;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.world.item.Item;
import net.minecraft.tags.TagKey;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.LanguageProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * Datagen for {@link RollMIMaterials}: MI only generates assets/recipes for its own materials, so this mod
 * runs MI's generators for its materials alone. Output lands in the {@code modern_industrialization} namespace
 * (where the parts are registered) inside this mod's generated resources.
 */
public final class RollMIMaterialDatagen {

    /** MI standard material recipes (tiny dust 3x3 crafting, packer/unpacker, ...). Called from the main recipe provider. */
    public static void buildRecipes(RecipeOutput output) {
        for (Material material : RollMIMaterials.all()) {
            material.registerRecipes.accept(output);
        }
    }

    /** Item tags MI would generate for a part ({@code c:dusts/<material>}) and their category tag ({@code c:dusts}). */
    public static void addTags(BiConsumer<TagKey<Item>, Item> itemToTag, BiConsumer<TagKey<Item>, TagKey<Item>> tagToTag) {
        for (Material material : RollMIMaterials.all()) {
            for (MaterialItemPart part : material.getParts().values()) {
                String tagged = part.getTaggedItemId();
                if (!tagged.startsWith("#")) {
                    continue;
                }
                var tag = TagKey.create(net.minecraft.core.registries.Registries.ITEM, ResourceLocation.parse(tagged.substring(1)));
                itemToTag.accept(tag, part.asItem());
                var category = aztech.modern_industrialization.materials.part.MIParts.CATEGORY_TAGS.get(part.key());
                if (category != null) {
                    tagToTag.accept(TagKey.create(net.minecraft.core.registries.Registries.ITEM, ResourceLocation.parse(category.tag())), tag);
                }
            }
        }
    }

    private static String itemPath(MaterialItemPart part) {
        return ResourceLocation.parse(part.getItemId()).getPath();
    }

    /** Recolored part textures, produced by MI's own texture generator from the templates in MI's jar. */
    public static final class Textures implements DataProvider {
        private final PackOutput packOutput;

        public Textures(PackOutput packOutput) {
            this.packOutput = packOutput;
        }

        private static PackLocationInfo info(String name) {
            return new PackLocationInfo(name, Component.literal(name), PackSource.BUILT_IN, Optional.empty());
        }

        @Override
        public CompletableFuture<?> run(CachedOutput cache) {
            List<PackResources> packs = new ArrayList<>();
            packs.add(new VanillaPackResourcesBuilder().exposeNamespace("minecraft").pushJarResources().build(info("vanilla")));
            packs.add(new PathPackResources(info("mi"), ModList.get().getModFileById(MI.ID).getFile().getSecureJar().getRootPath()));
            var resources = new MultiPackResourceManager(PackType.CLIENT_RESOURCES, packs);

            List<CompletableFuture<?>> writes = new ArrayList<>();
            var mtm = new TextureManager(resources,
                    (image, textureId) -> writeTexture(cache, image, textureId),
                    (json, path) -> writes.add(DataProvider.saveStable(cache, json,
                            packOutput.getOutputFolder().resolve("assets").resolve(path.replace(':', '/')))));

            for (Material material : RollMIMaterials.all()) {
                var coloramp = new TabulatedColoramp(mtm, material.get(MaterialProperty.MEAN_RGB), material.name);
                String set = material.get(MaterialProperty.SET).name;
                for (MaterialItemPart part : material.getParts().values()) {
                    if (part.getTextureGenParams() instanceof TextureGenParams.SimpleRecoloredItem item) {
                        String template = item.basePart() != null ? item.basePart().key : part.key().key;
                        MITextures.generateItemPartTexture(mtm, template, item.overlay(), set, itemPath(part), false, coloramp);
                    } else if (!(part.getTextureGenParams() instanceof TextureGenParams.NoTexture)) {
                        throw new IllegalStateException("Texture generation for part " + part.key().key + " of " + material.name
                                + " is not supported by RollMIMaterialDatagen yet");
                    }
                }
            }

            return mtm.doEndWork()
                    .thenCompose(v -> CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new)))
                    .whenComplete((v, t) -> resources.close());
        }

        private void writeTexture(CachedOutput cache, NativeImage image, String textureId) {
            try {
                byte[] bytes = image.asByteArray();
                var path = packOutput.getOutputFolder().resolve("assets").resolve(textureId.replace(':', '/'));
                cache.writeIfNeeded(path, bytes, Hashing.sha1().hashBytes(bytes));
            } catch (IOException e) {
                throw new RuntimeException("Failed to write texture " + textureId, e);
            }
        }

        @Override
        public String getName() {
            return "Roll MI material textures";
        }
    }

    /** Item models, using each part's MI model generator ({@code basicItem} for dusts). */
    public static final class Models extends ItemModelProvider {
        public Models(PackOutput output, ExistingFileHelper existingFileHelper) {
            super(output, MI.ID, existingFileHelper);
        }

        @Override
        protected void registerModels() {
            for (Material material : RollMIMaterials.all()) {
                for (MaterialItemPart part : material.getParts().values()) {
                    var id = ResourceLocation.parse(part.getItemId());
                    // The texture is written by Textures above, not present in any existing pack.
                    existingFileHelper.trackGenerated(id.withPrefix("item/"), TEXTURE);
                    var definition = MIItem.ITEM_DEFINITIONS.get(id);
                    if (definition == null) {
                        throw new IllegalStateException("No MI item definition for " + id);
                    }
                    definition.modelGenerator.accept(definition.asItem(), this);
                }
            }
        }

        @Override
        public String getName() {
            return "Roll MI material item models";
        }
    }

    /** Part names in the MI namespace. en_us uses MI's generated English name; other locales are listed explicitly. */
    public static final class Lang extends LanguageProvider {
        private static final Map<String, Map<String, String>> TRANSLATIONS = Map.of(
                "uk_ua", Map.of(
                        "sodium_chlorate_dust", "Пил хлорату натрію",
                        "sodium_chlorate_tiny_dust", "Пилинка хлорату натрію",
                        "tag.c.dusts.sodium_chlorate", "Пили хлорату натрію",
                        "tag.c.tiny_dusts.sodium_chlorate", "Пилинки хлорату натрію"));

        private final String locale;

        public Lang(PackOutput output, String locale) {
            super(output, MI.ID, locale);
            this.locale = locale;
        }

        @Override
        protected void addTranslations() {
            Map<String, String> localized = TRANSLATIONS.get(locale);
            for (Material material : RollMIMaterials.all()) {
                for (MaterialItemPart part : material.getParts().values()) {
                    var definition = MIItem.ITEM_DEFINITIONS.get(ResourceLocation.parse(part.getItemId()));
                    String path = itemPath(part);
                    String name = localized != null ? localized.get(path) : definition.getEnglishName();
                    if (name == null) {
                        throw new IllegalStateException("Missing " + locale + " translation for " + path);
                    }
                    add(definition.getTranslationKey(), name);

                    String tagged = part.getTaggedItemId();
                    if (tagged.startsWith("#")) {
                        var tagId = ResourceLocation.parse(tagged.substring(1));
                        String tagKey = ("tag." + tagId.getNamespace() + "." + tagId.getPath()).replace('/', '.');
                        // English follows MI's own tag naming: "<Part English Name>s".
                        String tagName = localized != null ? localized.get(tagKey) : definition.getEnglishName() + "s";
                        if (tagName == null) {
                            throw new IllegalStateException("Missing " + locale + " translation for " + tagKey);
                        }
                        add(tagKey, tagName);
                    }
                }
            }
        }
    }

    private RollMIMaterialDatagen() {
    }
}
