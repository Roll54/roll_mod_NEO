package com.roll_54.roll_mod.minestar.moderation.invsee;

import com.mojang.authlib.GameProfile;
import com.roll_54.roll_mod.RollMod;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.common.util.FakePlayer;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * An offline player's vanilla inventory, straight from their {@code playerdata/<uuid>.dat}.
 *
 * <p>The items are loaded into an {@link Inventory} owned by a throwaway {@link FakePlayer} with
 * the player's profile. That gives the stacks a registry context and gives the armour checks an
 * owner. The fake player is never added to the world.
 *
 * <p>{@link #save} writes through on every change, and touches only the {@code Inventory} list. It
 * re-reads the file, swaps that one list, and replaces the file the way vanilla's
 * {@code PlayerDataStorage.save} does (temp file, then {@code Util.safeReplaceFile} keeping a
 * {@code .dat_old}). Everything else the file holds is left exactly as the player left it:
 * position, attachments, curios, implants. Writes happen on the server thread, and the session
 * closes the moment the player logs in. So a login always reads the latest write and never races
 * one.
 */
final class OfflineInventory {

    private final MinecraftServer server;
    private final GameProfile profile;
    private final Path dir;
    final Inventory inventory;

    private OfflineInventory(MinecraftServer server, GameProfile profile, Path dir, Inventory inventory) {
        this.server = server;
        this.profile = profile;
        this.dir = dir;
        this.inventory = inventory;
    }

    /** The player's saved inventory, or {@code null} when they have no saved data to edit. */
    @Nullable
    static OfflineInventory load(MinecraftServer server, GameProfile profile) {
        Path dir = server.getWorldPath(LevelResource.PLAYER_DATA_DIR);
        CompoundTag tag = read(server, dir, profile);
        if (tag == null) return null;
        FakePlayer owner = new FakePlayer(server.overworld(), profile);
        Inventory inventory = owner.getInventory();
        inventory.load(tag.getList("Inventory", Tag.TAG_COMPOUND));
        return new OfflineInventory(server, profile, dir, inventory);
    }

    /** Whether the player has logged in since; the session closes then and nothing more is written. */
    boolean ownerOnline() {
        return server.getPlayerList().getPlayer(profile.getId()) != null;
    }

    /** Writes the inventory back into the player's file; {@code false} if that failed. */
    boolean save() {
        CompoundTag tag = read(server, dir, profile);
        if (tag == null) {
            RollMod.LOGGER.error("[InvSee] {}'s playerdata vanished; the edit was not saved", profile.getName());
            return false;
        }
        tag.put("Inventory", inventory.save(new ListTag()));
        // The tag was brought up to date on read, so it is stamped as current like vanilla's own save.
        NbtUtils.addCurrentDataVersion(tag);
        String id = profile.getId().toString();
        try {
            Path temp = Files.createTempFile(dir, id + "-", ".dat");
            NbtIo.writeCompressed(tag, temp);
            Util.safeReplaceFile(dir.resolve(id + ".dat"), temp, dir.resolve(id + ".dat_old"));
            return true;
        } catch (IOException e) {
            RollMod.LOGGER.error("[InvSee] could not save {}'s playerdata", profile.getName(), e);
            return false;
        }
    }

    @Nullable
    private static CompoundTag read(MinecraftServer server, Path dir, GameProfile profile) {
        Path file = dir.resolve(profile.getId() + ".dat");
        if (!Files.isRegularFile(file)) return null;
        try {
            CompoundTag tag = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
            // The same upgrade vanilla runs on load, so items saved by an older version still parse.
            int version = NbtUtils.getDataVersion(tag, -1);
            return DataFixTypes.PLAYER.updateToCurrentVersion(server.getFixerUpper(), tag, version);
        } catch (IOException | RuntimeException e) {
            RollMod.LOGGER.error("[InvSee] could not read {}'s playerdata", profile.getName(), e);
            return null;
        }
    }
}
