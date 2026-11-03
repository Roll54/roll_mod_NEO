package com.roll_54.roll_mod.gtmogs.data.command;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.BulkSectionAccess;
import net.minecraft.world.level.levelgen.structure.templatesystem.AlwaysTrueTest;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.roll_54.roll_mod.gtmogs.api.registry.GTRegistries;
import com.roll_54.roll_mod.gtmogs.api.worldgen.OreVeinDefinition;
import com.roll_54.roll_mod.gtmogs.api.worldgen.ores.GeneratedVeinMetadata;
import com.roll_54.roll_mod.gtmogs.api.worldgen.ores.OreGenDiagnostics;
import com.roll_54.roll_mod.gtmogs.api.worldgen.ores.OreGenerator;
import com.roll_54.roll_mod.gtmogs.api.worldgen.ores.OrePlacer;
import com.roll_54.roll_mod.gtmogs.core.mixins.ResourceKeyArgumentAccessor;

import static net.minecraft.commands.Commands.*;

public class GTCommands {

    private static final Dynamic2CommandExceptionType VEIN_PLACE_FAILURE = new Dynamic2CommandExceptionType(
            (id, sourcePos) -> Component.translatable("command.gtmogs.place_vein.failure", id, sourcePos));
    private static final DynamicCommandExceptionType ERROR_INVALID_VEIN = new DynamicCommandExceptionType(
            id -> Component.translatableEscape("command.gtmogs.place_vein.invalid", id));

    // spotless:off
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        dispatcher.register(literal("gtmogs")
                .then(literal("place_vein")
                        .requires(ctx -> ctx.hasPermission(LEVEL_ADMINS))
                        .then(argument("vein", ResourceKeyArgument.key(GTRegistries.ORE_VEIN_REGISTRY))
                                .executes(context -> {
                                    return GTCommands.placeVein(context, BlockPos.containing(context.getSource().getPosition()));
                                })
                                .then(argument("position", BlockPosArgument.blockPos())
                                        .executes(context -> {
                                            return GTCommands.placeVein(context, BlockPosArgument.getBlockPos(context, "position"));
                                        }))))
                .then(literal("diagnostics")
                        .requires(ctx -> ctx.hasPermission(LEVEL_ADMINS))
                        .executes(GTCommands::reportDiagnostics)
                        .then(literal("reset")
                                .executes(context -> {
                                    OreGenDiagnostics.reset();
                                    context.getSource().sendSuccess(() -> Component.literal("GTMOGS ore gen stats reset"), false);
                                    return 1;
                                }))));
    }
    // spotless:on

    /**
     * Prints the accumulated ore generation timings, so a laggy server can be checked without waiting for the
     * periodic summary in the log.
     */
    private static int reportDiagnostics(CommandContext<CommandSourceStack> context) {
        if (!OreGenDiagnostics.enabled()) {
            context.getSource().sendFailure(Component
                    .literal("Ore gen diagnostics are disabled (worldgen.diagnostics.enabled in the config)"));
            return 0;
        }

        for (String line : OreGenDiagnostics.report()) {
            context.getSource().sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
    }

    private static int placeVein(CommandContext<CommandSourceStack> context,
                                 BlockPos sourcePos) throws CommandSyntaxException {
        Holder.Reference<OreVeinDefinition> vein = ResourceKeyArgumentAccessor.callResolveKey(context, "vein",
                GTRegistries.ORE_VEIN_REGISTRY, ERROR_INVALID_VEIN);
        ResourceLocation id = vein.key().location();

        ChunkPos chunkPos = new ChunkPos(sourcePos);
        ServerLevel level = context.getSource().getLevel();

        GeneratedVeinMetadata metadata = new GeneratedVeinMetadata(chunkPos, sourcePos, vein);
        RandomSource random = level.random;

        OrePlacer placer = new OrePlacer();
        OreGenerator generator = placer.getOreGenCache().getOreGenerator();

        try (BulkSectionAccess access = new BulkSectionAccess(level)) {
            var generated = generator.generateOres(new OreGenerator.VeinConfiguration(metadata, random), level,
                    chunkPos);
            if (generated.isEmpty()) {
                throw VEIN_PLACE_FAILURE.create(id.toString(), sourcePos.toString());
            }
            for (ChunkPos pos : generated.get().getGeneratedChunks()) {
                placer.placeVein(pos, random, access, generated.get(), AlwaysTrueTest.INSTANCE);
                level.getChunk(pos.x, pos.z).setUnsaved(true);
            }
            context.getSource().sendSuccess(() -> Component.translatable("command.gtmogs.place_vein.success",
                    id.toString(), sourcePos.toString()), true);
        }

        return 1;
    }
}
