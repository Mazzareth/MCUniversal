package com.mazzy.mcuniversal.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.network.NetworkEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * Handles dimensional amulet operations through network packets.
 * Manages complex teleportation logic with chunk pre-generation, position validation,
 * and phased teleportation with player feedback.
 */
public class DimensionalAmuletActionPacket {
    /** Available amulet interaction types */
    public enum Action {
        SET_HOME,          // Set permanent respawn point
        TELEPORT_HOME,     // Return to saved respawn
        SET_NATION_NAME,   // Configure player faction
        RAND_WARP,         // Random overworld teleport
        RANDOM_TP_DIM      // Cross-dimensional teleport
    }

    // Teleport configuration constants
    private static final int TELEPORT_RANGE = 100000;         // Max offset from world origin
    private static final int MAX_RETRIES = 3;                 // Normal scan attempts
    private static final int MAX_HARSH_RETRIES = 2;           // Intensive scan attempts
    private static final int CHUNK_PREGEN_RADIUS = 2;         // Chunk loading radius
    private static final int WARMUP_SECONDS = 3;              // Teleport delay duration
    private static final int SCAN_STEP = 3;                   // Vertical scan increment
    private static final int HARSH_SCAN_STEP = 1;             // Intensive scan increment
    private static final long MAX_TASK_DURATION_MS = 30000;   // Teleport timeout
    private static final int MAX_TASKS_PER_TICK = 2;          // Performance throttle

    // Dimension configuration
    private static final ResourceLocation EARTH_DIM_LOCATION = new ResourceLocation("mcuniversal", "extra");
    private static final ResourceKey<Level> EARTH_DIM_KEY = ResourceKey.create(Registries.DIMENSION, EARTH_DIM_LOCATION);

