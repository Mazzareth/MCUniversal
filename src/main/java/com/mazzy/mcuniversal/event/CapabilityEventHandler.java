package com.mazzy.mcuniversal.event;

import com.mazzy.mcuniversal.McUniversal;
import com.mazzy.mcuniversal.data.IPlayerDimensionData;
import com.mazzy.mcuniversal.data.PlayerDimensionDataProvider;
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

    /**
     * Registers the dimension data capability with Forge's capability system.
     * @param event RegisterCapabilitiesEvent provided by Forge
     */
    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(IPlayerDimensionData.class);
    }

    /**
     * Attaches dimension data capability to all Player entities.
     * @param event Fired when any entity's capabilities are being attached
     */
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

    /**
     * Copies dimension access data when a player respawns after death.
     * Preserves unlocked dimensions between player instances.
     * @param event Player cloning event containing original and new player references
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            Player oldPlayer = event.getOriginal();
            Player newPlayer = event.getEntity();

            // Temporarily revive old player's capabilities for data access
            oldPlayer.reviveCaps();

            oldPlayer.getCapability(PlayerDimensionDataProvider.PLAYER_DIMENSION_DATA).ifPresent(oldCap -> {
                newPlayer.getCapability(PlayerDimensionDataProvider.PLAYER_DIMENSION_DATA).ifPresent(newCap -> {
                    // Transfer all unlocked dimensions to new player instance
                    for (String dim : oldCap.getUnlockedDimensions()) {
                        newCap.unlockDimension(dim);
                    }
                });
            });

            // Clean up old player's capabilities after data transfer
            oldPlayer.invalidateCaps();
        }
    }
}