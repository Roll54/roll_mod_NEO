package com.roll_54.roll_mod.compat.gtmogs.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.quantumgarbage.gtmogs.api.gui.widget.SlotWidget;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * GTMOGS's vein page in EMI/JEI puts every ore of a vein in one centred row across a 120 px panel, so a
 * vein with more than six ores runs off both sides. Here the ores wrap into rows of six, each row
 * centred, and the text and dimension icons under them move down by the rows added.
 *
 * <p>{@code setupBaseGui} (the ore row, at y 18) runs before {@code setupText} (labels from y 40,
 * dimension icons from y 80), so the shift is known by the time the text is laid out.
 *
 * <p>Targets GTMOGS by name; the config is soft, so without GTMOGS this is skipped.
 */
@Mixin(targets = "com.quantumgarbage.gtmogs.integration.xei.widgets.GTOreVeinWidget")
public abstract class OreVeinWidgetWrapMixin {

    @Unique
    private static final int ROLL_MOD$PANEL = 120;
    @Unique
    private static final int ROLL_MOD$SLOT = 18;
    @Unique
    private static final int ROLL_MOD$PER_ROW = 6;

    /** How far the text under the ores moves down: one slot height per extra row. */
    @Unique
    private int roll_mod$shift;

    @WrapOperation(method = "setupBaseGui",
            at = @At(value = "NEW",
                    target = "(Lnet/neoforged/neoforge/items/IItemHandlerModifiable;IIIZZ)Lcom/quantumgarbage/gtmogs/api/gui/widget/SlotWidget;"))
    private SlotWidget roll_mod$wrapOres(IItemHandlerModifiable handler, int index, int x, int y,
                                         boolean canTake, boolean canPut, Operation<SlotWidget> original) {
        int count = handler.getSlots();
        int rows = (count + ROLL_MOD$PER_ROW - 1) / ROLL_MOD$PER_ROW;
        roll_mod$shift = (rows - 1) * ROLL_MOD$SLOT;
        int row = index / ROLL_MOD$PER_ROW;
        int inRow = Math.min(ROLL_MOD$PER_ROW, count - row * ROLL_MOD$PER_ROW);
        int newX = (ROLL_MOD$PANEL - ROLL_MOD$SLOT * inRow) / 2 + ROLL_MOD$SLOT * (index % ROLL_MOD$PER_ROW);
        return original.call(handler, index, newX, y + row * ROLL_MOD$SLOT, canTake, canPut);
    }

    @ModifyArg(method = "setupText",
            at = @At(value = "INVOKE",
                    target = "Lcom/lowdragmc/lowdraglib/gui/widget/LabelWidget;<init>(IILjava/lang/String;)V"),
            index = 1)
    private int roll_mod$shiftLabel(int y) {
        return y + roll_mod$shift;
    }

    @ModifyArg(method = "setupText",
            at = @At(value = "INVOKE",
                    target = "Lcom/quantumgarbage/gtmogs/integration/xei/widgets/GTOreVeinWidget;setupDimensionMarker(I)V"),
            index = 0)
    private int roll_mod$shiftDimensions(int y) {
        return y + roll_mod$shift;
    }
}
