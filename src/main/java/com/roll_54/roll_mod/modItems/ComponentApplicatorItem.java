package com.roll_54.roll_mod.modItems;

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

    /* ============================= */
    /* ========= CONFIG ============ */
    /* ============================= */

    /** Компоненти, які НІКОЛИ не переносяться */
    private static final Set<DataComponentType<?>> BLACKLIST = Set.of(
            ModComponents.APPLICATOR_COLOR.get(), // RGB-ідентичність
            DataComponents.CUSTOM_NAME,
            DataComponents.LORE
    );

    /** Дефолтний колір (білий) */
    private static final int DEFAULT_COLOR = 0xFFFFFF;

    public ComponentApplicatorItem(Properties properties) {
        super(properties);
    }

    /* ============================= */
    /* ========= USE LOGIC ========= */
    /* ============================= */

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) {
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        }

        ItemStack main = player.getMainHandItem();
        ItemStack off  = player.getOffhandItem();

        if (off.isEmpty()) {
            return InteractionResultHolder.fail(main);
        }

        // SHIFT → напрям навпаки
        boolean reverse = player.isShiftKeyDown();

        ItemStack source = reverse ? off  : main;
        ItemStack target = reverse ? main : off;

        transferComponents(source, target);

        return InteractionResultHolder.success(main);
    }

    /* ============================= */
    /* ===== COMPONENT TRANSFER ==== */
    /* ============================= */

    private static void transferComponents(ItemStack from, ItemStack to) {
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

    /* ============================= */
    /* ===== COLOR HANDLING ======== */
    /* ============================= */

    /** Повертає RGB-колір аплікатора */
    public static int getColor(ItemStack stack) {
        return stack.getOrDefault(
                ModComponents.APPLICATOR_COLOR.get(),
                DEFAULT_COLOR
        );
    }

    /** Змішує старий колір з новим */
    public static void applyColor(ItemStack stack, int newColor) {
        int oldColor = getColor(stack);
        int mixed = blend(oldColor, newColor);
        stack.set(ModComponents.APPLICATOR_COLOR.get(), mixed);
    }

    /** Просте RGB-змішування (середнє значення) */
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