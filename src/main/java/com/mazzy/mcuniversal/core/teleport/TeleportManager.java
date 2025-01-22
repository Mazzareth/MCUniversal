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
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TeleportManager {
    public static final Map<UUID, TeleportTask> ACTIVE_TELEPORTS = new ConcurrentHashMap<>();
    public static final LinkedHashMap<ChunkPos, Boolean> CHUNK_CACHE = new LinkedHashMap<>(50, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<ChunkPos, Boolean> eldest) {
            return size() > 50;
        }
    };

    private static void processWarmup(TeleportTask task) {
        float warmupProgress = 1 - (float) task.warmupTicks / (20 * TeleportConstants.WARMUP_SECONDS);
        float chunkProgress = task.phase == TeleportTask.Phase.POST_SCAN_LOAD ?
                (float) task.chunksLoadedPostScan / task.totalChunksToLoad : 0;

        task.bossBar.setProgress((warmupProgress + chunkProgress) / 2);

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
        task.chunksToLoad = Collections.singletonList(task.targetChunk);
        task.totalChunksToLoad = 1;
        task.bossBar = new ServerBossEvent(
                Component.literal("Initializing teleport"),
                BossEvent.BossBarColor.BLUE,
                BossEvent.BossBarOverlay.PROGRESS
        );
        task.bossBar.addPlayer(player);
        task.phase = TeleportTask.Phase.CHUNK_PREP;

        synchronized (ACTIVE_TELEPORTS) {
            ACTIVE_TELEPORTS.put(player.getUUID(), task);
        }
        scheduleNextStep(task, 0);
    }

    public static void scheduleNextStep(TeleportTask task, int delay) {
        task.player.getServer().execute(() -> {
            if (!validateTask(task)) return;
            float tickTime = task.level.getServer().getAverageTickTime();
            int maxOperations = tickTime > 45 ? 1 : tickTime > 30 ? 2 : 3;

            switch (task.phase) {
                case CHUNK_PREP:
                    processInitialChunkLoad(task, maxOperations);
                    break;
                case POSITION_SCAN:
                    processPositionScan(task, maxOperations);
                    break;
                case WARMUP:
                    processWarmup(task);
                    break;
                case POST_SCAN_LOAD:
                    processPostScanLoading(task, maxOperations);
                    processWarmup(task);
                    break;
            }
        });
    }

    private static void processInitialChunkLoad(TeleportTask task, int maxOperations) {
        if (!task.level.hasChunk(task.targetChunk.x, task.targetChunk.z)) {
            task.level.getChunk(task.targetChunk.x, task.targetChunk.z, ChunkStatus.FULL, true);
            task.chunksLoaded++;
        }

        if (task.chunksLoaded >= 1) {
            task.phase = TeleportTask.Phase.POSITION_SCAN;
            task.scanY = task.level.getMaxBuildHeight();
            task.bossBar.setName(Component.literal("Scanning for safe position..."));
            scheduleNextStep(task, 0);
        }
    }

    private static void processPositionScan(TeleportTask task, int maxOperations) {
        ChunkAccess chunk = task.level.getChunk(task.targetChunk.x, task.targetChunk.z);
        int operationsDone = 0;

        while (operationsDone < maxOperations && task.scanY >= task.level.getMinBuildHeight()) {
            // Fixed the argument count here
            task.foundPos = TeleportPositionHelper.findValidPosition(
                    task.level,  // Added ServerLevel parameter
                    chunk,
                    task.targetChunk,
                    task.scanY,
                    task.harshScan
            );
            if (task.foundPos != null) break;
            task.scanY -= task.harshScan ? TeleportConstants.HARSH_SCAN_STEP : TeleportConstants.SCAN_STEP;
            operationsDone++;
        }

        if (task.foundPos != null) {
            task.phase = TeleportTask.Phase.POST_SCAN_LOAD;
            task.chunksToLoad = TeleportPositionHelper.generateChunkGrid(task.targetChunk,
                    task.level.dimension().equals(Level.OVERWORLD) ?
                            TeleportConstants.OVERWORLD_CHUNK_PREGEN_RADIUS :
                            TeleportConstants.CHUNK_PREGEN_RADIUS);
            task.totalChunksToLoad = task.chunksToLoad.size();
            task.chunksLoadedPostScan = 0;
            task.warmupTicks = 1;
            task.bossBar.setName(Component.literal("Preparing area..."));
            scheduleNextStep(task, 0);
        } else if (task.scanY < task.level.getMinBuildHeight()) {
            handleScanFailure(task);
        } else {
            task.bossBar.setProgress(1 - (float) task.scanY / task.level.getMaxBuildHeight());
            scheduleNextStep(task, 1);
        }
    }

    private static void processPostScanLoading(TeleportTask task, int maxOperations) {
        for (int i = 0; i < maxOperations && task.chunksLoadedPostScan < task.totalChunksToLoad; i++) {
            ChunkPos current = task.chunksToLoad.get(task.chunksLoadedPostScan);
            if (!task.level.hasChunk(current.x, current.z)) {
                task.level.getChunk(current.x, current.z,
                        task.level.dimension().equals(Level.OVERWORLD) ?
                                ChunkStatus.STRUCTURE_STARTS :
                                ChunkStatus.FULL,
                        true);
            }
            task.chunksLoadedPostScan++;
        }

        task.bossBar.setName(Component.literal(String.format(
                "Preparing area (%d/%d) | Warping in %.1fs",
                task.chunksLoadedPostScan,
                task.totalChunksToLoad,
                (20 * TeleportConstants.WARMUP_SECONDS - task.warmupTicks) / 20f
        )));
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
                task.chunksToLoad = Collections.singletonList(task.targetChunk);
                task.chunksLoaded = 0;
                task.scanY = task.level.getMaxBuildHeight();
                scheduleNextStep(task, 0);
                return;
            }
            cleanupTask(task);
        } else {
            task.retries++;
            task.targetChunk = TeleportPositionHelper.generateNewChunk(task.level);
            task.chunksToLoad = Collections.singletonList(task.targetChunk);
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