package com.mazzy.mcuniversal.network;

import com.mazzy.mcuniversal.data.PlayerDimensionDataProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.List;

public class NetworkHandler {

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("mcuniversal", "network"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        // Register the OpenDimensionalAmuletPacket (handles opening the DimensionalAmuletScreen)
        CHANNEL.registerMessage(
                packetId++,
                OpenDimensionalAmuletPacket.class,
                OpenDimensionalAmuletPacket::encode,
                OpenDimensionalAmuletPacket::decode,
                OpenDimensionalAmuletPacket::handle
        );

        // Register the multi-purpose "DimensionalAmuletActionPacket"
        CHANNEL.registerMessage(
                packetId++,
                DimensionalAmuletActionPacket.class,
                DimensionalAmuletActionPacket::encode,
                DimensionalAmuletActionPacket::decode,
                DimensionalAmuletActionPacket::handle
        );

        // Register a separate "GlobalWaypointsPacket" for public waypoints
        CHANNEL.registerMessage(
                packetId++,
                GlobalWaypointsPacket.class,
                GlobalWaypointsPacket::encode,
                GlobalWaypointsPacket::decode,
                GlobalWaypointsPacket::handle
        );

        // Register SyncDimensionalDataPacket for other potential dimension sync needs
        CHANNEL.registerMessage(
                packetId++,
                SyncDimensionalDataPacket.class,
                SyncDimensionalDataPacket::encode,
                SyncDimensionalDataPacket::decode,
                SyncDimensionalDataPacket::handle
        );
    }

    /**
     * Send a message to the specified player (client side).
     */
    public static void sendToPlayer(Object message, ServerPlayer player) {
        CHANNEL.sendTo(message, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    /**
     * Convenience method for sending messages from client -> server.
     */
    public static void sendToServer(Object message) {
        CHANNEL.sendToServer(message);
    }

    /**
     * Gather the player's unlocked dimensions, then open the DimensionalAmuletScreen
     * on the client, passing along the updated list.
     */
    public static void openAmuletScreenForPlayer(ServerPlayer player) {
        player.getCapability(PlayerDimensionDataProvider.PLAYER_DIMENSION_DATA).ifPresent(cap -> {
            List<String> dimIds = new ArrayList<>(cap.getUnlockedDimensions());
            // Use OpenDimensionalAmuletPacket instead of SyncDimensionalDataPacket
            OpenDimensionalAmuletPacket packet = new OpenDimensionalAmuletPacket(dimIds);
            sendToPlayer(packet, player);
        });
    }
}