package com.roll_54.roll_mod.compat.LDLib;

import com.lowdragmc.lowdraglib2.gui.event.ContainerMenuEvent;
import com.lowdragmc.lowdraglib2.gui.holder.IModularUIHolderMenu;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.texture.SpriteTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.MCSprites;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.plugin.ILDLibPlugin;
import com.lowdragmc.lowdraglib2.plugin.LDLibPlugin;
import com.roll_54.roll_mod.RollMod;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.FurnaceMenu;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@LDLibPlugin
public class MyLDLibPlugin implements ILDLibPlugin {
    public void onLoad() {
        // do your register or setup for LDLib2 here.
    }

    private static ModularUI createModularUI() {
        // create a root element
        var root = new UIElement();
        root.addChildren(
                // add a label to display text
                new Label().setText("My First UI"),
                // add a button with text
                new Button().setText("Click Me!"),
                // add an element to display an image based on a resource location
                new UIElement().layout(layout -> layout.width(80).height(80))
                        .style(style -> style.background(
                                SpriteTexture.of("ldlib2:textures/gui/icon.png"))
                        )
        ).style(style -> style.background(Sprites.BORDER)); // set a background for the root element
        // create a UI
        var ui = UI.of(root);
        // return a modular UI for runtime instance
        return ModularUI.of(ui);
    }


    /**
     * Register this class to the NeoForge event bus
     * This ensures the @SubscribeEvent methods are called
     */
    @EventBusSubscriber(modid = RollMod.MODID)
    public static class ModEvents {
        @SubscribeEvent
        public static void onContainerMenuCreateEvent(ContainerMenuEvent.Create event) throws Exception {
            // Attach a burn-time label to any furnace screen
            if (event.menu instanceof AbstractFurnaceMenu furnaceMenu
                    && furnaceMenu instanceof IModularUIHolderMenu uiHolderMenu) {
                var player = event.player;
                var field = AbstractFurnaceMenu.class.getDeclaredField("data");
                field.setAccessible(true);
                ContainerData data = (ContainerData) field.get(furnaceMenu);

                var mui = ModularUI.of(UI.of(
                        new UIElement().layout(l -> l.width(176).height(166)).addChildren(
                                new UIElement()
                                        .addChildren(
                                                new Label().bind(DataBindingBuilder.componentS2C(() ->
                                                        Component.literal("burn time: %.2f / %.2f s"
                                                                .formatted(data.get(2) / 20f, data.get(3) / 20f))
                                                ).build())
                                        )
                                        .layout(l -> l.positionType(TaffyPosition.ABSOLUTE)
                                                .widthPercent(100).paddingAll(5).top(-15))
                                        .style(s -> s.background(MCSprites.BORDER))
                        )
                ), player);
                uiHolderMenu.setModularUI(mui);
            }
        }
    }
}
