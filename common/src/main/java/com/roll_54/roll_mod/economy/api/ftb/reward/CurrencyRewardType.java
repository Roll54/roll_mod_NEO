package com.roll_54.roll_mod.economy.api.ftb.reward;

import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftbquests.quest.reward.RewardType;
import dev.ftb.mods.ftbquests.quest.reward.RewardTypes;
import net.minecraft.resources.ResourceLocation;

public class CurrencyRewardType {
  public static final ResourceLocation ID =
      // Compatibility id, not an oversight: the server's quest files reference this type by id,
      // so it stays on the old namespace even though the mod no longer uses it anywhere else.
      ResourceLocation.fromNamespaceAndPath("roll_mod_currency", "currency");

  public static final RewardType CURRENCY =
      RewardTypes.register(
          ID, CurrencyReward::new, () -> Icon.getIcon("roll_mod:item/lp"));

  private CurrencyRewardType() {}

  public static void init() {
    RewardType unused = CurrencyRewardType.CURRENCY;
  }
}
