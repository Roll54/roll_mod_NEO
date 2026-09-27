package com.roll_54.roll_mod.minestar.data;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.roll_54.roll_mod.RollMod;
import com.roll_54.roll_mod.minestar.moderation.ModerationStatus;
import com.roll_54.roll_mod.minestar.moderation.PunishmentLetters;
import com.roll_54.roll_mod.util.Durations;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /mute} and {@code /unmute}, and the same under {@code /rollmod}.
 *
 * <p>Took over from FTB Essentials' mute along with the state behind it; the chat handler reads
 * {@link MuteStore} now.
 */
@EventBusSubscriber(modid = RollMod.MODID)
public final class MuteCommands {

    private MuteCommands() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("rollmod").then(mute()).then(unmute()));
        dispatcher.register(mute());
        dispatcher.register(unmute());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> mute() {
        return Commands.literal("mute")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> mute(ctx.getSource(),
                                EntityArgument.getPlayer(ctx, "target"), 0, ""))
                        .then(Commands.argument("minutes", IntegerArgumentType.integer(0))
                                .executes(ctx -> mute(ctx.getSource(),
                                        EntityArgument.getPlayer(ctx, "target"),
                                        IntegerArgumentType.getInteger(ctx, "minutes"), ""))
                                .then(Commands.argument("reason", StringArgumentType.greedyString())
                                        .executes(ctx -> mute(ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "target"),
                                                IntegerArgumentType.getInteger(ctx, "minutes"),
                                                StringArgumentType.getString(ctx, "reason"))))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> unmute() {
        return Commands.literal("unmute")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> unmute(ctx.getSource(),
                                EntityArgument.getPlayer(ctx, "target"))));
    }

    /** {@code minutes = 0} means indefinite, which is what a mute with no duration should be. */
    private static int mute(CommandSourceStack source, ServerPlayer target, int minutes, String reason) {
        long until = minutes <= 0 ? 0L : System.currentTimeMillis() + minutes * 60_000L;
        MuteStore.mute(target.getUUID(), until, source.getTextName(), reason);

        String duration = minutes <= 0
                ? Component.translatable("msg.roll_mod.mute.forever").getString()
                : Durations.format(minutes * 60_000L);
        source.sendSuccess(() -> Component.translatable("msg.roll_mod.mute.muted",
                target.getGameProfile().getName(), duration), true);
        target.sendSystemMessage(Component.translatable("msg.roll_mod.mute.youAre", duration)
                .withStyle(ChatFormatting.RED));
        ModerationStatus.sendTo(target);
        // This command takes no rule, only free text, which the letter carries as the note.
        PunishmentLetters.send(target.server, target.getUUID(), PunishmentLetters.Kind.MUTE,
                source.getTextName(), minutes * 60_000L, "", reason, 0);
        return 1;
    }

    private static int unmute(CommandSourceStack source, ServerPlayer target) {
        if (!MuteStore.unmute(target.getUUID())) {
            source.sendFailure(Component.translatable("msg.roll_mod.mute.notMuted",
                    target.getGameProfile().getName()));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("msg.roll_mod.mute.unmuted",
                target.getGameProfile().getName()), true);
        target.sendSystemMessage(Component.translatable("msg.roll_mod.mute.youAreNot"));
        ModerationStatus.sendTo(target);
        return 1;
    }
}
