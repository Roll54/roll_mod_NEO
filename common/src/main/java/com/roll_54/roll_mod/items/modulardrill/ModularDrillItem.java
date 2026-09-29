package com.roll_54.roll_mod.items.modulardrill;

import aztech.modern_industrialization.MIComponents;
import com.roll_54.roll_mod.items.electricItems.DrillDropCollector;
import com.roll_54.roll_mod.util.EnergyFormatUtils;
import dev.technici4n.grandpower.api.ISimpleEnergyItem;
import com.roll_54.roll_mod.items.ModToolTiers;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The modular mining drill: a bare single-block tool whose area, loot, speed and energy behavior
 * all come from the modules installed at the Module Installation Table.
 *
 * <p>The mining pipeline is the old {@code EnergyDrillItem} one — raytrace for the mined face, map
 * a square onto it, break through {@code fireBlockBreak}, collect the drops — but every fixed
 * number is now a question to {@link DrillModuleHelper}. Not enchantable by design: Fortune and
 * Silk Touch are modules, applied to the loot through a throwaway enchanted copy of the drill.
 */
public class ModularDrillItem extends DiggerItem implements ISimpleEnergyItem, ModularTool {

    /** EU gained per point of experience captured by the XP Reactor module. */
    public static final long EU_PER_XP = 100L;

    private final DrillVoltage voltage;

    public ModularDrillItem(DrillVoltage voltage, Properties properties) {
        super(ModToolTiers.METEORITE_METAL, BlockTags.MINEABLE_WITH_PICKAXE, properties.stacksTo(1));
        this.voltage = voltage;
    }

    public DrillVoltage voltage() {
        return voltage;
    }

    @Override
    public int moduleSlots() {
        return voltage.slots;
    }

    @Override
    public ToolKind toolKind() {
        return ToolKind.DRILL;
    }

    @Override
    public int maxComplexity() {
        return voltage.complexity;
    }

    /** A placeholder name while the client hides new items — see {@link NewItemVisibility}. */
    @Override
    public Component getName(ItemStack stack) {
        return NewItemVisibility.name(super.getName(stack));
    }

    /* -------------------------------------------- energy -------------------------------------------- */

    @Override
    public DataComponentType<Long> getEnergyComponent() {
        return MIComponents.ENERGY.get();
    }

    @Override
    public long getEnergyCapacity(ItemStack stack) {
        return (long) (voltage.baseCapacity * DrillModuleHelper.capacityFactor(stack));
    }

    /** Zero with an XP Reactor installed — mined experience becomes the only way in. */
    @Override
    public long getEnergyMaxInput(ItemStack stack) {
        return DrillModuleHelper.hasXpReactor(stack) ? 0L : getEnergyCapacity(stack);
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

    /* -------------------------------------------- speed --------------------------------------------- */

    /**
     * Tier speed × Speed module × throttle. The guard keeps the multipliers off the two fixed
     * values the parent returns for wrong-tool blocks; a drill without the energy for one block
     * digs like a bare hand instead of eating half a block's worth of progress for free.
     */
    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        float speed = super.getDestroySpeed(stack, state);
        if (!isCorrectToolForDrops(stack, state)) {
            return speed;
        }
        if (getStoredEnergy(stack) < DrillModuleHelper.costPerBlock(stack, voltage)) {
            return 1.0F;
        }
        return speed
                * (float) DrillModuleHelper.speedFactor(stack)
                * (DrillModuleHelper.get(stack).throttle() / 100.0F);
    }

    /* -------------------------------------------- mining -------------------------------------------- */

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        if (level.isClientSide) return true;
        if (!(miner instanceof ServerPlayer player)) return true;
        if (state.isAir() || state.getDestroySpeed(level, pos) <= 0) return true;

        List<BlockPos> targets = collectArea(player, pos, DrillModuleHelper.volume(stack));

        DrillModules modules = DrillModuleHelper.get(stack);
        var op = new DrillDropCollector.DrillOp();
        op.burnDrops = modules.has(ModuleType.BURN);
        op.trashFilter = DrillModuleHelper.trashFilter(stack);
        op.smeltDrops = modules.has(ModuleType.SMELT);
        // Always captured: mined experience never spills as orbs. The XP Reactor turns it into
        // energy; without one it goes straight to the player.
        op.captureXp = true;

        DrillDropCollector.collect(player, op, () -> mineArea(stack, level, player, targets));

