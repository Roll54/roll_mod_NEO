package com.roll_54.roll_mod.gtmogs.common.network.packets.prospecting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import java.util.Collection;

public abstract class SPacketProspect<T> implements CustomPacketPayload {

    protected final Table<ResourceKey<Level>, BlockPos, T> data;

    protected SPacketProspect() {
        data = HashBasedTable.create();
    }

    protected SPacketProspect(ResourceKey<Level> key, Collection<BlockPos> positions, Collection<T> prospected) {
        data = HashBasedTable.create(1, prospected.size());
        var posIterator = positions.iterator();
        var prospectedIterator = prospected.iterator();
        while (posIterator.hasNext()) {
            data.put(key, posIterator.next(), prospectedIterator.next());
        }
    }

    protected SPacketProspect(ResourceKey<Level> key, BlockPos position, T prospected) {
        data = HashBasedTable.create(1, 1);
        data.put(key, position, prospected);
    }

    public SPacketProspect(RegistryFriendlyByteBuf buf) {
        this();
        var rowCount = buf.readInt();
        for (int i = 0; i < rowCount; i++) {
            var rowKey = buf.readResourceKey(Registries.DIMENSION);
            var entryCount = buf.readInt();
            for (int j = 0; j < entryCount; j++) {
                var blockPos = buf.readBlockPos();
                var t = decodeData(buf);
                data.put(rowKey, blockPos, t);
            }
        }
    }

    public abstract void encodeData(RegistryFriendlyByteBuf buf, T data);

    public abstract T decodeData(RegistryFriendlyByteBuf buf);

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeInt(data.rowKeySet().size());
        data.rowMap().forEach((key, entry) -> {
            buf.writeResourceKey(key);
            buf.writeInt(entry.size());
            entry.forEach(((blockPos, t) -> {
                buf.writeBlockPos(blockPos);
                encodeData(buf, t);
            }));
        });
    }

    public abstract void execute(IPayloadContext context);
}
