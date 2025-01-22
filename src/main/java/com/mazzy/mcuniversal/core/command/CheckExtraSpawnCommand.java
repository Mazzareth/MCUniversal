package com.mazzy.mcuniversal.core.command;

import com.mazzy.mcuniversal.data.ExtraSpawnsSavedData;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Command for checking available spawn points in custom dimension
 * Requires OP level 2 to prevent abuse
 */
public class CheckExtraSpawnCommand {

    // Custom dimension resource identifiers
    private static final ResourceLocation EXTRA_LOCATION = new ResourceLocation("mcuniversal", "extra");
    private static final ResourceKey<Level> EXTRA_DIM_KEY =
            ResourceKey.create(Registries.DIMENSION, EXTRA_LOCATION);

    /**
     * Registers the command with server
     * @return Configured command literal
     */
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("checkextraspawn")
                .requires(src -> src.hasPermission(2)) // Restrict to server operators
                .executes(ctx -> {
                    CommandSourceStack source = ctx.getSource();

                    // Validate command executor is a player
                    ServerPlayer player;
                    try {
                        player = source.getPlayerOrException();
                    } catch (Exception e) {
                        source.sendFailure(Component.literal("This command can only be used by a player."));
                        return 0;
                    }

                    // Verify custom dimension exists
                    ServerLevel extraDimension = player.getServer().getLevel(EXTRA_DIM_KEY);
                    if (extraDimension == null) {
                        source.sendFailure(Component.literal("Extra dimension not found."));
                        return 0;
                    }

                    // Retrieve and display spawn data
                    ExtraSpawnsSavedData data = ExtraSpawnsSavedData.get(extraDimension);
                    var spawnEntry = data.peekFreeSpawn();
                    if (spawnEntry == null) {
                        source.sendSuccess(() -> Component.literal("No free spawn found."), false);
                    } else {
                        String coords = spawnEntry.x + " " + spawnEntry.y + " " + spawnEntry.z;
                        source.sendSuccess(() -> Component.literal("Next free spawn is at: " + coords), false);
                    }

                    return 1;
                });
    }
}