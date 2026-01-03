package com.roll_54.roll_mod.modItems;

import aztech.modern_industrialization.MIComponents;
import com.roll_54.roll_mod.data.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Set;

public class ComponentApplicatorItem extends Item {

    private static final Set<DataComponentType<?>> BLACKLIST = Set.of(
            ModComponents.APPLICATOR_COLOR.get(),
            net.minecraft.core.component.DataComponents.CUSTOM_NAME,
            net.minecraft.core.component.DataComponents.LORE,
            ModComponents.ACTIVATED.get(),
            MIComponents.ENERGY.get(),
            ModComponents.SKIN_APPLICATOR_USED.get()
    );

    public ComponentApplicatorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack applicator = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.pass(applicator);
        }

        ItemStack target =
                hand == InteractionHand.MAIN_HAND
                        ? player.getOffhandItem()
                        : player.getMainHandItem();

        if (target.isEmpty()) {
            return InteractionResultHolder.fail(applicator);
        }

        if (target.getItem() instanceof ComponentApplicatorItem) {
            return InteractionResultHolder.fail(applicator);
        }

        // ❌ якщо вже використано — блокуємо
        if (target.has(ModComponents.SKIN_APPLICATOR_USED.get())) {
            player.displayClientMessage(
                    Component.translatable("message.roll_mod.skin_applicator_already_used").withStyle(ChatFormatting.RED),
                    true // над хотбаром
            );
            return InteractionResultHolder.fail(applicator);
        }

        // 1️⃣ копіюємо компоненти
        copyNonBlacklisted(applicator, target);

        // 2️⃣ ставимо маркер використання
        target.set(ModComponents.SKIN_APPLICATOR_USED.get(), true);

        // 3️⃣ витрачаємо аплікатор
        applicator.shrink(1);

        return InteractionResultHolder.success(applicator);
    }

    private static void copyNonBlacklisted(ItemStack from, ItemStack to) {
        from.getComponents().forEach(component -> {
            DataComponentType<?> type = component.type();
            if (!BLACKLIST.contains(type)) {
                copyComponent(component, to);
            }
        });
    }

    private static <T> void copyComponent(TypedDataComponent<T> component, ItemStack target) {
        target.set(component.type(), component.value());
    }
}
