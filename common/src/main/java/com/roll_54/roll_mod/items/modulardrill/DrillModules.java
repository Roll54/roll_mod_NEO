package com.roll_54.roll_mod.items.modulardrill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A modular drill's whole configuration: which modules are installed and where the throttle sits.
 *
 * <p>Modules are stored as item ids rather than stacks — a {@link DrillModuleItem} is stateless,
 * so the id is the entire module and reconstructing the item from it is lossless. The component is
 * network-synced (unlike the legacy {@code UPGRADES} component) so tooltips and the config GUI can
 * read it client-side.
 *
 * <p>Throttle is a percentage of the drill's current top speed. 100 is the normal ceiling; the
 * Overcharge module raises it, and every point above 100 costs +10% energy per block.
 *
 * <p>The volume is the player's chosen mining box (width × height × depth), set in the config
 * GUI. It starts at 1×1×1 and remembers whatever was dialed in; the AOE module only sets the
 * ceiling, which {@link DrillModuleHelper#volume} clamps against on every read.
 */
public record DrillModules(List<ResourceLocation> modules, int throttle, int volX, int volY, int volZ) {

    public static final DrillModules EMPTY = new DrillModules(List.of(), 100, 1, 1, 1);

    public static final Codec<DrillModules> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.listOf().fieldOf("modules").forGetter(DrillModules::modules),
            Codec.INT.optionalFieldOf("throttle", 100).forGetter(DrillModules::throttle),
            Codec.INT.optionalFieldOf("vol_x", 1).forGetter(DrillModules::volX),
            Codec.INT.optionalFieldOf("vol_y", 1).forGetter(DrillModules::volY),
            Codec.INT.optionalFieldOf("vol_z", 1).forGetter(DrillModules::volZ)
    ).apply(instance, DrillModules::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, DrillModules> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), DrillModules::modules,
            ByteBufCodecs.VAR_INT, DrillModules::throttle,
            ByteBufCodecs.VAR_INT, DrillModules::volX,
            ByteBufCodecs.VAR_INT, DrillModules::volY,
            ByteBufCodecs.VAR_INT, DrillModules::volZ,
            DrillModules::new);

    public DrillModules withThrottle(int throttle) {
        return new DrillModules(modules, throttle, volX, volY, volZ);
    }

    public DrillModules withVolume(int x, int y, int z) {
        return new DrillModules(modules, throttle, x, y, z);
    }

    public DrillModules withModuleAdded(ResourceLocation id) {
        List<ResourceLocation> list = new ArrayList<>(modules);
        list.add(id);
        return new DrillModules(List.copyOf(list), throttle, volX, volY, volZ);
    }

    public DrillModules withModuleRemoved(int index) {
        List<ResourceLocation> list = new ArrayList<>(modules);
        list.remove(index);
        return new DrillModules(List.copyOf(list), throttle, volX, volY, volZ);
    }

    /** The installed module items, in slot order. Ids that no longer resolve are skipped. */
    public List<DrillModuleItem> moduleItems() {
        List<DrillModuleItem> items = new ArrayList<>(modules.size());
        for (ResourceLocation id : modules) {
            if (BuiltInRegistries.ITEM.get(id) instanceof DrillModuleItem module) {
                items.add(module);
            }
        }
        return items;
    }

    /** The installed module of this type, or {@code null}. A drill holds at most one per type. */
    public @Nullable DrillModuleItem ofType(ModuleType type) {
        for (DrillModuleItem module : moduleItems()) {
            if (module.type == type) return module;
        }
        return null;
    }

    public boolean has(ModuleType type) {
        return ofType(type) != null;
    }
}
