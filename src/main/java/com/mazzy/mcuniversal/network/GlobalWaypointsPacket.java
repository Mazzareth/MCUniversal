package com.mazzy.mcuniversal.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import java.util.function.Supplier;

/**
 * Handles network communication for global waypoint management.
 * Manages creation, teleportation, and removal of persistent world locations.
 * Currently contains placeholder logic awaiting implementation.
 */
public class GlobalWaypointsPacket {

    /** Available operations for waypoint management */
    public enum WaypointAction {
        CREATE,    // Register new waypoint
        TELEPORT,  // Move player to waypoint
        REMOVE     // Delete existing waypoint
    }

    private final WaypointAction action;
    private final String waypointName;

    /**
     * @param action Operation to perform (CREATE/TELEPORT/REMOVE)
     * @param waypointName Unique identifier for the waypoint
     */
    public GlobalWaypointsPacket(WaypointAction action, String waypointName) {
        this.action = action;
        this.waypointName = waypointName;
    }

    /** Serializes packet data for network transmission */
    public static void encode(GlobalWaypointsPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.action.ordinal());
        buf.writeUtf(packet.waypointName);
    }

    /** Deserializes packet data from network transmission */
    public static GlobalWaypointsPacket decode(FriendlyByteBuf buf) {
        return new GlobalWaypointsPacket(
                WaypointAction.values()[buf.readInt()],  // Convert ordinal to enum
                buf.readUtf(32767)                       // Read UTF string with max length
        );
    }

    /**
     * Processes received packet on server-side
     * @implNote Current implementation shows placeholder messages -
     *           actual waypoint storage/retrieval logic needs implementation
     */
    public static void handle(GlobalWaypointsPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        // Only process on server side
        if (!ctx.getDirection().getReceptionSide().isServer()) {
            return;
        }

        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            // TODO: Implement actual waypoint storage/retrieval system
            switch (packet.action) {
                case CREATE -> {
                    // Planned: Store position with waypointName
                    player.sendSystemMessage(Component.literal("Created waypoint: " + packet.waypointName));
                }
                case TELEPORT -> {
                    // Planned: Retrieve and teleport to stored position
                    player.sendSystemMessage(Component.literal("Teleport to waypoint: " + packet.waypointName));
                }
                case REMOVE -> {
                    // Planned: Delete stored waypoint data
                    player.sendSystemMessage(Component.literal("Removed waypoint: " + packet.waypointName));
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}