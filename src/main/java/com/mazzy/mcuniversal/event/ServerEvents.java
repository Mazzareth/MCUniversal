// File: ServerEvents.java
package com.mazzy.mcuniversal.event;

import com.mazzy.mcuniversal.McUniversal;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = McUniversal.MODID)
public class ServerEvents {

    /**
     * Runs when the server is starting.
     * You could call a DimensionFileHandler or handle region file setup here
     * if your mod needs it.
     */
    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        // Custom server-start logic can go here.
    }
}