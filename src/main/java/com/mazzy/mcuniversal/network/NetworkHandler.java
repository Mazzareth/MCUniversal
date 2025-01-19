package com.mazzy.mcuniversal.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

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
        // Existing registration for opening the DimensionalAmuletScreen
        CHANNEL.registerMessage(
                packetId++,
                OpenDimensionalAmuletPacket.class,
                OpenDimensionalAmuletPacket::encode,
                OpenDimensionalAmuletPacket::decode,
                OpenDimensionalAmuletPacket::handle
        );

        // Register the new multi-purpose "DimensionalAmuletActionPacket"
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
}