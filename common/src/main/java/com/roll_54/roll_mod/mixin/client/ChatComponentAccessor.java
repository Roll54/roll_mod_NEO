package com.roll_54.roll_mod.mixin.client;

import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

/** Which chat line is under the mouse, for replying to it ({@code ChatReply}). */
@Mixin(ChatComponent.class)
public interface ChatComponentAccessor {

    @Accessor("allMessages")
    List<GuiMessage> roll_mod$allMessages();

    @Accessor("trimmedMessages")
    List<GuiMessage.Line> roll_mod$trimmedMessages();

    @Invoker("screenToChatX")
    double roll_mod$screenToChatX(double x);

    @Invoker("screenToChatY")
    double roll_mod$screenToChatY(double y);

    @Invoker("getLineHeight")
    int roll_mod$getLineHeight();

    @Invoker("getMessageEndIndexAt")
    int roll_mod$getMessageEndIndexAt(double chatX, double chatY);
}
