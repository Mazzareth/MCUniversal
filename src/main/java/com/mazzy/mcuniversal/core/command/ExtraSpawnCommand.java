package com.mazzy.mcuniversal.core.command;

import com.mazzy.mcuniversal.data.ExtraSpawnsSavedData;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/**
 * Command for administrators to set custom spawn points in special dimensions
 */
public class ExtraSpawnCommand {

    /**
     * Registers command with server
     * @return Configured command structure
     */
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("extra_set_spawn")
                .requires(src -> src.hasPermission(2)) // Restricted to server operators
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(ctx -> {
                            // Get validated block position from context
                            BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
                            CommandSourceStack source = ctx.getSource();
                            ServerLevel level = source.getLevel();

                            // Access persistent spawn storage
                            ExtraSpawnsSavedData data = ExtraSpawnsSavedData.get(level);

                            // Store new spawn point
                            data.addSpawn(pos);

                            // Confirm creation to operator
                            source.sendSuccess(
                                    () -> Component.literal(
                                            "Spawn point added at " + pos + " in dimension " + level.dimension().location()
                                    ),
                                    true
                            );

                            return Command.SINGLE_SUCCESS;
                        })
                );
    }
}