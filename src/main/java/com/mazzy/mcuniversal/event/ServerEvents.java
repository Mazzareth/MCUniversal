package com.mazzy.mcuniversal.event;

import com.mazzy.mcuniversal.McUniversal;
import com.mazzy.mcuniversal.core.dimension.DimensionFileHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = McUniversal.MODID)
public class ServerEvents {

    /**
     * Runs when the server is starting.
     * Calls our DimensionFileHandler to perform the region file setup.
     */
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        DimensionFileHandler.setupDimensionFiles(server);
    }
}