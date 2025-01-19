package com.mazzy.mcuniversal;

import com.mazzy.mcuniversal.config.DimensionConfig;
import com.mazzy.mcuniversal.network.NetworkHandler;
import com.mazzy.mcuniversal.registration.RegistryHandler;
import com.mazzy.mcuniversal.event.ChatMessageHandler;
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

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, DimensionConfig.SERVER_SPEC);

        RegistryHandler.ITEMS.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);

        NetworkHandler.register();

        new ChatMessageHandler();

        LOGGER.info("McUniversal mod initialized");
    }
}