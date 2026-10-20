package com.roll_54.roll_mod.blocks.entity;

import com.roll_54.roll_mod.registry.BlockEntites;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * The statue's lower half. Besides the geo model it keeps the flowers players donated
 * ({@code HueriStatueFlowers}), saved with the chunk and synced to clients for the renderer.
 */
public class HueriStatueBlockEntity extends BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /**
     * One donated flower's hologram: who left it, the flower item, its offset from the block's
     * centre on the floor one block below the statue, and its turn in degrees.
     */
    public record Flower(UUID donor, String donorName, ResourceLocation item, float dx, float dz, float yaw) {

        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("donor", donor);
            tag.putString("name", donorName);
            tag.putString("item", item.toString());
            tag.putFloat("dx", dx);
            tag.putFloat("dz", dz);
            tag.putFloat("yaw", yaw);
            return tag;
        }

        static Flower load(CompoundTag tag) {
            return new Flower(tag.getUUID("donor"), tag.getString("name"),
                    ResourceLocation.parse(tag.getString("item")),
                    tag.getFloat("dx"), tag.getFloat("dz"), tag.getFloat("yaw"));
        }
    }

    private final List<Flower> flowers = new ArrayList<>();

    public HueriStatueBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntites.HUERI_STATUE_BE.get(), pos, blockState);
    }

    public List<Flower> flowers() {
        return flowers;
    }

    public boolean hasDonated(UUID player) {
        for (Flower flower : flowers) if (flower.donor().equals(player)) return true;
        return false;
    }

    /** Adds {@code added}, saves and sends them to every client tracking the chunk. */
    public void addAll(Collection<Flower> added) {
        flowers.addAll(added);
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (Flower flower : flowers) list.add(flower.save());
        tag.put("flowers", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        flowers.clear();
        for (Tag entry : tag.getList("flowers", Tag.TAG_COMPOUND)) {
            try {
                flowers.add(Flower.load((CompoundTag) entry));
            } catch (Exception ignored) {
                // A malformed entry is dropped rather than losing the whole list.
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // No animations - static geo model only
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
