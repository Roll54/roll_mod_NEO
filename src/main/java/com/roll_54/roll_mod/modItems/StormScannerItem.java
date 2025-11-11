package com.roll_54.roll_mod.modItems;

import com.roll_54.roll_mod.netherstorm.StormHandler;
import com.roll_54.roll_mod.util.TimeUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import static com.roll_54.roll_mod.netherstorm.StormHandler.isStormActive;

public class StormScannerItem extends Item  {

    private final int tier;

    public StormScannerItem(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }


    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (tier == 1) {
            // 🌩️ Примітивний сканер — лише стан шторму
            if (!level.isClientSide) {
                if (isStormActive()) {
                    player.displayClientMessage(
                            Component.translatable("message.roll_mod.storm_scanner.storm_active")
                                    .withStyle(style -> style.withColor(TextColor.fromRgb(0xb00538))),
                            true
                    );
                } else {
                    player.displayClientMessage(
                            Component.translatable("message.roll_mod.storm_scanner.storm_not_active")
                                    .withStyle(style -> style.withColor(TextColor.fromRgb(0x009921))),
                            true
                    );
                }
            }
        } else {
            // ⚡ Просунутий сканер — детальна інформація
            if (!level.isClientSide) {

                int ticks = StormHandler.getStormTicks();
                int duration = StormHandler.getStormDuration();
                int untilNext = StormHandler.getTicksUntilNextStorm();

                boolean active = isStormActive();

                // 1️⃣ Стан шторму
                player.displayClientMessage(
                        Component.translatable(active
                                ? "message.roll_mod.storm_scanner.status_active"
                                : "message.roll_mod.storm_scanner.status_inactive"
                        ).withStyle(style -> style.withColor(TextColor.fromRgb(active ? 0xb00538 : 0x009921))),
                        false
                );

                // 2️⃣ До кінця або початку
                if (active) {
                    player.displayClientMessage(
                            Component.translatable("message.roll_mod.storm_scanner.time_until_end",
                                            TimeUtil.formatTime(duration - ticks))
                                    .withStyle(style -> style.withColor(TextColor.fromRgb(0x0099ff))),
                            false
                    );
                } else {
                    player.displayClientMessage(
                            Component.translatable("message.roll_mod.storm_scanner.time_until_start",
                                            TimeUtil.formatTime(untilNext))
                                    .withStyle(style -> style.withColor(TextColor.fromRgb(0x0099ff))),
                            false
                    );
                }

                // 3️⃣ Повідомлення у верхній частині екрана
                player.displayClientMessage(
                        Component.translatable("message.roll_mod.storm_scanner.detailed_info")
                                .withStyle(style -> style.withColor(TextColor.fromRgb(0x00bfff))),
                        true
                );
            }
        }
        player.getCooldowns().addCooldown(this, 200);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
