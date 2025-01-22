package com.mazzy.mcuniversal.core.teleport;

import com.mazzy.mcuniversal.config.teleport.TeleportConstants;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class TeleportPositionHelper {
    // Modified method signature to accept ServerLevel
    public static BlockPos findValidPosition(ServerLevel level, ChunkAccess chunk, ChunkPos chunkPos, int startY, boolean harshScan) {
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
                int minY = harshScan ? level.getMinBuildHeight() : Math.max(surfaceY - 8, level.getMinBuildHeight());
                int maxY = harshScan ? level.getMaxBuildHeight() : Math.min(surfaceY + 3, level.getMaxBuildHeight());
                int step = getScanStep(level, harshScan);

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

    // Now using ServerLevel directly
    private static int getScanStep(ServerLevel level, boolean harshScan) {
        if (harshScan) return TeleportConstants.HARSH_SCAN_STEP;
        return level.dimension().equals(Level.OVERWORLD) ?
                TeleportConstants.OVERWORLD_SCAN_STEP :
                TeleportConstants.SCAN_STEP;
    }

    // Rest of the class remains unchanged
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
        chunks.add(center);
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x == 0 && z == 0) continue;
                chunks.add(new ChunkPos(center.x + x, center.z + z));
            }
        }
        return chunks;
    }

    public static ChunkPos generateNewChunk(ServerLevel level) {
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        int range = TeleportConstants.TELEPORT_RANGE / 16;
        int attempts = 0;

        do {
            ChunkPos newChunk = new ChunkPos(
                    rand.nextInt(-range, range + 1),
                    rand.nextInt(-range, range + 1)
            );

            if (!TeleportManager.CHUNK_CACHE.containsKey(newChunk) &&
                    !level.hasChunk(newChunk.x, newChunk.z)) {
                TeleportManager.CHUNK_CACHE.put(newChunk, true);
                return newChunk;
            }
            attempts++;
        } while (attempts < 100);

        return new ChunkPos(rand.nextInt(-range, range + 1), rand.nextInt(-range, range + 1));
    }
}