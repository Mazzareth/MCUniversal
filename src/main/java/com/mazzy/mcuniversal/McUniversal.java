package com.mazzy.mcuniversal;

import com.mazzy.mcuniversal.registration.RegistryHandler;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(McUniversal.MODID)
public class McUniversal {

    public static final String MODID = "mcuniversal";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public McUniversal(FMLJavaModLoadingContext modEventBus) {
        IEventBus bus = modEventBus.getModEventBus();
        bus.register(this);
        RegistryHandler.ITEMS.register(bus);
    }
}