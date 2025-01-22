package com.mazzy.mcuniversal.network;

import com.mazzy.mcuniversal.data.PlayerDimensionDataProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles network communication setup and packet registration for the mod.
 * Manages both client-bound and server-bound packet routing.
 */
public class NetworkHandler {

    // Network protocol version for compatibility checking
    private static final String PROTOCOL_VERSION = "1";

    /**
     * Main network channel instance configured with:
     * - Channel ID: "mcuniversal:network"
     * - Protocol version checking
     * - Bi-directional communication
     */
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("mcuniversal", "network"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,  // Client version check
            PROTOCOL_VERSION::equals   // Server version check
    );

    // Auto-incrementing packet ID counter
    private static int packetId = 0;

    /** Registers all network packets in sequence */
    public static void register() {
        // GUI opening packet
        CHANNEL.registerMessage(
                packetId++,
                OpenDimensionalAmuletPacket.class,
                OpenDimensionalAmuletPacket::encode,
                OpenDimensionalAmuletPacket::decode,
                OpenDimensionalAmuletPacket::handle
        );

        // Amulet action packet
        CHANNEL.registerMessage(
                packetId++,
                DimensionalAmuletActionPacket.class,
                DimensionalAmuletActionPacket::encode,
                DimensionalAmuletActionPacket::decode,
                DimensionalAmuletActionPacket::handle
        );

        // Waypoint management packet
        CHANNEL.registerMessage(
                packetId++,
                GlobalWaypointsPacket.class,
                GlobalWaypointsPacket::encode,
                GlobalWaypointsPacket::decode,
                GlobalWaypointsPacket::handle
        );

        // Data synchronization packet
        CHANNEL.registerMessage(
                packetId++,
                SyncDimensionalDataPacket.class,
                SyncDimensionalDataPacket::encode,
                SyncDimensionalDataPacket::decode,
                SyncDimensionalDataPacket::handle
        );
    }

    /** Sends a packet to a specific player (server -> client) */
    public static void sendToPlayer(Object message, ServerPlayer player) {
        CHANNEL.sendTo(message, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    /** Sends a packet to the server (client -> server) */
    public static void sendToServer(Object message) {
        CHANNEL.sendToServer(message);
    }

    /**
     * Initiates dimensional amulet GUI opening sequence
     * @param player Target player to receive the GUI
     */
    public static void openAmuletScreenForPlayer(ServerPlayer player) {
        player.getCapability(PlayerDimensionDataProvider.PLAYER_DIMENSION_DATA).ifPresent(cap -> {
            // Collect unlocked dimensions and send GUI data
            List<String> dimIds = new ArrayList<>(cap.getUnlockedDimensions());

            // Explicitly uses OpenDimensionalAmuletPacket instead of sync packet
            // to trigger GUI opening on client side
            OpenDimensionalAmuletPacket packet = new OpenDimensionalAmuletPacket(dimIds);
            sendToPlayer(packet, player);
        });
    }
}