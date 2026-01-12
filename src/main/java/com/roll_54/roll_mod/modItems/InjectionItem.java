package com.roll_54.roll_mod.modItems;

import com.roll_54.roll_mod.init.ItemRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;



public class InjectionItem extends Item {

    private final List<InjectionEffect> effects;

    public InjectionItem(Properties properties, List<InjectionEffect> effects) {
        super(properties);
        this.effects = effects;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            for (InjectionEffect e : effects) {
                player.addEffect(new MobEffectInstance(
                        e.effect(),
                        e.durationTicks(),
                        e.amplifier()
                ));
            }

            if (!player.isCreative()) {
                stack.shrink(1);
                player.addItem(new ItemStack(ItemRegistry.SYRINGE.get()));
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack,
                                Item.TooltipContext ctx,
                                List<Component> tooltip,
                                TooltipFlag flag) {

        // Опис
        tooltip.add(
                Component.translatable("item.roll_mod.injection.description")
                        .withStyle(ChatFormatting.GRAY)
        );

        // Заголовок (1 / багато)
        if (effects.size() == 1) {
            tooltip.add(Component.translatable("item.roll_mod.injection.effect_single")
                    .withStyle(ChatFormatting.YELLOW));
        } else {
            tooltip.add(Component.translatable("item.roll_mod.injection.effect_plural")
                    .withStyle(ChatFormatting.YELLOW));
        }

        // Ефекти
        effects.forEach(e -> addEffectLine(tooltip, e));
    }


    private void addEffectLine(List<Component> tooltip, InjectionEffect e) {

        int totalSeconds = e.durationTicks() / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;


        Component time = Component.literal(minutes + "хв " + seconds);


        Component level = Component.translatable(
                "item.roll_mod.injection.effect_level",
                toRoman(e.amplifier() + 1)
        );

        MutableComponent line = Component.translatable(
                "item.roll_mod.injection.effect_line",
                Component.translatable(e.effect().value().getDescriptionId()),
                level,
                time
        );

        // 🔹 Колір з самого ефекту
        int color = e.effect().value().getColor();
        line.setStyle(line.getStyle().withColor(color));

        tooltip.add(line);
    }

    private static String toRoman(int number) {
        return switch (number) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(number);
        };
    }

}