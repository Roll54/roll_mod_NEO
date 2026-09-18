package com.roll_54.roll_mod.economy;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.economy.command.CurrencyTypeArgument;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRegistry {

  private static final DeferredRegister<ArgumentTypeInfo<?, ?>> ARGUMENT_TYPES =
      DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, RollMod.MODID);

  public static final DeferredHolder<
          ArgumentTypeInfo<?, ?>, SingletonArgumentInfo<CurrencyTypeArgument>>
      CURRENCY_TYPE_ARG =
          ARGUMENT_TYPES.register(
              "currency_type",
              () ->
                  ArgumentTypeInfos.registerByClass(
                      CurrencyTypeArgument.class,
                      SingletonArgumentInfo.contextFree(CurrencyTypeArgument::currencyType)));

  public static void register(IEventBus eventBus) {
    ARGUMENT_TYPES.register(eventBus);
  }
}
