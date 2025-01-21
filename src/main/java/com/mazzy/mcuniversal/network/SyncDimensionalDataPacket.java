package com.mazzy.mcuniversal.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SyncDimensionalDataPacket {
    private final List<String> unlockedDimensions;

    public SyncDimensionalDataPacket(List<String> unlockedDimensions) {
        this.unlockedDimensions = unlockedDimensions;
    }

    public static SyncDimensionalDataPacket decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<String> dims = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            dims.add(buf.readUtf());
        }
        return new SyncDimensionalDataPacket(dims);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(unlockedDimensions.size());
        unlockedDimensions.forEach(buf::writeUtf);
    }

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            if (ctx.getDirection().getReceptionSide().isClient()) {
                ClientHandler.handle(this);
            }
        });
        ctx.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static class ClientHandler {
        private static void handle(SyncDimensionalDataPacket packet) {
            // Use fully qualified names to avoid import issues
            net.minecraft.client.Minecraft.getInstance().setScreen(
                    new com.mazzy.mcuniversal.core.screen.DimensionalAmuletScreen(packet.unlockedDimensions)
            );
        }
    }
}