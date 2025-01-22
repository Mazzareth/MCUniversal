package com.mazzy.mcuniversal.event;

import com.mazzy.mcuniversal.data.ExtraSpawnsSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles automatic spawn assignment in a custom dimension for new players.
 * Manages first-time spawn allocations and prevents respawn point conflicts.
 */
@Mod.EventBusSubscriber
public class PlayerSpawnHandler {

    // Identifier for the custom dimension where spawns will be assigned
    private static final ResourceLocation EXTRA_DIM_ID = new ResourceLocation("mcuniversal", "extra");
    // Resource key for the custom dimension registration
    private static final ResourceKey<Level> EXTRA_DIM_KEY = ResourceKey.create(Registries.DIMENSION, EXTRA_DIM_ID);

    /**
     * Processes player logins to assign initial spawn points in the custom dimension.
     * @param event Triggered when a player successfully logs into the server
     */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // Skip players already set to respawn in the custom dimension
        if (player.getRespawnDimension().location().equals(EXTRA_DIM_ID)) {
            return;
        }

        // Check persistent data to prevent re-assignment
        CompoundTag persistentData = player.getPersistentData();
        if (persistentData.getBoolean("extraDimSpawnAssigned")) {
            return;
        }

        // Access the custom dimension world instance
        ServerLevel extraDimension = player.server.getLevel(EXTRA_DIM_KEY);
        if (extraDimension == null) {
            player.sendSystemMessage(
                    Component.literal("Extra dimension not found! Cannot assign spawn.")
            );
            return;
        }

        // Claim a spawn point from managed spawn data
        ExtraSpawnsSavedData spawnsData = ExtraSpawnsSavedData.get(extraDimension);
        ExtraSpawnsSavedData.SpawnEntry entry = spawnsData.claimFreeSpawn();
        if (entry == null) {
            player.sendSystemMessage(
                    Component.literal("No free spawns left in the Extra dimension.")
            );
            return;
        }

        // Configure player's spawn point
        BlockPos spawnPos = new BlockPos(entry.x, entry.y, entry.z);
        player.setRespawnPosition(
                extraDimension.dimension(),
                spawnPos,
                player.getYRot(),
                true,  // Force spawn position even if obstructed
                false  // Don't show respawn animation
        );

        // Teleport player to new spawn location with center-block alignment
        player.teleportTo(
                extraDimension,
                entry.x + 0.5,  // Center of block X
                entry.y + 0.1,  // Slightly above block Y to prevent clipping
                entry.z + 0.5,  // Center of block Z
                player.getYRot(),
                player.getXRot()
        );

        // Mark player as having received a spawn assignment
        persistentData.putBoolean("extraDimSpawnAssigned", true);

        // Notify player of their new spawn coordinates
        player.sendSystemMessage(
                Component.literal(
                        "Assigned Extra dimension spawn at: " +
                                spawnPos.getX() + " " + spawnPos.getY() + " " + spawnPos.getZ()
                )
        );
    }
}