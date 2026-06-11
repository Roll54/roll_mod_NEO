package com.roll_54.roll_mod.compat;

import dev.ftb.mods.ftbranks.FTBRanks;
import net.neoforged.fml.ModList;

public class Compat {

    public static final boolean FTBRANKS = ModList.get().isLoaded(FTBRanks.MOD_ID);


    private Compat(){}
}
