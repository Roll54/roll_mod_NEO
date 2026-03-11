package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.data.RMMComponents;
import com.roll_54.roll_mod.registry.TagRegistry;
import com.roll_54.roll_mod.items.spaceModule.CartridgeData;
import com.roll_54.roll_mod.screen.menu.RocketControllerMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import javax.annotation.Nullable;

public record PacketLaunchRocket(BlockPos pos) implements CustomPacketPayload {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Type<PacketLaunchRocket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "launch_rocket"));
    public static final StreamCodec<ByteBuf, PacketLaunchRocket> STREAM_CODEC =
            BlockPos.STREAM_CODEC.map(PacketLaunchRocket::new, PacketLaunchRocket::pos);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketLaunchRocket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                if (player.containerMenu instanceof RocketControllerMenu menu) {
                    Slot rocketSlot = menu.getSlot(36); // TE slots start after player inv (36)
                    Slot fuelSlot = menu.getSlot(37);
                    Slot cartridgeSlot = menu.getSlot(38);

                    ItemStack rocket = rocketSlot.getItem();
                    ItemStack fuel = fuelSlot.getItem();
                    ItemStack cartridge = cartridgeSlot.getItem();

                    if (rocket.isEmpty() || fuel.isEmpty() || cartridge.isEmpty()) return;
                    if (!rocket.is(TagRegistry.ROCKET_ITEM)) return;
                    if (!fuel.is(TagRegistry.ROCKET_FUEL)) return;

                    int tier = rocket.getOrDefault(RMMComponents.ROCKET_TIER.get(), 1);

                    CartridgeData cartridgeData = cartridge.get(RMMComponents.CARTRIDGE_DATA.get());
                    if (cartridgeData == null) return;
                    if (tier < cartridgeData.requiredTier()) return;

                    int requiredFuel = 5 * tier;
                    if (fuel.getCount() < requiredFuel) return;

                    ResourceKey<Level> targetDim = null;
                    ResourceLocation dimId = ResourceLocation.tryParse(cartridgeData.dimensionRaw());
                    if (dimId != null) {
                        targetDim = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, dimId);
                    }

                    if (targetDim != null) {
                        ServerLevel targetLevel = player.server.getLevel(targetDim);
                        if (targetLevel != null) {
                            int originX = payload.pos().getX();
                            int originZ = payload.pos().getZ();

                            BlockPos safePos = findSafeLanding(targetLevel, targetDim, originX, originZ, 20);
                            if (safePos == null) {
                                player.sendSystemMessage(Component.translatable("message.roll_mod.no_safe_landing"));
                                player.closeContainer();
                                return;
                            }

                            DimensionTransition transition = createRocketTransition(targetLevel, safePos, player);

                            fuel.shrink(requiredFuel);
                            player.changeDimension(transition);
                        }
                    }
                }
            }
        });
    }

    private static DimensionTransition createRocketTransition(ServerLevel level, BlockPos standPos, Entity entity) {
        EntityDimensions dimensions = entity.getDimensions(entity.getPose());
        Vec3 base = new Vec3(standPos.getX() + 0.5D, standPos.getY(), standPos.getZ() + 0.5D);
        Vec3 safe = PortalShape.findCollisionFreePosition(base, level, entity, dimensions);

        LOGGER.info("[RocketLaunch] Collision-safe landing adjusted to ({}, {}, {})",
                safe.x, safe.y, safe.z);

        return new DimensionTransition(
                level,
                safe,
                entity.getDeltaMovement(),
                entity.getYRot(),
                entity.getXRot(),
                DimensionTransition.DO_NOTHING
        );
    }

    /**
     * Searches a square spiral of radius {@code radius} around (originX, originZ) for a
     * safe two-block-high air column sitting on a solid block.
     *
     * @return the Y-level of the floor block (player stands one above), or {@code null} if nothing was found.
     */
    @Nullable
    private static BlockPos findSafeLanding(ServerLevel level, ResourceKey<Level> dim,
                                            int originX, int originZ, int radius) {
        LOGGER.info("[RocketLaunch] Searching for safe landing in dimension '{}' around ({}, {}) with radius {}",
                dim.location(), originX, originZ, radius);

        for (int r = 0; r <= radius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    // Only check the perimeter ring for r > 0
                    if (r > 0 && Math.abs(dx) != r && Math.abs(dz) != r) continue;

                    int cx = originX + dx;
                    int cz = originZ + dz;

                    LOGGER.debug("[RocketLaunch] Trying column ({}, {})", cx, cz);

                    BlockPos candidate = tryFindColumn(level, cx, cz);
                    if (candidate != null) {
                        LOGGER.info("[RocketLaunch] Safe landing found at ({}, {}, {})", candidate.getX(), candidate.getY(), candidate.getZ());
                        return candidate;
                    }
                }
            }
        }

        LOGGER.warn("[RocketLaunch] No safe landing found within radius {} around ({}, {}) in '{}'",
                radius, originX, originZ, dim.location());
        return null;
    }

    @Nullable
    private static BlockPos tryFindColumn(ServerLevel level, int x, int z) {
        for (int y = level.getMaxBuildHeight() - 2; y > level.getMinBuildHeight(); y--) {
            BlockPos floor = new BlockPos(x, y, z);
            BlockPos stand = floor.above();
            BlockPos head = stand.above();

            boolean floorSolid = level.getBlockState(floor).isFaceSturdy(level, floor, net.minecraft.core.Direction.UP);
            boolean standAir = level.getBlockState(stand).isAir();
            boolean headAir = level.getBlockState(head).isAir();

            LOGGER.debug("[RocketLaunch]   Full scan ({}, {}, {}) floorSolid={}, standAir={}, headAir={}",
                    x, y, z, floorSolid, standAir, headAir);

            if (floorSolid && standAir && headAir) {
                if (level.dimension() == Level.NETHER && !isSafeNetherLanding(level, stand)) {
                    LOGGER.debug("[RocketLaunch]   Rejected Nether landing at ({}, {}, {}) due to roof/ceiling constraints",
                            stand.getX(), stand.getY(), stand.getZ());
                    continue;
                }
                return stand;
            }
        }

        return null;
    }

    private static boolean isSafeNetherLanding(ServerLevel level, BlockPos standPos) {
        BlockPos floorPos = standPos.below();

        // Nether bedrock roof sits around y=127, so reject high roof-band floors directly.
        // Using getMaxBuildHeight() here is wrong because build height is 320+ in modern versions.
        int roofFloorMinY = 123;
        if (floorPos.getY() >= roofFloorMinY) {
            LOGGER.debug("[RocketLaunch]   Nether landing rejected: floor is in roof band at y={}", floorPos.getY());
            return false;
        }

        int ceilingCheck = 8;
        for (int i = 0; i <= ceilingCheck; i++) {
            BlockPos checkPos = standPos.above(i);
            if (!level.getBlockState(checkPos).isAir()) {
                LOGGER.debug("[RocketLaunch]   Nether landing rejected: blocked above at ({}, {}, {})",
                        checkPos.getX(), checkPos.getY(), checkPos.getZ());
                return false;
            }
        }

        return true;
    }
}
