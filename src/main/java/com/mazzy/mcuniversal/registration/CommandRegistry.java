package com.mazzy.mcuniversal.registration;

import com.mazzy.mcuniversal.McUniversal;
import com.mazzy.mcuniversal.core.command.TestCommand;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber(modid = McUniversal.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommandRegistry {
    /**
     * Called by forge when it's time to register commands (automatically)
     *
     * @param event the RegisterCommandsEvent provided by forge
     */

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        var dispatcher = event.getDispatcher();
        // Register Commands Here
        TestCommand.register(dispatcher);
    }
}
