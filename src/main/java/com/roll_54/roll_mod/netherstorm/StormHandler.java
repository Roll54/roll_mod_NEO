package com.roll_54.roll_mod.netherstorm;

import com.roll_54.roll_mod.Roll_mod;
import dev.emi.emi.config.EmiConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Random;

@EventBusSubscriber(modid = Roll_mod.MODID) // змініть MODID тут і у StormShared
public class StormHandler {

    private static StormState state;

    /** Тег для речей, що захищають від шторму (маски тощо) */
    public static final TagKey<Item> STORM_PROTECTIVE_TAG =
            TagKey.create(
                    Registries.ITEM,
                    ResourceLocation.parse("roll_mod:storm_protective")
            );


    /** Лінива ініціалізація стану при першому тіку */
    private static void initIfNeeded(MinecraftServer server) {
        if (state != null) return;
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld != null) {
            state = StormState.get(overworld);
            if (state.ticksUntilNextStorm <= 0 && !state.stormActive) {
                // Початкову затримку, якщо її ще не згенеровано
                state.ticksUntilNextStorm = getRandomStormDelay();
                state.dirty();
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post e) { // вже "Post" без phase
        var server = e.getServer();
        if (server == null) return;
        if (server == null) return;

        initIfNeeded(server);
        if (state == null) return;

        // Якщо зараз немає шторму — відлічуємо до старту
        if (!state.stormActive) {
            if (--state.ticksUntilNextStorm <= 0) {
                startStorm(server);
            } else {
                state.dirty();
            }
            return;
        }

        // Якщо шторм активний — тікаємо та застосовуємо ефекти
        if (++state.stormTicks >= state.stormDuration) {
            endStorm(server);
        } else {
            applyStormInNether(server);
            state.dirty();
        }
    }

    //Перевірка на захист від шторму ДЛЯ ВСІЄЇ БРОНІ, а то хулє ми шапку надягаємо і нам добре?
    private static boolean isWearingFullProtectiveSet(ServerPlayer player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(STORM_PROTECTIVE_TAG)
                && player.getItemBySlot(EquipmentSlot.CHEST).is(STORM_PROTECTIVE_TAG)
                && player.getItemBySlot(EquipmentSlot.LEGS).is(STORM_PROTECTIVE_TAG)
                && player.getItemBySlot(EquipmentSlot.FEET).is(STORM_PROTECTIVE_TAG);
    }


    private static void applyStormInNether(MinecraftServer server) {
        ServerLevel nether = server.getLevel(Level.NETHER);
        if (nether == null) return;

        for (ServerPlayer player : nether.players()) {
            // пропускаємо креатив/спектатор
            var gm = player.gameMode.getGameModeForPlayer();
            if (gm.isCreative() || gm == GameType.SPECTATOR) continue;

            // Якщо повний сет захисту — шторм не діє + знімаємо погані ефекти
            if (isWearingFullProtectiveSet(player)) {
                // гарантовано нема wither/poison під час шторму з повним сетом
                player.removeEffect(MobEffects.WITHER);
                player.removeEffect(MobEffects.POISON);
                // можеш також прибрати інші «штормові» ефекти, якщо треба:
                // player.removeEffect(MobEffects.WEAKNESS);
                // player.removeEffect(MobEffects.CONFUSION);
                // player.removeEffect(MobEffects.BLINDNESS);
                continue;
            }

            // Інакше — частковий захист: маска в шоломі
            ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
            boolean hasProtectiveHelmet = helmet.is(STORM_PROTECTIVE_TAG);

            if (hasProtectiveHelmet) {
                // кожні 20 тік — зношувати шолом/маску
                if (player.tickCount % 20 == 0) {
                    helmet.hurtAndBreak(1, player, EquipmentSlot.HEAD);
                }
                // За бажанням: можна зробити «легші» дебафи або взагалі нічого.
                // Зараз — нічого не накладаємо.
            } else {
                // Повного сету немає і шолома немає — накладаємо негативні ефекти
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 1600, 3)); // 80с Weakness IV
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0)); // 10с Nausea I
                player.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 4));    // 10с Wither V
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200, 0)); // 10с Blindness I
                // poison за бажанням — ти згадував саме про неї:
                player.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 2));    // 10с Poison III
            }
        }
    }

    private static void startStorm(MinecraftServer server) {
        state.stormActive = true;
        state.stormTicks = 0;
        state.stormDuration = getRandomStormDuration();
        state.ticksUntilNextStorm = getRandomStormDelay(); // одразу готуємо затримку на наступний раз
        state.dirty();

        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.sendSystemMessage(Component.translatable("message.roll_mod.nether_storm.start").withStyle(style -> style.withColor(0xb00538)));
        }
    }

    private static void endStorm(MinecraftServer server) {
        state.stormActive = false;
        state.stormTicks = 0;
        state.dirty();

        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.sendSystemMessage(Component.translatable("message.roll_mod.nether_storm.end").withStyle(style -> style.withColor(0x009921)));
        }
    }

    private static int getRandomStormDelay() {
        // 1–12 годин; 72000 тік = 1 година (у Minecraft 20 тік/с)
        return 72_000 * (1 + new Random().nextInt(12));
    }

    private static int getRandomStormDuration() {
        // 1–6 годин
        return 72_000 * (1 + new Random().nextInt(6));
    }

    // Публічне API (можна викликати з команд)
    public static boolean isStormActive() { return state != null && state.stormActive; }
    public static int getStormTicks() { return state != null ? state.stormTicks : 0; }
    public static int getStormDuration() { return state != null ? state.stormDuration : 0; }
    public static int getTicksUntilNextStorm() { return state != null ? state.ticksUntilNextStorm : 0; }

    /** Примусовий запуск/завершення */
    static void forceStart(MinecraftServer server) {
        initIfNeeded(server);
        if (state != null && !state.stormActive) startStorm(server);
    }
    static void forceEnd(MinecraftServer server) {
        initIfNeeded(server);
        if (state != null && state.stormActive) endStorm(server);
    }
}
