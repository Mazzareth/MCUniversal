package com.mazzy.mcuniversal.event;

import com.mazzy.mcuniversal.data.NationsSavedData;
import com.mazzy.mcuniversal.data.NationsSavedData.NationEntry;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;

/**
 * Handles chat messages by decorating them with nation affiliation tags.
 * Integrates with Forge's event system to modify chat messages in real-time.
 */
public class ChatMessageHandler {

    /**
     * Registers this handler with Forge's event bus automatically on instantiation
     */
    public ChatMessageHandler() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    /**
     * Processes server chat events to add nation prefixes to messages.
     * <p>
     * When a player in a nation sends a chat message:
     * 1. Looks up player's nation affiliation
     * 2. Prepends nation tag in format: "[NationName] - PlayerName message"
     * 3. Broadcasts modified message to all players
     * 4. Cancels original plain message
     *
     * @param event The chat event containing player and message data
     */
    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        Level level = player.level();

        // Only process on server side with valid world data
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            NationsSavedData nationsData = NationsSavedData.get(serverLevel);
            NationEntry nation = nationsData.getNationByMember(player.getUUID());

            if (nation != null) {
                String nationName = nation.getName();
                String playerName = player.getName().getString();
                String originalMessage = event.getMessage().getString();

                // Create nation-tagged message format
                String newMessage = "[" + nationName + "] - " + playerName + " " + originalMessage;

                // Prevent original message from being sent
                event.setCanceled(true);

                // Broadcast modified message to all players
                serverLevel.getServer().getPlayerList().broadcastSystemMessage(
                        Component.literal(newMessage),
                        false
                );
            }
        }
    }
}