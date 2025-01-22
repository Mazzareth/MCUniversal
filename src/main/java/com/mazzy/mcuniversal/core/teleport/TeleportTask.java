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

    public boolean validate() {
        return TeleportManager.validateTask(this);
    }

    public void cleanup() {
        TeleportManager.cleanupTask(this);
    }
}