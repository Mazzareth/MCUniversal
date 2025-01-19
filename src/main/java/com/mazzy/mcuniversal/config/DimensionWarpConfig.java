// DimensionWarpConfig.java
package com.mazzy.mcuniversal.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber
public class DimensionWarpConfig {
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final CommonConfig COMMON;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        COMMON = new CommonConfig(builder);
        COMMON_SPEC = builder.build();
    }

    public static class CommonConfig {
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> dimensionWhitelist;

        public CommonConfig(ForgeConfigSpec.Builder builder) {
            builder.push("randomTeleport");
            dimensionWhitelist = builder
                    .comment("List of dimensions (by their namespaced ID) that are eligible for random teleport.")
                    .defineList("dimension_whitelist", defaultDimensionList(),
                            // Each item must be String
                            obj -> obj instanceof String
                    );
            builder.pop();
        }

        private List<String> defaultDimensionList() {
            List<String> dims = new ArrayList<>();
            dims.add("minecraft:overworld");
            dims.add("minecraft:the_nether");
            dims.add("mcuniversal:extra");
            // Add any others you like
            return dims;
        }
    }
}