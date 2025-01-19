// Example: McUniversalConfig.java
package com.mazzy.mcuniversal.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber
public class McUniversalConfig {

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

            // A list of dimension ResourceLocations that can be used for random TP
            dimensionWhitelist = builder
                    .comment("List of dimensions (by their ResourceLocation) " +
                            "that support random teleportation.")
                    .defineList("dimensionWhitelist",
                            defaultDimensionList(), // A default
                            // Validator: each element must be a string
                            obj -> obj instanceof String
                    );

            builder.pop(); // end "randomTeleport"
        }

        private List<String> defaultDimensionList() {
            List<String> defaultDims = new ArrayList<>();
            defaultDims.add("minecraft:overworld");
            defaultDims.add("minecraft:the_nether");
            // ... add any other default dimensions you’d like
            return defaultDims;
        }
    }
}