package com.roll_54.roll_mod.data;

import com.mojang.serialization.Codec;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

import static com.roll_54.roll_mod.RollMod.MODID;

public class RMMAttachment {
    // Create the DeferredRegister for attachment types
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MODID);

    // Serialization via map codec
    private static final Supplier<AttachmentType<Integer>> MANA = ATTACHMENT_TYPES.register(
            "mana", () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).build()
    );

    public static final Supplier<AttachmentType<Boolean>> STORM_PROTECTED = ATTACHMENT_TYPES.register(
            "storm_protected", () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL).copyOnDeath().build()
    );

    public static final Supplier<AttachmentType<Boolean>> AUTO_GIVE = ATTACHMENT_TYPES.register(
            "auto_give", () -> AttachmentType.builder(() -> true).serialize(Codec.BOOL).copyOnDeath().build()
    );
    /**
     * No serialization (NOT!!!! PERSISTENT)
     private static final Supplier<AttachmentType<SomeCache>> SOME_CACHE = ATTACHMENT_TYPES.register(
     "some_cache", () -> AttachmentType.builder(() -> new SomeCache()).build()
     );

     **/
}
