package com.roll_54.roll_mod.blocks;

import com.roll_54.roll_mod.blocks.entity.HueriStatueBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import java.util.List;

/**
 * Flowers left at the Hueri statue: once per player per statue, {@link #COUNT} vanilla flowers at
 * once, taken from the inventory. The price is a gesture rather than a count: the whole donation
 * becomes a single green hologram — of the first flower taken — on the floor round the statue, one
 * block below its base, with the donor's name over it when looked at ({@code HueriStatueRenderer}).
 *
 * <p>Vanilla means {@code #minecraft:small_flowers} from the {@code minecraft} namespace — a mod's
 * flowers in that tag do not count, and tall flowers are left out, their model being two blocks.
 */
public final class HueriStatueFlowers {

    public static final int COUNT = 10;
    /** 15 blocks across: seven either side of the statue's own block. */
    private static final float RADIUS = 7.5F;
    /**
     * How far from the block's centre a flower must stand on either axis: the 3x3 under and round
     * the statue (1.5) plus half a flower's own block, so none of the hologram reaches into it.
     */
    private static final float CLEAR = 2.0F;
    private static final double REACH = 8.0D;

    private HueriStatueFlowers() {}

    public static boolean isVanillaFlower(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ItemTags.SMALL_FLOWERS)
                && BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE);
    }

    /** How many vanilla flowers {@code player} carries. Either side. */
    public static int count(Player player) {
        int total = 0;
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (isVanillaFlower(stack)) total += stack.getCount();
        }
        return total;
    }

    /**
     * Server side of the popup's Donate: takes {@link #COUNT} flowers, slot by slot, and plants the
     * one hologram. Ignored unless {@code pos} is a statue's lower half within reach;
     * refused, with a message, for a second donation or too few flowers.
     */
    public static void donate(ServerPlayer player, BlockPos pos) {
        BlockState state = player.level().getBlockState(pos);
        if (!(state.getBlock() instanceof HueriStatueBlock)
                || state.getValue(HueriStatueBlock.HALF) != DoubleBlockHalf.LOWER
                || !player.canInteractWithBlock(pos, REACH)
                || !(player.level().getBlockEntity(pos) instanceof HueriStatueBlockEntity statue)) return;

        if (statue.hasDonated(player.getUUID())) {
            player.sendSystemMessage(Component.translatable("msg.roll_mod.hueri_statue.flowers.already")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        if (count(player) < COUNT) {
            player.sendSystemMessage(Component.translatable("msg.roll_mod.hueri_statue.flowers.not_enough", COUNT)
                    .withStyle(ChatFormatting.RED));
            return;
        }

        ResourceLocation shown = null;
        int taken = 0;
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize() && taken < COUNT; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!isVanillaFlower(stack)) continue;
            if (shown == null) shown = BuiltInRegistries.ITEM.getKey(stack.getItem());
            int take = Math.min(COUNT - taken, stack.getCount());
            stack.shrink(take);
            taken += take;
        }
        inventory.setChanged();

        RandomSource random = player.getRandom();
        float[] at = spot(random);
        statue.addAll(List.of(new HueriStatueBlockEntity.Flower(player.getUUID(), player.getGameProfile().getName(),
                shown, at[0], at[1], random.nextFloat() * 360.0F)));

        player.level().playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.2F);
        player.sendSystemMessage(Component.translatable("msg.roll_mod.hueri_statue.flowers.done")
                .withColor(0x76CCA8));
    }

    /** A uniform point in the disc of {@link #RADIUS}, outside the 3x3 centred on the statue. */
    private static float[] spot(RandomSource random) {
        float dx;
        float dz;
        do {
            float angle = random.nextFloat() * Mth.TWO_PI;
            float r = RADIUS * Mth.sqrt(random.nextFloat());
            dx = Mth.cos(angle) * r;
            dz = Mth.sin(angle) * r;
        } while (Math.abs(dx) < CLEAR && Math.abs(dz) < CLEAR);
        return new float[]{dx, dz};
    }
}