        if (op.xpCollected > 0) {
            if (DrillModuleHelper.hasXpReactor(stack)) {
                absorbXp(stack, player, op.xpCollected);
            } else {
                player.giveExperiencePoints(op.xpCollected);
            }
        }
        return true;
    }

    private void mineArea(ItemStack stack, Level level, ServerPlayer player, List<BlockPos> targets) {
        long costPerBlock = DrillModuleHelper.costPerBlock(stack, voltage);
        ItemStack lootTool = lootTool(stack, level);

        Set<BlockPos> solidified = DrillModuleHelper.get(stack).has(ModuleType.LAVA_SOLIDIFIER)
                ? solidifyFluids(stack, level, targets, costPerBlock)
                : Set.of();

        int broken = 0;
        for (BlockPos targetPos : targets) {
            // A block this very operation set where fluid was is the tunnel's crust — mining it
            // straight back out would reopen the lava it just sealed.
            if (solidified.contains(targetPos)) continue;

            BlockState targetState = level.getBlockState(targetPos);

            if (targetState.isAir()) continue;
            if (targetState.getDestroySpeed(level, targetPos) <= 0) continue;
            if (level.getBlockEntity(targetPos) != null) continue;

            if (getStoredEnergy(stack) < costPerBlock) {
                break;
            }

            var event = net.neoforged.neoforge.common.CommonHooks.fireBlockBreak(
                    level, player.gameMode.getGameModeForPlayer(), player, targetPos, targetState);
            if (event.isCanceled()) continue;

            if (targetState.getBlock()
                    .onDestroyedByPlayer(targetState, level, targetPos, player, true, targetState.getFluidState())) {
                targetState.getBlock().destroy(level, targetPos, targetState);
                Block.dropResources(targetState, level, targetPos, null, player, lootTool);
                tryUseEnergy(stack, costPerBlock);
                broken++;
            }
        }

        DrillModuleHelper.countSpeakBlocks(stack, player, broken);
    }

    /**
     * The Lava Solidifier's pass over the volume, before anything breaks: lava sources set into
     * obsidian, flowing lava into cobblestone, water simply removed — so a deep dig does not
     * flood or burn the tunnel it just opened. Every conversion is paid for like a mined block,
     * and the pass stops when the energy does.
     *
     * @return where solid blocks were placed, so the break loop leaves the fresh crust standing.
     */
    private Set<BlockPos> solidifyFluids(ItemStack stack, Level level, List<BlockPos> targets, long costPerBlock) {
        Set<BlockPos> solidified = new HashSet<>();
        for (BlockPos pos : targets) {
            BlockState state = level.getBlockState(pos);
            FluidState fluid = state.getFluidState();
            if (fluid.isEmpty()) continue;
            if (getStoredEnergy(stack) < costPerBlock) return solidified;

            BlockState replacement;
            if (fluid.is(FluidTags.LAVA)) {
                replacement = fluid.isSource()
                        ? Blocks.OBSIDIAN.defaultBlockState()
                        : Blocks.COBBLESTONE.defaultBlockState();
            } else if (fluid.is(FluidTags.WATER)) {
                replacement = Blocks.AIR.defaultBlockState();
            } else {
                continue;
            }

            // Only pure fluid blocks are replaced; a waterlogged block keeps its block half and
            // is handled by the ordinary break loop.
            if (state.canBeReplaced()) {
                level.setBlockAndUpdate(pos, replacement);
                if (!replacement.isAir()) {
                    solidified.add(pos);
                }
                tryUseEnergy(stack, costPerBlock);
            }
        }
        return solidified;
    }

    /**
     * The tool the loot tables see. Fortune and Silk Touch live on a throwaway copy, never on the
     * drill itself, so the drill stays visibly unenchanted while the vanilla {@code match_tool}
     * and {@code apply_bonus} conditions read the levels they expect.
     */
    private ItemStack lootTool(ItemStack stack, Level level) {
        DrillModules modules = DrillModuleHelper.get(stack);

        DrillModuleItem fortune = modules.ofType(ModuleType.FORTUNE);
        DrillModuleItem silk = modules.ofType(ModuleType.SILK_TOUCH);
        if (fortune == null && silk == null) {
            return stack;
        }

        ItemStack copy = stack.copy();
        if (silk != null) {
            copy.enchant(enchantment(level, Enchantments.SILK_TOUCH), 1);
        } else {
            copy.enchant(enchantment(level, Enchantments.FORTUNE), fortune.tier);
        }
        return copy;
    }

    private static Holder<Enchantment> enchantment(Level level, ResourceKey<Enchantment> key) {
        return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    /** The XP Reactor's bookkeeping — see {@link DrillModuleHelper#absorbEnergy}. */
    private void absorbXp(ItemStack stack, ServerPlayer player, int xp) {
        DrillModuleHelper.absorbEnergy(stack, this, player, xp * EU_PER_XP);
    }

    /**
     * The mined box: the configured X×Y×Z volume mapped onto the face the player is looking at.
     * X is the lateral width, Y the height, Z the depth along the face's normal — the same
     * orientation the old square drill used, generalized to three independent extents. The clip
     * length follows the player's block interaction range so the Reach module extends the
     * targeting along with the reach itself.
     *
     * <p>Sizes are always odd ({@link DrillModuleHelper#clampAxis}), so lateral spans center
     * exactly on the target. The vertical span on a wall keeps the old drills' feel: the bottom
     * row lands one below the targeted block (your feet), the rest stack upward — which is
     * exactly the span the old 3×3 and 5×5 produced.
     */
    private List<BlockPos> collectArea(ServerPlayer player, BlockPos originalCenter,
                                       DrillModuleHelper.Volume volume) {
        List<BlockPos> result = new ArrayList<>();

        if (volume.single()) {
            result.add(originalCenter);
            return result;
        }

        BlockHitResult hit = player.level().clip(new ClipContext(
                player.getEyePosition(1f),
                player.getEyePosition(1f).add(player.getViewVector(1f).scale(player.blockInteractionRange())),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player));

        if (hit.getType() != HitResult.Type.BLOCK) {
            result.add(originalCenter);
            return result;
        }

        Direction face = hit.getDirection();

        // Centered spans; sizes are odd, so -(n-1)/2 .. n/2 is symmetric.
        int fromX = -(volume.x() - 1) / 2;
        int toX = volume.x() / 2;
        // Wall mining: height 1 is the target row; more stacks up from one below your feet.
        int fromY = volume.y() == 1 ? 0 : -1;
        int toY = volume.y() == 1 ? 0 : volume.y() - 2;
        // On a floor or ceiling there is no "up" to anchor to, so Y centers like X does.
        int fromFlatY = -(volume.y() - 1) / 2;
        int toFlatY = volume.y() / 2;

        for (int d = 0; d < volume.z(); d++) {
            for (int x = fromX; x <= toX; x++) {
                if (face == Direction.UP || face == Direction.DOWN) {
                    for (int y = fromFlatY; y <= toFlatY; y++) {
                        result.add(originalCenter.offset(x, -d * face.getStepY(), y));
                    }
                } else if (face == Direction.NORTH || face == Direction.SOUTH) {
                    for (int y = fromY; y <= toY; y++) {
                        result.add(originalCenter.offset(x, y, -d * face.getStepZ()));
                    }
                } else { // EAST / WEST
                    for (int y = fromY; y <= toY; y++) {
                        result.add(originalCenter.offset(-d * face.getStepX(), y, x));
                    }
                }
            }
        }
        return result;
    }

    /* ------------------------------------------ enchanting ------------------------------------------ */

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return false;
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return false;
    }

    /* ------------------------------------------ item churn ------------------------------------------ */

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return oldStack.getItem() != newStack.getItem() || slotChanged;
    }

    /**
     * Energy and throttle both live in components that change mid-dig (XP charging, Ctrl+scroll),
     * and the NeoForge default would restart block breaking on every such change.
     */
    @Override
    public boolean shouldCauseBlockBreakReset(ItemStack oldStack, ItemStack newStack) {
        return oldStack.getItem() != newStack.getItem();
    }

    /* -------------------------------------------- tooltip ------------------------------------------- */

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        long stored = getStoredEnergy(stack);
        long cap = getEnergyCapacity(stack);
        tooltip.add(Component.translatable("tooltip.roll_mod.energy",
                        EnergyFormatUtils.formatEnergy(stored), EnergyFormatUtils.formatEnergy(cap))
                .withStyle(ChatFormatting.AQUA));

        DrillModules modules = DrillModuleHelper.get(stack);
        if (!Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("tooltip.roll_mod.general_press_shift")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }

        int throttle = modules.throttle();
        tooltip.add(Component.translatable("tooltip.roll_mod.modular_drill.throttle", throttle)
                .withStyle(throttle > 100 ? ChatFormatting.YELLOW : ChatFormatting.WHITE));

        DrillModuleHelper.Volume volume = DrillModuleHelper.volume(stack);
        tooltip.add(Component.translatable("tooltip.roll_mod.modular_drill.area",
                        volume.x(), volume.y(), volume.z())
                .withStyle(ChatFormatting.WHITE));

        tooltip.add(Component.translatable("tooltip.roll_mod.modular_drill.cost",
                        EnergyFormatUtils.formatEnergyWithUnit(DrillModuleHelper.costPerBlock(stack, voltage)))
                .withStyle(ChatFormatting.AQUA));

        tooltip.add(Component.translatable("tooltip.roll_mod.modular_drill.slots",
                        modules.modules().size(), voltage.slots)
                .withStyle(ChatFormatting.WHITE));
        int complexity = DrillModuleHelper.complexity(stack);
        tooltip.add(Component.translatable("tooltip.roll_mod.modular_tool.complexity",
                        complexity, maxComplexity())
                .withStyle(complexity >= maxComplexity() ? ChatFormatting.RED : ChatFormatting.WHITE));
        for (DrillModuleItem module : modules.moduleItems()) {
            tooltip.add(Component.literal("  • ")
                    .append(module.getDescription())
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }

        if (DrillModuleHelper.hasXpReactor(stack)) {
            tooltip.add(Component.translatable("tooltip.roll_mod.modular_drill.xp_locked")
                    .withStyle(ChatFormatting.GOLD));
        }

        tooltip.add(Component.translatable("tooltip.roll_mod.modular_drill.install_hint")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
