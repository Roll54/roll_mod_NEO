package com.roll_54.roll_mod.minestar.containers;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.mixin.client.ImageButtonAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.world.inventory.RecipeBookType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Vanilla's recipe book, gone: the green book button is removed from every screen that has one
 * (inventory, crafting table, furnaces, and any mod screen reusing it), and the book is kept closed so
 * no screen opens with it showing or shifted aside for it. Recipes are looked up in TEI.
 *
 * <p>This is the client half. The server keeps no book and sends none: see
 * {@code mixin/recipebook} (no unlocks, no initial book, recipe-book packets ignored, every recipe
 * counted as known so TEI's grid transfer still works).
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = RollMod.MODID, value = Dist.CLIENT)
public final class NoRecipeBook {

    private NoRecipeBook() {}

    /** Before the screen lays itself out: a book left open would otherwise push the screen sideways. */
    @SubscribeEvent
    public static void onInitPre(ScreenEvent.Init.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        for (RecipeBookType type : RecipeBookType.values()) {
            if (mc.player.getRecipeBook().isOpen(type)) mc.player.getRecipeBook().setOpen(type, false);
        }
    }

    @SubscribeEvent
    public static void onInitPost(ScreenEvent.Init.Post event) {
        List<GuiEventListener> buttons = new ArrayList<>();
        for (GuiEventListener child : event.getListenersList()) {
            if (child instanceof ImageButton button
                    && ((ImageButtonAccessor) button).roll_mod$sprites() == RecipeBookComponent.RECIPE_BUTTON_SPRITES) {
                buttons.add(child);
            }
        }
        buttons.forEach(event::removeListener);
    }
}
