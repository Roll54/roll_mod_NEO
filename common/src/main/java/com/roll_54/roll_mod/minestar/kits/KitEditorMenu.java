package com.roll_54.roll_mod.minestar.kits;

import com.roll_54.roll_mod.util.LegacyText;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * A kit's items as a four-row chest — 36 slots, a player inventory's worth, which is all a claim can
 * hand over — opened from the moderation tab: the kit on top, the moderator's own inventory below.
 * Items move as in any chest: what goes in leaves the moderator's inventory, what is taken out is
 * theirs to keep (operators only, see {@code KitService#mayRename}).
 *
 * <p>Closing the window saves the kit, in slot order: Esc or the inventory key closes it as any chest
 * does, and the "Back to kits" button over it ({@code KitEditorBackButton}) returns to the moderation
 * tab. A plain vanilla chest menu, so the client needs no screen of its own.
 *
 * @deprecated Replaced by the LdLib kit window ({@link KitWindows}), which lays a kit out as an
 *             inventory with armour, offhand and curio slots. Nothing opens this chest any more.
 */
@Deprecated
public final class KitEditorMenu extends ChestMenu {

    public static final int SLOTS = 36;

    private final String kit;
    private final SimpleContainer items;

    private KitEditorMenu(int id, Inventory inventory, SimpleContainer items, String kit) {
        super(MenuType.GENERIC_9x4, id, inventory, items, 4);
        this.items = items;
        this.kit = kit;
    }

    /** Opens the editor on {@code kit} for {@code player}; the caller has checked they may. */
    static void open(ServerPlayer player, Kit kit) {
        SimpleContainer items = new SimpleContainer(SLOTS);
        for (int i = 0; i < kit.items().size() && i < SLOTS; i++) items.setItem(i, kit.items().get(i).copy());
        Component title = Component.translatable("gui.roll_mod.kit.editor.title", LegacyText.display(kit.title()));
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new KitEditorMenu(id, inventory, items, kit.name()), title));
    }

    /** The window closed: what is in it now is the kit, and the moderator goes back to the hub. */
    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < items.getContainerSize(); i++) {
            ItemStack stack = items.getItem(i);
            if (!stack.isEmpty()) stacks.add(stack.copy());
        }
        KitService.saveItems(serverPlayer, kit, stacks, List.of());
    }
}
