package com.mazzy.mcuniversal.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import com.mazzy.mcuniversal.core.item.AmuletActionHandlers;
import java.util.function.Supplier;

public class DimensionalAmuletActionPacket {
    public enum Action { SET_HOME, TELEPORT_HOME, SET_NATION_NAME, RAND_WARP, RANDOM_TP_DIM }

    private final Action action;
    private final String data;

    public DimensionalAmuletActionPacket(Action action, String data) {
        this.action = action;
        this.data = data;
    }

    public static void encode(DimensionalAmuletActionPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.action.ordinal());
        buf.writeUtf(packet.data);
    }

    public static DimensionalAmuletActionPacket decode(FriendlyByteBuf buf) {
        return new DimensionalAmuletActionPacket(Action.values()[buf.readInt()], buf.readUtf(32767));
    }

    public static void handle(DimensionalAmuletActionPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            CompoundTag pData = player.getPersistentData();
            MinecraftServer server = player.getServer();
            if (server == null) return;

            switch (packet.action) {
                case SET_HOME -> AmuletActionHandlers.handleSetHome(player, pData, server);
                case TELEPORT_HOME -> AmuletActionHandlers.handleTeleportHome(player, pData, server);
                case RAND_WARP -> AmuletActionHandlers.handleRandWarp(player, server);
                case RANDOM_TP_DIM -> AmuletActionHandlers.handleRandomTpDim(player, server, packet.data);
            }
        });
        ctx.setPacketHandled(true);
    }
}