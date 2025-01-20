// File: DimensionalAmuletActionPacket.java
package com.mazzy.mcuniversal.network;

import com.mazzy.mcuniversal.config.DimensionConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

public class DimensionalAmuletActionPacket {

    public enum Action {
        SET_HOME,
        TELEPORT_HOME,
        SET_NATION_NAME,
        RAND_WARP,
        RANDOM_TP_DIM // <-- Random-TP into a specified dimension
    }

    private final Action action;
    private final String data;

    private static final ResourceLocation EARTH_DIM_LOCATION = new ResourceLocation("mcuniversal", "extra");
    private static final ResourceKey<Level> EARTH_DIM_KEY =
            ResourceKey.create(Registries.DIMENSION, EARTH_DIM_LOCATION);

    public DimensionalAmuletActionPacket(Action action, String data) {
        this.action = action;
        this.data = data;
    }

    public DimensionalAmuletActionPacket(Action action) {
        this(action, "");
    }

    public static void encode(DimensionalAmuletActionPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.action.ordinal());
        buf.writeUtf(packet.data);
    }

    public static DimensionalAmuletActionPacket decode(FriendlyByteBuf buf) {
        Action action = Action.values()[buf.readInt()];
        String data = buf.readUtf(32767);
        return new DimensionalAmuletActionPacket(action, data);
    }

    public static void handle(DimensionalAmuletActionPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        if (!ctx.getDirection().getReceptionSide().isServer()) {
            return;
        }

        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                return;
            }

            CompoundTag pData = player.getPersistentData();
            MinecraftServer server = player.server;
            if (server == null) {
                return;
            }

            switch (packet.action) {
                case SET_HOME -> {
                    ResourceLocation currentDim = player.level().dimension().location();
                    if (!currentDim.equals(EARTH_DIM_LOCATION)) {
                        player.sendSystemMessage(
                                Component.literal("You must be in 'mcuniversal:extra' (Earth) to set your home!")
                        );
                        return;
                    }

                    final long cooldownTicks = 72_000L; // 1 hour
                    long lastSetHomeTime = pData.getLong("mcuniversal:lastSetHomeTime");
                    long currentTime = player.level().getGameTime();

                    if (lastSetHomeTime != 0) {
                        long timeSinceLast = currentTime - lastSetHomeTime;
                        if (timeSinceLast < cooldownTicks) {
                            long secondsLeft = (cooldownTicks - timeSinceLast) / 20;
                            player.sendSystemMessage(Component.literal(
                                    "You must wait " + secondsLeft + " more seconds before setting home again."
                            ));
                            return;
                        }
                    }

                    pData.putLong("mcuniversal:lastSetHomeTime", currentTime);

                    BlockPos pos = player.blockPosition();
                    pData.putInt("mcuniversal:homeX", pos.getX());
                    pData.putInt("mcuniversal:homeY", pos.getY());
                    pData.putInt("mcuniversal:homeZ", pos.getZ());
                    pData.putString("mcuniversal:homeDim", EARTH_DIM_LOCATION.toString());

                    ServerLevel earthDim = server.getLevel(EARTH_DIM_KEY);
                    if (earthDim != null) {
                        player.setRespawnPosition(
                                earthDim.dimension(),
                                pos,
                                player.getYRot(),
                                true,
                                false
                        );
                    }

                    player.sendSystemMessage(
                            Component.literal("Home set in Earth dimension and spawn updated!")
                    );
                }

                case TELEPORT_HOME -> {
                    if (!pData.contains("mcuniversal:homeX")) {
                        player.sendSystemMessage(Component.literal("No home has been set yet!"));
                        return;
                    }

                    int homeX = pData.getInt("mcuniversal:homeX");
                    int homeY = pData.getInt("mcuniversal:homeY");
                    int homeZ = pData.getInt("mcuniversal:homeZ");

                    ServerLevel earthDim = server.getLevel(EARTH_DIM_KEY);
                    if (earthDim == null) {
                        player.sendSystemMessage(Component.literal(
                                "Earth dimension not found, cannot teleport!"
                        ));
                        return;
                    }

                    player.teleportTo(earthDim,
                            homeX + 0.5,
                            homeY + 0.1,
                            homeZ + 0.5,
                            player.getYRot(),
                            player.getXRot()
                    );

                    player.sendSystemMessage(Component.literal(
                            "Teleported to your home in Earth at X:" + homeX +
                                    " Y:" + homeY +
                                    " Z:" + homeZ
                    ));
                }

                case SET_NATION_NAME -> {
                    String nationName = packet.data;
                    player.sendSystemMessage(Component.literal("Nation name set to: " + nationName));
                }

                case RAND_WARP -> {
                    ServerLevel overworld = server.getLevel(Level.OVERWORLD);
                    if (overworld == null) {
                        player.sendSystemMessage(Component.literal("Overworld not found!"));
                        return;
                    }
                    randomTeleportInDimension(player, overworld, "minecraft:overworld");
                }

                case RANDOM_TP_DIM -> {
                    String dimId = packet.data;
                    ResourceKey<Level> targetKey = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(dimId));
                    ServerLevel targetLevel = server.getLevel(targetKey);
                    if (targetLevel == null) {
                        player.sendSystemMessage(Component.literal("Could not find dimension: " + dimId));
                        return;
                    }
                    randomTeleportInDimension(player, targetLevel, dimId);
                }
            }
        });

        ctx.setPacketHandled(true);
    }

    /**
     * Randomly teleports the player within the given dimension, but caps
     * the search height at Y=127 if it's the Nether ("minecraft:the_nether").
     *
     * Scans from top to bottom in each chunk, looking for:
     *   - A solid block (not fluid)
     *   - Two open (air/fluid-free) blocks above
     */
    private static void randomTeleportInDimension(ServerPlayer player, ServerLevel level, String dimensionId) {
        Random random = new Random();
        int maxAttempts = 50;
        int range = 100000;
        boolean success = false;

        for (int i = 0; i < maxAttempts; i++) {
            int x = random.nextInt(range * 2 + 1) - range;
            int z = random.nextInt(range * 2 + 1) - range;

            ChunkAccess chunk = level.getChunk(x >> 4, z >> 4, ChunkStatus.FULL, true);
            if (chunk == null) {
                continue;
            }

            // Adjust topY if dimension is Nether to ensure we stay below Y=128
            int topY = chunk.getMaxBuildHeight();
            if ("minecraft:the_nether".equals(dimensionId)) {
                // Cap topY at 120 or so to avoid going into the roof
                topY = Math.min(topY, 120);
            }

            int bottomY = chunk.getMinBuildHeight();

            BlockPos foundPos = null;
            // Scan from just below the top, down to chunk's minimum
            for (int scanY = topY - 1; scanY > bottomY; scanY--) {
                BlockPos groundPos = new BlockPos(x, scanY, z);
                BlockState groundState = level.getBlockState(groundPos);

                if (!groundState.getFluidState().isEmpty() || !groundState.isSolid()) {
                    continue;
                }

                BlockPos airPos1 = groundPos.above(1);
                BlockPos airPos2 = groundPos.above(2);
                BlockState state1 = level.getBlockState(airPos1);
                BlockState state2 = level.getBlockState(airPos2);

                if (!state1.isAir() || !state2.isAir()) {
                    continue;
                }

                foundPos = airPos1;
                break;
            }

            if (foundPos == null) {
                continue;
            }
            player.teleportTo(
                    level,
                    foundPos.getX() + 0.5D,
                    foundPos.getY(),
                    foundPos.getZ() + 0.5D,
                    player.getYRot(),
                    player.getXRot()
            );

            // Set skipOverworldReturn if Overworld
            if ("minecraft:overworld".equals(dimensionId)) {
                player.getPersistentData().putBoolean("mcuniversal:skipOverworldReturn", true);
            }

            player.sendSystemMessage(
                    Component.literal("Randomly warped to [" + dimensionId + "] at " +
                            "X: " + foundPos.getX() +
                            " Y: " + foundPos.getY() +
                            " Z: " + foundPos.getZ())
            );

            success = true;
            break;
        }

        if (!success) {
            player.sendSystemMessage(Component.literal(
                    "Failed to find a safe random spot in " + dimensionId +
                            " after " + maxAttempts + " attempts."
            ));
        }
    }
}