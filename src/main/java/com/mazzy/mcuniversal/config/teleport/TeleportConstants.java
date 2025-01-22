package com.mazzy.mcuniversal.config.teleport;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.core.registries.Registries;

public class TeleportConstants {
    public static final int TELEPORT_RANGE = 100000;
    public static final int MAX_RETRIES = 3;
    public static final int MAX_HARSH_RETRIES = 2;
    public static final int CHUNK_PREGEN_RADIUS = 2;
    public static final int WARMUP_SECONDS = 3;
    public static final int SCAN_STEP = 3;
    public static final int HARSH_SCAN_STEP = 1;
    public static final long MAX_TASK_DURATION_MS = 30000;

    public static final ResourceLocation EARTH_DIM_LOCATION = new ResourceLocation("mcuniversal", "extra");
    public static final ResourceKey<Level> EARTH_DIM_KEY =
            ResourceKey.create(Registries.DIMENSION, EARTH_DIM_LOCATION);
}