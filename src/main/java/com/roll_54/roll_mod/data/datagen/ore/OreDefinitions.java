package com.roll_54.roll_mod.data.datagen.ore;

import com.roll_54.roll_mod.data.datagen.ore.OreTextureTemplates.BlockOverlay;
import com.roll_54.roll_mod.data.datagen.ore.OreTextureTemplates.BlockSubLayer;
import com.roll_54.roll_mod.data.datagen.ore.OreTextureTemplates.ItemBase;

import java.util.List;

public final class OreDefinitions {
    private OreDefinitions() {}

    public static final List<OreDefinition> ALL = List.of(
            new OreDefinition("mica", "#6fb82a", List.of(BlockSubLayer.NETHERRACK), BlockOverlay.COPPER, ItemBase.COPPER, 10, 20, "Mica", "Слюда"),
            new OreDefinition("hematite", "#d1553f", List.of(BlockSubLayer.STONE, BlockSubLayer.DEEPSLATE, BlockSubLayer.NETHERRACK, BlockSubLayer.MOON), BlockOverlay.REDSTONE, ItemBase.IRON, 12, 25, "Hematite", "Гематит"),
            new OreDefinition("yellow_limonite", "#a38933", List.of(BlockSubLayer.STONE, BlockSubLayer.DEEPSLATE), BlockOverlay.GOLD, ItemBase.IRON, 10, 22, "Yellow Limonite", "Жовтий лімоніт"),
            new OreDefinition("biotite", "#582c8a", List.of(BlockSubLayer.NETHERRACK), BlockOverlay.LAPIS, ItemBase.GOLD, 18, 35, "Biotite", "Біотит"),
            new OreDefinition("magnetite", "#211f21", List.of(BlockSubLayer.STONE, BlockSubLayer.DEEPSLATE, BlockSubLayer.MOON, BlockSubLayer.END), BlockOverlay.IRON, ItemBase.IRON, 14, 28, "Magnetite", "Магнетит"),
            new OreDefinition("garnierite", "#174722", List.of(BlockSubLayer.STONE), BlockOverlay.COPPER, ItemBase.COPPER, 11, 24, "Garnierite", "Гарнієрит"),
            new OreDefinition("pentlandite", "#5e5924", List.of(BlockSubLayer.STONE), BlockOverlay.QUARTZ, ItemBase.IRON, 16, 32, "Pentlandite", "Пентландит"),
            new OreDefinition("chalcopyrite", "#5c5345", List.of(BlockSubLayer.STONE, BlockSubLayer.NETHERRACK), BlockOverlay.COPPER, ItemBase.COPPER, 13, 26, "Chalcopyrite", "Халькопірит"),
            new OreDefinition("certus_quartz", "#8dbce3", List.of(BlockSubLayer.NETHERRACK), BlockOverlay.LAPIS, ItemBase.QUARTZ, 20, 40, "Certus Quartz", "Істинний кварц"),
            new OreDefinition("pyrochlore", "#0e4435", List.of(BlockSubLayer.MOON), BlockOverlay.REDSTONE, ItemBase.IRON, 15, 29, "Pyrochlore", "Пірохлор"),
            new OreDefinition("pyrope", "#42122d", List.of(BlockSubLayer.END), BlockOverlay.GOLD, ItemBase.COPPER, 12, 25, "Pyrope", "Піроп"),
            new OreDefinition("apatite", "#24bbed", List.of(BlockSubLayer.MOON), BlockOverlay.COPPER, ItemBase.CRYSTAL, 10, 21, "Apatite", "Апатит"),
            new OreDefinition("galena", "#332f73", List.of(BlockSubLayer.STONE, BlockSubLayer.NETHERRACK), BlockOverlay.LEAD, ItemBase.GOLD, 17, 34, "Galena", "Галена"),
            new OreDefinition("pyrolusite", "#466671", List.of(BlockSubLayer.MARS, BlockSubLayer.END), BlockOverlay.IRON, ItemBase.IRON, 14, 27, "Pyrolusite", "Піролюзит"),
            new OreDefinition("chromite", "#733853", List.of(BlockSubLayer.END, BlockSubLayer.MOON), BlockOverlay.REDSTONE, ItemBase.QUARTZ, 19, 38, "Chromite", "Хроміт"),
            new OreDefinition("malachite", "#22e3b9", List.of(BlockSubLayer.DEEPSLATE), BlockOverlay.COPPER, ItemBase.COPPER, 13, 26, "Malachite", "Малахіт"),
            new OreDefinition("fluorite", "#375e34", List.of(BlockSubLayer.NETHERRACK), BlockOverlay.URANIUM, ItemBase.ZINC, 16, 31, "Fluorite", "Флюорит"),
            new OreDefinition("pyrite", "#c99347", List.of(BlockSubLayer.STONE, BlockSubLayer.END, BlockSubLayer.NETHERRACK), BlockOverlay.GOLD, ItemBase.IRON, 11, 23, "Pyrite", "Пірит"),
            new OreDefinition("cinnabar", "#d80e40", List.of(BlockSubLayer.NETHERRACK), BlockOverlay.OSMIUM, ItemBase.CRYSTAL, 15, 30, "Cinnabar", "Кіновар"),
            new OreDefinition("peridot", "#78b955", List.of(BlockSubLayer.END), BlockOverlay.REDSTONE, ItemBase.AMETHYST, 18, 35, "Peridot", "Перидот"),
            new OreDefinition("sodalite", "#4a5e87", List.of(BlockSubLayer.END), BlockOverlay.LAPIS, ItemBase.IRON, 12, 24, "Sodalite", "Содаліт"),
            new OreDefinition("lazurite", "#4663c9", List.of(BlockSubLayer.END), BlockOverlay.BISMUTH, ItemBase.CRYSTAL, 14, 28, "Lazurite", "Лазуріт"),
            new OreDefinition("lapis_lazuli", "#2a3b86", List.of(BlockSubLayer.END), BlockOverlay.URANIUM, ItemBase.IRON, 14, 28, "Lapis Lazuli", "Лапіс-лазур"),
            new OreDefinition("tetrahedrite", "#9b5827", List.of(BlockSubLayer.NETHERRACK), BlockOverlay.COPPER, ItemBase.COPPER, 13, 26, "Tetrahedrite", "Тетраедрит"),
            new OreDefinition("stibnite", "#6e6053", List.of(BlockSubLayer.NETHERRACK), BlockOverlay.IRON, ItemBase.ZINC, 16, 33, "Stibnite", "Стибніт"),
            new OreDefinition("ilmenite", "#2f3732", List.of(BlockSubLayer.MOON), BlockOverlay.REDSTONE, ItemBase.IRON, 11, 22, "Ilmenite", "Ільменіт"),
            new OreDefinition("blue_topaz", "#3686d8", List.of(BlockSubLayer.NETHERRACK), BlockOverlay.DIAMOND, ItemBase.AMETHYST, 22, 45, "Blue Topaz", "Блакитний топаз"),
            new OreDefinition("topaz", "#c29511", List.of(BlockSubLayer.NETHERRACK), BlockOverlay.GOLD, ItemBase.QUARTZ, 20, 42, "Topaz", "Топаз"),
            new OreDefinition("chalcocite", "#2f2f2f", List.of(BlockSubLayer.NETHERRACK), BlockOverlay.COAL, ItemBase.COAL, 10, 20, "Chalcocite", "Халкоцит"),
            new OreDefinition("bornite", "#c3a104", List.of(BlockSubLayer.END), BlockOverlay.COPPER, ItemBase.COPPER, 12, 25, "Bornite", "Борніт"),
            new OreDefinition("sulfur", "#ffce1f", List.of(BlockSubLayer.NETHERRACK), BlockOverlay.GOLD, ItemBase.GOLD, 15, 30, "Sulfur", "Сірка"),
            new OreDefinition("sphalerite", "#7d4a17", List.of(BlockSubLayer.NETHERRACK), BlockOverlay.IRON, ItemBase.COPPER, 14, 27, "Sphalerite", "Сфалерит"),
            new OreDefinition("monazite", "#d15dbf", List.of(BlockSubLayer.NETHERRACK), BlockOverlay.GOLD, ItemBase.SALT_CRYSTAL, 18, 36, "Monazite", "Монацит"),
            new OreDefinition("ruby", "#c21041", List.of(BlockSubLayer.NETHERRACK, BlockSubLayer.DEEPSLATE), BlockOverlay.REDSTONE, ItemBase.IRON, 25, 50, "Ruby", "Рубін"),
            new OreDefinition("redstone", "#b90f00", List.of(BlockSubLayer.NETHERRACK, BlockSubLayer.DEEPSLATE), BlockOverlay.REDSTONE, ItemBase.IRON, 10, 25, "Redstone", "Редстоун"),
            new OreDefinition("saltpeter", "#d6d6d6", List.of(BlockSubLayer.STONE, BlockSubLayer.NETHERRACK), BlockOverlay.QUARTZ, ItemBase.QUARTZ, 10, 20, "Saltpeter", "Селітра"), //
            new OreDefinition("emerald", "#25c76c", List.of(BlockSubLayer.NETHERRACK), BlockOverlay.REDSTONE, ItemBase.COAL, 25, 50, "Emerald", "Смарагд"),
            new OreDefinition("beryllium", "#c3f0c1", List.of(BlockSubLayer.NETHERRACK), BlockOverlay.COPPER, ItemBase.COAL, 16, 32, "Beryllium", "Берилій"),
            new OreDefinition("tantalite", "#5d574a", List.of(BlockSubLayer.MARS), BlockOverlay.LEAD, ItemBase.IRON, 19, 39, "Tantalite", "Танталіт"),
            new OreDefinition("molybdenite", "#2d2836", List.of(BlockSubLayer.MOON, BlockSubLayer.NETHERRACK), BlockOverlay.ZINC, ItemBase.IRON, 17, 35, "Molybdenite", "Молібденіт"), //
            new OreDefinition("wulfenite", "#e8c04a", List.of(BlockSubLayer.NETHERRACK), BlockOverlay.GOLD, ItemBase.GOLD, 15, 30, "Wulfenite", "Вульфеніт"),
            new OreDefinition("powellite", "#b7a54d", List.of(BlockSubLayer.MERCURY, BlockSubLayer.NETHERRACK), BlockOverlay.GOLD, ItemBase.GOLD, 14, 29, "Powellite", "Повеліт"), //
            new OreDefinition("molybdenum", "#5c6470", List.of(BlockSubLayer.MARS), BlockOverlay.IRON, ItemBase.IRON, 16, 33, "Molybdenum", "Молібден"),
            new OreDefinition("gold", "#e6c44a", List.of(BlockSubLayer.STONE, BlockSubLayer.NETHERRACK, BlockSubLayer.END), BlockOverlay.GOLD, ItemBase.GOLD, 15, 35, "Gold", "Золото"),
            new OreDefinition("goethite", "#4e3f29", List.of(BlockSubLayer.DEEPSLATE), BlockOverlay.IRON, ItemBase.IRON, 13, 27, "Goethite", "Гетит"),
            new OreDefinition("vanadium_magnetite", "#ee5308", List.of(BlockSubLayer.MOON), BlockOverlay.OSMIUM, ItemBase.IRON, 18, 37, "Vanadium Magnetite", "Ванадій-магнетит"),
            new OreDefinition("rutile", "#aa4b24", List.of(BlockSubLayer.MOON), BlockOverlay.GOLD, ItemBase.GOLD, 17, 34, "Rutile", "Рутил"),
            new OreDefinition("silver", "#d5e0f0", List.of(BlockSubLayer.STONE), BlockOverlay.IRON, ItemBase.ZINC, 14, 29, "Silver", "Срібло"),
            new OreDefinition("bauxite", "#b3643b", List.of(BlockSubLayer.MOON, BlockSubLayer.STONE, BlockSubLayer.DEEPSLATE), BlockOverlay.COPPER, ItemBase.COPPER, 12, 25, "Bauxite", "Боксит"),
            new OreDefinition("salt", "#e3e0cf", List.of(BlockSubLayer.STONE), BlockOverlay.QUARTZ, ItemBase.SALT_CRYSTAL, 10, 15, "Salt", "Сіль"),
            new OreDefinition("rock_salt", "#dad6c5", List.of(BlockSubLayer.STONE), BlockOverlay.QUARTZ, ItemBase.CRYSTAL, 10, 15, "Rock Salt", "Кам'яна сіль"),
            new OreDefinition("lepidolite", "#af7cd3", List.of(BlockSubLayer.STONE), BlockOverlay.LAPIS, ItemBase.AMETHYST, 15, 30, "Lepidolite", "Лепідоліт"),
            new OreDefinition("tungstate", "#6e6b67", List.of(BlockSubLayer.END), BlockOverlay.IRON, ItemBase.IRON, 18, 36, "Tungstate", "Вольфрамат"),
            new OreDefinition("scheelite", "#d9a83a", List.of(BlockSubLayer.END), BlockOverlay.GOLD, ItemBase.ZINC, 16, 32, "Scheelite", "Шеєліт"),
            new OreDefinition("lithium", "#d8d0cc", List.of(BlockSubLayer.END), BlockOverlay.QUARTZ, ItemBase.QUARTZ, 12, 24, "Lithium", "Літій"),
            new OreDefinition("sheldonite", "#a302a3", List.of(BlockSubLayer.END), BlockOverlay.IRON, ItemBase.IRON, 20, 40, "Cooperite", "Куперіт"),
            new OreDefinition("uraninite", "#4a7a35", List.of(BlockSubLayer.END), BlockOverlay.LEAD, ItemBase.URANIUM, 23, 46, "Uraninite", "Уранініт"),
            new OreDefinition("thorium", "#155818", List.of(BlockSubLayer.END), BlockOverlay.IRON, ItemBase.IRON, 21, 42, "Thorium", "Торій"),
            new OreDefinition("pitchblende", "#70D53A", List.of(BlockSubLayer.END), BlockOverlay.URANIUM, ItemBase.COPPER, 22, 45, "Pitchblende", "Пітчбленд"),
            new OreDefinition("cassiterite", "#6d665b", List.of(BlockSubLayer.STONE), BlockOverlay.TIN, ItemBase.TIN, 12, 25, "Cassiterite", "Каситерит"),
            new OreDefinition("rhodochrosite", "#e67373", List.of(BlockSubLayer.END), BlockOverlay.GOLD, ItemBase.CRYSTAL, 16, 32, "Rhodochrosite", "Родохрозит"),
            new OreDefinition("lead", "#365491", List.of(BlockSubLayer.STONE), BlockOverlay.LEAD, ItemBase.GOLD, 13, 26, "Lead", "Свинець"),
            new OreDefinition("olivine", "#89a23c", List.of(BlockSubLayer.END), BlockOverlay.REDSTONE, ItemBase.AMETHYST, 17, 35, "Olivine", "Олівін"),
            new OreDefinition("trona", "#eae2c5", List.of(BlockSubLayer.NETHERRACK), BlockOverlay.QUARTZ, ItemBase.QUARTZ, 11, 23, "Trona", "Трона"), //
            new OreDefinition("bismuth", "#9e61bc", List.of(BlockSubLayer.NETHERRACK), BlockOverlay.BISMUTH, ItemBase.GOLD, 15, 31, "Bismuth", "Бісмут"),
            new OreDefinition("quartz", "#b9b1a3", List.of(BlockSubLayer.NETHERRACK, BlockSubLayer.END), BlockOverlay.QUARTZ, ItemBase.QUARTZ, 10, 25, "Quartz", "Кварц"), //треба замінити
            new OreDefinition("diamond", "#7ce8d9", List.of(BlockSubLayer.DEEPSLATE), BlockOverlay.DIAMOND, ItemBase.DIAMOND, 25, 55, "Diamond", "Алмаз"), //треба замінити
            new OreDefinition("bort", "#5cc8bc", List.of(BlockSubLayer.DEEPSLATE), BlockOverlay.DIAMOND, ItemBase.SALT_CRYSTAL, 20, 45, "Bort", "Борт"), //
            new OreDefinition("cassiterite_sand", "#9e927b", List.of(BlockSubLayer.STONE, BlockSubLayer.DEEPSLATE), BlockOverlay.TIN, ItemBase.TIN, 11, 22, "Cassiterite Sand", "Каситеритовий пісок"), //
            new OreDefinition("iridium", "#ffffff", List.of(BlockSubLayer.MARS), BlockOverlay.IRON, ItemBase.IRIDIUM, 25, 59, "Iridium", "Іридій"), //
            new OreDefinition("gold_amalgam", "#f1d76c", List.of(BlockSubLayer.END), BlockOverlay.GOLD, ItemBase.QUARTZ, 15, 30, "Gold Amalgam", "Золота амальгама"),
            new OreDefinition("silver_amalgam", "#d7d7dd", List.of(BlockSubLayer.END), BlockOverlay.GOLD, ItemBase.OSMIUM, 14, 28, "Silver Amalgam", "Срібна амальгама"),
            new OreDefinition("coal", "#1a1a1a", List.of(BlockSubLayer.STONE, BlockSubLayer.DEEPSLATE), BlockOverlay.COAL, ItemBase.COAL, 10, 20, "Coal", "Вугілля"), //треба замінити
            new OreDefinition("lignite_coal", "#3b2c1f", List.of(BlockSubLayer.STONE), BlockOverlay.COAL, ItemBase.COAL, 10, 18, "Lignite Coal", "Буре вугілля"), //треба замінити
            new OreDefinition("azure_silver", "#cb6494", List.of(BlockSubLayer.END), BlockOverlay.IRON, ItemBase.IRON, 24, 48, "Azure Silver", "Лазурне срібло"),
            new OreDefinition("stannite", "#B9C8C1", List.of(BlockSubLayer.NETHERRACK), BlockOverlay.TIN, ItemBase.TIN, 13, 27, "Stannite", "Станніт"),
            new OreDefinition("crimson_iron", "#db4941", List.of(BlockSubLayer.NETHERRACK), BlockOverlay.REDSTONE, ItemBase.TIN, 14, 29, "Crimson Iron", "Багряне залізо"),
            new OreDefinition("nickel", "#b1b487", List.of(BlockSubLayer.STONE), BlockOverlay.ZINC, ItemBase.TIN, 12, 25, "Nickel", "Нікель")

            );
}
