package com.mazzy.mcuniversal.core.command;

import com.mazzy.mcuniversal.data.PlayerDimensionDataProvider;
import com.mazzy.mcuniversal.network.NetworkHandler;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Manages dimension-specific RTP permissions on per-player basis
 * Allows operators to control which dimensions players can randomly teleport to
 */
public class RTPDimensionCommands {

    /**
     * Registers both unlock/lock commands with the server
     * @param dispatcher Command system entry point
     */
    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Unlock command structure
        dispatcher.register(
                Commands.literal("unlockdimensionrtp")
                        .requires(source -> source.hasPermission(2)) // Admin-only
                        .then(Commands.literal("player")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .then(Commands.argument("dimensionId", ResourceLocationArgument.id())
                                                .suggests((ctx, builder) -> {
                                                    // Auto-suggest existing dimension IDs
                                                    for (ServerLevel level : ctx.getSource().getServer().getAllLevels()) {
                                                        builder.suggest(level.dimension().location().toString());
                                                    }
                                                    return builder.buildFuture();
                                                })
                                                .executes(ctx -> {
                                                    // Resolve command parameters
                                                    CommandSourceStack source = ctx.getSource();
                                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                                    ResourceLocation dimLoc = ResourceLocationArgument.getId(ctx, "dimensionId");

                                                    // Update player's dimension permissions
                                                    target.getCapability(PlayerDimensionDataProvider.PLAYER_DIMENSION_DATA)
                                                            .ifPresent(playerData -> {
                                                                playerData.unlockDimension(dimLoc.toString());
                                                                source.sendSuccess(
                                                                        () -> Component.literal("Unlocked dimension ["
                                                                                + dimLoc + "] for player "
                                                                                + target.getName().getString()),
                                                                        true
                                                                );
                                                                // Force client UI refresh
                                                                NetworkHandler.openAmuletScreenForPlayer(target);
                                                            });

                                                    return Command.SINGLE_SUCCESS;
                                                })
                                        )
                                )
                        )
        );

        // Lock command structure (mirrors unlock flow)
        dispatcher.register(
                Commands.literal("lockdimensionrtp")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("player")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .then(Commands.argument("dimensionId", ResourceLocationArgument.id())
                                                .suggests((ctx, builder) -> {
                                                    // Reuse dimension suggestion logic
                                                    for (ServerLevel level : ctx.getSource().getServer().getAllLevels()) {
                                                        builder.suggest(level.dimension().location().toString());
                                                    }
                                                    return builder.buildFuture();
                                                })
                                                .executes(ctx -> {
                                                    CommandSourceStack source = ctx.getSource();
                                                    ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                                    ResourceLocation dimLoc = ResourceLocationArgument.getId(ctx, "dimensionId");

                                                    target.getCapability(PlayerDimensionDataProvider.PLAYER_DIMENSION_DATA)
                                                            .ifPresent(playerData -> {
                                                                // Only remove if currently unlocked
                                                                if (playerData.isDimensionUnlocked(dimLoc.toString())) {
                                                                    playerData.getUnlockedDimensions()
                                                                            .remove(dimLoc.toString());
                                                                    source.sendSuccess(
                                                                            () -> Component.literal("Locked dimension ["
                                                                                    + dimLoc + "] for player "
                                                                                    + target.getName().getString()),
                                                                            true
                                                                    );
                                                                    // Update client UI
                                                                    NetworkHandler.openAmuletScreenForPlayer(target);
                                                                } else {
                                                                    source.sendFailure(
                                                                            Component.literal("Dimension [" + dimLoc
                                                                                    + "] was not unlocked for player "
                                                                                    + target.getName().getString())
                                                                    );
                                                                }
                                                            });

                                                    return Command.SINGLE_SUCCESS;
                                                })
                                        )
                                )
                        )
        );
    }
}