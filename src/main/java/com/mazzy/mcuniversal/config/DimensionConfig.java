package com.mazzy.mcuniversal.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class DimensionConfig {
    // The server config spec object
    public static final ForgeConfigSpec SERVER_SPEC;

    // Example config values
    public static final ForgeConfigSpec.ConfigValue<String> DIMENSION_PATH;
    public static final ForgeConfigSpec.ConfigValue<Long> DIMENSION_SEED;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("DimensionSettings");

        DIMENSION_PATH = builder
                .comment("Path to the external region folder for the dimension.")
                .define("dimensionPath", "C:/path/to/premade_map");

        DIMENSION_SEED = builder
                .comment("Seed for generating new chunks in the dimension.")
                .defineInRange("dimensionSeed", 12345L, Long.MIN_VALUE, Long.MAX_VALUE);

        builder.pop();

        SERVER_SPEC = builder.build();
    }
}