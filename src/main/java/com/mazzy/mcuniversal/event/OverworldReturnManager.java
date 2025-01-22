/*******************************************************************************
 * Manages player returns to the Overworld by enforcing spawn point teleportation
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

@Mod.EventBusSubscriber(modid = McUniversal.MODID)
public class OverworldReturnManager {

    /**
     * Handles dimension changes to enforce controlled Overworld returns
     * @param event Fired when a player changes dimensions
     */
    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        // Only process server-side player entities
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // Trigger only when entering Overworld
        if (event.getTo().equals(Level.OVERWORLD)) {
            // Check for temporary bypass flag in player's persistent data
            var pData = player.getPersistentData();
            boolean skipThisTime = pData.contains("mcuniversal:skipOverworldReturn");

            if (skipThisTime) {
                // Single-use flag removal for controlled bypass
                pData.remove("mcuniversal:skipOverworldReturn");
                return;
            }

            // Determine valid spawn dimension with fallbacks:
            // 1. Player's set respawn dimension
            // 2. Overworld (final fallback)
            ServerLevel spawnLevel = player.server.getLevel(player.getRespawnDimension());
            if (spawnLevel == null) spawnLevel = player.server.getLevel(Level.OVERWORLD);

            // Get spawn coordinates with fallback to world spawn
            BlockPos spawnPos = player.getRespawnPosition();
            if (spawnPos == null) spawnPos = spawnLevel.getSharedSpawnPos();

            // Teleport player to spawn coordinates with center-block alignment:
            // - X/Z +0.5D positions player at block center
            // - Y +0.1D prevents falling through partial blocks
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