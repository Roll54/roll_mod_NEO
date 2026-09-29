package com.roll_54.roll_mod.items.modulardrill;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.config.MyConfig;
import com.roll_54.roll_mod.registry.ModConfigs;
import com.roll_54.roll_mod.registry.BlockRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The server option that hides the new modular items behind a placeholder — see
 * {@link MyConfig.NewItems#showNewItems}. "New" means the modular drills and sabers, every module,
 * and the Module Installation Table's item.
 *
 * <p>A server setting, synced to every client by Fzzy Config, so the server owner decides for
 * everyone. The model switch is a {@code roll_mod:hidden} item predicate, read every frame, so
 * flipping the option swaps the textures live; names come from {@link #name} on both sides (so
 * server messages naming the item say "Unknown item" too), and tooltips are cut by a client
 * tooltip handler.
 */
public final class NewItemVisibility {

    /** The model every hidden item switches to: the "no texture yet" placeholder. */
    public static final String PLACEHOLDER_MODEL = "item/no_texture_yet_placeholder";

    private NewItemVisibility() {}

    /** Whether new items are hidden right now. */
    public static boolean hidden() {
        MyConfig config = ModConfigs.MAIN;
        return config != null && !config.newItems.showNewItems.get();
    }

    public static boolean isNew(ItemStack stack) {
        return isNew(stack.getItem());
    }

    public static boolean isNew(Item item) {
        return item instanceof ModularTool
                || item instanceof DrillModuleItem
                || item == BlockRegistry.MODULE_INSTALLATION_TABLE.get().asItem();
    }

    /** The name to show instead of {@code real} while hidden. */
    public static Component name(Component real) {
        return hidden() ? Component.translatable("item." + RollMod.MODID + ".hidden_placeholder") : real;
    }
}
