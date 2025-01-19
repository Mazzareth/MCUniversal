package com.mazzy.mcuniversal.core.command;

import com.mazzy.mcuniversal.config.DimensionConfig;
import com.mazzy.mcuniversal.data.IPlayerDimensionData;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Commands to lock/unlock dimensions for random-TP usage (server-wide via config),
 * plus unlocking/locking them on a per-player basis via capabilities.
 */
public class RTPDimensionCommands {

    /**
     * Registers:
     *
     * 1) /unlockdimensionrtp <dimensionId>
     *    (Adds the dimension to global whitelist)
     * 2) /unlockdimensionrtp player <player> <dimensionId>
     *    (Unlocks dimension for that player's personal data)
     *
     * 3) /lockdimensionrtp <dimensionId>
     *    (Removes the dimension from global whitelist)
     * 4) /lockdimensionrtp player <player> <dimensionId>
     *    (Locks dimension for that player's personal data)
     *
     * @param dispatcher the active CommandDispatcher
     */
    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {

        //--------------------------------------------------------------------------------
        // 1) Global dimension unlocking
        //--------------------------------------------------------------------------------
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

                                    List<? extends String> currentList = DimensionConfig.SERVER.dimensionWhitelist.get();
                                    List<String> mutableList = new ArrayList<>(currentList);

                                    String dimAsString = dimLoc.toString();
                                    if (!mutableList.contains(dimAsString)) {
                                        mutableList.add(dimAsString);
                                        DimensionConfig.SERVER.dimensionWhitelist.set(mutableList);
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
                        //--------------------------------------------------------------------------------
                        // 2) Per-player dimension unlocking
                        //--------------------------------------------------------------------------------
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
                                                            .ifPresent(data -> {
                                                                data.unlockDimension(dimLoc.toString());
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

        //--------------------------------------------------------------------------------
        // 3) Global dimension locking
        //--------------------------------------------------------------------------------
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

                                    List<? extends String> currentList = DimensionConfig.SERVER.dimensionWhitelist.get();
                                    List<String> mutableList = new ArrayList<>(currentList);

                                    String dimAsString = dimLoc.toString();
                                    if (mutableList.contains(dimAsString)) {
                                        mutableList.remove(dimAsString);
                                        DimensionConfig.SERVER.dimensionWhitelist.set(mutableList);
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
                        //--------------------------------------------------------------------------------
                        // 4) Per-player dimension locking
                        //--------------------------------------------------------------------------------
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
                                                            .ifPresent(data -> {
                                                                if (data.isDimensionUnlocked(dimLoc.toString())) {
                                                                    data.getUnlockedDimensions().remove(dimLoc.toString());
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