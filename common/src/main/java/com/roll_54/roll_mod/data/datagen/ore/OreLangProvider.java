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

            String rawSuffix;
            String crushedSuffix;
            String refinedSuffix;
            String purifiedSuffix;
            String dustSuffix = isEnglish ? " (Dust)" : " (пил)";
            String dustPureSuffix = isEnglish ? " (Pure Dust)" : " (Очищений пил)";
            String rawBlockSuffix = isEnglish ? " (Raw Block)" : " (Необроблений блок)";
            String dustImpureSuffix;

            if (isEnglish) {
                rawSuffix = " (Raw)";
                crushedSuffix = " (Crushed Ore)";
                refinedSuffix = " (Refined Ore)";
                purifiedSuffix = " (Purified Ore)";
                dustImpureSuffix = " (Impure Dust)";
            } else {
                // Ukrainian masculine/feminine variants depending on name ending
                rawSuffix = " (необроблена копалина)";
                crushedSuffix = " (дроблена копалина)";
                refinedSuffix = " (рафінована копалина)";
                purifiedSuffix = " (очищена копалина)";
                dustImpureSuffix =  " (неочищена копалина)";
            }

            add("item.roll_mod.raw_" + def.id(), oreName + rawSuffix);
            add("item.roll_mod.crushed_" + def.id() + "_ore", oreName + crushedSuffix);
            add("item.roll_mod.refined_" + def.id() + "_ore", oreName + refinedSuffix);
            add("item.roll_mod.purified_" + def.id() + "_ore", oreName + purifiedSuffix);
            add("item.roll_mod." + def.id() + "_dust", oreName + dustSuffix);
            add("item.roll_mod.pure_" + def.id() + "_dust", oreName + dustPureSuffix);
            add("item.roll_mod.impure_" + def.id() + "_dust", oreName + dustImpureSuffix);

            add("block.roll_mod.raw_" + def.id() + "_block", oreName + rawBlockSuffix);

            for (var base : def.bases()) {

                String baseId = switch (base) {
                    case STONE -> "stone";
                    case DEEPSLATE -> "deepslate";
                    case NETHERRACK -> "netherrack";
                    case END -> "end";
                    case MOON -> "moon";
                    case MARS -> "mars";
                    case VENUS -> "venus";
                    case MERCURY -> "mercury";
                };

                String baseName = switch (base) {
                    case STONE -> isEnglish ? "Stone" : "Кам'яна руда";
                    case DEEPSLATE -> isEnglish ? "Deepslate" : "Глибосланецева руда";
                    case NETHERRACK -> isEnglish ? "Netherrack" : "Незеракова руда";
                    case END -> isEnglish ? "End Stone" : "Ендер руда";
                    case MOON -> isEnglish ? "Moon Stone" : "Місячна руда";
                    case MARS -> isEnglish ? "Mars Stone" : "Марсіанська руда";
                    case VENUS -> isEnglish ? "Venus Stone" : "Венеріанська руда";
                    case MERCURY -> isEnglish ? "Mercury Stone" : "Меркуріанська руда";
                };

                String blockKey = "block.roll_mod." + baseId + "_" + def.id();

                add(blockKey, String.format("%s (%s)", oreName, baseName));

                String sampleKey = "block.roll_mod." + baseId + "_" + def.id() + "_ore_sample";

                add(sampleKey, isEnglish
                        ? String.format("%s (Ore sample)", oreName)
                        : String.format("%s (Зразок)", oreName));
            }
        }

        addMBD2EnergyTrait();
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
