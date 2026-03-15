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

        for (OreDefinition def : OreDefinitions.ALL) {

            String oreName = isEnglish ? def.enUsName() : def.ukUaName();

            String rawSuffix;
            String crushedSuffix;
            String refinedSuffix;
            String purifiedSuffix;
            String dustSuffix = isEnglish ? " (Dust)" : " (пил)";
            String dustPureSuffix = isEnglish ? " (Pure Dust)" : " (Очищений пил)";
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
                    case END -> isEnglish ? "End Stone" : "Ендернякова руда";
                    case MOON -> isEnglish ? "Moon Stone" : "Місячна руда";
                    case MARS -> isEnglish ? "Mars Stone" : "Марсіанська руда";
                    case VENUS -> isEnglish ? "Venus Stone" : "Венеріанська руда";
                    case MERCURY -> isEnglish ? "Mercury Stone" : "Меркуріанська руда";
                };

                String blockKey = "block.roll_mod." + baseId + "_" + def.id();

                add(blockKey, String.format("%s (%s)", oreName, baseName));
            }
        }
    }
}
