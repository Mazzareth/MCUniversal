package com.mazzy.mcuniversal.core.teleport;

import com.mazzy.mcuniversal.config.teleport.TeleportConstants;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.BossEvent;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;

public class TeleportManager {
    public static final Map<UUID, TeleportTask> ACTIVE_TELEPORTS = new ConcurrentHashMap<>();
    public static final LinkedHashMap<ChunkPos, Boolean> CHUNK_CACHE = new LinkedHashMap<>(50, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<ChunkPos, Boolean> eldest) {
            return size() > 50;
        }
    };

    private static void processWarmup(TeleportTask task) {
        task.bossBar.setProgress(1 - (float) task.warmupTicks / (20 * TeleportConstants.WARMUP_SECONDS));

        if (++task.warmupTicks >= 20 * TeleportConstants.WARMUP_SECONDS) {
            executeTeleport(task);
            cleanupTask(task);
        } else {
            spawnWarmupEffects(task.player);
            scheduleNextStep(task, 1);
        }
    }

    private static void executeTeleport(TeleportTask task) {
        if (task.foundPos == null) {
            task.player.sendSystemMessage(Component.literal("Teleport failed!"));
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
        task.player.sendSystemMessage(Component.literal(
                "Warped to " + task.dimId + " at " +
                        task.foundPos.getX() + ", " + task.foundPos.getZ()
        ));
        preloadSurroundingChunks(task);
    }

    private static void preloadSurroundingChunks(TeleportTask task) {
        List<ChunkPos> chunks = TeleportPositionHelper.generateChunkGrid(task.targetChunk, 2);
        for (ChunkPos chunk : chunks) {
            task.level.getChunk(chunk.x, chunk.z, ChunkStatus.FULL, true);
        }
    }

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



    public static void startPhasedTeleport(ServerPlayer player, ServerLevel level, String dimId) {
        synchronized (ACTIVE_TELEPORTS) {
            if (ACTIVE_TELEPORTS.containsKey(player.getUUID())) {
                player.sendSystemMessage(Component.literal("Already teleporting!"));
                return;
            }
        }

        TeleportTask task = new TeleportTask();
        task.player = player;
        task.level = level;
        task.dimId = dimId;
        task.startTime = System.currentTimeMillis();
        task.startPos = player.blockPosition();
        task.targetChunk = TeleportPositionHelper.generateNewChunk(level);
        task.chunksToLoad = TeleportPositionHelper.generateChunkGrid(task.targetChunk, TeleportConstants.CHUNK_PREGEN_RADIUS);
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

    public static void scheduleNextStep(TeleportTask task, int delay) {
        task.player.getServer().execute(() -> {
            if (!validateTask(task)) return;
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

    private static void processChunkPreGen(TeleportTask task, int maxOperations) {
        for (int i = 0; i < maxOperations && task.chunksLoaded < task.chunksToLoad.size(); i++) {
            ChunkPos current = task.chunksToLoad.get(task.chunksLoaded);
            task.level.getChunk(current.x, current.z, ChunkStatus.FULL, true);
            task.chunksLoaded++;
        }

        task.bossBar.setProgress((float) task.chunksLoaded / task.chunksToLoad.size());
        task.bossBar.setName(Component.literal(
                "Loading chunks (" + task.chunksLoaded + "/" + task.chunksToLoad.size() + ")"
        ));
        scheduleNextStep(task, task.chunksLoaded < task.chunksToLoad.size() ? 1 : 0);
    }

    private static void processYScan(TeleportTask task, int maxOperations) {
        ChunkAccess chunk = task.level.getChunk(task.targetChunk.x, task.targetChunk.z);
        int operationsDone = 0;

        while (operationsDone < maxOperations && task.scanY >= task.level.getMinBuildHeight()) {
            task.foundPos = TeleportPositionHelper.findValidPosition(chunk, task.targetChunk, task.scanY, task.harshScan);
            if (task.foundPos != null) break;
            task.scanY -= task.harshScan ? TeleportConstants.HARSH_SCAN_STEP : TeleportConstants.SCAN_STEP;
            operationsDone++;
        }

        if (task.foundPos != null) {
            task.warmupTicks = 1;
            task.startPos = task.player.blockPosition();
            task.bossBar.setName(Component.literal("Teleporting in " + TeleportConstants.WARMUP_SECONDS + "s..."));
            scheduleNextStep(task, 0);
        } else if (task.scanY < task.level.getMinBuildHeight()) {
            handleScanFailure(task);
        } else {
            task.bossBar.setProgress(1 - (float) task.scanY / task.level.getMaxBuildHeight());
            scheduleNextStep(task, 1);
        }
    }

    private static void handleScanFailure(TeleportTask task) {
        if (task.retries >= (task.harshScan ? TeleportConstants.MAX_HARSH_RETRIES : TeleportConstants.MAX_RETRIES)) {
            if (task.harshScan) {
                task.player.sendSystemMessage(Component.literal("Harsh scan failed after " + TeleportConstants.MAX_HARSH_RETRIES + " attempts!"));
            } else {
                task.player.sendSystemMessage(Component.literal("Failed after " + TeleportConstants.MAX_RETRIES + " attempts! Starting harsh scan..."));
                task.harshScan = true;
                task.retries = 0;
                task.targetChunk = TeleportPositionHelper.generateNewChunk(task.level);
                task.chunksToLoad = TeleportPositionHelper.generateChunkGrid(task.targetChunk, TeleportConstants.CHUNK_PREGEN_RADIUS);
                task.chunksLoaded = 0;
                task.scanY = task.level.getMaxBuildHeight();
                scheduleNextStep(task, 0);
                return;
            }
            cleanupTask(task);
        } else {
            task.retries++;
            task.targetChunk = TeleportPositionHelper.generateNewChunk(task.level);
            task.chunksToLoad = TeleportPositionHelper.generateChunkGrid(task.targetChunk, TeleportConstants.CHUNK_PREGEN_RADIUS);
            task.chunksLoaded = 0;
            task.scanY = task.level.getMaxBuildHeight();
            scheduleNextStep(task, 0);
        }
    }

    protected static boolean validateTask(TeleportTask task) {
        if (!task.player.isAlive() || task.player.level().isClientSide ||
                System.currentTimeMillis() - task.startTime > TeleportConstants.MAX_TASK_DURATION_MS) {
            cleanupTask(task);
            return false;
        }

        if (task.warmupTicks > 0 && task.player.distanceToSqr(task.startPos.getX(), task.startPos.getY(), task.startPos.getZ()) > 1.0) {
            task.player.sendSystemMessage(Component.literal("Teleport cancelled!"));
            cleanupTask(task);
            return false;
        }
        return true;
    }

    protected static void cleanupTask(TeleportTask task) {
        task.bossBar.removeAllPlayers();
        synchronized (ACTIVE_TELEPORTS) {
            ACTIVE_TELEPORTS.remove(task.player.getUUID());
        }
    }
}