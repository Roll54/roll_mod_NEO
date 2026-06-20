package com.roll_54.roll_mod_server.minestar;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.roll_54.roll_mod.RollMod;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import ua.com.minestar.model.ShopProduct;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static ua.com.minestar.Minestar.minestar;

public class ShopReceiveCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("shop-receive")
                        .executes(ShopReceiveCommand::execute)
        );
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        player.sendSystemMessage(Component.literal("Перевірка придбаних предметів...").withStyle(ChatFormatting.GRAY));
        RollMod.LOGGER.info("[Shop Sync] Checking for pending products for player {}.", player.getName().getString());

        minestar.getUserPendingShopProductsByProfileUuidAndServerId(player.getUUID())
                .onSuccess(products -> {

                    if (products.isEmpty()) {
                        player.sendSystemMessage(Component.literal("У вас немає товарів для отримання.").withStyle(ChatFormatting.YELLOW));
                        RollMod.LOGGER.info("[Shop Sync] No pending products found for {}.", player.getName().getString());
                        return;
                    }

                    player.sendSystemMessage(Component.literal("Знайдено товарів: %d. Починаємо видачу...".formatted(products.size())).withStyle(ChatFormatting.GREEN));
                    RollMod.LOGGER.info("[Shop Sync] Found {} pending products for {}.", products.size(), player.getName().getString());
                    processAndConfirmDelivery(player, products);
                })
                .onFailure(cause -> {
                    player.sendSystemMessage(Component.literal("Помилка під час отримання товарів. Спробуйте пізніше.").withStyle(ChatFormatting.RED));
                    RollMod.LOGGER.error("[Shop Sync] Failed to get pending products for {}.", player.getName().getString(), cause);
                });

        return 1;
    }

    private static void processAndConfirmDelivery(ServerPlayer player, List<ShopProduct> products) {
        MinecraftServer server = player.getServer();
        if (server == null) return;

        Set<Long> deliveredProductIds = new HashSet<>();
        CommandSourceStack source = server.createCommandSourceStack().withPermission(4); // Високий рівень доступу

        for (ShopProduct product : products) {
            boolean allCommandsSuccessful = true;

            for (int i = 0; i < product.quantity(); i++) {
                for (String commandTemplate : product.commands()) {
                    try {
                        String command = commandTemplate.replace("%player%", player.getName().getString());
                        server.getCommands().performPrefixedCommand(source, command);
                        RollMod.LOGGER.info("[Shop Sync] Executed command for product ID {} (Item {} of {}): {}", product.id(), i + 1, product.quantity(), command);
                    } catch (Exception e) {
                        allCommandsSuccessful = false;
                        RollMod.LOGGER.error("[Shop Sync] Failed to execute a command for product ID {}.", product.id(), e);
                        break;
                    }
                }
                if (!allCommandsSuccessful) {
                    break;
                }
            }

            if (allCommandsSuccessful) {
                deliveredProductIds.add(product.id());
            }
        }

        if (!deliveredProductIds.isEmpty()) {
            RollMod.LOGGER.info("[Shop Sync] Confirming delivery of {} products for {}.", deliveredProductIds.size(), player.getName().getString());


            minestar.deleteUserPendingShopProductsByIds(deliveredProductIds)
                    .onSuccess(unit -> {
                        String ids = deliveredProductIds.stream().map(String::valueOf).collect(Collectors.joining(", "));
                        RollMod.LOGGER.info("[Shop Sync] Successfully confirmed delivery of product IDs [{}] for {}.", ids, player.getName().getString());
                        player.sendSystemMessage(Component.literal("Усі товари успішно отримано!").withStyle(ChatFormatting.GREEN));
                    })
                    .onFailure(cause -> RollMod.LOGGER.error("[Shop Sync] Failed to confirm product delivery for {}.", player.getName().getString(), cause));
        }
    }
}