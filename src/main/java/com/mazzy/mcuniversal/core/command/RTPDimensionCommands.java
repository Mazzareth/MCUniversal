package com.mazzy.mcuniversal.core.command;

import com.mazzy.mcuniversal.data.RTPDimensionData;
import com.mazzy.mcuniversal.data.PlayerDimensionDataProvider;
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

import java.util.Set;

public class RTPDimensionCommands {

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {

        // 1) /unlockdimensionrtp <dimensionId>
        dispatcher.register(
                Commands.literal("unlockdimensionrtp")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("dimensionId", ResourceLocationArgument.id())
                                .suggests((ctx, builder) -> {
                                    for (ServerLevel level : ctx.getSource().getServer().getAllLevels()) {
                                        builder.suggest(level.dimension().location().toString());
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    CommandSourceStack source = ctx.getSource();
                                    ResourceLocation dimLoc = ResourceLocationArgument.getId(ctx, "dimensionId");
                                    String dimAsString = dimLoc.toString();

                                    // Get the global data storage
                                    ServerLevel overworld = source.getServer().overworld();

                                    // Use .get(...) instead of a non-existent .getOrCreate(...)
                                    RTPDimensionData data = RTPDimensionData.get(overworld);
                                    Set<String> allowedDims = data.getUnlockedDimensions();

                                    // Try adding the dimension
                                    if (!allowedDims.contains(dimAsString)) {
                                        data.addDimension(dimAsString);
                                        source.sendSuccess(
                                                () -> Component.literal("Unlocked random-TP for dimension: " + dimAsString),
                                                true
                                        );
                                        return Command.SINGLE_SUCCESS;
                                    } else {
                                        source.sendFailure(
                                                Component.literal("Dimension is already unlocked: " + dimAsString)
                                        );
                                        return 0;
                                    }
                                })
                        )
                        // 2) /unlockdimensionrtp player <player> <dimensionId>
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
                                                                playerData.unlockDimension(dimLoc.toString());
                                                                source.sendSuccess(
                                                                        () -> Component.literal("Unlocked dimension ["
                                                                                + dimLoc + "] for player "
                                                                                + target.getName().getString()),
                                                                        true
                                                                );
                                                            });

                                                    return Command.SINGLE_SUCCESS;
                                                })
                                        )
                                )
                        )
        );

        // 3) /lockdimensionrtp <dimensionId>
        dispatcher.register(
                Commands.literal("lockdimensionrtp")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("dimensionId", ResourceLocationArgument.id())
                                .suggests((ctx, builder) -> {
                                    for (ServerLevel level : ctx.getSource().getServer().getAllLevels()) {
                                        builder.suggest(level.dimension().location().toString());
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    CommandSourceStack source = ctx.getSource();
                                    ResourceLocation dimLoc = ResourceLocationArgument.getId(ctx, "dimensionId");
                                    String dimAsString = dimLoc.toString();

                                    // Get the data storage as above
                                    ServerLevel overworld = source.getServer().overworld();

                                    // Use .get(...) instead of a non-existent .getOrCreate(...)
                                    RTPDimensionData data = RTPDimensionData.get(overworld);
                                    Set<String> allowedDims = data.getUnlockedDimensions();

                                    // Try removing the dimension
                                    if (allowedDims.contains(dimAsString)) {
                                        data.removeDimension(dimAsString);
                                        source.sendSuccess(
                                                () -> Component.literal("Locked random-TP for dimension: " + dimAsString),
                                                true
                                        );
                                        return Command.SINGLE_SUCCESS;
                                    } else {
                                        source.sendFailure(
                                                Component.literal("Dimension was not unlocked or is already locked: " + dimAsString)
                                        );
                                        return 0;
                                    }
                                })
                        )
                        // 4) /lockdimensionrtp player <player> <dimensionId>
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
                                                                    playerData.getUnlockedDimensions().remove(dimLoc.toString());
                                                                    source.sendSuccess(
                                                                            () -> Component.literal("Locked dimension ["
                                                                                    + dimLoc + "] for player "
                                                                                    + target.getName().getString()),
                                                                            true
                                                                    );
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