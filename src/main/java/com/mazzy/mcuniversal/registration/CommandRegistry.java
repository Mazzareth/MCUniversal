package com.mazzy.mcuniversal.registration;

import com.mazzy.mcuniversal.McUniversal;
import com.mazzy.mcuniversal.core.command.TestCommand;
import com.mazzy.mcuniversal.core.command.ExtraSpawnCommand;
import com.mazzy.mcuniversal.core.command.CheckExtraSpawnCommand;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = McUniversal.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommandRegistry {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        var dispatcher = event.getDispatcher();

        // Register existing commands
        TestCommand.register(dispatcher);

        // Register any other commands
        dispatcher.register(ExtraSpawnCommand.register());

        // Now also register "CheckExtraSpawnCommand"
        dispatcher.register(CheckExtraSpawnCommand.register());
    }
}