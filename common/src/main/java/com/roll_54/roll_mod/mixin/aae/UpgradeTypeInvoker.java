package com.roll_54.roll_mod.mixin.aae;

import appeng.core.definitions.ItemDefinition;
import net.pedroksl.advanced_ae.common.items.upgrades.UpgradeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** AAE's private {@link UpgradeType} constructor, for {@link UpgradeTypeMixin} to add a card type with. */
@Mixin(value = UpgradeType.class, remap = false)
public interface UpgradeTypeInvoker {

    @Invoker("<init>")
    static UpgradeType roll_mod$create(String internalName, int ordinal, String name, UpgradeType.Ability ability,
                                       UpgradeType.SettingType settingType, int cost,
                                       UpgradeType.ApplicationType applicationType, ItemDefinition<?> item) {
        throw new AssertionError();
    }
}
