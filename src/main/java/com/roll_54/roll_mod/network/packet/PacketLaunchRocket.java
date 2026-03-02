package com.roll_54.roll_mod.network.packet;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.init.ItemRegistry;
import com.roll_54.roll_mod.init.TagRegistry;
import com.roll_54.roll_mod.screen.menu.RocketControllerMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PacketLaunchRocket(BlockPos pos) implements CustomPacketPayload {
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
                    Slot rocketSlot = menu.getSlot(36 + 0); // TE slots start after player inv (36)
                    Slot fuelSlot = menu.getSlot(36 + 1);
                    Slot cartridgeSlot = menu.getSlot(36 + 2);

                    ItemStack rocket = rocketSlot.getItem();
                    ItemStack fuel = fuelSlot.getItem();
                    ItemStack cartridge = cartridgeSlot.getItem();

                    if (rocket.isEmpty() || fuel.isEmpty() || cartridge.isEmpty()) return;
                    if (!rocket.is(TagRegistry.ROCKET_ITEM)) return;
                    if (!fuel.is(TagRegistry.ROCKET_FUEL)) return;

                    int tier = 1;
                    CustomData customData = rocket.get(DataComponents.CUSTOM_DATA);
                    if (customData != null) {
                        CompoundTag tag = customData.copyTag();
                        if (tag.contains("rocket_tier")) {
                            tier = tag.getInt("rocket_tier");
                        }
                    }

                    int requiredFuel = 5 * tier;
                    if (fuel.getCount() < requiredFuel) return;

                    ResourceKey<Level> targetDim = null;
                    if (cartridge.getItem() == ItemRegistry.NETHER_CARTRIDGE.get()) {
                        targetDim = Level.NETHER;
                    } else if (cartridge.getItem() == ItemRegistry.END_CARTRIDGE.get()) {
                        targetDim = Level.END;
                    }

                    if (targetDim != null) {
                        ServerLevel targetLevel = player.server.getLevel(targetDim);
                        if (targetLevel != null) {
                            fuel.shrink(requiredFuel);

                            // Teleport logic
                            BlockPos spawnPos = targetLevel.getSharedSpawnPos();
                            // Try to find a safe surface spot near spawn or 0,0
                            int x = spawnPos.getX();
                            int z = spawnPos.getZ();
                            int y = targetLevel.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

                            // Simple safety check: if y < min build height, default to something safe or search
                            if (y <= targetLevel.getMinBuildHeight()) {
                                y = 64; // Fallback
                                if (targetDim == Level.NETHER) y = 32; // In nether top is bedrock
                            }

                            // For Nether specifically, MOTION_BLOCKING gives bedrock ceiling usually.
                            // We need to find air pockets.
                            // For MVP simplicity, let's just teleport to spawnPos if valid, or scan down from top if Overworld/End.
                            // For Nether, finding a safe spot is complex. I'll use a simple heuristic: Teleport to 0, 80, 0 if safe, or find nearest air.

                            if (targetDim == Level.NETHER) {
                                // Find valid ground
                                for (int i = 50; i < 100; i++) {
                                    BlockPos p = new BlockPos(x, i, z);
                                    if (targetLevel.getBlockState(p).isAir() && targetLevel.getBlockState(p.above()).isAir() && !targetLevel.getBlockState(p.below()).isAir()) {
                                        y = i;
                                        break;
                                    }
                                }
                            }

                            player.teleportTo(targetLevel, x + 0.5, y, z + 0.5, player.getYRot(), player.getXRot());
                        }
                    }
                }
            }
        });
    }
}
