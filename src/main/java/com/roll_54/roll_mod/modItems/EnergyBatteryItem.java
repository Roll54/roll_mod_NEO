package com.roll_54.roll_mod.modItems;

import aztech.modern_industrialization.MIComponents;
import com.roll_54.roll_mod.util.EnergyFormatUtils;
import com.roll_54.roll_mod.сonfig.GeneralConfig;
import com.roll_54.roll_mod.data.ToggleableItem;
import dev.technici4n.grandpower.api.ILongEnergyStorage;
import dev.technici4n.grandpower.api.ISimpleEnergyItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class EnergyBatteryItem extends Item implements ISimpleEnergyItem, ToggleableItem {

    private final long energyCapacity;
    private final long maxInput;
    private final long maxOutput;
    private final int barColor;

    public EnergyBatteryItem(Properties properties,
                             long energyCapacity,
                             long maxInput,
                             long maxOutput,
                             int barColor) {
        super(properties);
        this.energyCapacity = energyCapacity;
        this.maxInput = maxInput;
        this.maxOutput = maxOutput;
        this.barColor = barColor;
    }

    // === ISimpleEnergyItem ===
    @Override
    public net.minecraft.core.component.DataComponentType<Long> getEnergyComponent() {
        return MIComponents.ENERGY.get();
    }

    @Override
    public long getEnergyCapacity(ItemStack stack) {
        return energyCapacity;
    }

    @Override
    public long getEnergyMaxInput(ItemStack stack) {
        return maxInput;
    }

    @Override
    public long getEnergyMaxOutput(ItemStack stack) {
        return maxOutput;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        double frac = (double) getStoredEnergy(stack) / getEnergyCapacity(stack);
        return (int) (13 * frac);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return barColor;
    }

    // === Перемикач ON/OFF через ПКМ ===
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean active = isActivated(stack);

        if (!level.isClientSide) {
            boolean newState = !active;
            setActivated(player, stack, newState);

            // ⚡️ Оновити дані на сервері й клієнті
            player.getInventory().setChanged();

            Component stateText = Component.translatable(
                    newState ? "tooltip.roll_mod.on" : "tooltip.roll_mod.off"
            ).withStyle(newState ? ChatFormatting.GREEN : ChatFormatting.RED);

            player.displayClientMessage(
                    Component.translatable("tooltip.roll_mod.battery_toggled", stateText),
                    true
            );
        }
        level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6f, 1.4f);

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    // === Основна логіка живлення предметів ===
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide) return;
        if (!(entity instanceof Player player)) return;

        // === Тумблер активності ===
        boolean active = isActivated(stack);
        if (!active) return; // батарейка вимкнена

        long stored = getStoredEnergy(stack);
        if (stored <= 0) return;

        if (GeneralConfig.isDebug) {
            System.out.println("[Battery] === Tick start ===");
            System.out.println("[Battery] Поточна енергія батарейки: " + stored + " EU");
            System.out.println("[Battery] Тумблер активний: " + active);
            System.out.println("[Battery] Перевіряємо предмети в інвентарі гравця " + player.getName().getString() + "...");
            System.out.println("[Battery] ==============================");
        }

        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack other = player.getInventory().items.get(i);
            if (other.isEmpty()) continue;
            if (other == stack) continue; // не заряджаємо саму себе
            if (other.getItem() instanceof EnergyBatteryItem) continue; // не заряджаємо інші батарейки

            String name = other.getHoverName().getString();
            if (GeneralConfig.isDebug) System.out.println("[Battery] Слот " + i + ": " + name);

            ILongEnergyStorage target = null;

            // === Спроба знайти GrandPower capability ===
            try {
                target = other.getCapability(ILongEnergyStorage.ITEM);
            } catch (Exception ignored) {}

            // === Якщо нема — пробуємо Forge Energy ===
            if (target == null) {
                try {
                    var forge = other.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM);
                    if (forge != null) target = ILongEnergyStorage.of(forge);
                } catch (Exception ignored) {}
            }

            // === Основна передача через capability ===
            long received = 0L;
            if (target != null) {
                long toSend = Math.min(getEnergyMaxOutput(stack), stored);
                received = target.receive(toSend, false);
                if (received > 0) {
                    setStoredEnergy(stack, stored - received);
                    stored -= received;
                    if (GeneralConfig.isDebug)
                        System.out.println("   ✅ [Capability] Передано " + received + " EU у " + name);
                } else if (GeneralConfig.isDebug) {
                    System.out.println("   ❌ [Capability] Предмет не прийняв енергію (0 EU).");
                }
            }

            // === Якщо capability не знайдено або не прийняв — MI/EI компоненти ===
            if (received == 0) {
                try {
                    var miEnergy = aztech.modern_industrialization.MIComponents.ENERGY.get();

                    // визначаємо, який компонент є
                    var comp = aztech.modern_industrialization.MIComponents.ENERGY.get();
                    if (!other.has(comp)) {
                        if (GeneralConfig.isDebug)
                            System.out.println("   ⚠️ [MI Write] У предмета немає energy-компонента (MI/EI).");
                        continue;
                    }

                    long energy = other.getOrDefault(comp, 0L);
                    long capacity = 0L;

                    if (other.getItem() instanceof ISimpleEnergyItem simple)
                        capacity = simple.getEnergyCapacity(other);
                    if (capacity <= 0L) capacity = 10_000_000L; // запасне значення

                    long toSend = Math.min(getEnergyMaxOutput(stack), stored);
                    long space = Math.max(0, capacity - energy);
                    long inserted = Math.min(toSend, space);

                    if (inserted > 0) {
                        other.set(comp, energy + inserted);
                        setStoredEnergy(stack, stored - inserted);
                        stored -= inserted;
                        if (GeneralConfig.isDebug)
                            System.out.println("   ✅ [" + (comp == miEnergy ? "MI" : "EI") + " Write] Передано " + inserted + " EU у " + name);
                    } else if (GeneralConfig.isDebug) {
                        System.out.println("   ⚠️ [" + (comp == miEnergy ? "MI" : "EI") + " Write] Повний або не прийняв енергію (inserted=0)");
                    }
                } catch (Exception ex) {
                    if (GeneralConfig.isDebug) {
                        System.out.println("   ⚠️ [Write] Помилка під час запису енергії: " + ex);
                        ex.printStackTrace();
                    }
                }
            }

            // === Якщо батарейка розрядилась — зупиняємо цикл ===
            if (stored <= 0) {
                if (GeneralConfig.isDebug)
                    System.out.println("[Battery] ⚡ Енергія батарейки вичерпана.");
                break;
            }
        }

        if (GeneralConfig.isDebug) System.out.println("[Battery] === Tick end ===");
    }

    // === Tooltip ===
    @Override
    public void appendHoverText(ItemStack stack,
                                Item.TooltipContext ctx,
                                List<Component> tooltip,
                                TooltipFlag flag) {
        long stored = getStoredEnergy(stack);
        long cap = getEnergyCapacity(stack);
        boolean active = isActivated(stack);

        String formattedStored = EnergyFormatUtils.formatEnergy(stored);
        String formattedCap = EnergyFormatUtils.formatEnergy(cap);

        tooltip.add(Component.translatable("tooltip.roll_mod.energy", formattedStored, formattedCap)
                .withStyle(ChatFormatting.AQUA));

        tooltip.add(Component.translatable("tooltip.roll_mod.mode",
                        Component.translatable(active ? "tooltip.roll_mod.on" : "tooltip.roll_mod.off"))
                .withStyle(active ? ChatFormatting.GREEN : ChatFormatting.RED));

        tooltip.add(Component.translatable("tooltip.roll_mod.toggle_hint")
                .withStyle(ChatFormatting.GRAY));

    }
}
