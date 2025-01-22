package com.mazzy.mcuniversal.core.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import com.mazzy.mcuniversal.config.teleport.TeleportConstants;
import com.mazzy.mcuniversal.core.teleport.TeleportManager;
import net.minecraft.core.registries.Registries;

public class AmuletActionHandlers {


    public static void handleRandWarp(ServerPlayer player, MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld != null) {
            TeleportManager.startPhasedTeleport(player, overworld, "minecraft:overworld");
        } else {
            player.sendSystemMessage(Component.literal("Overworld not found!"));
        }
    }

    public static void handleRandomTpDim(ServerPlayer player, MinecraftServer server, String dimId) {
        ResourceKey<Level> targetKey = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(dimId));
        ServerLevel targetLevel = server.getLevel(targetKey);

        if (targetLevel == null) {
            player.sendSystemMessage(Component.literal("Dimension not found: " + dimId));
            return;
        }

        TeleportManager.startPhasedTeleport(player, targetLevel, dimId);
    }


    public static void handleSetHome(ServerPlayer player, CompoundTag pData, MinecraftServer server) {
        if (!player.level().dimension().location().equals(TeleportConstants.EARTH_DIM_LOCATION)) {
            player.sendSystemMessage(Component.literal("You must be in 'mcuniversal:extra' (Earth) to set your home!"));
            return;
        }

        final long cooldownTicks = 72000;
        long lastSetHomeTime = pData.getLong("mcuniversal:lastSetHomeTime");
        long currentTime = player.level().getGameTime();

        if (lastSetHomeTime != 0 && (currentTime - lastSetHomeTime) < cooldownTicks) {
            long secondsLeft = (cooldownTicks - (currentTime - lastSetHomeTime)) / 20;
            player.sendSystemMessage(Component.literal("Wait " + secondsLeft + " seconds before setting home again."));
            return;
        }

        BlockPos pos = player.blockPosition();
        pData.putInt("mcuniversal:homeX", pos.getX());
        pData.putInt("mcuniversal:homeY", pos.getY());
        pData.putInt("mcuniversal:homeZ", pos.getZ());
        pData.putString("mcuniversal:homeDim", TeleportConstants.EARTH_DIM_LOCATION.toString());
        pData.putLong("mcuniversal:lastSetHomeTime", currentTime);

        ServerLevel earthDim = server.getLevel(TeleportConstants.EARTH_DIM_KEY);
        if (earthDim != null) {
            player.setRespawnPosition(earthDim.dimension(), pos, player.getYRot(), true, false);
            player.sendSystemMessage(Component.literal("Home set at: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()));
        } else {
            player.sendSystemMessage(Component.literal("Failed to set home - Earth dimension unavailable!"));
        }
    }

    public static void handleTeleportHome(ServerPlayer player, CompoundTag pData, MinecraftServer server) {
        if (!pData.contains("mcuniversal:homeX")) {
            player.sendSystemMessage(Component.literal("No home set!"));
            return;
        }

        ServerLevel earthDim = server.getLevel(TeleportConstants.EARTH_DIM_KEY);
        if (earthDim == null) {
            player.sendSystemMessage(Component.literal("Earth dimension missing!"));
            return;
        }

        BlockPos homePos = new BlockPos(
                pData.getInt("mcuniversal:homeX"),
                pData.getInt("mcuniversal:homeY"),
                pData.getInt("mcuniversal:homeZ")
        );

        player.teleportTo(earthDim,
                homePos.getX() + 0.5,
                homePos.getY() + 0.1,
                homePos.getZ() + 0.5,
                player.getYRot(),
                player.getXRot()
        );
        player.sendSystemMessage(Component.literal("Teleported home!"));
    }

}