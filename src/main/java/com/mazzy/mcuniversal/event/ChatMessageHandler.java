package com.mazzy.mcuniversal.event;

import com.mazzy.mcuniversal.data.NationsSavedData;
import com.mazzy.mcuniversal.data.NationsSavedData.NationEntry;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;

public class ChatMessageHandler {

    public ChatMessageHandler() {
        // Register this handler on the EventBus
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        Level level = player.level();
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {

            // Retrieve your NationsSavedData, to find the player's nation
            NationsSavedData nationsData = NationsSavedData.get(serverLevel);
            NationEntry nation = nationsData.getNationByMember(player.getUUID());

            // If the player is in a nation, prepend the nation name to chat
            if (nation != null) {
                String nationName = nation.getName();
                String playerName = player.getName().getString();
                String originalMessage = event.getMessage().getString();
                // Build your custom chat text
                // [Nation_Name] - [Player Name] "Message Content!"
                String newMessage = "[" + nationName + "] - " + playerName + " " + originalMessage;

                // Cancel the event's default chat handling
                event.setCanceled(true);

                // Broadcast our own message (using a component) to all players
                serverLevel.getServer().getPlayerList().broadcastSystemMessage(Component.literal(newMessage), false);
            }
        }
    }
}