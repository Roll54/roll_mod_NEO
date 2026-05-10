package com.roll_54.roll_mod.mixin.antidupe;

import net.minecraft.network.protocol.game.ServerboundEditBookPacket;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ServerboundEditBookPacket.class)
public class ServerboundEditBookPacketMixin {

    @ModifyConstant(method = "<clinit>", constant = @Constant(intValue = 8192, ordinal = 0))
    private static int antidupe$modifyPageEditLength(int original) {
        return WritableBookContent.PAGE_EDIT_LENGTH;
    }

    @ModifyConstant(method = "<clinit>", constant = @Constant(intValue = 200, ordinal = 0))
    private static int antidupe$modifyMaxPages(int original) {
        return WritableBookContent.MAX_PAGES;
    }

    @ModifyConstant(method = "<clinit>", constant = @Constant(intValue = 128, ordinal = 0))
    private static int antidupe$modifyMaxTitleLength(int original) {
        return WrittenBookContent.TITLE_MAX_LENGTH;
    }
}
