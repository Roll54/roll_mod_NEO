package com.roll_54.roll_mod_client.mixin.crops;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

/**
 * Keeps the colours in the magnifying glass overlay. AgriCraft 4.0.24 wraps each tooltip line to
 * the overlay's width — which keeps its styles — and then turns every wrapped piece into
 * {@code Component.literal(piece.getString())}, which throws them away. Here the piece is remembered
 * as its text is read, and rebuilt with its styles where the plain literal would have been made.
 */
@Mixin(targets = "com.agricraft.agricraft.client.gui.MagnifyingGlassOverlay")
public abstract class MagnifyingGlassOverlayColourMixin {

    /** The wrapped piece whose text was just read; {@code renderOverlay} runs on the render thread only. */
    @Unique
    private static FormattedText roll_mod$piece;

    @WrapOperation(method = "renderOverlay",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/FormattedText;getString()Ljava/lang/String;"))
    private static String roll_mod$rememberPiece(FormattedText piece, Operation<String> original) {
        roll_mod$piece = piece;
        return original.call(piece);
    }

    @WrapOperation(method = "renderOverlay",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Component;literal(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;"))
    private static MutableComponent roll_mod$styledPiece(String text, Operation<MutableComponent> original) {
        FormattedText piece = roll_mod$piece;
        roll_mod$piece = null;
        if (piece == null || !piece.getString().equals(text)) return original.call(text);
        MutableComponent styled = Component.empty();
        piece.visit((style, part) -> {
            styled.append(Component.literal(part).withStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        return styled;
    }
}
