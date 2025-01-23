package com.mazzy.mcuniversal.event;

import com.mazzy.mcuniversal.McUniversal;
import com.mazzy.mcuniversal.data.IPlayerDimensionData;
import com.mazzy.mcuniversal.data.PlayerDimensionDataProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles capability-related events for player dimension data.
 * Manages capability registration, attachment to players, and data persistence across deaths.
 */
@Mod.EventBusSubscriber(modid = McUniversal.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CapabilityEventHandler {

    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(IPlayerDimensionData.class);
    }

    @SubscribeEvent
    public static void onAttachCapabilitiesPlayer(AttachCapabilitiesEvent<?> event) {
        if (event.getObject() instanceof Player) {
            PlayerDimensionDataProvider provider = new PlayerDimensionDataProvider();
            event.addCapability(
                    new ResourceLocation(McUniversal.MODID, "player_dimension_data"),
                    provider
            );
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            Player oldPlayer = event.getOriginal();
            Player newPlayer = event.getEntity();

            // Copy persistent NBT data (including home position)
            CompoundTag oldData = oldPlayer.getPersistentData();
            newPlayer.getPersistentData().merge(oldData);

            // Copy capability data
            oldPlayer.reviveCaps();
            oldPlayer.getCapability(PlayerDimensionDataProvider.PLAYER_DIMENSION_DATA).ifPresent(oldCap -> {
                newPlayer.getCapability(PlayerDimensionDataProvider.PLAYER_DIMENSION_DATA).ifPresent(newCap -> {
                    for (String dim : oldCap.getUnlockedDimensions()) {
                        newCap.unlockDimension(dim);
                    }
                });
            });
            oldPlayer.invalidateCaps();
        }
    }
}