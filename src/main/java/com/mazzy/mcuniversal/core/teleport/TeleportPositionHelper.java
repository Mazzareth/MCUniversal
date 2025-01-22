package com.mazzy.mcuniversal.core.teleport;

import com.mazzy.mcuniversal.config.teleport.TeleportConstants;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class TeleportPositionHelper {
    public static BlockPos findValidPosition(ChunkAccess chunk, ChunkPos chunkPos, int startY, boolean harshScan) {
        List<Integer> xOffsets = new ArrayList<>();
        List<Integer> zOffsets = new ArrayList<>();

        if (harshScan) {
            for (int i = 0; i < 16; i += 4) xOffsets.add(i);
            for (int i = 0; i < 16; i += 4) zOffsets.add(i);
        } else {
            xOffsets = Arrays.asList(4, 8, 12);
            zOffsets = Arrays.asList(4, 8, 12);
        }

        Collections.shuffle(xOffsets);
        Collections.shuffle(zOffsets);

        for (int xOffset : xOffsets) {
            for (int zOffset : zOffsets) {
                int surfaceY = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, xOffset, zOffset);
                int minY = harshScan ? chunk.getMinBuildHeight() : Math.max(surfaceY - 8, chunk.getMinBuildHeight());
                int maxY = harshScan ? chunk.getMaxBuildHeight() : Math.min(surfaceY + 3, chunk.getMaxBuildHeight());
                int step = harshScan ? TeleportConstants.HARSH_SCAN_STEP : TeleportConstants.SCAN_STEP;

                for (int y = Math.min(startY, maxY); y >= minY; y -= step) {
                    BlockPos pos = new BlockPos(
                            chunkPos.getBlockX(xOffset),
                            y,
                            chunkPos.getBlockZ(zOffset)
                    );
                    if (isPositionValid(chunk, pos, harshScan)) {
                        return pos.above();
                    }
                }
            }
        }
        return null;
    }

    public static boolean isPositionValid(ChunkAccess chunk, BlockPos pos, boolean harshScan) {
        BlockState state = chunk.getBlockState(pos);
        BlockState below = chunk.getBlockState(pos.below());
        BlockState above = chunk.getBlockState(pos.above());

        boolean valid = state.isAir() &&
                above.isAir() &&
                below.blocksMotion() &&
                !below.is(Blocks.LAVA) &&
                !below.is(Blocks.BEDROCK);

        if (harshScan) {
            valid = valid && !below.is(Blocks.FIRE) &&
                    !below.is(Blocks.MAGMA_BLOCK) &&
                    !below.is(Blocks.CACTUS);
        }
        return valid;
    }

    public static List<ChunkPos> generateChunkGrid(ChunkPos center, int radius) {
        List<ChunkPos> chunks = new ArrayList<>();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                chunks.add(new ChunkPos(center.x + x, center.z + z));
            }
        }
        return chunks;
    }

    public static ChunkPos generateNewChunk(ServerLevel level) {
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        int range = TeleportConstants.TELEPORT_RANGE / 16;
        ChunkPos newChunk;

        do {
            newChunk = new ChunkPos(
                    rand.nextInt(-range, range + 1),
                    rand.nextInt(-range, range + 1)
            );
        } while (TeleportManager.CHUNK_CACHE.containsKey(newChunk));

        TeleportManager.CHUNK_CACHE.put(newChunk, true);
        return newChunk;
    }
}