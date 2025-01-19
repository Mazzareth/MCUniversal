/*******************************************************************************
 * OverworldReturnManager.java
 ******************************************************************************/
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
 * Redirects players to their spawn point whenever they move into the Overworld,
 * *unless* a temporary skip flag is set (for example, if they used the "Random TP
 * overworld" button). After skipping once, that flag is cleared.
 */
@Mod.EventBusSubscriber(modid = McUniversal.MODID)
public class OverworldReturnManager {

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        // Ensure we're on a server and the player is valid
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // If the dimension they arrived in is Overworld, check if we should skip
        if (Level.OVERWORLD.equals(event.getTo())) {
            // Read the skip flag from the player's persistent data
            var pData = player.getPersistentData();
            boolean skipThisTime = pData.getBoolean("mcuniversal:skipOverworldReturn");

            if (skipThisTime) {
                // Clear the flag and do nothing this time
                pData.remove("mcuniversal:skipOverworldReturn");
                return;
            }

            // The usual path: Teleport them to their spawn if not skipping
            var spawnDimensionKey = player.getRespawnDimension();
            ServerLevel spawnLevel = player.server.getLevel(spawnDimensionKey);
            if (spawnLevel == null) {
                // Fallback: If their spawn dimension isn't found for some reason, default to Overworld.
                spawnLevel = player.server.getLevel(Level.OVERWORLD);
            }

            // Retrieve the exact BlockPos of their spawn
            BlockPos spawnPos = player.getRespawnPosition();
            if (spawnPos == null) {
                // If no bed or specific spawn is set, default to world spawn
                spawnPos = spawnLevel.getSharedSpawnPos();
            }

            // Teleport them to the determined spawn
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