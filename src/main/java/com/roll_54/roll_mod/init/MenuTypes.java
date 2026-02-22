package com.roll_54.roll_mod.init;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.screen.menu.GrowthChamberMenu;
import com.roll_54.roll_mod.screen.menu.PedestalMenu;
import com.roll_54.roll_mod.screen.menu.ResearchWorkbenchMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class MenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, RollMod.MODID);

    private static <T extends AbstractContainerMenu>DeferredHolder<MenuType<?>, MenuType<T>> registerMenuType(String name, IContainerFactory<T> factory){
        return MENUS.register(name, () -> IMenuTypeExtension.create(factory));
    }

    public static void register(net.neoforged.bus.api.IEventBus eventBus) {
        MENUS.register(eventBus);
    }


    public static final DeferredHolder<MenuType<?>, MenuType<PedestalMenu>> PEDESTAL_MENU =
            registerMenuType("pedestal_menu", PedestalMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<GrowthChamberMenu>> GROWTH_CHAMBER_MENU =
            registerMenuType("growth_chamber_menu", GrowthChamberMenu::new);

    public static final DeferredHolder<MenuType<?>, MenuType<ResearchWorkbenchMenu>> RESEARCH_WORKBENCH_MENU =
            registerMenuType("research_workbench_menu", ResearchWorkbenchMenu::new);
}
