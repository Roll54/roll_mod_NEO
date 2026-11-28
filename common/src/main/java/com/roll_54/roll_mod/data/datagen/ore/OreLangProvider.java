package com.roll_54.roll_mod.data.datagen.ore;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class OreLangProvider extends LanguageProvider {

    private final boolean isEnglish;

    public OreLangProvider(PackOutput output, String modid, String locale, ExistingFileHelper existingFileHelper) {
        super(output, modid, locale);
        this.isEnglish = locale.equals("en_us");
    }

    @Override
    protected void addTranslations() {

        for (OreDefinition def : OreDefinitions.ORE_DEFINITIONS) {

            String oreName = isEnglish ? def.enUsName() : def.ukUaName();
            // The bare material name, for screens that list materials rather than items (the ore scanner).
            add("material.roll_mod." + def.id(), oreName);

            if (isEnglish) english(def, oreName);
            else ukrainian(def, oreName);
        }

        addMBD2EnergyTrait();
    }

    /**
     * English, after vanilla's pattern: "Hematite Dust", "Raw Hematite", "Block of Raw Hematite", "Hematite Ore"
     * (stone), "Deepslate Hematite Ore", "Hematite Ore Sample".
     */
    private void english(OreDefinition def, String name) {
        String id = def.id();
        add("item.roll_mod.raw_" + id, "Raw " + name);
        add("item.roll_mod.crushed_" + id + "_ore", "Crushed " + name + " Ore");
        add("item.roll_mod.refined_" + id + "_ore", "Refined " + name + " Ore");
        add("item.roll_mod.purified_" + id + "_ore", "Purified " + name + " Ore");
        add("item.roll_mod." + id + "_dust", name + " Dust");
        add("item.roll_mod.pure_" + id + "_dust", "Pure " + name + " Dust");
        add("item.roll_mod.impure_" + id + "_dust", "Impure " + name + " Dust");
        add("block.roll_mod.raw_" + id + "_block", "Block of Raw " + name);

        for (var base : def.bases()) {
            String rock = switch (base) {
                case STONE -> "";
                case DEEPSLATE -> "Deepslate ";
                case NETHERRACK -> "Netherrack ";
                case END -> "End Stone ";
                case MOON -> "Moon ";
                case MARS -> "Mars ";
                case VENUS -> "Venus ";
                case MERCURY -> "Mercury ";
            };
            add("block.roll_mod." + base.id() + "_" + id, rock + name + " Ore");
            // Named by its rock too, so the samples of one ore can be told apart.
            String sampleRock = base == OreTextureTemplates.BlockSubLayer.STONE ? "Stone " : rock;
            add("block.roll_mod." + base.id() + "_" + id + "_ore_sample", sampleRock + name + " Ore Sample");
        }
        for (var variant : def.variants()) {
            String rock = switch (variant) {
                case GRANITE -> "Granite";
                case DIORITE -> "Diorite";
                case ANDESITE -> "Andesite";
                case TUFF -> "Tuff";
                case BLACKSTONE -> "Blackstone";
                case BASALT -> "Basalt";
            };
            add("block.roll_mod." + def.variantBlockId(variant), rock + " " + name + " Ore");
        }
    }

    /**
     * Ukrainian, "what + of what" ({@link UkOreGrammar}): "Пил гематиту" (or "Сірчаний пил" where the language has
     * the adjective), "Необроблений гематит", "Блок необробленого гематиту", "Дроблена руда гематиту",
     * "Глибосланцева руда гематиту", "Глибосланцевий зразок гематиту".
     */
    private void ukrainian(OreDefinition def, String name) {
        String id = def.id();
        UkOreGrammar.Forms forms = UkOreGrammar.of(id);
        String gen = forms.genitive();
        String lower = name.toLowerCase(java.util.Locale.ROOT);

        add("item.roll_mod.raw_" + id, UkOreGrammar.capitalize(UkOreGrammar.agree("необроблений", forms.gender())) + " " + lower);
        add("item.roll_mod.crushed_" + id + "_ore", "Дроблена руда " + gen);
        add("item.roll_mod.refined_" + id + "_ore", "Рафінована руда " + gen);
        add("item.roll_mod.purified_" + id + "_ore", "Очищена руда " + gen);
        String dust = forms.dustAdj() != null ? forms.dustAdj() + " пил" : "пил " + gen;
        add("item.roll_mod." + id + "_dust", UkOreGrammar.capitalize(dust));
        add("item.roll_mod.pure_" + id + "_dust", "Очищений " + dust);
        add("item.roll_mod.impure_" + id + "_dust", "Неочищений " + dust);
        add("block.roll_mod.raw_" + id + "_block",
                "Блок " + UkOreGrammar.agreeGenitive("необроблений", forms.gender()) + " " + gen);

        for (var base : def.bases()) {
            String ore = switch (base) {
                case STONE -> "Кам'яна руда " + gen;
                case DEEPSLATE -> "Глибосланцева руда " + gen;
                case NETHERRACK -> "Незеракова руда " + gen;
                case END -> "Руда " + gen + " з каменю Енду";
                case MOON -> "Місячна руда " + gen;
                case MARS -> "Марсіанська руда " + gen;
                case VENUS -> "Венеріанська руда " + gen;
                case MERCURY -> "Меркуріанська руда " + gen;
            };
            add("block.roll_mod." + base.id() + "_" + id, ore);
            // Named by its rock too, so the samples of one ore can be told apart.
            String sample = switch (base) {
                case STONE -> "Кам'яний зразок " + gen;
                case DEEPSLATE -> "Глибосланцевий зразок " + gen;
                case NETHERRACK -> "Незераковий зразок " + gen;
                case END -> "Зразок " + gen + " з каменю Енду";
                case MOON -> "Місячний зразок " + gen;
                case MARS -> "Марсіанський зразок " + gen;
                case VENUS -> "Венеріанський зразок " + gen;
                case MERCURY -> "Меркуріанський зразок " + gen;
            };
            add("block.roll_mod." + base.id() + "_" + id + "_ore_sample", sample);
        }
        for (var variant : def.variants()) {
            String rock = switch (variant) {
                case GRANITE -> "Гранітна";
                case DIORITE -> "Діоритова";
                case ANDESITE -> "Андезитова";
                case TUFF -> "Туфова";
                case BLACKSTONE -> "Чорнокам'яна";
                case BASALT -> "Базальтова";
            };
            add("block.roll_mod." + def.variantBlockId(variant), rock + " руда " + gen);
        }
    }

    /** Config labels for the Modern Industrialization EU trait added to Multiblocked2 machines. */
    private void addMBD2EnergyTrait() {
        if (isEnglish) {
            add("config.definition.trait.mi_energy_storage", "MI Energy Storage (EU)");
            add("config.definition.trait.mi_energy_storage.capacity", "Capacity (EU)");
            add("config.definition.trait.mi_energy_storage.max_receive", "Max Receive (EU/t)");
            add("config.definition.trait.mi_energy_storage.max_receive.tooltip", "Maximum EU accepted per operation.");
            add("config.definition.trait.mi_energy_storage.max_extract", "Max Extract (EU/t)");
            add("config.definition.trait.mi_energy_storage.max_extract.tooltip", "Maximum EU emitted per operation.");
            add("config.definition.trait.mi_energy_storage.cable_tier", "Cable Tier");
            add("config.definition.trait.mi_energy_storage.cable_tier.tooltip", "MI voltage tier this machine bonds to: lv, mv, hv, ev, superconductor.");
            add("config.definition.trait.mi_energy_storage.auto_io.tooltip", "Automatically push/pull EU to adjacent blocks.");
        } else {
            add("config.definition.trait.mi_energy_storage", "Сховище енергії MI (EU)");
            add("config.definition.trait.mi_energy_storage.capacity", "Місткість (EU)");
            add("config.definition.trait.mi_energy_storage.max_receive", "Макс. прийом (EU/t)");
            add("config.definition.trait.mi_energy_storage.max_receive.tooltip", "Максимум EU, що приймається за операцію.");
            add("config.definition.trait.mi_energy_storage.max_extract", "Макс. видача (EU/t)");
            add("config.definition.trait.mi_energy_storage.max_extract.tooltip", "Максимум EU, що віддається за операцію.");
            add("config.definition.trait.mi_energy_storage.cable_tier", "Рівень кабелю");
            add("config.definition.trait.mi_energy_storage.cable_tier.tooltip", "Рівень напруги MI, до якого під'єднується машина: lv, mv, hv, ev, superconductor.");
            add("config.definition.trait.mi_energy_storage.auto_io.tooltip", "Автоматично передавати/приймати EU до сусідніх блоків.");
        }
    }
}
