package com.mazzy.mcuniversal.core.teleport;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerBossEvent;
import java.util.List;

public class TeleportTask {
    public ServerPlayer player;
    public ServerLevel level;
    public String dimId;
    public ChunkPos targetChunk;
    public List<ChunkPos> chunksToLoad;
    public int chunksLoaded;
    public int scanY;
    public int warmupTicks;
    public int retries;
    public boolean harshScan;
    public ServerBossEvent bossBar;
    public long startTime;
    public BlockPos startPos;
    public BlockPos foundPos;

    public enum Phase {
        CHUNK_PREP,      // Loading initial target chunk
        POSITION_SCAN,   // Scanning for valid position
        WARMUP,          // Warmup period
        POST_SCAN_LOAD   // Loading surrounding chunks after position found
    }

    public Phase phase = Phase.CHUNK_PREP;
    public int totalChunksToLoad;
    public int chunksLoadedPostScan;

    public boolean validate() {
        return TeleportManager.validateTask(this);
    }

    public void cleanup() {
        TeleportManager.cleanupTask(this);
    }
}