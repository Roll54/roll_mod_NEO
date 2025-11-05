package com.roll_54.roll_mod.PYDatagen;

import com.roll_54.roll_mod.RollMod;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class PYItems {

    public static final DeferredRegister.Items ORE_ITEMS =
            DeferredRegister.createItems(RollMod.MODID);

    private static DeferredHolder<Item, Item> registerSimpleItem(String name) {
        return ORE_ITEMS.register(name, () -> new Item(new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        ORE_ITEMS.register(eventBus);
    }

    public static final DeferredHolder<Item, Item> MICA_RAW = registerSimpleItem("raw_mica");
    public static final DeferredHolder<Item, Item> MICA_DUST = registerSimpleItem("mica_dust");

    public static final DeferredHolder<Item, Item> KYANITE_RAW = registerSimpleItem("raw_kyanite");
    public static final DeferredHolder<Item, Item> KYANITE_DUST = registerSimpleItem("kyanite_dust");

    public static final DeferredHolder<Item, Item> HEMATITE_RAW = registerSimpleItem("raw_hematite");
    public static final DeferredHolder<Item, Item> HEMATITE_DUST = registerSimpleItem("hematite_dust");

    public static final DeferredHolder<Item, Item> YELLOW_LIMONITE_RAW = registerSimpleItem("raw_yellow_limonite");
    public static final DeferredHolder<Item, Item> YELLOW_LIMONITE_DUST = registerSimpleItem("yellow_limonite_dust");

    public static final DeferredHolder<Item, Item> BIOTITE_RAW = registerSimpleItem("raw_biotite");
    public static final DeferredHolder<Item, Item> BIOTITE_DUST = registerSimpleItem("biotite_dust");

    public static final DeferredHolder<Item, Item> MAGNETITE_RAW = registerSimpleItem("raw_magnetite");
    public static final DeferredHolder<Item, Item> MAGNETITE_DUST = registerSimpleItem("magnetite_dust");

    public static final DeferredHolder<Item, Item> GARNIERITE_RAW = registerSimpleItem("raw_garnierite");
    public static final DeferredHolder<Item, Item> GARNIERITE_DUST = registerSimpleItem("garnierite_dust");

    public static final DeferredHolder<Item, Item> PENTLANDITE_RAW = registerSimpleItem("raw_pentlandite");
    public static final DeferredHolder<Item, Item> PENTLANDITE_DUST = registerSimpleItem("pentlandite_dust");

    public static final DeferredHolder<Item, Item> CHALCOPYRITE_RAW = registerSimpleItem("raw_chalcopyrite");
    public static final DeferredHolder<Item, Item> CHALCOPYRITE_DUST = registerSimpleItem("chalcopyrite_dust");

    public static final DeferredHolder<Item, Item> CERTUS_QUARTZ_RAW = registerSimpleItem("raw_certus_quartz");
    public static final DeferredHolder<Item, Item> CERTUS_QUARTZ_DUST = registerSimpleItem("certus_quartz_dust");

    public static final DeferredHolder<Item, Item> PYROCHLORE_RAW = registerSimpleItem("raw_pyrochlore");
    public static final DeferredHolder<Item, Item> PYROCHLORE_DUST = registerSimpleItem("pyrochlore_dust");

    public static final DeferredHolder<Item, Item> PYROPE_RAW = registerSimpleItem("raw_pyrope");
    public static final DeferredHolder<Item, Item> PYROPE_DUST = registerSimpleItem("pyrope_dust");

    public static final DeferredHolder<Item, Item> APATITE_RAW = registerSimpleItem("raw_apatite");
    public static final DeferredHolder<Item, Item> APATITE_DUST = registerSimpleItem("apatite_dust");

    public static final DeferredHolder<Item, Item> GALENA_RAW = registerSimpleItem("raw_galena");
    public static final DeferredHolder<Item, Item> GALENA_DUST = registerSimpleItem("galena_dust");

    public static final DeferredHolder<Item, Item> PYROLUSITE_RAW = registerSimpleItem("raw_pyrolusite");
    public static final DeferredHolder<Item, Item> PYROLUSITE_DUST = registerSimpleItem("pyrolusite_dust");

    public static final DeferredHolder<Item, Item> CHROMITE_RAW = registerSimpleItem("raw_chromite");
    public static final DeferredHolder<Item, Item> CHROMITE_DUST = registerSimpleItem("chromite_dust");

    public static final DeferredHolder<Item, Item> MALACHITE_RAW = registerSimpleItem("raw_malachite");
    public static final DeferredHolder<Item, Item> MALACHITE_DUST = registerSimpleItem("malachite_dust");

    public static final DeferredHolder<Item, Item> FLUORITE_RAW = registerSimpleItem("raw_fluorite");
    public static final DeferredHolder<Item, Item> FLUORITE_DUST = registerSimpleItem("fluorite_dust");

    public static final DeferredHolder<Item, Item> PYRITE_RAW = registerSimpleItem("raw_pyrite");
    public static final DeferredHolder<Item, Item> PYRITE_DUST = registerSimpleItem("pyrite_dust");

    public static final DeferredHolder<Item, Item> CINNABAR_RAW = registerSimpleItem("raw_cinnabar");
    public static final DeferredHolder<Item, Item> CINNABAR_DUST = registerSimpleItem("cinnabar_dust");

    public static final DeferredHolder<Item, Item> PERIDOT_RAW = registerSimpleItem("raw_peridot");
    public static final DeferredHolder<Item, Item> PERIDOT_DUST = registerSimpleItem("peridot_dust");

    public static final DeferredHolder<Item, Item> SODALITE_RAW = registerSimpleItem("raw_sodalite");
    public static final DeferredHolder<Item, Item> SODALITE_DUST = registerSimpleItem("sodalite_dust");

    public static final DeferredHolder<Item, Item> TETRAHEDRITE_RAW = registerSimpleItem("raw_tetrahedrite");
    public static final DeferredHolder<Item, Item> TETRAHEDRITE_DUST = registerSimpleItem("tetrahedrite_dust");

    public static final DeferredHolder<Item, Item> STIBNITE_RAW = registerSimpleItem("raw_stibnite");
    public static final DeferredHolder<Item, Item> STIBNITE_DUST = registerSimpleItem("stibnite_dust");

    public static final DeferredHolder<Item, Item> ILMENITE_RAW = registerSimpleItem("raw_ilmenite");
    public static final DeferredHolder<Item, Item> ILMENITE_DUST = registerSimpleItem("ilmenite_dust");
}
