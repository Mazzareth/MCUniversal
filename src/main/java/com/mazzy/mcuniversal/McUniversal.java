package com.mazzy.mcuniversal;

import com.mazzy.mcuniversal.config.DimensionConfig;
import com.mazzy.mcuniversal.network.NetworkHandler;
import com.mazzy.mcuniversal.registration.RegistryHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(McUniversal.MODID)
public class McUniversal {
    public static final String MODID = "mcuniversal";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    @SuppressWarnings("removal")
    public McUniversal() {
        // Access the mod-specific event bus
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register your configuration settings
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, DimensionConfig.SERVER_SPEC);

        // Register custom items or other mod-specific objects to the mod event bus
        RegistryHandler.ITEMS.register(modEventBus);

        // Register on the global event bus if needed to handle common game events
        MinecraftForge.EVENT_BUS.register(this);

        // Register your network packets
        NetworkHandler.register();

        LOGGER.info("McUniversal mod initialized");
    }
}