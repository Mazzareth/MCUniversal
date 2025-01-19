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

public class ExtraSpawnCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        // /extra_set_spawn <x> <y> <z>
        return Commands.literal("extra_set_spawn")
                .requires(src -> src.hasPermission(2)) // Admin-level permission
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(ctx -> {
                            BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
                            CommandSourceStack source = ctx.getSource();
                            ServerLevel level = source.getLevel();

                            // Acquire the data instance
                            ExtraSpawnsSavedData data = ExtraSpawnsSavedData.get(level);

                            // (Optional) Check if the player is in the "Extra" dimension:
                            // if (!level.dimension().location().toString().equals("my_mod:extra_dimension")) {
                            //     source.sendFailure(Component.literal("You must be in the Extra dimension to set spawn points."));
                            //     return 0;
                            // }

                            data.addSpawn(pos);

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