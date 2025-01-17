package com.mazzy.mcuniversal.core.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import static net.minecraft.commands.Commands.literal;

public class TestCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("testcommand")
                .executes(commandContext -> {
                    commandContext.getSource().sendSuccess(() -> Component.literal("Test command executed"), true);
                    return 1;
                })
        );
    }
}