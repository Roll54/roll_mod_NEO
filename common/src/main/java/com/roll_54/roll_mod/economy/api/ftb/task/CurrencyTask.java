package com.roll_54.roll_mod.economy.api.ftb.task;

import com.roll_54.roll_mod.economy.api.CurrencyService;
import com.roll_54.roll_mod.economy.currency.model.CurrencyType;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbquests.quest.task.TaskType;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class CurrencyTask extends Task {

  private long amount;

  public CurrencyTask(long id, Quest quest) {
    super(id, quest);
    this.amount = 1L;
  }

  @Override
  public TaskType getType() {
    return CurrencyTaskType.CURRENCY;
  }

  @Override
  public long getMaxProgress() {
    return amount;
  }

  @Override
  public void submitTask(TeamData teamData, ServerPlayer player, ItemStack craftedItem) {
    if (!checkTaskSequence(teamData)) {
      return;
    }

    CurrencyService.get(player, CurrencyType.MAIN)
        .thenAcceptAsync(
            balance -> {
              long current = teamData.getProgress(this);

              long remaining = amount - current;
              if (remaining <= 0) return;

              long take = Math.min(balance, remaining);
              if (take <= 0) return;

              CurrencyService.withdraw(player, CurrencyType.MAIN, take);
              teamData.addProgress(this, take);
            },
            player.server);
  }

  @Override
  public void writeData(CompoundTag tag, HolderLookup.Provider provider) {
    super.writeData(tag, provider);
    tag.putLong("required_count", this.amount);
  }

  @Override
  public void readData(CompoundTag tag, HolderLookup.Provider provider) {
    super.readData(tag, provider);
    amount = Math.max(tag.getLong("required_count"), amount);
  }

  @Override
  public void fillConfigGroup(ConfigGroup config) {
    super.fillConfigGroup(config);

    config
        .addLong(
            "count", // ID в NBT
            amount, // поточне значення
            v -> amount = v, // setter
            1L, // default
            1L, // min
            Long.MAX_VALUE // max
            )
        .setNameKey("roll_mod.task.count");
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

  @Override
  public boolean canInsertItem() {
    return false;
  }

  @Override
  public boolean consumesResources() {
    return true;
  }

  @Override
  public boolean submitItemsOnInventoryChange() {
    return false;
  }

  @Override
  public boolean checkOnLogin() {
    return false;
  }

  @Override
  public Component getAltTitle() {
    return Component.translatable(
        "ftbquests.task.roll_mod_currency.currency",
        amount,
        Component.translatable("currency.rollcurrency.name"));
  }
}
