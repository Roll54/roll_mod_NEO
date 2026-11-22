package com.roll_54.roll_mod.items;

import com.roll_54.roll_mod.minestar.chat.ChatChannel;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
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
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A pickaxe that, used on a block, reports which ores lie in a 5x5 column running {@link #DEPTH} blocks into the face
 * it was used on (plus the cube around the first and last steps, as it always reached).
 *
 * <p>The scan runs on the client, from the chunks it already has: the server only wears the pick and starts its
 * cooldown, so prospecting costs the server nothing. The findings go to the chat's Local tab, nearest ore first.
 *
 * <p>The scanned volume is one box (the column), read once, chunk section by chunk section; a section whose palette
 * holds no ore at all is skipped without looking at a single block in it.
 */
public class ProspectorPickItem extends PickaxeItem {

    private static final TagKey<Block> C_ORES = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "ores"));
    /** Half the column's width: 5x5. */
    private static final int RADIUS = 2;
    /** How many blocks deep the column's centre runs. */
    private static final int DEPTH = 90;
    /** Ticks between uses. */
    private static final int COOLDOWN = 100;

    public ProspectorPickItem(Tier tier, Properties properties) {
        super(tier, properties.stacksTo(1));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) return InteractionResultHolder.pass(stack);
        HitResult hit = player.pick(player.blockInteractionRange(), 0.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }

        if (level.isClientSide) {
            Client.report(scan(level, blockHit.getBlockPos(), blockHit.getDirection().getOpposite()));
        } else {
            stack.hurtAndBreak(1, player, player.getEquipmentSlotForItem(stack));
        }
        // Both sides, as vanilla does: the client's copy stops a second scan before the server's arrives.
        player.getCooldowns().addCooldown(this, COOLDOWN);
        player.swing(hand, !level.isClientSide);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /**
     * The ores in the column from {@code start} into {@code face}, nearest first (by how far along the column each
     * kind is first met). The column is the union of the {@link #RADIUS}-cubes centred on each of the {@link #DEPTH}
     * steps: along the axis from one block behind {@code start} to {@code DEPTH + RADIUS} ahead, {@code RADIUS} to
     * every side.
     */
    static List<Block> scan(Level level, BlockPos start, Direction face) {
        Direction.Axis axis = face.getAxis();
        int sign = face.getAxisDirection().getStep();
        int near = 1 - RADIUS, far = DEPTH + RADIUS;
        // The column as an axis-aligned box in world coordinates.
        int a0 = start.get(axis) + sign * near, a1 = start.get(axis) + sign * far;
        int minX = start.getX() - RADIUS, maxX = start.getX() + RADIUS;
        int minY = start.getY() - RADIUS, maxY = start.getY() + RADIUS;
        int minZ = start.getZ() - RADIUS, maxZ = start.getZ() + RADIUS;
        switch (axis) {
            case X -> { minX = Math.min(a0, a1); maxX = Math.max(a0, a1); }
            case Y -> { minY = Math.min(a0, a1); maxY = Math.max(a0, a1); }
            case Z -> { minZ = Math.min(a0, a1); maxZ = Math.max(a0, a1); }
        }
        minY = Math.max(minY, level.getMinBuildHeight());
        maxY = Math.min(maxY, level.getMaxBuildHeight() - 1);

        // Each ore kind, and how far along the column it was first met.
        Map<Block, Integer> found = new HashMap<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int cx = SectionPos.blockToSectionCoord(minX); cx <= SectionPos.blockToSectionCoord(maxX); cx++) {
            for (int cz = SectionPos.blockToSectionCoord(minZ); cz <= SectionPos.blockToSectionCoord(maxZ); cz++) {
                // Only chunks already there: never loads or generates one.
                if (!(level.getChunk(cx, cz, net.minecraft.world.level.chunk.status.ChunkStatus.FULL, false)
                        instanceof LevelChunk chunk)) continue;
                for (int sy = SectionPos.blockToSectionCoord(minY); sy <= SectionPos.blockToSectionCoord(maxY); sy++) {
                    int index = chunk.getSectionIndexFromSectionY(sy);
                    if (index < 0 || index >= chunk.getSectionsCount()) continue;
                    LevelChunkSection section = chunk.getSection(index);
                    // No ore anywhere in this section's palette: nothing to look for in it.
                    if (section.hasOnlyAir() || !section.maybeHas(state -> state.is(C_ORES))) continue;
                    int x0 = Math.max(minX, cx << 4), x1 = Math.min(maxX, (cx << 4) + 15);
                    int y0 = Math.max(minY, sy << 4), y1 = Math.min(maxY, (sy << 4) + 15);
                    int z0 = Math.max(minZ, cz << 4), z1 = Math.min(maxZ, (cz << 4) + 15);
                    for (int y = y0; y <= y1; y++) {
                        for (int z = z0; z <= z1; z++) {
                            for (int x = x0; x <= x1; x++) {
                                BlockState state = section.getBlockState(x & 15, y & 15, z & 15);
                                if (!state.is(C_ORES)) continue;
                                pos.set(x, y, z);
                                int along = (pos.get(axis) - start.get(axis)) * sign;
                                found.merge(state.getBlock(), along, Math::min);
                            }
                        }
                    }
                }
            }
        }
        List<Block> ordered = new ArrayList<>(found.keySet());
        ordered.sort((a, b) -> Integer.compare(found.get(a), found.get(b)));
        return ordered;
    }

    /** Client only: the findings into the chat's Local tab. Kept apart so the server never loads the chat store. */
    private static final class Client {
        static void report(List<Block> ores) {
            if (ores.isEmpty()) {
                post(Component.translatable("message.roll_mod.prospector.none").withStyle(ChatFormatting.RED));
                return;
            }
            for (Block ore : ores) {
                post(Component.translatable("message.roll_mod.prospector.found",
                        ore.getName().copy().withStyle(ChatFormatting.GRAY)).withStyle(ChatFormatting.GREEN));
            }
        }

        private static void post(Component message) {
            com.roll_54.roll_mod.minestar.chat.client.ClientChatStore.receive(ChatChannel.LOCAL, message, "", "", 0L);
        }
    }
}
