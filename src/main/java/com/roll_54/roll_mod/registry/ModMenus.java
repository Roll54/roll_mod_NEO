package com.roll_54.roll_mod.registry;

import com.roll_54.roll_mod.Roll_mod;
import com.roll_54.roll_mod.machine.menu.LCRMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Roll_mod.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<LCRMenu>> LARGE_CHEMICAL_REACTOR_MENU =
            MENUS.register("large_chemical_reactor",
                    () -> IMenuTypeExtension.create((windowId, inv, data) -> {
                        BlockPos pos = data.readBlockPos();
                        return new LCRMenu(windowId, inv, inv.player.level(), pos);
                    })
            );

    private ModMenus() {}
}
