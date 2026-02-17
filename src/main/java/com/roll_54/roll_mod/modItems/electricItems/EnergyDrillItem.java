package com.roll_54.roll_mod.modItems.electricItems;

import aztech.modern_industrialization.MIComponents;
import com.roll_54.roll_mod.data.RMMComponents;
import com.roll_54.roll_mod.util.EnergyFormatUtils;
import dev.technici4n.grandpower.api.ISimpleEnergyItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;

public class EnergyDrillItem extends DiggerItem implements ISimpleEnergyItem {

    private final long PER_BLOCK_BREAK_COST;
    private final long ITEM_CAPACITY;
    private final int MINING_RADIUS;
    private final int DEPTH;

    public EnergyDrillItem(Tier tier, Properties properties, long item_capacity, int depth, int mining_radius, int block_cost ) {

        super(tier, BlockTags.MINEABLE_WITH_PICKAXE, properties.stacksTo(1));

        this.ITEM_CAPACITY = item_capacity;
        this.MINING_RADIUS = mining_radius;
        this.DEPTH = depth;
        this.PER_BLOCK_BREAK_COST = block_cost;
    }

    @SuppressWarnings("@ParametersAreNonnullByDefaul parametr")
    @Override
    public DataComponentType<Long> getEnergyComponent() {
        return MIComponents.ENERGY.get();
    }

    @Override
    public long getEnergyCapacity(ItemStack stack) {
        return ITEM_CAPACITY;
    }

    @Override
    public long getEnergyMaxInput(ItemStack stack) {
        return ITEM_CAPACITY;
    }

    @Override
    public long getEnergyMaxOutput(ItemStack stack) {
        return 0;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x30e996;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        double frac = (double) getStoredEnergy(stack) / getEnergyCapacity(stack);
        return (int) (13 * frac);
    }

    @Override
    public void appendHoverText(ItemStack stack,
                                Item.TooltipContext ctx,
                                List<Component> tooltip,
                                TooltipFlag flag) {

        long stored = getStoredEnergy(stack);
        long cap = getEnergyCapacity(stack);
        int diameter = MINING_RADIUS * 2 + 1;

        String formattedStored = EnergyFormatUtils.formatEnergy(stored);
        String formattedCap = EnergyFormatUtils.formatEnergy(cap);
        String formattedCost = EnergyFormatUtils.formatEnergyWithUnit(PER_BLOCK_BREAK_COST);

        // Енергія
        tooltip.add(
                Component.translatable("tooltip.roll_mod.energy", formattedStored, formattedCap)
                        .withStyle(ChatFormatting.AQUA)
        );

        if (Screen.hasShiftDown()) {

            if (stack.getOrDefault(RMMComponents.TRANS.get(), 0.0F) >= 1.0F) {
                tooltip.add(
                        Component.translatable("tooltip.roll_mod.mining_drill.trans")
                                .withStyle(ChatFormatting.LIGHT_PURPLE).withStyle(ChatFormatting.ITALIC)
                );
            }

            if (stack.getOrDefault(RMMComponents.CAT.get(), 0.0F) >= 1.0F) {
                tooltip.add(
                        Component.translatable("tooltip.roll_mod.mining_drill.cat")
                                .withStyle(ChatFormatting.LIGHT_PURPLE).withStyle(ChatFormatting.ITALIC)
                );
            }

            // 1
            tooltip.add(
                    Component.translatable("tooltip.roll_mod.mining_drill.arc")
                            .withStyle(ChatFormatting.WHITE)
            );

            // 2 (з параметрами)
            tooltip.add(
                    Component.translatable(
                            "tooltip.roll_mod.mining_drill.area",
                            Component.literal(String.valueOf(diameter)).withStyle(ChatFormatting.GOLD),
                            Component.literal(String.valueOf(diameter)).withStyle(ChatFormatting.GOLD),
                            Component.literal(String.valueOf(DEPTH)).withStyle(ChatFormatting.GOLD)
                    ).withStyle(ChatFormatting.WHITE)
            );

            // 3 cost
            tooltip.add(
                    Component.translatable(
                            "tooltip.roll_mod.mining_drill.cost",
                            Component.literal(formattedCost).withStyle(ChatFormatting.AQUA)
                    ).withStyle(ChatFormatting.WHITE)
            );

            // 4 (lore / flavor)
            tooltip.add(
                    Component.translatable("tooltip.roll_mod.mining_drill.trademark",
                                    Component.literal("Minestar").withStyle(ChatFormatting.LIGHT_PURPLE))
                            .withStyle(ChatFormatting.WHITE)
            );

        } else {
            tooltip.add(
                    Component.translatable("tooltip.roll_mod.general_press_shift")
                            .withStyle(ChatFormatting.GRAY)
            );
        }
    }

