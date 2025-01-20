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
 * Forged event handler to register the capability, attach it to the player,
 * and optionally copy data on respawn if you want that behavior.
 */
@Mod.EventBusSubscriber(modid = McUniversal.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CapabilityEventHandler {

    /**
     * Tell Forge to register your IPlayerDimensionData capability interface.
     * This is required so the system knows about it at runtime.
     */
    @SubscribeEvent
    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(IPlayerDimensionData.class);
    }

    /**
     * Attach a new PlayerDimensionDataProvider to each player when they spawn in.
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
     * Copy the capability data to a new player instance if the old player is being "cloned"
     * (for example on death and respawn). If you don't want this behavior, you can omit this method.
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            Player oldPlayer = event.getOriginal();
            Player newPlayer = event.getEntity();

            // Temporarily revive capabilities so they can be read from the old player.
            oldPlayer.reviveCaps();

            oldPlayer.getCapability(PlayerDimensionDataProvider.PLAYER_DIMENSION_DATA).ifPresent(oldCap -> {
                newPlayer.getCapability(PlayerDimensionDataProvider.PLAYER_DIMENSION_DATA).ifPresent(newCap -> {
                    // Copy the unlocked dimensions from the old player to the new player
                    for (String dim : oldCap.getUnlockedDimensions()) {
                        newCap.unlockDimension(dim);
                    }
                });
            });

            // Clean up the old player's capabilities now that we're done copying.
            oldPlayer.invalidateCaps();
        }
    }
}