    // Teleport state tracking
    private static final Map<UUID, TeleportTask> ACTIVE_TELEPORTS = new ConcurrentHashMap<>();
    private static final LinkedHashMap<ChunkPos, Boolean> CHUNK_CACHE = new LinkedHashMap<ChunkPos, Boolean>(50, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<ChunkPos, Boolean> eldest) {
            return size() > 50;  // LRU cache for chunk positions
        }
    };

    /** Tracks state for ongoing teleport operations */
    private static class TeleportTask {
        ServerPlayer player;          // Target player
        ServerLevel level;            // Destination dimension
        String dimId;                 // Dimension ID string
        ChunkPos targetChunk;         // Center chunk for teleport
        List<ChunkPos> chunksToLoad;  // Chunk pre-gen queue
        int chunksLoaded;             // Chunks processed count
        int scanY;                    // Current Y-level scan position
        int warmupTicks;              // Countdown timer
        int retries;                  // Attempt counter
        boolean harshScan;            // Intensive scan mode
        ServerBossEvent bossBar;      // Player progress UI
        long startTime;               // Operation start timestamp
        BlockPos startPos;            // Original position
        BlockPos foundPos;            // Validated destination
    }

    // Packet payload
    private final Action action;
    private final String data;

    public DimensionalAmuletActionPacket(Action action, String data) {
        this.action = action;
        this.data = data;
    }

    // Network serialization
    public static void encode(DimensionalAmuletActionPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.action.ordinal());
        buf.writeUtf(packet.data);
    }

    public static DimensionalAmuletActionPacket decode(FriendlyByteBuf buf) {
        return new DimensionalAmuletActionPacket(Action.values()[buf.readInt()], buf.readUtf(32767));
    }

    /** Main packet handler routing actions to appropriate logic */
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

    /** Sets player's home position with cooldown and dimension restrictions */
    private static void handleSetHome(ServerPlayer player, CompoundTag pData, MinecraftServer server) {
        // Validate dimension
        if (!player.level().dimension().location().equals(EARTH_DIM_LOCATION)) {
            player.sendSystemMessage(Component.literal("You must be in 'mcuniversal:extra' (Earth) to set your home!"));
            return;
        }

        // Enforce cooldown
        final long cooldownTicks = 72000;  // 1 hour real-time
        long lastSetHomeTime = pData.getLong("mcuniversal:lastSetHomeTime");
        long currentTime = player.level().getGameTime();

        if (lastSetHomeTime != 0 && (currentTime - lastSetHomeTime) < cooldownTicks) {
            long secondsLeft = (cooldownTicks - (currentTime - lastSetHomeTime)) / 20;
            player.sendSystemMessage(Component.literal("Wait " + secondsLeft + " seconds before setting home again."));
            return;
        }

        // Store position
        BlockPos pos = player.blockPosition();
        pData.putInt("mcuniversal:homeX", pos.getX());
        pData.putInt("mcuniversal:homeY", pos.getY());
        pData.putInt("mcuniversal:homeZ", pos.getZ());
        pData.putString("mcuniversal:homeDim", EARTH_DIM_LOCATION.toString());
        pData.putLong("mcuniversal:lastSetHomeTime", currentTime);

        // Update respawn point
        ServerLevel earthDim = server.getLevel(EARTH_DIM_KEY);
        if (earthDim != null) {
            player.setRespawnPosition(earthDim.dimension(), pos, player.getYRot(), true, false);
            player.sendSystemMessage(Component.literal("Home set at: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()));
        } else {
            player.sendSystemMessage(Component.literal("Failed to set home - Earth dimension unavailable!"));
        }
    }

    /** Teleports player to saved home position */
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

        // Calculate position with center-block alignment
        BlockPos homePos = new BlockPos(
                pData.getInt("mcuniversal:homeX"),
                pData.getInt("mcuniversal:homeY"),
                pData.getInt("mcuniversal:homeZ")
        );

        player.teleportTo(earthDim,
                homePos.getX() + 0.5,
                homePos.getY() + 0.1,  // Prevent floor clipping
                homePos.getZ() + 0.5,
                player.getYRot(),
                player.getXRot()
        );
        player.sendSystemMessage(Component.literal("Teleported home!"));
    }

    /** Stores nation/faction name in player data */
    private static void handleSetNationName(ServerPlayer player, String nationName) {
        player.getPersistentData().putString("mcuniversal:nationName", nationName);
        player.sendSystemMessage(Component.literal("Nation name set to: " + nationName));
    }

    /** Initiates random overworld teleport */
    private static void handleRandWarp(ServerPlayer player, MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld != null) {
            startPhasedTeleport(player, overworld, "minecraft:overworld");
        } else {
            player.sendSystemMessage(Component.literal("Overworld not found!"));
        }
    }

    /** Handles cross-dimensional teleport with full state management */
    private static void handleRandomTpDim(ServerPlayer player, MinecraftServer server, String dimId) {
        synchronized (ACTIVE_TELEPORTS) {
            if (ACTIVE_TELEPORTS.containsKey(player.getUUID())) {
                player.sendSystemMessage(Component.literal("Already teleporting!"));
                return;
            }
        }

        // Validate target dimension
        ResourceKey<Level> targetKey = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(dimId));
        ServerLevel targetLevel = server.getLevel(targetKey);
        if (targetLevel == null) {
            player.sendSystemMessage(Component.literal("Dimension not found: " + dimId));
            return;
        }

        // Initialize teleport task
        TeleportTask task = new TeleportTask();
        task.player = player;
        task.level = targetLevel;
        task.dimId = dimId;
        task.startTime = System.currentTimeMillis();
        task.startPos = player.blockPosition();
        task.targetChunk = generateNewChunk(targetLevel);
        task.chunksToLoad = generateChunkGrid(task.targetChunk, CHUNK_PREGEN_RADIUS);
        task.bossBar = new ServerBossEvent(
                Component.literal("Teleporting"),
                BossEvent.BossBarColor.BLUE,
                BossEvent.BossBarOverlay.PROGRESS
        );
        task.bossBar.addPlayer(player);

        synchronized (ACTIVE_TELEPORTS) {
            ACTIVE_TELEPORTS.put(player.getUUID(), task);
        }

        scheduleNextStep(task, 0);
    }

    /** Coordinates phased teleport steps with performance throttling */
    private static void scheduleNextStep(TeleportTask task, int delay) {
        task.player.getServer().execute(() -> {
            if (!validateTask(task)) return;

            // Dynamic workload based on server health
            float tickTime = task.level.getServer().getAverageTickTime();
            int maxOperations = tickTime > 45 ? 1 : 2;

            if (task.chunksLoaded < task.chunksToLoad.size()) {
                processChunkPreGen(task, maxOperations);
            } else if (task.scanY == 0) {
                task.scanY = task.level.getMaxBuildHeight();
                processYScan(task, maxOperations);
            } else if (task.warmupTicks > 0) {
                processWarmup(task);
            } else {
                processYScan(task, maxOperations);
            }
        });
    }

    /** Validates teleport task viability */
    private static boolean validateTask(TeleportTask task) {
        // Check for player/logistical issues
        if (!task.player.isAlive() ||
                task.player.level().isClientSide ||
                System.currentTimeMillis() - task.startTime > MAX_TASK_DURATION_MS) {

            cleanupTask(task);
            return false;
        }

        // Prevent movement during warmup
        if (task.warmupTicks > 0 &&
                task.player.distanceToSqr(task.startPos.getX(), task.startPos.getY(), task.startPos.getZ()) > 1.0) {
            task.player.sendSystemMessage(Component.literal("Teleport cancelled!"));
            cleanupTask(task);
            return false;
        }
        return true;
    }

    /** Loads chunks in controlled batches */
    private static void processChunkPreGen(TeleportTask task, int maxOperations) {
        for (int i = 0; i < maxOperations && task.chunksLoaded < task.chunksToLoad.size(); i++) {
            ChunkPos current = task.chunksToLoad.get(task.chunksLoaded);
            task.level.getChunk(current.x, current.z, ChunkStatus.FULL, true);
            task.chunksLoaded++;
        }

        // Update progress UI
        task.bossBar.setProgress((float) task.chunksLoaded / task.chunksToLoad.size());
        task.bossBar.setName(Component.literal(
                "Loading chunks (" + task.chunksLoaded + "/" + task.chunksToLoad.size() + ")"
        ));

        scheduleNextStep(task, task.chunksLoaded < task.chunksToLoad.size() ? 1 : 0);
    }

    /** Scans vertical column for safe landing position */
    private static void processYScan(TeleportTask task, int maxOperations) {
        ChunkAccess chunk = task.level.getChunk(task.targetChunk.x, task.targetChunk.z);
        int operationsDone = 0;

        // Scan from top to bottom
        while (operationsDone < maxOperations && task.scanY >= task.level.getMinBuildHeight()) {
            task.foundPos = findValidPosition(chunk, task.targetChunk, task.scanY, task);
            if (task.foundPos != null) break;

            task.scanY -= task.harshScan ? HARSH_SCAN_STEP : SCAN_STEP;
            operationsDone++;
        }

        if (task.foundPos != null) {
            // Start warmup phase
            task.warmupTicks = 1;
            task.startPos = task.player.blockPosition();
            task.bossBar.setName(Component.literal("Teleporting in " + WARMUP_SECONDS + "s..."));
            scheduleNextStep(task, 0);
        } else if (task.scanY < task.level.getMinBuildHeight()) {
            handleScanFailure(task);
        } else {
            // Update progress and continue
            task.bossBar.setProgress(1 - (float) task.scanY / task.level.getMaxBuildHeight());
            scheduleNextStep(task, 1);
        }
    }

    /** Finds valid teleport position using adaptive scanning */
    private static BlockPos findValidPosition(ChunkAccess chunk, ChunkPos chunkPos, int startY, TeleportTask task) {
        // Configure scan pattern based on mode
        List<Integer> xOffsets = new ArrayList<>();
        List<Integer> zOffsets = new ArrayList<>();

        if (task.harshScan) {
            for (int i = 0; i < 16; i += 4) xOffsets.add(i);
            for (int i = 0; i < 16; i += 4) zOffsets.add(i);
        } else {
            xOffsets = Arrays.asList(4, 8, 12);
            zOffsets = Arrays.asList(4, 8, 12);
        }

        Collections.shuffle(xOffsets);
        Collections.shuffle(zOffsets);

        // Scan chunk columns
        for (int xOffset : xOffsets) {
            for (int zOffset : zOffsets) {
                int surfaceY = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, xOffset, zOffset);
                int minY = task.harshScan ? chunk.getMinBuildHeight() : Math.max(surfaceY - 8, chunk.getMinBuildHeight());
                int maxY = task.harshScan ? chunk.getMaxBuildHeight() : Math.min(surfaceY + 3, chunk.getMaxBuildHeight());
                int step = task.harshScan ? HARSH_SCAN_STEP : SCAN_STEP;

                // Vertical scan
                for (int y = Math.min(startY, maxY); y >= minY; y -= step) {
                    BlockPos pos = new BlockPos(
                            chunkPos.getBlockX(xOffset),
                            y,
                            chunkPos.getBlockZ(zOffset)
                    );
                    if (isPositionValid(chunk, pos, task.harshScan)) {
                        return pos.above();  // Prevent head-in-block
                    }
                }
            }
        }
        return null;
    }

    /** Validates position safety */
    private static boolean isPositionValid(ChunkAccess chunk, BlockPos pos, boolean harshScan) {
        BlockState state = chunk.getBlockState(pos);
        BlockState below = chunk.getBlockState(pos.below());
        BlockState above = chunk.getBlockState(pos.above());

        // Basic safety checks
        boolean valid = state.isAir() &&
                above.isAir() &&
                below.blocksMotion() &&
                !below.is(Blocks.LAVA) &&
                !below.is(Blocks.BEDROCK);

        // Additional checks for intensive scan
        if (harshScan) {
            valid = valid && !below.is(Blocks.FIRE) &&
                    !below.is(Blocks.MAGMA_BLOCK) &&
                    !below.is(Blocks.CACTUS);
        }

        return valid;
    }

    /** Handles warmup phase with visual effects */
    private static void processWarmup(TeleportTask task) {
        task.bossBar.setProgress(1 - (float) task.warmupTicks / (20 * WARMUP_SECONDS));

        if (++task.warmupTicks >= 20 * WARMUP_SECONDS) {
            executeTeleport(task);
            cleanupTask(task);
        } else {
            spawnWarmupEffects(task.player);
            scheduleNextStep(task, 1);
        }
    }

    /** Executes final teleport with position validation */
    private static void executeTeleport(TeleportTask task) {
        if (task.foundPos == null) {
            task.player.sendSystemMessage(Component.literal("Teleport failed!")); // Fixed variable reference
            cleanupTask(task);
            return;
        }

        if (task.level.dimension().equals(Level.OVERWORLD)) {
            task.player.getPersistentData().putBoolean("mcuniversal:skipOverworldReturn", true);
        }

        task.level.getChunk(task.targetChunk.x, task.targetChunk.z, ChunkStatus.FULL, true);

        task.player.teleportTo(
                task.level,
                task.foundPos.getX() + 0.5,
                task.foundPos.getY(),
                task.foundPos.getZ() + 0.5,
                task.player.getYRot(),
                task.player.getXRot()
        );

        task.player.connection.resetPosition();
        task.player.sendSystemMessage(Component.literal( // Fixed variable reference
                "Warped to " + task.dimId + " at " +
                        task.foundPos.getX() + ", " + task.foundPos.getZ()
        ));
        preloadSurroundingChunks(task);
    }

    /** Preloads adjacent chunks for smooth post-teleport movement */
    private static void preloadSurroundingChunks(TeleportTask task) {
        List<ChunkPos> chunks = generateChunkGrid(task.targetChunk, 2);
        for (ChunkPos chunk : chunks) {
            task.level.getChunk(chunk.x, chunk.z, ChunkStatus.FULL, true);
        }
    }

    /** Visual feedback during warmup */
    private static void spawnWarmupEffects(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        level.sendParticles(
                player,
                ParticleTypes.PORTAL,
                true,
                player.getX(),
                player.getY() + 1,
                player.getZ(),
                10,
                0.5,
                0.5,
                0.5,
                0.2
        );
    }

    /** Handles teleport failure scenarios */
    private static void handleScanFailure(TeleportTask task) {
        if (task.retries >= (task.harshScan ? MAX_HARSH_RETRIES : MAX_RETRIES)) {
            if (task.harshScan) {
                task.player.sendSystemMessage(Component.literal("Harsh scan failed after " + MAX_HARSH_RETRIES + " attempts!"));
            } else {
                task.player.sendSystemMessage(Component.literal("Failed after " + MAX_RETRIES + " attempts! Starting harsh scan..."));
                task.harshScan = true;
                task.retries = 0;
                task.targetChunk = generateNewChunk(task.level);
                task.chunksToLoad = generateChunkGrid(task.targetChunk, CHUNK_PREGEN_RADIUS);
                task.chunksLoaded = 0;
                task.scanY = task.level.getMaxBuildHeight();
                scheduleNextStep(task, 0);
                return;
            }
            cleanupTask(task);
        } else {
            // Retry with new chunk
            task.retries++;
            task.targetChunk = generateNewChunk(task.level);
            task.chunksToLoad = generateChunkGrid(task.targetChunk, CHUNK_PREGEN_RADIUS);
            task.chunksLoaded = 0;
            task.scanY = task.level.getMaxBuildHeight();
            scheduleNextStep(task, 0);
        }
    }

    /** Cleans up task resources */
    private static void cleanupTask(TeleportTask task) {
        task.bossBar.removeAllPlayers();
        synchronized (ACTIVE_TELEPORTS) {
            ACTIVE_TELEPORTS.remove(task.player.getUUID());
        }
    }

    /** Generates new chunk position with cache management */
    private static ChunkPos generateNewChunk(ServerLevel level) {
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        int range = TELEPORT_RANGE / 16;
        ChunkPos newChunk;

        // Find unique chunk position
        do {
            newChunk = new ChunkPos(
                    rand.nextInt(-range, range + 1),
                    rand.nextInt(-range, range + 1)
            );
        } while (CHUNK_CACHE.containsKey(newChunk));

        CHUNK_CACHE.put(newChunk, true);
        return newChunk;
    }

    /** Generates chunk grid around center position */
    private static List<ChunkPos> generateChunkGrid(ChunkPos center, int radius) {
        List<ChunkPos> chunks = new ArrayList<>();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                chunks.add(new ChunkPos(center.x + x, center.z + z));
            }
        }
        return chunks;
    }

    /** Starts phased teleport with state tracking */
    private static void startPhasedTeleport(ServerPlayer player, ServerLevel level, String dimId) {
        synchronized (ACTIVE_TELEPORTS) {
            if (ACTIVE_TELEPORTS.containsKey(player.getUUID())) {
                player.sendSystemMessage(Component.literal("Already teleporting!"));
                return;
            }
        }

        // Initialize new teleport task
        TeleportTask task = new TeleportTask();
        task.player = player;
        task.level = level;
        task.dimId = dimId;
        task.startTime = System.currentTimeMillis();
        task.startPos = player.blockPosition();
        task.targetChunk = generateNewChunk(level);
        task.chunksToLoad = generateChunkGrid(task.targetChunk, CHUNK_PREGEN_RADIUS);
        task.bossBar = new ServerBossEvent(
                Component.literal("Teleporting"),
                BossEvent.BossBarColor.BLUE,
                BossEvent.BossBarOverlay.PROGRESS
        );
        task.bossBar.addPlayer(player);

        synchronized (ACTIVE_TELEPORTS) {
            ACTIVE_TELEPORTS.put(player.getUUID(), task);
        }

        scheduleNextStep(task, 0);
    }
}