    @Override
    public boolean mineBlock(ItemStack stack,
                             Level level,
                             BlockState state,
                             BlockPos pos,
                             LivingEntity miner) {

        if (level.isClientSide) return true;
        if (!(miner instanceof ServerPlayer player)) return true;

        // повітря, нульова твердість — не чіпаємо
        if (state.isAir() || state.getDestroySpeed(level, pos) <= 0) {
            return true;
        }

        long energyPerBlock = PER_BLOCK_BREAK_COST;

        List<BlockPos> targets = collectArea(player, pos, MINING_RADIUS);

        for (BlockPos targetPos : targets) {
            BlockState targetState = level.getBlockState(targetPos);

            if (targetState.isAir()) continue;
            if (targetState.getDestroySpeed(level, targetPos) <= 0) continue;
            if (level.getBlockEntity(targetPos) != null) continue;

            if (getStoredEnergy(stack) < energyPerBlock) {
                break;
            }

            // стандартний Forge/NeoForge шлях
            var event = net.neoforged.neoforge.common.CommonHooks.fireBlockBreak(
                    level,
                    player.gameMode.getGameModeForPlayer(),
                    player,
                    targetPos,
                    targetState
            );

            if (event.isCanceled()) continue;

            if (targetState.getBlock()
                    .onDestroyedByPlayer(targetState, level, targetPos, player, true, targetState.getFluidState())) {

                targetState.getBlock().destroy(level, targetPos, targetState);
                Block.dropResources(targetState, level, targetPos, null, player, stack);
                tryUseEnergy(stack, energyPerBlock);
            }
        }

        return true;
    }

    private List<BlockPos> collectArea(ServerPlayer player,
                                       BlockPos originalCenter,
                                       int radius) {

        List<BlockPos> result = new ArrayList<>();

        BlockHitResult hit = player.level().clip(
                new ClipContext(
                        player.getEyePosition(1f),
                        player.getEyePosition(1f).add(player.getViewVector(1f).scale(6)),
                        ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE,
                        player
                )
        );

        if (hit.getType() != HitResult.Type.BLOCK || radius == 0) {
            result.add(originalCenter);
            return result;
        }

        Direction face = hit.getDirection();
        BlockPos center = originalCenter;

        // Move center
        if (face.getAxis() != Direction.Axis.Y) {
            int diameter = radius * 2 + 1;
            int yOffset = (diameter - 3) / 2;
            center = center.above(yOffset);
        }

        for (int d = 0; d < DEPTH; d++) {
            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {

                    BlockPos pos;

                    if (face == Direction.UP || face == Direction.DOWN) {
                        // копаємо ВНИЗ / ВГОРУ (у світ)
                        pos = center.offset(x, -d * face.getStepY(), y);

                    } else if (face == Direction.NORTH || face == Direction.SOUTH) {
                        // копаємо В СТІНУ по Z
                        pos = center.offset(x, y, -d * face.getStepZ());

                    } else { // EAST / WEST
                        // копаємо В СТІНУ по X
                        pos = center.offset(-d * face.getStepX(), y, x);
                    }

                    result.add(pos);
                }
            }
        }

        return result;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (player == null) return InteractionResult.PASS;

        // CAKE → CAT
        if (handleCake(level, pos, state, stack)) {
            return InteractionResult.CONSUME;
        }

        // Shift + BEACON → TRANS
        if (handleBeacon(level, pos, state, player, stack)) {
            return InteractionResult.CONSUME;
        }

        return super.useOn(context);
    }

    private boolean handleCake(Level level,
                               BlockPos pos,
                               BlockState state,
                               ItemStack stack) {

        if (!state.is(Blocks.CAKE)) return false;

        level.destroyBlock(pos, false);
        stack.set(RMMComponents.CAT.get(), 1.0F);
        stack.set(RMMComponents.TRANS.get(), 0.0F);

        level.playSound(
                null,
                pos,
                SoundEvents.CAT_AMBIENT,
                SoundSource.BLOCKS,
                1.0F,
                1.0F
        );

        return true;
    }

    private boolean handleBeacon(Level level,
                                 BlockPos pos,
                                 BlockState state,
                                 Player player,
                                 ItemStack stack) {

        if (!player.isShiftKeyDown()) return false;
        if (!state.is(Blocks.BEACON)) return false;

        level.destroyBlock(pos, false);
        stack.set(RMMComponents.TRANS.get(), 1.0F);
        stack.set(RMMComponents.CAT.get(), 0.0F);

        level.playSound(
                null,
                pos,
                SoundEvents.BEACON_ACTIVATE,
                SoundSource.BLOCKS,
                1.0F,
                1.0F
        );

        return true;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        if (oldStack.getItem() != newStack.getItem()) {
            return true;
        }
        if (slotChanged) {
            return true;
        }
        return false;
    }
}
