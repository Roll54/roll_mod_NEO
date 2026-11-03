package com.roll_54.roll_mod.gtmogs.data.datagen;

import com.roll_54.roll_mod.gtmogs.common.registry.GTRegistration;
import com.roll_54.roll_mod.gtmogs.data.datagen.lang.LangHandler;
import net.minecraft.data.DataProvider;

import com.tterrag.registrate.providers.ProviderType;

public class GTRegistrateDatagen {

    public static void initPre() {
        DataProvider.INDENT_WIDTH.set(4);
        // replace some default providers with ours
    }

    public static void initPost() {

        GTRegistration.REGISTRATE.addDataGenerator(ProviderType.LANG, LangHandler::init);

    }
}