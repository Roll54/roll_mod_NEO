package com.roll_54.roll_mod.Netherstorm;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.init.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import javax.annotation.Nullable;
import java.util.Random;

@EventBusSubscriber(modid = RollMod.MODID) // змініть MODID тут і у StormShared
public class StormHandler {

    private static StormState state;

    /**
     * Тег для речей, що захищають від шторму (маски тощо)
     */
    public static final TagKey<Item> STORM_PROTECTIVE_TAG =
            TagKey.create(
                    Registries.ITEM,
                    ResourceLocation.parse("roll_mod:storm_protective")
            );


    /**
     * Лінива ініціалізація стану при першому тіку
     */
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

    private static int spawnTickCounter = 0;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post e) {
        var server = e.getServer();
        initIfNeeded(server);
        if (state == null) return;

        if (!state.stormActive) {
            if (--state.ticksUntilNextStorm <= 0) {
                startStorm(server);
            } else {
                state.dirty();
            }
            return;
        }

        if (++state.stormTicks >= state.stormDuration) {
            endStorm(server);
        } else {
            applyStormInNether(server);
            state.dirty();

            // 🔥 Нове: спавн лічильником
            if (++spawnTickCounter >= 18000) { // 400 тік = 20 секунд
                spawnStormMobs(server);
                spawnTickCounter = 0;
            }
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

//            if (player.tickCount % 400 == 0) {
//                spawnAroundPlayer(nether, player, EntityType.WITHER_SKELETON);
//                spawnAroundPlayer(nether, player, EntityType.PIGLIN_BRUTE);
//                spawnAroundPlayer(nether, player, EntityType.BLAZE);
//            }

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
            }

        }
        if (state != null && state.stormTicks % 3000 == 0) { // кожні ~10 секунд
            tryGrowSulfurBerries(nether);
        }
    }

    private static void startStorm(MinecraftServer server) {
        state.stormActive = true;
        state.stormTicks = 0;
        state.stormDuration = getRandomStormDuration();
        state.ticksUntilNextStorm = getRandomStormDelay();
        state.dirty();

        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.sendSystemMessage(Component.translatable("message.roll_mod.nether_storm.start")
                    .withStyle(s -> s.withColor(0xb00538)));
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
    public static boolean isStormActive() {
        return state != null && state.stormActive;
    }

    public static int getStormTicks() {
        return state != null ? state.stormTicks : 0;
    }

    public static int getStormDuration() {
        return state != null ? state.stormDuration : 0;
    }

    public static int getTicksUntilNextStorm() {
        return state != null ? state.ticksUntilNextStorm : 0;
    }

    /**
     * Примусовий запуск/завершення
     */
    static void forceStart(MinecraftServer server, @Nullable Integer durationTicks) {
        initIfNeeded(server);
        if (state != null && !state.stormActive) {
            state.stormActive = true;
            state.stormTicks = 0;
            state.stormDuration = durationTicks != null ? durationTicks : getRandomStormDuration();
            state.ticksUntilNextStorm = getRandomStormDelay();
            state.dirty();

            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                p.sendSystemMessage(Component.literal(
                        "⚡ NetherStorm started for " + state.stormDuration + " ticks"
                ));
            }
        }
    }

    static void forceEnd(MinecraftServer server) {
        initIfNeeded(server);
        if (state != null && state.stormActive) endStorm(server);
    }

    private static void spawnStormMobs(MinecraftServer server) {
        ServerLevel nether = server.getLevel(Level.NETHER);
        if (nether == null) return;
        if (nether.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) return;

        for (ServerPlayer player : nether.players()) {
            // ❌ прибрано перевірку Creative/Spectator
            int count = 3 + nether.random.nextInt(8); // 3..10
            spawnWitherPack(nether, player, count);

            spawnAroundPlayer(nether, player, EntityType.BLAZE);
        }
    }

    private static void spawnWitherPack(ServerLevel level, ServerPlayer player, int count) {
        // Спершу знаходимо одну БАЗОВУ валідну позицію біля гравця (щоб зграя була поруч)
        BlockPos base = findGroundedPosNearPlayer(level, player, 12, 16);
        if (base == null) return;

        // Розкладаємо скелетів навколо бази у маленькому радіусі
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            // компактний розкид: -1..+1 по X/Z, щоб юніти стояли близько
            int ox = level.random.nextIntBetweenInclusive(-1, 1);
            int oz = level.random.nextIntBetweenInclusive(-1, 1);

            BlockPos pos = findGroundedPosFrom(level, base.offset(ox, 0, oz), 6);
            if (pos == null) continue;

            Mob mob = EntityType.WITHER_SKELETON.create(level);
            if (mob == null) continue;

            mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360F, 0);

            if (!level.noCollision(mob)) continue;

            // 1) finalizeSpawn
            mob.finalizeSpawn(
                    level,
                    level.getCurrentDifficultyAt(pos),
                    MobSpawnType.EVENT,
                    null
            );

            // 2) Баф HP ×2.5
            applyHealthBuff(mob);

            // 3) Додаємо в світ, а потім на наступному тіку видаємо зброю
            if (level.addFreshEntity(mob)) {
                var pName = player.getGameProfile().getName();
                RollMod.LOGGER.info("[NetherStorm] Spawned PACK wither_skeleton #{}/{} at {} {} {} near {}",
                        (++spawned), count, pos.getX(), pos.getY(), pos.getZ(), pName);

                // видати меч на наступному тіку — стабільніше за миттєве спорядження
                level.getServer().execute(() -> {
                    tryEquipWitherSkeleton(level, mob);          // меч (див. твій метод нижче)
                    mob.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
                    mob.setPersistenceRequired();
                });
            }
        }
    }

    @org.jetbrains.annotations.Nullable
    private static BlockPos findGroundedPosNearPlayer(ServerLevel level, ServerPlayer player, int radius, int tries) {
        for (int i = 0; i < tries; i++) {
            int dx = level.random.nextIntBetweenInclusive(-radius, radius);
            int dz = level.random.nextIntBetweenInclusive(-radius, radius);
            if (dx * dx + dz * dz < 9) continue; // не під самі ноги

            BlockPos guess = new BlockPos(player.getBlockX() + dx, player.getBlockY(), player.getBlockZ() + dz);
            BlockPos grounded = findGroundedPosFrom(level, guess, 12);
            if (grounded != null) return grounded;
        }
        return null;
    }

    /**
     * Від заданої точки шукає найближчу «землю під ногами»: спускаємось вниз, трохи перевіряємо вгору.
     */
    @org.jetbrains.annotations.Nullable
    private static BlockPos findGroundedPosFrom(ServerLevel level, BlockPos from, int verticalScan) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(from.getX(), from.getY(), from.getZ());

        // Спочатку спускаємося, поки знизу порожньо (нема колізії) або не досягли меж
        int minY = level.getMinBuildHeight() + 1;
        int steps = 0;
        while (pos.getY() > minY
                && level.getBlockState(pos.below()).getCollisionShape(level, pos.below()).isEmpty()
                && steps++ < verticalScan) {
            pos.move(0, -1, 0);
        }

        // Якщо так і не знайшли підлогу — спробуємо кілька кроків вгору (раптом зайшли у порожнину)
        if (level.getBlockState(pos.below()).getCollisionShape(level, pos.below()).isEmpty()) {
            for (int up = 0; up < 6; up++) {
                pos.move(0, 1, 0);
                if (!level.getBlockState(pos.below()).getCollisionShape(level, pos.below()).isEmpty()) break;
            }
        }

        // Умова валідності: є підлога + у самій клітинці порожньо
        if (level.getBlockState(pos.below()).getCollisionShape(level, pos.below()).isEmpty()) return null;
        if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) return null;

        return pos.immutable();
    }

    private static void applyHealthBuff(Mob mob) {
        var max = mob.getAttribute(Attributes.MAX_HEALTH);
        if (max != null) {
            max.setBaseValue(max.getBaseValue() * 2.5D); // +50%
            mob.setHealth(mob.getMaxHealth());
        }
    }

    /**
     * Пробує кілька разів знайти валідну позицію біля гравця і заспавнити моба з бафом HP.
     */
    //TODO перенести в більш розумний міксін, ідіот ти на роллі..
    //todo заміняти спавн звичайних мобів на візер скелетів під час шторму через міксін
    private static void spawnAroundPlayer(ServerLevel level, ServerPlayer player, EntityType<? extends Mob> type) {
        RandomSource rnd = level.random;
        final int RADIUS = 12;
        final int TRIES = 4;

        for (int i = 0; i < TRIES; i++) {
            int dx = rnd.nextIntBetweenInclusive(-RADIUS, RADIUS);
            int dz = rnd.nextIntBetweenInclusive(-RADIUS, RADIUS);
            if (dx * dx + dz * dz < 9) continue; // не під самі ноги

            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(
                    player.getBlockX() + dx,
                    player.getBlockY(),
                    player.getBlockZ() + dz
            );

            int minY = level.getMinBuildHeight() + 1;
            while (pos.getY() > minY &&
                    level.getBlockState(pos.below()).getCollisionShape(level, pos.below()).isEmpty()) {
                pos.move(0, -1, 0);
            }

            if (level.getBlockState(pos.below()).getCollisionShape(level, pos.below()).isEmpty()) continue;
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) continue;

            Mob mob = type.create(level);
            if (mob == null) continue;

            mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, rnd.nextFloat() * 360F, 0);

            if (!level.noCollision(mob)) continue;

            mob.finalizeSpawn(
                    level,
                    level.getCurrentDifficultyAt(pos),
                    MobSpawnType.EVENT,
                    null
            );


            tryEquipWitherSkeleton(level, mob);


            applyHealthBuff(mob);

            if (level.addFreshEntity(mob)) {
                // Логуємо спавн
                RollMod.LOGGER.info(
                        "[NetherStorm] Spawned {} at {} {} {} near {}",
                        type.toShortString(), pos.getX(), pos.getY(), pos.getZ(),
                        player.getGameProfile().getName()
                );

                // ✅ Додаємо зброю wither-скелету на наступному тіку,
                // щоб не конфліктувало з внутрішньою ініціалізацією обладнання/цілей
                level.getServer().execute(() -> {
                    tryEquipWitherSkeleton(level, mob);
                    mob.setPersistenceRequired();                // опціонально: не зникає випадково
                    mob.setDropChance(EquipmentSlot.MAINHAND, 0.0f); // не дропати меч
                    // Якщо захочеш, можна примусити агресію:
                    // if (mob instanceof net.minecraft.world.entity.monster.Monster mon) mon.setAggressive(true);
                });

                break;
            }

        }
    }

    private static void tryEquipWitherSkeleton(ServerLevel level, Mob mob) {
        if (mob.getType() != EntityType.WITHER_SKELETON) return;

        // Меч у руку
        mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_SWORD));
        mob.setDropChance(EquipmentSlot.MAINHAND, 0.0f);

        // Повний сет броні
        mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
        mob.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
        mob.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS));
        mob.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.NETHERITE_BOOTS));

        mob.setDropChance(EquipmentSlot.HEAD, 0.0f);
        mob.setDropChance(EquipmentSlot.CHEST, 0.0f);
        mob.setDropChance(EquipmentSlot.LEGS, 0.0f);
        mob.setDropChance(EquipmentSlot.FEET, 0.0f);

        // Позначимо, що цей моб — «штормовий», щоб потім перевіряти в івентах/дропі
        mob.getPersistentData().putBoolean("roll_mod:storm_spawn", true);
    }

    private static void tryGrowSulfurBerries(ServerLevel nether) {
        RandomSource random = nether.random;

        // Скільки спроб за тік
        for (int i = 0; i < 10; i++) {
            // випадкові координати поблизу гравця
            ServerPlayer player = nether.getRandomPlayer();
            if (player == null) return;

            int x = player.getBlockX() + random.nextIntBetweenInclusive(-16, 16);
            int z = player.getBlockZ() + random.nextIntBetweenInclusive(-16, 16);
            int y = player.getBlockY() + random.nextIntBetweenInclusive(-4, 4);

            BlockPos pos = new BlockPos(x, y, z);
            BlockPos above = pos.above();

            // перевірка: пісок душ або ґрунт душ
            var state = nether.getBlockState(pos);
            if ((state.is(Blocks.SOUL_SAND) || state.is(Blocks.SOUL_SOIL))
                    && nether.isEmptyBlock(above)) {

                var berryBlock = BlockRegistry.SULFUR_BERRY_BLOCK.get().defaultBlockState();

                // якщо має властивість age — виставляємо 0
                if (berryBlock.hasProperty(BlockStateProperties.AGE_7)) {
                    berryBlock = berryBlock.setValue(BlockStateProperties.AGE_7, 0);
                }

                nether.setBlock(above, berryBlock, 3);
                RollMod.LOGGER.info("[NetherStorm] Grew sulfur berry at {}, {}, {}", x, y + 1, z);
            }
        }
    }
}
