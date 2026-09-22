package com.roll_54.roll_mod.minestar.perks;

import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.config.MyConfig;
import com.roll_54.roll_mod.economy.vendingblock.auction.LuckPermsCompat;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.neoforge.common.NeoForgeMod;

/**
 * {@code /fly} — flight as a rank perk, remembered between sessions.
 *
 * <p>Flight is granted through NeoForge's {@code creative_flight} attribute, never by writing
 * {@code Abilities.mayfly} — that field is deprecated in 21.1 and, more to the point, it is a single
 * boolean that several things here want to own at once. The attribute is a
 * {@link net.neoforged.neoforge.common.BooleanAttribute}, whose whole purpose is that enabling
 * modifiers coexist: the game mode, the Extended Industrialization gravichestplate (which adds its
 * own modifier to this same attribute) and this command each hold their own, and none can take
 * another's flight away.
 *
 * <p>Nothing here stops a player flying, either. {@code ServerPlayer.doTick} already clears
 * {@code flying} the tick after {@code mayFly()} goes false, so revoking the modifier is the whole
 * of turning flight off — and the player falls from wherever they were, as they should.
 *
 * <p>The flag itself lives in the player's persistent NBT, the way the PvP toggle does. It outlives
 * a logout on its own; death and dimension changes hand out a new entity, so {@link PerkEvents}
 * carries it across and re-applies the modifier, which is transient and therefore never saved.
 */
public final class FlyService {

    /** Whether this player asked to fly. Namespaced, because persistent data is shared by every mod. */
    public static final String TAG_FLY = "roll_mod:fly";

    private static final ResourceLocation MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(RollMod.MODID, "fly_command");

    /** The boolean attribute's "on" shape: {@code ADD_VALUE 1}. Anything else is undefined behaviour. */
    private static final AttributeModifier MODIFIER =
            new AttributeModifier(MODIFIER_ID, 1.0D, AttributeModifier.Operation.ADD_VALUE);

    private FlyService() {}

    public static boolean enabled(ServerPlayer player) {
        return player.getPersistentData().getBoolean(TAG_FLY);
    }

    /**
     * Turns flight on or off, or explains why it will not.
     *
     * @return whether anything changed.
     */
    public static boolean toggle(ServerPlayer player) {
        if (!LuckPermsCompat.canFly(player)) {
            refuse(player, "msg.roll_mod.fly.denied");
            return false;
        }

        boolean turningOn = !enabled(player);
        if (turningOn && !allowedHere(player)) {
            refuse(player, "msg.roll_mod.fly.blockedHere");
            return false;
        }

        player.getPersistentData().putBoolean(TAG_FLY, turningOn);
        apply(player);
        player.sendSystemMessage(Component.translatable(
                        turningOn ? "msg.roll_mod.fly.enabled" : "msg.roll_mod.fly.disabled")
                .withStyle(turningOn ? ChatFormatting.GREEN : ChatFormatting.GRAY));
        return true;
    }

    /**
     * Puts the attribute in step with the flag. Idempotent, and cheap enough to call from every
     * event that hands the player a new entity or a new dimension.
     *
     * <p>The permission is re-read here, not only in {@link #toggle}: a player who loses the rank
     * while offline stops flying at their next login without anybody running a command, and the flag
     * survives, so restoring the rank restores their flight.
     */
    public static void apply(ServerPlayer player) {
        AttributeInstance instance = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (instance == null) return;

        if (enabled(player) && allowedHere(player) && LuckPermsCompat.canFly(player)) {
            instance.addOrUpdateTransientModifier(MODIFIER);
        } else {
            instance.removeModifier(MODIFIER_ID);
        }
    }

    /** Whether {@code /fly} works in the dimension the player is standing in. */
    public static boolean allowedHere(ServerPlayer player) {
        return !MyConfig.INSTANCE.fly.blacklistedDimensions
                .contains(player.level().dimension().location().toString());
    }

    private static void refuse(ServerPlayer player, String key) {
        player.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.RED));
    }
}
