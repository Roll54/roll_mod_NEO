package com.roll_54.roll_mod.compat.tide;

import aztech.modern_industrialization.MIComponents;
import com.li64.tide.Tide;
import com.li64.tide.network.messages.EchoRodHookedMsg;
import com.li64.tide.registries.entities.misc.fishing.TideFishingHook;
import com.roll_54.roll_mod.util.EnergyFormatUtils;
import dev.technici4n.grandpower.api.ISimpleEnergyItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The electric fishing rod: a Tide rod that runs on energy instead of wearing out.
 * <ul>
 *   <li>Each catch costs {@link #ENERGY_PER_CATCH} EU; with less than that stored it will not cast. It never breaks.</li>
 *   <li>Sonar: when something bites, the player is shown what it is — Tide's own Rod of Resonance display.</li>
 *   <li>Fast: fish bite 50% sooner (the hook's wait runs at 1.5x).</li>
 *   <li>Enchantable like a good tool (enchantability 15): Lure, Luck of the Sea and the rest.</li>
 * </ul>
 */
public class ElectricFishingRodItem extends RollFishingRodItem implements ISimpleEnergyItem {

    public static final long CAPACITY = 1_000_000L;
    public static final long ENERGY_PER_CATCH = 2_000L;
    private static final int BAR_COLOR = 0x3FB5FF;

    /** The hooks already reported, so a bite is shown once. */
    private static final Map<TideFishingHook, Boolean> SHOWN = new WeakHashMap<>();
    private static Field timeUntilLured;

    public ElectricFishingRodItem(int baitSlots, Properties properties) {
        super(baitSlots, 1000, properties, Perk.SONAR, Perk.FAST, Perk.STEADY, Perk.ELECTRIC);
    }

    /* ----------------------------------------------------- energy ---------------------------------------------------- */

    @Override public DataComponentType<Long> getEnergyComponent() { return MIComponents.ENERGY.get(); }
    @Override public long getEnergyCapacity(ItemStack stack) { return CAPACITY; }
    @Override public long getEnergyMaxInput(ItemStack stack) { return CAPACITY; }
    @Override public long getEnergyMaxOutput(ItemStack stack) { return 0; }

    /** Runs on energy, so it takes no wear at all. */
    @Override
    public void setDamage(ItemStack stack, int damage) {}

    @Override public boolean isBarVisible(ItemStack stack) { return true; }
    @Override public int getBarColor(ItemStack stack) { return BAR_COLOR; }

    @Override
    public int getBarWidth(ItemStack stack) {
        return (int) Math.round(13.0 * getStoredEnergy(stack) / CAPACITY);
    }

    @Override
    public int getEnchantmentValue() {
        return 15;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // Reeling in is always allowed; casting wants a catch's worth of energy.
        if (player.fishing == null && getStoredEnergy(stack) < ENERGY_PER_CATCH) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("message.roll_mod.electric_rod.no_energy",
                    EnergyFormatUtils.formatEnergy(ENERGY_PER_CATCH)).withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(stack);
        }
        return super.use(level, player, hand);
    }

    /** Called by {@link TideRodPerks} on a catch. */
    void payForCatch(ItemStack stack) {
        tryUseEnergy(stack, ENERGY_PER_CATCH);
    }

    /* ---------------------------------------------- sonar & speed, while held --------------------------------------- */

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
        super.inventoryTick(stack, level, holder, slot, selected);
        if (level.isClientSide || !(holder instanceof ServerPlayer player)) return;
        // Tide keeps its own hook beside the vanilla one the player holds.
        TideFishingHook hook = com.li64.tide.registries.entities.misc.fishing.HookAccessor.getHook(player);
        if (hook == null) return;
        if (player.getMainHandItem() != stack && player.getOffhandItem() != stack) return;

        // Fast: the hook's wait for a bite counts down half as fast again.
        if (player.getRandom().nextBoolean()) speedUp(hook);

        // Sonar: once per bite, what is on the hook.
        if (hook.hasHookedItem() && !SHOWN.containsKey(hook)
                && (hook.getCatchType() == TideFishingHook.CatchType.FISH || hook.getCatchType() == TideFishingHook.CatchType.ITEM)) {
            List<ItemStack> hooked = hook.getHookedItems();
            if (!hooked.isEmpty() && !hooked.get(0).isEmpty()) {
                SHOWN.put(hook, Boolean.TRUE);
                Tide.NETWORK.sendToPlayer(new EchoRodHookedMsg(hooked.get(0)), player);
                level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.PLAYERS, 0.8f, 1.6f);
            }
        }
    }

    private static void speedUp(TideFishingHook hook) {
        try {
            if (timeUntilLured == null) {
                timeUntilLured = TideFishingHook.class.getDeclaredField("timeUntilLured");
                timeUntilLured.setAccessible(true);
            }
            int left = timeUntilLured.getInt(hook);
            if (left > 1) timeUntilLured.setInt(hook, left - 1);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // A Tide without the field: the rod is simply not faster.
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.roll_mod.electric_rod.energy",
                EnergyFormatUtils.formatEnergy(getStoredEnergy(stack)), EnergyFormatUtils.formatEnergy(CAPACITY))
                .withStyle(ChatFormatting.AQUA));
        super.appendHoverText(stack, context, lines, flag);
    }
}
