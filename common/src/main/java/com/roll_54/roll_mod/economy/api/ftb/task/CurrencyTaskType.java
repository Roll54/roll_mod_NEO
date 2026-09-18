package com.roll_54.roll_mod.economy.api.ftb.task;

import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftbquests.quest.task.TaskType;
import dev.ftb.mods.ftbquests.quest.task.TaskTypes;
import net.minecraft.resources.ResourceLocation;

public final class CurrencyTaskType {

  public static final ResourceLocation ID =
      // Compatibility id, not an oversight: the server's quest files reference this type by id,
      // so it stays on the old namespace even though the mod no longer uses it anywhere else.
      ResourceLocation.fromNamespaceAndPath("roll_mod_currency", "currency");

  public static final TaskType CURRENCY =
      TaskTypes.register(ID, CurrencyTask::new, () -> Icon.getIcon("roll_mod:item/lp"));

  private CurrencyTaskType() {}

  public static void init() {
    TaskType unused = CurrencyTaskType.CURRENCY;
  }
}
