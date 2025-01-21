package com.mazzy.mcuniversal.network;

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
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.network.NetworkEvent;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

public class DimensionalAmuletActionPacket {

    public enum Action {
        SET_HOME,
        TELEPORT_HOME,
        SET_NATION_NAME,
        RAND_WARP,
        RANDOM_TP_DIM
    }

    private final Action action;
    private final String data;

    private static final ResourceLocation EARTH_DIM_LOCATION = new ResourceLocation("mcuniversal", "extra");
    private static final ResourceKey<Level> EARTH_DIM_KEY =
            ResourceKey.create(Registries.DIMENSION, EARTH_DIM_LOCATION);

    // Phased check configuration
    private static final int TELEPORT_RANGE = 100000;
    private static final int MAX_CHUNK_ATTEMPTS = 5;
    private static final int PHASE1_CHECKS = 10;
    private static final int PHASE2_GRID_STEP = 4;
    private static final int PHASE3_GRID_STEP = 2;
    private static final int SCAN_DEPTH = 12;

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
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            CompoundTag pData = player.getPersistentData();
            MinecraftServer server = player.getServer();
            if (server == null) return;

            switch (packet.action) {
                case SET_HOME -> handleSetHome(player, pData, server);
                case TELEPORT_HOME -> handleTeleportHome(player, pData, server);
                case SET_NATION_NAME -> handleSetNationName(player, packet.data);
                case RAND_WARP -> handleRandWarp(player, server);
                case RANDOM_TP_DIM -> handleRandomTpDim(player, server, packet.data);
            }
        });
        ctx.setPacketHandled(true);
    }

    // Existing home and nation methods unchanged
    private static void handleSetHome(ServerPlayer player, CompoundTag pData, MinecraftServer server) {
        ResourceLocation currentDim = player.level().dimension().location();
        if (!currentDim.equals(EARTH_DIM_LOCATION)) {
            player.sendSystemMessage(Component.literal("You must be in 'mcuniversal:extra' (Earth) to set your home!"));
            return;
        }

        final long cooldownTicks = 72000;
        long lastSetHomeTime = pData.getLong("mcuniversal:lastSetHomeTime");
        long currentTime = player.level().getGameTime();

        if (lastSetHomeTime != 0 && (currentTime - lastSetHomeTime) < cooldownTicks) {
            long secondsLeft = (cooldownTicks - (currentTime - lastSetHomeTime)) / 20;
            player.sendSystemMessage(Component.literal("Wait " + secondsLeft + " seconds before setting home again."));
            return;
        }

        pData.putLong("mcuniversal:lastSetHomeTime", currentTime);
        BlockPos pos = player.blockPosition();
        pData.putInt("mcuniversal:homeX", pos.getX());
        pData.putInt("mcuniversal:homeY", pos.getY());
        pData.putInt("mcuniversal:homeZ", pos.getZ());
        pData.putString("mcuniversal:homeDim", EARTH_DIM_LOCATION.toString());

        ServerLevel earthDim = server.getLevel(EARTH_DIM_KEY);
        if (earthDim != null) {
            player.setRespawnPosition(earthDim.dimension(), pos, player.getYRot(), true, false);
        }
        player.sendSystemMessage(Component.literal("Home set in Earth dimension!"));
    }

    private static void handleTeleportHome(ServerPlayer player, CompoundTag pData, MinecraftServer server) {
        if (!pData.contains("mcuniversal:homeX")) {
            player.sendSystemMessage(Component.literal("No home set!"));
            return;
        }

        ServerLevel earthDim = server.getLevel(EARTH_DIM_KEY);
        if (earthDim == null) {
            player.sendSystemMessage(Component.literal("Earth dimension missing!"));
            return;
        }

        BlockPos homePos = new BlockPos(
                pData.getInt("mcuniversal:homeX"),
                pData.getInt("mcuniversal:homeY"),
                pData.getInt("mcuniversal:homeZ")
        );

        player.teleportTo(earthDim,
                homePos.getX() + 0.5,
                homePos.getY() + 0.1,
                homePos.getZ() + 0.5,
                player.getYRot(),
                player.getXRot()
        );
        player.sendSystemMessage(Component.literal("Teleported home!"));
    }

    private static void handleSetNationName(ServerPlayer player, String nationName) {
        player.sendSystemMessage(Component.literal("Nation name set to: " + nationName));
    }

    private static void handleRandWarp(ServerPlayer player, MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld != null) {
            optimizedRandomTeleport(player, overworld, "minecraft:overworld");
        } else {
            player.sendSystemMessage(Component.literal("Overworld not found!"));
        }
    }

    private static void handleRandomTpDim(ServerPlayer player, MinecraftServer server, String dimId) {
        ResourceKey<Level> targetKey = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(dimId));
        ServerLevel targetLevel = server.getLevel(targetKey);
        if (targetLevel != null) {
            optimizedRandomTeleport(player, targetLevel, dimId);
        } else {
            player.sendSystemMessage(Component.literal("Dimension not found: " + dimId));
        }
    }

    private static void optimizedRandomTeleport(ServerPlayer player, ServerLevel level, String dimensionId) {
        final ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int chunkAttempt = 0; chunkAttempt < MAX_CHUNK_ATTEMPTS; chunkAttempt++) {
            ChunkPos chunkPos = new ChunkPos(
                    random.nextInt(TELEPORT_RANGE * 2 / 16) - TELEPORT_RANGE / 16,
                    random.nextInt(TELEPORT_RANGE * 2 / 16) - TELEPORT_RANGE / 16
            );

            try {
                ChunkAccess chunk = level.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.FULL, true);

                // Phase 1: Quick random checks
                BlockPos foundPos = phase1QuickCheck(chunk, chunkPos, level);
                if (tryTeleport(player, level, dimensionId, foundPos)) return;

                // Phase 2: Guided grid check
                foundPos = phase2GridCheck(chunk, chunkPos, level);
                if (tryTeleport(player, level, dimensionId, foundPos)) return;

                // Phase 3: Full chunk scan
                foundPos = phase3FullScan(chunk, chunkPos, level);
                if (tryTeleport(player, level, dimensionId, foundPos)) return;

            } catch (Exception e) {
                continue;
            }
        }
        player.sendSystemMessage(Component.literal("Failed to find safe location in " + dimensionId));
    }

    private static boolean tryTeleport(ServerPlayer player, ServerLevel level, String dimId, BlockPos pos) {
        if (pos != null && validateFinalPosition(level, pos)) {
            executeTeleport(player, level, dimId, pos);
            return true;
        }
        return false;
    }

    private static BlockPos phase1QuickCheck(ChunkAccess chunk, ChunkPos chunkPos, ServerLevel level) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < PHASE1_CHECKS; i++) {
            int x = random.nextInt(16);
            int z = random.nextInt(16);
            BlockPos pos = scanColumn(chunk, chunkPos, x, z, level);
            if (pos != null) return pos;
        }
        return null;
    }

    private static BlockPos phase2GridCheck(ChunkAccess chunk, ChunkPos chunkPos, ServerLevel level) {
        for (int x = 0; x < 16; x += PHASE2_GRID_STEP) {
            for (int z = 0; z < 16; z += PHASE2_GRID_STEP) {
                BlockPos pos = scanColumn(chunk, chunkPos, x, z, level);
                if (pos != null) return pos;
            }
        }
        return null;
    }

    private static BlockPos phase3FullScan(ChunkAccess chunk, ChunkPos chunkPos, ServerLevel level) {
        for (int x = 0; x < 16; x += PHASE3_GRID_STEP) {
            for (int z = 0; z < 16; z += PHASE3_GRID_STEP) {
                BlockPos pos = scanColumn(chunk, chunkPos, x, z, level);
                if (pos != null) return pos;
            }
        }
        return null;
    }

    private static BlockPos scanColumn(ChunkAccess chunk, ChunkPos chunkPos, int x, int z, ServerLevel level) {
        int surfaceY = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();

        // Scan from surface downward
        for (int y = Math.min(surfaceY + SCAN_DEPTH, maxY); y >= Math.max(surfaceY - SCAN_DEPTH, minY); y--) {
            BlockPos pos = new BlockPos(
                    chunkPos.getBlockX(x),
                    y,
                    chunkPos.getBlockZ(z)
            );
            if (isPositionValid(level, pos)) {
                return adjustToSafePosition(level, pos);
            }
        }
        return null;
    }

    private static BlockPos adjustToSafePosition(ServerLevel level, BlockPos initialPos) {
        // Find highest valid position above initial spot
        for (int y = initialPos.getY(); y < level.getMaxBuildHeight(); y++) {
            BlockPos pos = new BlockPos(initialPos.getX(), y, initialPos.getZ());
            if (level.getBlockState(pos.below()).isSolid() &&
                    level.getBlockState(pos).isAir() &&
                    level.getBlockState(pos.above()).isAir()) {
                return pos;
            }
        }
        return initialPos;
    }

    private static boolean isPositionValid(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockState belowState = level.getBlockState(pos.below());

        return state.isAir() &&
                belowState.isSolid() &&
                !belowState.is(Blocks.BEDROCK) &&
                !belowState.is(Blocks.LAVA) &&
                level.getBlockState(pos.above()).isAir();
    }

    private static boolean validateFinalPosition(ServerLevel level, BlockPos pos) {
        // Final 3x3 area check
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos checkPos = pos.offset(dx, 0, dz);
                BlockState state = level.getBlockState(checkPos);
                if (state.is(Blocks.LAVA) || state.is(Blocks.FIRE) || state.is(Blocks.CAMPFIRE)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void executeTeleport(ServerPlayer player, ServerLevel level,
                                        String dimId, BlockPos pos) {
        player.getPersistentData().putBoolean("mcuniversal:skipOverworldReturn", true);
        player.teleportTo(level,
                pos.getX() + 0.5,
                pos.getY(),
                pos.getZ() + 0.5,
                player.getYRot(),
                player.getXRot()
        );

        player.sendSystemMessage(Component.literal(
                String.format("Warped to [%s] at X:%d Y:%d Z:%d",
                        dimId,
                        pos.getX(),
                        pos.getY(),
                        pos.getZ())
        ));
    }
}