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
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        if (event.getTo().equals(Level.OVERWORLD)) {
            var pData = player.getPersistentData();
            boolean skipThisTime = pData.contains("mcuniversal:skipOverworldReturn");

            if (skipThisTime) {
                pData.remove("mcuniversal:skipOverworldReturn");  // Clear only AFTER confirming existence
                return;
            }

            // Existing teleport logic
            ServerLevel spawnLevel = player.server.getLevel(player.getRespawnDimension());
            if (spawnLevel == null) spawnLevel = player.server.getLevel(Level.OVERWORLD);

            BlockPos spawnPos = player.getRespawnPosition();
            if (spawnPos == null) spawnPos = spawnLevel.getSharedSpawnPos();

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