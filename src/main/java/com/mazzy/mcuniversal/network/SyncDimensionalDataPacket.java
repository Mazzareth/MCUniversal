package com.mazzy.mcuniversal.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Sent from the server to the client to sync player's unlocked dimension IDs.
 */
public class SyncDimensionalDataPacket {

    private final List<String> unlockedDimensions;

    public SyncDimensionalDataPacket(List<String> unlockedDimensions) {
        this.unlockedDimensions = unlockedDimensions;
    }

    /**
     * Decode constructor: used to create an instance from the buffer data.
     */
    public static SyncDimensionalDataPacket decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<String> dims = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            dims.add(buf.readUtf());
        }
        return new SyncDimensionalDataPacket(dims);
    }

    /**
     * Write (encode) this packet's data into the buffer.
     */
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(unlockedDimensions.size());
        for (String dim : unlockedDimensions) {
            buf.writeUtf(dim);
        }
    }

    /**
     * Handle the packet client side: open the screen with the dimension list.
     */
    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        if (ctx.getDirection().getReceptionSide().isClient()) {
            ctx.enqueueWork(() -> {
                net.minecraft.client.Minecraft.getInstance().setScreen(
                        new com.mazzy.mcuniversal.core.screen.DimensionalAmuletScreen(unlockedDimensions)
                );
            });
        }
        ctx.setPacketHandled(true);
    }
}