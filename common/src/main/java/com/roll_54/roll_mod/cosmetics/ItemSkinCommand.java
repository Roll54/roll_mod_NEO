package com.roll_54.roll_mod.cosmetics;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.roll_54.roll_mod.RollMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /rollmod admin skin …} — grants skins and picks which one renders in which slot.
 *
 * <p>Operator-only in full, gated once on the {@code admin} literal at permission level 2, the same
 * way {@code CurrencyCommand} and {@code AutoGiveCommands} gate theirs. Players reach the same
 * {@link ItemSkinService} methods through {@code SkinActionPacket} when the hub tab is built; this
 * command exists so the rendering can be driven and tested before then, which is administrative
 * work rather than a second way for a player to dress up.
 *
 * <p>In {@code common} rather than the server module for the reason {@code TierCommand} gives: so
 * it exists in the dev client run, which is where it gets used.
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class ItemSkinCommand {
    private ItemSkinCommand() {}

    private static final SuggestionProvider<CommandSourceStack> SKIN_IDS = (ctx, builder) ->
            SharedSuggestionProvider.suggestResource(
                    ItemSkinRegistry.all().stream().map(ItemSkinDefinition::skinId).toList(), builder);

    private static final SuggestionProvider<CommandSourceStack> SLOTS = (ctx, builder) ->
            SharedSuggestionProvider.suggest(
                    java.util.Arrays.stream(SkinCategory.values()).map(SkinCategory::getSerializedName), builder);

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rollmod")
                .then(Commands.literal("admin")
                        .then(Commands.literal("skin")
                            // One gate for the whole subtree — every skin subcommand is operator
                            // work. It sits on "skin" rather than on "admin" deliberately: Brigadier
                            // merges same-named literals by copying only the command and children
                            // onto the node that got there first, silently dropping the newcomer's
                            // requirement. Several classes register "rollmod admin" and
                            // NetherstormCommand's copy carries no requirement at all, so a gate
                            // placed there would hold or not depending on registration order. This
                            // node is registered by nobody else, so its gate always applies.
                            .requires(src -> src.hasPermission(2))
                            .then(Commands.literal("unlock")
                                    .then(Commands.argument("skin", ResourceLocationArgument.id())
                                            .suggests(SKIN_IDS)
                                            .executes(ctx -> unlock(ctx.getSource(),
                                                    ctx.getSource().getPlayerOrException(),
                                                    ResourceLocationArgument.getId(ctx, "skin")))
                                            .then(Commands.argument("target", EntityArgument.player())
                                                    .executes(ctx -> unlock(ctx.getSource(),
                                                            EntityArgument.getPlayer(ctx, "target"),
                                                            ResourceLocationArgument.getId(ctx, "skin"))))))
                            .then(Commands.literal("revoke")
                                    .then(Commands.argument("skin", ResourceLocationArgument.id())
                                            .suggests(SKIN_IDS)
                                            .executes(ctx -> revoke(ctx.getSource(),
                                                    ctx.getSource().getPlayerOrException(),
                                                    ResourceLocationArgument.getId(ctx, "skin")))))
                            .then(Commands.literal("set")
                                    .then(Commands.argument("skin", ResourceLocationArgument.id())
                                            .suggests(SKIN_IDS)
                                            .executes(ctx -> set(ctx.getSource(),
                                                    ResourceLocationArgument.getId(ctx, "skin")))))
                            .then(Commands.literal("clear")
                                    .then(Commands.argument("slot", StringArgumentType.word())
                                            .suggests(SLOTS)
                                            .executes(ctx -> clear(ctx.getSource(),
                                                    StringArgumentType.getString(ctx, "slot")))))
                            .then(Commands.literal("slot")
                                    .executes(ctx -> slot(ctx.getSource())))
                            .then(Commands.literal("list")
                                    .executes(ctx -> list(ctx.getSource()))))));
    }

    private static int unlock(CommandSourceStack source, ServerPlayer target, ResourceLocation skinId) {
        if (notReady(source, target)) {
            return 0;
        }
        if (!ItemSkinService.unlock(target, skinId)) {
            source.sendFailure(Component.literal(
                    "Unknown skin " + skinId + ", or " + target.getGameProfile().getName() + " already owns it"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "Unlocked " + skinId + " for " + target.getGameProfile().getName()), true);
        return 1;
    }

    private static int revoke(CommandSourceStack source, ServerPlayer target, ResourceLocation skinId) {
        if (notReady(source, target)) {
            return 0;
        }
        if (!ItemSkinService.revoke(target, skinId)) {
            source.sendFailure(Component.literal(
                    target.getGameProfile().getName() + " does not own " + skinId));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(
                "Revoked " + skinId + " from " + target.getGameProfile().getName()), true);
        return 1;
    }

    private static int set(CommandSourceStack source, ResourceLocation skinId)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (notReady(source, player)) {
            return 0;
        }
        ItemSkinDefinition skin = ItemSkinRegistry.get(skinId);
        if (!ItemSkinService.setActive(player, skinId)) {
            // One message for both rejections: the skin does not exist, or you do not own it.
            source.sendFailure(Component.literal("Cannot apply " + skinId + " — unknown or not unlocked"));
            return 0;
        }
        String slot = skin == null ? "?" : skin.category().getSerializedName();
        source.sendSuccess(() -> Component.literal(skinId + " now renders on every " + slot), false);
        return 1;
    }

    private static int clear(CommandSourceStack source, String slotName)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (notReady(source, player)) {
            return 0;
        }
        SkinCategory category = SkinCategory.byName(slotName);
        if (category == null) {
            source.sendFailure(Component.literal("Unknown slot " + slotName));
            return 0;
        }
        if (!ItemSkinService.clearActive(player, category)) {
            source.sendFailure(Component.literal("No skin was active in the " + slotName + " slot"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Cleared the " + slotName + " slot"), false);
        return 1;
    }

    /**
     * Reports the slot the held item resolves to, and by which of the three layers. The quickest way
     * to check that a paxel lands on {@code pickaxe} rather than {@code axe}, or that a datapack
     * blacklist edit took effect after a {@code /reload}.
     */
    private static int slot(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            source.sendFailure(Component.literal("Hold the item you want to check"));
            return 0;
        }
        SkinCategory category = SkinCategory.of(held.getItem());
        source.sendSuccess(() -> Component.literal(
                held.getItem() + " -> " + (category == null ? "not skinnable" : category.getSerializedName())
                        + " (" + SkinCategory.explain(held.getItem()) + ")"), false);
        return category == null ? 0 : 1;
    }

    /**
     * Cosmetics live in a database now, so "no" can mean "not loaded yet" or "storage is down" as
     * well as "that is not allowed". Separating them here keeps the per-command failure messages
     * about what the operator actually did wrong.
     */
    private static boolean notReady(CommandSourceStack source, ServerPlayer target) {
        if (ItemSkinService.isReady(target)) {
            return false;
        }
        source.sendFailure(Component.literal("Cosmetic storage is unavailable or still loading for "
                + target.getGameProfile().getName()));
        return true;
    }

    private static int list(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        PlayerItemSkins skins = ItemSkinService.get(source.getPlayerOrException());
        source.sendSuccess(() -> Component.literal(
                "Unlocked: " + (skins.unlocked().isEmpty() ? "(none)" : skins.unlocked())
                        + "\nActive: " + (skins.active().isEmpty() ? "(none)" : skins.active())), false);
        return skins.unlocked().size();
    }
}
