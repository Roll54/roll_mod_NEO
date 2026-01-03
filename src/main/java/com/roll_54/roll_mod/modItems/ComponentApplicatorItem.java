package com.roll_54.roll_mod.modItems;

import aztech.modern_industrialization.MIComponents;
import com.roll_54.roll_mod.data.ModComponents;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Set;

public class ComponentApplicatorItem extends Item {


    private static final Set<DataComponentType<?>> BLACKLIST = Set.of(
            ModComponents.APPLICATOR_COLOR.get(), // RGB-ідентичність
            DataComponents.CUSTOM_NAME,
            DataComponents.LORE,
            ModComponents.ACTIVATED.get(),
            MIComponents.ENERGY.get()
    );

    private static final int DEFAULT_COLOR = 0xFFFFFF;

    public ComponentApplicatorItem(Properties properties) {
        super(properties);
    }

    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack applicator = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.pass(applicator);
        }

        ItemStack other =
                hand == InteractionHand.MAIN_HAND
                        ? player.getOffhandItem()
                        : player.getMainHandItem();

        if (other.isEmpty()) {
            return InteractionResultHolder.fail(applicator);
        }

        boolean reverse = player.isShiftKeyDown();

        ItemStack source = reverse ? other : applicator;
        ItemStack target = reverse ? applicator : other;

        // ❌ аплікатор ніколи не може бути target
        if (target.getItem() instanceof ComponentApplicatorItem) {
            return InteractionResultHolder.fail(applicator);
        }

        // 1️⃣ копіюємо стан
        copyNonBlacklisted(source, target);

        // 2️⃣ якщо аплікатор був source — чистимо його
        if (source == applicator) {
            clearNonBlacklisted(applicator);
        }

        return InteractionResultHolder.success(applicator);
    }



    private static void transferComponents(ItemStack from, ItemStack to) {

        to.getComponents().forEach(component -> {
            DataComponentType<?> type = component.type();
            if (!BLACKLIST.contains(type)) {
                to.remove(type);
            }
        });

        from.getComponents().forEach(component -> {
            DataComponentType<?> type = component.type();
            if (!BLACKLIST.contains(type)) {
                copyComponent(component, to);
            }
        });
    }


    public static int getColor(ItemStack stack) {
        return stack.getOrDefault(
                ModComponents.APPLICATOR_COLOR.get(),
                DEFAULT_COLOR
        );
    }

    public static void applyColor(ItemStack stack, int newColor) {
        int oldColor = getColor(stack);
        int mixed = blend(oldColor, newColor);
        stack.set(ModComponents.APPLICATOR_COLOR.get(), mixed);
    }

    private static void clearNonBlacklistedComponents(ItemStack stack) {
        var toRemove = new java.util.ArrayList<DataComponentType<?>>();

        stack.getComponents().forEach(component -> {
            DataComponentType<?> type = component.type();
            if (!BLACKLIST.contains(type)) {
                toRemove.add(type);
            }
        });

        for (DataComponentType<?> type : toRemove) {
            stack.remove(type);
        }
    }

    private static void clearNonBlacklisted(ItemStack stack) {
        for (DataComponentType<?> type : collectRemovable(stack)) {
            stack.remove(type);
        }
    }

    private static void copyNonBlacklisted(ItemStack from, ItemStack to) {
        from.getComponents().forEach(component -> {
            DataComponentType<?> type = component.type();
            if (!BLACKLIST.contains(type)) {
                copyComponent(component, to);
            }
        });
    }


    private static java.util.List<DataComponentType<?>> collectRemovable(ItemStack stack) {
        var list = new java.util.ArrayList<DataComponentType<?>>();

        stack.getComponents().forEach(component -> {
            DataComponentType<?> type = component.type();
            if (!BLACKLIST.contains(type)) {
                list.add(type);
            }
        });

        return list;
    }

    private static <T> void copyComponent(TypedDataComponent<T> component, ItemStack target) {
        target.set(component.type(), component.value());
    }


    private static int blend(int a, int b) {
        int ar = (a >> 16) & 0xFF;
        int ag = (a >> 8)  & 0xFF;
        int ab =  a        & 0xFF;

        int br = (b >> 16) & 0xFF;
        int bg = (b >> 8)  & 0xFF;
        int bb =  b        & 0xFF;

        return ((ar + br) / 2 << 16)
                | ((ag + bg) / 2 << 8)
                | ((ab + bb) / 2);
    }
}