package com.roll_54.roll_mod.modItems;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class ProspectorPickItem extends PickaxeItem {

    public ProspectorPickItem(Tier tier, Properties properties) {
        super(tier, properties.stacksTo(1));
    }

    private static final TagKey<Block> C_ORES = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "ores"));

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        HitResult hit = player.pick(
                player.blockInteractionRange(),
                0.0F,
                false
        );

        if (level.isClientSide
                || player.isShiftKeyDown()
                || hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }


        scanForOres(level, player);
        stack.hurtAndBreak(
                1,
                player,
                player.getEquipmentSlotForItem(stack));

        player.getCooldowns().addCooldown(this, 10 * 10);
        player.swing(hand, true);

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    private void scanForOres(Level level, Player player) {

        HitResult hitResult = player.pick(
                player.blockInteractionRange(),
                0.0F,
                false
        );

        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockHitResult hit = (BlockHitResult) hitResult;


        BlockPos start = hit.getBlockPos();
        Direction face = hit.getDirection().getOpposite();
        Vec3 dir = Vec3.atLowerCornerOf(face.getNormal()).normalize();

        int radius = 2; // 5x5
        int depth = 90;

        Set<Block> foundOres = new HashSet<>();

        for (int d = 1; d <= depth; d++) {

            BlockPos center = start.offset(
                    (int) (dir.x * d),
                    (int) (dir.y * d),
                    (int) (dir.z * d)
            );

            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {

                        BlockPos pos = center.offset(x, y, z);
                        BlockState state = level.getBlockState(pos);

                        if (!state.is(C_ORES)) continue;

                        Block block = state.getBlock();

                        if (foundOres.add(block)) {
                            player.sendSystemMessage(
                                    Component.translatable(
                                            "message.roll_mod.prospector.found",
                                            block.getName().copy().withStyle(ChatFormatting.GRAY)
                                    ).withStyle(ChatFormatting.GREEN)
                            );
                        }
                    }
                }
            }
        }

        if (foundOres.isEmpty()) {
            player.sendSystemMessage(
                    Component.translatable("message.roll_mod.prospector.none")
                            .withStyle(ChatFormatting.RED)
            );
        }

    }


}
