package com.mazzy.mcuniversal.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

/**
 * A multi-purpose packet handling "My Nation" and "Teleports" actions
 * via an enum. Each button in the DimensionalAmuletScreen can send
 * an instance of this packet to the server to perform the desired action.
 */
public class DimensionalAmuletActionPacket {

    public enum Action {
        // --- My Nation ---
        SET_HOME,
        TELEPORT_HOME,
        SET_NATION_NAME,

        // --- Teleports ---
        RAND_WARP,
        TP_EARTH
    }

    private final Action action;

    // Example of optional extra data if desired (e.g., a String for the nation's name)
    private final String data;

    /**
     * Main constructor for actions that might need extra data.
     */
    public DimensionalAmuletActionPacket(Action action, String data) {
        this.action = action;
        this.data = data;
    }

    /**
     * Simplified constructor for actions without extra data.
     */
    public DimensionalAmuletActionPacket(Action action) {
        this(action, "");
    }

    /**
     * Encode data into the buffer.
     */
    public static void encode(DimensionalAmuletActionPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.action.ordinal());
        buf.writeUtf(packet.data);
    }

    /**
     * Decode data from the buffer.
     */
    public static DimensionalAmuletActionPacket decode(FriendlyByteBuf buf) {
        Action action = Action.values()[buf.readInt()];
        String data = buf.readUtf(32767);
        return new DimensionalAmuletActionPacket(action, data);
    }

    /**
     * Handle the packet server side.
     */
    public static void handle(DimensionalAmuletActionPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        if (!ctx.getDirection().getReceptionSide().isServer()) {
            return; // Ensure it's handled on the server
        }
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            switch (packet.action) {
                case SET_HOME -> {
                    // Example: store "home" data in player's persistent data, or in world data, etc.
                    player.sendSystemMessage(Component.literal("Home set! (example)"));
                }
                case TELEPORT_HOME -> {
                    // Example: read data from persistent store and teleport
                    player.sendSystemMessage(Component.literal("Teleporting home! (example)"));
                }
                case SET_NATION_NAME -> {
                    String nationName = packet.data;
                    // Example: store the name
                    player.sendSystemMessage(Component.literal("Nation name set to: " + nationName));
                }
                case RAND_WARP -> {
                    // Example: random coordinate calculation
                    player.sendSystemMessage(Component.literal("Random warp triggered! (example)"));
                }
                case TP_EARTH -> {
                    // Example: dimension or coordinate-based teleport
                    player.sendSystemMessage(Component.literal("Teleport to Earth dimension triggered! (example)"));
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}