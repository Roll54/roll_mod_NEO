package com.roll_54.roll_mod.compat.ftb;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Reads and clears the homes a player set through FTB Essentials, without hard-depending on it.
 *
 * <p>Same shape as {@link TeamsFacade}: no {@code dev.ftb.mods} class may be named from a class that
 * loads unconditionally, so every such reference lives in {@link FTBEssentialsHomesFacadeImpl},
 * which {@link #get()} only class-loads once it has confirmed the mod is present.
 *
 * <p>This exists for one job — {@code HomeCommandRedirect} hands {@code /home} and friends to this
 * mod, which leaves any home a player had already set with FTB stranded: still in FTB's player data,
 * no longer reachable by any command. Rather than silently orphan them, the homes tab lists them and
 * offers to bring each one across.
 */
public interface HomesFacade {

    /** Every home the player still has in FTB Essentials, in whatever order FTB holds them. */
    List<Legacy> homesOf(ServerPlayer player);

    /**
     * Drops one FTB home by name, once it has been copied across. {@code false} when there was no
     * such home, which is also what makes migrating the same row twice harmless.
     */
    boolean forget(ServerPlayer player, String name);

    /**
     * One FTB home, flattened to the fields {@code PlayerHome} needs. FTB stores the position as a
     * {@code BlockPos} plus two rotations; the block centre is what this reports for x and z.
     */
    record Legacy(String name, ResourceLocation dimension, double x, double y, double z,
                  float yaw, float pitch) {}

    /** Used when ftbessentials is absent: nobody has anything to migrate. */
    HomesFacade NONE = new HomesFacade() {
        @Override
        public List<Legacy> homesOf(ServerPlayer player) {
            return List.of();
        }

        @Override
        public boolean forget(ServerPlayer player, String name) {
            return false;
        }
    };

    static HomesFacade get() {
        return Holder.INSTANCE;
    }

    /** Lazy holder so the {@code Class.forName} lookup happens once, on first use. */
    final class Holder {
        static final HomesFacade INSTANCE = load();

        private Holder() {}

        private static HomesFacade load() {
            if (!net.neoforged.fml.ModList.get().isLoaded("ftbessentials")) {
                return NONE;
            }
            try {
                return (HomesFacade) Class
                        .forName("com.roll_54.roll_mod.compat.ftb.FTBEssentialsHomesFacadeImpl")
                        .getDeclaredConstructor()
                        .newInstance();
            } catch (Throwable t) {
                com.roll_54.roll_mod.RollMod.LOGGER.warn(
                        "[Homes] ftbessentials is loaded but its API could not be bound; "
                                + "legacy homes will not be offered for migration.", t);
                return NONE;
            }
        }
    }
}
