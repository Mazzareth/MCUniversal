package com.mazzy.mcuniversal.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.mazzy.mcuniversal.core.client.ClientMethods;

/**
 * Packet to instruct the client to open the DimensionalAmuletScreen
 * and provide the unlocked dimension IDs for the local player.
 */
public class OpenDimensionalAmuletPacket {

    private final List<String> unlockedDimensions;

    /**
     * Create a new packet with the given list of unlocked dimension IDs.
     */
    public OpenDimensionalAmuletPacket(List<String> unlockedDimensions) {
        this.unlockedDimensions = unlockedDimensions;
    }

    /**
     * Empty constructor for network framework only.
     */
    public OpenDimensionalAmuletPacket() {
        this.unlockedDimensions = new ArrayList<>();
    }

    public List<String> getUnlockedDimensions() {
        return unlockedDimensions;
    }

    // ------------------------------------------------------------------------
    // Encoding: write data to buffer
    // ------------------------------------------------------------------------
    public static void encode(OpenDimensionalAmuletPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.unlockedDimensions.size());
        for (String dimId : packet.unlockedDimensions) {
            buf.writeUtf(dimId);
        }
    }

    // ------------------------------------------------------------------------
    // Decoding: read data back from buffer
    // ------------------------------------------------------------------------
    public static OpenDimensionalAmuletPacket decode(FriendlyByteBuf buf) {
        OpenDimensionalAmuletPacket pkt = new OpenDimensionalAmuletPacket();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            pkt.unlockedDimensions.add(buf.readUtf(32767));
        }
        return pkt;
    }

    // ------------------------------------------------------------------------
    // Client-side handling
    // ------------------------------------------------------------------------
    public static void handle(OpenDimensionalAmuletPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        if (!ctx.getDirection().getReceptionSide().isClient()) {
            return;
        }
        ctx.enqueueWork(() -> {
            // Open the GUI with the unlocked dimension IDs
            ClientMethods.openDimensionalAmuletScreen(packet.getUnlockedDimensions());
        });
        ctx.setPacketHandled(true);
    }
}