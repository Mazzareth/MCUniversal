package com.mazzy.mcuniversal.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Synchronizes player-specific dimension unlock data between server and client.
 * Used to update client-side GUIs with latest dimension access information.
 */
public class SyncDimensionalDataPacket {
    /** List of unlocked dimension IDs in ResourceLocation format */
    private final List<String> unlockedDimensions;

    /**
     * @param unlockedDimensions Current collection of accessible dimensions
     */
    public SyncDimensionalDataPacket(List<String> unlockedDimensions) {
        this.unlockedDimensions = unlockedDimensions;
    }

    /**
     * Deserializes packet data from network buffer
     * @param buf Network data buffer
     * @return Packet with decoded dimension list
     */
    public static SyncDimensionalDataPacket decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<String> dims = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            dims.add(buf.readUtf());  // Read UTF-8 strings with default max length
        }
        return new SyncDimensionalDataPacket(dims);
    }

    /**
     * Serializes packet data for network transmission
     * @param buf Network data buffer to write to
     */
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(unlockedDimensions.size());
        unlockedDimensions.forEach(buf::writeUtf);  // Write strings sequentially
    }

    /**
     * Processes received packet on appropriate side
     * @param ctxSupplier Network context provider
     */
    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            // Route to client-side handler if received on client
            if (ctx.getDirection().getReceptionSide().isClient()) {
                ClientHandler.handle(this);
            }
        });
        ctx.setPacketHandled(true);
    }

    /**
     * Client-side handler for GUI updates
     */
    @OnlyIn(Dist.CLIENT)
    private static class ClientHandler {
        /**
         * Opens dimensional amulet interface with synced data
         * @param packet Received synchronization data
         */
        private static void handle(SyncDimensionalDataPacket packet) {
            net.minecraft.client.Minecraft.getInstance().setScreen(
                    new com.mazzy.mcuniversal.core.screen.DimensionalAmuletScreen(
                            packet.unlockedDimensions
                    )
            );
        }
    }
}