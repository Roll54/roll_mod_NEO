package com.roll_54.roll_mod.data;

import com.mojang.serialization.Codec;
import com.roll_54.roll_mod.cosmetics.PlayerItemSkins;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import net.minecraft.core.UUIDUtil;

import java.util.UUID;
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

    // Accumulated radiation dose (0 .. RadiationHandler.MAX_RADIATION). Persists and copies on death.
    public static final Supplier<AttachmentType<Integer>> RADIATION = ATTACHMENT_TYPES.register(
            "radiation", () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).copyOnDeath().build()
    );

    /**
     * How far the player has progressed, {@code 0 .. PlayerTier.MAX} — see
     * {@link com.roll_54.roll_mod.minestar.hub.PlayerTier}. Set by {@code /rollmod tier} and shown
     * on the hub's home screen. Progression is not something a death should undo, hence copyOnDeath.
     */
    public static final Supplier<AttachmentType<Integer>> TIER = ATTACHMENT_TYPES.register(
            "tier", () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).copyOnDeath().build()
    );
    /**
     * Cosmetic item skins — <b>legacy, kept for one release to import old saves.</b>
     *
     * <p>This is no longer where cosmetics live. It serialised into {@code world/playerdata/}, which
     * meant a world reset destroyed every skin anyone had earned or paid for; they now live in a
     * database outside the world save, owned by {@code cosmetics.storage.ItemSkinStore}.
     *
     * <p>All that remains is the import: on a player's first login after the move, a non-empty
     * attachment is copied into the database and then set to {@link PlayerItemSkins#EMPTY}. Being
     * empty <em>is</em> the "already imported" flag, which is what makes the import idempotent and
     * safe to interrupt.
     *
     * <p>Delete this field, {@link PlayerItemSkins#CODEC} and
     * {@code ItemSkinStore#importLegacyAttachment} one release after that ships.
     */
    public static final Supplier<AttachmentType<PlayerItemSkins>> ITEM_SKINS = ATTACHMENT_TYPES.register(
            "item_skins", () -> AttachmentType.builder(() -> PlayerItemSkins.EMPTY)
                    .serialize(PlayerItemSkins.CODEC).copyOnDeath().build()
    );

    /**
     * Who a production block works for, as far as daily tasks are concerned — a furnace, smoker or
     * blast furnace, or a Farmer's Delight cooking pot: the player who placed it or last opened it.
     * {@link #NO_OWNER} until either happens. Lives on the block entity, so it is saved with the chunk.
     */
    public static final UUID NO_OWNER = new UUID(0L, 0L);

    public static final Supplier<AttachmentType<UUID>> STATION_OWNER = ATTACHMENT_TYPES.register(
            "station_owner", () -> AttachmentType.builder(() -> NO_OWNER).serialize(UUIDUtil.CODEC).build()
    );

    /**
     * No serialization (NOT!!!! PERSISTENT)
     private static final Supplier<AttachmentType<SomeCache>> SOME_CACHE = ATTACHMENT_TYPES.register(
     "some_cache", () -> AttachmentType.builder(() -> new SomeCache()).build()
     );

     **/
}
