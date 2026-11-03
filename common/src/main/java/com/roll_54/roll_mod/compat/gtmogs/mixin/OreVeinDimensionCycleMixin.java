package com.roll_54.roll_mod.compat.gtmogs.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.quantumgarbage.gtmogs.api.gui.widget.SlotWidget;
import com.quantumgarbage.gtmogs.api.transfer.item.CustomItemStackHandler;
import com.roll_54.roll_mod.compat.gtmogs.CyclingMarkerHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * A vein that rolls in several dimensions (the overworld veins also roll in the mining world) shows one
 * dimension slot that switches between their markers by itself, once a second, instead of a slot per
 * dimension.
 *
 * <p>GTMOGS fills one item handler with every marker and adds a slot widget per marker. Here the handler's
 * first slot cycles through all of them, and only the first slot widget is added.
 *
 * <p>Targets GTMOGS by name; the config is soft, so without GTMOGS this is skipped.
 */
@Mixin(targets = "com.quantumgarbage.gtmogs.integration.xei.widgets.GTOreVeinWidget")
public abstract class OreVeinDimensionCycleMixin {

    /** Marker slots after the first, created but left out of the widget. */
    @Unique
    private final Set<Object> roll_mod$skipped = Collections.newSetFromMap(new IdentityHashMap<>());

    @WrapOperation(method = "setupDimensionMarker",
            at = @At(value = "NEW", target = "(I)Lcom/quantumgarbage/gtmogs/api/transfer/item/CustomItemStackHandler;"))
    private CustomItemStackHandler roll_mod$cyclingMarkers(int size, Operation<CustomItemStackHandler> original) {
        return new CyclingMarkerHandler(size);
    }

    @WrapOperation(method = "setupDimensionMarker",
            at = @At(value = "NEW",
                    target = "(Lnet/neoforged/neoforge/items/IItemHandlerModifiable;IIIZZ)Lcom/quantumgarbage/gtmogs/api/gui/widget/SlotWidget;"))
    private SlotWidget roll_mod$firstSlotOnly(IItemHandlerModifiable handler, int index, int x, int y,
                                             boolean canTake, boolean canPut, Operation<SlotWidget> original) {
        SlotWidget slot = original.call(handler, index, x, y, canTake, canPut);
        if (index > 0) roll_mod$skipped.add(slot);
        return slot;
    }

    @WrapOperation(method = "setupDimensionMarker",
            at = @At(value = "INVOKE",
                    target = "Lcom/quantumgarbage/gtmogs/integration/xei/widgets/GTOreVeinWidget;addWidget(Lcom/lowdragmc/lowdraglib/gui/widget/Widget;)Lcom/lowdragmc/lowdraglib/gui/widget/WidgetGroup;"))
    private WidgetGroup roll_mod$skipExtraSlots(WidgetGroup self, Widget widget, Operation<WidgetGroup> original) {
        if (roll_mod$skipped.remove(widget)) return self;
        return original.call(self, widget);
    }
}
