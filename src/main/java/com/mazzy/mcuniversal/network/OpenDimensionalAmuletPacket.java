package com.mazzy.mcuniversal.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import com.mazzy.mcuniversal.core.client.ClientMethods;

/**
 * Network packet for opening the dimensional amulet interface on clients.
 * Synchronizes unlocked dimensions from server to client for GUI display.
 */
public class OpenDimensionalAmuletPacket {

    /** List of unlocked dimension resource locations (e.g., "minecraft:overworld") */
    private final List<String> unlockedDimensions;

    /** Primary constructor for server-side packet creation */
    public OpenDimensionalAmuletPacket(List<String> unlockedDimensions) {
        this.unlockedDimensions = unlockedDimensions;
    }

    /** Empty constructor for client-side packet decoding */
    public OpenDimensionalAmuletPacket() {
        this.unlockedDimensions = new ArrayList<>();
    }

    /** @return Read-only list of dimension IDs available to the player */
    public List<String> getUnlockedDimensions() {
        return unlockedDimensions;
    }

    /** Serializes packet data for network transmission */
    public static void encode(OpenDimensionalAmuletPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.unlockedDimensions.size());
        for (String dimId : packet.unlockedDimensions) {
            buf.writeUtf(dimId);  // UTF-8 string with max length 32767
        }
    }

    /** Deserializes packet data from network transmission */
    public static OpenDimensionalAmuletPacket decode(FriendlyByteBuf buf) {
        OpenDimensionalAmuletPacket pkt = new OpenDimensionalAmuletPacket();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            pkt.unlockedDimensions.add(buf.readUtf(32767));
        }
        return pkt;
    }

    /** Handles packet reception on client-side */
    public static void handle(OpenDimensionalAmuletPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        // Only process on client side
        if (!ctx.getDirection().getReceptionSide().isClient()) {
            return;
        }

        ctx.enqueueWork(() -> {
            // Forward dimension list to client-side GUI handler
            ClientMethods.openDimensionalAmuletScreen(packet.getUnlockedDimensions());
        });
        ctx.setPacketHandled(true);
    }
}