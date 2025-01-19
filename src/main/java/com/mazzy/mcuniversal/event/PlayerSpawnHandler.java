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

@Mod.EventBusSubscriber
public class PlayerSpawnHandler {

    private static final ResourceLocation EXTRA_DIM_ID = new ResourceLocation("mcuniversal", "extra");
    private static final ResourceKey<Level> EXTRA_DIM_KEY = ResourceKey.create(Registries.DIMENSION, EXTRA_DIM_ID);

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // 1) If the player already has a forced spawn in the Extra dimension, skip
        if (player.getRespawnDimension().location().equals(EXTRA_DIM_ID)) {
            return;
        }

        /*
         * 2) Check the player’s persistent data to see if we've already assigned
         *    an Extra dimension spawn. If so, skip.
         */
        CompoundTag persistentData = player.getPersistentData();
        if (persistentData.getBoolean("extraDimSpawnAssigned")) {
            return;
        }

        // 3) Retrieve the Extra dimension
        ServerLevel extraDimension = player.server.getLevel(EXTRA_DIM_KEY);
        if (extraDimension == null) {
            player.sendSystemMessage(
                    Component.literal("Extra dimension not found! Cannot assign spawn.")
            );
            return;
        }

        // 4) Claim (and thereby remove) a free spawn from the pool
        ExtraSpawnsSavedData spawnsData = ExtraSpawnsSavedData.get(extraDimension);
        ExtraSpawnsSavedData.SpawnEntry entry = spawnsData.claimFreeSpawn();
        if (entry == null) {
            player.sendSystemMessage(
                    Component.literal("No free spawns left in the Extra dimension.")
            );
            return;
        }

        // 5) Assign the player's respawn point in the Extra dimension
        BlockPos spawnPos = new BlockPos(entry.x, entry.y, entry.z);
        player.setRespawnPosition(
                extraDimension.dimension(),
                spawnPos,
                player.getYRot(),
                true,
                false
        );

        // 6) Teleport them right away if desired
        player.teleportTo(
                extraDimension,
                entry.x + 0.5,
                entry.y + 0.1,
                entry.z + 0.5,
                player.getYRot(),
                player.getXRot()
        );

        // 7) Mark their spawn as assigned in their persistent data
        persistentData.putBoolean("extraDimSpawnAssigned", true);

        // 8) Notify the player
        player.sendSystemMessage(
                Component.literal(
                        "Assigned Extra dimension spawn at: " +
                                spawnPos.getX() + " " + spawnPos.getY() + " " + spawnPos.getZ()
                )
        );
    }
}