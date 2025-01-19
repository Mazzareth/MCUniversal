package com.mazzy.mcuniversal.event;

import com.mazzy.mcuniversal.McUniversal;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Redirects players to their spawn point whenever they move into the Overworld.
 * That spawn point is the same one they'd use if they died (a bed, anchor, or
 * the world spawn if no bed / anchor is set).
 */
@Mod.EventBusSubscriber(modid = McUniversal.MODID)
public class OverworldReturnManager {

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        // Ensure we're on a server and the player is valid
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // If the dimension they arrived in is Overworld, redirect them
        if (Level.OVERWORLD.equals(event.getTo())) {
            // The "home" dimension is whichever dimension the player has as their
            // respawn dimension (commonly Overworld, but can be others).
            var spawnDimensionKey = player.getRespawnDimension();
            ServerLevel spawnLevel = player.server.getLevel(spawnDimensionKey);
            if (spawnLevel == null) {
                // Fallback: If their spawn dimension isn't found for some reason,
                // default to Overworld.
                spawnLevel = player.server.getLevel(Level.OVERWORLD);
            }

            // Retrieve the exact BlockPos of their spawn
            BlockPos spawnPos = player.getRespawnPosition();
            if (spawnPos == null) {
                // If no bed or specific spawn is set, default to world spawn
                // of the spawn dimension.
                spawnPos = spawnLevel.getSharedSpawnPos();
            }

            // Teleport them to the determined spawn
            // Be sure to add offsets (0.5, etc.) so the player isn’t stuck in a block.
            player.teleportTo(
                    spawnLevel,
                    spawnPos.getX() + 0.5D,
                    spawnPos.getY() + 0.1D,
                    spawnPos.getZ() + 0.5D,
                    player.getYRot(),
                    player.getXRot()
            );
        }
    }
}