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
 * Commands related to unlocking/locking dimensions for Random-TP
 * at a per-player level only.
 */
public class RTPDimensionCommands {

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {

        // /unlockdimensionrtp player <player> <dimensionId>
        dispatcher.register(
                Commands.literal("unlockdimensionrtp")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("player")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .then(Commands.argument("dimensionId", ResourceLocationArgument.id())
                                                .suggests((ctx, builder) -> {
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
                                                                // Unlock the dimension for this player
                                                                playerData.unlockDimension(dimLoc.toString());
                                                                source.sendSuccess(
                                                                        () -> Component.literal("Unlocked dimension ["
                                                                                + dimLoc + "] for player "
                                                                                + target.getName().getString()),
                                                                        true
                                                                );
                                                                // Notify the client to reopen the amulet screen
                                                                NetworkHandler.openAmuletScreenForPlayer(target);
                                                            });

                                                    return Command.SINGLE_SUCCESS;
                                                })
                                        )
                                )
                        )
        );

        // /lockdimensionrtp player <player> <dimensionId>
        dispatcher.register(
                Commands.literal("lockdimensionrtp")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("player")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .then(Commands.argument("dimensionId", ResourceLocationArgument.id())
                                                .suggests((ctx, builder) -> {
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
                                                                if (playerData.isDimensionUnlocked(dimLoc.toString())) {
                                                                    playerData.getUnlockedDimensions()
                                                                            .remove(dimLoc.toString());
                                                                    source.sendSuccess(
                                                                            () -> Component.literal("Locked dimension ["
                                                                                    + dimLoc + "] for player "
                                                                                    + target.getName().getString()),
                                                                            true
                                                                    );
                                                                    // Reopen the screen for the player
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