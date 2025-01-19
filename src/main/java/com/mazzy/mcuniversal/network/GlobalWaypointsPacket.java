package com.mazzy.mcuniversal.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

/**
 * A separate packet for global (public) waypoints:
 * e.g., create, teleport, remove, and so on.
 */
public class GlobalWaypointsPacket {

    public enum WaypointAction {
        CREATE,
        TELEPORT,
        REMOVE
    }

    private final WaypointAction action;
    private final String waypointName;  // Name or ID for the waypoint

    public GlobalWaypointsPacket(WaypointAction action, String waypointName) {
        this.action = action;
        this.waypointName = waypointName;
    }

    public static void encode(GlobalWaypointsPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.action.ordinal());
        buf.writeUtf(packet.waypointName);
    }

    public static GlobalWaypointsPacket decode(FriendlyByteBuf buf) {
        WaypointAction action = WaypointAction.values()[buf.readInt()];
        String name = buf.readUtf(32767);
        return new GlobalWaypointsPacket(action, name);
    }

    public static void handle(GlobalWaypointsPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        if (!ctx.getDirection().getReceptionSide().isServer()) {
            return; // Ensure it's handled on the server
        }
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            switch (packet.action) {
                case CREATE -> {
                    player.sendSystemMessage(Component.literal("Created waypoint: " + packet.waypointName));
                    // Actual creation logic
                }
                case TELEPORT -> {
                    player.sendSystemMessage(Component.literal("Teleport to waypoint: " + packet.waypointName));
                    // Actual teleport logic
                }
                case REMOVE -> {
                    player.sendSystemMessage(Component.literal("Removed waypoint: " + packet.waypointName));
                    // Actual removal logic
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}