package com.mazzy.mcuniversal.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
import com.mazzy.mcuniversal.core.client.ClientMethods;

/**
 * Packet to instruct the client to open the DimensionalAmuletScreen.
 */
public class OpenDimensionalAmuletPacket {

    // No fields; this packet simply signals the client to open the GUI.

    public OpenDimensionalAmuletPacket() {
        // Required no-arg constructor.
    }

    // Encode this packet’s data into the buffer for network transmission.
    public static void encode(OpenDimensionalAmuletPacket packet, FriendlyByteBuf buf) {
        // No data to write here
    }

    // Decode this packet, reconstructing after reading from the buffer.
    public static OpenDimensionalAmuletPacket decode(FriendlyByteBuf buf) {
        // No data to read; return a default instance
        return new OpenDimensionalAmuletPacket();
    }

    // Handle this packet client-side to open the GUI.
    public static void handle(OpenDimensionalAmuletPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        if (!ctx.getDirection().getReceptionSide().isClient()) {
            return; // Ensure it’s client-side
        }
        ctx.enqueueWork(() -> {
            // Open the DimensionalAmuletScreen on the client
            ClientMethods.openDimensionalAmuletScreen();
        });
        ctx.setPacketHandled(true);
    }
}