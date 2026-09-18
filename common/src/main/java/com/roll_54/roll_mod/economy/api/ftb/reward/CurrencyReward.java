package com.roll_54.roll_mod.economy.api.ftb.reward;

import static com.roll_54.roll_mod.economy.api.ftb.reward.CurrencyRewardType.CURRENCY;

import com.roll_54.roll_mod.economy.api.CurrencyService;
import com.roll_54.roll_mod.economy.currency.model.CurrencyType;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.reward.RewardType;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class CurrencyReward extends Reward {

  private long amount = 1L;

  public CurrencyReward(long id, Quest quest) {
    super(id, quest);
  }

  @Override
  public RewardType getType() {
    return CURRENCY;
  }

  // ===== CORE LOGIC =====
  @Override
  public void claim(ServerPlayer player, boolean notify) {
    if (amount <= 0) return;

    // Team reward is handled by FTB internally — claim() is called
    // for the correct player(s)
    CurrencyService.deposit(player, CurrencyType.MAIN, amount, player.server);
  }

  @Override
  public Component getAltTitle() {
    return Component.translatable(
        "ftbquests.reward.roll_mod_currency.currency",
        amount,
        Component.translatable("currency.rollcurrency.name"));
  }

  @Override
  public String getButtonText() {
    return "+" + amount;
  }

  // ===== PERSISTENCE =====
  @Override
  public void writeData(CompoundTag tag, HolderLookup.Provider provider) {
    super.writeData(tag, provider);
    tag.putLong("amount", amount);
  }

  @Override
  public void readData(CompoundTag tag, HolderLookup.Provider provider) {
    super.readData(tag, provider);
    this.amount = Math.max(1L, tag.getLong("amount"));
  }

  @Override
  public void writeNetData(RegistryFriendlyByteBuf buffer) {
    super.writeNetData(buffer);
    buffer.writeVarLong(this.amount);
  }

  @Override
  public void readNetData(RegistryFriendlyByteBuf buffer) {
    super.readNetData(buffer);
    this.amount = buffer.readVarLong();
  }

  // ===== CONFIG GUI =====
  @Override
  public void fillConfigGroup(ConfigGroup config) {
    super.fillConfigGroup(config);

    config
        .addLong("amount", amount, v -> amount = v, 1L, 1L, Long.MAX_VALUE)
        .setNameKey("roll_mod.reward.amount");
  }
}
